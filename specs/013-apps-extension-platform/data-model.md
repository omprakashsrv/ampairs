# Phase 1 Data Model: Apps & Extensions Connector Platform

Backend `connector` module (Spring Boot + Kotlin). All tenant-scoped entities extend **`OwnableBaseDomain : BaseDomain`**, inheriting: `id: Long` (`@Id @GeneratedValue`), `uid: String` (client-authored, `@Column(unique=true, updatable=false)`), `createdAt: Instant?` / `updatedAt: Instant?` (set in `@PrePersist`/`@PreUpdate`, UTC), `ownerId: String` (`@TenantId`, the workspace), `refId: String?` (stable external id, length 255). Each connector entity adds its own soft-delete marker (`active: Boolean` or a `status` field) per the module's convention so the `/sync` feed can propagate deletions. Timestamps are `Instant` → `TIMESTAMPTZ`/`TIMESTAMP` (Principle I). DTO isolation (Principle II) applies to every entity below.

> The **catalogue** `Connector` definition is **code-defined** (not a tenant table) — like `SettingDefinition` — so connectors ship with the build. Only per-workspace state (installations, config, mappings, checkpoints, runs) is persisted.

## Code-defined: Connector (catalogue definition)

Not a JPA entity — a registered definition (provider pattern, like `SettingDefinitionProvider`).

| Field | Type | Notes |
|---|---|---|
| `type` | String (id) | e.g. `"tally"` |
| `displayName` | String | |
| `description` | String | |
| `hostingType` | enum `CLIENT_SIDE` / `SERVER_SIDE` | Tally = CLIENT_SIDE |
| `supportedEntities` | Set<String> | e.g. customer, customer_group, product, product_catalog, unit, stock_balance |
| `supportedDirections` | Set<enum INBOUND/OUTBOUND> | first release: INBOUND only |
| `connectionSchema` | descriptor | fields the install must supply (e.g. host, port) + which are secret |
| `defaultMapping` | template | default field mapping per supported entity |
| `requiredTier` / `requiredModule` | entitlement | gates catalogue visibility (reuses workspace module/tier gating) |
| `multipleInstancesAllowed` | Boolean | default false (FR-005) |

## Entity: ConnectorInstallation

A connector enabled for a workspace.

| Field | Type | Notes |
|---|---|---|
| (OwnableBaseDomain) | | `uid`, `ownerId`(workspace), `active`, timestamps |
| `connectorType` | String | FK to a code-defined Connector `type` |
| `status` | enum | `NEEDS_CONFIG`, `ENABLED`, `PAUSED`, `ERROR`, `UNINSTALLED` (FR-006) |
| `autoStart` | Boolean | default true (FR-023) |
| `scheduleSeconds` | Int? | sync interval; null = connector default |
| `lastErrorMessage` | String? | last failure reason (FR-022) |

- **Uniqueness**: one active installation per (`ownerId`, `connectorType`) unless the connector's `multipleInstancesAllowed` (FR-005).
- **State transitions**: `NEEDS_CONFIG → ENABLED` (on valid config), `ENABLED ⇄ PAUSED` (admin), any `→ ERROR` (sync failure) `→ ENABLED` (recovery), any `→ UNINSTALLED` (soft-delete `active=false`, FR-003).
- Relationships: 1→1 `ConnectorConfig`, 1→N `ConnectorFieldMapping`, 1→N `ConnectorSyncCheckpoint`, 1→N `ConnectorSyncRun`. `@NamedEntityGraph` bundling config + mappings (Principle VII).

## Entity: ConnectorConfig

Per-installation connection details + secrets.

| Field | Type | Notes |
|---|---|---|
| (OwnableBaseDomain) | | |
| `installationUid` | String | FK → ConnectorInstallation.uid |
| `nonSecretValues` | JSON/map | e.g. host, port — returned to clients |
| `secretValuesEncrypted` | text | encrypted at rest; env-var key (FR-008, Principle XI) |
| `lastValidatedAt` | Instant? | last successful connection test (FR-009) |

- Response DTO MUST exclude `secretValuesEncrypted` and any secret keys (mask/write-only).

## Entity: ConnectorFieldMapping

Per-installation, per-entity field mapping (the allowlist).

| Field | Type | Notes |
|---|---|---|
| (OwnableBaseDomain) | | |
| `installationUid` | String | FK |
| `entityType` | String | e.g. `"customer"` |
| `rules` | JSON list | each: `externalField`, `ampairsField` (target column), `unmapped: Boolean`, optional `transform`/`sanitize` |
| `version` | Int | bumped on edit (mapping change tracking) |

- The **set of `ampairsField` targets where `unmapped=false`** is the writable allowlist used by the sparse upsert (R3).
- Validation: each `ampairsField` must exist on the target entity and be type-compatible (FR-013); invalid rules flagged at save.
- Uniqueness: one mapping per (`installationUid`, `entityType`).

## Entity: ConnectorSyncCheckpoint

Per-installation, per-entity, per-direction incremental watermark.

| Field | Type | Notes |
|---|---|---|
| (OwnableBaseDomain) | | |
| `installationUid` | String | FK |
| `entityType` | String | |
| `direction` | enum INBOUND/OUTBOUND | |
| `watermark` | String | ISO-8601 timestamp or external cursor (e.g. Tally alterId) |
| `lastSyncedAt` | Instant? | |

- Uniqueness: (`installationUid`, `entityType`, `direction`). Basis for incremental + stateful resume (FR-016/FR-017).

## Entity: ConnectorSyncRun

Audit record per sync execution (FR-020).

| Field | Type | Notes |
|---|---|---|
| (OwnableBaseDomain) | | |
| `installationUid` | String | FK |
| `entityType` | String? | null = multi-entity run |
| `trigger` | enum SCHEDULED/MANUAL/EVENT | |
| `startedAt` / `finishedAt` | Instant / Instant? | |
| `status` | enum RUNNING/SUCCESS/PARTIAL/FAILED | |
| `processed` / `created` / `updated` / `failed` | Int | counts |
| `errorDetail` | text? | |

## External Record Identity (no new table)

The entity's single `refId` (already on `OwnableBaseDomain` for customer/product/etc.) holds the owning connector's external id and is the sparse-upsert match key (R5/FR-019). No per-connector reference table.

## Client-side mirror (KMP `ampairs-app`)

Room cache (workspace-scoped) of installation/config/mapping/checkpoint pulled from the backend via `ConnectorSyncDelegate` (`SyncEntity.CONNECTOR`), read through a `ConnectorConfigProvider` (mirrors `StoreSettingsProvider`). `tallysync` reads host/port/mapping/watermark from this provider instead of `AppPreferencesDataStore`, and reports run results + checkpoints back.

## Validation rules summary

- Installation status transitions enforced server-side; cannot move to `ENABLED` without a validated config (FR-009).
- Mapping `ampairsField` targets validated against the live entity schema (FR-013).
- Sparse upsert: reject/ignore payload keys not in the installation's mapped allowlist for that entity (R3); never write outside the allowlist even if present (FR-018b).
- Secrets never serialized to response DTOs (FR-008).
- Identity match by `refId` scoped to workspace; second connector never overwrites another's `refId` (FR-019).
</content>
