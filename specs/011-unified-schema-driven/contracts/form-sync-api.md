# Phase 1 Contract: Unified Form Configuration API

Module base: `/form/v1/config`. Tenant scope via `X-Workspace-ID` (set at controller, `@TenantId`
auto-filter). All responses are `ApiResponse<T>`. JSON is global SNAKE_CASE. Conforms to
`docs/guides/offline-sync-contract.md`.

---

## 1. Sync feeds (NEW — primary going forward)

`form` syncs **two feeds under one logical entity** `SyncEntity.FORM` — `sections` and `fields` — each a
plain canonical `/sync` resource (no discriminated mega-record). This matches the proven pattern the
form module already used (it synced two feeds before) and keeps each DTO clean. **One checkpoint** for
`SyncEntity.FORM` = `max(updated_at)` across **both** `form_field` and `form_section`
(`FormCheckpointContributor`).

```
GET  /form/v1/config/sections/sync   ?last_sync&page&size&sort_by=updatedAt&sort_dir=ASC
POST /form/v1/config/sections/sync   body: List<FormSectionSyncRequest>
     → ApiResponse<PageResponse<FormSectionResponse>>  /  ApiResponse<List<FormSectionResponse>>

GET  /form/v1/config/fields/sync     ?last_sync&page&size&sort_by=updatedAt&sort_dir=ASC
POST /form/v1/config/fields/sync     body: List<FormFieldSyncRequest>
     → ApiResponse<PageResponse<FormFieldResponse>>    /  ApiResponse<List<FormFieldResponse>>
```

### Incremental (diff) semantics — both feeds

- **Row-level diff keyed on `updated_at`.** Pull returns every row with `updated_at >= last_sync` (whole
  row, not a column delta). Boundary uses `>=` (idempotent upsert tolerates the re-sent edge row).
- **Stable pagination**: sort is `(updated_at ASC, uid ASC)` — the `uid` tiebreaker prevents skip/dup at
  page boundaries when many rows share a timestamp (e.g. a bulk save stamps them identically).
- **Includes soft-deleted rows** (`active = false`) so deletes propagate. No active filter on the feed.
- `last_sync` optional; absent/blank ⇒ full feed. Defaults `page=0, size=100`.
- **Pull order = sections, then fields.** The delegate pulls `sections/sync` first so a field's
  `section_uid` usually resolves immediately; the I3 tolerance (render dangling `section_uid` in the
  default group, then re-group) still covers the rare cross-cycle gap. After both feeds drain, advance
  the single `SyncEntity.FORM` checkpoint to `max(updated_at)` seen across both.

### Section-detail updates (the common case)

Renaming / reordering / hiding a section bumps only that **section row's** `updated_at`. It comes down
on the `sections/sync` feed and the client upserts it; **fields are untouched** — they reference the
section by `uid`, so the form simply re-groups under the new name/order. Reassigning a field to another
section bumps the **field's** `section_uid` + `updated_at` (it re-syncs on `fields/sync`). Deleting a
section sends `active=false` on `sections/sync`; the normal reassignment of its fields re-syncs those
fields too.

### Push — both feeds

- UID-keyed bulk upsert per feed (`uid` exists → update, else create), honoring `active=false` in-band.
- **Push order = sections, then fields** (so a field's `section_uid` target exists server-side first).
- Server validates on the fields feed: STANDARD `field_key`/`entity_type` exist in the registry; STANDARD
  fields not soft-deletable and essential ones not hideable; CHOICE/CUSTOM invariants (data-model).
  Violations bubble to the global handler as `ApiResponse` errors (no per-row silent drop).
- Returns server-resolved rows; client reconciles by `uid`. Client batches at 100 per feed.

### DTO shapes

`FormFieldResponse` / `FormFieldSyncRequest` — the field columns from data-model.md (`uid`, `source`,
`field_key`, `display_name`, `data_type`, `widget_key`, `section_uid`, `visible`, `mandatory`, `enabled`,
`display_order`, `default_value` (`data_type` ∈ text|textarea|number|boolean|date|choice|multi_choice|custom), `option_source`, `enum_values`, `dynamic_source_key`,
`validation_rules`, `placeholder`, `help_text`, `active`, `created_at`, `updated_at`).
`FormSectionResponse` / `FormSectionSyncRequest` — (`uid`, `entity_type`, `name`, `display_order`,
`visible`, `active`, `created_at`, `updated_at`). Sync requests drop server-managed audit fields; `uid`
optional on create.

---

## 2. Read-only schema fetch (NEW — UI-invoked convenience)

```
GET /form/v1/config/schema?entity_type=customer
→ ApiResponse<EntityConfigSchemaResponse>     // { sections:[...], fields:[...], last_updated }
```

Active rows only (excludes soft-deleted). Seeds/merges registry defaults on first access for the
workspace (FR-022, non-destructive). This is the app's allowed UI-invoked read (not part of central sync).

---

## 3. Removed (no legacy compatibility)

Fresh setup, and the Angular web client is deprecated, so **no legacy endpoints are retained**. The
following are deleted outright (not adapted):

```
GET/POST /form/v1/config/field-configs/sync           # removed
GET/POST /form/v1/config/attribute-definitions/sync   # removed
POST /form/v1/config/field-config | /attribute-definition | /config   # removed
DELETE /form/v1/config/field-config | /attribute-definition           # removed
```

The two feeds `/config/sections/sync` + `/config/fields/sync` (push/pull) plus the read-only
`/config/schema` are the entire target contract. The new app build uses only these; older installs
simply re-provision from defaults on update (no legacy data to preserve).

---

## Client (app) API surface

`ConfigApi` (app) exposes the two feeds + the UI read:

```
suspend fun getSectionsSync(lastSync: String, page: Int, size: Int,
                            sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<FormSection>
suspend fun pushSections(sections: List<FormSection>): List<FormSection>

suspend fun getFieldsSync(lastSync: String, page: Int, size: Int,
                          sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<FormField>
suspend fun pushFields(fields: List<FormField>): List<FormField>

suspend fun getConfigSchema(entityType: String): EntityConfigSchema     // UI read (unchanged role)
```

`FormSyncDelegate` drives both feeds under one `SyncEntity.FORM` checkpoint (sections before fields on
both pull and push), soft-delete aware, batched 100 per feed, local-unsynced-wins. The single checkpoint
advances to `max(updated_at)` across both feeds after both drain. The old
`getFieldConfigsSync`/`getAttributeDefinitionsSync` methods are removed.
