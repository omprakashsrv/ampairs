# trade module

The cross-tenant **network & consent edge** between a brand workspace and a distributor workspace (feature 021, US2). The single, auditable trust boundary across which any data flows. Records extend `BaseDomain` (NOT `OwnableBaseDomain`): they carry explicit brand + distributor workspace ids and are never `@TenantId`-filtered to one side. Depends on `workspace`, `core`.

## REST Endpoints (`/trade/v1`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/trade/v1/links` | Brand invites a distributor (INVITED) |
| POST | `/trade/v1/links/{uid}/accept` | Distributor accepts (ACCEPTED, may tighten scope) |
| POST | `/trade/v1/links/{uid}/decline` | Distributor declines |
| POST | `/trade/v1/links/{uid}/revoke` | Distributor revokes (terminal) |
| POST/GET | `/trade/v1/network-brands` | Hop A: designate / list brand-label designations |
| POST | `/trade/v1/links/{uid}/schemes` | Publish a pricing/015 scheme down the link |
| GET | `/trade/v1/schemes?link_uid` | Distributor lists published schemes |
| POST | `/trade/v1/primary-orders` | Brand places a primary order (requires active link) |
| POST | `/trade/v1/primary-orders/{uid}/confirm` | Distributor confirms (→ distributor order) |
| POST | `/trade/v1/primary-orders/{uid}/reject` | Distributor rejects |

## Key entities
`TradeNetwork`, `TradeLink` + embedded `ConsentScope` (retailerVisibility CODED/IDENTIFIED, share flags), `NetworkRetailer`, `NetworkBrand` (Hop A), `NetworkProduct` (Hop B), `SchemePublication` (references a pricing/015 offer uid), `PrimaryOrderLink`.

## Key patterns
- `TradeLinkService` state machine: INVITED → ACCEPTED → REVOKED; INVITED → DECLINED. ≤1 non-revoked link per (brand, distributor); no self-link.
- **`CrossTenantReadGuard`** — every brand read of distributor data passes it: requires an ACCEPTED link whose `ConsentScope` permits the data category, else `ConsentRequiredException` (403). The feature's central trust boundary.
- Scheme *definition* stays in `pricing` (spec 015); this owns only the consented publish/visibility edge.
- Errors: `ConsentRequiredException` (403), `LinkStateException` (409), `TradeException` (422).

## Migrations
`V1.0.118` (networks, links, retailers, network_brands, network_products, scheme_publications, primary_order_links). PostgreSQL + MySQL.

## Deferred follow-ups
NPI import creating a product via `ProductService`; primary-order confirm creating an order via `OrderService`; withdraw publications on link revoke.
