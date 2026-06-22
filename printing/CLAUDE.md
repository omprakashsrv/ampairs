# printing module

Server-side storage + offline-sync for the mobile app's **print templates**. Top-level bounded
context (`com.ampairs.printing`) serving the `/printing/v1/**` namespace. Templates are
workspace-scoped (tenant-filtered via `OwnableBaseDomain` + `X-Workspace-ID`). The backend treats a
template's layout as an **opaque blob**: it stores and syncs `template_json` verbatim and never
parses or renders it. All rendering happens in the app.

Depends only on `:core`. Spring discovers it via the default `com.ampairs` component/entity/repo scan
(no extra config). Previously a sub-package of `workspace`; extracted to its own module.

## What it owns
- `PrintTemplate` (entity) — one row per template, `OwnableBaseDomain` (tenant-scoped via `@TenantId ownerId`).
- The canonical offline-sync endpoints for templates (`GET` + `POST /printing/v1/templates/sync`).

## Endpoints (`/printing/v1/templates`)
Canonical `/sync` contract — see `/ampairs/docs/guides/offline-sync-contract.md` and root rule #9.
Mirrors the unit/customer sync controllers.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/sync` | Incremental pull feed. Params (snake_case): `last_sync`, `page`, `size`, `sort_by`, `sort_dir`. Returns `ApiResponse<PageResponse<PrintTemplateResponse>>`. **Includes inactive (soft-deleted) rows** so clients detect deletions. |
| `POST` | `/sync` | Bulk upsert keyed by `id` (= uid): create if absent, update if present. Soft-deleted rows (`active = false`) ride along **in-band** — there is no per-row DELETE. Returns `ApiResponse<List<PrintTemplateResponse>>`. |

Tenant context is set by `SessionUserFilter` from the `X-Workspace-ID` header — never in the service.

## Entity `print_template`
One template per `(document_type, printer_class)` pair — e.g. THERMAL invoice vs PAGE invoice.

| Column | Notes |
|---|---|
| `uid` | client-generated id (app prefix `PTPL`); server prefix `PTM` only for server-minted rows |
| `document_type` | INVOICE / ORDER / RECEIPT / LABEL … (matches app `DocumentType`) |
| `printer_class` | THERMAL / PAGE / LABEL (matches app `PrinterClass`) |
| `name` | display name |
| `template_json` | **opaque** client-rendered layout (TEXT). Stored verbatim, never parsed |
| `template_version` | client-managed revision counter (informational; LWW is by `updatedAt`) |
| `active` | soft-delete flag; `/sync` GET returns inactive rows too |
| `is_default` | the chosen template for its `(document_type, printer_class)` pair; client-managed, one default per pair |

## Sync semantics (server side)
- **Pull**: `last_sync` blank → `findAllForSync` (whole workspace, incl. inactive). Otherwise
  `findByUpdatedAtAfter(Instant.parse(decoded))`; an unparseable `last_sync` falls back to full feed.
- **Authority**: `updatedAt` (server timestamp) is authoritative for LWW. The client sends
  `updated_at` but the server ignores it on write.
- **Push**: `bulkUpsert` matches on `uid`; `applyRequest` copies fields, `asResponse` echoes back.
  Each create/update fires `EntityChangePublisher` (`print_template`) so other devices get a
  WebSocket nudge to pull.

## DTOs (`domain/dto/PrintTemplateDto.kt`)
- `PrintTemplateRequest` (push) / `PrintTemplateResponse` (pull/echo). Field names mirror the app's
  `TemplateDto`; global Jackson SNAKE_CASE handles camelCase → snake_case. `id` == uid.
- Conversions: `PrintTemplate.applyRequest(req)`, `PrintTemplate.asResponse()`. DTO isolation —
  never expose the entity (root rule #2).

## Migrations (`printing/src/main/resources/db/migration/{mysql,postgresql}/`)
- `V1.0.93__create_print_template_table.sql`
- `V1.0.94__add_print_template_is_default.sql`
Write **both** vendor variants (runtime DB is PostgreSQL). Never edit an applied migration —
check `./gradlew :ampairs_service:flywayInfo` and add a new version.

## Related: static HTML templates live in the `file` module
A STATIC template's HTML is uploaded as a real file (not in `template_json`) to the entity-scoped
file endpoint `POST /api/file/v1/images/PRINT_TEMPLATE/{templateUid}`. `template_json` then only
carries the `html_file_uid` reference. See `/ampairs/file` (`EntityImageService` has a document
branch for `PRINT_TEMPLATE` that stores raw HTML, skipping the image pipeline).

## App counterpart
`ampairs-app` → `feature/printing` (+ `printing/core|render|transport`). `TemplateSyncDelegate` and
`TemplateApi` on the app side talk to these endpoints.
