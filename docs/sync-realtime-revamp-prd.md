# PRD — Real-Time Sync Event Revamp (Backend ⇄ Mobile)

**Status:** Draft for review (rev 2)
**Owner:** omprakashsrv
**Repos:** `ampairs` (backend), `ampairs-app` (mobile KMP)
**Branch:** `claude/modest-hamilton-hE762`
**Constraint:** No database migrations. Reuse existing tables/columns only.

> **Terminology:** we track, per workspace per entity type, the **`lastUpdatedAt`** — the maximum
> `updatedAt` of that entity's rows. The full set `{entityType → lastUpdatedAt}` is the workspace's
> **sync checkpoint**. (Earlier draft called this a "watermark"; renamed for clarity.)

---

## 1. Summary

Make multi-device, multi-user sync fully **stateful and self-healing** by reconciling, on every
workspace connect / reconnect / hourly tick, the server's **`lastUpdatedAt` per entity type**
against the client's **last-synced `updatedAt` per entity type** — and by pushing lightweight
change signals over the existing per-workspace WebSocket while a client is live.

The client never guesses: at any moment each entity is either `IDLE` (in sync) or `PENDING_PULL`
(server is ahead). The change channel carries only *signals* — `entity_type` + `lastUpdatedAt` +
minimal metadata — never full record payloads.

The server does **not** query each module for its max `updatedAt`. Instead, every write
(single or bulk) stamps the affected rows' `max(updatedAt)` into the event it publishes
post-commit. The connect/hourly bootstrap simply reads the **latest persisted event per entity
type** — i.e. "the last event of each entity type."

---

## 2. Goals & Non-Goals

### Goals
- Fresh app launch / new workspace login → sync state **empty**; nothing pulled blindly.
- After workspace selection, the client opens the workspace WebSocket and **bootstraps**: reads
  the server's `lastUpdatedAt` per entity and marks only the lagging entities `PENDING_PULL`.
  Re-run on every **reconnect** and on an **hourly timer** (safety net for any missed live event).
- An entity type with **no server data** (no `lastUpdatedAt`) ⇒ client does **not** pull.
- While connected, any create/update/delete publishes a **slim** signal to all connected devices
  of the workspace in near-real-time, flipping the relevant entity to `PENDING_PULL` elsewhere.
- A client that missed events catches up from bootstrap reconciliation alone — no event-log replay.
- Correct multi-user / multi-device fan-out; the originating device does not echo-pull.
- Bounded storage: events are signal-sized; **no full entity rows** in the event stream.
- **Kafka is optional** — DB persistence + bootstrap guarantees correctness without it.
- **Zero migrations** — `lastUpdatedAt` is derived from existing `updatedAt` columns and carried in
  the existing `workspace_events.payload` column.

### Non-Goals
- No change to the offline-first write path (DB-first `synced=false`, push via `CentralSyncService`).
- No change to per-entity incremental pull internals (batched `updatedAt > lastSync`).
- No field-level/CRDT merge. Conflict policy stays **local-unsynced-wins on pull**, else last-write-wins.

---

## 3. Current State (as-is)

### 3.1 Backend (`ampairs/event` module)
- **WebSocket/STOMP**: `WebSocketConfig` (`@EnableWebSocketMessageBroker`), endpoint `/ws`, topics
  `/topic/workspace.events.{workspaceId}` + `/topic/workspace.status.{workspaceId}`, app prefix `/app`.
- **Auth/scoping**: `WebSocketAuthInterceptor` (handshake JWT) + `WebSocketChannelInterceptor`
  (validates subscribed `workspaceId` vs token `tenantId`, sets tenant/device context).
- **Persist + broadcast**: `WorkspaceEventListener` consumes Spring `ApplicationEvent`s, assigns a
  per-workspace monotonic `sequenceNumber`, persists a `WorkspaceEvent` row (with JSON `payload`),
  then broadcasts via `SimpMessagingTemplate` (or Kafka in multi-instance mode).
- **Kafka fan-out**: optional (`SIMPLE`/`KAFKA`/`AUTO`), topic `workspace-events`, per-instance
  consumer group.
- **Replay**: `GET /event/v1/events?sinceSequence=` (paginated). No "latest per entity type" /
  "max updatedAt per entity" endpoint.
- **Coverage**: events published manually from services for **Customer, Product, Order, Invoice**
  only. Missing: CustomerGroup/Type, Product sub-entities, Tax*, Unit*, Inventory, Business, Form, File.
- **Base entity**: `BaseDomain` sets `updatedAt: Instant` on `@PrePersist`/`@PreUpdate`.

### 3.2 Mobile (`ampairs-app`)
- **`CentralSyncService`** (`data/sync`): stateful per-entity machine
  (`IDLE → PENDING_PUSH/PENDING_PULL → SYNCING → SUCCESS/FAILED`), persisted Room `SyncStateDatabase`,
  per-entity push/pull mutexes, reactive re-trigger of pending states, lazy delegate resolution.
- **`EventManager`** (`data/event`): Krossbow STOMP; connects on workspace activation, subscribes
  to `/topic/workspace.events.{wsId}`, **filters its own `deviceId`**, auto-reconnects with backoff,
  forwards to `CentralSyncService.onBackendEvent(...)`. `onConnectionRestored()` currently only
  flushes pending **pushes**.
- **Incremental pull**: per-entity `updatedAt > lastSync`, 100-row batches; per-entity `lastSyncTime`
  (ISO-8601) in DataStore.
- **`SyncEntity`**: 15 types (customer, customer_group, customer_type, product, product_catalog,
  product_image, customer_image, order, invoice, business, tax, unit, inventory, form, file).

### 3.3 Gap Analysis
| # | Expected behavior | Today | Gap |
|---|---|---|---|
| 1 | Empty sync state on launch | ✅ | none |
| 2 | Connect/reconnect: read `lastUpdatedAt` per entity, mark laggards `PENDING_PULL` | ❌ | **Backend: no checkpoint endpoint. Mobile: no bootstrap.** |
| 3 | Hourly reconcile as safety net | ❌ | **add periodic bootstrap** |
| 4 | No data ⇒ no pull | ⚠️ implicit | explicit "null ⇒ skip" rule |
| 5 | Reconnect re-reconciles | ❌ (only flushes pushes) | extend `onConnectionRestored()` |
| 6 | Live change ⇒ slim signal ⇒ `PENDING_PULL` everywhere | ⚠️ partial coverage, heavy payload | slim + broaden coverage |
| 7 | Late subscriber gets last event per entity type | ❌ | satisfied by bootstrap-from-events |
| 8 | Don't store full records | ⚠️ full `payload` persisted | stamp only `lastUpdatedAt` |
| 9 | Multi-user/multi-device, no self-echo | ✅ deviceId filter | verify same-user-other-device |
| 10 | No migrations | — | derive from `updatedAt`; reuse `workspace_events.payload` |

---

## 4. Target Architecture

### 4.1 Core idea — Checkpoint Reconciliation (event-stamped)
The sync signal is, per workspace per entity, `lastUpdatedAt` = `max(updatedAt)` of that entity's
rows. It is produced at **write time**, not by querying modules:

- **Write path (server):** any create/update/delete (single or bulk) → **post-commit**, the service
  takes `max(updatedAt)` over the affected rows and publishes one `ApplicationEvent` carrying
  `{entityType, lastUpdatedAt, eventType, id?}`. (For deletes, use the soft-delete `updatedAt`; see §10.)
- **Persist (server):** `WorkspaceEventListener` writes a `workspace_events` row, stamping
  `lastUpdatedAt` into the existing `payload` column (no migration), and broadcasts the slim signal.
- **Bootstrap (client, on connect / reconnect / hourly):** GET the sync checkpoint = the **latest
  persisted event per entity type** (`GROUP BY entity_type, MAX(sequence_number)` → its stamped
  `lastUpdatedAt`). For each entity: `serverLastUpdatedAt > clientLastSync` → `PENDING_PULL`;
  `null` → `IDLE`; else `IDLE`. `CentralSyncService` auto-pulls laggards incrementally.
- **Live (client):** on a slim signal, if `signal.lastUpdatedAt > clientLastSync(entity)` →
  `PENDING_PULL`.
- **Self-healing:** correctness derives from the comparison, so *missed* live events are harmless —
  the next bootstrap (connect, reconnect, or hourly tick) re-detects the lag.

### 4.2 Lifecycle

```
App launch / login → select workspace → WorkspaceManager.activateWorkspace()
  ├─ start CentralSyncService (state empty)
  ├─ EventManager.connect() → STOMP /topic/workspace.events.{wsId}
  ├─ on CONNECTED → Bootstrap:
  │     GET /event/v1/sync/checkpoints
  │       └─ per entity: serverLastUpdatedAt > clientLastSync ? PENDING_PULL : IDLE
  │            └─ auto-pull (updatedAt > lastSync); advance clientLastSync; → SUCCESS/IDLE
  └─ hourly timer → re-run Bootstrap (safety net for missed live events)

While connected (another device writes X):
  service computes max(updatedAt) of affected X rows (post-commit)
    → publish ApplicationEvent → WorkspaceEventListener persists (stamp lastUpdatedAt) + broadcasts slim signal
       → every connected device except origin deviceId:
            signal.lastUpdatedAt > clientLastSync(X) ? PENDING_PULL(X) → auto-pull : ignore

Disconnect → reconnect:
  EventManager reconnects → onConnectionRestored()
    ├─ flush PENDING_PUSH (existing)
    └─ re-run Bootstrap (NEW)
```

### 4.3 Why bootstrap-via-REST (not STOMP push on SUBSCRIBE)
The "send the last event of each entity type to a late subscriber" requirement is satisfied by the
bootstrap reading the latest persisted event per entity type. Fetching it via a REST call right
after CONNECTED (and hourly) is deterministic, testable, and identical for first-connect, reconnect,
and the periodic tick — no per-session broker push plumbing. *(Optional later: also push on SUBSCRIBE
to save one round-trip.)*

### 4.4 Kafka — optional, not required
Because every event is persisted and the client reconciles on connect + reconnect + hourly, the
system is eventually consistent **without Kafka**. Kafka only reduces cross-instance real-time
latency when the backend runs on multiple instances (event on instance A → STOMP client on instance
B). Even then a missed cross-instance signal heals within the hourly cycle.
**Recommendation:** run `broker = SIMPLE` (in-memory), Kafka **off**, now; keep the existing Kafka
path as a config-gated latency optimization for future horizontal scaling.

---

## 5. Functional Requirements

### FR-1 Sync checkpoint endpoint (backend, NEW)
`GET /event/v1/sync/checkpoints` (workspace-scoped via `X-Workspace-ID`). Returns the latest
`lastUpdatedAt` per syncable entity type, read from the latest persisted event per entity type.
```json
{
  "success": true,
  "data": {
    "checkpoints": {
      "customer":       "2026-06-04T10:30:45.123Z",
      "customer_group": "2026-05-20T08:00:00.000Z",
      "product":        null,
      "order":          "2026-06-03T17:12:09.000Z",
      "tax":            "2026-04-01T00:00:00.000Z"
    },
    "server_time": "2026-06-04T10:31:00.000Z"
  }
}
```
- `null` ⇒ no signal for that entity ⇒ client must **not** pull.
- Source: `SELECT entity_type, MAX(sequence_number) ...` over `workspace_events`, reading the
  stamped `lastUpdatedAt` from `payload`. No per-module query, no migration.

### FR-2 Write-time stamping (backend)
On every create/update/delete (single **and** bulk), **post-commit**, the owning service:
1. computes `lastUpdatedAt = max(updatedAt)` over the affected rows (delete → soft-delete `updatedAt`);
2. publishes an `ApplicationEvent` carrying `{entityType, entityId?, lastUpdatedAt, eventType}`.
`WorkspaceEventListener` stamps `lastUpdatedAt` into `payload` and broadcasts the slim signal.
- Use a post-commit hook (`TransactionSynchronization` / `@TransactionalEventListener(AFTER_COMMIT)`)
  so the stamped time reflects committed state.

### FR-3 Client bootstrap on connect / reconnect / hourly (mobile, NEW)
- New `SyncBootstrapService` (or extend `EventConnectionManager`): on `ConnectionState.Connected`,
  on reconnect, and on an **hourly timer**, call the checkpoint endpoint and for each `SyncEntity`:
  - `serverLastUpdatedAt == null` → `IDLE`, no pull.
  - `serverLastUpdatedAt > clientLastSync` → `CentralSyncService.markPendingPull(entity)` → auto-pull.
  - else → `IDLE`.
- `onConnectionRestored()` runs **both** the existing push-flush **and** the bootstrap.
- Idempotent; safe to run repeatedly; debounced against reconnect storms; pulls serialized by mutex.

### FR-4 Slim live signal contract (backend + mobile)
```json
{
  "uid": "EVT_...",
  "event_type": "CUSTOMER_UPDATED",
  "entity_type": "customer",
  "entity_id": "CUS_123",                  // optional; for targeted single-record refresh
  "last_updated_at": "2026-06-04T10:30:45.123Z",  // max(updatedAt) of the affected rows
  "device_id": "DEV_A",                    // origin; receivers ignore their own
  "user_id": "USR_1",
  "sequence_number": 42,
  "workspace_id": "WS_1",
  "created_at": "2026-06-04T10:30:45.200Z"
}
```
- **No full entity payload.** Client reacts by pulling, not by trusting embedded data.
- Mobile `WorkspaceEvent` gains nullable `last_updated_at`. On receipt: if
  `last_updated_at == null || last_updated_at > clientLastSync(entity)` → `PENDING_PULL`
  (the `null` branch preserves today's always-pull fallback).

### FR-5 Storage minimization (backend)
- `workspace_events.payload` holds only `{ "last_updated_at": ... }` (or null) — never full records.
- Keep the existing daily cleanup of consumed events > 30 days; rows are now tiny.
- No new table.

### FR-6 Event coverage parity (backend)
Publish create/update/delete events for **every** independent `SyncEntity` (§8). Fill the gaps:
CustomerGroup, CustomerType, customer_image, product sub-entities, Tax*, Unit*, Business, Inventory,
Form, File — via the existing manual `ApplicationEventPublisher` pattern.

### FR-7 Multi-user / multi-device semantics
- Per-workspace broadcast to every connected device; origin device suppresses its own signal by
  `device_id` (a **different device of the same user still pulls**). Add a regression test.
- Checkpoint comparison makes duplicate/late signals idempotent.

---

## 6. API & Message Contracts (summary)
| Contract | Direction | Transport | Change |
|---|---|---|---|
| `GET /event/v1/sync/checkpoints` | client→server | REST | **NEW** |
| `/topic/workspace.events.{wsId}` | server→clients | STOMP | **slim payload + `last_updated_at`** |
| `GET /event/v1/events?sinceSequence=` | client→server | REST | unchanged (audit/debug) |
| Per-entity incremental pull `updatedAt > lastSync` | client→server | REST | unchanged |

---

## 7. No-Migration Strategy & Key Decisions
- **No new columns/tables.** `lastUpdatedAt` derives from existing `updatedAt`; it is carried in the
  existing `workspace_events.payload`. The checkpoint endpoint is a `GROUP BY entity_type` over that table.
- **D1 — Bootstrap transport:** REST on connect/reconnect/hourly *(recommended)* vs STOMP push on SUBSCRIBE.
- **D2 — Checkpoint source:** event-stamped + read-latest-per-entity-type *(chosen, per feedback)* — no per-module `MAX(updatedAt)` query.
- **D3 — Event payload:** signal-only *(chosen)*.
- **D4 — Kafka:** off / `SIMPLE` broker for now *(chosen)*; keep as optional multi-instance latency optimization.
- **D5 — Periodic reconcile:** hourly bootstrap *(chosen)*.

---

## 8. Entity Checkpoint Matrix
**Decision:** every syncable sub-entity gets its **own** checkpoint and pulls independently
(matches the existing `SyncEntity` enum).

| SyncEntity | Own checkpoint | Backend events today | Action |
|---|---|---|---|
| customer | ✅ | ✅ | slim payload |
| customer_group | ✅ | ❌ | add events |
| customer_type | ✅ | ❌ | add events |
| customer_image | ✅ | ❌ | add events |
| product | ✅ | ✅ | slim payload |
| product_catalog | ✅ | ❌ | add events |
| product_image | ✅ | ❌ | add events |
| order | ✅ | ✅ (order_item rides along) | slim payload |
| invoice | ✅ | ✅ (invoice_item rides along) | slim payload |
| business | ✅ | ❌ | add events |
| tax | ✅ | ❌ | add events |
| unit | ✅ | ❌ | add events |
| inventory | ✅ | ❌ | add events |
| form | ✅ | ❌ | add events |
| file | ✅ | ❌ | add events |

---

## 9. Implementation Plan
### 9.1 Backend (`ampairs`)
1. Post-commit stamping helper: compute `max(updatedAt)` of affected rows and publish an event.
2. Wire it into every owning service's create/update/delete (single + bulk).
3. `WorkspaceEventListener`: stamp `last_updated_at` into `payload`, broadcast slim signal.
4. `GET /event/v1/sync/checkpoints` controller (`ApiResponse<CheckpointsResponse>`; tenant context set/cleared at controller).
5. Coverage parity for the entities in §8.
6. Set `broker = SIMPLE` (Kafka off) in config; keep Kafka beans behind their condition.
7. Tests: checkpoint accuracy, tenant isolation, slim broadcast, delete propagation, multi-device fan-out.

### 9.2 Mobile (`ampairs-app`)
1. `WorkspaceEvent`: add nullable `last_updated_at`; update `onBackendEvent` comparison.
2. `CentralSyncService.markPendingPull(entity)` public entry (mirror of `markPendingPush`).
3. `SyncBootstrapService`: run on `Connected`, on reconnect, and on an hourly timer; call checkpoint API; compare vs DataStore `lastSyncTime`; mark laggards `PENDING_PULL`.
4. `onConnectionRestored()`: also run bootstrap.
5. Checkpoint API client + DTO (snake_case `@SerialName`).
6. Validate 3 targets: `shared:compileKotlinIosSimulatorArm64`, `androidApp:compileDebugKotlinAndroid`, `desktopApp:compileKotlin`.

---

## 10. Edge Cases & Failure Handling
- **Bootstrap/checkpoint call fails:** leave states unchanged; retry next reconnect/hourly; never wedge into `SYNCING`.
- **Delete propagation:** incremental pull (`updatedAt > lastSync`) won't return a hard-deleted row.
  Server must **soft-delete** (e.g. `active=false`, bump `updatedAt`) so the pull returns the tombstone
  and the client removes it locally. Required for any entity that can be deleted remotely.
- **Pre-existing data with no event row:** a brand-new client sees `null` checkpoint for an entity
  whose data predates stamping → would skip pulling it. Mitigation options (pick one): one-time
  seed of "latest event per entity type" per workspace; OR a fallback `MAX(updatedAt)` read only when
  the event-derived checkpoint is null. **Open — see §12.**
- **Clock skew:** comparisons use server-authoritative `updatedAt`; client never uses its own clock.
- **`last_updated_at == null` on a live signal:** treat as pull (safe fallback).
- **Same-user second device:** must still pull (filter is by `device_id`, not `user_id`).
- **Race: live signal during bootstrap:** idempotent; both converge to one serialized pull (per-entity mutex).
- **Reconnect storm:** bootstrap debounced/idempotent; pulls serialized by mutex.

---

## 11. Acceptance Criteria / Test Scenarios
1. Fresh login, empty workspace → checkpoints all `null` → **zero pulls**, all `IDLE`.
2. Fresh login, workspace with data → non-null entities → each pulls once → `IDLE`.
3. 2 users × 2 devices live; user1/device1 adds a customer → device1 does **not** pull; the other
   three (incl. user1/device2) receive the signal and pull.
4. Device offline during 3 changes, then reconnects → one bootstrap reconciles all 3; only laggards pull.
5. Idle device, hourly tick after a missed event → bootstrap detects lag and pulls.
6. WebSocket drops & auto-reconnects → bootstrap re-runs; nothing pulls if already in sync.
7. Event channel + `workspace_events` rows carry no full record payload (verify wire + row size).
8. Remote delete (soft-delete) propagates: other devices remove the row after pull.
9. New-coverage entities (tax, unit, customer_group, …) trigger `PENDING_PULL` on other devices.

---

## 12. Open Questions
1. **Pre-existing data with null checkpoint** (§10): one-time seed, or `MAX(updatedAt)` fallback only
   when the event-derived checkpoint is null? (The fallback re-introduces a per-module read but only
   on the null path / first run.)
2. Hourly interval — fixed 1h, or backoff when app is backgrounded / on cellular?
3. Confirm all deletable entities already soft-delete on the server (needed for delete propagation).
4. Should a live signal ever advance `lastSync` without a pull (micro-optimization), or always pull?
   *(Recommend always pull in v1.)*

---

## 13. Rollout Phases
- **Phase 1 (backend, additive):** post-commit stamping + `GET /sync/checkpoints` + `SIMPLE` broker.
- **Phase 2 (mobile):** bootstrap on connect/reconnect/hourly consuming Phase 1.
- **Phase 3 (backend):** slim signal payload + `last_updated_at`; mobile reads it.
- **Phase 4 (backend):** event coverage parity (§8) + soft-delete audit.
- **Phase 5:** storage tuning, tests, optional multi-instance Kafka validation.

Phase 1 + 2 alone deliver the core stateful connect/reconnect/hourly reconciliation.
