# Kafka Event Contracts

**Purpose**: Cross-module event contracts between management and the ecom module.  
**Serialization**: JSON via Jackson (snake_case per global convention).  
**All `Instant` fields** serialize as ISO-8601 UTC strings.

---

## Topics

| Topic | Direction | Producer | Consumer |
|-------|-----------|----------|----------|
| `ecom-catalog-events` | management → ecom | `product` module | `ecom` module |
| `ecom-order-placed` | ecom → management | `ecom` module | `order` module |
| `ecom-order-status` | management → ecom | `order` module | `ecom` module |

**Partition key**: `workspace_id` (ensures ordering per workspace).

**Consumer groups**:
- `ecom-catalog-consumer` — `ecom` module consuming `ecom-catalog-events`
- `management-ecom-order-consumer` — `order` module consuming `ecom-order-placed`
- `ecom-order-status-consumer` — `ecom` module consuming `ecom-order-status`

**Retry policy**: 3 retries with 500ms backoff. Failed messages routed to dead-letter topic (`{topic}.dlq`) after exhausting retries.

---

## ecom-catalog-events

Published by the `product` module when a product's ecom listing state, price, stock, or details change.

### Message key

```
{workspace_id}
```

### Payload schema

```json
{
  "event_type": "PRODUCT_LISTED",
  "workspace_id": "WS-ABC",
  "storefront_id": "SF-A1B2C3D4",
  "management_product_id": "PROD-XYZ",
  "name": "Basmati Rice 5kg",
  "brand": "India Gate",
  "category": "Grains",
  "subcategory": "Rice",
  "price": "450.00",
  "stock_quantity": 100,
  "image_urls": [
    "https://cdn.ampairs.com/products/basmati-5kg-main.jpg",
    "https://cdn.ampairs.com/products/basmati-5kg-side.jpg"
  ],
  "description": "Premium long-grain basmati rice",
  "published_at": "2026-05-30T08:00:00Z"
}
```

### event_type values and nullable fields

| event_type | Required fields | Nullable fields |
|------------|----------------|-----------------|
| `PRODUCT_LISTED` | All | `description`, `image_urls`, `subcategory` |
| `PRODUCT_UNLISTED` | `workspace_id`, `storefront_id`, `management_product_id` | All others |
| `PRICE_UPDATED` | `workspace_id`, `storefront_id`, `management_product_id`, `price` | All others |
| `STOCK_UPDATED` | `workspace_id`, `storefront_id`, `management_product_id`, `stock_quantity` | All others |
| `DETAILS_UPDATED` | `workspace_id`, `storefront_id`, `management_product_id` | Any changed field (incl. `image_urls`) |

### Consumer behavior (ecom module)

- `PRODUCT_LISTED`: Upsert `EcomListedProduct` — create if not exists, set `is_visible = true`
- `PRODUCT_UNLISTED`: Set `is_visible = false` on the `EcomListedProduct`
- `PRICE_UPDATED`: Update `price` and `last_synced_at`
- `STOCK_UPDATED`: Update `stock_quantity`, recalculate `stock_status`, update `last_synced_at`
- `DETAILS_UPDATED`: Update only the non-null fields in the payload

**Idempotency**: Consumer uses `management_product_id` + `storefront_id` as the natural key. Re-processing the same event is safe (upsert pattern).

---

## ecom-order-placed

Published by the `ecom` module when a customer confirms checkout.

### Message key

```
{workspace_id}
```

### Payload schema

```json
{
  "ecom_order_ref": "ECO-A1B2C3D4",
  "workspace_id": "WS-ABC",
  "storefront_id": "SF-A1B2C3D4",
  "customer_id": "USR-C1D2E3",
  "customer_name": "Priya Sharma",
  "customer_email": "priya@example.com",
  "customer_phone": "9876543210",
  "delivery_address": {
    "street": "42 MG Road",
    "street2": "Apt 5B",
    "address": "",
    "city": "Bangalore",
    "state": "Karnataka",
    "country": "IN",
    "pincode": "560001",
    "phone": "9876543210",
    "attention": ""
  },
  "line_items": [
    {
      "listed_product_id": "LP-X1Y2Z3",
      "management_product_id": "PROD-XYZ",
      "product_name": "Basmati Rice 5kg",
      "unit_price": "450.00",
      "quantity_ordered": 2,
      "line_total": "900.00"
    }
  ],
  "subtotal": "900.00",
  "total_amount": "900.00",
  "placed_at": "2026-05-30T10:15:00Z"
}
```

### Consumer behavior (order module)

1. Set `TenantContextHolder` to `workspace_id`
2. Check stock availability for each line item via inventory
3. **If all items fulfillable**: Create `Order` with `orderType = "ECOM"`, `ecomOrderRef` set, `status = CONFIRMED`; deduct inventory; publish `EcomOrderStatusEvent(CONFIRMED)` 
4. **If partial or no fulfilment**: Create `Order` with `status = PENDING_MERCHANT_REVIEW`, `ecomOrderRef` set; notify merchant; publish `EcomOrderStatusEvent(PENDING_MERCHANT_REVIEW)`
5. Set `management_order_ref` on the ecom order via the status event response

**Idempotency**: Consumer checks if an `Order` with matching `ecomOrderRef` already exists before creating. Duplicate events are skipped.

---

## ecom-order-status

Published by the `order` module when an ecom order's status changes.

### Message key

```
{workspace_id}
```

### Payload schema

```json
{
  "ecom_order_ref": "ECO-A1B2C3D4",
  "workspace_id": "WS-ABC",
  "new_status": "CONFIRMED",
  "management_order_ref": "ORD-M1N2O3",
  "confirmed_line_items": [
    {
      "management_product_id": "PROD-XYZ",
      "quantity_confirmed": 2,
      "status": "CONFIRMED"
    }
  ],
  "updated_at": "2026-05-30T10:45:00Z"
}
```

**`confirmed_line_items`**: Present only on `CONFIRMED` event. `null` on all other status transitions.

### Consumer behavior (ecom module)

1. Find `EcomOrder` by `ecom_order_ref`
2. Update `status` and `management_order_ref`
3. If `confirmed_line_items` is present: update each `EcomOrderLineItem.quantity_confirmed` and `status`
4. Recalculate order `total_amount` based on confirmed quantities

---

## Dead Letter Queue handling

Failed messages (after 3 retries) are written to `{topic}.dlq`:
- `ecom-catalog-events.dlq`
- `ecom-order-placed.dlq`
- `ecom-order-status.dlq`

DLQ messages include the original payload plus error metadata. An operational dashboard or periodic scheduled task should alert on DLQ depth > 0.
