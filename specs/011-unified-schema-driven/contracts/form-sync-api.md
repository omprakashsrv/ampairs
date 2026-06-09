# Phase 1 Contract: Unified Form Configuration API

Module base: `/form/v1/config`. Tenant scope via `X-Workspace-ID` (set at controller, `@TenantId`
auto-filter). All responses are `ApiResponse<T>`. JSON is global SNAKE_CASE. Conforms to
`docs/guides/offline-sync-contract.md`.

---

## 1. Unified sync feed (NEW — primary going forward)

One feed for the whole schema (fields **and** sections), replacing the two legacy feeds.

### Pull

```
GET /form/v1/config/schema/sync
    ?last_sync={ISO-8601}&page={int}&size={int}&sort_by=updatedAt&sort_dir=ASC
→ ApiResponse<PageResponse<FormConfigRecordResponse>>
```

- Feed is the **union** of fields and sections as `FormConfigRecordResponse` items, discriminated by
  `record_type` (`FIELD` | `SECTION`), so one paged feed + one client checkpoint covers both.
- **Includes soft-deleted rows** (`active = false`) — this is how deletes propagate. No active filter.
- `last_sync` optional; absent/blank ⇒ full feed. Defaults `page=0,size=100,sort_by=updatedAt,sort_dir=ASC`.

### Push

```
POST /form/v1/config/schema/sync
     body: List<FormConfigRecordRequest>     (UID-keyed; active upserts AND soft-deletes in-band)
→ ApiResponse<List<FormConfigRecordResponse>>
```

- UID-keyed bulk upsert (`uid` exists → update, else create). Honors `active=false` (in-band delete).
- Server validates: STANDARD `field_key`/`entity_type` exist in the registry; STANDARD fields not
  soft-deletable and essential ones not hideable; CHOICE/CUSTOM invariants (data-model). Violations bubble
  to the global handler as `ApiResponse` errors (no per-row silent drop).
- Returns server-resolved rows; client reconciles by `uid`.

### `FormConfigRecordResponse` (discriminated)

```jsonc
{
  "record_type": "FIELD",            // or "SECTION"
  "uid": "FF20260609...",
  "entity_type": "customer",
  // --- when record_type = FIELD ---
  "source": "standard",              // standard | custom
  "field_key": "phone",
  "display_name": "Phone",
  "data_type": "text",               // text|textarea|number|boolean|date|choice|custom
  "widget_key": null,                // set iff data_type=custom
  "section_uid": "FS2026...",        // nullable
  "visible": true, "mandatory": true, "enabled": true, "display_order": 2,
  "default_value": null,
  "option_source": null,             // choice only: static|dynamic
  "enum_values": null,               // choice+static
  "dynamic_source_key": null,        // choice+dynamic, e.g. "customer_types"
  "validation_rules": [ { "type": "required" }, { "type": "format", "kind": "phone" } ],
  "placeholder": null, "help_text": null,
  // --- when record_type = SECTION ---
  // "name": "Contact", "display_order": 1, "visible": true,
  "active": true,
  "created_at": "2026-06-09T10:00:00Z",
  "updated_at": "2026-06-09T10:00:00Z"
}
```

`FormConfigRecordRequest` mirrors this minus server-managed audit fields; `uid` optional on create.

---

## 2. Read-only schema fetch (NEW — UI-invoked convenience)

```
GET /form/v1/config/schema?entity_type=customer
→ ApiResponse<EntityConfigSchemaResponse>     // { sections:[...], fields:[...], last_updated }
```

Active rows only (excludes soft-deleted). Seeds/merges registry defaults on first access for the
workspace (FR-022, non-destructive). This is the app's allowed UI-invoked read (not part of central sync).

---

## 3. Legacy feeds (RETAINED as adapters during rollout — FR-026)

These keep older app builds working. They read/write the **same `form_field` store** via adapters and
are removed in a later cleanup once all clients update.

```
GET/POST /form/v1/config/field-configs/sync           # source=STANDARD ⇄ legacy FieldConfig shape
GET/POST /form/v1/config/attribute-definitions/sync   # source=CUSTOM  ⇄ legacy AttributeDefinition shape
```

- GET projects `form_field` rows of the matching `source` into the legacy DTO shape (legacy clients have
  no section concept — `section`/`category` is flattened to the legacy `category` string).
- POST upserts into `form_field` with the correct `source`. Legacy clients cannot soft-delete (their
  contract never could) — unchanged behavior.
- New `active=false` rows are still surfaced to legacy GET so deletes at least disappear for them too.

---

## 4. Deprecated / removed

- Legacy single-record CRUD (`POST /field-config`, `POST /attribute-definition`, `DELETE ...`) and the
  bulk `POST /config` are superseded by the unified `/schema/sync` push. They MAY be kept read-compatible
  short-term but are not part of the target contract.

---

## Client (app) API surface

`ConfigApi` (app) gains:

```
suspend fun getSchemaSync(lastSync: String, page: Int, size: Int,
                          sortBy: String = "updatedAt", sortDir: String = "ASC"
): PageResponse<FormConfigRecord>
suspend fun pushSchema(records: List<FormConfigRecord>): List<FormConfigRecord>
suspend fun getConfigSchema(entityType: String): EntityConfigSchema     // UI read (unchanged role)
```

`FormSyncDelegate` drives the single feed under `SyncEntity.FORM` (one checkpoint), soft-delete aware,
batched 100, local-unsynced-wins. The two legacy `getFieldConfigsSync`/`getAttributeDefinitionsSync`
methods are removed from the new app build (the new build uses only the unified feed; legacy endpoints
exist solely for older installs).
