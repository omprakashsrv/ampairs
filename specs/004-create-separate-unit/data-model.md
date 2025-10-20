# Data Model: Unit Module

**Feature**: Separate Unit Module
**Date**: 2025-10-12
**Phase**: 1 - Design & Contracts

## Overview

This document defines the data model for the unit module, including entities, DTOs, and their relationships. The model supports measurement units and unit conversions for multi-unit product management across the Ampairs ecosystem.

## Entities

### Unit Entity

**Purpose**: Represents a measurement unit (e.g., kg, meter, piece, box) used throughout the system for products, inventory, and invoices.

**Table**: `unit`

**Package**: `com.ampairs.unit.domain.model`

**Inheritance**: Extends `OwnableBaseDomain`
- Inherits: uid, refId, ownerId (@TenantId), workspaceId, active, createdAt, updatedAt, createdBy, updatedBy

**Fields**:

| Field | Type | Column | Constraints | Description |
|-------|------|--------|-------------|-------------|
| name | String | name | NOT NULL, length=10 | Full name of the unit (e.g., "Kilogram") |
| shortName | String | short_name | NOT NULL, length=10 | Short symbol (e.g., "kg") |
| decimalPlaces | Int | decimal_places | NOT NULL, default=2 | Precision for quantities (0-6) |

**Indexes**:
- `unit_idx` on `name`
- `idx_unit_uid` on `uid` (unique)

**Unique Constraints**: None beyond inherited uid

**Entity Graph**:
```kotlin
@NamedEntityGraph(
    name = "Unit.basic",
    attributeNodes = []  // No relationships in basic unit
)
```

**Example**:
```json
{
  "uid": "UNIT-001",
  "name": "Kilogram",
  "short_name": "kg",
  "decimal_places": 3,
  "owner_id": "WS-123",
  "workspace_id": "WS-123",
  "active": true,
  "created_at": "2025-10-12T10:00:00Z"
}
```

---

### UnitConversion Entity

**Purpose**: Defines conversion relationships between two units, optionally scoped to a specific product. Supports both global conversions (e.g., 1 kg = 1000 g) and product-specific conversions (e.g., 1 box of Product A = 12 pieces).

**Table**: `unit_conversion`

**Package**: `com.ampairs.unit.domain.model`

**Inheritance**: Extends `OwnableBaseDomain`

**Fields**:

| Field | Type | Column | Constraints | Description |
|-------|------|--------|-------------|-------------|
| baseUnitId | String | base_unit_id | NOT NULL, length=200 | UID of the base unit |
| derivedUnitId | String | derived_unit_id | NOT NULL, length=200 | UID of the derived unit |
| productId | String | product_id | length=200 | Optional: Product UID if conversion is product-specific |
| multiplier | Double | multiplier | NOT NULL, default=1.0 | Conversion factor: derived = base × multiplier |
| baseUnit | Unit | - | @OneToOne (lazy) | Reference to base unit entity |
| derivedUnit | Unit | - | @OneToOne (lazy) | Reference to derived unit entity |
| product | Product? | - | @OneToOne (lazy, nullable) | Optional reference to product |

**Relationships**:
```kotlin
@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "base_unit_id", referencedColumnName = "uid", insertable = false, updatable = false)
lateinit var baseUnit: Unit

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "derived_unit_id", referencedColumnName = "uid", insertable = false, updatable = false)
lateinit var derivedUnit: Unit

@OneToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "product_id", referencedColumnName = "uid", insertable = false, updatable = false)
var product: Product? = null
```

**Entity Graph**:
```kotlin
@NamedEntityGraph(
    name = "UnitConversion.withUnits",
    attributeNodes = [
        NamedAttributeNode("baseUnit"),
        NamedAttributeNode("derivedUnit")
    ]
)
```

**Validation Rules**:
- baseUnitId ≠ derivedUnitId (prevent self-conversion)
- multiplier > 0 (must be positive)
- If productId is null, conversion is global
- No circular conversions allowed (A→B→C→A)

**Example**:
```json
{
  "uid": "CONV-001",
  "base_unit_id": "UNIT-001",  // kg
  "derived_unit_id": "UNIT-002",  // gram
  "product_id": null,  // Global conversion
  "multiplier": 1000.0,  // 1 kg = 1000 g
  "owner_id": "WS-123",
  "workspace_id": "WS-123",
  "active": true,
  "created_at": "2025-10-12T10:00:00Z"
}
```

---

## DTOs (Request/Response)

### UnitRequest

**Purpose**: Client input for creating or updating a unit

**Package**: `com.ampairs.unit.domain.dto`

**Fields**:

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| id | String | Optional on create, required on update | UID for the unit |
| name | String | @NotBlank, @Size(max=10) | Full name of the unit |
| shortName | String? | @Size(max=10) | Short symbol (defaults to name if null) |
| decimalPlaces | Int | @Min(0), @Max(6) | Precision for quantities |
| refId | String? | Optional | External reference ID |

**Example**:
```json
{
  "name": "Kilogram",
  "short_name": "kg",
  "decimal_places": 3
}
```

**Converter Function**:
```kotlin
fun UnitRequest.toEntity(): Unit = Unit().apply {
    this.uid = this@toEntity.id ?: UUID.randomUUID().toString()
    this.name = this@toEntity.name
    this.shortName = this@toEntity.shortName ?: this@toEntity.name
    this.decimalPlaces = this@toEntity.decimalPlaces
    this.refId = this@toEntity.refId
}
```

---

### UnitResponse

**Purpose**: API output for unit data (never exposes JPA entity directly)

**Package**: `com.ampairs.unit.domain.dto`

**Fields**:

| Field | Type | Description |
|-------|------|-------------|
| uid | String | Unique identifier |
| name | String | Full name |
| shortName | String | Short symbol |
| decimalPlaces | Int | Precision |
| refId | String? | External reference |
| active | Boolean | Soft delete flag |
| createdAt | Instant | Creation timestamp (UTC) |
| updatedAt | Instant | Last update timestamp (UTC) |

**Example**:
```json
{
  "uid": "UNIT-001",
  "name": "Kilogram",
  "short_name": "kg",
  "decimal_places": 3,
  "ref_id": null,
  "active": true,
  "created_at": "2025-10-12T10:00:00Z",
  "updated_at": "2025-10-12T10:00:00Z"
}
```

**Converter Function**:
```kotlin
fun Unit.asUnitResponse(): UnitResponse = UnitResponse(
    uid = this.uid,
    name = this.name,
    shortName = this.shortName,
    decimalPlaces = this.decimalPlaces,
    refId = this.refId,
    active = this.active,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)

fun List<Unit>.asUnitResponses(): List<UnitResponse> = map { it.asUnitResponse() }
```

---

### UnitConversionRequest

**Purpose**: Client input for creating or updating a unit conversion

**Package**: `com.ampairs.unit.domain.dto`

**Fields**:

| Field | Type | Validation | Description |
|-------|------|------------|-------------|
| id | String | Optional on create, required on update | UID for the conversion |
| baseUnitId | String | @NotBlank | UID of base unit |
| derivedUnitId | String | @NotBlank | UID of derived unit |
| productId | String? | Optional | Product UID if product-specific |
| multiplier | Double | @Positive, @NotNull | Conversion multiplier |
| refId | String? | Optional | External reference ID |

**Validation**:
```kotlin
init {
    require(baseUnitId != derivedUnitId) { "Base and derived units must be different" }
    require(multiplier > 0) { "Multiplier must be positive" }
}
```

**Example**:
```json
{
  "base_unit_id": "UNIT-001",
  "derived_unit_id": "UNIT-002",
  "product_id": null,
  "multiplier": 1000.0
}
```

---

### UnitConversionResponse

**Purpose**: API output for unit conversion data

**Package**: `com.ampairs.unit.domain.dto`

**Fields**:

| Field | Type | Description |
|-------|------|-------------|
| uid | String | Unique identifier |
| baseUnitId | String | Base unit UID |
| derivedUnitId | String | Derived unit UID |
| productId | String? | Optional product UID |
| multiplier | Double | Conversion multiplier |
| baseUnit | UnitResponse | Nested base unit details |
| derivedUnit | UnitResponse | Nested derived unit details |
| refId | String? | External reference |
| active | Boolean | Soft delete flag |
| createdAt | Instant | Creation timestamp (UTC) |
| updatedAt | Instant | Last update timestamp (UTC) |

**Example**:
```json
{
  "uid": "CONV-001",
  "base_unit_id": "UNIT-001",
  "derived_unit_id": "UNIT-002",
  "product_id": null,
  "multiplier": 1000.0,
  "base_unit": {
    "uid": "UNIT-001",
    "name": "Kilogram",
    "short_name": "kg",
    ...
  },
  "derived_unit": {
    "uid": "UNIT-002",
    "name": "Gram",
    "short_name": "g",
    ...
  },
  "active": true,
  "created_at": "2025-10-12T10:00:00Z",
  "updated_at": "2025-10-12T10:00:00Z"
}
```

**Converter Function**:
```kotlin
fun UnitConversion.asUnitConversionResponse(): UnitConversionResponse = UnitConversionResponse(
    uid = this.uid,
    baseUnitId = this.baseUnitId,
    derivedUnitId = this.derivedUnitId,
    productId = this.productId,
    multiplier = this.multiplier,
    baseUnit = this.baseUnit.asUnitResponse(),
    derivedUnit = this.derivedUnit.asUnitResponse(),
    refId = this.refId,
    active = this.active,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt
)
```

---

## Entity Relationships

```
┌─────────────────────────────────────────────────────────────┐
│                    OwnableBaseDomain                        │
│  (uid, refId, ownerId @TenantId, workspaceId, active,      │
│   createdAt, updatedAt, createdBy, updatedBy)              │
└─────────────────────────────────────────────────────────────┘
                         ▲           ▲
                         │           │
            ┌────────────┴───┐   ┌──┴──────────────┐
            │                │   │                  │
       ┌────┴────┐     ┌─────┴────────┐      ┌─────┴─────┐
       │  Unit   │     │ UnitConversion│      │  Product  │
       └─────────┘     └──────────────┘      └───────────┘
            ▲                 │    │                │
            │                 │    │                │
            │         ┌───────┘    └────────┐       │
            │         │                     │       │
            └─────────┤ baseUnit            │       │
                      │                     │       │
                      │ derivedUnit  ───────┘       │
                      │                             │
                      │ product (optional) ─────────┘
```

**Cardinality**:
- Unit → UnitConversion: One-to-Many (one unit can be base/derived in multiple conversions)
- UnitConversion → Unit: Many-to-One (each conversion references exactly one base and one derived unit)
- UnitConversion → Product: Many-to-One (optional, for product-specific conversions)

**Multi-Tenancy**:
- All entities extend OwnableBaseDomain
- `ownerId` field has @TenantId annotation
- Automatic filtering by workspace context
- Cross-tenant queries require native SQL

---

## Database Schema

### Table: `unit`

```sql
CREATE TABLE unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(255) NOT NULL UNIQUE,
    ref_id VARCHAR(255),
    owner_id VARCHAR(255) NOT NULL,  -- @TenantId
    workspace_id VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    name VARCHAR(10) NOT NULL,
    short_name VARCHAR(10) NOT NULL,
    decimal_places INT DEFAULT 2,

    INDEX unit_idx (name),
    INDEX idx_unit_uid (uid),
    INDEX idx_unit_owner (owner_id)
);
```

### Table: `unit_conversion`

```sql
CREATE TABLE unit_conversion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uid VARCHAR(255) NOT NULL UNIQUE,
    ref_id VARCHAR(255),
    owner_id VARCHAR(255) NOT NULL,  -- @TenantId
    workspace_id VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),

    base_unit_id VARCHAR(200) NOT NULL,
    derived_unit_id VARCHAR(200) NOT NULL,
    product_id VARCHAR(200),
    multiplier DOUBLE NOT NULL DEFAULT 1.0,

    FOREIGN KEY (base_unit_id) REFERENCES unit(uid),
    FOREIGN KEY (derived_unit_id) REFERENCES unit(uid),
    INDEX idx_conversion_owner (owner_id),
    INDEX idx_conversion_product (product_id)
);
```

**Note**: These tables already exist in the database. No migration scripts are needed for this refactoring.

---

## Validation Rules

### Unit Validation

1. **Name**: Required, max 10 characters, trimmed
2. **ShortName**: Optional, defaults to name if not provided, max 10 characters
3. **DecimalPlaces**: Must be between 0 and 6 inclusive
4. **Uniqueness**: Unit name should be unique per workspace (business rule, not database constraint)
5. **Deletion**: Cannot delete unit if referenced by any product, inventory, or unit conversion

### UnitConversion Validation

1. **Base ≠ Derived**: Base unit ID must differ from derived unit ID
2. **Multiplier**: Must be positive (> 0)
3. **Unit Existence**: Both base and derived units must exist and be active
4. **Product Existence**: If productId provided, product must exist
5. **No Circular Conversions**: A→B, B→C, C→A is invalid (validated at service layer)
6. **Workspace Isolation**: Base unit, derived unit, and product must belong to same workspace

---

## Usage Examples

### Creating a Unit

```kotlin
val request = UnitRequest(
    name = "Kilogram",
    shortName = "kg",
    decimalPlaces = 3
)

val response: UnitResponse = unitService.create(request)
// Response: { "uid": "UNIT-001", "name": "Kilogram", ... }
```

### Creating a Unit Conversion

```kotlin
val request = UnitConversionRequest(
    baseUnitId = "UNIT-001",  // kg
    derivedUnitId = "UNIT-002",  // gram
    multiplier = 1000.0
)

val response: UnitConversionResponse = unitConversionService.create(request)
// Response: { "uid": "CONV-001", "multiplier": 1000.0, "base_unit": {...}, ... }
```

### Converting Quantities

```kotlin
val grams: Double = unitConversionService.convert(
    quantity = 2.5,          // 2.5 kg
    fromUnitId = "UNIT-001", // kg
    toUnitId = "UNIT-002",   // gram
    productId = null         // Global conversion
)
// Result: 2500.0 grams
```

---

## Next Steps

Proceed to generate API contracts in `contracts/` directory with OpenAPI specifications for all REST endpoints.
