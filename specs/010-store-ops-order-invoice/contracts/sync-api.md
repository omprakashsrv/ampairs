# API Contracts: Order & Invoice Offline Sync

**Spec**: `specs/010-store-ops-order-invoice/spec.md` · **Plan**: `plan.md`
New/changed backend endpoints. All return `ApiResponse<T>`; all require `X-Workspace-ID` (tenant at
controller). Pattern mirrors `ProductController.getProductsSync` + batch `POST`. Money fields `Double`.

## 1. Order sync

### `POST /order/v1/orders/sync` — bulk upsert  `[NEW]`
Body: `List<OrderUpdateRequest>` (client UID-keyed; preserves `taxInfos`/`totalTax`/`priceMode`/
`overallDiscountMode` as supplied — **no server recompute**).
Resp: `ApiResponse<List<OrderResponse>>` (per-row upsert result; assigns server `orderNumber` if blank).

### `GET /order/v1/orders/sync?last_sync={ISO-8601}&page={0}&size={100}` — paginated pull  `[NEW]`
Resp: `ApiResponse<PageResponse<OrderResponse>>` with `content`, `totalElements`, `totalPages`,
`hasNext`, `hasPrevious`. Includes soft-deleted/cancelled rows (status carries the signal).

> Existing single `POST /order/v1/orders` and `POST /order/v1/orders/create-invoice` remain.

## 2. Invoice sync

### `POST /invoice/v1/invoices/sync` — bulk upsert  `[NEW]`
Body: `List<InvoiceUpdateRequest>` (carries client `series` + `sequenceNumber`).
- On `(owner_id, series, sequence_number)` collision with a **different** invoice → that row fails with
  a conflict error (HTTP-level per-row error in the response); the server **does not renumber** (C5/FR-B09).
Resp: `ApiResponse<List<InvoiceResponse>>`.

### `GET /invoice/v1/invoices/sync?last_sync=&page=&size=` — paginated pull  `[NEW]`
Resp: `ApiResponse<PageResponse<InvoiceResponse>>` (as order).

## 3. DTO additions

### OrderItemRequest / OrderItemResponse (and Invoice equivalents)
```
+ unit_id: String
+ base_quantity: Double
+ variant_sku: String?          // nullable
// existing: quantity, unit_price, line_total, discount_amount, selling_price,
// product_price, mrp, dp, total_cost, base_price, total_tax, tax_infos[], discount[], tax_code
```

### OrderUpdateRequest / OrderResponse (and Invoice equivalents)
```
+ price_mode: String                 // TAX_EXCLUSIVE | TAX_INCLUSIVE
+ overall_discount_mode: String      // PRE_TAX_APPORTIONED | POST_TAX_REDUCTION
// Invoice only:
+ series: String
+ sequence_number: Long
```

### TaxInfo / Discount (unchanged shape, documented convention)
```
TaxInfo  { id, name, percentage, formatted_name, tax_spec, value }   // tax_spec: INTRA_STATE|INTER_STATE
Discount { percent, value }   // flat => percent=0,value=amount ; percent => percent set,value=resolved
```

## 4. Business settings (read by app)  `[NEW]`
Exposed via the existing business-settings response:
```
+ default_price_mode: String
+ default_overall_discount_mode: String
+ invoice_series_prefix: String      // per device/branch; seeds client numbering series
```

## 5. Validation rules (server)
- `taxInfos` internally consistent (Σ component `value` ≈ `totalTax`; reject malformed).
- `(owner_id, series, sequence_number)` unique for invoices.
- `unit_id` references a known unit; `base_quantity > 0`.
- No tax/total recomputation — client is authority (store as supplied).

## 6. Client sync behavior (app, per `/offline-sync`)
- `OrderSyncDelegate.pushPendingToServer()` → reads `synced=0`, bulk `POST .../sync` in batches of 100,
  marks `synced=1`; returns failure if all fail.
- `pullFromServer()` → page `GET .../sync` with `last_sync` cursor until `hasNext=false`; local unsynced
  edits win; server-cancelled/deleted rows reconciled.
- `dependsOn`: order `[CUSTOMER, PRODUCT]`; invoice `[CUSTOMER, PRODUCT, ORDER]`.
