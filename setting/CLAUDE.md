# setting module

Central workspace settings registry — module-specific display/business-logic toggles
(e.g. tax-inclusive pricing, show discount options) consumed across modules and synced
to the mobile/web clients.

## Key entities
- `StoreSetting : OwnableBaseDomain` — `moduleCode`, `settingKey`, `value` (text), `valueType` (enum), `active`. One row per `(owner_id, module_code, setting_key)`. Stores only **overrides**; defaults come from definitions.

## Definitions (in code, not the DB)
- `SettingDefinition` / `SettingDefinitionProvider` / `SettingValueType` live in **`core.setting`** (like `SyncCheckpointContributor`), so any module declares its own settings without depending on `setting`.
- Each owning module registers a `@Component SettingDefinitionProvider` (e.g. `InvoiceSettingDefinitions`, `OrderSettingDefinitions`); cross-cutting keys live in `setting`'s `CommonSettingDefinitions`.
- `SettingDefinition.requiresModule` gates a setting by an installed module code (`null` = always). `WorkspaceInstalledModulesProvider` (in `workspace`, implements `core.setting.InstalledModulesProvider`) resolves the workspace's enabled modules.

## API (offline-first: bulk sync only — no per-record CRUD)
`/setting/v1/settings/**`
- `GET /setting/v1/settings?last_sync=&page=&size=` — incremental pull (includes inactive rows so deletes propagate)
- `POST /setting/v1/settings` — bulk upsert (validates each value against its definition; soft-delete via `active=false`)
- `GET /setting/v1/settings/definitions` — definition catalog filtered to the workspace's installed modules

## Cross-module reads
Other modules read effective values via `SettingService.getBoolean/getString(module, key)` (override → definition default).

## Sync
`SettingCheckpointContributor` contributes `max(updatedAt)` under entity code `setting`; writes broadcast via `EntityChangePublisher` for multi-device pull. Mobile `SyncEntity.STORE` maps to this.

## Migrations
`V1.0.79` (both `mysql/` and `postgresql/`). Add new migrations in BOTH vendor dirs.

## Full docs
`docs/modules/setting.md`
