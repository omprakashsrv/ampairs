# Product Module

## Overview

The Product module provides comprehensive product catalog management with inventory control, tax management, and
hierarchical product categorization. It includes advanced features like multi-unit inventory management, price
variations, image handling, and integration with AWS S3 for media storage.

## Architecture

### Package Structure

```
com.ampairs.product/
├── config/                    # Configuration constants
├── controller/                # REST API endpoints
├── domain/                    # Product domain layer
│   ├── dto/                   # Data Transfer Objects
│   │   ├── group/             # Product categorization DTOs
│   │   ├── product/           # Product-specific DTOs
│   │   ├── tax/               # Tax management DTOs
│   │   └── unit/              # Unit conversion DTOs
│   ├── enums/                 # Product enumerations
│   └── model/                 # Product entities
│       └── group/             # Product categorization models
├── exception/                 # Product exception handling
├── repository/                # Data access layer
└── service/                   # Product business logic

com.ampairs.inventory/
├── config/                    # Inventory configuration
├── controller/                # Inventory API endpoints
├── domain/                    # Inventory domain layer
│   ├── dto/                   # Inventory DTOs
│   └── model/                 # Inventory entities
├── repository/                # Inventory data access
└── service/                   # Inventory business logic
```

## Key Components

### Controllers

- **`ProductController.kt`** - Product CRUD operations, categorization, and image management
- **`TaxController.kt`** - Tax code and tax information management
- **`InventoryController.kt`** - Stock management and unit conversions

### Product Management

#### Core Product Models

- **`Product.kt`** - Main product entity with pricing, categorization, and metadata
- **`ProductImage.kt`** - Product image management with AWS S3 integration
- **`ProductPrice.kt`** - Multi-tier pricing support (MRP, DP, selling price)

#### Product Categorization Models

- **`ProductGroup.kt`** - Top-level product grouping
- **`ProductBrand.kt`** - Brand management and association
- **`ProductCategory.kt`** - Product categories within groups
- **`ProductSubCategory.kt`** - Detailed subcategory classification

### Inventory Management

#### Inventory Models

- **`Inventory.kt`** - Stock management with multi-unit support
- **`InventoryTransaction.kt`** - Stock movement tracking and audit trail
- **`InventoryUnitConversion.kt`** - Unit conversion ratios and calculations

#### Unit Management

- **`Unit.kt`** - Base unit definitions
- **`UnitConversion.kt`** - Unit conversion relationships

### Tax Management

#### Tax Models

- **`TaxCode.kt`** - Tax classification codes
- **`TaxInfo.kt`** - Tax rate information and calculations
- **`TaxInfoModel.kt`** - Tax model relationships

### Services

- **`ProductService.kt`** - Core product business logic and operations
- **`TaxService.kt`** - Tax calculation and management logic
- **`InventoryService.kt`** - Inventory operations and stock management

## Key Features

### Comprehensive Product Catalog

- Complete product information management
- SKU generation and management
- Product descriptions and specifications
- Multi-currency pricing support
- Product lifecycle management (active/inactive)

### Hierarchical Product Organization

- Product Groups → Categories → SubCategories
- Brand-based product organization
- Flexible categorization system
- Cross-category product relationships

### Advanced Pricing Models

- Multiple price tiers (MRP, Dealer Price, Selling Price)
- Currency-specific pricing
- Bulk pricing support
- Price history tracking

### Inventory Management

- Real-time stock tracking
- Multi-unit inventory (pieces, boxes, cartons)
- Stock movement audit trail
- Inventory transaction history
- Unit conversion calculations

### Tax Integration

- GST and other tax type support
- Tax code classification
- Automatic tax calculations
- Tax rate management
- Compliance reporting

### Media Management

- Product image upload to AWS S3
- Multiple images per product
- Image optimization and resizing
- CDN integration for fast delivery

## Data Model

### Product Entity Structure

```kotlin
data class Product(
    val name: String,
    val description: String?,
    val sku: String,
    val barcode: String?,
    val brand: ProductBrand?,
    val category: ProductCategory?,
    val subCategory: ProductSubCategory?,
    val group: ProductGroup?,
    val unit: Unit,
    val isActive: Boolean,
    val specifications: JsonNode?,
    val tags: List<String>?
) : OwnableBaseDomain()
```

### Pricing Structure

```kotlin
data class ProductPrice(
    val product: Product,
    val mrp: BigDecimal,
    val dealerPrice: BigDecimal?,
    val sellingPrice: BigDecimal,
    val currency: String,
    val effectiveFrom: LocalDateTime,
    val effectiveTo: LocalDateTime?
) : BaseDomain()
```

### Inventory Structure

```kotlin
data class Inventory(
    val product: Product,
    val location: String?,
    val availableQuantity: BigDecimal,
    val reservedQuantity: BigDecimal,
    val unit: Unit,
    val minimumStock: BigDecimal?,
    val maximumStock: BigDecimal?,
    val reorderPoint: BigDecimal?
) : OwnableBaseDomain()
```

## API Endpoints

### Product Management

```http
GET /product/v1/products
Authorization: Bearer <access-token>
Parameters:
  - page: 0
  - size: 20
  - category: category-id
  - brand: brand-id
  - active: true
```

```http
POST /product/v1/products
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "name": "Premium Widget",
  "description": "High-quality widget for professional use",
  "sku": "PWG-001",
  "barcode": "1234567890123",
  "brandId": "brand-uuid",
  "categoryId": "category-uuid",
  "subCategoryId": "subcategory-uuid",
  "groupId": "group-uuid",
  "unitId": "unit-uuid",
  "isActive": true,
  "specifications": {
    "weight": "1.5 kg",
    "dimensions": "10x15x5 cm",
    "material": "Aluminum"
  },
  "pricing": {
    "mrp": 1500.00,
    "dealerPrice": 1200.00,
    "sellingPrice": 1350.00,
    "currency": "INR"
  }
}
```

```http
PUT /product/v1/products/{productId}
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "name": "Premium Widget Updated",
  "description": "Updated description"
}
```

### Product Images

```http
POST /product/v1/products/{productId}/images
Authorization: Bearer <access-token>
Content-Type: multipart/form-data
- file: [image file]
- description: "Product front view"
```

```http
GET /product/v1/products/{productId}/images
Authorization: Bearer <access-token>
```

### Product Categorization

```http
GET /product/v1/groups
Authorization: Bearer <access-token>
```

```http
POST /product/v1/groups
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "name": "Electronics",
  "description": "Electronic products and components",
  "isActive": true
}
```

```http
GET /product/v1/categories
Authorization: Bearer <access-token>
Parameters:
  - groupId: group-uuid
```

### Inventory Management

```http
GET /inventory/v1/stock
Authorization: Bearer <access-token>
Parameters:
  - productId: product-uuid
  - location: warehouse-location
```

```http
POST /inventory/v1/transactions
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "productId": "product-uuid",
  "transactionType": "STOCK_IN",
  "quantity": 100,
  "unitId": "unit-uuid",
  "location": "WAREHOUSE_A",
  "reference": "PO-2023-001",
  "notes": "Initial stock receipt"
}
```

### Tax Management

```http
GET /tax/v1/codes
Authorization: Bearer <access-token>
```

```http
POST /tax/v1/codes
Authorization: Bearer <access-token>
Content-Type: application/json
{
  "code": "GST18",
  "description": "GST 18%",
  "taxType": "GST",
  "rate": 18.0,
  "isActive": true
}
```

## Product Categorization Hierarchy

### Organization Structure

```
Product Group (Electronics)
├── Product Category (Mobile Phones)
│   ├── Product SubCategory (Smartphones)
│   └── Product SubCategory (Feature Phones)
└── Product Category (Computers)
    ├── Product SubCategory (Laptops)
    └── Product SubCategory (Desktops)
```

### Brand Management

- Cross-category brand support
- Brand-specific product filtering
- Brand hierarchy and relationships
- Brand image and logo management

## Inventory Features

### Stock Management

- Multi-location inventory tracking
- Real-time stock updates
- Reserved stock handling
- Stock alerts and notifications

### Unit Conversions

- Base unit definitions
- Conversion ratios between units
- Automatic quantity calculations
- Support for complex unit relationships

### Transaction Types

- **STOCK_IN** - Inventory receipt
- **STOCK_OUT** - Inventory dispatch
- **ADJUSTMENT** - Stock adjustments
- **TRANSFER** - Location transfers
- **DAMAGE** - Damaged stock write-off

## Tax System

### Tax Types

- **GST** - Goods and Services Tax
- **VAT** - Value Added Tax
- **EXCISE** - Excise Duty
- **CESS** - Additional tax/cess
- **CUSTOM** - Custom duties

### Tax Specifications

```kotlin
enum class TaxSpec {
    INCLUSIVE,  // Tax included in price
    EXCLUSIVE,  // Tax calculated on price
    COMPOUND    // Compound tax calculation
}
```

## Configuration

### Required Properties

```yaml
ampairs:
  product:
    pagination:
      default-page-size: 20
      max-page-size: 100
    images:
      max-size: 10MB
      allowed-types: ["image/jpeg", "image/png", "image/webp"]
      s3-folder: "products/"
    inventory:
      auto-reserve: true
      negative-stock: false
  aws:
    s3:
      bucket: ${S3_BUCKET_NAME}
      region: ${AWS_REGION:us-east-1}
```

## Dependencies

### Core Dependencies

- Spring Boot Starter Web
- Spring Boot Starter Data JPA
- Spring Boot Starter Validation
- AWS SDK for S3
- Jackson (JSON processing)

### Integration Dependencies

- Core Module (Multi-tenancy, File handling, Base entities)
- Workspace Module (Tenant context)
- Hypersistence Utils (JSON support)

## Validation Rules

### Product Validation

- Name: Required, 2-200 characters
- SKU: Unique within tenant
- Barcode: Valid format (optional)
- Price: Positive values only
- Unit: Must exist and be active

### Inventory Validation

- Quantity: Non-negative values
- Unit conversions: Valid conversion ratios
- Location: Valid location identifier
- Transaction references: Proper audit trail

## Error Handling

### Product Errors

- Product not found
- Duplicate SKU
- Invalid category assignments
- Price validation errors
- Image upload failures

### Inventory Errors

- Insufficient stock
- Invalid unit conversions
- Location not found
- Transaction validation errors

## Testing

### Unit Tests

- Product CRUD operations
- Inventory calculations
- Tax computations
- Unit conversions
- Image upload handling

### Integration Tests

- End-to-end product workflows
- Inventory transaction flows
- Tax calculation accuracy
- S3 integration testing
- Multi-tenant data isolation

## Build & Deployment

### Build Commands

```bash
# Build the module
./gradlew :product:build

# Run tests
./gradlew :product:test

# Run specific test suite
./gradlew :product:test --tests ProductServiceTest
```

## Usage Examples

### Service Integration

```kotlin
@Service
class CatalogService(
    private val productService: ProductService,
    private val inventoryService: InventoryService
) {
    
    fun createProductWithInventory(
        productRequest: ProductRequest,
        initialStock: BigDecimal
    ): ProductResponse {
        val product = productService.createProduct(productRequest)
        inventoryService.addStock(product.id, initialStock, "INITIAL_STOCK")
        return product
    }
}
```

### Price Calculation

```kotlin
@Service
class PricingService(
    private val taxService: TaxService
) {
    
    fun calculateTotalPrice(productId: String, quantity: BigDecimal): PriceCalculation {
        val product = productService.getProduct(productId)
        val basePrice = product.sellingPrice * quantity
        val taxAmount = taxService.calculateTax(product.taxCode, basePrice)
        return PriceCalculation(
            basePrice = basePrice,
            taxAmount = taxAmount,
            totalPrice = basePrice + taxAmount
        )
    }
}
```

## Integration

This module integrates with:

- **Core Module**: Multi-tenancy support, file handling, base entities
- **Workspace Module**: Tenant context and workspace isolation
- **Order Module**: Product-order relationships and inventory reservation
- **Invoice Module**: Product billing and tax calculations
- **Tally Module**: Product data synchronization
- **AWS S3**: Product image storage and retrieval

The Product module serves as the comprehensive product catalog foundation for all sales, inventory, and billing
operations within the Ampairs application, providing robust product management with advanced inventory control and tax
compliance features.