# API Contract: Storefront Public API

**Base path**: `/api/v1/store`  
**Auth**: None required for browsing and cart operations. JWT required for checkout, account, and order history.  
**Tenant context**: Slug is resolved to `workspaceId`; controller sets `TenantContextHolder` before repository calls.

---

## Storefront

### GET /api/v1/store/{slug}

Returns storefront info for a given slug. Returns 404 if slug does not exist or storefront is in Draft state.

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "uid": "SF-A1B2C3D4",
    "name": "Green Mart",
    "slug": "green-mart",
    "description": "Fresh groceries delivered to your door",
    "logo_url": "https://cdn.ampairs.com/logos/green-mart.png",
    "banner_url": null,
    "status": "PUBLISHED"
  }
}
```

**Response** `404 Not Found` (slug not found or Draft state)
```json
{
  "success": false,
  "error": { "code": "STOREFRONT_NOT_FOUND", "message": "Store not found or unavailable" }
}
```

---

## Product Discovery

### GET /api/v1/store/{slug}/products

List all visible in-stock and limited-stock products for the storefront. Supports filtering and pagination.

**Query params**:
- `page` (int, default 0)
- `size` (int, default 20, max 100)
- `category` (string, optional)
- `brand` (string, optional)
- `subcategory` (string, optional)
- `stock_status` (enum: IN_STOCK | LIMITED | OUT_OF_STOCK, optional)
- `sort` (string: `price_asc` | `price_desc` | `name_asc`, default `name_asc`)

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "uid": "LP-X1Y2Z3",
        "name": "Basmati Rice 5kg",
        "brand": "India Gate",
        "category": "Grains",
        "subcategory": "Rice",
        "price": "450.00",
        "stock_status": "IN_STOCK",
        "image_urls": ["https://cdn.ampairs.com/products/basmati-5kg-main.jpg"],
        "description": null
      }
    ],
    "page_number": 0,
    "page_size": 20,
    "total_elements": 145,
    "total_pages": 8,
    "first": true,
    "last": false,
    "has_next": true,
    "has_previous": false,
    "empty": false
  }
}
```

---

### GET /api/v1/store/{slug}/products/search

Full-text search across product name, brand, category, and subcategory.

**Query params**:
- `q` (string, required, min 2 chars)
- `page`, `size`, `sort` (same as list endpoint)
- `category`, `brand`, `subcategory` (optional filters to narrow results)

**Response**: Same shape as product list.

---

### GET /api/v1/store/{slug}/products/{productId}

Get product detail.

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "uid": "LP-X1Y2Z3",
    "name": "Basmati Rice 5kg",
    "brand": "India Gate",
    "category": "Grains",
    "subcategory": "Rice",
    "price": "450.00",
    "stock_quantity": 48,
    "stock_status": "IN_STOCK",
    "image_urls": [
      "https://cdn.ampairs.com/products/basmati-5kg-main.jpg",
      "https://cdn.ampairs.com/products/basmati-5kg-side.jpg"
    ],
    "description": "Premium long-grain basmati rice"
  }
}
```

**Response** `404 Not Found` — product not listed or not visible.

---

## Cart

### POST /api/v1/store/{slug}/cart

Create a new cart for the storefront. Returns a session token the client must persist.

**Request**: Empty body (no auth required)

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "uid": "CRT-D4E5F6",
    "session_token": "3f7a1b2c-...",
    "storefront_id": "SF-A1B2C3D4",
    "status": "ACTIVE",
    "expires_at": "2026-05-31T12:00:00Z",
    "items": []
  }
}
```

---

### GET /api/v1/store/{slug}/cart/{sessionToken}

Retrieve an existing cart.

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "uid": "CRT-D4E5F6",
    "session_token": "3f7a1b2c-...",
    "status": "ACTIVE",
    "expires_at": "2026-05-31T12:00:00Z",
    "items": [
      {
        "uid": "CI-G7H8I9",
        "listed_product_id": "LP-X1Y2Z3",
        "product_name": "Basmati Rice 5kg",
        "unit_price": "450.00",
        "quantity": 2,
        "primary_image_url": "https://cdn.ampairs.com/products/basmati-5kg-main.jpg",
        "line_total": "900.00"
      }
    ],
    "subtotal": "900.00"
  }
}
```

**Response** `404 Not Found` — cart expired or does not exist.

---

### PUT /api/v1/store/{slug}/cart/{sessionToken}/items

Add or update a product in the cart. If the product already exists in cart, quantity is replaced (not incremented).

**Request**
```json
{
  "listed_product_id": "LP-X1Y2Z3",
  "quantity": 3
}
```

**Validation**:
- `quantity` ≥ 1
- `quantity` ≤ `stock_quantity` of the listed product (capped server-side; error returned if requested qty exceeds stock)

**Response** `200 OK` — returns updated cart (same shape as GET cart)

**Response** `422 Unprocessable Entity`
```json
{
  "success": false,
  "error": { "code": "INSUFFICIENT_STOCK", "message": "Only 2 units available", "available_quantity": 2 }
}
```

**Response** `409 Conflict`
```json
{
  "success": false,
  "error": { "code": "PRODUCT_UNAVAILABLE", "message": "Product is out of stock or no longer listed" }
}
```

---

### DELETE /api/v1/store/{slug}/cart/{sessionToken}/items/{itemId}

Remove an item from the cart.

**Response** `200 OK` — returns updated cart.

---

### DELETE /api/v1/store/{slug}/cart/{sessionToken}

Clear all items from the cart (does not delete cart).

**Response** `200 OK` — returns empty cart.

---

## Checkout

### POST /api/v1/store/{slug}/cart/{sessionToken}/checkout

Confirm an order. **Requires authentication** (`Authorization: Bearer <JWT>`). The JWT must belong to an `END_CUSTOMER` user.

On success, the guest cart is converted, the session token is invalidated, and an `EcomOrderPlaced` Kafka event is published.

**Request**
```json
{
  "delivery_address_id": "ADDR-J1K2L3",
  "notes": "Please leave at gate"
}
```
_Or inline address (when customer has no saved addresses):_
```json
{
  "delivery_address": {
    "address_line1": "42 MG Road",
    "address_line2": "Apt 5B",
    "city": "Bangalore",
    "state": "Karnataka",
    "pin_code": "560001",
    "country": "IN",
    "phone": "9876543210"
  },
  "save_address": true,
  "notes": null
}
```

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "ecom_order_ref": "ECO-A1B2C3D4",
    "status": "PLACED",
    "storefront_id": "SF-A1B2C3D4",
    "line_items": [
      {
        "product_name": "Basmati Rice 5kg",
        "unit_price": "450.00",
        "quantity_ordered": 2,
        "line_total": "900.00",
        "status": "ORDERED"
      }
    ],
    "subtotal": "900.00",
    "total_amount": "900.00",
    "delivery_address": { "address_line1": "42 MG Road", "city": "Bangalore", ... },
    "placed_at": "2026-05-30T10:15:00Z"
  }
}
```

**Response** `400 Bad Request` — empty cart.

**Response** `401 Unauthorized` — no valid END_CUSTOMER JWT.

---

## Customer Account

All endpoints below require `Authorization: Bearer <JWT>` with `user_type = END_CUSTOMER`.

### GET /api/v1/ecom/account/addresses

List saved delivery addresses.

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "uid": "ADDR-J1K2L3",
      "label": "Home",
      "address_line1": "42 MG Road",
      "address_line2": "Apt 5B",
      "city": "Bangalore",
      "state": "Karnataka",
      "pin_code": "560001",
      "country": "IN",
      "phone": "9876543210",
      "is_default": true
    }
  ]
}
```

---

### POST /api/v1/ecom/account/addresses

Add a new saved address.

**Request**
```json
{
  "label": "Office",
  "address_line1": "100 Brigade Road",
  "city": "Bangalore",
  "state": "Karnataka",
  "pin_code": "560025",
  "country": "IN",
  "is_default": false
}
```

**Response** `201 Created` — returns created address.

---

### PUT /api/v1/ecom/account/addresses/{addressId}

Update an existing address.

**Response** `200 OK` — returns updated address.

---

### DELETE /api/v1/ecom/account/addresses/{addressId}

Delete a saved address.

**Response** `204 No Content`

---

### GET /api/v1/ecom/account/orders

Customer's order history. Scoped to a specific storefront.

**Query params**:
- `storefront_id` (string, required)
- `page`, `size` (pagination)

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "ecom_order_ref": "ECO-A1B2C3D4",
        "status": "DELIVERED",
        "total_amount": "900.00",
        "placed_at": "2026-05-30T10:15:00Z",
        "line_items_count": 2
      }
    ],
    "page_number": 0,
    "total_elements": 5,
    ...
  }
}
```

---

### GET /api/v1/ecom/account/orders/{ecomOrderRef}

Order detail with line items and current status.

**Response** `200 OK` — returns full `EcomOrderResponse` (same as checkout response).

**Response** `403 Forbidden` — order does not belong to the authenticated customer.

---

## Customer Auth

End customers use the **existing** auth endpoints — no separate ecom auth wrappers:

- **Register**: `POST /api/v1/auth/register` — same endpoint as merchants. Pass `"user_type": "END_CUSTOMER"` (optional field; defaults to `MERCHANT_USER`). When `END_CUSTOMER`, no workspace role is assigned.
- **Login**: `POST /api/v1/auth/login` — identical request shape. The returned JWT includes a `user_type` claim.

The `user_type` claim in the JWT is what checkout (`POST …/checkout`) uses to enforce the `END_CUSTOMER` requirement — no separate login flow needed.
