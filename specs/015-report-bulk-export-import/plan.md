# Implementation Plan: Data Export & Bulk Upload

**Branch**: `claude/report-bulk-export-import-2mb4aq` | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/report-api.md](./contracts/report-api.md)

## Summary

Deliver a **module-agnostic Data Exchange capability** with two faces — **export (download)** and **import (bulk upload)** — across CSV / JSON / XML / Excel, on a **hybrid** runtime: the client generates and imports against its on-device Room DB for offline/everyday volumes; the backend runs heavy/validated **async jobs** for scale and rich formats. The cornerstone insight is that **bulk upload is a thin file→DTO adapter over the canonical `/sync` UID-keyed bulk-upsert** that every module already exposes — so "update each record by UID" needs no new write API, and extends to any module that has (or adds) a `/sync` endpoint. Per-module behavior is supplied by a small **descriptor** registered by each module; the export/import engine itself is generic. Custom reports are **column-select + filters** saved as syncable **Export Templates**.

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Spring Boot 4.0 / Java 21; App Kotlin 2.4 KMP / Compose Multiplatform 1.11.
**Primary Dependencies**: Backend — Spring Data JPA, Jackson (already has `jackson-datatype-jsr310`), virtual-thread executors, file/object-storage module, event (STOMP/Kafka); **new**: `jackson-dataformat-csv` + `jackson-dataformat-xml` (Jackson family, already in the stack) and **Apache POI `poi-ooxml`** for Excel. App — Room KMP, Ktor, Metro DI, FileKit (already used by `feature/file`), `CentralSyncService`; **new**: a small per-platform spreadsheet/zip writer (see research.md R4).
**Storage**: Backend — MySQL/Postgres via Flyway (new `report` module migrations) for jobs + templates; artifacts in object storage (S3/MinIO/local) via the `file` module. App — Room (`export` feature DB, workspace-scoped) for templates + local job tracking; exported/imported files via device storage (FileKit / `FileManager`).
**Testing**: Backend — JUnit + Testcontainers (`./gradlew :report:test`), contract tests for `/sync`-shaped template endpoint and job endpoints. App — compile gate on all 3 targets; unit tests for format writers/parsers and the descriptor→DTO mapping.
**Target Platform**: Backend Linux server; App Android / iOS / Desktop (JVM) / Wasm (export/import primarily Android/iOS/Desktop).
**Project Type**: Mobile + API (new backend module `report`; new app feature module `feature/export`).
**Performance Goals**: CLIENT export/import streams in batches (default 1k rows/batch) to bound memory; SERVER jobs handle ≥50k rows without blocking request threads (virtual-thread worker + batched 100-row upserts, matching the existing `/sync` push batch size).
**Constraints**: Offline-capable CLIENT path (no network); tenant isolation on every read/write; round-trip-stable machine formats for ids/timestamps/money-minor while display columns honor workspace locale.
**Scale/Scope**: All ~10 data-bearing modules eligible; v1 onboards customer + product + one more (order or offers) to prove genericity.

## Constitution Check

Mapped to the repo rules (`.claude/rules/*`, both CLAUDE.md files):

| Rule | How this plan complies |
|---|---|
| Timestamps = `Instant` | Job/template timestamps use `Instant`; round-trip columns serialize ISO-8601 UTC. |
| DTO isolation | New `ExportTemplateRequest/Response`, `DataJobResponse`, `ImportResultResponse` in `report/domain/dto/`; never expose JPA entities. |
| JSON snake_case | Global strategy; no `@JsonProperty` for standard fields. |
| `ApiResponse<T>` wrapper + no try/catch in controllers | All endpoints return `ApiResponse`; errors bubble to the global handler. |
| Tenant context at controller | Template + job controllers set/clear `TenantContextHolder`; services never do. Jobs extend `OwnableBaseDomain`. |
| One canonical `/sync` contract | Export Templates ride the **exact** `/sync` shape (`GET/POST /report/v1/templates/sync`). **Bulk import reuses each module's existing `/sync` upsert** rather than inventing a write path. |
| Flyway both vendors | `report/src/main/resources/db/migration/{mysql,postgresql}/`; add `report` to `migrationModules`. |
| Module boundaries | New bounded context `report`; per-module descriptors live in **their own** modules (no cross-module repo access — the engine calls module **services**, e.g. `CustomerService.bulkUpsert`). |
| App offline-first | CLIENT import writes Room `synced=false` + `markPendingPush`; never calls the API in the write path. SERVER calls go through a SyncDelegate-style boundary. |
| App KMP purity | CSV/JSON/XML writers/parsers live in `commonMain` (pure Kotlin); Excel via expect/actual (POI on JVM, native writer/SERVER-fallback on iOS). No `java.*` in `commonMain`. |
| Metro DI | Descriptors contributed `@ContributesIntoMap(WorkspaceScope::class)` keyed by module; template DB `@SingleIn(WorkspaceScope::class)` + closable registry. |

**No constitution violations** → Complexity Tracking left empty.

## Project Structure

### Documentation (this feature)

```
specs/015-report-bulk-export-import/
├── spec.md              # Feature spec (done)
├── plan.md              # This file
├── research.md          # Decisions & rationale (done)
├── data-model.md        # Entities, columns, state machine (done)
├── contracts/
│   └── report-api.md     # REST contract: templates /sync + job endpoints (done)
└── tasks.md             # /speckit.tasks output (not created yet)
```
Companion **app-side** design doc lives in the app repo: `ampairs-app/docs/design/report-bulk-export-import.md`.

### Source Code

**Backend — new module `report/`** (`com.ampairs.report`):
```
report/
├── build.gradle.kts                     # + jackson-dataformat-csv/xml, poi-ooxml
├── src/main/kotlin/com/ampairs/report/
│   ├── controller/
│   │   ├── ExportTemplateController.kt    # GET/POST /report/v1/templates/sync  (canonical /sync)
│   │   ├── DataExportController.kt        # POST /report/v1/exports  → job; GET /report/v1/exports/{uid}
│   │   └── DataImportController.kt        # POST /report/v1/imports/{module} (multipart) → job; GET .../{uid}
│   ├── domain/
│   │   ├── model/        ExportTemplate, DataJob, ImportRowError (OwnableBaseDomain)
│   │   ├── dto/          ExportTemplate{Request,Response,UpdateRequest}, DataJobResponse, ImportResultResponse
│   │   └── service/      ExportTemplateService, DataExportService, DataImportService
│   ├── engine/          # generic, module-agnostic
│   │   ├── format/        FormatWriter (CSV/JSON/XML/EXCEL), FormatReader  (Jackson + POI)
│   │   ├── descriptor/    ModuleExportDescriptor (SPI) + ModuleExportRegistry
│   │   └── job/           DataJobWorker (virtual-thread executor + persisted queue drain)
│   ├── repository/      ExportTemplateRepository, DataJobRepository, ImportRowErrorRepository
│   ├── sync/            ExportTemplateCheckpointContributor   # ride existing sync SPI
│   ├── listener/        DataJobCompletionListener  # emit STOMP/Kafka event + notification
│   ├── config/          ReportConfig (executor, retention scheduler)
│   └── exception/       ReportException, JobNotFoundException, UnsupportedModuleException
└── src/main/resources/db/migration/{mysql,postgresql}/V<next>__report_init.sql
```
Per-module descriptors live **in each module** (e.g. `customer/.../export/CustomerExportDescriptor.kt`) implementing the `ModuleExportDescriptor` SPI from `report` (or a tiny `core` interface to avoid `report`→domain coupling — see research R5). Each descriptor declares columns + maps rows to the module's existing `{X}UpdateRequest`/`{X}Response` and points the importer at the module's `bulkUpsert` service.

Wire-up: add `implementation(project(":report"))` to `ampairs_service`; add `"report"` to `migrationModules`; add descriptor deps where modules implement the SPI.

**App — new feature module `feature/export/`** (`com.ampairs.export`):
```
feature/export/src/
├── commonMain/kotlin/com/ampairs/export/
│   ├── data/api/         ExportTemplateApi(+Impl)  # /report/v1/templates/sync ; DataJobApi  # server export/import + status
│   ├── data/db/          ExportTemplateEntity/Dao, DataJobEntity/Dao (local-only job tracking), ExportDatabase
│   ├── data/repository/  ExportTemplateRepository (local-only + markPendingPush), DataJobRepository (local)
│   ├── domain/           ExportTemplate, DataJob, ExportFormat, GenerationLocation, ImportMode
│   ├── engine/
│   │   ├── ModuleExporter (SPI)   # per-module: columns + Room read + row mapping
│   │   ├── ModuleExporterRegistry # Map<moduleKey, ModuleExporter> via Metro multibinding
│   │   ├── format/  CsvWriter/Reader, JsonWriter/Reader, XmlWriter/Reader (pure commonMain)
│   │   └── format/  SpreadsheetWriter/Reader (expect)  # actual: JVM=POI, iOS=native/zip, see research R4
│   ├── sync/             ExportTemplateSyncDelegate (@SyncEntityKey(EXPORT_TEMPLATE))
│   ├── di/               ExportModule (platform DB providers)
│   └── ui/               ExportScreen, ImportScreen, TemplateEditorScreen + ViewModels
├── androidMain / iosMain / desktopMain   # DB factories + SpreadsheetWriter actuals + file save/share
└── commonMain/composeResources/values/strings.xml
```
Per-module `ModuleExporter` implementations live **in each feature module** (e.g. `feature/customer/.../export/CustomerExporter.kt`), contributed `@ContributesIntoMap(WorkspaceScope::class) @ModuleExporterKey("customer")`, mirroring the existing `ModuleQueryExecutor` pattern in the agent module.

Wire-up: add `:feature:export` to `settings.gradle.kts`; add `reportUrl()` to `ApiUrlBuilder`; add `SyncEntity.EXPORT_TEMPLATE`; add `Route.Export` + entry provider; register the feature in `ModuleRegistry` so it appears in navigation (or surface Export/Import as an action on each module's list screen).

**Structure Decision**: Mobile + API. A single new backend bounded context (`report`) owns templates, jobs, the generic engine, and format libraries; a single new app feature module (`feature/export`) owns the client engine, templates DB/sync, and UI. **Per-module specifics are pushed into the owning modules via a descriptor SPI on both sides**, satisfying module-boundary rules and the "extensible to any module" requirement without the engine knowing any module's schema.

## Phasing (suggested implementation order)

1. **P1 — CLIENT export, one module (US1)**: `feature/export` engine + CSV/JSON/XML writers (commonMain) + Excel writer (JVM first) + `CustomerExporter` + Export UI (standard report only) + file save/share. Proves the data pipeline offline. *(No backend needed.)*
2. **P1 — Round-trip CLIENT import (US2)**: format readers + import→Room(`synced=false`)→`markPendingPush` adapter mapping rows to `CustomerUpdateRequest` shape; modes (update-only/upsert); local validation + on-device result summary + error file. Reuses existing `CustomerSyncDelegate` push.
3. **P2 — Export Templates (US3)**: backend `report` module skeleton + `ExportTemplate` entity + `/templates/sync` (canonical contract) + Flyway; app `ExportTemplateEntity`/Dao/repo/`ExportTemplateSyncDelegate` + Template editor UI (columns/filters/sort). Templates now drive export shape on both sides.
4. **P2 — SERVER jobs (US4)**: backend `DataJob` + `DataExportService`/`DataImportService` + `DataJobWorker` (virtual-thread + queue drain) + Jackson CSV/XML + POI Excel + multipart import + error report; completion event + notification; app `DataJobApi` + server-location toggle + status polling/STOMP + download.
5. **P3 — Genericity (US5)**: add `ProductExporter` + product descriptor; add `order`/`offers`; documentation for "onboard a module in one descriptor". Validate SC-006.

## Complexity Tracking

*No constitution violations — section intentionally empty.*
