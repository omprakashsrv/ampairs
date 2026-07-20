# supplier module

Supplier (vendor) management for the purchase side. Tenant-scoped (`OwnableBaseDomain`), canonical offline-sync contract.

## Key entities
- `Supplier` — name, contact details, addressing, GST details, `active` (in-band soft delete)

## Base path
`/supplier/v1/**` — `GET`/`POST /supplier/v1/suppliers/sync` (canonical `/sync` contract), `GET /supplier/v1/{supplierId}`

## Rules
- Follow the canonical `/sync` contract (`docs/guides/offline-sync-contract.md`): snake_case params, pull feed includes soft-deleted rows, UID-keyed bulk upsert, in-band delete
- Tenant context set by `SessionUserFilter` from `X-Workspace-ID` — never in services
- Depends on `:core` only

## Migrations
`supplier/src/main/resources/db/migration/{postgresql,mysql}/` — write BOTH vendors

## Full docs
`docs/modules/supplier.md`
