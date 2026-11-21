# Quickstart: Apps & Extensions Connector Platform

How to stand up and verify the new `connector` backend module, then wire the client. Paths are relative to repo roots (`/home/user/ampairs` backend, `/home/user/ampairs-app` client).

## 1. Register the new backend module

1. `settings.gradle.kts` → add `include("connector")`.
2. `ampairs_service/build.gradle.kts`:
   - add `implementation(project(mapOf("path" to ":connector")))` to dependencies;
   - add `"connector"` to the `migrationModules` list.
3. Create `connector/build.gradle.kts` by copying `customer/build.gradle.kts` (Spring Boot library jar: `bootJar` disabled, `jar` enabled, `api(project(":core"))`, allOpen for JPA, group `com.ampairs`).
4. Package root: `connector/src/main/kotlin/com/ampairs/connector/` with `controller/ service/ repository/ domain/{model,dto,catalogue} config/` (mirrors `setting`/`customer`).

## 2. Entities + migrations

- Entities extend `OwnableBaseDomain` (see `data-model.md`): `ConnectorInstallation`, `ConnectorConfig`, `ConnectorFieldMapping`, `ConnectorSyncCheckpoint`, `ConnectorSyncRun`.
- Flyway: write **both** vendors — `connector/src/main/resources/db/migration/postgresql/V1.0.0__connector_tables.sql` and `.../mysql/V1.0.0__connector_tables.sql` (`TIMESTAMPTZ` on Postgres, `TIMESTAMP` on MySQL; `?serverTimezone=UTC`). Check `./gradlew :ampairs_service:flywayInfo` before picking the version.

## 3. Catalogue + entitlement gating

- Define the Tally `CatalogueConnector` as a code-defined provider (like `SettingDefinitionProvider`): `type="tally"`, `hosting_type=CLIENT_SIDE`, `supported_entities=[customer, customer_group, product, product_catalog, unit, stock_balance]`, `supported_directions=[INBOUND]`, default mapping template.
- Gate catalogue visibility via the existing `InstalledModulesProvider.enabledModuleCodes()` + `MasterModule.requiredTier` (`SubscriptionTier`). Register a `connector` MasterModule with the desired `requiredTier` and `dependencies` (e.g. `customer-management`, `product-management`).
- Note: `SubscriptionAddon.TALLY_INTEGRATION` already exists — reuse it for Tally entitlement rather than inventing a new flag.

## 4. Controllers + tenant context

- All endpoints under `/connector/v1/...` (see `contracts/connector-api.yaml`).
- Set tenant context in each controller method: `TenantContextHolder.setCurrentTenant(workspaceId)` in try/finally (workspace from `X-Workspace-ID` via `SessionUserFilter`). Services never set it.
- Return `ApiResponse.success(...)`; paginated → `ApiResponse.success(PageResponse.from(page) { it.asResponse() })`. Secrets excluded from `ConfigResponse`.

## 5. Sparse upsert service (the data-loss fix)

`POST /connector/v1/installations/{uid}/data/{entity_type}/upsert` body = `List<SparseUpsertRow>` where each row has `ref_id` + `values: Map<column, value?>`.

Per row (reusing the `ProductService.updateProducts()` load-existing-then-copy pattern):
1. Resolve the installation's mapped allowlist for `entity_type` (`ConnectorFieldMapping.rules` where `unmapped=false`).
2. `writable = values.keys ∩ allowlist`; ignore keys outside the allowlist.
3. Load existing target-entity row by `refId` (within workspace); if none, create.
4. Apply ONLY `writable` columns — **key present** ⇒ write that value (including explicit null = clear); **key absent** ⇒ leave column untouched.
5. Save; broadcast via `EntityChangePublisher`. Return per-row `applied_columns` + outcome.

> CRITICAL: do NOT reuse the global `/{module}/v1/{resource}/sync` fixed-DTO upsert here — a fixed DTO can't tell "omitted" from "null" and would null unmapped columns (the bug this feature fixes). Use the sparse `Map` body. The global `/sync` contract stays unchanged (FR-018a).

## 6. Verify (backend)

```bash
./gradlew :ampairs_service:flywayInfo          # confirm V1.0.0__connector_* picked up
./gradlew :connector:compileKotlin
./gradlew testAll                               # needs Docker (Testcontainers)
```

Acceptance smoke (maps to spec SCs):
- Install Tally → installation row `NEEDS_CONFIG` (US1).
- PUT config + mapping → persisted; secrets not returned (US2, FR-008).
- Sparse upsert row1 `{ref_id:1, values:{column1, column2}}` and row2 `{ref_id:2, values:{column3, column4}}` → row1 writes only column1/2, row2 only column3/4, all other columns on each record unchanged (US3, SC-004, FR-018).
- Re-send a row omitting a previously-set column → that column retains its value (no data loss). Send the column with explicit null → it clears.
- PUT checkpoint, POST run → readable via `/runs` (US3/US4 observability).

## 7. Client wiring (ampairs-app — separate repo)

- Add `SyncEntity.CONNECTOR`; create `ConnectorSyncDelegate` (mirror `StoreSyncDelegate`) pulling `/connector/v1/sync` into a workspace-scoped Room cache; expose `ConnectorConfigProvider` (mirror `StoreSettingsProvider`).
- Repoint `shared/src/desktopMain/com/ampairs/tallysync/TallySyncScheduler` + `TallySyncService` to read host/port/mapping/watermark from `ConnectorConfigProvider` instead of `AppPreferencesDataStore`; after each Tally cycle, push business data via the sparse-upsert endpoint and report checkpoint + run to the backend.
- Auto-start: when the client discovers an `ENABLED` Tally installation from the pulled config, start background sync (FR-023) from the backend-persisted checkpoint.
- Validate all targets: `./gradlew shared:compileKotlinIosSimulatorArm64 androidApp:compileDebugKotlinAndroid desktopApp:compileKotlin`.

## 8. Web (ampairs-web — separate repo, not in this environment)

Apps catalogue + install + connection-config form + data-mapping editor under workspace-settings/module-management, Angular Material 3 only. Plan against the web repo separately.
</content>
