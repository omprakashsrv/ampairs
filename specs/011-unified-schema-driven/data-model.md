# Phase 1 Data Model: Unified Schema-Driven Dynamic Forms

Entities are defined once and mirrored on both sides: backend JPA (`OwnableBaseDomain`, `Instant`,
SNAKE_CASE JSON) and app Room/`@Serializable` (`@SerialName` snake_case, ISO-8601 string timestamps).
Field names below are camelCase (Kotlin); DB/JSON are snake_case automatically.

---

## Enums (shared backend + app)

```
EntityType    = CUSTOMER | PRODUCT | ORDER | INVOICE | BUSINESS
FieldSource   = STANDARD | CUSTOM
FieldDataType = TEXT | TEXTAREA | NUMBER | BOOLEAN | DATE | CHOICE | CUSTOM
OptionSource  = STATIC | DYNAMIC          # for CHOICE fields
```

- `EntityType` replaces all magic `"customer"`/`"product"` strings (FR-024). Unknown values rejected at
  the API boundary.
- `FieldDataType.CUSTOM` is the escape hatch (D5) — pairs with `widgetKey`.

---

## Entity: FormField

The single unified field model. Replaces `FieldConfig` **and** `AttributeDefinition` (FR-001).

| Field | Type | Notes |
|---|---|---|
| `uid` | String(200), unique | Sync key. Prefix `FF`. Client-generated for CUSTOM; STANDARD seeded server-side. |
| `ownerId` | String(200) | `@TenantId` — workspace scope (FR-019). |
| `entityType` | `EntityType` | Part of uniqueness. |
| `source` | `FieldSource` | STANDARD (binds to an entity column) or CUSTOM (stored in entity `attributes` map). |
| `fieldKey` | String(100) | For STANDARD = the entity property name (validated against registry); for CUSTOM = the attribute key. Part of uniqueness. |
| `displayName` | String(255) | UI label. |
| `dataType` | `FieldDataType` | |
| `widgetKey` | String(100)? | Required iff `dataType = CUSTOM` (e.g. `image_gallery`, `address`, `location`, `business_hours`). |
| `sectionUid` | String(200)? | FK→`FormSection.uid` (nullable = default/unsectioned group). |
| `visible` | Boolean = true | |
| `mandatory` | Boolean = false | |
| `enabled` | Boolean = true | Editable vs read-only. |
| `displayOrder` | Int = 0 | Order within section. |
| `defaultValue` | String(255)? | |
| `optionSource` | `OptionSource`? | CHOICE only. |
| `enumValues` | JSON (List<String>)? | CHOICE + STATIC: the static options. |
| `dynamicSourceKey` | String(100)? | CHOICE + DYNAMIC: e.g. `customer_types`, `tax_codes`, `units`. |
| `validationRules` | JSON (List<ValidationRule>)? | Typed rules (see below). |
| `placeholder` | String(255)? | |
| `helpText` | TEXT? | |
| `active` | Boolean = true | **Soft-delete** flag (D2). `false` = deleted; carried in-band on `/sync`. |
| `createdAt` | `Instant` | |
| `updatedAt` | `Instant` | Sync ordering / checkpoint. |

**Uniqueness**: `(owner_id, entity_type, source, field_key)`.
**Indexes**: `(owner_id, entity_type)`; unique `(uid)`; `(entity_type, section_uid, display_order)`.

**Invariants** (service-enforced):
- STANDARD field: `fieldKey` MUST exist in the registry for `entityType`; cannot be soft-deleted; cannot be made `visible=false` if registry marks it structurally essential (FR-015).
- CUSTOM field: freely creatable/deletable; value lives in the entity's `attributes` JSON.
- `dataType=CHOICE` ⇒ `optionSource` set; STATIC ⇒ `enumValues` non-empty; DYNAMIC ⇒ `dynamicSourceKey` set.
- `dataType=CUSTOM` ⇒ `widgetKey` set.

---

## Entity: FormSection

First-class, configurable, syncable grouping (clarified — D/Q3, FR-010a).

| Field | Type | Notes |
|---|---|---|
| `uid` | String(200), unique | Sync key. Prefix `FS`. |
| `ownerId` | String(200) | `@TenantId`. |
| `entityType` | `EntityType` | |
| `name` | String(255) | Section title. |
| `displayOrder` | Int = 0 | Section order on the form. |
| `visible` | Boolean = true | Hidden section ⇒ its fields are not rendered. |
| `active` | Boolean = true | Soft-delete. |
| `createdAt` / `updatedAt` | `Instant` | |

**Uniqueness**: `(owner_id, entity_type, uid)`. **Index**: `(owner_id, entity_type, display_order)`.

**Lifecycle / transitions**:
- Create → Active. Rename/reorder/hide → in place (`updatedAt` bumped).
- Soft-delete: only when empty, OR fields are reassigned first; otherwise the service reassigns orphaned
  fields to the default/unsectioned group (FR edge case "Deleting a non-empty section").

---

## Value object: ValidationRule (typed; stored as JSON list)

```
sealed ValidationRule:
  Required
  LengthRange(min: Int?, max: Int?)
  NumberRange(min: Double?, max: Double?)
  Format(kind: EMAIL | PHONE | GSTIN | PAN | URL | ...)   # curated set, not free-form regex
  AllowedChoices(values: List<String>)                    # mirrors STATIC enumValues
```

Serialized with a `type` discriminator. Evaluated by the shared `ValidationEngine` on both sides (D6).
Contradictory rules (e.g. `LengthRange(min>max)`) are rejected by the editor before save (FR edge case).

---

## Aggregate: EntityConfigSchema (transport + render unit)

```
EntityConfigSchema(
  entityType: EntityType,
  sections:   List<FormSection>,     # ordered, visible-aware
  fields:     List<FormField>,       # ordered within section
  lastUpdated: Instant?              # max(updatedAt) across fields+sections
)
```

Helpers (app): `visibleFields()`, `mandatoryFields()`, `fieldsBySection()`, `isFieldVisible(key)`.
This is what the unified `/sync` feed materializes and what `DynamicFormRenderer` consumes.

---

## Relationship to owning entities (unchanged storage pattern)

- **STANDARD** field values bind to the owning entity's real columns (e.g. `Customer.name`).
- **CUSTOM** field values are stored in the owning entity's existing `attributes: Map<String, Any>` JSON
  column (e.g. `Customer.attributes`). No new per-value tables (FR-008, transparent to staff).
- Deleting a CUSTOM field soft-deletes the definition; **stored values in `attributes` are retained**
  (Assumptions / edge case) — not purged.

---

## Fresh provisioning (FR-025, SC-008) — no migration

This is a clean-slate setup. There is **no backfill** of the old `field_config` /
`attribute_definition` data and **no legacy compatibility layer**.

**Backend** (`V1.0.x__create_unified_form_model.sql`, mysql + postgresql):
1. Create `form_section` and `form_field` (with `active`, indexes, uniqueness above). Tables start empty.
2. The legacy `field_config` / `attribute_definition` tables and their endpoints are **dropped/removed**
   (the old form module is replaced, not adapted). No data is carried over.
3. A workspace's initial fields/sections are produced lazily by `FormConfigService` from the
   `FormFieldRegistry` on first access (seed-on-read), then persisted — not by a data migration.

**App** (`FormDatabase`): introduced with the unified `form_field` / `form_section` schema directly.
The old `entity_field_configs` / `entity_attribute_definitions` tables are removed (a fresh schema /
destructive recreate on first launch of the new build) — no row copy. `synced` flags start clean.

> Because we provision fresh, FR-025/SC-008 are about *good defaults on first use*, not data
> preservation. Standard field defaults live only in the registry (SC-004).

---

## Standard Field Registry (backend SPI, not persisted)

```
interface StandardFieldProvider {
  fun entityType(): EntityType
  fun standardFields(): List<StandardFieldSpec>   # key, label, dataType, defaultSection,
                                                  # essential?, default validation, default order
}
```

Each domain module implements it; `FormFieldRegistry` aggregates. Used to seed defaults, merge new
fields on read without overwriting customizations (FR-022), and validate STANDARD `fieldKey`/`entityType`
(FR-020). This is the single source of truth that removes both hardcoded lists (FR-021).
