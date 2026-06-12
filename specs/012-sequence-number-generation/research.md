# Research: Sequence Number Generation Module (012)

## R1 — Atomic counter advancement strategy (backend)

- **Decision**: Pessimistic row lock on the definition row inside a `@Transactional` service method: `@Lock(LockModeType.PESSIMISTIC_WRITE)` on a `findByUidForUpdate` repository query, then read–increment–save.
- **Rationale**: The codebase has no existing durable counter pattern (the only prior art, `subscription/InvoiceGenerationService`, uses an in-memory `AtomicInteger` that resets on restart — explicitly what this module replaces). A `SELECT … FOR UPDATE` serializes concurrent generators and block allocators on the single counter row, is portable across MySQL/Postgres, and composes with Hibernate `@TenantId` filtering. Contention is low by design because mobile devices consume from pre-allocated blocks.
- **Alternatives considered**: (a) `@Modifying UPDATE … SET current_value = current_value + :delta` — atomic but cannot return the pre-image portably without a second read, and `@TenantId` does not auto-filter modifying JPQL the same way; (b) optimistic `@Version` retry loop — more code, retry storms under burst load; (c) DB-native sequences — cannot be tenant/definition dynamic.

## R2 — Uniqueness of active definition per lookup key

- **Decision**: Service-level validation (`existsBy…ActiveTrue…`) before create/activate, no partial unique index.
- **Rationale**: The key is (owner_id, entity_type, scope, user_id) *where active*. MySQL has no partial unique indexes, so a DB constraint would also block keeping deactivated history rows. Matches existing duplicate-check convention (`UnitServiceImpl.existsByOwnerIdAndNameIgnoreCase`).

## R3 — Sync contract placement

- **Decision**: `sequence_definition` goes on the canonical offline-sync contract (`GET`/`POST /sequence/v1/definitions/sync`); `sequence_allocation` is **off-contract** (request/response RPC: `POST /sequence/v1/allocations`, `POST /sequence/v1/allocations/report`).
- **Rationale**: Definitions are workspace configuration data, identical in shape to `setting` — both ends edit, both ends need the full set. Allocations are inherently device-scoped server grants: a device must never "pull" another device's range, and a grant must be a server-side atomic operation, not a UID-keyed upsert. This mirrors the documented off-contract precedents (tax subscribe, file multipart).
- **Counter conflict rule**: on definition push, the server's `current_value` is authoritative — the client never lowers it; bulk upsert ignores client `current_value` for existing rows.

## R4 — Device identity

- **Decision**: The client sends `device_id` explicitly in allocation requests; backend stores it opaque (no FK to device sessions).
- **Rationale**: Backend controllers do not read JWT claims directly (convention: `SessionUserFilter` + `TenantContextHolder` only). The mobile app already owns a stable device id (`feature/auth-api` `DeviceService.getDeviceId()`), which is the same id embedded in the JWT — passing it explicitly keeps the contract testable from web/curl too.

## R5 — User-scope resolution at generation time

- **Decision**: Controller obtains the caller's user id via `AuthenticationHelper.getCurrentUserId(SecurityContextHolder.getContext().authentication)` (existing core helper used by `SessionUserFilter`) and passes it to the service. Resolution order: active USER-scope definition for caller → active WORKSPACE-scope definition → auto-provisioned default.
- **Default provisioning**: known entity types map to standard prefixes (product→PRD, customer→CUS, order→ORD, invoice→INV); unknown types use uppercased first 3 chars. Start 1, increment 1, padding 0, workspace scope.

## R6 — Mobile module shape

- **Decision**: Single `feature/sequence` KMP module (no `-api` split yet). Room DB `sequence` (workspace-scoped, v1) with `sequence_definition` + `sequence_allocation` tables; allocation rows carry a **definition format snapshot** (prefix/suffix/padding/increment) so offline formatting never depends on a definition row being present. `SequenceNumberProvider` (`@Inject`, unscoped) is the cross-feature entry point, following the `StoreSettingsProvider` precedent. `SequenceSyncDelegate` (`@SyncEntityKey(SyncEntity.SEQUENCE)`) pulls definitions (canonical feed) and pushes unsynced definition edits + allocation consumption reports.
- **Allowed API-in-repository exception**: the on-demand block grant (`POST /allocations`) is a UI-invoked, non-sync operation with no central-sync equivalent (same category as the file repo's entity-scoped pull) — the allocation repository may call `SequenceApi.requestAllocation` directly when the provider needs a block *now*; consumption reporting still flows through the delegate.
- **Provisional numbers (FR-011)**: when offline with an exhausted block, the provider returns `SequenceNumberResult(provisional = true)` with a locally formatted placeholder (`{prefix}-P{localCounter}`); finalization is the consuming feature's responsibility when it lands (v1 surfaces the flag; no automatic renumbering of stored records).

## R7 — Flyway versioning

- **Decision**: New module `sequence`, single migration `V1.0.83__create_sequence_module_tables.sql` written under **both** `db/migration/mysql/` and `db/migration/postgresql/`. `V1.0.82` is the current repo-wide max. Register `"sequence"` in `migrationModules` in `ampairs_service/build.gradle.kts`.

## R8 — Out-of-scope confirmations (FR-016)

Periodic resets, branch scopes, and prefix templates are accommodated structurally (open `entity_type` string, `scope` enum extensible, formatting isolated in one `SequenceFormatter`) but not implemented. No audit table in v1; allocations themselves provide coarse usage history.
