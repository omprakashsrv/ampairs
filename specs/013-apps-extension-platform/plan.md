# Implementation Plan: Apps & Extensions Connector Platform

**Branch**: `013-apps-extension-platform` | **Date**: 2026-06-15 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/013-apps-extension-platform/spec.md`

## Summary

Build a generic, multi-tenant **connector platform** that lets a workspace install, configure, map, and run external-system integrations (Tally first; Zoho/Salesforce/HubSpot later). The platform has two hosting types: **client-side** connectors (priority — Tally; all push/pull executes in the Ampairs desktop app) and **server-side** connectors (deferred). The new behaviour is that connector **configuration, field mapping, sync checkpoints, and run history are persisted to the backend** so setup survives reinstalls/devices, while the client keeps executing the sync.

Technical approach: introduce a new backend bounded-context module **`connector`** that owns the catalogue, per-workspace installations, secure configuration/credentials, per-installation field mappings, sync checkpoints, and run history — all over the canonical `/sync` REST style plus a new **connector-scoped sparse upsert** write path that applies a *per-row presence ∩ connector-mapping-allowlist* partial update (so unmapped/omitted columns are never nulled). On the KMP client, migrate the existing `tallysync` flow to read its config/mapping from the backend (mirroring the `StoreSettingsProvider` / `StoreSyncDelegate` pull pattern) and to report checkpoints/run-history back. The Angular web app gains an Apps catalogue + install + data-mapping UI (planned separately — repo not present in this environment).

This plan covers the **backend `connector` module** in full and specifies the **client (KMP)** and **web** workstreams at the interface level so `/speckit.tasks` can decompose all three.

## Technical Context

**Language/Version**: Backend — Kotlin 2.3 / Java 21 / Spring Boot 4.0. Client — Kotlin Multiplatform 2.4, Compose Multiplatform 1.11 (Tally execution is desktop/JVM only). Web — Angular 20 + Material 3 (separate repo).
**Primary Dependencies**: Backend — Spring Data JPA, Hibernate, Jackson (global SNAKE_CASE), Flyway. Client — Ktor (Tally XML), Room, Metro DI, existing `CentralSyncService`/`SyncDelegate`, DataStore.
**Storage**: PostgreSQL (runtime/dev) + MySQL (Flyway vendor parity) on the backend; Room (workspace-scoped) on the client. New `connector` tables are tenant-scoped (`OwnableBaseDomain`). Secrets stored encrypted at rest.
**Testing**: Backend — JUnit5 + Spring Boot Test + Testcontainers (`./gradlew testAll`, needs Docker). Client — KMP compile checks across targets (`compileKotlinIosSimulatorArm64`, `compileDebugKotlinAndroid`, `desktopApp:compileKotlin`) + existing sync tests.
**Target Platform**: Linux server (backend); Desktop JVM is the priority client host for Tally (Android/iOS hosting follows the same model but Tally XML is desktop-only today).
**Project Type**: Multi-repo — Spring Boot backend (this repo) + KMP client (`ampairs-app`) + Angular web (`ampairs-web`, separate).
**Performance Goals**: Incremental sync — a no-change cycle processes 0 records (SC-003); batched at 100 records/page consistent with the canonical sync pagination style (the connector data write uses the dedicated sparse-upsert endpoint, not the global `/sync`); connector catalogue/install/config endpoints are standard CRUD (<200ms p95).
**Constraints**: Mapped-fields-only, per-row presence-based partial update (no data loss, FR-018); global `/sync` contract for normal offline clients MUST remain unchanged (FR-018a); secrets never returned to clients (FR-008); multi-tenant isolation via `X-Workspace-ID` + `@TenantId`; timestamps `Instant`/`TIMESTAMPTZ`.
**Scale/Scope**: Per workspace: a handful of connector installations, each mapping ~6 entity types (Tally: customer, customer_group, product, product_catalog, unit, stock balance). Catalogue is small (single-digit connectors initially).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate | Status |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | All new timestamp fields (`lastSyncedAt`, run start/end, checkpoints) use `Instant`; migrations use `TIMESTAMPTZ` | PASS — designed in |
| II. DTO & Contract Isolation | All connector endpoints use request/response DTOs in `connector/domain/dto/`; no JPA entity exposed; secrets excluded from response DTOs | PASS — designed in |
| III. Global JSON SNAKE_CASE | No `@JsonProperty` for standard fields. **Exception risk**: the sparse-upsert payload must distinguish omitted vs null — documented, justified inline (see Complexity Tracking) | PASS w/ justification |
| IV. Multi-Tenant Isolation | New entities extend `OwnableBaseDomain`; tenant context set in controllers (try/finally); catalogue browse is workspace-aware | PASS — designed in |
| V. ApiResponse standardization | All endpoints return `ApiResponse<T>` / `ApiResponse<PageResponse<T>>` | PASS — designed in |
| VI. Centralized Exception Handling | No try/catch for business exceptions in controllers; typed domain exceptions bubble | PASS — designed in |
| VII. Efficient Data Loading | `@NamedEntityGraph` for installation→config→mappings bundles to avoid N+1 | PASS — designed in |
| VIII. Angular Material 3 (web) | Web mapping UI uses only Angular Material 3 (planned in web repo) | DEFERRED to web workstream |
| IX. Module Boundaries | New `connector` bounded context; cross-module reads (entitlement via `setting`/workspace-modules) go through public service interfaces, not repositories | PASS — designed in |
| X. Compose Multiplatform Parity | Client connector config/state read in `commonMain` via a provider mirroring `StoreSettingsProvider`; Tally XML execution stays desktop-only (documented platform deviation) | PASS w/ documented deviation |
| XI. Security & Secrets | Connector credentials encrypted at rest, never in source, never returned plaintext; encryption key via env var | PASS — designed in |

**Result**: No unjustified violations. One justified deviation (sparse payload, Principle III) tracked below.

## Project Structure

### Documentation (this feature)

```
specs/013-apps-extension-platform/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (OpenAPI for connector endpoints)
└── tasks.md             # Phase 2 output (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

Backend — new bounded-context module `connector` (mirrors existing module layout, e.g. `setting`/`customer`):

```
connector/
├── build.gradle.kts                         # new module (add to settings.gradle.kts,
│                                            #   migrationModules, ampairs_service deps)
└── src/main/kotlin/com/ampairs/connector/
    ├── domain/model/                        # JPA entities (OwnableBaseDomain)
    │   ├── ConnectorInstallation.kt
    │   ├── ConnectorConfig.kt               # secrets encrypted
    │   ├── ConnectorFieldMapping.kt
    │   ├── ConnectorSyncCheckpoint.kt
    │   └── ConnectorSyncRun.kt
    ├── domain/dto/                          # request/response DTOs + converters
    ├── domain/catalogue/                    # code-defined Connector catalogue (Tally def + default mapping)
    ├── repository/                          # Spring Data JPA repositories
    ├── service/                             # install/config/mapping/checkpoint/run + sparse-upsert merge
    ├── controller/                          # REST: catalogue, install, config, mapping, sync-state, run, sparse upsert
    ├── config/                              # module config, secret encryption
    └── src/main/resources/db/migration/{postgresql,mysql}/   # Flyway V{semver}__connector_*.sql
```

Client (`ampairs-app`, separate repo) — migrate `tallysync` onto backend-persisted config:

```
feature/connector/ (new)  or extend feature/store pattern
├── commonMain/  ConnectorConfigProvider (mirrors StoreSettingsProvider),
│                ConnectorSyncDelegate (mirrors StoreSyncDelegate, SyncEntity.CONNECTOR),
│                ConnectorRepository + Room entities (config/mapping/checkpoint/run cache)
shared/src/desktopMain/com/ampairs/tallysync/
└── TallySyncScheduler / TallySyncService  → read config+mapping from ConnectorConfigProvider
   instead of AppPreferencesDataStore; report checkpoints + run history to backend
```

Web (`ampairs-web`, separate repo — NOT present locally): Apps catalogue, install flow, connection config form, and data-mapping editor under the workspace-settings/module-management area (planned against that repo separately).

**Structure Decision**: A new backend `connector` module is the system of record (Principle IX — new bounded context). The client reuses the proven server-driven-config pull pattern (`StoreSettingsProvider`/`StoreSyncDelegate`) so Tally execution stays where it is (desktop) while config/mapping/state move server-side. The data write path adds a connector-scoped sparse-upsert endpoint and leaves the global `/sync` contract untouched (FR-018a).

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Sparse/map-style payload for connector sparse-upsert (deviates from fixed-DTO + global SNAKE_CASE norm, Principle III) | FR-018/FR-018c require distinguishing "column omitted" (skip) from "column present = null" (intentional clear) per row; a fixed DTO serializes unset fields as null, which is indistinguishable and causes data loss | Fixed request DTO can't express per-row column presence; null-means-skip can't clear a value; client field-mask was rejected by stakeholder in favour of presence. Confined to the connector write path only; global `/sync` DTOs unchanged. |
| New `connector` module rather than extending `setting` | Connector is a distinct bounded context (installations, credentials, mappings, sync runs) with its own lifecycle; Principle IX requires new bounded contexts get their own module | Folding into `setting` would overload a config-registry context with execution/credential/run-history concerns |
</content>
