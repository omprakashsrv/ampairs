# invoice module

GST-compliant invoices. Created directly or converted from orders.

## Key entities
- `Invoice` — invoiceNumber, orderRefId, fromCustomerGst, toCustomerGst, placeOfSupply, totalCost, totalTax, status, discount (JSON), taxInfos (JSON), invoiceItems
- `InvoiceItem` — productId, quantity, rate, lineTotal, totalTax

## Statuses
`DRAFT` → `SENT` → `VIEWED` → `PARTIALLY_PAID` → `PAID` | `OVERDUE` | `CANCELLED`

## Note
`placeOfSupply` determines IGST (inter-state) vs CGST+SGST (intra-state).

## Base path
`/invoice/v1/**`

## Migrations
`V1.0.11`

## Full docs
`docs/modules/invoice.md`
