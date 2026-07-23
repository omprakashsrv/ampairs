# purchase module

Purchase recording — supplier bills and line items. Tenant-scoped, canonical offline-sync contract (mobile records purchases offline and syncs up).

## Key entities
- `Purchase` — supplier ref, document number/date, totals, status, `active`
- `PurchaseItem` — product/variant ref, quantity, unit, rate, tax/discount breakdown

## Base path
`/purchase/v1/purchases/**` — `GET`/`POST /sync` (canonical `/sync` contract), `GET /{purchaseId}`

## Rules
- Follow the canonical `/sync` contract (`docs/guides/offline-sync-contract.md`)
- Tenant context set by `SessionUserFilter` from `X-Workspace-ID` — never in services
- Depends on `:core` only — supplier/product references are UID strings, not cross-module imports

## Migrations
`purchase/src/main/resources/db/migration/{postgresql,mysql}/` — write BOTH vendors

## Full docs
`docs/modules/purchase.md`
