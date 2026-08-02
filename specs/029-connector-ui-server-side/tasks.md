# Tasks: Connector UI & Server-Side Execution (spec 029)

**Stacked on**: `013-apps-extension-platform` (must build against the connector platform, not `main`).
**Repos**: `ampairs` (backend), `ampairs-app` (client). Backend builds/tests locally (JDK 21); client relies on CI.

Legend: `[P]` parallelizable · effort S/M/L.

---

## Phase A — Generic Connector UI (client: `feature/connector`, `commonMain`, Metro/MVI)

Serves BOTH hosting types. Turns `feature/connector` (today pure data/sync plumbing) into a real feature with a `ui/` layer.

- **T001** (S) Add `ConnectorRoute` sealed `NavKey` (Catalogue, Detail(uid), Config(uid), Mapping(uid), Runs(uid)) + `ConnectorEntryProvider`; register in `CombinedEntryProvider`. Add a top-level `Route.Connectors` and a workspace-modules/nav entry point.
- **T002** [P] (M) `ConnectorCatalogueViewModel` + `ConnectorCatalogueScreen` — `GET /catalogue`, installed-state merge, hosting-type badge, install (`POST /installations`) / uninstall (`DELETE`). `@ContributesIntoMap(WorkspaceScope)`.
- **T003** [P] (M) `ConnectorConfigViewModel` + `ConnectorConfigScreen` — dynamic form from `connectionSchema` (non-secret + masked secret fields); save via `updateConfig()` (incl. `secretValues`); show `secretKeysSet` for set secrets. **Fixes the local-DataStore gap.**
- **T004** (S) Test Connection wiring — SERVER_SIDE → `connectorApi.testConnection()`; CLIENT_SIDE → keep local ping. Surface `last_validated_at` / error.
- **T005** [P] (M) `ConnectorMappingViewModel` + `ConnectorMappingScreen` — **T028b**: per-entity list of `FieldMappingRuleDto` (external → Ampairs field + transform), add/edit/remove, reset-to-catalogue-default; save via `PUT /mappings`.
- **T006** [P] (M) `ConnectorRunsViewModel` + status/runs screen — status, pause/resume, run history (`GET /runs`), checkpoints, last error. (Absorbs today's status card.)
- **T007** (M) Point the desktop `TallySettingsScreen` config/mapping at the generic UI/backend (`updateConfig` instead of local DataStore); keep local-Tally-ping + tax-import specifics desktop-only.
- **T008** (S) commonMain strings (`composeResources/values/strings.xml`), accessibility (icon `contentDescription`), `collectAsStateWithLifecycle`.
- **T009** (S) Compile-validate all targets on CI: `androidApp:compileDebugKotlinAndroid`, `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`.

## Phase B — Server-Side Execution Engine (backend: `connector` module) ✅ DONE

Makes `HostingType.SERVER_SIDE` real, reusing the writer SPI + sparse-upsert + secret cipher unchanged.

- ✅ **T010** `ServerSideConnectorSyncExecutor` SPI (`service/ServerSideConnectorSyncExecutor.kt`): `connectorType`, `runCycle(installation): ServerSyncOutcome`, optional `testConnection(installation)`. Registered by type in a Spring map.
- ✅ **T011** `ConnectorRunScheduler` (`@Scheduled`; `@EnableScheduling` already app-wide) — selects active ENABLED **SERVER_SIDE** installations across tenants via native `findAllEnabledAcrossTenants()` (bypasses `@TenantId`), sets tenant context per row, dispatches by type; per-installation error isolation; records FAILED runs for pre-upsert entity errors. **CLIENT_SIDE never dispatched (FR-S02).**
- ✅ **T012** `ConnectorHttpClient` — `RestClient` GET + header auth (from decrypted secrets) + dotted JSON records-path extraction + a reachability `probe()`. Secrets never logged (FR-S05).
- ✅ **T013** `GenericHttpJsonExecutor` — per supported entity with a `path.{entity}` + mapping: fetch → map rows via stored `FieldMapping` (presence-preserving) → `ConnectorSparseUpsertService.upsert(..., "SCHEDULED")` → advance checkpoint on clean cycle. Optional `since_param` incremental. Config resolved via new `ConnectorConfigService.resolveForExecution()` (decrypted, server-execution-only).
- ✅ **T014** `GenericHttpJsonConnectorProvider : ConnectorCatalogueProvider` — `hostingType = SERVER_SIDE`, schema = base_url + api_key (secret) + auth header/scheme + per-entity path; default mapping (customer/product); `INBOUND`.
- ✅ **T015** Server-side connection test — `ConnectorConnectionTester` routes `POST /config/test` by hosting type: SERVER_SIDE probes via the executor and stores `last_validated_at` (FR-S06); CLIENT_SIDE records the client-reported result (unchanged).
- ✅ **T016** Tests (13, green): `GenericHttpJsonExecutorTest` (mapping apply, presence-scoping, checkpoint advance/no-advance, fetch-error capture, no-refId drop), `ConnectorRunSchedulerTest` (dispatch only SERVER_SIDE, disabled no-op, failure recording), `ConnectorConnectionTesterTest` (hosting-type routing).
- ✅ **T017** `:connector:compileKotlin` + `:connector:test` green locally.

**Deferred within Phase B (follow-ups):** per-cycle pagination loop / `hasNext` (currently one bounded GET per entity), OAuth + webhooks (FR-S08), vendor providers.

## Phase C — Client-side Tally completeness (parallel, smaller — optional in this phase)

- **T018** [P] (S) `supplier` connector writer (`AbstractRefIdEntityWriter`) + test — closes the clearest master-data gap.
- **T019** [P] (M) Pricing/cost child DSL writers: `price_list`, `product_standard_cost`, `unit_conversion` (`DslEntityWriter`), + seed mappings in `TallyConnectorProvider`.
- **T020** [P] (M) Mapping-aware client push + data-driven transforms (replace hardcoded `TallyProductMapper`/`TallyCustomerMapper` reads with the stored mapping) — lets the T005 editor actually change client behavior.

## Cross-cutting / docs

- **T021** (S) `plan.md` + `data-model.md` deltas for spec 029 (executor SPI, generic provider config shape). No new tables — reuses 013's.
- **T022** (S) Update `013` FR-H05 note to reference 029 as the landing spec for server-side.

---

## Sequencing

1. **T001–T003, T005** first — the config + mapping UI you asked about (unblocks both hosting types; immediately improves Tally).
2. **T010–T014** in parallel once the sparse-upsert reuse pattern from A is clear.
3. **T004/T006/T007/T015** integrate the two.
4. **Phase C** trails / optional.

## Validation gates
- Backend: `:connector:compileKotlin` + `:connector:test` (and `:product:test` if writers touched) locally.
- Client: CI green (coverage-bot comment = compile + tests passed).
