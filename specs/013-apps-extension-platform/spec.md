# Feature Specification: Apps & Extensions Connector Platform

**Feature Branch**: `013-apps-extension-platform`  
**Created**: 2026-06-15  
**Status**: Draft  
**Input**: User description: "we need to create the Apps or extension platform. Currently we are having tally as client app, which we are syncing the data to local db and then local db sync's back to backend. This we need to change and create a generic app or extension platform. Currently it's tally, later there can be zoho, salesforce, hubspot or any other app or extension. Current tally sync is happening in client side but we need to move this to server sync, so we should be able to install new apps/extension. Configure these per workspace and post configuration and data mapping all the data should be sent back to server. The server should update only mapped data and retain the other un-mapped column. App/extension should auto started if configured for the workspace. Also need to think about the data mapping UI and configuration and all these should persist to backend database. The sync between external app like tally and ampairs backend should be incremental and stateful and it can be two way as well. Existing tally app one way sync should be migrated to new generic app/extension based model. Later we can also introduce the server based two way sync which auto do the two way sync between ampairs and external connectors."

## Overview

Ampairs needs a **generic Apps & Extensions platform** that lets a workspace connect to external business systems (Tally today; Zoho, Salesforce, HubSpot and others later) without bespoke, hardcoded integration code. A workspace administrator browses a catalogue of available connectors, installs one into their workspace, supplies connection details, and maps the external system's fields to Ampairs fields. The connector then synchronises data incrementally and statefully between the external system and Ampairs.

The platform supports **two connector hosting types**:

1. **Client-side connectors (priority — e.g. Tally)**: all push/pull execution happens in the Ampairs client (desktop) app, which talks to the local external system and pushes mapped data to the Ampairs backend via a dedicated connector sparse-upsert endpoint (not the global per-resource `/sync`). What is **new** is that the connector's configuration, field mapping, sync checkpoints, and run history are **persisted to (and synced from) the backend**, so the setup is retained across reinstalls/devices and the client keeps the sync running in the background with a status UI.
2. **Server-side connectors (deferred — not a priority now)**: both push and pull run on the server for internet-reachable systems. The platform's model (catalogue, install, config, mapping, sync-state) is designed to accommodate this later without re-architecting.

When the backend receives synced records, it updates only the mapped fields and preserves unmapped columns. Configured connectors start automatically. The existing client-side, one-way Tally integration is migrated onto this platform so it becomes one (client-side) connector among many.

## Clarifications

### Session 2026-06-15

- Q: How should server-driven sync reach an on-premise system like Tally? → A: Connectors come in two hosting types — **client-side** (priority; Tally — all push/pull runs in the client desktop app) and **server-side** (deferred — both push/pull run on the server). Tally stays client-executed; no separate server-side relay agent is introduced. (Supersedes the earlier "local agent/bridge" answer.)
- Q: For client-side connectors (Tally), what persists to the backend beyond the synced data itself? → A: Connector configuration + field mapping + per-entity sync checkpoints/watermarks + run/job history. (Data reaches Ampairs via a dedicated connector sparse-upsert endpoint — not the global per-resource `/sync`; backend applies mapped-fields-only partial updates.)
- Q: Where do client-side connectors execute? → A: Inside the existing Ampairs client (desktop) app, extending today's Tally module — reusing the existing client sync path.
- Q: Two-way conflict authority when both sides change a record between syncs? → A: Most-recent-update-wins (newer last-modified timestamp), consistent with the project's last-write-wins semantics.
- Q: How should the server avoid nulling out unmapped fields when a connector pushes records (current `/sync` does a full upsert)? → A: Server-derived from the backend-stored mapping — the server loads the existing record (matched by stable external id / `refId`), applies only the columns the connector mapped for that entity, and preserves all others; an unmapped field can never overwrite existing data even if the payload carries null/empty. (Reuses the existing partial-merge pattern already used in the product module.)
- Q: Should this partial-update behavior change the global `/sync` contract or be scoped to connectors? → A: Connector-scoped only — applied on the connector-originated write path; the existing global `/sync` upsert used by normal offline clients is unchanged.
- Q: With multiple connectors syncing the same entity into one workspace, how are records matched (each record has only one `refId`)? → A: Single `refId`/`uid`, one owner — the entity's `refId` (or `uid`) is the identity key; the connector that first creates/owns a record owns its `refId`. Matching is by `refId`/`uid` only (see remediation below — no business-key reconciliation); a non-matching row creates a new record. (Field mapping is defined per connector installation, so each connector has its own mapping.)
- Q: When two connectors in the same workspace map the SAME Ampairs field on the same entity, who wins? → A: Overlap is allowed; last-write-wins by timestamp, consistent with the project's existing conflict rule.
- Q: How does the server know, per row, which columns to upsert (row1 updates column1/column2 while row2 updates column3/column4, without nulling the rest)? → A: Presence-based (omitted = skip) — the server upserts only the columns actually present in each row's payload, bounded by the connector's mapped allowlist; columns omitted from a row are left untouched. The wire format MUST distinguish "column omitted" from "column present with null" so an omitted column is skipped while an explicit null can intentionally clear a mapped value.
- Q: (Analysis remediation) Should a second connector reconcile to an existing record by business keys? → A: No — matching is by the entity's `refId` (or `uid`) only; a non-matching row creates a new record. No business-key reconciliation. (Supersedes the earlier "reconcile by business keys" wording.)
- Q: (Analysis remediation) Who performs the connection reachability test for client-side connectors? → A: The **client** performs the reachability test and reports the result to the backend; the server stores `last_validated_at` and never attempts to reach a client-side connector's external system.
- Q: (Analysis remediation) Does the mapping UI include value formatting/conversion? → A: Yes — the app provides a data-mapping **plus** formatting/converter (transformation) UI, not just field-to-field mapping.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Install a connector into a workspace (Priority: P1)

A workspace administrator opens the Apps & Extensions area, sees a catalogue of available connectors (e.g. Tally), and installs one into their workspace. The installation is recorded server-side against the workspace and appears as "installed, needs configuration". An administrator of another workspace is unaffected.

**Why this priority**: Without a server-side registry of available connectors and per-workspace installations, nothing else can exist. This is the foundation that turns a hardcoded integration into a pluggable platform.

**Independent Test**: Browse the catalogue, install one connector, confirm it appears as installed for that workspace only and persists across sessions/devices; uninstall returns it to the catalogue.

**Acceptance Scenarios**:

1. **Given** a workspace with no connectors installed, **When** the administrator views the Apps & Extensions catalogue, **Then** all connectors available to that workspace's subscription tier are listed with name, description, and capabilities (supported entities, sync direction).
2. **Given** a connector in the catalogue, **When** the administrator installs it, **Then** a per-workspace installation record is persisted server-side with status "needs configuration" and is visible on every device/client for that workspace.
3. **Given** an installed connector, **When** an administrator of a different workspace views their catalogue, **Then** that installation is not visible to them (tenant isolation).
4. **Given** an installed connector, **When** the administrator uninstalls it, **Then** the installation, its configuration, and its sync state are removed (or deactivated) and sync stops.

---

### User Story 2 - Configure a connector and map its data (Priority: P1)

After installing a connector, the administrator supplies the connection details (e.g. the address/credentials needed to reach the external system) and defines how external entities and fields map to Ampairs entities and fields. The connector ships with a sensible default mapping that the administrator can review and adjust. All configuration and mappings persist to the backend database.

**Why this priority**: A connector cannot sync correctly until it knows how to reach the external system and which external field corresponds to which Ampairs field. Configuration + mapping is the second half of the foundation and is required before any data flows.

**Independent Test**: Configure connection details, accept or edit the default field mapping for at least one entity (e.g. customers), save, reload, and confirm the configuration and mapping are retrieved unchanged.

**Acceptance Scenarios**:

1. **Given** an installed connector that needs configuration, **When** the administrator enters the required connection details and saves, **Then** the configuration is validated, persisted server-side, and secrets are stored securely (never returned in plain text to clients).
2. **Given** a connector with a default mapping template, **When** the administrator opens the data-mapping screen for an entity type, **Then** the external fields and their default Ampairs targets are shown, and the administrator can change a target, mark a field unmapped, or set transformation/sanitisation rules.
3. **Given** an edited mapping, **When** the administrator saves it, **Then** the mapping persists to the backend and is the mapping used by subsequent syncs.
4. **Given** a connection configuration that fails validation (unreachable system or invalid credentials), **When** the administrator tries to save/test it, **Then** a clear error is shown and the connector remains "needs configuration".

---

### User Story 3 - Incremental stateful sync (Priority: P1)

Once a connector is configured and mapped, it periodically (and on demand) synchronises data between the external system and Ampairs. For client-side connectors (Tally — the priority), the Ampairs client (desktop) app performs the push/pull and pushes mapped data to the backend; for server-side connectors (deferred) the server performs it. Either way, each sync only processes what changed since the last successful sync (incremental) and remembers its position so the next run resumes from there (stateful). The sync checkpoints and run history are persisted to the backend so the setup survives reinstalls/devices. When the backend stores a record, only the mapped fields are written; any unmapped Ampairs columns on an existing record are left exactly as they were.

**Why this priority**: Incremental, stateful, non-destructive sync — with config, mapping, checkpoints, and history retained server-side — is the core value and the reason the platform exists.

**Independent Test**: With a configured connector, run a sync, verify mapped records appear/update in Ampairs; change one external record, run again, verify only the changed record is processed and unmapped columns on it are untouched; reinstall the client and confirm the connector resumes incrementally from the backend-persisted checkpoint.

**Acceptance Scenarios**:

1. **Given** a configured connector running its first sync, **When** the sync completes, **Then** external records are created/updated in Ampairs using the mapping, and a sync-state checkpoint for that connector/entity is persisted to the backend.
2. **Given** a connector that has synced before, **When** a new sync runs, **Then** only records changed in the external system since the last checkpoint are processed (incremental), and the checkpoint advances.
3. **Given** an existing Ampairs record with values in unmapped columns, **When** synced data updates that record, **Then** the mapped fields are overwritten and the unmapped columns retain their previous values.
4. **Given** a sync run, **When** it completes or fails, **Then** an auditable run record (start/end time, records processed, created/updated/failed counts, errors) is persisted to the backend and is visible to the administrator.
5. **Given** a sync interrupted mid-run (client crash/restart or reinstall), **When** sync resumes, **Then** it continues from the last backend-persisted checkpoint without duplicating or losing records.

---

### User Story 4 - Auto-start configured connectors (Priority: P2)

A connector that has been fully configured for a workspace begins syncing automatically on its schedule without anyone manually triggering it. For a client-side connector, the Ampairs client app auto-starts background sync once it detects (from the backend-persisted config) that the workspace has a configured connector; for a server-side connector, the server auto-starts it.

**Why this priority**: Automation is the point of a connector — administrators configure once and expect data to keep flowing. It depends on P1–P3 existing but materially raises the product's value.

**Independent Test**: Configure a connector, do nothing further, and confirm a sync runs on schedule; restart the host (client app for Tally) and confirm scheduled background sync resumes from the backend-persisted checkpoint.

**Acceptance Scenarios**:

1. **Given** a fully configured, enabled connector, **When** its schedule elapses, **Then** a sync runs automatically with no manual trigger.
2. **Given** a client-side connector and a workspace whose connector config is persisted in the backend, **When** the Ampairs client app starts (or restarts), **Then** it discovers the configured connector and resumes background sync from the last persisted checkpoint without manual setup.
3. **Given** an administrator pauses a connector, **When** the schedule elapses, **Then** no sync runs until it is re-enabled.

---

### User Story 5 - Migrate the existing Tally integration to the platform (Priority: P2)

The current client-side, one-way Tally sync is migrated so Tally becomes a connector in the new platform. Workspaces already using Tally continue to receive their Tally data without re-entering everything from scratch, and without duplicating or losing previously synced records.

**Why this priority**: Tally is the only live integration; the platform is not "done" until the existing experience runs on it. It depends on the platform (P1–P4) being in place.

**Independent Test**: For a workspace that currently uses client-side Tally, enable Tally as a platform connector and confirm its customers/products/units/groups continue to sync with no duplication and no loss of prior data.

**Acceptance Scenarios**:

1. **Given** a workspace currently using client-side Tally sync, **When** Tally is migrated to the connector platform, **Then** the same entities (customers, customer groups, products, product groups/categories, units, stock balances) continue to sync.
2. **Given** records previously synced from Tally via the old path, **When** the new platform syncs the same Tally data, **Then** records are matched to existing Ampairs records (no duplicates) and prior data is preserved.
3. **Given** the migration is complete, **When** the old client-side Tally sync path is retired, **Then** no workspace loses Tally connectivity.

---

### User Story 6 - Two-way sync (Priority: P3, future-facing)

Beyond pulling external data into Ampairs, the platform can also push Ampairs changes back to the external system, keeping both sides aligned automatically.

**Why this priority**: Explicitly described by stakeholders as a later phase ("Later we can also introduce the server based two-way sync"). The platform must be designed so this can be added without re-architecting, but the first release delivers external→Ampairs (one-way) for Tally.

**Independent Test**: For a connector that declares two-way capability, change an Ampairs record and confirm the change is propagated to the external system on the next sync, with conflict handling between the two sides.

**Acceptance Scenarios**:

1. **Given** a connector that supports two-way sync, **When** an Ampairs record covered by the mapping changes, **Then** the change is propagated to the external system on the next sync cycle.
2. **Given** the same record changed on both sides between syncs, **When** sync runs, **Then** the conflict is resolved by a defined, predictable rule and the outcome is recorded.

---

### Edge Cases

- **External system unreachable / credentials expired**: the connector is marked in an error state, the failure is recorded with a reason, the administrator is notified, and sync retries on a backoff without losing the checkpoint.
- **Partial sync failure**: if some records fail mapping/validation while others succeed, successful records are committed, failures are recorded per-record, and the checkpoint only advances past records that were fully accounted for.
- **Row omits a column / sends null / sends an out-of-mapping column**: a column omitted from a row is left untouched (no data loss — the failure mode the current full-upsert `/sync` would otherwise cause); a mapped column present with an explicit null clears that value intentionally; a column outside the connector's mapping is never written even if present. Different rows in the same push may therefore update different column sets.
- **Mapping points to a non-existent or removed Ampairs field**: such a mapping is flagged invalid at save time (and skipped safely at sync time) rather than corrupting data.
- **A record deleted in the external system**: handled per the connector's declared delete behaviour (propagate as soft-delete vs. ignore); the default must be explicit and non-destructive unless configured otherwise.
- **Duplicate external identifiers / records that match more than one Ampairs record**: resolved deterministically (e.g. by stable external id) and logged rather than creating duplicates.
- **Multiple connectors syncing the same entity into one workspace**: identity is matched by `refId` (or `uid`) only; a row that matches updates that record, otherwise a new record is created — no business-key reconciliation. A connector never overwrites another connector's `refId`. If two connectors map the same field, the later write wins. Each connector still writes only its own mapped columns.
- **Client host offline (client-side connectors)**: when the Ampairs client app hosting a client-side connector (e.g. Tally) is closed or offline, no sync runs, the backend-persisted checkpoint is preserved, and background sync resumes automatically from that checkpoint the next time the client app starts.
- **Connector uninstalled mid-sync**: an in-flight sync stops cleanly and no further syncs run.
- **Subscription tier no longer includes a connector**: an already-installed connector is disabled (not silently deleted) and the administrator is informed.
- **Same workspace, same connector installed twice**: prevented — at most one installation per connector per workspace (unless the connector explicitly supports multiple connections).
- **Concurrent syncs for the same connector/entity**: serialised so two runs cannot process the same checkpoint window simultaneously.
- **Two-way conflict (future)**: a record changed on both sides between syncs resolves by most-recent-update-wins (newer last-modified timestamp), and the resolution is recorded in the run history.

## Requirements *(mandatory)*

### Functional Requirements

#### Connector hosting types
- **FR-H01**: Each connector MUST declare a hosting type — **client-side** (push/pull executed in the Ampairs client app) or **server-side** (push/pull executed on the server) — and the platform MUST support both within the same catalogue/install/config/mapping/sync-state model.
- **FR-H02**: For a **client-side** connector, all external-system push/pull MUST execute in the Ampairs client (desktop) app, which pushes mapped data to the Ampairs backend via a dedicated connector sparse-upsert endpoint (not the global per-resource `/sync`, per FR-018a); the backend MUST NOT require a direct connection to the external system.
- **FR-H03**: For a client-side connector, the connector configuration, field mapping, per-entity sync checkpoints/watermarks, and run/job history MUST be persisted to the backend and synced down to the client, so setup and sync progress are retained across reinstalls and devices.
- **FR-H04**: Client-side sync MUST continue to run in the background while the Ampairs client app is open, with a status UI showing the connector's state and last sync result.
- **FR-H05**: Server-side connectors (both push and pull executed on the server) are explicitly deferred (not in the first release); the model MUST accommodate them later without re-architecting.

#### Connector catalogue & installation
- **FR-001**: The system MUST maintain a catalogue of available connectors, each described by a type/identifier, display name, description, the Ampairs entity types it supports, and its supported sync direction(s).
- **FR-002**: The system MUST gate which connectors appear in a workspace's catalogue by that workspace's subscription tier / module entitlements.
- **FR-003**: A workspace administrator MUST be able to install a connector into their workspace and uninstall it.
- **FR-004**: The system MUST persist connector installations server-side, scoped per workspace, isolated from other workspaces, and visible to every client of that workspace.
- **FR-005**: The system MUST prevent more than one active installation of the same connector in a workspace unless that connector explicitly supports multiple connections.
- **FR-006**: The system MUST track each installation's lifecycle state (e.g. needs configuration, configured/enabled, paused, error, uninstalled).

#### Configuration & credentials
- **FR-007**: A workspace administrator MUST be able to provide the connection details required by a connector to reach the external system, and the system MUST persist them server-side.
- **FR-008**: The system MUST store connector secrets (credentials, tokens, keys) securely and MUST never return secret values in plain text to clients.
- **FR-009**: The system MUST allow the administrator to validate/test a connection and report success or a clear failure reason before sync is enabled. For **client-side** connectors the reachability test is performed by the **client** (which can reach the local external system) and its result is reported to the backend, which records `last_validated_at`; the server MUST NOT attempt to reach a client-side connector's external system.
- **FR-010**: All connector configuration MUST persist to the backend database (not only on a client device).

#### Data mapping
- **FR-011**: Each connector MUST provide a default mapping template that maps external entities and fields to Ampairs entities and fields out of the box.
- **FR-012**: A workspace administrator MUST be able to review and customise the mapping per entity type — change a field's Ampairs target, mark a field as unmapped, and define transformation/sanitisation rules — and the customised mapping MUST persist to the backend.
- **FR-013**: The system MUST validate a mapping when saved, rejecting or flagging mappings that target non-existent Ampairs fields or violate type expectations.
- **FR-014**: The connector configuration MUST be presented through a UI that lets a non-developer administrator complete, without writing code: (a) **field mapping** (external field ↔ Ampairs field, mark unmapped), and (b) **value formatting/conversion (transformation)** rules (e.g. phone/GSTIN sanitisation, unit conversion, date/number formatting) per mapped field.

#### Sync engine (incremental, stateful)
- **FR-015**: The synchronisation between the external system and Ampairs MUST be performed by the connector's host — the Ampairs client app for client-side connectors (e.g. Tally), or the server for server-side connectors. For client-side connectors, sync runs whenever the client app is running and does not depend on a server-initiated trigger.
- **FR-016**: Sync MUST be incremental — each run processes only records changed in the external system since the last successful checkpoint.
- **FR-017**: Sync MUST be stateful — a per-connector, per-entity (and per-direction) checkpoint/watermark is persisted to the backend and resumed from, including after a client/server restart, reinstall, or interrupted run.
- **FR-018**: When the backend persists a connector-synced record, it MUST apply a per-row partial update: the columns written for a given row are the columns **present** in that row's payload, bounded by (intersected with) the connector's backend-persisted mapped allowlist for that entity. Columns omitted from a row's payload MUST be left untouched (preserved), and columns outside the connector's mapping MUST never be written — even if present in the payload. This lets row 1 update column1/column2 while row 2 updates column3/column4 in the same push, with all other columns preserved.
- **FR-018a**: The partial-update behavior MUST be applied on the connector-originated write path only. The existing global `/sync` upsert contract used by normal offline clients MUST remain unchanged (no presence/allowlist semantics imposed on non-connector sync).
- **FR-018b**: The backend MUST load the existing record (matched per FR-019) before applying a row's present, in-allowlist columns, and MUST leave fields outside that set — including audit/system columns and fields owned by other writers — untouched.
- **FR-018c**: The connector sync wire format MUST distinguish "column omitted from this row" from "column present with a null value": an omitted column is skipped (existing value preserved), whereas a column explicitly present with null MAY intentionally clear a mapped value. The server MUST NOT treat all nulls as either always-skip or always-write — presence is the signal, not nullness.
- **FR-019**: The system MUST match incoming external records to existing Ampairs records by the entity's `refId` (or client-authored `uid`) within the workspace, and by that key **only** — there is no business-key reconciliation. A row whose `refId`/`uid` matches an existing record updates it; a row that matches nothing creates a new record. Repeated syncs therefore update rather than duplicate. The connector that first creates/owns a record owns its `refId`; a connector MUST NOT overwrite or collide on another connector's `refId`.
- **FR-019a**: Field mapping MUST be defined per connector installation (workspace + connector + entity). Multiple client installs of the same connector for one workspace share the single server-persisted mapping for that installation; distinct connectors each have their own mapping.
- **FR-019b**: When two connectors in the same workspace map the same Ampairs field on the same record, overlap is permitted and resolved by last-write-wins (newer last-modified timestamp), consistent with FR-031. Each connector's write still applies only its own mapped columns (FR-018).
- **FR-020**: The system MUST record an auditable run history per sync (start/end time, trigger, records processed, created/updated/failed counts, and error details).
- **FR-021**: The system MUST serialise concurrent syncs for the same connector/entity so checkpoint windows do not overlap.
- **FR-022**: On failure, the system MUST retry on a backoff, preserve the last good checkpoint, surface the error state to the administrator, and not advance the checkpoint past unaccounted records.

#### Auto-start & scheduling
- **FR-023**: The system MUST automatically run sync on a schedule for every enabled, fully configured connector without a manual trigger. For client-side connectors, the Ampairs client app MUST auto-start background sync upon discovering (from the backend-persisted config) that the workspace has an enabled connector.
- **FR-024**: After a host restart (client app for client-side connectors; server for server-side connectors), the host MUST resume scheduled syncs for all enabled connectors from their backend-persisted checkpoints without manual re-setup.
- **FR-025**: An administrator MUST be able to pause and resume a connector, and to trigger an on-demand sync.

#### Migration of existing Tally integration
- **FR-026**: Tally MUST be modelled as a connector on this platform, supporting the entities the current integration supports (customers, customer groups, products, product groups/categories, units, stock balances).
- **FR-027**: Migration MUST match previously synced Tally records to existing Ampairs records so no duplicates are created and prior data is preserved.
- **FR-028**: After migration, the legacy client-side Tally sync path MUST be retired without any workspace losing Tally connectivity.
- **FR-029**: Tally MUST be modelled as a **client-side** connector: its push/pull continues to execute in the Ampairs client (desktop) app talking to the local Tally instance (no server-side relay agent is introduced). The migration's new behaviour is that Tally's configuration, mapping, sync checkpoints, and run history move from client-only storage to backend-persisted storage (per FR-H03), so the setup is retained and discoverable across reinstalls/devices.

#### Two-way sync (future-facing)
- **FR-030**: The platform MUST be designed so a connector can declare and later perform bidirectional sync (Ampairs → external) without re-architecting the catalogue, configuration, mapping, or sync-state model.
- **FR-031**: When two-way sync is enabled for a connector, the system MUST resolve records changed on both sides between syncs using a most-recent-update-wins rule (the side with the newer last-modified timestamp wins), consistent with the project's established last-write-wins sync semantics, and MUST record the resolution outcome in the run history.

### Key Entities *(include if feature involves data)*

- **Connector (catalogue definition)**: A type of external integration available to install (Tally, Zoho, etc.). Attributes: identifier/type, **hosting type (client-side / server-side)**, display name, description, supported entity types, supported sync directions, default mapping template, connection-detail schema, entitlement/tier requirement.
- **Connector Installation**: A connector enabled for a specific workspace. Attributes: workspace, connector reference, lifecycle state, enabled/auto-start flag, schedule, timestamps. Tenant-scoped.
- **Connection Configuration / Credentials**: The per-installation connection details and secrets needed to reach the external system. Securely stored; secrets never exposed to clients.
- **Data Mapping**: Per-connector-installation, per-entity rules describing how external fields map to Ampairs fields, which fields are unmapped, and transformation/sanitisation rules. Persisted server-side. Different connectors in a workspace may map overlapping fields; overlaps resolve by last-write-wins.
- **Sync State / Checkpoint**: Per-installation, per-entity, per-direction watermark recording how far sync has progressed (the basis for incremental + stateful behaviour). Persisted to the backend (including for client-side connectors) so it survives reinstalls/devices.
- **Sync Run / Job History**: An auditable record of each sync execution — trigger, timing, counts (created/updated/failed), and errors. Persisted to the backend (including for client-executed runs).
- **External Record Identity**: The entity's `refId` (with `uid` as the client-authored alternative) holds the stable external identifier of the connector that owns that record; it is the **sole** match key for repeated syncs — no business-key reconciliation. A non-matching row creates a new record. A given record has exactly one owning connector's `refId`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A workspace administrator can go from "no integration" to a configured, mapped, syncing connector in under 15 minutes without developer help.
- **SC-002**: Adding a brand-new connector type (e.g. Zoho) to the platform requires no changes to the install/configure/map/sync user experience — the same flows work for it.
- **SC-003**: 100% of syncs after the first are incremental — a run with no external changes processes zero records and completes quickly.
- **SC-004**: Across repeated syncs, zero duplicate Ampairs records are created from the same external record, and 100% of columns not present in a row's payload (or outside the connector's mapping) retain their prior values — even when different rows in the same push update different column sets (no data loss).
- **SC-005**: Configured connectors run automatically on schedule, and after a host restart (client app for Tally) 100% of enabled connectors resume from their backend-persisted checkpoint with no manual intervention.
- **SC-006**: Every workspace currently using client-side Tally sync continues to receive Tally data after migration, with no data loss and no duplicate records.
- **SC-007**: For every sync run, an administrator can see whether it succeeded or failed and why, within the workspace UI.
- **SC-008**: A sync interrupted by a crash or restart resumes without losing or double-processing records (verified by record counts before/after).

## Assumptions

- **Backend persistence, client execution (priority)**: For client-side connectors (the first-release priority), connector configuration, mapping, sync checkpoints, and run history live in the backend database (replacing today's DataStore-only Tally config), but the actual push/pull executes in the Ampairs client (desktop) app, which pushes mapped data to the backend via a dedicated connector sparse-upsert endpoint (not the global per-resource `/sync`). Credentials/secrets are stored securely.
- **Execution host**: Client-side connectors run inside the existing Ampairs client app, extending today's Tally module and reusing the existing client sync path; no separate standalone connector/agent process is introduced.
- **Server-side connectors deferred**: Server-side connectors (push/pull on the server, for internet-reachable systems) are designed-for but not built in the first release.
- **Conflict resolution**: Two-way conflicts resolve by most-recent-update-wins, consistent with the project's existing last-write-wins sync semantics.
- **First release scope**: The first release delivers the catalogue, per-workspace install, configuration, data mapping, client-side incremental/stateful one-way sync (external → Ampairs) with backend-persisted config/mapping/checkpoints/history, auto-start, and Tally migration. Two-way sync (Ampairs → external) and server-side-hosted connectors are platform-ready but delivered later, consistent with stakeholder intent.
- **Entitlements reuse existing concepts**: Connector availability reuses the existing workspace module-enablement / subscription-tier mechanism rather than inventing a new entitlement system.
- **Identity for matching**: External records carry a stable identifier (e.g. Tally's record GUID) stored in the entity's `refId`, used to match Ampairs records and drive incremental updates.
- **Partial-update mechanism**: The connector write path reuses the project's existing load-existing-then-copy-fields merge pattern (as already implemented for the product `/sync` upsert), applying per-row presence (which columns the row sends) bounded by the connector's backend-stored mapping allowlist. The wire format must carry per-row column presence (e.g. a sparse/map payload) so omitted columns are distinguishable from explicit nulls.
- **Sync semantics reuse the canonical model**: Incremental, checkpointed, non-destructive, multi-tenant sync follows the project's established offline-sync principles (timestamps/watermarks, in-band soft deletes, last-write-wins for conflicts) adapted to external connectors.
- **Administrator role**: Installing/configuring connectors is an administrator/owner-level capability within a workspace, not available to every member.
- **Notifications**: Error and connection-failure notifications use the workspace's existing notification surface; no new channel is introduced for this feature.

## Out of Scope

- Building specific Zoho / Salesforce / HubSpot connector implementations (the platform must support them; only Tally is delivered first).
- A public third-party developer/marketplace SDK for external parties to publish connectors.
- Bidirectional/two-way write-back sync execution in the first release (design must accommodate it; FR-030).
- Server-side-hosted connectors (push/pull executed on the server) in the first release (design must accommodate them; FR-H05).
- Real-time/streaming sync (the first release is scheduled + on-demand incremental sync).
- Migrating non-Tally legacy integrations (none exist today).
</content>
</invoke>
