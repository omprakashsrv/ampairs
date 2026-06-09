# Phase 1 Data Model: Unified Schema-Driven Dynamic Forms

Entities are defined once and mirrored on both sides: backend JPA (`OwnableBaseDomain`, `Instant`,
SNAKE_CASE JSON) and app Room/`@Serializable` (`@SerialName` snake_case, ISO-8601 string timestamps).
Field names below are camelCase (Kotlin); DB/JSON are snake_case automatically.

---

## Enums (shared backend + app)

```
EntityType    = CUSTOMER | PRODUCT | ORDER | INVOICE | BUSINESS
FieldSource   = STANDARD | CUSTOM
FieldDataType = TEXT | TEXTAREA | NUMBER | BOOLEAN | DATE | CHOICE | MULTI_CHOICE | CUSTOM
OptionSource  = STATIC | DYNAMIC          # for CHOICE and MULTI_CHOICE fields
```

- `EntityType` replaces all magic `"customer"`/`"product"` strings (FR-024). Unknown values rejected at
  the API boundary.
- `CHOICE` = single selection (value is one option); `MULTI_CHOICE` = multi-select (value is a list of
  options). Both share the same option config (`optionSource` + `enumValues`/`dynamicSourceKey`).
- `FieldDataType.CUSTOM` is the escape hatch (D5) — pairs with `widgetKey`.

---

## Aggregate model (DDD)

The schema is a **`FormSchema` aggregate**, one per `(workspace, entityType)`:

```
FormSchema (aggregate root — identity = workspace + entityType)
└── Section (ordered)        owns its fields
      └── Field (ordered)    belongs to exactly one section
```

- **Aggregate root** = the per-entityType schema. `Section` and `Field` are members; **every `Field`
  belongs to exactly one `Section`** (mandatory ownership — no orphan fields).
- **Invariants enforced at the aggregate boundary** (on save and on every `/sync` push, applied to the
  resulting state for that entityType): unique `fieldKey` per `(source)`; unique `displayOrder` within a
  section; a `Section` cannot be removed while it still owns fields (reassign first); a STANDARD field
  cannot be removed and an essential STANDARD field cannot be hidden; CHOICE/CUSTOM field invariants.
- **A default `General` section is always present** (seeded from the registry) so the registry can place
  every standard field and every new custom field has a home.
- **Persistence is relational** — a `form_schema` header row (per workspace+entityType, holds `version`)
  + `form_section` + `form_field` rows (mandatory section FK) — loaded and saved as one aggregate
  (validated together, atomic write).
- **The aggregate is also the sync unit.** One `FormSchema` record per entityType (uid = entityType) is
  transferred whole over a single `/sync` feed. Deletions propagate by **absence** — a removed field or
  section simply isn't in the next aggregate — so there is **no soft-delete flag** on any form row.
  Concurrency is whole-form last-write-wins guarded by an optimistic `version` stamp (FR-018). Editing
  different entityTypes never conflicts. `EntityConfigSchema` / `FormSchemaResponse` (below) is the
  read/transfer projection of the aggregate.

---

## Entity: FormSchema (aggregate root)

The aggregate header — one per `(workspace, entityType)` — and the unit of sync.

| Field | Type | Notes |
|---|---|---|
| `uid` | String(200), unique | Sync key = the `entityType` value (workspace-scoped). |
| `ownerId` | String(200) | `@TenantId`. |
| `entityType` | `EntityType` | Unique per workspace. |
| `version` | Long = 0 | Optimistic-concurrency stamp; bumped on every aggregate save. |
| `createdAt` | `Instant` | |
| `updatedAt` | `Instant` | `max(updatedAt)` across the aggregate's members (the sync `last_sync` key). |

**Uniqueness**: `(owner_id, entity_type)`. The aggregate's `sections` and `fields` are the `FormSection`
/ `FormField` rows with the same `(owner_id, entity_type)`. Loaded/saved/transferred as one unit; no
soft-delete on members (removal = absence on the next save).

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
| `sectionUid` | String(200) | FK→`FormSection.uid`, **required** — every field belongs to exactly one section (a seeded default `General` section always exists). Always resolvable: the whole aggregate (sections + fields) is transferred together, so there is no dangling/partial-arrival case. |
| `visible` | Boolean = true | |
| `mandatory` | Boolean = false | |
| `enabled` | Boolean = true | Editable vs read-only. |
| `displayOrder` | Int = 0 | Order within section. |
| `defaultValue` | String(255)? | |
| `optionSource` | `OptionSource`? | CHOICE / MULTI_CHOICE only. |
| `enumValues` | JSON (List<String>)? | CHOICE/MULTI_CHOICE + STATIC: the static options. |
| `dynamicSourceKey` | String(100)? | CHOICE/MULTI_CHOICE + DYNAMIC: e.g. `customer_types`, `tax_codes`, `units`. |
| `validationRules` | JSON (List<ValidationRule>)? | Typed rules (see below). |
| `placeholder` | String(255)? | |
| `helpText` | TEXT? | |
| `createdAt` | `Instant` | |
| `updatedAt` | `Instant` | Feeds the aggregate's `updated_at`. (No soft-delete flag — removal = absence from the aggregate.) |

**Uniqueness**: `(owner_id, entity_type, source, field_key)`.
**Indexes**: `(owner_id, entity_type)`; unique `(uid)`; `(entity_type, section_uid, display_order)`.

**Invariants** (enforced at the aggregate boundary — on save and on `/sync` push):
- Every field MUST reference an existing `FormSection` in the same aggregate (mandatory ownership; the `General` section is the default home).
- STANDARD field: `fieldKey` MUST exist in the registry for `entityType`; cannot be removed; cannot be made `visible=false` if registry marks it structurally essential (FR-015).
- CUSTOM field: freely creatable/deletable; value lives in the entity's `attributes` JSON.
- `dataType ∈ {CHOICE, MULTI_CHOICE}` ⇒ `optionSource` set; STATIC ⇒ `enumValues` non-empty; DYNAMIC ⇒ `dynamicSourceKey` set. `MULTI_CHOICE` value is a list (stored as JSON in the entity's `attributes` for CUSTOM; a STANDARD field may be `MULTI_CHOICE` only if its bound column is a collection — registry-declared).
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
| `createdAt` / `updatedAt` | `Instant` | (No soft-delete flag — removal = absence from the aggregate.) |

**Uniqueness**: `(owner_id, entity_type, uid)`. **Index**: `(owner_id, entity_type, display_order)`.

**Lifecycle / transitions** (as a member of the `FormSchema` aggregate):
- A default `General` section is seeded per entityType and is the fallback home for fields; it cannot be
  deleted (an aggregate must always have at least one section so every field has a home).
- Create → Active. Rename/reorder/hide → in place (`updatedAt` bumped).
- Delete: only after its fields are reassigned (the aggregate rejects deleting a non-empty section);
  the editor moves the fields to another section — or the `General` section — first. On the next push the
  removed section is simply absent from the aggregate (delete-by-absence; no soft-delete row).

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

## Aggregate projection: EntityConfigSchema / FormSchemaResponse (transport + render unit)

```
EntityConfigSchema(           # == the FormSchema aggregate, serialized
  entityType: EntityType,
  version:    Long,           # optimistic-concurrency stamp
  sections:   List<FormSection>,     # ordered, visible-aware
  fields:     List<FormField>,       # ordered within section
  lastUpdated: Instant?              # max(updatedAt) across fields+sections
)
```

Helpers (app): `visibleFields()`, `mandatoryFields()`, `fieldsBySection()`, `isFieldVisible(key)`.
This **is** the aggregate transferred whole over the single `/config/schema/sync` feed and returned by
the read-only `GET /config/schema`. On the app it is assembled from / written back to the relational
`form_schema` + `form_section` + `form_field` Room tables. It is what `DynamicFormRenderer` consumes.

---

## Relationship to owning entities (unchanged storage pattern)

- **STANDARD** field values bind to the owning entity's real columns (e.g. `Customer.name`).
- **CUSTOM** field values are stored in the owning entity's existing `attributes: Map<String, Any>` JSON
  column (e.g. `Customer.attributes`). No new per-value tables (FR-008, transparent to staff).
- Deleting a CUSTOM field removes it from the aggregate (absent on the next save); **stored values in
  `attributes` are retained** (Assumptions / edge case) — not purged.

---

## Fresh provisioning (FR-025, SC-008) — no migration

This is a clean-slate setup. There is **no backfill** of the old `field_config` /
`attribute_definition` data and **no legacy compatibility layer**.

**Backend** (`V1.0.x__create_unified_form_model.sql`, mysql + postgresql):
1. Create `form_schema`, `form_section`, and `form_field` (no soft-delete column; indexes + uniqueness
   above). Tables start empty.
2. The legacy `field_config` / `attribute_definition` tables and their endpoints are **dropped/removed**
   (the old form module is replaced, not adapted). No data is carried over.
3. A workspace's initial schema (sections + fields + a `form_schema` header) is produced lazily by
   `FormConfigService` from the `FormFieldRegistry` on first access (seed-on-read), then persisted — not
   by a data migration.

**App** (`FormDatabase`): introduced with the unified `form_schema` / `form_section` / `form_field`
schema directly. The old `entity_field_configs` / `entity_attribute_definitions` tables are removed (a
fresh schema / destructive recreate on first launch of the new build) — no row copy. Dirtiness is
tracked per entityType aggregate (no per-row `synced`/`active`).

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

Each domain module implements it (`customer`, `product`, `order`, `invoice`, and **`workspace`** for the
`BUSINESS` entity type — there is no separate `business` backend module); `FormFieldRegistry` aggregates.
Used to seed defaults, merge new fields on read without overwriting customizations (FR-022), and validate
STANDARD `fieldKey`/`entityType` (FR-020). This is the single source of truth that removes both hardcoded
lists (FR-021).

**Seed-on-read merge contract (FR-022)**: on `getConfigSchema`, for the workspace's `entityType`, add any
registry standard field whose `fieldKey` has no existing row (create as a new `FormField`), and create any
registry section that is missing — but **never overwrite** an existing row's visibility, order, label,
section assignment, or validation. New releases that add registry fields thus appear without erasing
workspace customizations.
