# Ampairs Ecom — Mobile / Frontend API Contract

Base URL: `https://<host>/api/v1`

All responses are wrapped in `ApiResponse<T>`:
```json
{ "success": true, "data": { ... }, "error": null, "timestamp": "...", "path": "...", "traceId": "..." }
```

Timestamps are ISO-8601 UTC strings (`2025-01-09T14:30:00Z`). Monetary values are decimal strings.  
JSON keys are `snake_case` throughout.

---

## Authentication

Storefront customer endpoints that require login use Bearer JWT in the `Authorization` header.  
Workspace-management endpoints additionally require `X-Workspace-ID: <workspaceId>` header.

---

## Enums

| Enum | Values |
|------|--------|
| `StorefrontStatus` | `DRAFT`, `PUBLISHED`, `UNPUBLISHED` |
| `StorefrontAccessMode` | `PUBLIC`, `RESTRICTED` |
| `CartStatus` | `ACTIVE`, `CONVERTED`, `MERGED`, `ABANDONED` |
| `StockStatus` | `IN_STOCK`, `LIMITED`, `OUT_OF_STOCK` |
| `EcomOrderStatus` | `PLACED`, `PENDING_MERCHANT_REVIEW`, `CONFIRMED`, `PROCESSING`, `DISPATCHED`, `DELIVERED`, `CANCELLED` |
| `EcomLineItemStatus` | `ORDERED`, `CONFIRMED`, `CANCELLED` |
| `StorefrontAccessIdentifierType` | `USER_ID`, `PHONE`, `EMAIL`, `EXTERNAL_ID` |
| `TaxonomyType` | `CATEGORY`, `SUBCATEGORY`, `BRAND` |

---

## Storefront — Public

These endpoints resolve tenant from the `{slug}` path segment. No auth required unless noted.

### GET `/store/{slug}`
Returns storefront details.

**Response `StorefrontResponse`:**
```json
{
  "uid": "sf_01",
  "name": "My Shop",
  "slug": "my-shop",
  "description": "...",
  "logo_url": "https://...",
  "banner_url": "https://...",
  "status": "PUBLISHED",
  "access_mode": "PUBLIC",
  "published_at": "2025-01-01T00:00:00Z",
  "unpublished_at": null,
  "created_at": "...",
  "updated_at": "..."
}
```

### GET `/store/{slug}/catalog-meta`
Returns category, subcategory and brand taxonomy with product counts and optional images.

**Response `StorefrontCatalogMetaResponse`:**
```json
{
  "categories": [
    {
      "name": "Beverages",
      "product_count": 42,
      "image_url": "https://...",
      "sort_order": 1,
      "subcategories": [
        { "name": "Juice", "product_count": 10, "image_url": null, "sort_order": 0 }
      ]
    }
  ],
  "brands": [
    { "name": "Tropicana", "product_count": 5, "image_url": null, "sort_order": 0 }
  ]
}
```

### GET `/store/{slug}/products`
Paginated product listing with optional filters.

**Query params:**
| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `page` | int | 0 | Page index (0-based) |
| `size` | int | 20 | Page size |
| `category` | string | — | Filter by category name |
| `subcategory` | string | — | Filter by subcategory name |
| `brand` | string | — | Filter by brand name |

**Response `PageResponse<ListedProductResponse>`:**
```json
{
  "content": [
    {
      "uid": "lp_01",
      "name": "Mango Juice 1L",
      "brand": "Tropicana",
      "category": "Beverages",
      "subcategory": "Juice",
      "unit": "1 L",
      "price": "85.00",
      "mrp": "99.00",
      "stock_status": "IN_STOCK",
      "stock_quantity": 100,
      "image_urls": ["https://..."],
      "description": "..."
    }
  ],
  "page": 0,
  "size": 20,
  "total_elements": 42,
  "total_pages": 3
}
```

### GET `/store/{slug}/products/search`
Full-text product search.

**Query params:** `q` (required), `page` (default 0), `size` (default 20)

**Response:** same shape as product listing above.

### GET `/store/{slug}/products/{productId}`
Single product detail.

**Response:** `ListedProductResponse` (same shape as one item in the listing above).

### GET `/store/{slug}/products/sync`
Delta sync for offline-capable clients. Returns products updated since the given timestamp.

**Query params:**
| Param | Type | Description |
|-------|------|-------------|
| `since` | ISO-8601 | Last sync timestamp |
| `page` | int (default 0) | Page index |
| `size` | int (default 100) | Page size |

**Response `SyncPage<ProductSyncItem>`:**
```json
{
  "items": [
    {
      "uid": "lp_01",
      "name": "Mango Juice 1L",
      "brand": "Tropicana",
      "category": "Beverages",
      "subcategory": "Juice",
      "unit": "1 L",
      "price": "85.00",
      "mrp": "99.00",
      "stock_status": "IN_STOCK",
      "stock_quantity": 100,
      "image_urls": ["https://..."],
      "description": "...",
      "is_visible": true,
      "updated_at": "2025-06-01T10:00:00Z"
    }
  ],
  "total_changes": 5,
  "page": 0,
  "size": 100,
  "has_more": false,
  "next_since": "2025-06-01T10:00:01Z"
}
```

> **Sync pattern:** store `next_since` from the response and pass it as `since` on the next call.

---

## Cart

Cart session is identified by `sessionToken`. Guest carts are created without auth; authenticated customers may claim them.

### POST `/store/{slug}/cart`
Create a new cart.

**Auth:** optional (pass JWT if logged in to associate cart with customer immediately).

**Response:** `CartResponse`

```json
{
  "uid": "cart_01",
  "session_token": "abc123",
  "status": "ACTIVE",
  "expires_at": "2025-06-08T10:00:00Z",
  "items": [],
  "subtotal": "0.00",
  "item_total_mrp": null,
  "savings": null
}
```

### GET `/store/{slug}/cart/{sessionToken}`
Fetch current cart state.

**Response:** `CartResponse` (same shape as above, with populated `items`)

`CartItemResponse` shape:
```json
{
  "uid": "ci_01",
  "listed_product_id": "lp_01",
  "management_product_id": "mp_01",
  "product_name": "Mango Juice 1L",
  "brand": "Tropicana",
  "unit": "1 L",
  "unit_price": "85.00",
  "mrp_at_add": "99.00",
  "quantity": 2,
  "primary_image_url": "https://...",
  "line_total": "170.00",
  "line_mrp": "198.00"
}
```

### POST `/store/{slug}/cart/{sessionToken}/items`
Add item or update quantity (upsert by `listedProductId`).

**Request:**
```json
{ "listed_product_id": "lp_01", "quantity": 2 }
```

**Response:** `CartResponse`

### DELETE `/store/{slug}/cart/{sessionToken}/items/{itemId}`
Remove a single item from the cart.

**Response:** `CartResponse`

### POST `/store/{slug}/cart/{sessionToken}/claim`
Claim a guest cart after login (merges it with any existing authenticated cart).

**Auth:** required.

**Response:** `CartResponse`

### DELETE `/store/{slug}/cart/{sessionToken}`
Clear all items from the cart.

**Response:** `CartResponse` (empty items, zero totals)

---

## Checkout

### POST `/store/{slug}/cart/{sessionToken}/checkout`
Place an order from the active cart. Requires authentication.

**Auth:** required.

**Request `CheckoutRequest`** — supply either `delivery_address_id` (saved address UID) **or** an inline `delivery_address` object:
```json
{
  "delivery_address_id": "addr_01",
  "delivery_address": null,
  "save_address": false,
  "notes": "Leave at door"
}
```

Inline `delivery_address` shape:
```json
{
  "address_line1": "42 Main St",
  "address_line2": "Apt 3",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pin_code": "400001",
  "country": "IN",
  "phone": "9876543210"
}
```

**Response `EcomOrderResponse` (201 Created):**
```json
{
  "uid": "ord_01",
  "ecom_order_ref": "ECM-20250601-0001",
  "storefront_id": "sf_01",
  "customer_name": "Ravi Kumar",
  "customer_email": "ravi@example.com",
  "customer_phone": "9876543210",
  "delivery_address": { "address_line1": "42 Main St", "city": "Mumbai", "..." : "..." },
  "status": "PLACED",
  "management_order_ref": null,
  "line_items": [
    {
      "uid": "li_01",
      "listed_product_id": "lp_01",
      "management_product_id": "mp_01",
      "product_name": "Mango Juice 1L",
      "unit_price": "85.00",
      "quantity_ordered": 2,
      "quantity_confirmed": null,
      "line_total": "170.00",
      "status": "ORDERED"
    }
  ],
  "subtotal": "170.00",
  "total_amount": "170.00",
  "notes": "Leave at door",
  "placed_at": "2025-06-01T10:00:00Z",
  "confirmed_at": null
}
```

---

## Customer Account

All endpoints require authentication. No `X-Workspace-ID` header needed.

Base path: `/ecom/account`

### GET `/ecom/account/addresses`
List all saved delivery addresses for the authenticated customer.

**Response:** `List<CustomerAddressResponse>`
```json
[
  {
    "uid": "addr_01",
    "label": "Home",
    "address_line1": "42 Main St",
    "address_line2": null,
    "city": "Mumbai",
    "state": "Maharashtra",
    "pin_code": "400001",
    "country": "IN",
    "phone": "9876543210",
    "is_default": true
  }
]
```

### POST `/ecom/account/addresses`
Add a new delivery address.

**Request `CustomerAddressRequest`:**
```json
{
  "label": "Home",
  "address_line1": "42 Main St",
  "address_line2": null,
  "city": "Mumbai",
  "state": "Maharashtra",
  "pin_code": "400001",
  "country": "IN",
  "phone": "9876543210",
  "is_default": false
}
```

**Response (201):** `CustomerAddressResponse`

### PUT `/ecom/account/addresses/{addressId}`
Update an existing address.

**Request:** same as POST.  
**Response:** `CustomerAddressResponse`

### DELETE `/ecom/account/addresses/{addressId}`
Delete an address.

**Response:** 204 No Content

### GET `/ecom/account/orders`
Paginated order history for the authenticated customer, scoped to a storefront.

**Query params:**
| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `storefront_slug` | string | yes | Slug of the storefront |
| `page` | int | no (default 0) | |
| `size` | int | no (default 20) | |

**Response:** `PageResponse<EcomOrderResponse>` (same order shape as checkout response)

### GET `/ecom/account/orders/{ecomOrderRef}`
Single order detail for the authenticated customer.

**Query params:** `storefront_slug` (required)

**Response:** `EcomOrderResponse`

---

## Management — Storefront (Merchant / Admin)

All endpoints require authentication and `X-Workspace-ID` header.

### POST `/ecom/management/storefront`
Create the storefront for a workspace (one per workspace).

**Request `StorefrontRequest`:**
```json
{
  "name": "My Shop",
  "slug": "my-shop",
  "description": "...",
  "logo_url": "https://...",
  "banner_url": "https://..."
}
```

**Response (201):** `StorefrontResponse`

### GET `/ecom/management/storefront`
Get the workspace's storefront.

**Response:** `StorefrontResponse`

### PUT `/ecom/management/storefront`
Update storefront details.

**Request `StorefrontUpdateRequest`:** same fields as create (all optional).  
**Response:** `StorefrontResponse`

### PUT `/ecom/management/storefront/publish`
Publish the storefront (makes it publicly accessible).

**Response:** `StorefrontResponse` with `status: "PUBLISHED"`

### PUT `/ecom/management/storefront/unpublish`
Take the storefront offline.

**Response:** `StorefrontResponse` with `status: "UNPUBLISHED"`

---

## Management — Taxonomy Images

### GET `/ecom/management/taxonomy`
List taxonomy images (category/subcategory/brand display images), optionally filtered by type.

**Query params:** `type` (`CATEGORY` | `SUBCATEGORY` | `BRAND`, optional)

**Response:** `List<TaxonomyImageResponse>`
```json
[
  {
    "uid": "ti_01",
    "type": "CATEGORY",
    "name": "Beverages",
    "image_url": "https://...",
    "sort_order": 1
  }
]
```

### PUT `/ecom/management/taxonomy`
Upsert a taxonomy image (create or update by type+name).

**Request `TaxonomyImageRequest`:**
```json
{
  "type": "CATEGORY",
  "name": "Beverages",
  "image_url": "https://...",
  "sort_order": 1
}
```

**Response:** `TaxonomyImageResponse`

### DELETE `/ecom/management/taxonomy/{type}/{name}`
Delete a taxonomy image by type and name (e.g. `DELETE /ecom/management/taxonomy/CATEGORY/Beverages`).

**Response:** 204 No Content

---

## Management — Storefront Access Control

Controls which customers can access a `RESTRICTED` storefront.

### GET `/ecom/management/storefront/access`
List access entries (paginated).

**Query params:** `page` (default 0), `size` (default 20)

**Response:** `PageResponse<StorefrontAccessEntryResponse>`
```json
{
  "content": [
    {
      "uid": "ae_01",
      "storefront_id": "sf_01",
      "identifier_type": "PHONE",
      "identifier_value": "9876543210",
      "created_at": "2025-06-01T10:00:00Z"
    }
  ]
}
```

### POST `/ecom/management/storefront/access`
Grant access to a customer identifier.

**Request `StorefrontAccessEntryRequest`:**
```json
{ "identifier_type": "PHONE", "identifier_value": "9876543210" }
```

**Response (201):** `StorefrontAccessEntryResponse`

### DELETE `/ecom/management/storefront/access/{entryUid}`
Revoke an access entry.

**Response:**
```json
{ "deleted": true, "uid": "ae_01" }
```

### POST `/ecom/management/storefront/access/bulk-import`
Bulk-import access entries from CSV.

**Content-Type:** `text/plain`  
**Body:** CSV text — one identifier per line in format `IDENTIFIER_TYPE,value` (e.g. `PHONE,9876543210`)

**Response `StorefrontAccessBulkImportResult`:**
```json
{ "imported": 45, "skipped": 2, "failed": ["bad_row_3"] }
```

---

## Management — Orders

### GET `/ecom/management/orders`
List ecom orders for the workspace, optionally filtered by status. Supports Spring Data pagination.

**Query params:** `status` (optional enum), `page`, `size`, `sort`

**Response:** `PageResponse<EcomOrderManagementResponse>`

`EcomOrderManagementResponse` extends the customer view with merchant-only fields:
```json
{
  "uid": "ord_01",
  "ecom_order_ref": "ECM-20250601-0001",
  "status": "PLACED",
  "storefront_id": "sf_01",
  "workspace_id": "ws_01",
  "customer_id": "usr_01",
  "customer_name": "Ravi Kumar",
  "customer_email": "ravi@example.com",
  "customer_phone": "9876543210",
  "delivery_address": { "..." : "..." },
  "line_items": [
    {
      "uid": "li_01",
      "management_product_id": "mp_01",
      "product_name": "Mango Juice 1L",
      "unit_price": "85.00",
      "quantity_ordered": 2,
      "quantity_confirmed": null,
      "line_total": "170.00",
      "status": "ORDERED"
    }
  ],
  "subtotal": "170.00",
  "total_amount": "170.00",
  "notes": null,
  "placed_at": "2025-06-01T10:00:00Z",
  "confirmed_at": null,
  "merchant_reviewed_at": null,
  "management_order_ref": null
}
```

### GET `/ecom/management/orders/{ecomOrderRef}`
Single order detail (merchant view).

**Response:** `EcomOrderManagementResponse`

### PUT `/ecom/management/orders/{ecomOrderRef}/line-items`
Edit line item quantities and statuses before confirmation.

**Request:** `List<EcomOrderLineItemEditRequest>`
```json
[
  { "uid": "li_01", "quantity_confirmed": 1, "status": "CONFIRMED" }
]
```

**Response:** `EcomOrderManagementResponse`

### POST `/ecom/management/orders/{ecomOrderRef}/confirm`
Confirm the order (transitions to `CONFIRMED`, creates a management order).

**Response:** `EcomOrderManagementResponse`

### PUT `/ecom/management/orders/{ecomOrderRef}/status`
Advance order status.

**Query params:** `newStatus` (enum value, e.g. `PROCESSING`)

**Valid transitions:** `CONFIRMED → PROCESSING → DISPATCHED → DELIVERED`; any state `→ CANCELLED`

**Response:** `EcomOrderManagementResponse`