# pricing module

Dynamic pricing: price lists (+ items and quantity tiers), offers/coupons, and PostGIS-backed geo zones. Tenant-scoped; list resources are on the canonical offline-sync contract, plus online `resolve`/`apply`/coupon endpoints for checkout-time evaluation.

## Key entities
- `PriceList` / `PriceListItem` / `PriceTier` — price books, per-product prices, quantity-break tiers
- `Offer` / `CouponRedemption` — discount rules and redemption records
- `AttributePredicate` — condition tree for applicability
- `GeoZone` / `GeoZoneMembers` — regional zones (PostGIS geometry)

## Base paths
- `/pricing/v1/price-lists/**` — `/sync`, `/items/sync`, `GET /{uid}`, `GET /{uid}/items`, `POST /resolve`
- `/pricing/v1/offers/**` — `/sync`, `GET /{uid}`, `POST /apply`, `POST /coupon/validate`, `POST /coupon/redeem`
- `/pricing/v1/geo-zones/**` — `/sync`, `GET /{uid}`

## Rules
- Follow the canonical `/sync` contract for list resources (`docs/guides/offline-sync-contract.md`)
- Geo queries need PostGIS — dev/runtime DB is `postgis/postgis` (see `docker-compose.yml`); keep MySQL migrations in parity where the schema allows
- Tenant context set by `SessionUserFilter` from `X-Workspace-ID` — never in services
- Depends on `:core` only — product/customer references are UID strings

## Migrations
`pricing/src/main/resources/db/migration/{postgresql,mysql}/` — write BOTH vendors

## Full docs
`docs/modules/pricing.md`
