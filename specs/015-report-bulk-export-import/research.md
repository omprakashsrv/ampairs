# Research & Decisions: Data Export & Bulk Upload

Decisions that shape the plan, each with rationale and the alternatives rejected. Driven by the clarification answers (hybrid; all formats both sides; column-select+filters templates) and the existing architecture.

---

## R1 — Bulk upload IS the existing `/sync` upsert, not a new write API

**Decision**: Import maps each file row to the module's existing canonical **`POST /{module}/v1/{resource}/sync`** UID-keyed bulk-upsert (`List<{X}UpdateRequest>` → `List<{X}Response>`). No new per-record or per-module write endpoint is created for updates.

**Why**: The `/sync` POST already does exactly "update each record, matched by UID, in bulk" (`CustomerService.bulkUpsert` finds by `uid`, applies the request, saves — preserving `refId`). It is already idempotent by `uid`, already tenant-scoped, already used by the offline push. Reusing it makes bulk upload (a) instantly available for every module already on the contract (customer, customer_group/type, product + catalog, unit, setting, order, invoice, form), (b) consistent with the offline push semantics, and (c) extensible: a new module ("offers") becomes importable the moment it adds the standard `/sync` endpoint.

**Rejected**: A dedicated `/bulk-update` endpoint per module (duplicates `/sync`, diverges in conflict handling); a single mega "import everything" endpoint (breaks module boundaries, needs the report module to know every schema).

**Consequence**: Import is a thin **file → `List<{X}UpdateRequest>` → existing upsert** adapter. CLIENT import skips even the HTTP call: it writes the mapped rows into the module's Room tables as `synced = false`, and the existing `CentralSyncService` push delivers them to `/sync`.

---

## R2 — Hybrid processing: client offline for everyday, backend for scale/validation

**Decision**: Support **both** locations behind a per-run / per-template `generation_location` flag (CLIENT | SERVER).

- **CLIENT**: export reads the module's Room data and writes a file on-device (offline); import parses a file and writes rows to Room (`synced=false`) → the existing push reconciles them. Default for typical volumes and when offline.
- **SERVER**: export/import run as async backend jobs (rich Excel, strict validation, large volumes, downloadable error report).

**Why**: The app is offline-first and already holds every module's full dataset locally, so the most common flows (export a few hundred/thousand rows, bulk-edit, re-upload) should not require connectivity or a server round-trip. But on-device generation has real limits — memory/time on huge datasets, and rich `.xlsx` on iOS — and server-side validation gives an authoritative, auditable result. Offering both, with a sensible default (CLIENT when offline or below a row threshold; SERVER otherwise), covers both ends without forcing one compromise.

**Rejected**: Client-only (no server validation, weak Excel on iOS, can't scale); Backend-only (loses the offline-first promise and forces connectivity for a trivial export).

---

## R3 — Formats: Jackson family + POI on the backend; pure-Kotlin + expect/actual on the client

**Decision**:
- **Backend**: CSV via `jackson-dataformat-csv`, XML via `jackson-dataformat-xml`, JSON via the existing Jackson, Excel via **Apache POI (`poi-ooxml`)** (streaming `SXSSF` for large sheets). All four formats supported.
- **Client**: CSV / JSON / XML implemented in **pure `commonMain`** (string building + kotlinx.serialization for JSON); Excel via **expect/actual** `SpreadsheetWriter`/`Reader` — see R4.

**Why**: Jackson is already in the stack and its CSV/XML dataformat modules are first-party, minimal additions; POI is the de-facto standard for `.xlsx`. On the client, CSV/JSON/XML are trivial and platform-pure (no `java.*`), so they belong in `commonMain` and "just work" on iOS. Only Excel needs platform help.

**Rejected**: OpenCSV / hand-rolled CSV on the backend (Jackson CSV handles quoting/escaping correctly and is already a transitive-friendly dependency); generating Excel by emitting CSV-with-`.xlsx`-extension (the current `workspace` stub — produces a file Excel mis-opens; unacceptable now that Excel is a first-class requirement).

---

## R4 — Client Excel (`.xlsx`) on all platforms

**Problem**: Apache POI is JVM-only. The requirement is "all formats on the client too," including iOS.

**Decision**: Define `expect`/`actual` `SpreadsheetWriter` and `SpreadsheetReader`:
- **JVM targets (`androidMain`, `desktopMain`)**: actual = **Apache POI** (full fidelity, streaming for big sheets).
- **iOS/native (`iosMain`)**: actual = a **minimal pure-Kotlin OOXML writer/reader**. `.xlsx` is a ZIP of a few flat XML parts (`workbook.xml`, `sheet1.xml`, `sharedStrings.xml`, `[Content_Types].xml`, rels). For purely tabular export/import this is a small, well-bounded implementation over a KMP zip (`kotlinx-io` + a tiny zip writer, or `okio`/`korlibs`-style zip). 
- **Fallback**: if the iOS native writer is deferred, the UI forces **generation_location = SERVER** for Excel on iOS (backend POI produces the file; client downloads it). CSV/JSON/XML remain fully offline on iOS regardless.

**Why**: This keeps the universal formats truly offline everywhere, isolates the one hard platform piece behind a small interface, and has a graceful degradation (SERVER Excel) so the feature is never blocked on the native writer. Tabular OOXML is far simpler than general POI, so the native writer is feasible.

**Rejected**: Requiring connectivity for all client Excel (breaks "all formats on client"); shipping a heavy cross-platform office library (none mature/light enough in KMP); CSV-as-Excel (rejected in R3).

---

## R5 — Generic engine + per-module **descriptor** (the extensibility mechanism)

**Decision**: The export/import **engine is module-agnostic**. Each module supplies a small **descriptor**:
- **Backend**: a `ModuleExportDescriptor` SPI bean per module — `moduleKey`, column definitions (key, header, type, `isMatchKey`, `isActiveFlag`), a `fetch(filters, page)` that returns rows (via the module's own repository/service, respecting tenant + `@EntityGraph`), and `importRows(rows, mode)` that delegates to the module's existing `bulkUpsert`. Registered in a `ModuleExportRegistry` (Spring `Map<String, ModuleExportDescriptor>`).
- **App**: a `ModuleExporter` SPI per module — same column metadata, a Room read (`Flow`/paged) for export, and a `mapRowsToUpsert(rows, mode)` that writes the module's Room entities (`synced=false`) + `markPendingPush`. Registered via Metro `@ContributesIntoMap(WorkspaceScope::class) @ModuleExporterKey("customer")`.

To avoid the `report` module depending on every domain module (and vice-versa), the SPI interface lives in **`core`** (backend) and **`data/common`** (app); modules implement it, and the engine consumes the registry. This mirrors the **proven** `ModuleQueryExecutor` / `ModuleQuerySchema` pattern already used by the agent module for SafeQuery — same registration shape, same boundary discipline.

**Why**: Satisfies "extensible to any module (product, customer, offers)" with **one file per module** and no engine changes (SC-006). Keeps module boundaries intact — the engine never touches another module's repository directly; it calls the module's public service via the descriptor.

**Rejected**: Reflection/annotation scanning over entities (leaks internal columns, fragile, can't express labels/filters/match-key); the `report` module importing every domain module (boundary violation, build coupling).

---

## R6 — Custom reports = column-select + filters, saved as syncable Export Templates

**Decision**: A custom report is an **Export Template**: module key + selected columns (and order) + filters (typed: date-range, enum/status, foreign-key like group/category, text contains) + sort + default format + default location. Stored tenant-scoped, exposed on the **canonical `/sync` contract** (`/report/v1/templates/sync`) so it syncs to all devices and is available offline to drive CLIENT export.

**Why**: Matches the chosen scope (column-select + filters, not free SQL). Filters map cleanly to both Spring Data queries (backend) and Room queries (client) per the descriptor's column types, so the **same template runs in both locations** and produces the same shape. Syncing templates means a report configured on the web/desktop is usable on mobile offline.

**Rejected**: Free-form SQL / query-builder (powerful but single-module, needs guardrails, and the agent module's SafeQuery already covers ad-hoc analytical questions — see R7); per-device local-only templates (a saved report should follow the user across devices).

---

## R7 — Relationship to the agent module's SafeQuery (don't duplicate)

**Decision**: Keep this feature as **row-level data export/import**. Do **not** route exports through the agent SafeQuery/text-to-SQL engine. Leave SafeQuery as the path for ad-hoc *analytical* questions ("how many customers", "total sales this month").

**Why**: SafeQuery answers aggregate questions and depends on an on-device LLM (RAM ≥ 3 GB) to generate SQL — wrong tool for deterministic, full-fidelity, offline data dumps and round-trip imports. The descriptor approach is deterministic, needs no model, and produces import-compatible columns (incl. `uid`). They are complementary: SafeQuery for "tell me a number," export for "give me the rows / let me bulk-edit them."

**Note**: The export descriptor and the SafeQuery `ModuleQuerySchema` overlap (curated tables/columns per module). A later refactor could share column metadata between them, but they stay separate engines.

---

## R8 — Jobs, async, events, retention (reuse existing infra)

**Decision**: SERVER export/import are **persisted `DataJob`s** drained by a **virtual-thread executor + `@Scheduled` queue drain**, exactly like the `notification` module's queue. Completion publishes an event over the existing **STOMP/Kafka workspace-events** channel (and optionally a `notification`), and the app refreshes job status (poll + event). Artifacts are stored via the existing **file/object-storage** module and **purged on a TTL** by a scheduled cleanup.

**Why**: All the primitives exist (virtual-thread executors, persisted-queue+scheduler pattern, event bus, object storage). No new infrastructure — just a new job type and worker. Import upserts in **batches of 100**, matching the existing `/sync` push batch size and DB-friendly chunking.

**Rejected**: Synchronous generation inside the request (blocks threads, times out on large data); a new external queue/broker (unjustified — the in-process persisted-queue pattern already serves notifications at scale).

---

## R9 — Round-trip data fidelity (machine vs display formats)

**Decision**: Columns split into **machine columns** (stable, round-trip-critical: `uid`, timestamps as ISO-8601 UTC, money in **minor units**, enum codes, foreign-key `uid`s) and **display columns** (locale-formatted currency/date, human labels) which are **export-only / read-ignored** on import. The `uid` column is the import match-key. The importer parses machine columns strictly and ignores display-only columns.

**Why**: A bulk edit must update real fields deterministically; locale-formatted values (`₹9,20,710.50`, `27 Jun 2026`) are ambiguous to parse back. Keeping a stable machine column alongside an optional pretty column gives a file that's both human-readable and safely re-importable. This directly supports the customer round-trip in US2 and the locale rules in the app CLAUDE.md (display-only formatting; storage stays UTC/minor-units).

**Rejected**: Exporting only pretty/localized values (un-reimportable); exporting only raw values (hard for humans to read during bulk edit). The dual-column approach (machine + optional display) is the compromise; templates choose which to include.
