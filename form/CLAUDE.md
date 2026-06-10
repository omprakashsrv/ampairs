# form module

Unified, schema-driven dynamic forms — one field model per entity type, configurable visibility,
ordering, sections, validation, and custom attributes (spec 011).

## Aggregate model
- `FormSchema` — aggregate root, one per `workspace × entityType`; carries `version` (optimistic
  concurrency) and assembles ordered `sections → fields`. Its `updatedAt` = max across members.
- `FormSection` — first-class, ordered, hideable; every field belongs to exactly one section.
- `FormField` — unified field: `source` = STANDARD (binds to an entity column via `fieldKey`) or
  CUSTOM (stored in the entity's `attributes` JSON). `dataType` ∈ TEXT/TEXTAREA/NUMBER/BOOLEAN/DATE/
  CHOICE/MULTI_CHOICE/CUSTOM. Choices are STATIC (`enumValues`) or DYNAMIC (`dynamicSourceKey`).
  No soft-delete column — removal is by absence from the pushed aggregate.
- `ValidationRule` — typed sealed model (Required / LengthRange / NumberRange / Format / AllowedChoices).

## Standard fields via SPI
Each domain module declares its built-in fields by implementing `StandardFieldProvider`
(`@Component`, returns `StandardSectionSpec` + `StandardFieldSpec`). `FormFieldRegistry` aggregates
all providers to seed defaults, merge new fields non-destructively (seed-on-read), and validate
STANDARD keys. Providers: `CustomerStandardFieldProvider` (customer), `ProductStandardFieldProvider`
(product), `OrderStandardFieldProvider` (order), `InvoiceStandardFieldProvider` (invoice),
`BusinessStandardFieldProvider` (workspace, `EntityType.BUSINESS`).

## Offline-sync — single aggregate feed
`GET/POST /form/v1/config/schema/sync` — one `FormSchema` record per entityType (uid = entityType).
- Pull includes every aggregate updated since `last_sync`; **delete-by-absence** (a member omitted
  from a pushed schema is deleted server-side and disappears on the next pull everywhere).
- Push replaces the aggregate with optimistic `version`: a stale `base_version` is rejected; the
  client re-pulls, re-applies local edits, and retries.
- `GET /form/v1/config/schema?entity_type=customer` seeds + returns the current schema for the UI.

See `docs/guides/offline-sync-contract.md` (form is an aggregate-grained `/sync` resource, a
documented nuance alongside `tax`/`file`).

## Base path
`/form/v1/**`

## Full docs
`docs/modules/form.md`
