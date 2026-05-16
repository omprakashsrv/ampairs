# order module

Manages the full order lifecycle from draft through delivery. Handles B2B and B2C orders, discounts, tax calculation hooks, and converts orders to invoices.

## REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/order/v1` | Create or update order |
| GET | `/order/v1` | List orders (with sync timestamp for mobile) |
| POST | `/order/v1/create_invoice` | Convert order to invoice |

## Key Entities

### Order

```kotlin
class Order : OwnableBaseDomain() {
    val orderNumber: String        // auto-generated sequential
    val orderType: String          // REGULAR, RETURN, etc.
    // Customer
    val customerId: String?
    val customerName: String?
    val customerPhone: String?
    val isWalkIn: Boolean          // true = no registered customer
    // B2B supply chain
    val fromCustomerId: String?
    val fromCustomerName: String?
    val fromCustomerGst: String?
    val toCustomerId: String?
    val toCustomerName: String?
    val toCustomerGst: String?
    val placeOfSupply: String?     // state code for GST
    // Payment
    val paymentMethod: String?     // CASH, CARD, UPI, CREDIT
    val invoiceRefId: String?
    // Dates
    val orderDate: Instant
    val deliveryDate: Instant?
    // Amounts
    val subtotal: BigDecimal
    val discountAmount: BigDecimal
    val taxAmount: BigDecimal
    val totalAmount: BigDecimal
    val totalCost: BigDecimal
    val basePrice: BigDecimal
    val totalTax: BigDecimal
    // Metadata
    val notes: String?
    val internalNotes: String?
    val status: OrderStatus
    val totalItems: Int
    val totalQuantity: BigDecimal
    // Addresses
    val billingAddress: Address?   // JSON
    val shippingAddress: Address?  // JSON
    // Tax / Discount breakdown
    val discount: List<Discount>?  // JSON
    val taxInfos: List<TaxInfo>?   // JSON
    val attributes: Map<String, Any>?
    val orderItems: List<OrderItem>
}
```

### OrderItem

```kotlin
class OrderItem : BaseDomain() {
    val orderId: String
    val productId: String?
    val productName: String
    val productSku: String?
    val quantity: BigDecimal
    val rate: BigDecimal
    val lineTotal: BigDecimal
    val discountAmount: BigDecimal
    val totalTax: BigDecimal
    val status: ItemStatus
    val serialNumbers: List<String>?   // JSON
    val batchNumbers: List<String>?    // JSON
}
```

### OrderStatus

| Status | Description |
|--------|-------------|
| `DRAFT` | Being assembled |
| `CONFIRMED` | Customer confirmed |
| `PROCESSING` | Being prepared |
| `SHIPPED` | Dispatched |
| `DELIVERED` | Received by customer |
| `CANCELLED` | Cancelled |

### ItemStatus

| Status | Description |
|--------|-------------|
| `PENDING` | Not yet allocated |
| `IN_STOCK` | Stock confirmed |
| `BACKORDERED` | Insufficient stock |
| `SHIPPED` | Dispatched |
| `DELIVERED` | Received |
| `CANCELLED` | Cancelled |

## Tax and Discount Structures

```json
// Discount breakdown (stored as JSON)
[
  { "type": "PERCENTAGE", "description": "Group discount", "percentage": 5.0, "amount": 25.00 },
  { "type": "FLAT", "description": "Promo code", "amount": 50.00 }
]

// Tax breakdown (stored as JSON)
[
  { "taxType": "CGST", "rate": 9.0, "amount": 45.00 },
  { "taxType": "SGST", "rate": 9.0, "amount": 45.00 }
]
```

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.0__create_order_module_tables.sql` | orders, order_items tables |
| `V1.0.22__add_performance_indexes.sql` | Indexes for order queries |

## Package Structure

```
com.ampairs.order
├── config/
├── controller/     — OrderController
├── domain/
│   ├── dto/        — OrderResponse, OrderUpdateRequest, OrderItemRequest/Response,
│   │                  Discount, TaxInfo
│   ├── enums/      — OrderStatus, ItemStatus
│   └── model/      — Order, OrderItem
├── exception/      — OrderExceptionHandler
├── repository/     — OrderRepository, OrderItemRepository, OrderPagingRepository
└── service/        — OrderService
```
