# unit module

Units of measure and conversion rules used by product catalog and inventory.

## Key entities
- `Unit` — name, shortName, decimalPlaces (0-10), category, active
- `UnitConversion` — baseUnitId, derivedUnitId, multiplier (BigDecimal 20,6), entityId (optional product scope)

Formula: `derivedQuantity = baseQuantity × multiplier`

## Guardrails
- Circular conversions rejected (`CircularConversionException`)
- Units in use by products cannot be deleted (`UnitInUseException`)

## Base path
`/api/v1/unit/**`

## Migrations
`V1.0.41`

## Full docs
`docs/modules/unit.md`
