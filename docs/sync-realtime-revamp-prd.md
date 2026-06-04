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
- **Dependency-ordered pulls:** when several entities are `PENDING_PULL`, the client pulls them in
  topological order — referenced parents before dependents (e.g. customer_group + customer_type
  before customer; product_catalog before product). A dependent waits for its dependencies' pulls
  to finish.
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
| 7 | Late subscriber gets last state per entity type | ❌ | satisfied by data-derived bootstrap (FR-1) |
| 8 | Don't store full records | ⚠️ full `payload` persisted | stamp only `lastUpdatedAt` |
| 9 | Multi-user/multi-device, no self-echo | ✅ deviceId filter | verify same-user-other-device |
| 10 | No migrations | — | derive from `updatedAt`; reuse `workspace_events.payload` |

---

## 4. Target Architecture

### 4.1 Core idea — Checkpoint Reconciliation
`lastUpdatedAt` = per workspace per entity, the `max(updatedAt)` of that entity's rows. `BaseDomain`
sets `updatedAt` on every persist/update, so it is **never null** — an absent value means only
"no rows for this entity." Two independent sources feed the same comparison; **neither migrates nor
seeds existing data/events**:

- **Bootstrap (client, on connect / reconnect / hourly):** GET the sync checkpoint = the server's
  `MAX(updatedAt)` per entity, read **directly from each entity's own table**. This covers all
  pre-existing data with zero seeding. For each entity: `serverLastUpdatedAt > clientLastSync` →
  `PENDING_PULL`; absent (no rows) → `IDLE`; else `IDLE`. `CentralSyncService` auto-pulls laggards.
- **Live (server → client, while connected):** any create/update/delete → **post-commit** the
  service computes `max(updatedAt)` over the affected rows (no query — it is the rows just written;
  deletes use the soft-delete `updatedAt`, see §10) and publishes a slim signal
  `{entityType, lastUpdatedAt, eventType, id?}`. Client: `signal.lastUpdatedAt > clientLastSync` →
  `PENDING_PULL`.
- **Self-healing:** correctness derives from the comparison, so *missed* live signals are harmless —
  the next bootstrap re-detects the lag straight from live data. The `workspace_events` log is **not**
  the source of truth for checkpoints.

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
- `null` / absent ⇒ no rows for that entity ⇒ client must **not** pull.
- Source: server `MAX(updatedAt)` per entity, read from each owning module's table via its public
  service interface (cross-module access by interface, per module-boundary rules). Covers
  pre-existing data; **no event seeding, no migration**. Cheap, indexed `MAX` per entity, called
  only on connect/reconnect/hourly.

### FR-2 Write-time stamping for the live signal (backend)
On every create/update/delete (single **and** bulk), **post-commit**, the owning service:
1. computes `lastUpdatedAt = max(updatedAt)` over the affected rows (delete → soft-delete `updatedAt`);
2. publishes an `ApplicationEvent` carrying `{entityType, entityId?, lastUpdatedAt, eventType}`.
`WorkspaceEventListener` broadcasts the slim signal (and may persist a slim `workspace_events` row
for audit / the existing `/events` replay endpoint — persistence is **not** required for sync
correctness, since the bootstrap reads live data).
- Use a post-commit hook (`TransactionSynchronization` / `@TransactionalEventListener(AFTER_COMMIT)`)
  so the stamped time reflects committed state.
- This feeds the **live** path only; the bootstrap checkpoint (FR-1) is independent and reads data.

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
- The event log is no longer the source of truth for checkpoints, so persistence is optional/audit.
- If persisted, `workspace_events.payload` holds only `{ "last_updated_at": ... }` (or null) — never
  full records. Keep the existing daily cleanup of consumed events > 30 days; rows are tiny.
- No new table, no migration.

### FR-6 Event coverage parity (backend)
Publish create/update/delete events for **every** independent `SyncEntity` (§8). Fill the gaps:
CustomerGroup, CustomerType, customer_image, product sub-entities, Tax*, Unit*, Business, Inventory,
Form, File — via the existing manual `ApplicationEventPublisher` pattern.

### FR-7 Multi-user / multi-device semantics
- Per-workspace broadcast to every connected device; origin device suppresses its own signal by
  `device_id` (a **different device of the same user still pulls**). Add a regression test.
- Checkpoint comparison makes duplicate/late signals idempotent.

### FR-8 Dependency-ordered pulls (mobile)
Each entity declares the entities it depends on for a **pull** (its referenced parents). When
bootstrap (or live signals) leave several entities `PENDING_PULL`, `CentralSyncService` resolves a
topological order and pulls **dependencies first**, in waves:
- A wave of entities whose dependencies are all satisfied (already `IDLE`/`SUCCESS`, or have no
  pending dependency) is pulled; the next wave starts only after the previous wave's pulls reach
  `SUCCESS`.
- If a dependency's pull **fails**, its dependents are **held** in `PENDING_PULL` (not pulled with a
  missing parent) and retried on the next cycle (reconnect / hourly).
- Reuse and generalize the existing `SyncDelegate.pushDependencies` into a shared dependency
  declaration (or add a parallel `pullDependencies`). Push already wants the same "dependencies
  first" order (create parent on server before the child that references it), so a single
  `dependsOn` list can drive **both** push and pull ordering.
- The dependency set forms a DAG (see §8). Cycles are not allowed; validate at startup.

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
- **No new columns/tables, no data/event seeding.** `lastUpdatedAt` derives from the existing
  `updatedAt` column. The checkpoint endpoint reads `MAX(updatedAt)` per entity table; the live
  signal carries the write-time `max(updatedAt)` of the affected rows.
- **D1 — Bootstrap transport:** REST on connect/reconnect/hourly *(recommended)* vs STOMP push on SUBSCRIBE.
- **D2 — Checkpoint source:** *bootstrap* reads `MAX(updatedAt)` directly from entity tables —
  covers legacy data with no seeding; *live signal* carries the write-time `max(updatedAt)`. (The
  per-module `MAX` read is used only for the infrequent bootstrap, **not** as the live change-detection
  mechanism — live detection is the write-time push, per earlier feedback.)
- **D3 — Event payload:** signal-only *(chosen)*.
- **D4 — Kafka:** off / `SIMPLE` broker for now *(chosen)*; keep as optional multi-instance latency optimization.
- **D5 — Periodic reconcile:** hourly bootstrap *(chosen)*.

---

## 8. Entity Checkpoint Matrix
**Decision:** every syncable sub-entity gets its **own** checkpoint and pulls independently
(matches the existing `SyncEntity` enum).

`Depends on` = entities that must be pulled **before** this one (DAG, drives both pull and push order).

| SyncEntity | Own checkpoint | Depends on (pull before) | Backend events today | Action |
|---|---|---|---|---|
| customer_group | ✅ | — | ❌ | add events |
| customer_type | ✅ | — | ❌ | add events |
| customer | ✅ | customer_group, customer_type | ✅ | slim payload |
| customer_image | ✅ | customer | ❌ | add events |
| product_catalog | ✅ | — | ❌ | add events |
| tax | ✅ | — | ❌ | add events |
| unit | ✅ | — | ❌ | add events |
| product | ✅ | product_catalog, unit, tax | ✅ | slim payload |
| product_image | ✅ | product | ❌ | add events |
| order | ✅ | customer, product | ✅ (order_item rides along) | slim payload |
| invoice | ✅ | order, customer, product, tax | ✅ (invoice_item rides along) | slim payload |
| inventory | ✅ | product | ❌ | add events |
| business | ✅ | — | ❌ | add events |
| form | ✅ | — | ❌ | add events |
| file | ✅ | — | ❌ | add events |

> Leaf entities (no deps) — customer_group, customer_type, product_catalog, tax, unit, business,
> form, file — pull first; dependents follow in topological waves. **Edges confirmed:** product
> depends on product_catalog + unit + tax; order depends on customer + product.

---

## 9. Implementation Plan
### 9.1 Backend (`ampairs`)
1. Per-module `maxUpdatedAt(workspaceId): Instant?` (derived `@Query MAX(updatedAt)`, existing column).
2. `SyncCheckpointService` (event module) aggregating per-entity `maxUpdatedAt` via public service interfaces.
3. `GET /event/v1/sync/checkpoints` controller (`ApiResponse<CheckpointsResponse>`; tenant context set/cleared at controller).
4. Post-commit stamping helper for the **live signal**: compute `max(updatedAt)` of affected rows and publish an `ApplicationEvent`; wire into every owning service's create/update/delete (single + bulk).
5. `WorkspaceEventListener`: broadcast slim signal (payload empty/slim; persistence optional/audit).
6. Coverage parity for the entities in §8.
7. Set `broker = SIMPLE` (Kafka off) in config; keep Kafka beans behind their condition.
8. Tests: checkpoint accuracy (incl. legacy data), tenant isolation, slim broadcast, delete propagation, multi-device fan-out.

### 9.2 Mobile (`ampairs-app`)
1. `WorkspaceEvent`: add nullable `last_updated_at`; update `onBackendEvent` comparison.
2. `CentralSyncService.markPendingPull(entity)` public entry (mirror of `markPendingPush`).
3. `SyncBootstrapService`: run on `Connected`, on reconnect, and on an hourly timer; call checkpoint API; compare vs DataStore `lastSyncTime`; mark laggards `PENDING_PULL`.
4. `onConnectionRestored()`: also run bootstrap.
5. Checkpoint API client + DTO (snake_case `@SerialName`).
6. **Dependency ordering:** generalize `SyncDelegate.pushDependencies` into a single `dependsOn`
   list per delegate; add a topological scheduler in `CentralSyncService` that pulls dependencies
   before dependents in waves, holding dependents if a dependency pull fails. Validate the DAG
   (no cycles) at startup.
7. Validate 3 targets: `shared:compileKotlinIosSimulatorArm64`, `androidApp:compileDebugKotlinAndroid`, `desktopApp:compileKotlin`.

---

## 10. Edge Cases & Failure Handling
- **Bootstrap/checkpoint call fails:** leave states unchanged; retry next reconnect/hourly; never wedge into `SYNCING`.
- **Delete propagation:** incremental pull (`updatedAt > lastSync`) won't return a hard-deleted row.
  **Confirmed:** all deletable entities soft-delete server-side (`active=false`, bump `updatedAt`),
  so the pull returns the tombstone and the client removes it locally.
- **Pre-existing / legacy data:** resolved — the bootstrap reads `MAX(updatedAt)` **directly from
  each entity table** (FR-1), so all existing data is covered with no event/data seeding. `updatedAt`
  is never null (`BaseDomain`), so an absent checkpoint unambiguously means "no rows."
- **Clock skew:** comparisons use server-authoritative `updatedAt`; client never uses its own clock.
- **`last_updated_at == null` on a live signal:** treat as pull (safe fallback).
- **Same-user second device:** must still pull (filter is by `device_id`, not `user_id`).
- **Race: live signal during bootstrap:** idempotent; both converge to one serialized pull (per-entity mutex).
- **Reconnect storm:** bootstrap debounced/idempotent; pulls serialized by mutex.
- **Dependency pull fails:** dependents stay `PENDING_PULL` and are **not** pulled with a missing
  parent; they retry on the next reconnect/hourly cycle. A persistently failing leaf therefore
  stalls its subtree until it recovers (acceptable — better than inserting orphaned children).

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
10. **Dependency order:** fresh login where customer_group, customer_type, and customer are all
    stale → groups+types pull and reach `SUCCESS` **before** customer's pull starts; with a forced
    failure on customer_type, customer is **held** (not pulled) until the next cycle.

---

## 12. Open Questions
1. Hourly interval — fixed 1h, or backoff when app is backgrounded / on cellular?
2. Should a live signal ever advance `lastSync` without a pull (micro-optimization), or always pull?
   *(Recommend always pull in v1.)*

**Resolved:**
- §8 dependency edges — product ← product_catalog + unit + tax; order ← customer + product.
- Delete propagation — all deletable entities soft-delete server-side (pull carries tombstone).
- Legacy data — bootstrap reads `MAX(updatedAt)` from entity tables; no seeding/migration; `updatedAt` never null.
- Checkpoint source — data-derived bootstrap + write-time live signal; event log not the source of truth.

---

## 13. Rollout Phases
- **Phase 1 (backend, additive):** data-derived `GET /sync/checkpoints` + post-commit live stamping + `SIMPLE` broker.
- **Phase 2 (mobile):** bootstrap on connect/reconnect/hourly consuming Phase 1.
- **Phase 3 (backend):** slim signal payload + `last_updated_at`; mobile reads it.
- **Phase 4 (backend):** event coverage parity (§8) + soft-delete audit.
- **Phase 5:** storage tuning, tests, optional multi-instance Kafka validation.

Phase 1 + 2 alone deliver the core stateful connect/reconnect/hourly reconciliation.
