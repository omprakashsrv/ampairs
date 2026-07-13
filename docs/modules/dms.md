# dms module

Brand **distribution-management visibility** — the brand-facing, pull-only layer over published, recomputable snapshots (feature 021, US3/4/5). Every cross-tenant read passes the `trade` module's `CrossTenantReadGuard`. Depends on `trade`, `core`.

## REST Endpoints (`/dms/v1`)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/dms/v1/snapshots/secondary-sales?brand_workspace_id&distributor_workspace_id` | Consented secondary-sales rollup |
| GET | `/dms/v1/snapshots/distributor-stock?...` | Consented on-hand stock |
| POST | `/dms/v1/targets` | Create a sales target |
| GET | `/dms/v1/targets?brand_workspace_id&distributor_workspace_id?` | Targets (consent-gated if distributor given) |

## Key entities
`SecondarySalesSnapshot` (versioned; `attributed_brand_workspace_id` + nullable `brand_product_uid`/`brand_sku_code` + `area_code`), `DistributorStockSnapshot` (versioned, point-in-time), `SalesTarget` (tier PRIMARY/SECONDARY).

## Key patterns
- **`SnapshotAttributionCalculator`** (pure, fully unit-tested): deterministic two-level attribution + aggregation.
  - Hop A: a sale is attributed to a brand iff its as-of-sale brand label is designated for it (point-in-time); other-brand/untagged excluded (no leakage).
  - Hop B: itemized by brand SKU where a confirmed mapping exists, else a single aggregated "unmapped" bucket (never dropped).
  - Area: keyed off the retailer pincode (national standard; comparable across distributors), `UNKNOWN` when absent.
- Recompute replaces a distributor's rows wholesale → backdated/cancelled sources rebuild cleanly.

## Migrations
`V1.0.119` (secondary_sales_snapshots, distributor_stock_snapshots, sales_targets). PostgreSQL + MySQL.

## Deferred follow-ups
`@EventListener` on `InvoiceFinalizedEvent`/`InvoiceCancelledEvent` (+ order/inventory) → build `RawSale`s via public services → `SnapshotService.recompute` (debounced ≤5 min); live `nativeQuery` all-linked cross-distributor read.
