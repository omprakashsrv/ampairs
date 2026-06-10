# form module

Unified, schema-driven dynamic forms (spec 011). One configurable field model per entity type —
visibility, ordering, sections, labels, validation, and custom attributes — consumed by the mobile
client to render entry forms and the admin form-configuration editor.

## REST Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/form/v1/config/schema` | Current schema for one entity type (`?entity_type=customer`); seeds registry defaults + merges new standard fields on read |
| GET | `/form/v1/config/schema/sync` | Aggregate sync pull — one `FormSchema` record per entityType updated since `last_sync` (full feed seeds defaults for every provider-backed entity type) |
| POST | `/form/v1/config/schema/sync` | Aggregate push — replace each `FormSchema` (upsert present members, **delete absent**), optimistic `base_version` |

Supported entity types (`EntityType`, rejected otherwise): `customer`, `product`, `order`,
`invoice`, `business`.

## Aggregate Model

```
FormSchema  (1 per workspace × entityType; uid = entityType; version for optimistic concurrency)
 └── FormSection  (ordered, hideable; "General" fallback always exists)
      └── FormField (ordered within section)
```

### FormField (unified — no separate FieldConfig/AttributeDefinition)

```kotlin
class FormField : OwnableBaseDomain() {
    var entityType: String        // "customer", "product", …
    var source: String            // "standard" (binds to entity column) | "custom" (entity attributes JSON)
    var fieldKey: String          // column name for STANDARD; attributes key for CUSTOM
    var displayName: String
    var dataType: String          // text|textarea|number|boolean|date|choice|multi_choice|custom
    var widgetKey: String?        // for dataType=custom: client widget (location, contact, address, …)
    var sectionUid: String        // mandatory — every field lives in a section
    var visible/mandatory/enabled: Boolean
    var displayOrder: Int
    var optionSource: String?     // choice: "static" (enumValues) | "dynamic" (dynamicSourceKey)
    var enumValues: List<String>?         // @JdbcTypeCode(SqlTypes.JSON)
    var validationRules: List<ValidationRule>?  // typed: required|length_range|number_range|format|allowed_choices
}
```

No soft-delete column anywhere in the aggregate — **removal is by absence** from the pushed schema.

## Standard fields via SPI

Domains declare their built-in fields by implementing `StandardFieldProvider` (`@Component`);
`FormFieldRegistry` aggregates providers to seed defaults, merge new fields non-destructively, and
validate STANDARD keys. Essential fields (`essential = true`) cannot be removed or hidden.

Providers: `CustomerStandardFieldProvider` (customer), `ProductStandardFieldProvider` (product),
`OrderStandardFieldProvider` (order), `InvoiceStandardFieldProvider` (invoice),
`BusinessStandardFieldProvider` (workspace module, `EntityType.BUSINESS`).

## Sync semantics

- **Pull** pages whole aggregates (`ApiResponse<PageResponse<FormSchemaResponse>>`); a full pull
  (no `last_sync`) first seeds defaults for the workspace, so a fresh client never sees an empty feed.
- **Push** (`List<FormSchemaRequest>` with `base_version`) replaces each aggregate atomically inside
  one transaction: upsert members present, delete members absent, `version += 1`. A stale
  `base_version` is rejected with **409** (clean `ApiResponse` via the global
  `ResponseStatusException` handler); the client re-pulls the version, rebases its local edits, and
  retries (aggregate-level last-write-wins).
- Invariants validated on every push: ≥1 section; every field references a section in the aggregate;
  essential STANDARD fields present + visible; STANDARD keys must exist in the registry; choice
  fields need a complete option source; `custom` needs a `widget_key`; validation rules must be
  internally consistent (`ValidationEngine`).

See `docs/guides/offline-sync-contract.md` — form is the documented *aggregate-grained* `/sync`
resource.

## Tests

`form/src/test/...`: `FormConfigServiceTest` (seed-on-read, merge preservation, integrity rules,
optimistic conflict, delete-by-absence), `FormFieldRegistryTest`, `ValidationEngineTest`.

## Migrations

`V1.0.7` (legacy tables, superseded), `V1.0.80` creates `form_schema` + `form_section` +
`form_field` and drops the legacy `field_config` + `attribute_definition` tables (clean cutover,
no backfill).

## Base path

`/form/v1/**`
