# Data Model: Data Export & Bulk Upload

Backend entities (new `report` module) and the app-side mirror. Round-trip-critical fields are noted. All tenant-scoped entities extend `OwnableBaseDomain` (`uid`, `ownerId`/`@TenantId`, `refId`, `createdAt`, `updatedAt` as `Instant`).

---

## 1. ExportTemplate  *(syncable, tenant-scoped)*

A saved custom report = column-select + filters per module.

| Field | Type | Notes |
|---|---|---|
| `uid` | String | PK (prefix `EXT`). Sync match-key. |
| `moduleKey` | String | e.g. `customer`, `product`, `order`. Must match a registered descriptor. |
| `name` | String | User-visible name. |
| `selectedColumns` | JSON (ordered list of column keys) | Subset + order of the descriptor's columns. Empty ⇒ standard report (all columns). |
| `filters` | JSON | List of `{ columnKey, op, value }` (op ∈ eq, in, contains, gte, lte, between, isActive). Typed per column. |
| `sortBy` | String? | Column key. |
| `sortDir` | String | `ASC`/`DESC`. |
| `defaultFormat` | Enum | CSV / JSON / XML / EXCEL. |
| `defaultLocation` | Enum | CLIENT / SERVER. |
| `includeInactive` | Boolean | Default false (active rows only). |
| `active` | Boolean | Soft-delete (sync). |
| `synced` | Boolean | App-side only (push marker). |

- **Backend table**: `export_template`. On the canonical `/sync` contract (`/report/v1/templates/sync`).
- **App**: `ExportTemplateEntity` in the `export` Room DB (`@SingleIn(WorkspaceScope::class)`), synced via `ExportTemplateSyncDelegate` (`SyncEntity.EXPORT_TEMPLATE`). Repository is local-only + `markPendingPush`.

---

## 2. DataJob  *(SERVER: persisted on backend; CLIENT: tracked locally on device)*

A unit of export or import processing.

| Field | Type | Notes |
|---|---|---|
| `uid` | String | PK (prefix `DJB`). |
| `type` | Enum | EXPORT / IMPORT. |
| `moduleKey` | String | Target module. |
| `format` | Enum | CSV / JSON / XML / EXCEL. |
| `location` | Enum | CLIENT / SERVER. |
| `mode` | Enum? | Import only: UPDATE_ONLY / UPSERT. |
| `templateUid` | String? | Export only: the template used (if any). |
| `status` | Enum | PENDING / RUNNING / COMPLETED / PARTIAL / FAILED. |
| `totalRows` | Int | Known after read/parse. |
| `processedRows` | Int | Progress. |
| `createdRows` / `updatedRows` / `skippedRows` / `failedRows` / `conflictRows` | Int | Import counts (export uses total/processed only). `conflictRows` = rows left unchanged because of a pending unsynced local edit (FR-024). |
| `conflictPolicy` | Enum? | Import only (CLIENT): `SKIP_WITH_WARNING` (default) / `OVERWRITE_LOCAL`. |
| `inputFileUid` | String? | Import: uploaded file (in `file` module / device). |
| `outputFileUid` | String? | Export: generated artifact; or import error report. |
| `errorFileUid` | String? | Import: downloadable error report (failed rows + reasons). |
| `failureReason` | String? | Job-level failure message. |
| `startedAt` / `completedAt` | Instant? | |
| `createdBy` | String | User uid. |

- **Backend table**: `data_job`. Drained by `DataJobWorker` (virtual-thread executor + `@Scheduled` queue poll). **Tenant scope**: the worker runs outside a controller request, so it MUST establish tenant context from `DataJob.ownerId` (`TenantContextHolder.setCurrentTenant(ownerId)` in try/finally) before any repository/service access — the sanctioned manual-override of constitution IV (services still never set context themselves). Not on `/sync` — queried online via `GET /report/v1/exports/{uid}` & `/imports/{uid}`; completion pushed via STOMP/Kafka.
- **App**: `DataJobEntity` (local-only, **not synced**) tracks CLIENT jobs' progress and caches SERVER job status for the UI.

### State machine
```
PENDING ──▶ RUNNING ──▶ COMPLETED        (all rows ok)
                   ├──▶ PARTIAL           (import: some rows failed, valid rows applied)
                   └──▶ FAILED            (job-level error; no partial commit for failed rows)
```

---

## 3. ImportRowError  *(backend; materialized into the error report)*

| Field | Type | Notes |
|---|---|---|
| `id` | Long | PK. |
| `jobUid` | String | FK → DataJob. |
| `rowNumber` | Int | 1-based line in the source file. |
| `matchedUid` | String? | The row's `uid` if present/parsed. |
| `field` | String? | Offending column (null = row-level). |
| `reason` | String | Human-readable validation/parse error. |

- Aggregated into a CSV **error report** stored as `DataJob.errorFileUid`. (App CLIENT import builds the same report locally.)

---

## 4. ModuleExportDescriptor  *(code, not persisted — the extensibility SPI)*

Declared once per module. Interface in `core` (backend) / `data/common` (app) to avoid coupling the engine to domain modules.

**Backend SPI (`com.ampairs.core` … implemented in each module):**
```
interface ModuleExportDescriptor {
    val moduleKey: String
    val columns: List<ExportColumn>           // key, header, type, isMatchKey, isActiveFlag, isDisplayOnly
    fun fetch(filters: List<ExportFilter>, sort: ExportSort?, page: Int, size: Int): ExportPage   // via module repo/service, tenant-scoped
    fun importRows(rows: List<Map<String,String?>>, mode: ImportMode): ImportOutcome               // delegates to module bulkUpsert
}
```

**App SPI (`com.ampairs.common` … implemented in each feature module):**
```
interface ModuleExporter {
    val moduleKey: String
    val columns: List<ExportColumn>
    suspend fun readRows(filters, sort, includeInactive): List<Map<String,String?>>   // from Room (offline)
    suspend fun writeRows(rows: List<Map<String,String?>>, mode: ImportMode, policy: ConflictPolicy): ImportOutcome   // Room synced=false + markPendingPush; skips pending unsynced rows per policy (FR-024)
}
```
Registered: backend Spring `Map<String, ModuleExportDescriptor>`; app Metro `@ContributesIntoMap(WorkspaceScope::class) @ModuleExporterKey("customer")`.

### ExportColumn
| Field | Meaning |
|---|---|
| `key` | Stable column id (maps to a DTO/entity field). |
| `header` | File header label. |
| `type` | STRING / INT / DECIMAL / BOOLEAN / DATE / MONEY_MINOR / ENUM / FK_UID. |
| `isMatchKey` | True for `uid` — the import match-key. |
| `isActiveFlag` | True for the soft-delete column. |
| `isDisplayOnly` | True = export-only, ignored on import (locale-formatted display columns, see research R9). |

---

## 5. Column mapping example — Customer (round-trip)

| Column key | type | match/flag | export | import |
|---|---|---|---|---|
| `uid` | FK_UID/STRING | **isMatchKey** | yes | match key |
| `name` | STRING | | yes | updatable |
| `phone` | STRING | | yes | updatable |
| `email` | STRING | | yes | updatable (validated) |
| `group_uid` | FK_UID | | yes | updatable (resolved to group) |
| `active` | BOOLEAN | isActiveFlag | optional | updatable (false ⇒ soft-delete) |
| `balance_display` | STRING | isDisplayOnly | optional | ignored |
| `updated_at` | DATE(ISO-UTC) | | optional | ignored (server authoritative) |

Import builds `List<CustomerUpdateRequest>` from the updatable columns keyed by `uid` → existing `POST /customer/v1/customers/sync` (SERVER) or Room `synced=false` write → `CustomerSyncDelegate` push (CLIENT).

---

## 6. New enum values / sync wiring

- **App `SyncEntity`**: add `EXPORT_TEMPLATE`.
- **Backend**: `ExportTemplateCheckpointContributor` registered with the sync checkpoint SPI (mirrors `CustomerCheckpointContributor`).
- **Flyway** (`report` module, both vendors): `export_template`, `data_job`, `import_row_error` tables; `report` added to `migrationModules`.

---

## 7. Retention

`DataJob` + artifacts older than a configurable TTL (e.g. 7 days) are purged by a `@Scheduled` cleanup in `ReportConfig`; templates are never auto-purged (user-owned).
