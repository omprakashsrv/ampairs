# Feature Specification: Apps & Extensions Connector Platform

**Feature Branch**: `013-apps-extension-platform`  
**Created**: 2026-06-15  
**Status**: Draft  
**Input**: User description: "we need to create the Apps or extension platform. Currently we are having tally as client app, which we are syncing the data to local db and then local db sync's back to backend. This we need to change and create a generic app or extension platform. Currently it's tally, later there can be zoho, salesforce, hubspot or any other app or extension. Current tally sync is happening in client side but we need to move this to server sync, so we should be able to install new apps/extension. Configure these per workspace and post configuration and data mapping all the data should be sent back to server. The server should update only mapped data and retain the other un-mapped column. App/extension should auto started if configured for the workspace. Also need to think about the data mapping UI and configuration and all these should persist to backend database. The sync between external app like tally and ampairs backend should be incremental and stateful and it can be two way as well. Existing tally app one way sync should be migrated to new generic app/extension based model. Later we can also introduce the server based two way sync which auto do the two way sync between ampairs and external connectors."

## Overview

Ampairs needs a **generic, server-managed Apps & Extensions platform** that lets a workspace connect to external business systems (Tally today; Zoho, Salesforce, HubSpot and others later) without bespoke client-side code per integration. A workspace administrator browses a catalogue of available connectors, installs one into their workspace, supplies connection details, maps the external system's fields to Ampairs fields, and from then on the **server** drives an incremental, stateful synchronisation between the external system and Ampairs data. Mapped fields are updated on each sync; unmapped fields on existing records are preserved untouched. Configured connectors start automatically. The existing client-side, one-way Tally integration is migrated onto this platform so it becomes one connector among many.

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

### User Story 3 - Server-side incremental stateful sync (Priority: P1)

Once a connector is configured and mapped, the server periodically (and on demand) synchronises data from the external system into Ampairs. Each sync only fetches what changed since the last successful sync (incremental) and remembers its position so the next run resumes from there (stateful). For each record, only the mapped fields are written; any unmapped Ampairs fields on an existing record are left exactly as they were.

**Why this priority**: This is the core value — moving sync from the client to the server and making it incremental, stateful, and non-destructive. It is the reason the platform exists.

**Independent Test**: With a configured connector, run a sync, verify mapped records appear/update in Ampairs; change one external record, run again, verify only the changed record is processed and unmapped fields on it are untouched.

**Acceptance Scenarios**:

1. **Given** a configured connector running its first sync, **When** the sync completes, **Then** external records are created/updated in Ampairs using the mapping, and a sync-state checkpoint is recorded for that connector/entity.
2. **Given** a connector that has synced before, **When** a new sync runs, **Then** only records changed in the external system since the last checkpoint are processed (incremental), and the checkpoint advances.
3. **Given** an existing Ampairs record with values in unmapped fields, **When** a sync updates that record from the external system, **Then** the mapped fields are overwritten and the unmapped fields retain their previous values.
4. **Given** a sync run, **When** it completes or fails, **Then** an auditable run record is stored (start/end time, records processed, created/updated counts, errors) and is visible to the administrator.
5. **Given** a sync interrupted mid-run (crash/restart), **When** sync resumes, **Then** it continues from the last persisted checkpoint without duplicating or losing records.

---

### User Story 4 - Auto-start configured connectors (Priority: P2)

A connector that has been fully configured for a workspace begins syncing automatically on its schedule without anyone manually triggering it, and resumes automatically after a server restart.

**Why this priority**: Automation is the point of a connector — administrators configure once and expect data to keep flowing. It depends on P1–P3 existing but materially raises the product's value.

**Independent Test**: Configure a connector, do nothing further, and confirm a sync runs on schedule; restart the server and confirm scheduled syncs resume for all configured workspaces.

**Acceptance Scenarios**:

1. **Given** a fully configured, enabled connector, **When** its schedule elapses, **Then** a sync runs automatically with no manual trigger.
2. **Given** the server restarts, **When** it comes back up, **Then** all enabled, configured connectors across all workspaces resume their schedules from their last checkpoints.
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
- **Mapping points to a non-existent or removed Ampairs field**: such a mapping is flagged invalid at save time (and skipped safely at sync time) rather than corrupting data.
- **A record deleted in the external system**: handled per the connector's declared delete behaviour (propagate as soft-delete vs. ignore); the default must be explicit and non-destructive unless configured otherwise.
- **Duplicate external identifiers / records that match more than one Ampairs record**: resolved deterministically (e.g. by stable external id) and logged rather than creating duplicates.
- **Local agent offline**: when the workspace's local connectivity agent (for on-premise systems like Tally) is offline, the connector is shown as "waiting for agent", no sync is attempted, the checkpoint is preserved, and sync resumes automatically when the agent reconnects.
- **Connector uninstalled mid-sync**: an in-flight sync stops cleanly and no further syncs run.
- **Subscription tier no longer includes a connector**: an already-installed connector is disabled (not silently deleted) and the administrator is informed.
- **Same workspace, same connector installed twice**: prevented — at most one installation per connector per workspace (unless the connector explicitly supports multiple connections).
- **Concurrent syncs for the same connector/entity**: serialised so two runs cannot process the same checkpoint window simultaneously.
- **Two-way conflict (future)**: a record changed on both sides between syncs resolves by most-recent-update-wins (newer last-modified timestamp), and the resolution is recorded in the run history.

## Requirements *(mandatory)*

### Functional Requirements

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
- **FR-009**: The system MUST allow the administrator to validate/test a connection and report success or a clear failure reason before sync is enabled.
- **FR-010**: All connector configuration MUST persist to the backend database (not only on a client device).

#### Data mapping
- **FR-011**: Each connector MUST provide a default mapping template that maps external entities and fields to Ampairs entities and fields out of the box.
- **FR-012**: A workspace administrator MUST be able to review and customise the mapping per entity type — change a field's Ampairs target, mark a field as unmapped, and define transformation/sanitisation rules — and the customised mapping MUST persist to the backend.
- **FR-013**: The system MUST validate a mapping when saved, rejecting or flagging mappings that target non-existent Ampairs fields or violate type expectations.
- **FR-014**: The data-mapping configuration MUST be presented through a UI that lets a non-developer administrator complete mapping without writing code.

#### Sync engine (server-side, incremental, stateful)
- **FR-015**: The server MUST perform the synchronisation between the external system and Ampairs (sync MUST NOT depend on a client device being online).
- **FR-016**: Sync MUST be incremental — each run processes only records changed in the external system since the last successful checkpoint.
- **FR-017**: Sync MUST be stateful — the system persists a per-connector, per-entity (and per-direction) checkpoint/watermark and resumes from it, including after a server restart or interrupted run.
- **FR-018**: When writing a record, the system MUST update only the mapped fields and MUST preserve the existing values of unmapped fields on that record.
- **FR-019**: The system MUST match incoming external records to existing Ampairs records by a stable external identifier so that repeated syncs update rather than duplicate.
- **FR-020**: The system MUST record an auditable run history per sync (start/end time, trigger, records processed, created/updated/failed counts, and error details).
- **FR-021**: The system MUST serialise concurrent syncs for the same connector/entity so checkpoint windows do not overlap.
- **FR-022**: On failure, the system MUST retry on a backoff, preserve the last good checkpoint, surface the error state to the administrator, and not advance the checkpoint past unaccounted records.

#### Auto-start & scheduling
- **FR-023**: The system MUST automatically run sync on a schedule for every enabled, fully configured connector without a manual trigger.
- **FR-024**: The system MUST resume scheduled syncs for all enabled connectors across all workspaces after a server restart.
- **FR-025**: An administrator MUST be able to pause and resume a connector, and to trigger an on-demand sync.

#### Migration of existing Tally integration
- **FR-026**: Tally MUST be modelled as a connector on this platform, supporting the entities the current integration supports (customers, customer groups, products, product groups/categories, units, stock balances).
- **FR-027**: Migration MUST match previously synced Tally records to existing Ampairs records so no duplicates are created and prior data is preserved.
- **FR-028**: After migration, the legacy client-side Tally sync path MUST be retired without any workspace losing Tally connectivity.
- **FR-029**: For on-premise external systems such as Tally (which run on the user's local network and are not directly reachable from the server), reachability MUST be provided by a lightweight, workspace-installed local agent/bridge that runs near the external system and relays between it and the Ampairs server. The server remains the system of record for configuration, mapping, and sync state; the agent provides only connectivity to the local system. The agent MUST authenticate to the server as its workspace, and the platform MUST surface the agent's connectivity status (online/offline) to the administrator.

#### Two-way sync (future-facing)
- **FR-030**: The platform MUST be designed so a connector can declare and later perform bidirectional sync (Ampairs → external) without re-architecting the catalogue, configuration, mapping, or sync-state model.
- **FR-031**: When two-way sync is enabled for a connector, the system MUST resolve records changed on both sides between syncs using a most-recent-update-wins rule (the side with the newer last-modified timestamp wins), consistent with the project's established last-write-wins sync semantics, and MUST record the resolution outcome in the run history.

### Key Entities *(include if feature involves data)*

- **Connector (catalogue definition)**: A type of external integration available to install (Tally, Zoho, etc.). Attributes: identifier/type, display name, description, supported entity types, supported sync directions, default mapping template, connection-detail schema, entitlement/tier requirement.
- **Connector Installation**: A connector enabled for a specific workspace. Attributes: workspace, connector reference, lifecycle state, enabled/auto-start flag, schedule, timestamps. Tenant-scoped.
- **Connection Configuration / Credentials**: The per-installation connection details and secrets needed to reach the external system. Securely stored; secrets never exposed to clients.
- **Data Mapping**: Per-installation, per-entity rules describing how external fields map to Ampairs fields, which fields are unmapped, and transformation/sanitisation rules. Persisted server-side.
- **Sync State / Checkpoint**: Per-installation, per-entity, per-direction watermark recording how far sync has progressed (the basis for incremental + stateful behaviour).
- **Sync Run / Job History**: An auditable record of each sync execution — trigger, timing, counts (created/updated/failed), and errors.
- **External Record Reference**: The link between a stable external identifier and the corresponding Ampairs record, used to match updates and avoid duplicates.
- **Local Connectivity Agent**: A lightweight, workspace-installed bridge that runs near an on-premise external system (e.g. Tally) and relays between it and the server. Attributes: workspace, authentication identity, online/offline status, last-seen time. Holds no configuration or mapping itself — it only provides connectivity.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A workspace administrator can go from "no integration" to a configured, mapped, syncing connector in under 15 minutes without developer help.
- **SC-002**: Adding a brand-new connector type (e.g. Zoho) to the platform requires no changes to the install/configure/map/sync user experience — the same flows work for it.
- **SC-003**: 100% of syncs after the first are incremental — a run with no external changes processes zero records and completes quickly.
- **SC-004**: Across repeated syncs, zero duplicate Ampairs records are created from the same external record, and 100% of unmapped fields on updated records retain their prior values.
- **SC-005**: Configured connectors run automatically on schedule, and after a server restart 100% of enabled connectors resume from their last checkpoint with no manual intervention.
- **SC-006**: Every workspace currently using client-side Tally sync continues to receive Tally data after migration, with no data loss and no duplicate records.
- **SC-007**: For every sync run, an administrator can see whether it succeeded or failed and why, within the workspace UI.
- **SC-008**: A sync interrupted by a crash or restart resumes without losing or double-processing records (verified by record counts before/after).

## Assumptions

- **Server-side ownership**: All connector configuration, mappings, credentials, sync state, and run history live in the backend database; clients are thin views over server-managed state (replacing today's DataStore-only Tally config).
- **On-premise connectivity via agent**: On-premise systems like Tally are reached through a workspace-installed local agent/bridge (FR-029); the server orchestrates sync and holds all state, while the agent only relays data to/from the local system. Internet-reachable connectors (Zoho, Salesforce, HubSpot) may be reached by the server directly without an agent.
- **Conflict resolution**: Two-way conflicts resolve by most-recent-update-wins, consistent with the project's existing last-write-wins sync semantics.
- **First release scope**: The first release delivers the catalogue, per-workspace install, configuration, data mapping, server-side incremental/stateful one-way sync (external → Ampairs), auto-start, and Tally migration. Two-way sync (Ampairs → external) is platform-ready but delivered in a later phase, consistent with stakeholder intent.
- **Entitlements reuse existing concepts**: Connector availability reuses the existing workspace module-enablement / subscription-tier mechanism rather than inventing a new entitlement system.
- **Identity for matching**: External records carry a stable identifier (e.g. Tally's record GUID) used to match Ampairs records and drive incremental updates.
- **Sync semantics reuse the canonical model**: Incremental, checkpointed, non-destructive, multi-tenant sync follows the project's established offline-sync principles (timestamps/watermarks, in-band soft deletes, last-write-wins for conflicts) adapted to external connectors.
- **Administrator role**: Installing/configuring connectors is an administrator/owner-level capability within a workspace, not available to every member.
- **Notifications**: Error and connection-failure notifications use the workspace's existing notification surface; no new channel is introduced for this feature.

## Out of Scope

- Building specific Zoho / Salesforce / HubSpot connector implementations (the platform must support them; only Tally is delivered first).
- A public third-party developer/marketplace SDK for external parties to publish connectors.
- Bidirectional/two-way write-back sync execution in the first release (design must accommodate it; FR-030).
- Real-time/streaming sync (the first release is scheduled + on-demand incremental sync).
- Migrating non-Tally legacy integrations (none exist today).
</content>
</invoke>
