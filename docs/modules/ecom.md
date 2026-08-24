# ecom module

Customer-facing **storefronts**: a workspace can publish its catalog as an online store that buyers browse, add to cart, and order from. Serves both the white-label `clientApp` and the multi-store `marketplaceApp` in the mobile repo. Public storefront endpoints are slug-scoped (buyer-facing, no `X-Workspace-ID`); management endpoints are workspace-scoped for the store owner.

## REST Endpoints

### Public storefront (buyer-facing)

| Controller | Base path | Purpose |
|---|---|---|
| `StorefrontPublicController` | `/api/v1/store/{slug}` | Published catalog, taxonomy, product listing for a storefront |
| `CartController` | `/api/v1/store/{slug}/cart` | Session-token carts: add items, claim, checkout |
| `CheckoutController` | (checkout paths under the cart/order flow) | Convert cart to an ecom order; assigns sequential `order_number` |
| `CustomerAccountController` | `/api/v1/ecom/account` | Buyer account: addresses (CRUD), linked accounts, order history, **invoices, statement & order↔invoice link** (spec 029) |
| `StorefrontAccessController` (directory) | `/api/v1/storefronts` | Storefront directory for the marketplace app |

#### Buyer account: invoices, statement & order↔invoice link (spec 029)

Read-only access to a linked buyer's workspace documents, gated the same way order access is: the
`storefront_slug` scopes the tenant and the login is resolved to a linked CRM customer (`partyUid`) —
no workspace membership, no `X-Workspace-ID`. Unlinked → **403** `NOT_LINKED`; another party's
document → **404**.

| Method & path | Purpose |
|---|---|
| `GET /account/invoices?storefront_slug&customer_id?&page?&size?` | Finalized invoices (paginated, newest first) |
| `GET /account/invoices/{invoiceUid}?storefront_slug&customer_id?` | Single invoice detail (line items + totals) |
| `GET /account/orders/{ecomOrderRef}/invoices?storefront_slug&customer_id?` | Invoices raised for an order |
| `GET /account/outstanding?storefront_slug&customer_id?` | Current balance + open bills + aging |
| `GET /account/statement?storefront_slug&customer_id?&from?&to?` | Running-balance statement (invoices + payments) |

- `GET /account/orders/{ecomOrderRef}` also gains an `invoices[]` array (same order↔invoice link).
- **Cross-module:** `ecom` reads `invoice`/`payment` only through the `core` interfaces
  `InvoiceEcomService` (impl in `invoice`) and `PartyLedgerEcomService` (impl in `payment`) — no new
  module edges, no schema change. The order↔invoice link is the existing chain
  `EcomOrder.managementOrderRef == Order.uid == Invoice.orderRefId`; the buyer-facing `order_ref` is
  resolved in the ecom controller. Only finalized (`INVOICED`) invoices are ever exposed.

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
