# Client (ampairs-app) Integration — Status & Remaining

The client work lives in the **ampairs-app** repo, branch `claude/compassionate-carson-p1dvr6`.

> **Environment note:** the app's KMP build requires the JetBrains Runtime toolchain, which cannot be
> provisioned in the Claude Code remote environment (foojay.io returns 403). So client code here is
> written to mirror existing patterns exactly but is **verified by the app's CI / a dev machine**, not
> compiled locally.

## Done (committed & pushed)

- `SyncEntity.CONNECTOR("connector")` added (`data/sync`).
- `ApiUrlBuilder.connectorUrl(path)` added (`data/common`).
- New `feature/connector` module (lean: API + DTOs + provider; no Room yet), in `settings.gradle.kts`:
  - `domain/ConnectorModels.kt` — DTOs (installation, config, mapping, sparse-upsert row/result, checkpoint, run, connection-test).
  - `data/api/ConnectorApi` + `ConnectorApiImpl` — backend `/connector/v1` client: `installations`, `config`, `mappings`, `upsert` (sparse data push), `putCheckpoint`, `recordRun`, `testConnection`. Mirrors `StoreSettingApiImpl`.
  - `domain/ConnectorConfigProvider` — reads the Tally installation + `host`/`port` + mappings from the backend (the new source of truth, replacing DataStore-only config).

## Remaining (Tally repoint — the final integration)

This changes existing behavior in `shared/src/desktopMain/.../tallysync/`, so do it with the app build available to iterate.

1. **Resolve the Tally installation + config from the backend** (instead of `AppPreferencesDataStore`):
   - In `TallySyncScheduler.runOnce` / `TallySyncService`, inject `ConnectorConfigProvider`.
   - `val inst = provider.installation("tally") ?: return` (skip if not installed/enabled).
   - `host = provider.host(inst.uid)`, `port = provider.port(inst.uid)` — fall back to DataStore during migration.

2. **Push mapped data via the sparse-upsert endpoint** (instead of `markPendingPush`):
   - After mapping each Tally entity, build `List<SparseUpsertRow>` where `values` is a `JsonObject` of
     only the columns this row provides (presence = intent), and call
     `connectorApi.upsert(inst.uid, entityType, rows)` for `customer`, `customer_group`, `product`,
     `product_group`, `product_category`, `unit`, `stock_balance`.
   - Use the backend-pulled `mappings(inst.uid)` to decide which Ampairs field each Tally field maps to
     (or trust the backend allowlist — the server intersects anyway).
   - Replace the `centralSyncService.markPendingPush(SyncEntity.X)` calls with these upserts.

3. **Report checkpoint + run** after each cycle: `connectorApi.putCheckpoint(...)` (advance the alterId
   watermark) and `connectorApi.recordRun(...)` (counts/status) so progress is backend-persisted.

4. **Connection test (FR-009, G2):** the desktop app tests Tally host:port reachability and reports the
   result via `connectorApi.testConnection(inst.uid, ConnectionTestRequest(ok, message))`.

5. **Auto-start + status UI (T042/T042a):** scheduler auto-starts when `provider.installation("tally")`
   is `ENABLED`; surface installation `status` + last `recordRun` result in the Tally settings screen.

6. **Retire DataStore config (T048):** once config is read from the backend, migrate existing
   `getTallyHost/Port` + `getTallyLastAlterId` into the backend config/checkpoints on first run, then
   remove the DataStore keys.

7. **(Optional) Offline mirror:** add a Room-backed `ConnectorSyncDelegate` (mirror `StoreSyncDelegate`,
   `SyncEntity.CONNECTOR`) + `ConnectorDatabase` so config/mapping/checkpoints are cached locally and
   the connector status survives offline. Not required for the functional push path above.

## Web (ampairs-web) — out of scope here

Catalogue + install + connection-config form + data-mapping/formatting editor (Angular Material 3),
consuming the same `/connector/v1` endpoints. The web repo is not present in this environment.
