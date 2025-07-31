# Customer Module

## Overview

The Customer module provides comprehensive customer relationship management with advanced address handling, geographic
integration, and multi-tenant customer isolation. It supports complex customer profiles with billing and shipping
addresses, GST information, and pagination for large datasets.

## Architecture

### Package Structure

```
com.ampairs.customer/
├── config/                # Configuration constants
├── controller/            # REST API endpoints
├── domain/                # Customer domain layer
│   ├── dto/               # Data Transfer Objects
│   ├── model/             # Customer entities
│   └── service/           # Customer business logic
├── exception/             # Customer exception handling
└── repository/            # Data access layer
```

## Key Components

### Controllers

- **`CustomerController.kt`** - Main customer management endpoints with CRUD operations and search functionality

### Models

#### Core Entities

- **`Customer.kt`** - Primary customer entity with comprehensive profile information and JSON-based address fields
- **`State.kt`** - Geographic state management for address validation and location services

#### DTOs

- **`CustomerResponse.kt`** - Customer information response with full profile details
- **`CustomerUpdateRequest.kt`** - Customer profile update request with validation
- **`StateResponse.kt`** - Geographic state information response

### Services

- **`CustomerService.kt`** - Core business logic for customer management, address handling, and geographic operations

### Repositories

- **`CustomerRepository.kt`** - Standard customer data access operations
- **`CustomerPagingRepository.kt`** - Pagination and sorting support for large customer datasets
- **`StateRepository.kt`** - Geographic state data access

### Configuration

- **`Constants.kt`** - Customer-specific constants and configuration values

## Key Features

### Comprehensive Customer Profiles

- Complete customer information management
- Contact details with multiple phone numbers and emails
- Business information (GST numbers, business type)
- Customer categorization and classification
- Multi-tenant customer isolation

### Advanced Address Management

- JSON-based billing and shipping addresses
- Geographic validation and standardization
- State and country integration
- Pincode validation and lookup
- Address history and tracking

### Search and Filtering

- Pagination support for large customer lists
- Advanced search capabilities
- Filter by customer attributes
- Geographic-based filtering
- Sort by multiple criteria

### GST and Compliance

- GST number validation and storage
- Tax classification management
- Compliance status tracking
- Business registration details

## Data Model

### Customer Entity Structure

```kotlin
data class Customer(
    val name: String,
    val email: String?,
    val phone: String?,
    val alternatePhone: String?,
    val gstNumber: String?,
    val businessType: String?,
    val customerType: String,
    val billingAddress: JsonNode?,    // JSON structure for flexible address
    val shippingAddress: JsonNode?,   // JSON structure for flexible address
    val isActive: Boolean,
    val notes: String?,
    val creditLimit: BigDecimal?,
    val paymentTerms: String?
) : OwnableBaseDomain()
```

### Address JSON Structure

```json
{
  "street": "123 Business Street",
  "area": "Commercial District",
  "city": "Bangalore",
  "state": "Karnataka",
  "stateCode": "KA",
  "country": "India",
  "countryCode": "IN",
  "pincode": "560001",
  "landmark": "Near Metro Station",
  "addressType": "BILLING"
}
```

### State Entity

```kotlin
data class State(
    val name: String,
    val code: String,
    val countryCode: String,
    val isActive: Boolean
) : BaseDomain()
```

## API Endpoints

### Customer Management

```http
GET /customer/v1/customers
Authorization: Bearer <access-token>
Parameters:
  - page: 0 (default)
  - size: 20 (default)
  - sort: name,asc (default)
  - search: "search term"
```

```http
POST /customer/v1/customers
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "name": "ACME Corporation",
  "email": "contact@acme.com",
  "phone": "+91-9876543210",
  "alternatePhone": "+91-9876543211",
  "gstNumber": "29AABCU9603R1ZX",
  "businessType": "PRIVATE_LIMITED",
  "customerType": "BUSINESS",
  "billingAddress": {
    "street": "123 Business Street",
    "city": "Bangalore",
    "state": "Karnataka",
    "stateCode": "KA",
    "country": "India",
    "pincode": "560001"
  },
  "shippingAddress": {
    "street": "456 Delivery Street",
    "city": "Bangalore",
    "state": "Karnataka",
    "stateCode": "KA",
    "country": "India",
    "pincode": "560002"
  },
  "creditLimit": 100000.00,
  "paymentTerms": "NET_30"
}
```

```http
GET /customer/v1/customers/{customerId}
Authorization: Bearer <access-token>
```

```http
PUT /customer/v1/customers/{customerId}
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "name": "ACME Corp Updated",
  "email": "info@acme.com",
  "creditLimit": 150000.00
}
```

```http
DELETE /customer/v1/customers/{customerId}
Authorization: Bearer <access-token>
```

### Geographic Data

```http
GET /customer/v1/states
Authorization: Bearer <access-token>
Parameters:
  - countryCode: IN (optional)
```

```http
GET /customer/v1/states/{stateCode}
Authorization: Bearer <access-token>
```

### Search and Filtering

```http
GET /customer/v1/customers/search
Authorization: Bearer <access-token>
Parameters:
  - query: "acme"
  - customerType: BUSINESS
  - state: Karnataka
  - active: true
  - page: 0
  - size: 20
```

## Customer Types and Classification

### Customer Types

- **INDIVIDUAL** - Individual customers
- **BUSINESS** - Business customers
- **GOVERNMENT** - Government entities
- **NON_PROFIT** - Non-profit organizations

### Business Types

- **PRIVATE_LIMITED** - Private Limited Company
- **PUBLIC_LIMITED** - Public Limited Company
- **PARTNERSHIP** - Partnership Firm
- **PROPRIETORSHIP** - Sole Proprietorship
- **LLP** - Limited Liability Partnership

## Pagination and Sorting

### Pagination Parameters

- `page`: Page number (0-based)
- `size`: Number of items per page
- `sort`: Sort criteria (field,direction)

### Sortable Fields

- `name` - Customer name
- `email` - Email address
- `phone` - Phone number
- `createdAt` - Creation date
- `updatedAt` - Last update date
- `creditLimit` - Credit limit

### Example Pagination Response

```json
{
  "success": true,
  "data": {
    "content": [
      ...
    ],
    "pageable": {
      "page": 0,
      "size": 20,
      "sort": "name,asc"
    },
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```

## Configuration

### Required Properties

```yaml
ampairs:
  customer:
    pagination:
      default-page-size: 20
      max-page-size: 100
    validation:
      gst:
        enabled: true
        pattern: "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"
      phone:
        enabled: true
        pattern: "^\\+?[1-9]\\d{1,14}$"
```

## Validation Rules

### Customer Validation

- Name: Required, 2-100 characters
- Email: Valid email format (optional)
- Phone: Valid phone number format
- GST Number: Valid GST format (if provided)
- Customer Type: Must be valid enum value

### Address Validation

- City: Required for addresses
- State: Must be valid state code
- Pincode: Valid postal code format
- Country: Must be valid country code

## Dependencies

### Core Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Validation
- Jackson (JSON processing)

### Integration Dependencies

- Core Module (Multi-tenancy, Base entities)
- Workspace Module (Tenant context)
- Hibernate Types (JSON support)

## Error Handling

### Customer Errors

- Customer not found
- Duplicate customer data
- Invalid GST number format
- Invalid address information
- Pagination parameter errors

### Response Format

```json
{
  "success": false,
  "error": {
    "code": "CUSTOMER_NOT_FOUND",
    "message": "Customer with ID 12345 not found",
    "timestamp": "2023-01-01T12:00:00Z"
  }
}
```

## Testing

### Unit Tests

- Customer CRUD operations
- Address validation and formatting
- Pagination functionality
- Search and filtering
- GST validation

### Integration Tests

- End-to-end customer workflows
- Database integration testing
- Multi-tenant data isolation
- Geographic data integration

## Build & Deployment

### Build Commands

```bash
# Build the module
./gradlew :customer:build

# Run tests
./gradlew :customer:test

# Run with specific profile
./gradlew :customer:bootRun --args='--spring.profiles.active=dev'
```

## Usage Examples

### Service Integration

```kotlin
@Service
class SalesService(
    private val customerService: CustomerService
) {

    fun createBusinessCustomer(request: CustomerUpdateRequest): CustomerResponse {
        return customerService.createCustomer(request)
    }

    fun getCustomersByLocation(state: String, page: Int, size: Int): Page<CustomerResponse> {
        return customerService.searchCustomers(state = state, page = page, size = size)
    }
}
```

### Controller Usage

```kotlin
@RestController
@RequestMapping("/api/v1/sales")
class SalesController {

    @GetMapping("/customers")
    @PreAuthorize("hasRole('EMPLOYEE')")
    fun getCustomers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<Page<CustomerResponse>> {
        return ResponseEntity.ok(customerService.getCustomers(page, size, search))
    }
}
```

### Address JSON Handling

```kotlin
@Service
class AddressService {

    fun formatAddress(customer: Customer): String {
        val billingAddress = customer.billingAddress?.let {
            objectMapper.convertValue(it, Address::class.java)
        }
        return buildString {
            billingAddress?.let { addr ->
                appendLine(addr.street)
                appendLine("${addr.city}, ${addr.state} ${addr.pincode}")
                appendLine(addr.country)
            }
        }
    }
}
```

## Integration

This module integrates with:

- **Core Module**: Multi-tenancy support, base entities, file handling
- **Workspace Module**: Tenant context and workspace isolation
- **Order Module**: Customer-order relationships
- **Invoice Module**: Customer billing information
- **Tally Module**: Customer data synchronization

The Customer module provides the customer relationship foundation for all sales and billing operations within the
Ampairs application, ensuring comprehensive customer data management with geographic and compliance support.