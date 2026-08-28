# Feature Specification: Connector UI & Server-Side Execution

**Feature Branch**: `029-connector-ui-server-side` (stacked on `013-apps-extension-platform`)
**Created**: 2026-08-02
**Status**: Draft
**Depends on**: `013-apps-extension-platform` (connector platform: catalogue, installations, config, mapping, sparse-upsert, checkpoints, runs, `ConnectorEntityWriter` SPI, `ConnectorSecretCipher`)

**Input**: User direction: "Start with both client and server side connector. Is this fully ready even, with UI? If UI is ready for configuration or mapping for server side, let us know." — followed by "plan the next phase of development."

## Overview

Spec 013 delivered the connector platform's **backend system-of-record** and the **client-side (Tally) sync engine**, but with two gaps:

1. **No generic connector UI.** The only connector UI is a desktop-only, Tally-specific settings screen that bypasses the project's Metro/MVI pattern and writes its config to local DataStore instead of the backend. There is no catalogue/browse, no install flow, no **field-mapping editor** (T028b), and no way to enter **secret credentials**.
2. **No server-side execution.** `HostingType.SERVER_SIDE` exists as an enum value that no code path reads. There is no scheduler, no outbound API client, no OAuth, and no webhook receiver. Per 013 FR-H05 this was **explicitly deferred** — this spec begins it.

This feature closes both gaps by building **one shared, hosting-type-agnostic connector UI** in `feature/connector` (`commonMain`, Metro/MVI) that both client-side and server-side connectors use, plus a **server-side execution engine** with a **generic HTTP/JSON reference provider** (API-key auth) that pulls from a configurable REST endpoint, maps, and writes via the existing connector sparse-upsert path.

The generic HTTP/JSON provider is deliberately the first server-side connector: it needs no vendor SDK, proves the whole server-side path end-to-end (schedule → fetch → map → sparse-upsert → record run), and becomes the template every future vendor provider (Zoho, Shopify, QuickBooks) copies. OAuth and webhooks remain out of scope for this phase (they are additive to this same engine).

## Clarifications

### Session 2026-08-02

- Q: How far should server-side go in this phase? → A: **Skeleton + one working provider.** Executor SPI, scheduler, hosting-type dispatch, and one real end-to-end provider using API-key auth. No OAuth, no webhooks yet.
- Q: Which remote system for the first server-side provider? → A: **Generic HTTP/JSON connector** — a configurable REST-endpoint + API-key connector (base URL, auth header, per-entity JSON path). Template for future vendor providers.
- Q: Where does the mapping/config UI live and which hosting types does it serve? → A: A **single generic connector UI** in `feature/connector` (`commonMain`), driven by each connector's `connectionSchema` and mapping catalogue, serving **both** CLIENT_SIDE and SERVER_SIDE. The Tally desktop screen is pointed at it for config/mapping; local-Tally-ping specifics stay desktop-only.
- Q: Where do stored server-side credentials go? → A: The existing **`ConnectorSecretCipher` (AES-256-GCM) + write-only secret handling** in `ConnectorConfigService` — the config UI's secret fields write to `secretValuesEncrypted`; secret values are never returned to clients (only `secretKeysSet`).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse and install a connector from the app (Priority: P1)

A workspace administrator opens **Apps & Extensions** inside the Ampairs app, sees the catalogue of available connectors (from the backend `GET /connector/v1/catalogue`) with a **hosting-type badge** (Client-side / Server-side), installs one, and sees it move to "installed — needs configuration".

**Why this priority**: Today installation is assumed to already exist backend-side; there is no in-app install flow. This is the entry point for every connector, both hosting types.

**Independent Test**: Browse catalogue, install a connector, confirm it appears installed for that workspace only and persists across app restarts; uninstall returns it to the catalogue.

**Acceptance Scenarios**:
1. **Given** a workspace with no connectors, **When** the admin opens Apps & Extensions, **Then** available connectors are listed with name, description, hosting-type badge, and supported entities.
2. **Given** a catalogue connector, **When** the admin installs it, **Then** a per-workspace installation is persisted server-side and shown as "needs configuration".
3. **Given** an installed connector, **When** the admin uninstalls it, **Then** the installation, config, and sync state are removed and sync stops.

### User Story 2 - Configure connection details, including secrets (Priority: P1)

The admin opens an installed connector's **Configuration** screen. The form is **generated from the connector's `connectionSchema`** — non-secret fields (Tally host/port) and secret fields (an HTTP/JSON connector's base URL + API key). Saving persists to the backend (`PUT /config`); secret values are encrypted at rest and never returned.

**Why this priority**: A server-side connector cannot authenticate without a credential-entry form, and today's Tally config wrongly writes to local DataStore. This is the missing configuration surface for both hosting types.

**Acceptance Scenarios**:
1. **Given** a connector with a `connectionSchema` containing a secret field, **When** the admin views config, **Then** a masked input is rendered and, once set, the UI shows the key as "set" without revealing the value.
2. **Given** entered config, **When** the admin saves, **Then** non-secret values persist as JSON and secret values persist encrypted (`secretValuesEncrypted`), scoped to the workspace.
3. **Given** a configured connector, **When** the admin runs **Test Connection**, **Then** for SERVER_SIDE the backend performs the reachability test and stores `last_validated_at`; for CLIENT_SIDE the client tests locally and reports the result (unchanged from 013).

### User Story 3 - View and edit the field mapping (Priority: P1)

The admin opens the **Mapping** editor for an installed connector, sees the seeded default mapping per supported entity (external field → Ampairs field, with an optional transform), and can add/edit/remove rows and reset an entity to the catalogue default. Changes persist to `PUT /mappings`.

**Why this priority**: This is task T028b, unbuilt in 013. Without it the mapping is invisible and uneditable; it is required for both hosting types.

**Acceptance Scenarios**:
1. **Given** an installed connector, **When** the admin opens Mapping, **Then** each supported entity's rows are listed (external field, Ampairs field, transform).
2. **Given** the mapping editor, **When** the admin edits a row and saves, **Then** the change persists via `PUT /mappings` and is reflected on reload.
3. **Given** an edited entity mapping, **When** the admin chooses "reset to default", **Then** the catalogue default mapping for that entity is restored.

### User Story 4 - Server-side connector runs automatically (Priority: P1)

An admin installs and configures the **Generic HTTP/JSON** (server-side) connector with a base URL, API key, and per-entity JSON paths. Without any client app running, the backend **on a schedule** fetches each mapped entity from the remote endpoint, applies the connector's mapping, writes the mapped-only columns via the sparse-upsert path, advances checkpoints, and records a run.

**Why this priority**: This is the defining server-side capability — sync that runs on the server for an internet-reachable system, with no client involvement.

**Acceptance Scenarios**:
1. **Given** an active, configured SERVER_SIDE installation, **When** the scheduler cycle runs, **Then** the backend fetches, maps, and sparse-upserts each supported entity and records a `SUCCESS`/`PARTIAL`/`FAILED` run.
2. **Given** a fetched row matching an existing record by `refId`/`uid`, **When** it is written, **Then** only mapped ∩ present columns are updated and unmapped columns are preserved (same guarantee as the client-side path).
3. **Given** a remote endpoint that is unreachable or returns an error, **When** the cycle runs, **Then** the run is recorded `FAILED` with an error detail and the checkpoint is not advanced.
4. **Given** an installation that is `PAUSED`, **When** the scheduler cycle runs, **Then** it is skipped.

### Edge Cases
- Secret rotation: re-saving config with a new API key replaces the encrypted secret without exposing the old one.
- Partial fetch failure across entities: entities that succeeded advance their checkpoints; failed entities do not; run is `PARTIAL`.
- A CLIENT_SIDE connector must never be picked up by the server-side scheduler (dispatch strictly on `hostingType`).
- Rate-limited/paginated remote responses: the provider honors a page cursor from config and stops on `hasNext == false` (bounded per cycle, consistent with 013's batch caps).

## Requirements *(mandatory)*

### Functional Requirements — Connector UI (both hosting types)

- **FR-U01**: The app MUST provide an "Apps & Extensions" catalogue screen listing connectors from `GET /connector/v1/catalogue`, with hosting-type badge and installed state, and install/uninstall actions.
- **FR-U02**: The app MUST render a **configuration form generated from the connector's `connectionSchema`**, supporting non-secret and secret fields, persisting to `PUT /config` (secrets via `secretValues`).
- **FR-U03**: The config UI MUST NOT display stored secret values; it MUST show only which secret keys are set.
- **FR-U04**: The app MUST provide a **field-mapping editor** (view/add/edit/remove rows per entity, reset-to-default), persisting to `PUT /mappings` (T028b).
- **FR-U05**: The app MUST provide a connector **status/runs** screen (status, pause/resume, run history, checkpoints, last error).
- **FR-U06**: All connector UI MUST live in `feature/connector` `commonMain` using Metro/MVI (ViewModels `@ContributesIntoMap(WorkspaceScope)`), reachable via a `commonMain` nav route on all platforms.
- **FR-U07**: Test Connection MUST call the platform `POST /config/test` for SERVER_SIDE; CLIENT_SIDE keeps the local-ping path.
- **FR-U08**: The existing Tally desktop screen MUST source its config and mapping from the generic UI/backend (not local DataStore), retaining only local-Tally-ping specifics.

### Functional Requirements — Server-Side Execution

- **FR-S01**: The backend MUST define a `ServerSideConnectorSyncExecutor` SPI (one implementation per server-side connector type) that performs one incremental sync cycle for an installation.
- **FR-S02**: A backend scheduler MUST periodically select active, non-paused **SERVER_SIDE** installations and dispatch each to its executor; CLIENT_SIDE installations MUST never be dispatched.
- **FR-S03**: Executors MUST write mapped data through the **existing connector sparse-upsert path** (`ConnectorEntityWriter` + presence ∩ mapping-allowlist), inheriting the data-loss-safe guarantee unchanged.
- **FR-S04**: Executors MUST advance per-entity checkpoints on success and record a run (`SUCCESS`/`PARTIAL`/`FAILED`) via the existing sync-state services; a failed entity MUST NOT advance its checkpoint.
- **FR-S05**: Executors MUST read credentials from the encrypted secret store; secrets MUST NOT be logged.
- **FR-S06**: For SERVER_SIDE connectors, `POST /config/test` MUST perform an actual server-side reachability test and store `last_validated_at`.
- **FR-S07**: A **Generic HTTP/JSON** server-side connector MUST be provided as a `ConnectorCatalogueProvider` with `hostingType = SERVER_SIDE`, a `connectionSchema` of base URL (non-secret) + API key (secret) + per-entity JSON path/pagination config, and a default mapping template.
- **FR-S08 (deferred, documented)**: OAuth authorize/callback/refresh and webhook receivers are out of scope for this phase; the executor SPI and secret store MUST accommodate them without re-architecting.

### Key Entities
- **ServerSideConnectorSyncExecutor** (new SPI): `entityTypesFor(installation)`, `runCycle(installation): RunResult` — server-side analogue of the client's `TallySyncService`, reusing writers/checkpoints/runs.
- **GenericHttpJsonConnector** (new provider + executor): configurable REST source; no persistent entity of its own — all state lives in the shared connector tables.
- Reuses unchanged: `ConnectorInstallation`, `ConnectorConfig` (+ `secretValuesEncrypted`), `FieldMapping`, `ConnectorCheckpoint`, `ConnectorRun`, `ConnectorEntityWriter`, `ConnectorSparseUpsertService`.

## Success Criteria *(mandatory)*

- **SC-001**: An admin can install, configure (incl. a secret), map, test, and monitor a connector entirely from the app UI, on Android/iOS/Desktop, with no code changes per connector.
- **SC-002**: The field-mapping editor (T028b) exists and round-trips edits to the backend.
- **SC-003**: A configured Generic HTTP/JSON server-side connector syncs at least one entity end-to-end on a schedule with no client running, preserving unmapped columns.
- **SC-004**: A CLIENT_SIDE connector is never executed by the server scheduler.
- **SC-005**: Stored secrets are never returned to clients or written to logs.
- **SC-006**: `:connector:compileKotlin` + new executor/provider tests pass locally; client compiles on CI.

## Out of Scope (this phase)
- OAuth flows and webhook receivers (FR-S08 — additive later).
- Vendor providers (Zoho/Shopify/QuickBooks) — the generic provider is the template; vendors follow.
- Server-side **OUTBOUND** (Ampairs → external) execution; first server-side provider is INBOUND pull only.
- Web (`ampairs-web`) connector UI — separate repo.
