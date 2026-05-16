# form module

Dynamic form configuration system. Stores field visibility, ordering, validation rules, and custom attribute definitions per entity type. Web and mobile clients read this to render configurable UI.

## REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/form/v1/schema` | Get config schema for an entity type (`?entity_type=customer`) |
| GET | `/form/v1/schemas` | Get all configuration schemas |
| POST | `/form/v1/config` | Bulk create or update schema |
| POST | `/form/v1/field-config` | Create or update a field config |
| POST | `/form/v1/attribute-definition` | Create or update an attribute definition |
| DELETE | `/form/v1/field-config` | Delete a field config |
| DELETE | `/form/v1/attribute-definition` | Delete an attribute definition |

## Key Entities

### FieldConfig

Controls visibility and behaviour of standard entity fields:

```kotlin
class FieldConfig : OwnableBaseDomain() {
    val entityType: String         // "customer", "product", "order", etc.
    val fieldName: String          // maps to entity property name
    val displayName: String
    val visible: Boolean
    val mandatory: Boolean
    val enabled: Boolean
    val displayOrder: Int
    val validationType: String?    // "phone", "email", "gst", etc.
    val validationParams: String?  // JSON — e.g. min/max length
    val placeholder: String?
    val helpText: String?          // TEXT column
    val defaultValue: String?
}
```

### AttributeDefinition

Defines custom/extra fields that can be attached to entities:

```kotlin
class AttributeDefinition : OwnableBaseDomain() {
    val entityType: String
    val attributeKey: String       // unique per entity type in workspace
    val displayName: String
    val dataType: String           // STRING, NUMBER, BOOLEAN, DATE, ENUM
    val visible: Boolean
    val mandatory: Boolean
    val enabled: Boolean
    val displayOrder: Int
    val category: String?          // groups related attributes in UI
    val defaultValue: String?
    val validationType: String?
    val validationParams: String?  // JSON
    val enumValues: String?        // JSON array — for ENUM data type
    val placeholder: String?
    val helpText: String?          // TEXT
}
```

## Entity Types

Common values for `entityType`:
- `customer` — Customer fields
- `product` — Product catalog fields
- `order` — Order fields
- `invoice` — Invoice fields
- `business` — Business profile fields

## Usage Pattern

1. On workspace setup, `ConfigService` auto-seeds default schemas for each entity type.
2. Clients fetch schema via `GET /form/v1/schema?entity_type=customer` and render fields accordingly.
3. Workspace admins can customize via the API — hide fields, change order, add custom attributes.
4. Custom attribute values are stored as JSON in the entity's `attributes` column.

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.7__create_form_module_tables.sql` | field_config, attribute_definition tables |

## Package Structure

```
com.ampairs.form
├── controller/     — ConfigController
├── domain/
│   ├── dto/        — FieldConfigRequest/Response, AttributeDefinitionRequest/Response,
│   │                  EntityConfigSchemaRequest/Response
│   ├── model/      — FieldConfig, AttributeDefinition
│   └── repository/ — FieldConfigRepository, AttributeDefinitionRepository
└── domain/service/ — ConfigService
```
