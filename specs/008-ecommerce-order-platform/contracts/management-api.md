# API Contract: Management API (Merchant-Facing)

**Base path**: `/api/v1/ecom/management`  
**Auth**: Required — `Authorization: Bearer <JWT>` for a workspace member with appropriate role.  
**Headers**: `X-Workspace-ID: {workspaceId}` required on all requests.  
**Tenant context**: Set by `SessionUserFilter` from `X-Workspace-ID` header.

---

## Storefront Management

### POST /api/v1/ecom/management/storefront

Create the workspace's storefront. A workspace may have at most one storefront. Created in Draft state.

**Request**
```json
{
  "name": "Green Mart Online",
  "slug": "green-mart",
  "description": "Fresh groceries delivered to your door",
  "logo_url": "https://cdn.ampairs.com/logos/green-mart.png"
}
```

**Validation**:
- `name`: required, 1–100 chars
- `slug`: required, 3–50 chars, URL-safe (`[a-z0-9-]+`), unique globally
- `slug` must match or be derived from the workspace's own slug (anti-squatting rule)

**Response** `201 Created`
```json
{
  "success": true,
  "data": {
    "uid": "SF-A1B2C3D4",
    "name": "Green Mart Online",
    "slug": "green-mart",
    "description": "Fresh groceries delivered to your door",
    "logo_url": "https://cdn.ampairs.com/logos/green-mart.png",
    "status": "DRAFT",
    "created_at": "2026-05-30T08:00:00Z"
  }
}
```

**Response** `409 Conflict` — slug already taken by another workspace.

**Response** `409 Conflict` — workspace already has a storefront.

---

### GET /api/v1/ecom/management/storefront

Get the workspace's current storefront configuration.

**Response** `200 OK` — storefront object (same shape as above).

**Response** `404 Not Found` — storefront not yet created for this workspace.

---

### PUT /api/v1/ecom/management/storefront

Update storefront display configuration (name, description, logo, banner). Slug is immutable.

**Request**
```json
{
  "name": "Green Mart — Fresh & Fast",
  "description": "Updated description",
  "logo_url": "https://cdn.ampairs.com/logos/green-mart-v2.png",
  "banner_url": "https://cdn.ampairs.com/banners/summer-sale.jpg"
}
```

**Response** `200 OK` — updated storefront object.

---

### PUT /api/v1/ecom/management/storefront/publish

Publish the storefront. Makes it publicly accessible. Requires storefront to be in DRAFT or UNPUBLISHED state.

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "uid": "SF-A1B2C3D4",
    "status": "PUBLISHED",
    "published_at": "2026-05-30T09:00:00Z"
  }
}
```

**Response** `409 Conflict` — already published.

---

### PUT /api/v1/ecom/management/storefront/unpublish

Unpublish the storefront. Customers visiting the URL see "store unavailable" response.

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "uid": "SF-A1B2C3D4",
    "status": "UNPUBLISHED",
    "unpublished_at": "2026-05-30T11:00:00Z"
  }
}
```

---

## Product Ecom Listing

These endpoints are added to the existing **product module** controller, not the ecom module.

### PUT /api/v1/products/{productId}/ecom/list

Mark a product as listed on the workspace's storefront. Triggers `EcomCatalogEvent(PRODUCT_LISTED)` via Kafka.

**Request**: Empty body

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "product_id": "PROD-XYZ",
    "is_ecom_listed": true
  }
}
```

**Response** `404 Not Found` — product not found.

**Response** `409 Conflict` — no storefront created for this workspace yet.

---

### PUT /api/v1/products/{productId}/ecom/unlist

Remove product from storefront. Triggers `EcomCatalogEvent(PRODUCT_UNLISTED)`.

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "product_id": "PROD-XYZ",
    "is_ecom_listed": false
  }
}
```

---

## Ecom Order Management (Merchant)

### GET /api/v1/ecom/management/orders

List all ecom orders for this workspace. Supports status filter and pagination.

**Query params**:
- `status` (enum: PLACED | PENDING_MERCHANT_REVIEW | CONFIRMED | PROCESSING | DISPATCHED | DELIVERED | CANCELLED, optional)
- `page`, `size` (pagination)

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "ecom_order_ref": "ECO-A1B2C3D4",
        "customer_name": "Priya Sharma",
        "customer_phone": "9876543210",
        "status": "PENDING_MERCHANT_REVIEW",
        "total_amount": "900.00",
        "line_items_count": 2,
        "placed_at": "2026-05-30T10:15:00Z",
        "management_order_ref": null
      }
    ],
    "page_number": 0,
    "total_elements": 12,
    ...
  }
}
```

---

### GET /api/v1/ecom/management/orders/{ecomOrderRef}

Get full ecom order detail including line items.

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "ecom_order_ref": "ECO-A1B2C3D4",
    "status": "PENDING_MERCHANT_REVIEW",
    "customer_name": "Priya Sharma",
    "customer_email": "priya@example.com",
    "customer_phone": "9876543210",
    "delivery_address": { ... },
    "notes": null,
    "placed_at": "2026-05-30T10:15:00Z",
    "management_order_ref": null,
    "line_items": [
      {
        "uid": "ECOLI-X1",
        "product_name": "Basmati Rice 5kg",
        "management_product_id": "PROD-XYZ",
        "unit_price": "450.00",
        "quantity_ordered": 2,
        "quantity_confirmed": null,
        "line_total": "900.00",
        "status": "ORDERED"
      }
    ],
    "subtotal": "900.00",
    "total_amount": "900.00"
  }
}
```

---

### PUT /api/v1/ecom/management/orders/{ecomOrderRef}/line-items

Edit line items of an order in `PENDING_MERCHANT_REVIEW` state. Merchant adjusts quantities or cancels lines.

**Request**
```json
{
  "line_items": [
    {
      "uid": "ECOLI-X1",
      "quantity_confirmed": 1,
      "status": "CONFIRMED"
    },
    {
      "uid": "ECOLI-X2",
      "quantity_confirmed": 0,
      "status": "CANCELLED"
    }
  ]
}
```

**Validation**:
- Order must be in `PENDING_MERCHANT_REVIEW`
- At least one line item must remain `CONFIRMED`

**Response** `200 OK` — returns updated order.

---

### POST /api/v1/ecom/management/orders/{ecomOrderRef}/confirm

Merchant confirms the (possibly edited) order. Triggers `EcomOrderStatusEvent(CONFIRMED)` via Kafka.

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "ecom_order_ref": "ECO-A1B2C3D4",
    "status": "CONFIRMED",
    "confirmed_at": "2026-05-30T10:45:00Z"
  }
}
```

**Response** `409 Conflict` — order not in PENDING_MERCHANT_REVIEW state.

---

### PUT /api/v1/ecom/management/orders/{ecomOrderRef}/status

Update order status from management (merchant advances the fulfilment state).

**Request**
```json
{
  "status": "DISPATCHED"
}
```

**Allowed transitions**: CONFIRMED → PROCESSING → DISPATCHED → DELIVERED

**Response** `200 OK` — returns updated order status.
