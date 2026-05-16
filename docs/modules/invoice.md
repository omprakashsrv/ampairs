# invoice module

GST-compliant invoice generation, status tracking, and payment state management. Invoices can be created directly or converted from orders.

## REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/invoice/v1` | Create or update invoice |
| GET | `/invoice/v1` | List invoices (with sync timestamp for mobile) |

Invoices can also be created via the order module: `POST /order/v1/create_invoice`

## Key Entities

### Invoice

```kotlin
class Invoice : OwnableBaseDomain() {
    val invoiceNumber: String      // auto-generated sequential
    val orderRefId: String?        // reference to originating order
    val invoiceDate: Instant
    // B2B supply chain
    val fromCustomerId: String?
    val fromCustomerName: String?
    val fromCustomerGst: String?
    val toCustomerId: String?
    val toCustomerName: String?
    val toCustomerGst: String?
    val placeOfSupply: String?     // state code for IGST/CGST+SGST determination
    // Amounts
    val totalCost: BigDecimal
    val basePrice: BigDecimal
    val totalTax: BigDecimal
    // Breakdown (JSON)
    val discount: List<Discount>?
    val taxInfos: List<TaxInfo>?
    // Addresses
    val billingAddress: Address?
    val shippingAddress: Address?
    // Status
    val status: InvoiceStatus
    val totalItems: Int
    val totalQuantity: BigDecimal
    val invoiceItems: List<InvoiceItem>
}
```

### InvoiceItem

```kotlin
class InvoiceItem : BaseDomain() {
    val invoiceId: String
    val productId: String?
    val productName: String
    val productSku: String?
    val quantity: BigDecimal
    val rate: BigDecimal
    val lineTotal: BigDecimal
    val discountAmount: BigDecimal
    val totalTax: BigDecimal
    val status: ItemStatus
}
```

### InvoiceStatus

| Status | Description |
|--------|-------------|
| `DRAFT` | Not yet sent |
| `SENT` | Sent to customer |
| `VIEWED` | Customer opened it |
| `PARTIALLY_PAID` | Partial payment received |
| `PAID` | Fully paid |
| `OVERDUE` | Past due date unpaid |
| `CANCELLED` | Voided |

### ItemStatus

| Status | Description |
|--------|-------------|
| `PENDING` | Not yet billed |
| `BILLED` | Included in invoice |
| `DELIVERED` | Goods delivered |
| `CANCELLED` | Line item cancelled |

## GST Compliance

- `fromCustomerGst` / `toCustomerGst` — GSTIN of seller and buyer
- `placeOfSupply` — determines IGST (inter-state) vs CGST+SGST (intra-state)
- `taxInfos` JSON stores the component-level breakdown required for GSTR filings

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.11__create_invoice_module_tables.sql` | invoices, invoice_items tables |

## Package Structure

```
com.ampairs.invoice
├── config/
├── controller/     — InvoiceController
├── domain/
│   ├── dto/        — InvoiceResponse, InvoiceUpdateRequest,
│   │                  InvoiceItemRequest/Response, Discount, TaxInfo
│   ├── enums/      — InvoiceStatus, ItemStatus
│   └── model/      — Invoice, InvoiceItem
├── exception/      — InvoiceExceptionHandler
├── repository/     — InvoiceRepository, InvoiceItemRepository, InvoicePagingRepository
└── service/        — InvoiceService
```
