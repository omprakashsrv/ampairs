# Feature Specification: Data Export & Bulk Upload (Reports + Round-Trip Bulk Edit)

**Feature Branch**: `claude/report-bulk-export-import-2mb4aq`
**Created**: 2026-06-27
**Status**: Draft
**Input**: User description: "Research and plan the report generation / data export module. Data can be exported in multiple standard formats (XML, CSV, Excel, JSON). Provide standard reports plus configurable custom reports per module. Extend the same functionality to bulk upload — e.g. download the full customer report with UID, edit customer details in bulk, then upload a bulk job that updates each customer. Bulk upload should update via API calls and be extensible to any module (product, customer, offers). Decide whether download / bulk-upload processing should run fully offline on the client DB, only on the backend, or both."

**Scope decisions (from clarification):**
- **Processing location**: **Hybrid** — client generates from Room offline; backend runs heavy/validated async jobs. (US1, US4)
- **Formats**: **All formats (CSV, JSON, XML, Excel) on both client and backend.** A `generation_location` flag (CLIENT | SERVER) on the export/import config decides where a given run executes.
- **Custom reports**: **Column-select + filters per module**, saved as reusable, syncable **Export Templates**.
- **Deliverable**: full speckit spec + plan + companion app design doc. Nothing built yet.

---

## Problem Statement *(context)*

Today the platform has **no general export or bulk-import capability**. The only export code is two ad-hoc CSV builders in the `workspace` module (members / invitations) that hand-concatenate strings, stub Excel as CSV, and have no XML/JSON, no per-module reuse, and no import counterpart. The mobile app has zero export/import — only invoice HTML-for-print.

Yet the foundations for a clean, generic solution already exist and are unused for this purpose:

1. Every syncable module already exposes a **canonical UID-keyed bulk-upsert endpoint** (`POST /{module}/v1/{resource}/sync`, accepting `List<{X}UpdateRequest>`). This *is* a bulk-update API — bulk upload only needs a file→DTO adapter on top of it.
2. The app is **offline-first**: every module already holds its full dataset in Room, and the `CentralSyncService` already pushes local edits (`synced = false`) to those `/sync` endpoints. A client-side bulk edit only needs to write rows back to Room as unsynced — the existing push delivers the updates.
3. The backend already runs **async work** (virtual-thread executors, persisted queue + scheduled drain, STOMP/Kafka events) and has a **file/object-storage module** (S3/MinIO/local) for storing generated artifacts.

The result we want: a single, module-agnostic **Data Exchange** capability that lets a user (a) download any module's data as CSV/JSON/XML/Excel using standard or saved custom reports, and (b) round-trip — edit the exported file in bulk and upload it to update every record by UID — working **offline against the client DB for everyday volumes and on the backend for large/validated jobs**.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Export a module's data to a standard file, offline (Priority: P1)

A business owner opens any data-rich module (customers, products, orders, …), taps **Export**, picks a format (CSV, JSON, XML, or Excel), and gets a file saved/shared from the device. With no saved template, a sensible **standard report** (all primary columns, active records) is produced. This works with no connectivity because the data is already on-device.

**Why this priority**: This is the smallest end-to-end slice that delivers value and proves the per-module data pipeline. Everything else (templates, import, server jobs) builds on the same "module → rows → formatted file" path.

**Independent Test**: With connectivity disabled, export the customer module to CSV; verify the file contains a header row, one row per active customer, and a `uid` column, and that the values match what the app shows.

**Acceptance Scenarios**:

1. **Given** a module with N active records and no saved template, **When** the user exports to CSV, **Then** a file is produced with a header row and N data rows, including the record `uid`, and is offered to save/share.
2. **Given** the same module, **When** the user exports to JSON / XML / Excel, **Then** an equivalent file in that format is produced with the same logical rows and columns.
3. **Given** no network connectivity, **When** the user runs a CLIENT export, **Then** it completes entirely on-device with no network call.
4. **Given** a module with both active and soft-deleted records, **When** a standard export runs, **Then** only active records are included unless the template explicitly includes inactive rows.

---

### User Story 2 - Round-trip bulk edit: export, edit, re-upload to update each record (Priority: P1)

A user exports the **full customer list including the `uid` column**, edits many rows in a spreadsheet (e.g. corrects phone numbers, changes groups), and uploads the edited file as a **bulk update job**. Each row is matched to an existing record **by `uid`** and its fields are updated. Rows are validated; the user sees how many succeeded, how many were skipped/failed, and why.

**Why this priority**: This is the headline outcome the user asked for. It turns export into a practical mass-maintenance tool and reuses the existing UID-keyed bulk-upsert contract, so it generalizes to every module.

**Independent Test**: Export customers to CSV, change the `name` and `phone` of 5 rows in the file, re-import it; verify exactly those 5 customers are updated (matched by `uid`), no new records are created, and a result summary reports 5 updated / 0 failed.

**Acceptance Scenarios**:

1. **Given** an exported file whose rows carry valid `uid`s, **When** the user uploads it as a bulk update, **Then** each row updates the existing record with that `uid` and no duplicates are created.
2. **Given** a row whose `uid` is blank or unknown, **When** the import runs in "update-only" mode, **Then** that row is reported as skipped (not created); in "upsert" mode a new record is created with a generated `uid`.
3. **Given** a row that fails validation (e.g. malformed email, missing required field), **When** the import runs, **Then** that row is rejected with a field-level reason and the remaining valid rows still apply.
4. **Given** a completed import, **When** the user views the result, **Then** they see counts (total / updated / created / skipped / failed) and can download an **error report** listing each failed row with its reason.
5. **Given** the same file uploaded twice, **When** the second upload runs, **Then** records are upserted idempotently by `uid` (no duplication) — last write wins.
6. **Given** a record with a pending unsynced local edit, **When** a CLIENT import touches that same `uid`, **Then** the local edit is preserved by default and the row is reported as a conflict (not silently overwritten), unless the user chose overwrite-local.

---

### User Story 3 - Configure and save a custom report per module (Priority: P2)

A user customizes an export for a module: choose which **columns** to include and their order, apply **filters** (date range, status, group/category, etc.), choose a **sort**, a default **format**, and a default **generation location**. They save it as a named **Export Template** that appears next time and **syncs across their devices**. Templates can be reused for both export and as the column shape for import.

**Why this priority**: Custom configuration is the user's explicit ask and is what makes the feature "for each module" rather than one-size-fits-all. It is P2 because US1/US2 already deliver value with a standard report.

**Independent Test**: Create a customer template selecting only `uid, name, phone, group`, filtered to one customer group, sorted by name; run it and verify the output has exactly those columns in that order, only that group's customers, name-sorted; reopen the app on another device and confirm the template is present.

**Acceptance Scenarios**:

1. **Given** a module, **When** the user selects a subset of columns and an order, **Then** the export contains exactly those columns in that order.
2. **Given** filters (e.g. status = active, created in a date range, group = X), **When** the template runs, **Then** only matching records are exported.
3. **Given** a saved template, **When** the user reopens export later or on another device, **Then** the template is available (synced) and reproduces the same configuration.
4. **Given** a template with a default format and generation location, **When** the user runs it, **Then** those defaults are pre-selected (and still overridable per run).

---

### User Story 4 - Large / validated jobs run on the backend with status tracking (Priority: P2)

For large datasets or when strict server-side validation is wanted, the user chooses **generation location = SERVER**. The export becomes an async backend job that produces the file (any format, including richly formatted Excel) and returns a download link; the import becomes an async backend job that validates and upserts every row through the module's service and produces a downloadable error report. The user sees job progress and is notified on completion.

**Why this priority**: Needed for scale, rich Excel on all clients (including iOS, where in-app Excel generation is hardest), and authoritative validation — but the offline CLIENT path (US1/US2) already covers everyday use, so this is P2.

**Independent Test**: Trigger a SERVER export of a large module; verify a job is created with PENDING→RUNNING→COMPLETED status, a file is stored, and a download URL is returned; trigger a SERVER import of a file with some invalid rows and verify the job completes PARTIAL with an error report enumerating the bad rows.

**Acceptance Scenarios**:

1. **Given** generation location = SERVER, **When** the user starts an export, **Then** a job is created and its status transitions PENDING → RUNNING → COMPLETED (or FAILED), and on completion a download link to the stored file is available.
2. **Given** a SERVER import with mixed valid/invalid rows, **When** the job runs, **Then** valid rows are upserted, invalid rows are recorded, and the job ends COMPLETED (all valid) or PARTIAL (some failed) with a downloadable error report.
3. **Given** a running job, **When** the user reopens the screen or another device, **Then** the current status and progress (rows processed / total) are visible.
4. **Given** a job completes, **When** the backend finishes, **Then** the user's devices receive a completion event (existing STOMP/Kafka channel) and/or a notification.
5. **Given** a job fails irrecoverably, **When** the user views it, **Then** a clear failure reason is shown and no partial/corrupt data has been committed for failed rows.

---

### User Story 5 - One mechanism, many modules (Priority: P3)

The same export/import works across modules — customer, product, order, invoice, and new ones like offers — by each module declaring an **export/import descriptor** (its exportable columns, its import match-key, and how its rows map to the existing `/sync` DTOs). Adding a module to the feature requires only that descriptor, not new export/import engines.

**Why this priority**: Extensibility is an explicit requirement, but it is a structural property proven by enabling a second and third module after US1/US2 work for the first.

**Independent Test**: After customer export/import works, enable the product module by adding only its descriptor; verify product export and round-trip import work with no changes to the export/import engine.

**Acceptance Scenarios**:

1. **Given** a module that exposes the canonical `/sync` upsert contract and a registered descriptor, **When** the user opens Export/Import, **Then** that module is selectable and works end-to-end.
2. **Given** a module **not** on the `/sync` contract (e.g. a new "offers" module), **When** enabling import, **Then** the prerequisite is to add the standard `/sync` endpoint first; export-only can work without it.
3. **Given** two modules enabled, **When** the user switches between them, **Then** each shows its own columns, filters, and templates.

---

### Edge Cases

- **Huge export on-device**: a CLIENT export of a very large module could exhaust memory/time → the app streams rows to disk in batches and, above a configurable row threshold, recommends/forces SERVER generation.
- **Excel on iOS**: rich `.xlsx` generation is hard in shared/native code. **Decision (A1):** ship a **minimal pure-Kotlin OOXML writer** for iOS so offline Excel export holds on every platform (keeping the "all formats on the client" requirement true). SERVER generation is the **contingency** if that writer is deferred — not the baseline. CSV/JSON/XML are always offline on iOS.
- **UID collisions / wrong module**: an imported file whose `uid` prefixes belong to a different module → rows are rejected with a "wrong entity type" reason.
- **Partial connectivity during CLIENT import**: rows are written to Room as `synced = false`; the existing push retries until online — the import "succeeds locally" immediately and reconciles later.
- **Concurrent edit**: a record edited locally (unsynced) while an import also touches it → governed by **FR-024**: default **skip-with-warning** (local-edit-wins, row reported as a conflict), with an optional explicit **overwrite-local**. The import path MUST NOT silently clobber a pending unsynced local edit.
- **Format ambiguity on import**: commas/quotes/newlines in CSV fields, type coercion (numbers, booleans, dates, money in minor units) → strict, documented parsing with per-cell errors.
- **Soft-deleted rows**: export may optionally include inactive rows (with an `active` column); import setting `active = false` performs a soft-delete that propagates via the normal sync delete path.
- **Empty / header-only file on import**: reported as "0 rows", not an error.
- **Schema drift**: an imported file with unknown/missing columns → unknown columns ignored with a warning; missing required columns → job-level rejection with a clear message.
- **Tenant isolation**: a job created under one workspace must never read or write another workspace's data (enforced by tenant context + `OwnableBaseDomain`).

---

## Requirements *(mandatory)*

### Functional Requirements

**Export (download)**
- **FR-001**: System MUST export any enabled module's records to **CSV, JSON, XML, and Excel (.xlsx)**.
- **FR-002**: Every export MUST be able to include the record **`uid`** (and MUST include it whenever the export is intended for round-trip import).
- **FR-003**: System MUST support a **standard report** per module (all primary columns, active records) with no configuration.
- **FR-004**: System MUST support **custom reports** defined per module by selecting columns (and order), filters, and sort, saved as a reusable **Export Template**.
- **FR-005**: Export Templates MUST **sync across the user's devices** via the canonical `/sync` contract.
- **FR-006**: System MUST let the user choose **generation location** (CLIENT or SERVER) per run, with a template-level default.
- **FR-007**: CLIENT export MUST run **fully offline** against the on-device database (no network dependency).
- **FR-008**: SERVER export MUST run as an **async job**, store the artifact in object storage, expose **job status**, and return a **download reference** on completion.

**Import (bulk upload)**
- **FR-009**: System MUST import CSV/JSON/XML/Excel files and **update existing records matched by `uid`**.
- **FR-010**: Bulk update MUST be performed through the module's existing **canonical `/sync` UID-keyed bulk-upsert** path — no bespoke per-record write API.
- **FR-011**: System MUST support **modes**: *update-only* (skip rows without a known `uid`) and *upsert* (create rows with a generated `uid`). Default = update-only for round-trip safety.
- **FR-012**: Import MUST **validate** rows and apply valid rows even when some rows fail (partial success), reporting per-row, field-level errors.
- **FR-013**: System MUST produce a downloadable **error report** (the failed rows + reasons) for any import with failures.
- **FR-014**: CLIENT import MUST write parsed rows to the on-device DB as **unsynced**, so the existing push delivers the updates — usable offline, reconciling on reconnect.
- **FR-015**: SERVER import MUST run as an **async job** that validates and upserts server-side and tracks the same status/result/error-report lifecycle as SERVER export.
- **FR-016**: Imports MUST be **idempotent by `uid`** (re-uploading the same file does not duplicate records).
- **FR-024**: A CLIENT import MUST NOT silently overwrite a record that has **pending unsynced local edits**. The default **conflict policy is skip-with-warning** (keep the local edit, count the row as a conflict in the result); an explicit **overwrite-local** option may be offered. Conflicts MUST be surfaced in the import result, never dropped.

**Generic / cross-module**
- **FR-017**: Each module MUST be onboarded by declaring an **export/import descriptor** (exportable columns + labels + types, the import match-key, the active/soft-delete column, and the mapping to its `/sync` DTOs) — no changes to the export/import engine.
- **FR-018**: Modules **without** the `/sync` contract MAY be export-only; import requires the module to first expose the canonical `/sync` endpoint.
- **FR-019**: Both export and import MUST respect **tenant isolation** and the workspace's **locale** (currency, date/time format) for display-formatted columns, while round-trip-critical columns (ids, timestamps, money in minor units) use stable machine formats.

**Jobs & lifecycle**
- **FR-020**: SERVER jobs MUST be persisted with status (PENDING / RUNNING / COMPLETED / PARTIAL / FAILED), progress (rows processed / total), counts (total / created / updated / skipped / failed), and references to input/output/error files.
- **FR-021**: System MUST notify the user's devices on job completion via the existing event channel (STOMP/Kafka) and/or the notification module.
- **FR-022**: CLIENT jobs MAY be tracked locally (local-only progress), and need not sync.
- **FR-023**: Job artifacts MUST have a **retention policy** (configurable TTL) after which stored export/import/error files are purged.

### Key Entities *(include if feature involves data)*

- **Export Template**: a saved, syncable, per-module custom report — owner, module key, name, selected columns (+order), filters, sort, default format, default generation location. Tenant-scoped; on the `/sync` contract.
- **Data Job** (export or import): a unit of processing — type (EXPORT/IMPORT), module key, format, mode (for import), generation location, status, progress, counts, input/output/error file references, timestamps, initiator. Tenant-scoped. SERVER jobs persisted on backend; CLIENT jobs tracked locally on device.
- **Module Export/Import Descriptor**: a code-declared registration per module mapping its data to exportable columns and to the `/sync` DTOs; not user data, but the contract that makes the feature generic.
- **Import Row Error**: a failed row within an import job — row number, matched `uid` (if any), field, reason. Materialized into the downloadable error report.
- **Generated Artifact**: the export output / import input / error report file, stored via the existing file/object-storage module (SERVER) or device storage (CLIENT).

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can export any enabled module to all four formats and reopen the file in a standard tool (spreadsheet / text editor) without manual fix-up.
- **SC-002**: A round-trip edit of an exported customer file updates **exactly** the edited records, matched by `uid`, with **zero** duplicate records created, across **≥3 modules** (customer, product, one more) using only per-module descriptors.
- **SC-003**: CLIENT export and CLIENT import both complete with **network disabled**, and CLIENT-imported changes appear on the server automatically within one sync cycle after reconnect.
- **SC-004**: An import of a file containing a known mix of valid and invalid rows applies **all** valid rows and reports **every** invalid row with a field-level reason in a downloadable error report.
- **SC-005**: A SERVER export/import of a large dataset (e.g. 50k rows) completes as a tracked async job without blocking the request thread, and the user sees progress and a completion notification.
- **SC-006**: Onboarding a new module to export+import requires **only** adding its descriptor (no edits to the export/import engine), demonstrated by enabling the 3rd module.
- **SC-007**: No export or import run ever reads or writes data outside the active workspace (tenant isolation verified).

## Out of Scope (this feature)

- Scheduled/recurring exports and emailed report delivery (future; the job model leaves room).
- Analytical/aggregate reporting, charts, dashboards (this is row-level data export, not BI). The agent module's SafeQuery remains the path for ad-hoc analytical questions.
- PDF as an export format (HTML/PDF print stays per-module, e.g. invoice print).
- Cross-module joined exports in a single file (each export targets one module; the offline model is single-module by construction).
- Free-form SQL / query-builder custom reports (column-select + filters chosen instead; SafeQuery stays in the agent module).
