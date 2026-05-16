# customer module

CRM module for managing customers, customer groups, customer types, and associated images. Supports GST-aware addressing and GPS location.

## REST Endpoints

### Customers (`/customer/v1`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/customer/v1/create` | Create new customer |
| POST | `/customer/v1` | Create or upsert customer |
| POST | `/customer/v1/customers` | Bulk update customers |
| GET | `/customer/v1` | List with pagination and sync timestamp |
| GET | `/customer/v1/{customerId}` | Get by ID |
| PUT | `/customer/v1/{customerId}` | Update customer |
| DELETE | `/customer/v1/{customerId}` | Soft delete |
| GET | `/customer/v1/gst/{gstNumber}` | Lookup by GST number |
| POST | `/customer/v1/validate-gst` | Validate GST format |
| PUT | `/customer/v1/{customerId}/outstanding` | Update outstanding balance |
| GET | `/customer/v1/states` | Get all Indian states |

### Customer Images (`/customer/v1/{customerId}/images`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/customer/v1/{customerId}/images` | List images |
| GET | `/customer/v1/{customerId}/images/primary` | Get primary image |
| GET | `/customer/v1/{customerId}/images/stats` | Image statistics |
| GET | `/customer/v1/{customerId}/images/{imageUid}` | Get specific image |

### Customer Groups (`/customer/v1/groups`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/customer/v1/groups` | List with pagination |
| GET | `/customer/v1/groups/by-priority` | Sorted by priority |
| GET | `/customer/v1/groups/search` | Search by keyword |
| GET | `/customer/v1/groups/with-discount` | Groups with active discount |
| GET | `/customer/v1/groups/statistics` | Group statistics |
| GET | `/customer/v1/groups/{groupCode}` | Get by code |
| POST | `/customer/v1/groups` | Create group |
| PUT | `/customer/v1/groups/{groupCode}` | Update group |

### Customer Types (`/customer/v1/types`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/customer/v1/types` | List with pagination |
| GET | `/customer/v1/types/search` | Search by keyword |
| GET | `/customer/v1/types/with-credit` | Types with credit facility |
| GET | `/customer/v1/types/statistics` | Type statistics |
| GET | `/customer/v1/types/{typeCode}` | Get by code |
| POST | `/customer/v1/types` | Create type |
| PUT | `/customer/v1/types/{typeCode}` | Update type |

## Key Entities

### Customer

```kotlin
class Customer : OwnableBaseDomain() {
    val name: String
    val phone: String?
    val countryCode: Int           // default 91
    val email: String?
    val landline: String?
    val customerType: String?      // ref to CustomerType.typeCode
    val customerGroup: String?     // ref to CustomerGroup.groupCode
    // GST
    val gstNumber: String?
    val panNumber: String?
    // Credit
    val creditLimit: BigDecimal
    val creditDays: Int
    val outstandingAmount: BigDecimal
    // Address
    val address: String?
    val street: String?
    val street2: String?
    val city: String?
    val state: String?
    val pincode: String?
    val country: String?
    val location: Point?           // GPS coordinates
    val billingAddress: Address?   // JSON
    val shippingAddress: Address?  // JSON
    // Metadata
    val attributes: Map<String, Any>?   // custom JSON fields
    val active: Boolean
}
```

### CustomerGroup

```kotlin
class CustomerGroup : OwnableBaseDomain() {
    val name: String
    val groupCode: String          // unique per workspace
    val description: String?
    val displayOrder: Int
    val priorityLevel: Int
    val defaultDiscountPercentage: BigDecimal?
    val active: Boolean
    val metadata: Map<String, Any>?
}
```

### CustomerType

```kotlin
class CustomerType : OwnableBaseDomain() {
    val name: String
    val typeCode: String           // unique per workspace
    val description: String?
    val displayOrder: Int
    val defaultCreditLimit: BigDecimal
    val defaultCreditDays: Int
    val allowCreditFacility: Boolean
    val active: Boolean
}
```

### CustomerImage

```kotlin
class CustomerImage : OwnableBaseDomain() {
    val customerUid: String
    val storageUrl: String         // S3 object key (full size)
    val thumbnailUrl: String?      // S3 object key (thumbnail)
    val isPrimary: Boolean
    val active: Boolean
    val displayOrder: Int
    val fileExtension: String      // jpg, png, etc.
}
```

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.6__create_customer_module_tables.sql` | customer, customer_group, customer_type, state tables |
| `V1.0.20__add_performance_indexes.sql` | Indexes for common query patterns |

## Package Structure

```
com.ampairs.customer
├── config/
├── controller/     — CustomerController, CustomerGroupController,
│                     CustomerTypeController, CustomerImageController, MasterStateController
├── domain/
│   ├── dto/        — CustomerResponse, CustomerGroupRequest/Response,
│   │                  CustomerTypeRequest/Response, CustomerImageDTOs
│   ├── model/      — Customer, CustomerGroup, CustomerType, CustomerImage,
│   │                  MasterState, State
│   ├── repository/ — CustomerImageRepository
│   └── service/    — CustomerImageService
├── exception/      — CustomerExceptionHandler
├── repository/     — CustomerRepository, CustomerGroupRepository,
│                     CustomerTypeRepository, CustomerPagingRepository,
│                     MasterStateRepository, StateRepository
└── service/        — CustomerService, CustomerGroupService, CustomerTypeService,
                      MasterStateService, MasterStateSeederService,
                      ThumbnailMaintenanceService
```
