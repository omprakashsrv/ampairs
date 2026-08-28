# connector module

Apps & Extensions connector platform (spec `013-apps-extension-platform`). Owns the catalogue of
installable connectors (Tally first; Zoho/Salesforce/HubSpot later), per-workspace installations,
connection config + secrets, field mappings, sync checkpoints, and run history.

## Hosting model
- **Client-side connectors (Tally — priority)**: push/pull executes in the Ampairs desktop app; the
  backend stores config/mapping/checkpoints/runs and receives mapped data via the connector
  **sparse-upsert** endpoint (NOT the global `/sync`).
- **Server-side connectors**: deferred; the model accommodates them.

## Key rules specific to this module
- **Sparse upsert (the data-loss fix)**: writes apply only the columns *present* in each row's
  payload, intersected with the installation's mapping allowlist; omitted columns are preserved, an
  explicit null clears a mapped column, out-of-mapping keys are ignored. This is OFF the canonical
  `/sync` contract by design (like `tax`/`file`).
- **Cross-module writes** go through the `ConnectorEntityWriter` SPI (api package) implemented by
  each target module (customer/product/unit) — never reach into their repositories (Principle IX).
- **Identity match** is by `refId` (or `uid`) only — no business-key reconciliation; non-matching
  rows create new records.
- **Secrets** (`ConnectorConfig`) are encrypted at rest via `ConnectorSecretCipher`
  (`CONNECTOR_SECRET_KEY` env) and never returned to clients.

## Status (incremental delivery)
- ✅ Setup + Foundational + US1 (catalogue + install/uninstall).
- ✅ US2 (config + mapping), US3 (sparse upsert + checkpoints + runs), client migration.
- ✅ **Server-side execution (spec 029)**: `ServerSideConnectorSyncExecutor` SPI + `ConnectorRunScheduler`
  (cross-tenant `@Scheduled`, dispatches only `HostingType.SERVER_SIDE`) + `ConnectorHttpClient` +
  the **Generic HTTP/JSON** connector (`GenericHttpJsonExecutor`/`Provider`) + server-side connection
  test (`ConnectorConnectionTester`). Reuses the sparse-upsert path + encrypted secret store unchanged.
  Decrypted config for execution only via `ConnectorConfigService.resolveForExecution()`.
- ⏳ Server-side OAuth + webhooks (deferred, FR-S08); vendor providers; per-cycle pagination.
See `specs/013-apps-extension-platform/tasks.md` and `specs/029-connector-ui-server-side/`.
