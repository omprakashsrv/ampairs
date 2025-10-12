# Quickstart: Unit Module

**Feature**: Separate Unit Module
**Date**: 2025-10-12
**Audience**: Backend developers

## Overview

The unit module provides centralized management of measurement units and unit conversions for the Ampairs platform. This guide shows you how to use the unit module in your code.

## Setup

### 1. Add Dependency

Add the unit module to your module's `build.gradle.kts`:

```kotlin
dependencies {
    api(project(":unit"))
    // ... other dependencies
}
```

### 2. Component Scanning

The unit module is automatically scanned by Spring Boot when included in `ampairs_service`. No additional configuration needed.

---

## Core Concepts

### Unit
A measurement unit like "kg", "meter", "piece", etc.

- **Name**: Full name (e.g., "Kilogram")
- **Short Name**: Symbol (e.g., "kg")
- **Decimal Places**: Precision (0-6)
- **Workspace-Scoped**: Each workspace has its own units

### Unit Conversion
Defines how to convert between two units.

- **Base Unit**: The source unit
- **Derived Unit**: The target unit
- **Multiplier**: Conversion factor (derived = base × multiplier)
- **Product-Specific**: Optionally tied to a specific product

---

## Basic Usage

### Creating a Unit

```kotlin
@Service
class MyService(
    private val unitService: UnitService
) {
    fun setupUnits() {
        // Create a unit
        val kgRequest = UnitRequest(
            name = "Kilogram",
            shortName = "kg",
            decimalPlaces = 3
        )

        val kg: UnitResponse = unitService.create(kgRequest)
        println("Created unit: ${kg.uid}")  // UNIT-001
    }
}
```

### Retrieving Units

```kotlin
// Get by UID
val unit: UnitResponse? = unitService.findByUid("UNIT-001")

// Get by external reference ID
val unit: UnitResponse? = unitService.findByRefId("EXT-123")

// Get all units for current workspace
val allUnits: List<UnitResponse> = unitService.findAll()
```

### Updating a Unit

```kotlin
val updateRequest = UnitRequest(
    id = "UNIT-001",
    name = "Kilogram",
    shortName = "kg",
    decimalPlaces = 2  // Changed from 3 to 2
)

val updated: UnitResponse = unitService.update("UNIT-001", updateRequest)
```

### Deleting a Unit

```kotlin
// Check if unit is in use before deleting
if (!unitService.isUnitInUse("UNIT-001")) {
    unitService.delete("UNIT-001")
} else {
    // Unit is referenced by products, conversions, etc.
    val products = unitService.findProductsUsingUnit("UNIT-001")
    println("Cannot delete: used by ${products.size} products")
}
```

---

## Unit Conversions

### Creating a Global Conversion

```kotlin
@Service
class ConversionSetupService(
    private val unitConversionService: UnitConversionService
) {
    fun setupStandardConversions() {
        // 1 kg = 1000 grams
        val convRequest = UnitConversionRequest(
            baseUnitId = "UNIT-001",      // kg
            derivedUnitId = "UNIT-002",   // gram
            multiplier = 1000.0,
            productId = null  // Global conversion
        )

        val conversion = unitConversionService.create(convRequest)
        println("Conversion: 1 kg = 1000 g (UID: ${conversion.uid})")
    }
}
```

### Creating a Product-Specific Conversion

```kotlin
// For a specific product: 1 box = 12 pieces
val productConvRequest = UnitConversionRequest(
    baseUnitId = "UNIT-010",      // box
    derivedUnitId = "UNIT-011",   // piece
    multiplier = 12.0,
    productId = "PROD-123"  // Product-specific
)

val conversion = unitConversionService.create(productConvRequest)
```

### Converting Quantities

```kotlin
// Convert 2.5 kg to grams
val grams: Double = unitConversionService.convert(
    quantity = 2.5,
    fromUnitId = "UNIT-001",  // kg
    toUnitId = "UNIT-002",    // gram
    productId = null          // Use global conversion
)

println("2.5 kg = $grams grams")  // 2500.0
```

### Querying Conversions

```kotlin
// Get all conversions for a product
val productConversions: List<UnitConversionResponse> =
    unitConversionService.findByProductId("PROD-123")

// Get all conversions for a specific unit
val conversions: List<UnitConversionResponse> =
    unitConversionService.findByBaseOrDerivedUnit("UNIT-001")
```

---

## Integration Patterns

### Using Units in Product Service

```kotlin
@Service
class ProductService(
    private val unitService: UnitService,
    private val productRepository: ProductRepository
) {
    fun createProduct(request: ProductRequest): ProductResponse {
        // Validate unit exists
        val unit = unitService.findByUid(request.unitId)
            ?: throw IllegalArgumentException("Unit ${request.unitId} not found")

        // Create product with unit reference
        val product = Product().apply {
            this.name = request.name
            this.unitId = unit.uid
            // ... other fields
        }

        return productRepository.save(product).asProductResponse()
    }
}
```

### Using Conversions in Inventory Service

```kotlin
@Service
class InventoryService(
    private val unitConversionService: UnitConversionService,
    private val inventoryRepository: InventoryRepository
) {
    fun addStock(
        productId: String,
        quantity: Double,
        unitId: String
    ) {
        // Get product's base unit
        val product = findProduct(productId)

        // Convert quantity to base unit if necessary
        val baseQuantity = if (unitId != product.baseUnitId) {
            unitConversionService.convert(
                quantity = quantity,
                fromUnitId = unitId,
                toUnitId = product.baseUnitId,
                productId = productId
            )
        } else {
            quantity
        }

        // Update inventory in base unit
        inventoryRepository.addStock(productId, baseQuantity)
    }
}
```

### Using Units in Invoice Service

```kotlin
@Service
class InvoiceService(
    private val unitService: UnitService,
    private val invoiceRepository: InvoiceRepository
) {
    fun createInvoiceLineItem(
        productId: String,
        quantity: Double,
        unitId: String
    ): InvoiceLineItem {
        // Fetch unit for display
        val unit = unitService.findByUid(unitId)
            ?: throw IllegalArgumentException("Unit not found")

        return InvoiceLineItem(
            productId = productId,
            quantity = quantity,
            unitId = unit.uid,
            unitName = unit.name,
            unitSymbol = unit.shortName
        )
    }
}
```

---

## REST API Usage

### Create Unit

```bash
curl -X POST https://api.ampairs.com/api/v1/unit \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Workspace-ID: WS-123" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Kilogram",
    "short_name": "kg",
    "decimal_places": 3
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "uid": "UNIT-001",
    "name": "Kilogram",
    "short_name": "kg",
    "decimal_places": 3,
    "active": true,
    "created_at": "2025-10-12T10:00:00Z",
    "updated_at": "2025-10-12T10:00:00Z"
  },
  "timestamp": "2025-10-12T10:00:00Z",
  "trace_id": "abc123"
}
```

### List All Units

```bash
curl -X GET https://api.ampairs.com/api/v1/unit \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Workspace-ID: WS-123"
```

### Create Unit Conversion

```bash
curl -X POST https://api.ampairs.com/api/v1/unit/conversion \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Workspace-ID: WS-123" \
  -H "Content-Type: application/json" \
  -d '{
    "base_unit_id": "UNIT-001",
    "derived_unit_id": "UNIT-002",
    "multiplier": 1000.0
  }'
```

### Convert Quantity

```bash
curl -X POST https://api.ampairs.com/api/v1/unit/conversion/convert \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Workspace-ID: WS-123" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 2.5,
    "from_unit_id": "UNIT-001",
    "to_unit_id": "UNIT-002"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "original_quantity": 2.5,
    "converted_quantity": 2500.0,
    "from_unit": { "uid": "UNIT-001", "name": "Kilogram", "short_name": "kg" },
    "to_unit": { "uid": "UNIT-002", "name": "Gram", "short_name": "g" },
    "multiplier": 1000.0
  }
}
```

---

## Testing

### Unit Tests

```kotlin
@ExtendWith(MockitoExtension::class)
class UnitServiceTest {

    @Mock
    private lateinit var unitRepository: UnitRepository

    @InjectMocks
    private lateinit var unitService: UnitServiceImpl

    @Test
    fun `should create unit successfully`() {
        // Given
        val request = UnitRequest(
            name = "Kilogram",
            shortName = "kg",
            decimalPlaces = 3
        )

        val savedUnit = Unit().apply {
            uid = "UNIT-001"
            name = request.name
            shortName = request.shortName!!
            decimalPlaces = request.decimalPlaces
        }

        whenever(unitRepository.save(any())).thenReturn(savedUnit)

        // When
        val result = unitService.create(request)

        // Then
        assertEquals("UNIT-001", result.uid)
        assertEquals("Kilogram", result.name)
        assertEquals("kg", result.shortName)
        verify(unitRepository).save(any())
    }
}
```

### Integration Tests

```kotlin
@SpringBootTest
@Testcontainers
class UnitControllerIntegrationTest {

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun `should create and retrieve unit via API`() {
        // Given
        val request = UnitRequest(
            name = "Kilogram",
            shortName = "kg",
            decimalPlaces = 3
        )

        // When - Create
        val createResponse = restTemplate
            .withBasicAuth("admin", "password")
            .postForEntity(
                "/api/v1/unit",
                request,
                ApiResponse::class.java
            )

        // Then
        assertEquals(HttpStatus.CREATED, createResponse.statusCode)
        val unitUid = (createResponse.body?.data as Map<*, *>)["uid"] as String

        // When - Retrieve
        val getResponse = restTemplate
            .withBasicAuth("admin", "password")
            .getForEntity(
                "/api/v1/unit/$unitUid",
                ApiResponse::class.java
            )

        // Then
        assertEquals(HttpStatus.OK, getResponse.statusCode)
        val retrievedUnit = getResponse.body?.data as Map<*, *>
        assertEquals("Kilogram", retrievedUnit["name"])
    }
}
```

---

## Common Patterns

### Validation Before Deletion

```kotlin
fun safeDeleteUnit(uid: String) {
    val usage = unitService.isUnitInUse(uid)

    if (usage) {
        val products = unitService.findProductsUsingUnit(uid)
        throw IllegalStateException(
            "Cannot delete unit $uid: used by ${products.size} products"
        )
    }

    unitService.delete(uid)
}
```

### Batch Unit Creation

```kotlin
fun createStandardUnits(): Map<String, String> {
    val units = listOf(
        UnitRequest(name = "Kilogram", shortName = "kg", decimalPlaces = 3),
        UnitRequest(name = "Gram", shortName = "g", decimalPlaces = 2),
        UnitRequest(name = "Meter", shortName = "m", decimalPlaces = 2),
        UnitRequest(name = "Piece", shortName = "pcs", decimalPlaces = 0)
    )

    return units.associate { request ->
        request.name to unitService.create(request).uid
    }
}
```

### Conversion Chain

```kotlin
// Convert through multiple units: kg → g → mg
fun convertThroughChain(quantity: Double): Double {
    val grams = unitConversionService.convert(
        quantity, "UNIT-KG", "UNIT-G", null
    )

    val milligrams = unitConversionService.convert(
        grams, "UNIT-G", "UNIT-MG", null
    )

    return milligrams
}
```

---

## Error Handling

### Common Exceptions

```kotlin
try {
    unitService.findByUid("INVALID-UID")
        ?: throw UnitNotFoundException("Unit not found")
} catch (e: UnitNotFoundException) {
    // Handle: Unit doesn't exist
    log.error("Unit not found", e)
}

try {
    unitService.delete("UNIT-001")
} catch (e: UnitInUseException) {
    // Handle: Unit is referenced by products/conversions
    log.warn("Cannot delete unit in use", e)
}

try {
    unitConversionService.create(invalidRequest)
} catch (e: CircularConversionException) {
    // Handle: Conversion creates circular dependency
    log.error("Circular conversion detected", e)
}
```

### Global Exception Handler

The unit module relies on the global exception handler in the core module. Exceptions are automatically mapped to appropriate HTTP status codes:

- `UnitNotFoundException` → 404 NOT FOUND
- `UnitInUseException` → 409 CONFLICT
- `CircularConversionException` → 400 BAD REQUEST
- `DuplicateUnitException` → 409 CONFLICT

---

## Multi-Tenant Considerations

### Workspace Isolation

Units are automatically isolated by workspace via `@TenantId` on the `ownerId` field.

```kotlin
// User in workspace WS-123 creates unit
TenantContextHolder.setCurrentTenant("WS-123")
val unit = unitService.create(request)

// User in workspace WS-456 cannot see WS-123's unit
TenantContextHolder.setCurrentTenant("WS-456")
val result = unitService.findByUid(unit.uid)  // null
```

### Cross-Workspace Access (Admin Only)

```kotlin
// Use native query to bypass @TenantId filtering (admin operations only)
@Query("SELECT * FROM unit WHERE uid = ?1", nativeQuery = true)
fun findByUidGlobal(uid: String): Unit?
```

---

## Performance Tips

### Use Entity Graphs

Unit conversions load related units lazily. Use entity graphs to prevent N+1 queries:

```kotlin
@EntityGraph("UnitConversion.withUnits")
fun findAllWithUnits(): List<UnitConversion>
```

### Cache Frequently Used Units

Consider caching standard units at application startup:

```kotlin
@Component
class UnitCache(private val unitService: UnitService) {

    private val cache: Map<String, UnitResponse> by lazy {
        unitService.findAll().associateBy { it.uid }
    }

    fun getUnit(uid: String): UnitResponse? = cache[uid]
}
```

---

## Migration Checklist

If you're migrating from the product module's unit code to the new unit module:

- [ ] Update imports: `com.ampairs.product.domain.model.Unit` → `com.ampairs.unit.domain.model.Unit`
- [ ] Add unit module dependency to `build.gradle.kts`
- [ ] Replace direct repository access with service injection
- [ ] Update tests to use unit module classes
- [ ] Verify X-Workspace-ID header is sent in all API calls
- [ ] Run integration tests to ensure no breaking changes

---

## Further Reading

- [API Contracts](./contracts/unit-api.yaml) - Full OpenAPI specification
- [Data Model](./data-model.md) - Detailed entity and DTO documentation
- [Research](./research.md) - Architecture decisions and migration strategy
- [CLAUDE.md](/CLAUDE.md) - Project-wide development guidelines
- [Constitution](/.specify/memory/constitution.md) - Ampairs architectural principles

---

## Support

For questions or issues:
1. Check the [constitution](/.specify/memory/constitution.md) for architectural guidance
2. Review the [tax module](/ampairs-backend/tax) as a reference implementation
3. Consult the backend team for cross-module integration questions
