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
- **Ordering tolerance**: items are paged by `updated_at`, so a FIELD may arrive before the SECTION its
  `section_uid` references. The client MUST NOT treat a dangling `section_uid` as an error — render the
  field in the default/unsectioned group until the section row arrives, then re-group. No FK ordering is
  guaranteed across pages.
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

## 3. Removed (no legacy compatibility)

Fresh setup, and the Angular web client is deprecated, so **no legacy endpoints are retained**. The
following are deleted outright (not adapted):

```
GET/POST /form/v1/config/field-configs/sync           # removed
GET/POST /form/v1/config/attribute-definitions/sync   # removed
POST /form/v1/config/field-config | /attribute-definition | /config   # removed
DELETE /form/v1/config/field-config | /attribute-definition           # removed
```

The unified `/config/schema/sync` (push/pull) plus the read-only `/config/schema` are the entire
target contract. The new app build uses only the unified feed; older installs simply re-provision
from defaults on update (no legacy data to preserve).

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
