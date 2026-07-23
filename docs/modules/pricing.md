# pricing module

Dynamic pricing: **price lists** (with tiered/quantity pricing), **offers/coupons**, and **geo zones** (PostGIS-backed regional pricing). Workspace-scoped; list-type resources are on the canonical offline-sync contract, plus online resolve/apply endpoints for checkout-time evaluation.

## REST Endpoints

### Price Lists (`/pricing/v1/price-lists`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/pricing/v1/price-lists/sync` | Incremental pull feed (canonical `/sync` contract) |
| POST | `/pricing/v1/price-lists/sync` | UID-keyed bulk upsert |
| GET | `/pricing/v1/price-lists/{uid}` | Get price list |
| GET | `/pricing/v1/price-lists/{uid}/items` | List items of a price list |
| GET | `/pricing/v1/price-lists/items/sync` | Item-level pull feed |
| POST | `/pricing/v1/price-lists/items/sync` | Item-level bulk upsert |
| POST | `/pricing/v1/price-lists/resolve` | Resolve the effective price for product/customer/zone context |

### Offers (`/pricing/v1/offers`)

| Method | Path | Description |
|--------|------|-------------|
| GET / POST | `/pricing/v1/offers/sync` | Canonical `/sync` pull + bulk upsert |
| GET | `/pricing/v1/offers/{uid}` | Get offer |
| POST | `/pricing/v1/offers/apply` | Evaluate/apply offers to a cart or order context |
| POST | `/pricing/v1/offers/coupon/validate` | Validate a coupon code |
| POST | `/pricing/v1/offers/coupon/redeem` | Record a coupon redemption |

### Geo Zones (`/pricing/v1/geo-zones`)

| Method | Path | Description |
|--------|------|-------------|
| GET / POST | `/pricing/v1/geo-zones/sync` | Canonical `/sync` pull + bulk upsert |
| GET | `/pricing/v1/geo-zones/{uid}` | Get geo zone |

## Key Entities

- `PriceList` / `PriceListItem` / `PriceTier` — named price books, per-product prices, quantity-break tiers
- `Offer` / `CouponRedemption` — discount rules (with optional coupon codes) and redemption records
- `AttributePredicate` — condition tree for offer/price-list applicability
- `GeoZone` / `GeoZoneMembers` — regional zones (PostGIS geometry) used to scope prices/offers

## Dependencies

- `:core` only. The dev/runtime database is PostgreSQL with PostGIS (see `docker-compose.yml`).
