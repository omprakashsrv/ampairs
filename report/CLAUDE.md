# report module

Data export / bulk-upload & saved report configuration. Owns the cross-module **Data Exchange**
bounded context (spec `specs/015-report-bulk-export-import`).

## Implemented so far (US3 backend slice)
- `ExportTemplate` — workspace-scoped saved custom report: `moduleKey`, `name`,
  `selectedColumns` (JSON), `filters` (JSON), `sortBy/sortDir`, `defaultFormat`, `defaultLocation`,
  `includeInactive`, `active`. Stored opaque JSON for columns/filters.
- Canonical offline-sync contract: `GET/POST /report/v1/templates/sync` (UID-keyed bulk upsert,
  feed includes soft-deleted rows). `ReportCheckpointContributor` reports the `export_template`
  checkpoint.

## Planned (later phases — see the spec)
- `DataJob` (EXPORT/IMPORT async jobs) + `ImportRowError`, `DataJobWorker` (virtual-thread queue),
  `ModuleExportDescriptor` SPI (in `core`) implemented per module, Jackson CSV/XML + POI Excel,
  multipart import, completion events, retention TTL.
- Bulk **update** never lives here — it reuses each module's existing `/sync` UID-keyed upsert.

## Base path
`/report/v1/templates/**`

## Migrations
`V1.0.105` (export_template)

## Notes
- Tenant scope comes from `SessionUserFilter` / `@TenantId` (workspace-scoped). The future async
  worker establishes tenant scope from `DataJob.ownerId` in try/finally (it runs off-request).
