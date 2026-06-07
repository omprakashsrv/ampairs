# order module

Full order lifecycle — draft → confirmed → delivered. Supports B2B (GST supply chain) and B2C (walk-in).

## Key entities
- `Order` — orderNumber, customerId, isWalkIn, fromCustomerGst, toCustomerGst, placeOfSupply, paymentMethod, status, discount (JSON), taxInfos (JSON), orderItems
- `OrderItem` — productId, quantity, rate, lineTotal, discountAmount, totalTax, serialNumbers (JSON), batchNumbers (JSON)

## Statuses
`DRAFT` → `CONFIRMED` → `PROCESSING` → `SHIPPED` → `DELIVERED` | `CANCELLED`

## Base path
`/order/v1/**`

## Migrations
`V1.0.0`, `V1.0.22` (indexes), `V1.0.77` (unit/variant + price_mode/overall_discount_mode for offline sync)

## Full docs
`docs/modules/order.md`
