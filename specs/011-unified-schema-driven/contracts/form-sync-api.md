# Phase 1 Contract: Unified Form Configuration API

Module base: `/form/v1/config`. Tenant scope via `X-Workspace-ID` (set at controller, `@TenantId`
auto-filter). All responses are `ApiResponse<T>`. JSON is global SNAKE_CASE.

The form schema is a **DDD aggregate**, and the aggregate is **also the sync unit** — so there is exactly
**one** sync feed (one GET + one POST). One record = the whole `FormSchema` for one `entityType`.

> **Documented deviation from the canonical row-level `/sync` norm.** The house contract
> (CLAUDE.md rule #9 / `docs/guides/offline-sync-contract.md`) is row-level UID-keyed upsert with in-band
> soft-delete. `form` is intentionally **aggregate-grained** (uid = entityType; delete-by-absence;
> optimistic `version`) — a recorded exception alongside `tax` (subscribe model) and `file` (multipart).
> It still uses `GET`/`POST /sync` with `ApiResponse<PageResponse>` / `ApiResponse<List>`. The guide is
> updated to list this exception (task T070); a reviewer should bless it as such.

---

## 1. Aggregate sync feed (the entire sync contract)

`uid = entity_type` (workspace-scoped natural key). There are only ~5 entity types per workspace, so the
feed is tiny and pagination is effectively a formality.

```
GET  /form/v1/config/schema/sync
     ?last_sync={ISO-8601}&page={int}&size={int}&sort_by=updatedAt&sort_dir=ASC
     → ApiResponse<PageResponse<FormSchemaResponse>>

POST /form/v1/config/schema/sync
     body: List<FormSchemaRequest>
     → ApiResponse<List<FormSchemaResponse>>
```

### Record shape — `FormSchemaResponse`

```jsonc
{
  "uid": "customer",                 // = entity_type (workspace-scoped)
  "entity_type": "customer",
  "version": 7,                      // optimistic-concurrency stamp, bumped on each save
  "sections": [
    { "uid": "FS2026...", "name": "Contact", "display_order": 1, "visible": true }
    // ...
  ],
  "fields": [
    { "uid": "FF2026...", "source": "standard", "field_key": "phone", "display_name": "Phone",
      "data_type": "text",           // text|textarea|number|boolean|date|choice|multi_choice|custom
      "widget_key": null,            // set iff data_type=custom
      "section_uid": "FS2026...",    // required — always resolvable (whole aggregate arrives together)
      "visible": true, "mandatory": true, "enabled": true, "display_order": 2,
      "default_value": null,
      "option_source": null,         // choice/multi_choice only: static|dynamic
      "enum_values": null,           // +static
      "dynamic_source_key": null,    // +dynamic, e.g. "customer_types"
      "validation_rules": [ { "type": "required" }, { "type": "format", "kind": "phone" } ],
      "placeholder": null, "help_text": null }
    // ...
  ],
  "updated_at": "2026-06-09T10:00:00Z"   // = max(updated_at) across the aggregate's members
}
```

`FormSchemaRequest` mirrors this plus a `base_version` (the version the client edited from); member
`uid`s are optional on create. No `active`/soft-delete field anywhere — see below.

### Pull

- Returns the aggregates changed since `last_sync` (≤ one per entityType), each as a whole document.
- The client **replaces** its local schema for that entityType with the returned aggregate. **Members
  absent from the server aggregate are removed locally — this is how deletions propagate (by absence).**
  No soft-delete flag, no in-band delete row, no "include deleted rows" feed.
- `last_sync` optional; absent/blank ⇒ full feed. Defaults `page=0, size=100`.

### Push (replace-aggregate, atomic + optimistic)

- Body = the changed aggregate(s). For each, the server **replaces the aggregate in one transaction**:
  upsert members present, **delete members absent**, then validate all invariants on the resulting state
  (every field in an existing section; STANDARD not deletable & essential not hideable; CHOICE /
  MULTI_CHOICE / CUSTOM invariants). Violations bubble to the global handler as `ApiResponse` errors.
- **Optimistic concurrency**: if `base_version` < the server's current `version`, the form changed under
  the client → reject with an `ApiResponse` conflict. The client re-pulls, re-applies its edits, and
  retries — so no edit is silently lost. On match, apply and bump `version`.
- Returns the server-resolved aggregate(s) with the new `version`; the client reconciles by `uid`.

### Why one feed (not row-level)

The schema is one aggregate, so it transfers as one unit. Delete-by-absence removes the entire
soft-delete mechanism, the sections-before-fields push ordering, and the dangling-`section_uid` window.
The trade-off is concurrency granularity: edits to the **same** entityType form resolve at whole-form
last-write-wins, guarded by the `version` stamp (acceptable for an admin-only, low-frequency config —
see FR-018). Editing different entityTypes never conflicts.

---

## 2. Read-only schema fetch (UI convenience)

```
GET /form/v1/config/schema?entity_type=customer
→ ApiResponse<FormSchemaResponse>
```

Same shape as a sync record. Seeds/merges registry defaults on first access for the workspace (FR-022,
non-destructive). This is the app's allowed UI-invoked read (not part of central sync).

---

## 3. Removed (no legacy compatibility)

Fresh setup, and the Angular web client is deprecated, so **no legacy endpoints are retained**:

```
GET/POST /form/v1/config/field-configs/sync           # removed
GET/POST /form/v1/config/attribute-definitions/sync   # removed
POST /form/v1/config/field-config | /attribute-definition | /config   # removed
DELETE /form/v1/config/field-config | /attribute-definition           # removed
```

The single `/config/schema/sync` (push/pull) plus the read-only `/config/schema` are the entire target
contract.

---

## Client (app) API surface

```
suspend fun getSchemaSync(lastSync: String, page: Int, size: Int,
                          sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<FormSchema>
suspend fun pushSchema(schemas: List<FormSchema>): List<FormSchema>
suspend fun getConfigSchema(entityType: String): FormSchema     // UI read
```

`FormSyncDelegate` drives the single aggregate feed under one `SyncEntity.FORM` checkpoint
(= `max(updated_at)` across aggregates). **Pull replaces** each local aggregate (deleting local members
absent from the server copy); **push** sends the dirty aggregate(s) with `base_version`; on a version
conflict it re-pulls, re-applies, and retries. Persistence stays relational (`form_schema` /
`form_section` / `form_field`) — the aggregate is assembled at the sync boundary; there is no per-row
soft-delete or `synced` flag (dirtiness is tracked per entityType aggregate).
