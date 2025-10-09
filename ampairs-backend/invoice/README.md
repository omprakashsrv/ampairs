# Invoice Module

## Overview

The Invoice module provides comprehensive invoice generation and management with GST compliance, tax calculations, and
order-to-invoice conversion capabilities. It features complete billing functionality with multi-format address support,
status-based workflow, and integration with order processing systems.

## Architecture

### Package Structure

```
com.ampairs.invoice/
├── config/                # Configuration constants
├── controller/            # REST API endpoints
├── domain/                # Invoice domain layer
│   ├── dto/               # Data Transfer Objects
│   ├── enums/             # Invoice enumerations
│   └── model/             # Invoice entities
├── exception/             # Invoice exception handling
├── repository/            # Data access layer
└── service/               # Invoice business logic
```

## Key Components

### Controllers

- **`InvoiceController.kt`** - Main invoice management endpoints with CRUD operations, PDF generation, and status
  management

### Models

#### Core Entities

- **`Invoice.kt`** - Primary invoice entity with customer billing information, addresses, and comprehensive invoice
  details
- **`InvoiceItem.kt`** - Individual invoice line items with product details, quantities, pricing, and tax calculations

#### DTOs

- **`InvoiceResponse.kt`** - Complete invoice information response with calculated totals and compliance data
- **`InvoiceUpdateRequest.kt`** - Invoice modification request with validation
- **`InvoiceItemRequest.kt`** - Invoice item creation/update request
- **`InvoiceItemResponse.kt`** - Invoice item information with tax breakdowns
- **`Discount.kt`** - Discount information and calculations
- **`TaxInfo.kt`** - Tax breakdown and GST compliance data

#### Enumerations

- **`InvoiceStatus.kt`** - Invoice lifecycle status management
- **`ItemStatus.kt`** - Individual item status tracking

### Services

- **`InvoiceService.kt`** - Core invoice business logic, tax calculations, GST compliance, and PDF generation

### Repositories

- **`InvoiceRepository.kt`** - Standard invoice data access operations
- **`InvoicePagingRepository.kt`** - Pagination and sorting support for invoice listings
- **`InvoiceItemRepository.kt`** - Invoice item data access and management

### Configuration

- **`Constants.kt`** - Invoice-specific constants and configuration values

## Key Features

### Comprehensive Invoice Management

- Complete invoice lifecycle from draft to paid
- GST-compliant invoice generation
- Multi-currency invoice support
- Invoice numbering and series management
- Invoice modification and cancellation

### Order Integration

- Seamless order-to-invoice conversion
- Order item mapping to invoice items
- Order status synchronization
- Batch invoice creation from multiple orders

### Tax Compliance & GST

- GST-compliant invoice format
- Automatic tax calculations (CGST, SGST, IGST)
- Tax exemption handling
- HSN/SAC code integration
- Tax summary and reporting

### Advanced Address & Customer Management

- JSON-based billing and shipping addresses
- Customer GST validation
- Multi-format address support
- Business-to-business (B2B) and business-to-consumer (B2C) invoicing

### Invoice Generation & Export

- PDF invoice generation
- Multiple invoice templates
- Custom branding and letterheads
- Email delivery integration
- Bulk invoice operations

## Data Model

### Invoice Entity Structure

```kotlin
data class Invoice(
    val invoiceNumber: String,
    val invoiceDate: LocalDateTime,
    val dueDate: LocalDateTime?,
    val customerId: String,
    val customerName: String,
    val customerGstNumber: String?,
    val orderId: String?,
    val status: InvoiceStatus,
    val subTotal: BigDecimal,
    val discountAmount: BigDecimal,
    val taxAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val paidAmount: BigDecimal,
    val balanceAmount: BigDecimal,
    val billingAddress: JsonNode?,
    val shippingAddress: JsonNode?,
    val notes: String?,
    val terms: String?,
    val paymentTerms: String?
) : OwnableBaseDomain()
```

### Invoice Item Structure

```kotlin
data class InvoiceItem(
    val invoice: Invoice,
    val productId: String,
    val productName: String,
    val productSku: String?,
    val hsnCode: String?,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val taxableAmount: BigDecimal,
    val cgstRate: BigDecimal?,
    val cgstAmount: BigDecimal?,
    val sgstRate: BigDecimal?,
    val sgstAmount: BigDecimal?,
    val igstRate: BigDecimal?,
    val igstAmount: BigDecimal?,
    val totalTaxAmount: BigDecimal,
    val netAmount: BigDecimal,
    val status: ItemStatus
) : BaseDomain()
```

### Tax Information Structure

```json
{
  "taxType": "GST",
  "isInterState": false,
  "cgst": {
    "rate": 9.0,
    "amount": 135.0
  },
  "sgst": {
    "rate": 9.0,
    "amount": 135.0
  },
  "igst": {
    "rate": 0.0,
    "amount": 0.0
  },
  "totalTax": 270.0,
  "taxableAmount": 1500.0
}
```

## API Endpoints

### Invoice Management

```http
GET /invoice/v1/invoices
Authorization: Bearer <access-token>
Parameters:
  - page: 0
  - size: 20
  - status: DRAFT
  - customerId: customer-uuid
  - fromDate: 2023-01-01
  - toDate: 2023-12-31
  - invoiceNumber: INV-2023-001
```

```http
POST /invoice/v1/invoices
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "customerId": "customer-uuid",
  "invoiceDate": "2023-06-15T10:30:00",
  "dueDate": "2023-07-15T23:59:59",
  "orderId": "order-uuid",
  "billingAddress": {
    "companyName": "ACME Corporation",
    "gstNumber": "29AABCU9603R1ZX",
    "street": "123 Business Street",
    "city": "Bangalore",
    "state": "Karnataka",
    "stateCode": "KA",
    "pincode": "560001"
  },
  "shippingAddress": {
    "street": "456 Delivery Avenue",
    "city": "Bangalore",
    "state": "Karnataka",
    "stateCode": "KA",
    "pincode": "560002"
  },
  "items": [
    {
      "productId": "product-uuid",
      "quantity": 2,
      "unitPrice": 1500.00,
      "hsnCode": "8471",
      "discount": {
        "type": "PERCENTAGE",
        "value": 5.0
      },
      "taxInfo": {
        "cgstRate": 9.0,
        "sgstRate": 9.0,
        "igstRate": 0.0
      }
    }
  ],
  "paymentTerms": "NET_30",
  "notes": "Thank you for your business!"
}
```

```http
GET /invoice/v1/invoices/{invoiceId}
Authorization: Bearer <access-token>
```

```http
PUT /invoice/v1/invoices/{invoiceId}
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "status": "SENT",
  "notes": "Invoice sent to customer",
  "dueDate": "2023-08-15T23:59:59"
}
```

### Order to Invoice Conversion

```http
POST /invoice/v1/invoices/from-order/{orderId}
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "invoiceDate": "2023-06-15T10:30:00",
  "dueDate": "2023-07-15T23:59:59",
  "paymentTerms": "NET_30",
  "notes": "Invoice generated from order"
}
```

### Invoice PDF Generation

```http
GET /invoice/v1/invoices/{invoiceId}/pdf
Authorization: Bearer <access-token>
Accept: application/pdf
```

```http
POST /invoice/v1/invoices/{invoiceId}/send
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "email": "customer@example.com",
  "subject": "Invoice INV-2023-001",
  "message": "Please find your invoice attached."
}
```

### Payment Recording

```http
POST /invoice/v1/invoices/{invoiceId}/payments
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "amount": 2550.00,
  "paymentDate": "2023-06-20T14:30:00",
  "paymentMethod": "BANK_TRANSFER",
  "reference": "TXN123456789",
  "notes": "Payment received via bank transfer"
}
```

## Invoice Status Workflow

### Invoice Status Flow

```
DRAFT → PENDING → SENT → VIEWED → PAID
   ↓       ↓       ↓      ↓
CANCELLED ← CANCELLED ← OVERDUE ← PARTIALLY_PAID
```

### Status Descriptions

- **DRAFT** - Invoice being created/modified
- **PENDING** - Invoice ready for review
- **SENT** - Invoice sent to customer
- **VIEWED** - Customer has viewed the invoice
- **PAID** - Invoice fully paid
- **PARTIALLY_PAID** - Invoice partially paid
- **OVERDUE** - Invoice past due date
- **CANCELLED** - Invoice cancelled

## GST Compliance Features

### GST Calculation Logic

```kotlin
// Intra-state transaction (CGST + SGST)
if (customerState == companyState) {
    cgstRate = gstRate / 2
    sgstRate = gstRate / 2
    igstRate = 0.0
} else {
    // Inter-state transaction (IGST)
    cgstRate = 0.0
    sgstRate = 0.0
    igstRate = gstRate
}
```

### GST Invoice Format Compliance

- Company GST number and state code
- Customer GST number (for B2B)
- HSN/SAC codes for products
- Tax rate breakdown (CGST/SGST/IGST)
- Place of supply information
- Invoice numbering compliance

### Tax Summary Report

```json
{
  "taxSummary": {
    "cgst": {
      "rate": 9.0,
      "taxableAmount": 2850.0,
      "taxAmount": 256.5
    },
    "sgst": {
      "rate": 9.0,
      "taxableAmount": 2850.0,
      "taxAmount": 256.5
    },
    "totalTax": 513.0,
    "grandTotal": 3363.0
  }
}
```

## Configuration

### Required Properties

```yaml
ampairs:
  invoice:
    pagination:
      default-page-size: 20
      max-page-size: 100
    numbering:
      prefix: "INV"
      series: "2023"
      start-number: 1
      reset-yearly: true
    gst:
      enabled: true
      company-gst: ${COMPANY_GST_NUMBER}
      company-state: ${COMPANY_STATE_CODE}
    pdf:
      template: "default"
      logo-path: "company-logo.png"
      footer-text: "Thank you for your business!"
    email:
      enabled: true
      smtp-host: ${SMTP_HOST}
      smtp-port: ${SMTP_PORT}
      from-email: ${FROM_EMAIL}
```

## Dependencies

### Core Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Validation
- Spring Boot Starter Mail
- iText PDF (PDF generation)
- Jackson (JSON processing)

### Integration Dependencies

- Core Module (Multi-tenancy, Base entities)
- Customer Module (Customer information)
- Product Module (Product details and pricing)
- Order Module (Order-to-invoice conversion)
- Workspace Module (Tenant context)

## Validation Rules

### Invoice Validation

- Customer: Must exist and be active
- Invoice Date: Cannot be future date (configurable)
- Due Date: Must be after invoice date
- Items: At least one item required
- GST Numbers: Valid format when provided
- Total Amount: Must match calculated total

### GST Validation

- Company GST: Must be configured and valid
- Customer GST: Valid format for B2B transactions
- HSN Codes: Must be valid codes for products
- Tax Rates: Must be standard GST rates (5%, 12%, 18%, 28%)

## Error Handling

### Invoice Errors

- Invoice not found
- Invalid status transition
- GST validation failure
- PDF generation errors
- Email delivery failures
- Payment recording errors

### Response Format

```json
{
  "success": false,
  "error": {
    "code": "INVALID_GST_NUMBER",
    "message": "Customer GST number format is invalid",
    "details": {
      "gstNumber": "INVALID123",
      "expectedFormat": "22AAAAA0000A1Z5"
    },
    "timestamp": "2023-01-01T12:00:00Z"
  }
}
```

## Testing

### Unit Tests

- Invoice CRUD operations
- GST calculations
- Status workflow validation
- PDF generation
- Email functionality
- Order-to-invoice conversion

### Integration Tests

- End-to-end invoice workflows
- GST compliance testing
- Payment integration
- Multi-tenant data isolation
- Order synchronization

## Build & Deployment

### Build Commands

```bash
# Build the module
./gradlew :invoice:build

# Run tests
./gradlew :invoice:test

# Run GST compliance tests
./gradlew :invoice:test --tests GstComplianceTest
```

## Usage Examples

### Service Integration

```kotlin
@Service
class BillingService(
    private val invoiceService: InvoiceService,
    private val orderService: OrderService,
    private val emailService: EmailService
) {
    
    @Transactional
    fun createAndSendInvoice(orderId: String): InvoiceResponse {
        // Create invoice from order
        val invoice = invoiceService.createInvoiceFromOrder(orderId)
        
        // Generate PDF
        val pdfBytes = invoiceService.generatePdf(invoice.id)
        
        // Send email with PDF
        emailService.sendInvoiceEmail(invoice, pdfBytes)
        
        // Update status
        return invoiceService.updateStatus(invoice.id, InvoiceStatus.SENT)
    }
}
```

### GST Calculation Service

```kotlin
@Service
class GstCalculationService {
    
    fun calculateGst(
        amount: BigDecimal,
        gstRate: BigDecimal,
        customerState: String,
        companyState: String
    ): GstCalculation {
        
        val isInterState = customerState != companyState
        
        return if (isInterState) {
            GstCalculation(
                cgst = BigDecimal.ZERO,
                sgst = BigDecimal.ZERO,
                igst = amount * gstRate / 100,
                totalTax = amount * gstRate / 100
            )
        } else {
            val halfRate = gstRate / 2
            val cgst = amount * halfRate / 100
            val sgst = amount * halfRate / 100
            
            GstCalculation(
                cgst = cgst,
                sgst = sgst,
                igst = BigDecimal.ZERO,
                totalTax = cgst + sgst
            )
        }
    }
}
```

## Integration

This module integrates with:

- **Core Module**: Multi-tenancy support, base entities, file handling
- **Customer Module**: Customer information and GST details
- **Product Module**: Product details, pricing, and HSN codes
- **Order Module**: Order-to-invoice conversion and synchronization
- **Workspace Module**: Tenant context and company information
- **Email Service**: Invoice delivery and notifications
- **PDF Service**: Invoice document generation

The Invoice module provides the comprehensive billing foundation for all financial operations within the Ampairs
application, ensuring GST compliance and professional invoice management with robust calculation capabilities.