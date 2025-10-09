# Order Module

## Overview

The Order module provides comprehensive order processing and management with advanced tax calculations, discount
handling, and customer-to-customer order support. It features a complete order lifecycle management system with status
tracking, address management, and complex pricing calculations.

## Architecture

### Package Structure

```
com.ampairs.order/
├── config/                # Configuration constants
├── controller/            # REST API endpoints
├── domain/                # Order domain layer
│   ├── dto/               # Data Transfer Objects
│   ├── enums/             # Order enumerations
│   └── model/             # Order entities
├── exception/             # Order exception handling
├── repository/            # Data access layer
└── service/               # Order business logic
```

## Key Components

### Controllers

- **`OrderController.kt`** - Main order management endpoints with CRUD operations, search, and status management

### Models

#### Core Entities

- **`Order.kt`** - Primary order entity with customer relationships, addresses, and comprehensive order information
- **`OrderItem.kt`** - Individual order line items with product details, quantities, pricing, and tax calculations

#### DTOs

- **`OrderResponse.kt`** - Complete order information response with calculated totals
- **`OrderUpdateRequest.kt`** - Order modification request with validation
- **`OrderItemRequest.kt`** - Order item creation/update request
- **`OrderItemResponse.kt`** - Order item information with calculations
- **`Discount.kt`** - Discount information and calculations
- **`TaxInfo.kt`** - Tax breakdown and calculations

#### Enumerations

- **`OrderStatus.kt`** - Order lifecycle status management
- **`ItemStatus.kt`** - Individual item status tracking

### Services

- **`OrderService.kt`** - Core order business logic, calculations, and lifecycle management

### Repositories

- **`OrderRepository.kt`** - Standard order data access operations
- **`OrderPagingRepository.kt`** - Pagination and sorting support for order listings
- **`OrderItemRepository.kt`** - Order item data access and management

### Configuration

- **`Constants.kt`** - Order-specific constants and configuration values

## Key Features

### Comprehensive Order Management

- Complete order lifecycle from creation to fulfillment
- Customer-to-customer order support (buyer and seller)
- Multi-address support (billing and shipping addresses)
- Order status workflow management
- Order modification and cancellation

### Advanced Item Management

- Multi-item orders with flexible quantities
- Product-based item creation
- Unit-based quantity management
- Item-level status tracking
- Individual item pricing and discounts

### Complex Pricing & Tax Calculations

- Multi-level discount support (percentage and amount-based)
- Comprehensive tax calculations (item-level and order-level)
- Tax-inclusive and tax-exclusive pricing
- Automatic tax breakdown and reporting
- Currency-specific calculations

### Address Management

- JSON-based billing and shipping addresses
- Address validation and standardization
- Multiple address format support
- Address history and tracking

## Data Model

### Order Entity Structure

```kotlin
data class Order(
    val orderNumber: String,
    val orderDate: LocalDateTime,
    val customerId: String,
    val customerName: String,
    val status: OrderStatus,
    val totalAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val discountAmount: BigDecimal,
    val netAmount: BigDecimal,
    val billingAddress: JsonNode?,
    val shippingAddress: JsonNode?,
    val notes: String?,
    val referenceNumber: String?
) : OwnableBaseDomain()
```

### Order Item Structure

```kotlin
data class OrderItem(
    val order: Order,
    val productId: String,
    val productName: String,
    val productSku: String?,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val netAmount: BigDecimal,
    val status: ItemStatus,
    val notes: String?
) : BaseDomain()
```

### Discount Structure

```json
{
  "type": "PERCENTAGE",
  "value": 10.0,
  "description": "Early bird discount",
  "maxAmount": 1000.0,
  "appliedAmount": 150.0
}
```

### Tax Information Structure

```json
{
  "taxCode": "GST18",
  "taxType": "GST",
  "taxRate": 18.0,
  "taxableAmount": 1000.0,
  "taxAmount": 180.0,
  "isInclusive": false
}
```

## API Endpoints

### Order Management

```http
GET /order/v1/orders
Authorization: Bearer <access-token>
Parameters:
  - page: 0
  - size: 20
  - status: PENDING
  - customerId: customer-uuid
  - fromDate: 2023-01-01
  - toDate: 2023-12-31
```

```http
POST /order/v1/orders
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "customerId": "customer-uuid",
  "orderDate": "2023-06-15T10:30:00",
  "billingAddress": {
    "street": "123 Business Street",
    "city": "Bangalore",
    "state": "Karnataka",
    "pincode": "560001"
  },
  "shippingAddress": {
    "street": "456 Delivery Avenue",
    "city": "Bangalore",
    "state": "Karnataka", 
    "pincode": "560002"
  },
  "items": [
    {
      "productId": "product-uuid",
      "quantity": 2,
      "unitPrice": 1500.00,
      "discount": {
        "type": "PERCENTAGE",
        "value": 5.0
      },
      "taxInfo": {
        "taxCode": "GST18",
        "taxRate": 18.0,
        "isInclusive": false
      }
    }
  ],
  "notes": "Rush delivery required",
  "referenceNumber": "PO-2023-001"
}
```

```http
GET /order/v1/orders/{orderId}
Authorization: Bearer <access-token>
```

```http
PUT /order/v1/orders/{orderId}
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "status": "CONFIRMED",
  "notes": "Order confirmed and ready for processing"
}
```

### Order Status Management

```http
PUT /order/v1/orders/{orderId}/status
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "status": "PROCESSING",
  "statusNotes": "Order moved to processing queue"
}
```

### Order Items Management

```http
GET /order/v1/orders/{orderId}/items
Authorization: Bearer <access-token>
```

```http
POST /order/v1/orders/{orderId}/items
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "productId": "product-uuid",
  "quantity": 1,
  "unitPrice": 2500.00,
  "notes": "Additional item requested by customer"
}
```

```http
PUT /order/v1/orders/{orderId}/items/{itemId}
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "quantity": 3,
  "status": "CONFIRMED"
}
```

```http
DELETE /order/v1/orders/{orderId}/items/{itemId}
Authorization: Bearer <access-token>
```

## Order Status Workflow

### Order Status Flow

```
DRAFT → PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED
   ↓       ↓         ↓           ↓          ↓
CANCELLED ← CANCELLED ← CANCELLED ← CANCELLED ← RETURNED
```

### Status Descriptions

- **DRAFT** - Order being created/modified
- **PENDING** - Awaiting confirmation
- **CONFIRMED** - Order confirmed and accepted
- **PROCESSING** - Order being prepared/manufactured
- **SHIPPED** - Order dispatched for delivery
- **DELIVERED** - Order successfully delivered
- **CANCELLED** - Order cancelled
- **RETURNED** - Order returned by customer

### Item Status Flow

```
PENDING → CONFIRMED → ALLOCATED → PICKED → SHIPPED → DELIVERED
   ↓         ↓          ↓         ↓        ↓
CANCELLED ← CANCELLED ← CANCELLED ← CANCELLED ← RETURNED
```

## Pricing & Tax Calculations

### Discount Types

- **PERCENTAGE** - Percentage-based discount
- **AMOUNT** - Fixed amount discount
- **BUY_X_GET_Y** - Quantity-based offers
- **BULK** - Volume-based discounts

### Tax Calculation Logic

```kotlin
// Tax Exclusive Calculation
val taxableAmount = itemTotal - discountAmount
val taxAmount = taxableAmount * (taxRate / 100)
val finalAmount = taxableAmount + taxAmount

// Tax Inclusive Calculation  
val grossAmount = itemTotal - discountAmount
val taxAmount = grossAmount * (taxRate / (100 + taxRate))
val netAmount = grossAmount - taxAmount
```

### Order Total Calculation

```kotlin
data class OrderCalculation(
    val itemsTotal: BigDecimal,
    val discountTotal: BigDecimal,
    val taxableAmount: BigDecimal,
    val taxTotal: BigDecimal,
    val shippingCharges: BigDecimal,
    val finalTotal: BigDecimal
)
```

## Configuration

### Required Properties

```yaml
ampairs:
  order:
    pagination:
      default-page-size: 20
      max-page-size: 100
    workflow:
      auto-confirm: false
      status-notifications: true
    calculation:
      precision: 2
      rounding-mode: HALF_UP
    validation:
      max-items-per-order: 100
      min-order-amount: 1.0
```

## Validation Rules

### Order Validation

- Customer: Must exist and be active
- Order Date: Cannot be future date (configurable)
- Items: At least one item required
- Addresses: Valid address format
- Total Amount: Must match calculated total

### Item Validation

- Product: Must exist and be active
- Quantity: Must be positive
- Unit Price: Must be positive
- Discount: Cannot exceed item value
- Tax Rate: Must be valid tax rate

## Dependencies

### Core Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Validation
- Jackson (JSON processing)
- BigDecimal (Precise calculations)

### Integration Dependencies

- Core Module (Multi-tenancy, Base entities)
- Customer Module (Customer validation)
- Product Module (Product information)
- Workspace Module (Tenant context)

## Error Handling

### Order Errors

- Order not found
- Invalid order status transition
- Customer validation failure
- Calculation errors
- Item validation failures

### Response Format

```json
{
  "success": false,
  "error": {
    "code": "INVALID_STATUS_TRANSITION",
    "message": "Cannot change order status from DELIVERED to PROCESSING",
    "details": {
      "currentStatus": "DELIVERED",
      "requestedStatus": "PROCESSING"
    },
    "timestamp": "2023-01-01T12:00:00Z"
  }
}
```

## Testing

### Unit Tests

- Order CRUD operations
- Status workflow validation
- Pricing calculations
- Tax calculations
- Item management
- Address validation

### Integration Tests

- End-to-end order workflows
- Customer integration
- Product integration
- Database transaction testing
- Multi-tenant data isolation

## Build & Deployment

### Build Commands

```bash
# Build the module
./gradlew :order:build

# Run tests
./gradlew :order:test

# Run integration tests
./gradlew :order:integrationTest
```

## Usage Examples

### Service Integration

```kotlin
@Service
class SalesService(
    private val orderService: OrderService,
    private val customerService: CustomerService,
    private val inventoryService: InventoryService
) {
    
    @Transactional
    fun createOrderWithInventoryReservation(request: OrderUpdateRequest): OrderResponse {
        // Validate customer
        val customer = customerService.getCustomer(request.customerId)
        
        // Create order
        val order = orderService.createOrder(request)
        
        // Reserve inventory for each item
        order.items.forEach { item ->
            inventoryService.reserveStock(item.productId, item.quantity)
        }
        
        return order
    }
}
```

### Order Calculation Service

```kotlin
@Service
class OrderCalculationService {
    
    fun calculateOrderTotals(order: Order): OrderCalculation {
        val itemsTotal = order.items.sumOf { it.totalPrice }
        val discountTotal = order.items.sumOf { it.discountAmount }
        val taxTotal = order.items.sumOf { it.taxAmount }
        
        return OrderCalculation(
            itemsTotal = itemsTotal,
            discountTotal = discountTotal,
            taxableAmount = itemsTotal - discountTotal,
            taxTotal = taxTotal,
            shippingCharges = calculateShipping(order),
            finalTotal = itemsTotal - discountTotal + taxTotal
        )
    }
}
```

### Status Transition Validation

```kotlin
@Component
class OrderStatusValidator {
    
    private val validTransitions = mapOf(
        OrderStatus.DRAFT to setOf(OrderStatus.PENDING, OrderStatus.CANCELLED),
        OrderStatus.PENDING to setOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
        OrderStatus.CONFIRMED to setOf(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
        OrderStatus.PROCESSING to setOf(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
        OrderStatus.SHIPPED to setOf(OrderStatus.DELIVERED, OrderStatus.RETURNED),
        OrderStatus.DELIVERED to setOf(OrderStatus.RETURNED)
    )
    
    fun isValidTransition(from: OrderStatus, to: OrderStatus): Boolean {
        return validTransitions[from]?.contains(to) ?: false
    }
}
```

## Integration

This module integrates with:

- **Core Module**: Multi-tenancy support, base entities, exception handling
- **Customer Module**: Customer validation and information
- **Product Module**: Product details, pricing, and inventory
- **Workspace Module**: Tenant context and user permissions
- **Invoice Module**: Order-to-invoice conversion
- **Inventory Module**: Stock reservation and allocation

The Order module provides the comprehensive order processing foundation for all sales operations within the Ampairs
application, ensuring accurate order management with robust calculation capabilities and workflow control.