# Data Model: Store Operations — Order & Invoice with GST

**Spec**: `specs/010-store-ops-order-invoice/spec.md` · **Plan**: `plan.md`
Reuses existing entities; **adds** the fields below. Money stays `Double` (this feature). `[NEW]` =
added by 010; unmarked = existing, reused.

## 1. Backend entities (ampairs)

### Order (`customer_order`, `OwnableBaseDomain`)
Existing: `orderNumber`, `orderType`, `customerId/Name/Phone`, `isWalkIn`, `paymentMethod`,
`invoiceRefId`, `orderDate`, `fromCustomerId/Name/Gst`, `toCustomerId/Name/Gst`, `placeOfSupply`,
`subtotal`, `discountAmount`, `taxAmount`, `totalAmount`, `totalCost`, `basePrice`, `totalTax`,
`status` (DRAFT…CANCELLED), `totalItems`, `totalQuantity`, `billingAddress`, `shippingAddress`,
`discount: List<Discount>`, `taxInfos: List<TaxInfo>`, `attributes`, `ecomOrderRef`, `orderItems`.
- `[NEW] priceMode: String` — `TAX_EXCLUSIVE | TAX_INCLUSIVE` (C1)
- `[NEW] overallDiscountMode: String` — `PRE_TAX_APPORTIONED | POST_TAX_REDUCTION` (C2)

### OrderItem (`order_item`, `OwnableBaseDomain`)
Existing: `orderId`, `description`, `productId`, `taxCode`, `quantity`, `index`, `unitPrice`,
`lineTotal`, `discountAmount`, `sellingPrice`, `productPrice`, `mrp`, `dp`, `totalCost`, `basePrice`,
`totalTax`, `taxInfos: List<TaxInfo>`, `discount: List<Discount>`, `attributes`.
- `[NEW] unitId: String` — transacted unit (FR-B07)
- `[NEW] baseQuantity: Double` — quantity in the product's base unit (FR-B07; inventory reads this)
- `[NEW] variantSku: String?` — selected variant (if product has variants)

### Invoice (`invoice`, `OwnableBaseDomain`)
Existing: `invoiceNumber`, `orderRefId`, `invoiceDate`, `fromCustomer*`, `toCustomer*`,
`placeOfSupply`, `*Gst`, `totalCost`, `basePrice`, `totalTax`, `status` (DRAFT…CANCELLED),
`totalItems`, `totalQuantity`, addresses, `discount`, `taxInfos`, `invoiceItems`.
- `[NEW] priceMode`, `[NEW] overallDiscountMode` (as Order)
- `[NEW] series: String` — invoice numbering series prefix (C5)
- `[NEW] sequenceNumber: Long` — strictly sequential within `series`
- **`[NEW] UNIQUE(owner_id, series, sequence_number)`** — server-enforced (FR-B09)

### InvoiceItem (`invoice_item`) — same `[NEW]` fields as OrderItem.

### Business settings (`business` module)
- `[NEW] defaultPriceMode`, `[NEW] defaultOverallDiscountMode` — seed new documents (C1/C2)
- `[NEW] invoiceSeriesPrefix` per device/branch — seeds the numbering series (C5)

## 2. App Room entities (ampairs-app) — mirror, `synced/last_updated` retained

### OrderItemEntity / InvoiceItemEntity
- `[NEW] unit_id: String`, `[NEW] base_quantity: Double`, `[NEW] variant_sku: String?`

### OrderEntity / InvoiceEntity
- `[NEW] price_mode: String`, `[NEW] overall_discount_mode: String`
- InvoiceEntity: `[NEW] series: String`, `[NEW] sequence_number: Long`
- Room schema version **bump** + Room migration (back-fill as below)

### `[NEW]` InvoiceNumberSeriesEntity (`invoice_number_series`) — local, workspace-scoped
`{ seriesId: String (PK), prefix: String, financialYear: String, lastSequence: Long }`
- DAO: `nextSequence(seriesId)` atomic increment; seeded from business `invoiceSeriesPrefix`.

## 3. Value objects (existing, reused)

- **TaxInfo** `{ id, name, percentage, taxSpec (INTRA|INTER), value, formattedName }` — JSON per line & document.
- **Discount** `{ percent: Double, value: Double }` — JSON list per line & document. Convention (010):
  flat discount → `percent = 0`, `value` = amount; percent discount → `percent` set, `value` = resolved amount. Both forms stored for reproducibility.
- **TaxRule.componentComposition** `Map<scenario, { components: [{name, rate, order}], totalRate }>` — source of rates; `scenario ∈ {INTRA_STATE, INTER_STATE}`.

## 4. Calc-core models (new, app, pure — `feature/tax`)

```
DiscountInput     { kind: PERCENT|FLAT, amount: Double }
ResolvedRate      { components: List<{ name: String, percentage: Double }>, totalRate: Double }
LineCalcInput     { taxCode, unitPrice, quantity, lineDiscount: DiscountInput? }
DocumentCalcInput { lines: List<LineCalcInput>, priceMode, overallDiscount: DiscountInput?,
                    overallDiscountMode, scenario: INTRA|INTER, rates: Map<taxCode, ResolvedRate> }
LineCalcResult    { taxable, taxInfos: List<TaxInfo>, totalTax, lineTotal, appliedDiscountValue }
DocumentCalcResult{ lines: List<LineCalcResult>, basePrice, taxInfos, totalTax, totalAmount }
```

## 5. Migrations (back-fill = preserve legacy behavior)

| Module | Version | Change | Back-fill |
|---|---|---|---|
| order | V1.0.23 | `order_item`: +`unit_id`,+`base_quantity`,+`variant_sku`; `customer_order`: +`price_mode`,+`overall_discount_mode` | `base_quantity=quantity`; `unit_id`=product base unit; `price_mode=TAX_EXCLUSIVE`; `overall_discount_mode=POST_TAX_REDUCTION` |
| invoice | V1.0.12 | same item/doc cols; `invoice`: +`series`,+`sequence_number` + UNIQUE(owner,series,seq) | items as above; existing invoices → `series='DEFAULT'`, `sequence_number`=existing `invoiceNumber` numeric or row order |
| business | (next) | default price/discount mode + invoice series prefix settings | defaults as above; one series prefix per existing device/branch |

Both `mysql/` and `postgresql/`. Confirm next numbers with `./gradlew :ampairs_service:flywayInfo`.

## 6. SyncEntity wiring (app)

`SyncEntity.ORDER`, `SyncEntity.INVOICE` already exist. `OrderSyncDelegate` `dependsOn=[CUSTOMER,
PRODUCT]`; `InvoiceSyncDelegate` `dependsOn=[CUSTOMER, PRODUCT, ORDER]`. Inline-created products push
under `SyncEntity.PRODUCT` first (existing `ProductSyncDelegate`).
