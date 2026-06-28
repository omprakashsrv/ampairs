# API Contract: Data Export & Bulk Upload (`report` module)

Base: `/api/report/v1`. All responses wrapped in `ApiResponse<T>`. Snake_case JSON. `X-Workspace-ID` required (tenant context set at controller). Timestamps `Instant` (ISO-8601 UTC).

> **Bulk-update note**: actual record updates do **not** happen here. Import maps rows to each module's **existing** `POST /{module}/v1/{resource}/sync` UID-keyed bulk-upsert. This module orchestrates jobs, templates, and file artifacts only.

---

## 1. Export Templates — canonical `/sync` contract

Identical shape to every other syncable resource (see `docs/guides/offline-sync-contract.md`).

### Pull
```
GET /report/v1/templates/sync
    ?last_sync={ISO8601}&page=0&size=100&sort_by=updatedAt&sort_dir=ASC
→ ApiResponse<PageResponse<ExportTemplateResponse>>        # includes soft-deleted rows
```

### Push (UID-keyed bulk upsert; in-band soft-delete)
```
POST /report/v1/templates/sync
Body: List<ExportTemplateUpdateRequest>                     # active upserts AND active=false rows
→ ApiResponse<List<ExportTemplateResponse>>                # delegate batches at 100
```

**ExportTemplateResponse / UpdateRequest** (DTOs in `report/domain/dto/`):
```
{
  "uid": "EXT...",
  "module_key": "customer",
  "name": "Customers with phones",
  "selected_columns": ["uid","name","phone","group_uid"],
  "filters": [{"column_key":"group_uid","op":"eq","value":"CGR..."},
              {"column_key":"active","op":"isActive","value":"true"}],
  "sort_by": "name", "sort_dir": "ASC",
  "default_format": "CSV",
  "default_location": "CLIENT",
  "include_inactive": false,
  "active": true,
  "updated_at": "2026-06-27T10:00:00Z"
}
```

---

## 2. Server Export jobs

### Start an export job
```
POST /report/v1/exports
Body:
{
  "module_key": "customer",
  "format": "EXCEL",                 # CSV | JSON | XML | EXCEL
  "template_uid": "EXT...",          # optional; else standard report
  "filters": [ ... ],                # optional ad-hoc overrides
  "include_inactive": false
}
→ ApiResponse<DataJobResponse>       # status=PENDING
```

### Poll job status
```
GET /report/v1/exports/{jobUid}
→ ApiResponse<DataJobResponse>
```

### Download artifact (when COMPLETED)
```
GET /report/v1/exports/{jobUid}/download
→ 302 redirect to signed object-storage URL  (or streamed bytes for local storage)
```

---

## 3. Server Import (bulk upload) jobs

### Upload a file and start an import job
```
POST /report/v1/imports/{moduleKey}        # multipart/form-data
  file:   <CSV|JSON|XML|XLSX>
  format: CSV|JSON|XML|EXCEL               # or inferred from content-type/extension
  mode:   UPDATE_ONLY | UPSERT             # default UPDATE_ONLY
→ ApiResponse<DataJobResponse>             # status=PENDING; file stored as input_file_uid
```
Server flow: parse → validate per row (via module descriptor) → map valid rows to `List<{X}UpdateRequest>` → call the module's `bulkUpsert` in batches of 100 → record `ImportRowError`s → build error report.

### Poll job status
```
GET /report/v1/imports/{jobUid}
→ ApiResponse<DataJobResponse>             # counts: total/created/updated/skipped/failed
```

### Download error report (when PARTIAL/COMPLETED-with-errors)
```
GET /report/v1/imports/{jobUid}/errors
→ 302 / bytes : CSV of (row_number, matched_uid, field, reason)
```

**DataJobResponse**:
```
{
  "uid": "DJB...",
  "type": "IMPORT",                  # EXPORT | IMPORT
  "module_key": "customer",
  "format": "CSV",
  "location": "SERVER",
  "mode": "UPDATE_ONLY",
  "status": "PARTIAL",               # PENDING|RUNNING|COMPLETED|PARTIAL|FAILED
  "total_rows": 1200, "processed_rows": 1200,
  "created_rows": 0, "updated_rows": 1180, "skipped_rows": 5, "failed_rows": 15,
  "input_file_uid": "FIL...", "output_file_uid": null, "error_file_uid": "FIL...",
  "failure_reason": null,
  "started_at": "2026-06-27T10:01:00Z", "completed_at": "2026-06-27T10:01:09Z"
}
```

---

## 4. Module discovery (drive the UI)

```
GET /report/v1/modules
→ ApiResponse<List<ModuleExportInfo>>
   [{ "module_key":"customer", "label":"Customers",
      "supports_import": true,            # true only if module has /sync upsert
      "columns":[{"key":"uid","header":"UID","type":"STRING","is_match_key":true,"is_display_only":false},
                 {"key":"name","header":"Name","type":"STRING"}, ...] }]
```
The app can also build this list locally from its registered `ModuleExporter`s (offline), so the screen works without this call.

---

## 5. Completion events (existing channel)

On job completion the worker publishes to the existing workspace-events topic
`/topic/workspace/{workspaceId}` (STOMP, Kafka-backed in prod):
```
{ "type": "DATA_JOB_COMPLETED", "job_uid": "DJB...", "job_type":"IMPORT", "status":"PARTIAL" }
```
The app refreshes the corresponding `DataJobEntity` and may raise a local notification. No polling required when connected.

---

## 6. Client-only paths (no HTTP)

- **CLIENT export**: `ModuleExporter.readRows()` (Room) → `FormatWriter` (commonMain CSV/JSON/XML; expect/actual Excel) → FileKit save/share. No endpoint used.
- **CLIENT import**: FileKit pick → `FormatReader` → `ModuleExporter.writeRows(rows, mode)` → module Room tables `synced=false` + `markPendingPush` → existing `CentralSyncService` push hits each module's `POST /{module}/v1/{resource}/sync`. The only network traffic is the normal sync push.
