# API Contracts — `trade` module (Brand → Distributor DMS + SFA)

Base path: `/trade/v1`. All endpoints return `ApiResponse<T>` (constitution V); paginated reads return
`ApiResponse<PageResponse<T>>`. JSON is global SNAKE_CASE. Every workspace-scoped request carries the
`X-Workspace-ID` header; `SessionUserFilter` sets the tenant.

Three contract surfaces:

| File | Surface | Tenant / actor |
|---|---|---|
| [trade-sfa-sync.md](./trade-sfa-sync.md) | Offline-first SFA `/sync` (visit, field-order, attendance, beat, journey-plan) | distributor (FIELD_REP / MANAGER) |
| [trade-network-actions.md](./trade-network-actions.md) | Link invite/accept/revoke, scheme publish, claim lifecycle, primary-order handshake | brand + distributor |
| [trade-snapshots.md](./trade-snapshots.md) | Consented read of secondary-sales / distributor-stock / targets | brand (consent-gated) |

**Cross-tenant rule**: every snapshot/rollup read passes `CrossTenantReadGuard` — an `ACCEPTED` `TradeLink`
whose `ConsentScope` permits the data category, with the retailer dimension projected per
`retailerVisibility` (CODED default, IDENTIFIED opt-in, never full PII). No live cross-tenant table reads in
normal flows; genuine cross-tenant SQL uses `nativeQuery = true` behind the guard.

**Errors** (bubble to global handler → `ApiResponse` error): `ConsentRequiredException` (403 — no/insufficient
link), `LinkStateException` (409 — illegal link transition), `ClaimStateException` (409 — illegal claim
transition), `TradeException` (422 — validation).
