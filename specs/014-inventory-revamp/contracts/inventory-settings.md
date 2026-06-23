# Contract: Inventory Policy via the Central `setting` Module

Inventory policy is **not** a dedicated inventory entity or endpoint. It is five workspace settings managed
by the existing `setting` module (backend) and `feature/store` (mobile). This contract pins the keys,
types, defaults, gating, and read paths. See research R11.

## Setting definitions (namespace `inventory`)

Declared by `@Component InventorySettingDefinitions : SettingDefinitionProvider` in
`ampairs/product/.../inventory/config/`. All `requiresModule = "inventory-management"` (only surface when
the module is installed). Definitions are **code-based** — no Flyway migration defines them.

| key | value_type | default | label (UI) |
|---|---|---|---|
| `auto_deduct_on_order` | BOOLEAN | `true` | Auto-deduct stock on sale |
| `block_orders_when_out_of_stock` | BOOLEAN | `false` | Block sales when out of stock |
| `allow_negative_stock` | BOOLEAN | `false` | Allow negative stock |
| `allow_manual_override` | BOOLEAN | `true` | Allow manual stock override |
| `enable_low_stock_alerts` | BOOLEAN | `true` | Low-stock alerts |

## Transport (reuses the setting module's existing endpoints — nothing new)

- Definitions (pull-only catalog, filtered to installed modules):
  `GET /setting/v1/definitions` → includes the `inventory/*` rows above.
- Overrides (read-write, offline-sync): `GET/POST /setting/v1/settings/sync`
  (`ApiResponse<PageResponse<SettingResponse>>` / `ApiResponse<List<SettingResponse>>`). An override row is
  `(module='inventory', key, value, value_type='BOOLEAN', active)`.
- Mobile sync entity: **`SyncEntity.STORE`** (existing `StoreSyncDelegate`). No `INVENTORY_CONFIG`.

## Read paths (effective value = override ⊕ default)

- **Backend**: `settingService.getBoolean("inventory", "auto_deduct_on_order")` inside
  `InventoryStockService` (policy gate) and the low-stock scheduler (`enable_low_stock_alerts`). Precedent:
  `payment`/`invoice`/`common` providers.
- **Mobile**: inject `StoreSettingsProvider`; `getBoolean("inventory", key, default)` for one-shot reads,
  `observeBoolean("inventory", key, default)` for reactive UI. Precedent: `InvoiceViewModel` reading
  `common/prices_include_tax`. Defaults supplied at call site so behavior is correct offline before any
  override syncs.

## UI

No inventory-specific settings screen. The existing generic `feature/store` settings screen renders these
toggles automatically (module filter shows "inventory" once definitions are pulled for an installed
workspace) and writes overrides through the existing `StoreSettingRepository` → `SyncEntity.STORE` push.

## Migration

Retire `InventoryConfig` entity/`inventory_config` table/`InventoryConfigService`/`/config/sync`. Optional
one-time Flyway **data** migration: for each existing `inventory_config` row, upsert five `store_setting`
rows (`module_code='inventory'`, one per key, value = the legacy boolean as `"true"/"false"`) before
dropping `inventory_config`. Workspaces with no legacy config need no backfill (defaults apply).

## Conformance checklist

- [ ] `InventorySettingDefinitions` provider declares all 5 keys with `requiresModule="inventory-management"`.
- [ ] Backend policy reads go through `SettingService` (no `InventoryConfigService`, no `inventory_config`).
- [ ] Mobile reads go through `StoreSettingsProvider` (no inventory config entity/delegate/screen).
- [ ] No `INVENTORY_CONFIG` SyncEntity; no `/inventory/v1/config/sync`.
- [ ] Legacy `inventory_config` retired (+ optional backfill into `store_setting`).
