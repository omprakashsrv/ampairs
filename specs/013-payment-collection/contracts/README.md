# API Contracts — Payment & Collection

Base path: `/payment/v1/**`. All endpoints return `ApiResponse<T>`
(`{ success, data, error, timestamp, path, traceId }`); paginated reads return
`ApiResponse<PageResponse<T>>`. JSON is global SNAKE_CASE. Every request is workspace-scoped and
requires the `X-Workspace-ID` header (`SessionUserFilter` sets tenant context).

| File | Scope |
|---|---|
| [payment-sync.md](./payment-sync.md) | Canonical offline `/sync` endpoints (pull + push) per syncable entity |
| [payment-actions.md](./payment-actions.md) | Non-sync, UI-invoked actions: statement, open-bills, aging, recompute, bounce |

**Syncable entities** (each gets `GET`/`POST .../sync`): `vouchers`, `allocations`, `ledger-entries`,
`party-balances`, `adjustments`.

**Sync contract rules** (per repo `docs/guides/offline-sync-contract.md`):
- Same URL for pull (`GET`) and push (`POST`), suffix `/sync`.
- Query params snake_case: `last_sync` (ISO-8601, optional), `page` (0-based), `size` (default 100),
  `sort_by` (default `updatedAt`), `sort_dir` (default `ASC`).
- Pull feed **includes soft-deleted rows** (`active = false`) so deletes propagate in-band.
- Push body is a UID-keyed bulk upsert (create if uid absent, else update; honor soft-delete); response
  returns the server-resolved rows.
