# unit module

Unit of measure definitions and conversion rules used by the product catalog and inventory systems.

## REST Endpoints

### Units (`/api/v1/unit`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/unit` | List all units |
| POST | `/api/v1/unit` | Create unit |
| GET | `/api/v1/unit/{uid}` | Get unit by UID |
| PUT | `/api/v1/unit/{uid}` | Update unit |
| DELETE | `/api/v1/unit/{uid}` | Delete unit (blocked if in use) |
| GET | `/api/v1/unit/{uid}/usage` | Check where a unit is referenced |

### Unit Conversions (`/api/v1/unit/conversions`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/unit/conversions` | List all conversions |
| POST | `/api/v1/unit/conversions` | Create conversion |
| GET | `/api/v1/unit/conversions/{uid}` | Get conversion by UID |
| PUT | `/api/v1/unit/conversions/{uid}` | Update conversion |
| DELETE | `/api/v1/unit/conversions/{uid}` | Delete conversion |

## Key Entities

### Unit

```kotlin
class Unit : OwnableBaseDomain() {
    val name: String           // e.g. "Kilogram"
    val shortName: String      // e.g. "kg"
    val decimalPlaces: Int     // 0–10, default 2
    val description: String?
    val category: String?      // e.g. "weight", "volume", "length"
    val active: Boolean
}
```

### UnitConversion

```kotlin
class UnitConversion : OwnableBaseDomain() {
    val baseUnitId: String         // source unit UID
    val derivedUnitId: String      // target unit UID
    val entityId: String?          // optional product/entity scope
    val multiplier: BigDecimal     // precision 20, scale 6
    val active: Boolean
    // Relationships
    val baseUnit: Unit
    val derivedUnit: Unit
}
```

Conversion formula: `derivedQuantity = baseQuantity × multiplier`

## Validation

- Circular conversions are detected and rejected (`CircularConversionException`)
- Units referenced by products or conversions cannot be deleted (`UnitInUseException`)
- `shortName` must be 1–20 characters, `name` must be 1–100 characters

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.41__create_unit_module_tables.sql` | units, unit_conversions tables |

## Package Structure

```
com.ampairs.unit
├── config/         — Constants, UnitModuleConfiguration
├── controller/     — UnitController, UnitConversionController
├── domain/
│   ├── dto/        — UnitDto (Request/Response), UnitConversionDto,
│   │                  UnitUsageResponse
│   └── model/      — Unit, UnitConversion
├── exception/      — CircularConversionException, UnitInUseException, UnitNotFoundException
├── repository/     — UnitRepository, UnitConversionRepository
└── service/        — UnitService, UnitServiceImpl, UnitConversionService,
                      UnitConversionServiceImpl, UnitUsageProvider,
                      UnitConversionUsageProvider
```
