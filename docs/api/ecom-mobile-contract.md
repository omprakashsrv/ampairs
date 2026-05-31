# Ampairs Ecom — Mobile / Frontend API Contract

**Audience**: Compose Multiplatform (Android/iOS/Desktop) and Angular Web  
**Backend module**: `ecom`  
**Base URL prefix**: `/api/v1`  
**Last updated**: 2026-05-31

> For auth endpoints, token management, and device fingerprinting see `docs/api/mobile-contract.md`.  
> All responses follow the `ApiResponse<T>` envelope described there.

---

## Table of Contents

1. [Storefront Bootstrap](#1-storefront-bootstrap)
2. [Catalog — Browsing & Search](#2-catalog--browsing--search)
3. [Incremental Sync (Offline Mode)](#3-incremental-sync-offline-mode)
4. [Cart](#4-cart)
5. [Checkout](#5-checkout)
6. [Customer Account — Addresses](#6-customer-account--addresses)
7. [Customer Account — Orders & Tracking](#7-customer-account--orders--tracking)
8. [Offline Mode — Room DB Schema](#8-offline-mode--room-db-schema)
9. [Offline Sync Strategy](#9-offline-sync-strategy)
10. [Error Codes](#10-error-codes)

---

## 1. Storefront Bootstrap

### `GET /store/{slug}`

Fetch storefront branding and status. Call once on app launch to validate the slug is live.  
**Auth**: None

**Response `data`**

```json
{
  "uid": "SFR2026...",
  "slug": "green-mart",
  "name": "Green Mart",
  "description": "Fresh groceries delivered to your door",
  "logo_url": "https://cdn.ampairs.com/...",
  "banner_url": "https://cdn.ampairs.com/...",
  "status": "PUBLISHED"
}
```

| Field | Type | Notes |
|-------|------|-------|
| `uid` | string | Stable storefront ID — use as Room FK |
| `slug` | string | URL slug |
| `name` | string | Display name |
| `logo_url` | string \| null | CDN URL |
| `banner_url` | string \| null | CDN URL |
| `status` | string | `PUBLISHED` \| `DRAFT` \| `UNPUBLISHED` |

---

## 2. Catalog — Browsing & Search

### `GET /store/{slug}/catalog-meta`

Category tiles, subcategory chips, and brand cards for the home/browse screen.  
**Auth**: None  
**Cache**: Safe to cache locally; refresh on each app launch or after product sync.

**Response `data`**

```json
{
  "categories": [
    {
      "name": "Grains",
      "product_count": 24,
      "image_url": "https://cdn.ampairs.com/cat/grains.jpg",
      "sort_order": 1,
      "subcategories": [
        { "name": "Rice",  "product_count": 12, "image_url": null, "sort_order": 0 },
        { "name": "Atta",  "product_count": 8,  "image_url": null, "sort_order": 0 }
      ]
    }
  ],
  "brands": [
    { "name": "India Gate", "product_count": 10, "image_url": "https://cdn.ampairs.com/brand/india-gate.png", "sort_order": 1 }
  ]
}
```

**CategoryMeta**

| Field | Type | Notes |
|-------|------|-------|
| `name` | string | Category name — use as filter param |
| `product_count` | int | Visible products in this category |
| `image_url` | string \| null | Merchant-uploaded tile image |
| `sort_order` | int | Merchant-defined display order |
| `subcategories` | array | SubcategoryMeta list |

**SubcategoryMeta** — same shape as CategoryMeta minus `subcategories`.  
**BrandMeta** — same shape as CategoryMeta minus `subcategories`.

---

### `GET /store/{slug}/products`

Paginated product listing. Supports category/brand/subcategory drill-down.  
**Auth**: None

**Query params**

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `page` | int | `0` | Zero-based page number |
| `size` | int | `20` | Page size (max 100) |
| `category` | string | — | Exact category name |
| `subcategory` | string | — | Exact subcategory name |
| `brand` | string | — | Exact brand name |

**Response `data`**

```json
{
  "content": [ /* ListedProduct[] */ ],
  "page": 0,
  "size": 20,
  "total_elements": 154,
  "total_pages": 8,
  "first": true,
  "last": false
}
```

**ListedProduct object**

```json
{
  "uid": "ELP2026...",
  "name": "India Gate Basmati Rice",
  "brand": "India Gate",
  "category": "Grains",
  "subcategory": "Rice",
  "unit": "5 kg",
  "price": 499.00,
  "mrp": 549.00,
  "stock_status": "IN_STOCK",
  "stock_quantity": 200,
  "image_urls": ["https://cdn.ampairs.com/p/basmati.jpg"],
  "description": "Premium aged basmati rice"
}
```

| Field | Type | Notes |
|-------|------|-------|
| `uid` | string | Stable product ID — Room PK |
| `unit` | string \| null | Pack size, e.g. `"5 kg"`, `"500 ml"` |
| `price` | decimal | Selling price |
| `mrp` | decimal \| null | Maximum retail price — show as strikethrough |
| `stock_status` | string | `IN_STOCK` \| `LIMITED` \| `OUT_OF_STOCK` |
| `stock_quantity` | int | Exact units available |

**Savings**: `mrp - price`. Show only when `mrp != null && mrp > price`.

---

### `GET /store/{slug}/products/search`

Full-text search (PostgreSQL `tsvector`).  
**Auth**: None

**Query params**: `q` (required), `page`, `size`

**Response**: same `PageResponse<ListedProduct>` shape as above.

---

### `GET /store/{slug}/products/{productId}`

Single product detail.  
**Auth**: None

**Response `data`**: single **ListedProduct** object (same shape as above).

---

## 3. Incremental Sync (Offline Mode)

### `GET /store/{slug}/products/sync`

Returns all products changed since a given timestamp, **including unlisted products** (`is_visible: false`).  
Use this to keep the local Room DB up to date without a full re-download.  
**Auth**: None

**Query params**

| Param | Type | Required | Description |
|-------|------|----------|-------------|
| `since` | ISO-8601 UTC | **Yes** | e.g. `2026-05-31T10:00:00Z` |
| `page` | int | No (default `0`) | For large delta pages |
| `size` | int | No (default `100`) | Max 500 |

**Response `data`**

```json
{
  "items": [
    {
      "uid": "ELP2026...",
      "name": "Fortune Sunflower Oil",
      "brand": "Fortune",
      "category": "Oils",
      "subcategory": "Cooking Oil",
      "unit": "1 L",
      "price": 149.00,
      "mrp": 165.00,
      "stock_status": "IN_STOCK",
      "stock_quantity": 50,
      "image_urls": [],
      "description": null,
      "is_visible": true,
      "updated_at": "2026-05-31T10:15:00Z"
    },
    {
      "uid": "ELP2026xxx",
      "is_visible": false,
      "updated_at": "2026-05-31T09:00:00Z"
    }
  ],
  "total_changes": 3,
  "page": 0,
  "size": 100,
  "has_more": false,
  "next_since": "2026-05-31T10:20:00Z"
}
```

| Field | Type | Notes |
|-------|------|-------|
| `items` | array | **ProductSyncItem** objects |
| `has_more` | boolean | If `true`, fetch next page with same `since` |
| `next_since` | ISO-8601 | **Store this as your new sync cursor.** Server captures it before the query to avoid missing any concurrent writes. |

**ProductSyncItem** — same fields as ListedProduct **plus**:

| Field | Type | Notes |
|-------|------|-------|
| `is_visible` | boolean | `false` = product was unlisted. **Delete or hide it from your local catalog.** |
| `updated_at` | ISO-8601 | Last-modified timestamp |

**Protocol**:
1. First launch: call `/products` (paginated, all pages) to seed the Room DB. Store `next_since` = response envelope `timestamp`.
2. Subsequent opens: call `/products/sync?since=<stored_cursor>`. Paginate if `has_more: true`.
3. For each item: upsert into Room DB by `uid`. If `is_visible = false`, mark `is_visible = 0` in Room (do not delete — let the UI filter it out, so cart items still resolve).
4. After all pages: store `next_since` as the new cursor.

---

## 4. Cart

Cart is **session-based** — no auth required for guest shopping. Pass `session_token` in the URL.  
After login, **claim** the guest cart to merge it into the authenticated customer's cart.

### `POST /store/{slug}/cart`

Create a new cart. Call once per storefront visit (guest or logged-in).  
**Auth**: Optional. If `Authorization` header is present, the cart is linked to the user immediately.

**No request body.**

**Response `data`** — **CartResponse** (see §4.5)

---

### `GET /store/{slug}/cart/{sessionToken}`

Fetch the current cart with item list, subtotal, and savings.  
**Auth**: None

**Response `data`** — **CartResponse** (see §4.5)

---

### `POST /store/{slug}/cart/{sessionToken}/items`

Add a product or change its quantity. Send `quantity: 0` to remove.  
**Auth**: None

**Request**

```json
{
  "listed_product_id": "ELP2026...",
  "quantity": 2
}
```

| Field | Type | Constraints |
|-------|------|-------------|
| `listed_product_id` | string | Must be a visible, in-stock product for this storefront |
| `quantity` | int | `0` removes item; max capped at `stock_quantity` |

**Response `data`** — updated **CartResponse**

**Error codes**

| Code | HTTP | When |
|------|------|------|
| `PRODUCT_UNAVAILABLE` | 422 | Product not found or not visible |
| `INSUFFICIENT_STOCK` | 422 | `quantity > stock_quantity` |
| `CART_EXPIRED` | 410 | Cart session expired — create a new cart |

---

### `DELETE /store/{slug}/cart/{sessionToken}/items/{itemId}`

Remove one item by its `uid`.  
**Auth**: None  
**Response `data`** — updated **CartResponse**

---

### `DELETE /store/{slug}/cart/{sessionToken}`

Clear all items.  
**Auth**: None  
**Response `data`** — **CartResponse** with empty `items`

---

### `POST /store/{slug}/cart/{sessionToken}/claim`

Merge a guest cart into the authenticated customer's active cart.  
Call this **immediately after login** before proceeding to checkout.  
**Auth**: Required

**No request body.**

**Response `data`** — **CartResponse** for the merged customer cart. The guest cart is invalidated (`MERGED` status). Save the new `session_token`.

---

### 4.5 CartResponse

```json
{
  "uid": "CRT2026...",
  "session_token": "3f2a1b...",
  "status": "ACTIVE",
  "expires_at": "2026-06-01T10:00:00Z",
  "items": [
    {
      "uid": "CRI2026...",
      "listed_product_id": "ELP2026...",
      "management_product_id": "PRD2026...",
      "product_name": "India Gate Basmati Rice",
      "brand": "India Gate",
      "unit": "5 kg",
      "unit_price": 499.00,
      "mrp_at_add": 549.00,
      "quantity": 2,
      "primary_image_url": "https://cdn.ampairs.com/p/basmati.jpg",
      "line_total": 998.00,
      "line_mrp": 1098.00
    }
  ],
  "subtotal": 998.00,
  "item_total_mrp": 1098.00,
  "savings": 100.00
}
```

**CartResponse fields**

| Field | Type | Notes |
|-------|------|-------|
| `session_token` | string | **Store in Room DB / SharedPreferences** |
| `status` | string | `ACTIVE` \| `CHECKED_OUT` \| `ABANDONED` \| `MERGED` |
| `expires_at` | ISO-8601 | Guest: 24h · Authenticated: 30 days |
| `items` | array | **CartItemResponse** list |
| `subtotal` | decimal | Sum of `unit_price × quantity` |
| `item_total_mrp` | decimal \| null | Sum of `mrp_at_add × quantity` — use for "Item total (MRP)" row |
| `savings` | decimal \| null | `item_total_mrp - subtotal` — show "You save ₹X" banner |

**CartItemResponse fields**

| Field | Type | Notes |
|-------|------|-------|
| `mrp_at_add` | decimal \| null | MRP snapshotted at time of add — stable even if catalog changes |
| `line_mrp` | decimal \| null | `mrp_at_add × quantity` |
| `line_total` | decimal | `unit_price × quantity` |

---

## 5. Checkout

### `POST /store/{slug}/cart/{sessionToken}/checkout`

Places the order. Cart must be `ACTIVE` with at least one item.  
**Auth**: Required  

**Request**

```json
{
  "delivery_address_id": "ADR2026...",
  "save_address": false,
  "notes": "Please leave at the door"
}
```

**OR** inline address (when no saved address exists):

```json
{
  "delivery_address": {
    "address_line1": "12 MG Road",
    "address_line2": "Apt 4B",
    "city": "Bengaluru",
    "state": "Karnataka",
    "pin_code": "560001",
    "country": "IN",
    "phone": "9591781662"
  },
  "save_address": true,
  "notes": null
}
```

Either `delivery_address_id` or `delivery_address` must be provided (not both).

**Response `201`** — **EcomOrderResponse** (see §7.3)

**Error codes**

| Code | HTTP | When |
|------|------|------|
| `CART_EXPIRED` | 410 | Cart expired or already checked out |
| `INSUFFICIENT_STOCK` | 422 | Stock depleted between cart add and checkout |
| `VALIDATION_ERROR` | 400 | Address missing or invalid |

---

## 6. Customer Account — Addresses

All endpoints require `Authorization: Bearer <access_token>`.

### `GET /ecom/account/addresses`

List all saved delivery addresses.

**Response `data`** — array of **AddressResponse**

```json
[
  {
    "uid": "ADR2026...",
    "label": "Home",
    "address_line1": "12 MG Road",
    "address_line2": "Apt 4B",
    "city": "Bengaluru",
    "state": "Karnataka",
    "pin_code": "560001",
    "country": "IN",
    "phone": "9591781662",
    "is_default": true
  }
]
```

---

### `POST /ecom/account/addresses`

Add a new address. Returns `201`.

**Request**

```json
{
  "label": "Home",
  "address_line1": "12 MG Road",
  "address_line2": "Apt 4B",
  "city": "Bengaluru",
  "state": "Karnataka",
  "pin_code": "560001",
  "country": "IN",
  "phone": "9591781662",
  "is_default": true
}
```

| Field | Required | Notes |
|-------|----------|-------|
| `address_line1` | **Yes** | |
| `city` | **Yes** | |
| `state` | **Yes** | |
| `pin_code` | **Yes** | |
| `label` | No | e.g. `"Home"`, `"Office"` |
| `is_default` | No | Default `false` |

**Response `data`** — **AddressResponse**

---

### `PUT /ecom/account/addresses/{addressId}`

Update an existing address. Same request shape as POST.

**Response `data`** — updated **AddressResponse**

---

### `DELETE /ecom/account/addresses/{addressId}`

Delete an address. Returns `204 No Content`.

---

## 7. Customer Account — Orders & Tracking

All endpoints require `Authorization: Bearer <access_token>`.

### `GET /ecom/account/orders?storefrontSlug={slug}`

List all orders for this customer on a given storefront, newest first.

**Query params**: `storefront_slug` (required), `page` (default 0), `size` (default 20)

**Response `data`** — `PageResponse<EcomOrderResponse>`

---

### `GET /ecom/account/orders/{ecomOrderRef}?storefrontSlug={slug}`

Single order detail including line items and live status.

**Response `data`** — **EcomOrderResponse**

---

### 7.3 EcomOrderResponse

```json
{
  "uid": "ECO2026...",
  "ecom_order_ref": "ECO2026050100001",
  "storefront_id": "SFR2026...",
  "customer_name": "Rahul Kumar",
  "customer_email": "rahul@example.com",
  "customer_phone": "9591781662",
  "delivery_address": {
    "address_line1": "12 MG Road",
    "city": "Bengaluru",
    "state": "Karnataka",
    "pin_code": "560001",
    "country": "IN"
  },
  "status": "PROCESSING",
  "management_order_ref": "ORD2026...",
  "line_items": [
    {
      "uid": "ELI2026...",
      "listed_product_id": "ELP2026...",
      "management_product_id": "PRD2026...",
      "product_name": "India Gate Basmati Rice",
      "unit_price": 499.00,
      "quantity_ordered": 2,
      "quantity_confirmed": 2,
      "line_total": 998.00,
      "status": "CONFIRMED"
    }
  ],
  "subtotal": 998.00,
  "total_amount": 998.00,
  "notes": null,
  "placed_at": "2026-05-31T10:00:00Z",
  "confirmed_at": "2026-05-31T10:05:00Z"
}
```

**Order status values** (show in tracking timeline):

| Status | Customer label | Description |
|--------|----------------|-------------|
| `PENDING_MERCHANT_REVIEW` | Reviewing your order | Merchant hasn't acted yet |
| `CONFIRMED` | Order confirmed | Merchant confirmed all items |
| `PROCESSING` | Being packed | Warehouse picking |
| `DISPATCHED` | Out for delivery | In transit |
| `DELIVERED` | Delivered | Complete |
| `CANCELLED` | Cancelled | Cancelled by merchant or customer |

**Line item status values**:  
`PENDING` → `CONFIRMED` → `DISPATCHED` → `DELIVERED` \| `CANCELLED` \| `PARTIALLY_FULFILLED`

---

## 8. Offline Mode — Room DB Schema

```sql
-- Storefront (one row per store the app has loaded)
CREATE TABLE storefront (
    uid         TEXT PRIMARY KEY,
    slug        TEXT NOT NULL UNIQUE,
    name        TEXT NOT NULL,
    logo_url    TEXT,
    banner_url  TEXT,
    status      TEXT NOT NULL,
    cached_at   INTEGER NOT NULL  -- epoch ms
);

-- Taxonomy images (from /catalog-meta)
CREATE TABLE taxonomy_image (
    uid           TEXT PRIMARY KEY,
    storefront_id TEXT NOT NULL,
    type          TEXT NOT NULL,   -- CATEGORY | SUBCATEGORY | BRAND
    name          TEXT NOT NULL,
    image_url     TEXT NOT NULL,
    sort_order    INTEGER NOT NULL DEFAULT 0,
    UNIQUE(storefront_id, type, name)
);

-- Product catalog (from /products full sync + /products/sync incremental)
CREATE TABLE listed_product (
    uid                   TEXT PRIMARY KEY,
    storefront_id         TEXT NOT NULL,
    management_product_id TEXT NOT NULL,
    name                  TEXT NOT NULL,
    brand                 TEXT,
    category              TEXT,
    subcategory           TEXT,
    unit                  TEXT,
    price                 REAL NOT NULL,
    mrp                   REAL,
    stock_status          TEXT NOT NULL,  -- IN_STOCK | LIMITED | OUT_OF_STOCK
    stock_quantity        INTEGER NOT NULL DEFAULT 0,
    image_urls            TEXT NOT NULL DEFAULT '[]',  -- JSON array
    description           TEXT,
    is_visible            INTEGER NOT NULL DEFAULT 1,  -- 0 = unlisted
    updated_at            TEXT,   -- ISO-8601 from server
    FOREIGN KEY(storefront_id) REFERENCES storefront(uid)
);
CREATE INDEX idx_product_storefront_visible  ON listed_product(storefront_id, is_visible);
CREATE INDEX idx_product_category            ON listed_product(storefront_id, category);
CREATE INDEX idx_product_brand               ON listed_product(storefront_id, brand);

-- Sync cursor (one row per storefront)
CREATE TABLE sync_cursor (
    storefront_id TEXT PRIMARY KEY,
    next_since    TEXT NOT NULL,  -- ISO-8601; use as ?since= on next sync call
    synced_at     INTEGER NOT NULL  -- epoch ms
);

-- Cart (one active cart per storefront)
CREATE TABLE cart (
    uid            TEXT PRIMARY KEY,
    storefront_id  TEXT NOT NULL,
    session_token  TEXT NOT NULL UNIQUE,
    status         TEXT NOT NULL DEFAULT 'ACTIVE',
    expires_at     TEXT NOT NULL,  -- ISO-8601
    FOREIGN KEY(storefront_id) REFERENCES storefront(uid)
);

-- Cart items
CREATE TABLE cart_item (
    uid                   TEXT PRIMARY KEY,
    cart_id               TEXT NOT NULL,
    listed_product_id     TEXT NOT NULL,
    management_product_id TEXT NOT NULL,
    product_name          TEXT NOT NULL,
    brand                 TEXT,
    unit                  TEXT,
    unit_price            REAL NOT NULL,
    mrp_at_add            REAL,
    quantity              INTEGER NOT NULL,
    primary_image_url     TEXT,
    FOREIGN KEY(cart_id) REFERENCES cart(uid),
    FOREIGN KEY(listed_product_id) REFERENCES listed_product(uid)
);

-- Saved addresses (from /ecom/account/addresses)
CREATE TABLE customer_address (
    uid           TEXT PRIMARY KEY,
    label         TEXT,
    address_line1 TEXT NOT NULL,
    address_line2 TEXT,
    city          TEXT NOT NULL,
    state         TEXT NOT NULL,
    pin_code      TEXT NOT NULL,
    country       TEXT NOT NULL DEFAULT 'IN',
    phone         TEXT,
    is_default    INTEGER NOT NULL DEFAULT 0
);

-- Orders (from /ecom/account/orders — cache for order history)
CREATE TABLE ecom_order (
    uid                TEXT PRIMARY KEY,
    ecom_order_ref     TEXT NOT NULL UNIQUE,
    storefront_id      TEXT NOT NULL,
    status             TEXT NOT NULL,
    subtotal           REAL NOT NULL,
    total_amount       REAL NOT NULL,
    notes              TEXT,
    placed_at          TEXT NOT NULL,
    confirmed_at       TEXT,
    delivery_address   TEXT NOT NULL  -- JSON blob
);

CREATE TABLE ecom_order_line_item (
    uid                   TEXT PRIMARY KEY,
    order_uid             TEXT NOT NULL,
    listed_product_id     TEXT NOT NULL,
    product_name          TEXT NOT NULL,
    unit_price            REAL NOT NULL,
    quantity_ordered      INTEGER NOT NULL,
    quantity_confirmed    INTEGER,
    line_total            REAL NOT NULL,
    status                TEXT NOT NULL,
    FOREIGN KEY(order_uid) REFERENCES ecom_order(uid)
);
```

---

## 9. Offline Sync Strategy

### First launch (seeding)

```
1. GET /store/{slug}                         → upsert storefront row
2. GET /store/{slug}/catalog-meta            → upsert taxonomy_image rows
3. GET /store/{slug}/products?page=0&size=100 (loop until last=true)
   → upsert listed_product rows (is_visible=1 only)
4. Store sync_cursor.next_since = last response envelope timestamp
```

### Subsequent opens

```
1. Load storefront + catalog-meta from Room DB immediately (show UI)
2. Background:
   a. GET /store/{slug}/catalog-meta         → refresh taxonomy images
   b. GET /store/{slug}/products/sync?since=<next_since>
      → loop while has_more = true (page++)
      → for each item:
          if is_visible = true  → Room UPSERT (update price, stock, etc.)
          if is_visible = false → Room UPDATE SET is_visible = 0
      → store SyncPage.next_since as new cursor
3. Notify UI to recompose (Flow/StateFlow)
```

### Sync triggers

| Trigger | Action |
|---------|--------|
| App foreground | Full incremental sync (background) |
| Pull-to-refresh | Full incremental sync (foreground, show spinner) |
| Before checkout | Call `/products/{id}` for each cart item to validate live stock |
| After order placed | Refresh order list |

### Cart session lifecycle

```
Guest visit:
  POST /cart              → store session_token in SharedPreferences
  
User logs in:
  POST /cart/{token}/claim → store NEW session_token (old token is MERGED)
  
Token key: "ecom_cart_session_{storefrontSlug}"
```

### Stock validation before checkout

Never trust Room DB stock at checkout time. Before calling `/checkout`:

```kotlin
for (item in cartItems) {
    val live = api.getProduct(slug, item.listedProductId)
    if (live.stockQuantity < item.quantity) {
        // Show "Only X left" and cap quantity in local cart
    }
    if (live.stockStatus == OUT_OF_STOCK) {
        // Show "No longer available" and remove from cart
    }
}
```

### Offline write — cart

The cart APIs are **online-only** (no local-only write path). However, you can optimistically update the Room `cart_item` table on add/remove and roll back on API error. The server is always the source of truth for quantities (stock cap enforced server-side).

---

## 10. Error Codes

| Code | HTTP | Module | When |
|------|------|--------|------|
| `CART_EXPIRED` | 410 | ecom | Cart session expired or status != ACTIVE |
| `PRODUCT_UNAVAILABLE` | 422 | ecom | Product not found, not visible, or out of stock |
| `INSUFFICIENT_STOCK` | 422 | ecom | Requested quantity > stock_quantity |
| `STOREFRONT_NOT_FOUND` | 404 | ecom | Slug does not match a PUBLISHED storefront |
| `ECOM_ORDER_NOT_FOUND` | 404 | ecom | Order ref not found or not owned by this customer |
| `ADDRESS_NOT_FOUND` | 404 | ecom | Address UID not found or not owned by this customer |
| `INVALID_ORDER_TRANSITION` | 422 | ecom | Status transition not allowed |
| `VALIDATION_ERROR` | 400 | — | Request fields failed validation |
| `AUTH_003` | 401 | auth | Access token expired — refresh it |
| `AUTH_004` | 401 | auth | Token invalid — re-authenticate |
