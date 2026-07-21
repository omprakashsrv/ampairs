# ecom module

Customer-facing storefronts: a workspace publishes its catalog as an online store; buyers browse, cart, and order. Serves the mobile repo's white-label `clientApp` and multi-store `marketplaceApp`.

## Two API surfaces
- **Public (buyer-facing, slug-scoped — NO `X-Workspace-ID`):** `/api/v1/store/{slug}/**` (catalog, cart, checkout), `/api/v1/storefronts` (directory), `/api/v1/ecom/account/**` (buyer addresses, linked accounts, order history). These endpoints are exempted from the workspace-header check in `SessionUserFilter`; the slug scopes the tenant.
- **Management (store owner, workspace-scoped):** `/api/v1/ecom/management/**` — storefront publish/unpublish + taxonomy, ecom orders (line-item edit, confirm, status), buyer accounts, storefront access entries.

## Key entities
- `Storefront` — slug, branding/seed color, publish state
- `EcomListedProduct`, `EcomTaxonomyImage` — published catalog projection
- `EcomCart` / `EcomCartItem` — session-token carts, claimable by a logged-in buyer
- `EcomOrder` / `EcomOrderLineItem` — buyer orders; sequential `order_number` assigned at checkout; status bridged to the workspace `order` module
- `CustomerAddress`, `StorefrontAccessEntry`

## Rules
- Never require `X-Workspace-ID` on buyer endpoints; resolve the tenant from the storefront slug
- Order status bridges (workspace order ↔ ecom order) go through service interfaces / application events — no cross-module repository imports
- Catalog data is a projection: the source of truth stays in `product`

## Migrations
`ecom/src/main/resources/db/migration/{postgresql,mysql}/` — write BOTH vendors

## Full docs
`docs/modules/ecom.md`
