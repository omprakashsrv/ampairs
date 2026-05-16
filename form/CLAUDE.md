# form module

Dynamic form configuration — field visibility, ordering, validation, and custom attributes per entity type.

## Key entities
- `FieldConfig` — entityType, fieldName, visible, mandatory, displayOrder, validationType, validationParams (JSON)
- `AttributeDefinition` — entityType, attributeKey, dataType (STRING/NUMBER/BOOLEAN/DATE/ENUM), enumValues (JSON), mandatory, displayOrder

## Usage
Clients fetch `GET /form/v1/schema?entity_type=customer` and render fields accordingly.
Custom attribute values are stored as JSON in the entity's `attributes` column.

## Base path
`/form/v1/**`

## Migrations
`V1.0.7`

## Full docs
`docs/modules/form.md`
