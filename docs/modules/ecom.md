# ecom module

Customer-facing **storefronts**: a workspace can publish its catalog as an online store that buyers browse, add to cart, and order from. Serves both the white-label `clientApp` and the multi-store `marketplaceApp` in the mobile repo. Public storefront endpoints are slug-scoped (buyer-facing, no `X-Workspace-ID`); management endpoints are workspace-scoped for the store owner.

## REST Endpoints

### Public storefront (buyer-facing)

| Controller | Base path | Purpose |
|---|---|---|
| `StorefrontPublicController` | `/api/v1/store/{slug}` | Published catalog, taxonomy, product listing for a storefront |
| `CartController` | `/api/v1/store/{slug}/cart` | Session-token carts: add items, claim, checkout |
| `CheckoutController` | (checkout paths under the cart/order flow) | Convert cart to an ecom order; assigns sequential `order_number` |
| `CustomerAccountController` | `/api/v1/ecom/account` | Buyer account: addresses (CRUD), linked accounts, order history |
| `StorefrontAccessController` (directory) | `/api/v1/storefronts` | Storefront directory for the marketplace app |

### Management (store owner, workspace-scoped)

| Controller | Base path | Purpose |
|---|---|---|
| `StorefrontManagementController` | `/api/v1/ecom/management` | Storefront create/update, publish/unpublish, taxonomy |
| `EcomOrderManagementController` | `/api/v1/ecom/management/orders` | Review ecom orders, update line items, confirm, set status (bridged to the `order` module) |
| `EcomCustomerManagementController` | `/api/v1/ecom/management/customers` | Buyer accounts linked to this store |
| `StorefrontAccessController` | `/api/v1/ecom/management/storefront/access` | Access control entries for private storefronts |

## Key Entities

- `Storefront` — the published store (slug, branding/seed color, publish state)
- `EcomListedProduct` — products listed on a storefront (projection of the catalog)
- `EcomTaxonomyImage` — taxonomy/category imagery
- `EcomCart` / `EcomCartItem` — session-token carts (claimable by a logged-in buyer)
- `EcomOrder` / `EcomOrderLineItem` — buyer orders with sequential `order_number`, status bridged to the workspace `order` module
- `CustomerAddress` — buyer delivery addresses
- `StorefrontAccessEntry` — allow-list entries for restricted storefronts

## Notes

- Buyer/storefront endpoints are exempt from the workspace-header check (`SessionUserFilter`) — the storefront slug scopes the tenant.
- Order status changes on the workspace side are bridged back to the storefront so buyers see live status.
