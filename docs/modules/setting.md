# setting module

Central, workspace-scoped settings registry. Holds module-specific **display and
business-logic toggles** — e.g. "prices include tax", "show discount options on
invoices/orders" — that are read across the backend and synced to the mobile and web
clients. It is the single place to get and customize settings across modules.

## Design at a glance

- **Stores only overrides.** A `store_setting` row exists only when a workspace has
  changed a value from its default. The full catalog of *what settings exist* and their
  defaults lives in code (`SettingDefinition`s), not the DB.
- **Definitions live in `core`.** `SettingDefinition`, `SettingDefinitionProvider` and
  `SettingValueType` are in `com.ampairs.core.setting` (mirroring `SyncCheckpointContributor`),
  so any domain module declares its own settings without depending on the `setting` module.
- **Offline-first sync only.** The API is two bulk endpoints (pull/push) plus a read-only
  definition catalog — no per-record CRUD. Clients reconcile locally.

## REST Endpoints (`/setting/v1/settings`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/setting/v1/settings?last_sync=&page=&size=` | Incremental pull. Includes inactive rows so soft-deletes propagate. Returns `PageResponse<SettingResponse>`. |
| POST | `/setting/v1/settings` | Bulk upsert of `List<SettingRequest>`. Validates each value against its definition; soft-delete via `active=false`. Returns reconciled `List<SettingResponse>`. |
| GET | `/setting/v1/settings/definitions` | Definition catalog filtered to the current workspace's installed modules. |

Tenant context is set by `SessionUserFilter` from `X-Workspace-ID`.

## Key Entity

### StoreSetting

```kotlin
class StoreSetting : OwnableBaseDomain() {
    var moduleCode: String          // "invoice", "order", "common"
    var settingKey: String          // "show_discount_options"
    var value: String?              // stored as text, typed via valueType
    var valueType: SettingValueType // BOOLEAN | INT | DECIMAL | STRING | ENUM | JSON
    var active: Boolean             // soft-delete for sync
}
// Unique: (owner_id, module_code, setting_key)
```

## Definitions & the boundary

A value belongs in this module only if it is **workspace-scoped**, controls module
**display/business-logic behavior** (a toggle/choice, not domain records), is a **single
simple value** per key, has **no FK relationships**, and needs to be **read app-wide /
offline**. Collections, relational config, or workspace identity/branding stay in their
own modules (e.g. `tax`, `invoice` series, `workspace` settings).

Each owning module declares its settings via a `@Component SettingDefinitionProvider`:

```kotlin
@Component
class InvoiceSettingDefinitions : SettingDefinitionProvider {
    override fun definitions() = listOf(
        SettingDefinition(
            module = "invoice", key = "show_discount_options",
            valueType = SettingValueType.BOOLEAN, defaultValue = "true",
            label = "Show discount options on invoices",
            requiresModule = "invoice-billing", // gated by installed module
        ),
    )
}
```

`SettingServiceImpl` aggregates every provider (`List<SettingDefinitionProvider>`), validates
pushes against them, and resolves effective values (override ⊕ default). The `/definitions`
endpoint filters by `requiresModule` against `InstalledModulesProvider.enabledModuleCodes()`
(implemented in `workspace`).

Seeded settings: `common.prices_include_tax` (default `false`),
`invoice.show_discount_options` / `order.show_discount_options` (default `true`).

## Cross-module consumption

Other modules depend on the public interface, never the repository:

```kotlin
val inclusive = settingService.getBoolean("common", "prices_include_tax")
```

## Sync

- `SettingCheckpointContributor` reports `max(updatedAt)` under the mobile `SyncEntity` code
  `setting` for the connect/reconnect bootstrap.
- Successful upserts broadcast an `EntityChangedEvent("setting", …)` via `EntityChangePublisher`
  so other devices pull.
- Mobile maps this to `SyncEntity.STORE` (`feature/store`).

## Migrations

`V1.0.79__create_setting_module_tables.sql` in **both** `db/migration/mysql/` and
`db/migration/postgresql/`. The runtime/dev DB is PostgreSQL, so a Postgres migration is
required; always add new setting migrations in both vendor directories. The module is listed
in `migrationModules` in `ampairs_service/build.gradle.kts`.
