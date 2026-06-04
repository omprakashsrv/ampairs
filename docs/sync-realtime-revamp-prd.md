# PRD — Real-Time Sync Event Revamp (Backend ⇄ Mobile)

**Status:** Draft for review
**Owner:** omprakashsrv
**Repos:** `ampairs` (backend), `ampairs-app` (mobile KMP)
**Branch:** `claude/modest-hamilton-hE762`
**Constraint:** No database migrations. Reuse existing tables/columns only.

---

## 1. Summary

Make multi-device, multi-user data synchronization fully **stateful and self-healing** by
reconciling, on every workspace connect/reconnect, the server's *latest-updated watermark per
entity type* against the client's *last-synced watermark per entity type* — and by pushing
lightweight change signals over the existing per-workspace WebSocket while a client is live.

The client never has to guess what to sync: at any moment it knows, per entity, whether it is
`IDLE` (in sync) or `PENDING_PULL` (server is ahead). The server stores only *signals*
(entity type + watermark + minimal metadata), never full record payloads in the event stream.

---

## 2. Goals & Non-Goals

### Goals
- On app launch / fresh workspace login, sync state starts **empty**; nothing is pulled blindly.
- After workspace selection, the client opens the workspace WebSocket and **bootstraps**: it
  fetches the server's max-`updatedAt` per entity type and marks only the lagging entities
  `PENDING_PULL`. The same bootstrap runs on every reconnect.
- If an entity type has **no server data** (no watermark), the client does **not** trigger a pull.
- While connected, any create/update/delete on the server publishes a **lightweight** event to
  all connected devices of that workspace in near-real-time, flipping the relevant entity to
  `PENDING_PULL` on every other device.
- A client that connects **after** events were missed catches up purely from the bootstrap
  reconciliation — no event-log replay required for correctness.
- Correct multi-user / multi-device fan-out: a write on one device updates all other devices
  (including the same user's other devices); the originating device does not echo-pull.
- Storage stays bounded: the event channel carries `{entityType, watermark, eventType, id}` —
  **not** full entity rows. No unbounded Kafka/DB growth.
- **Zero migrations.** Achieved by deriving watermarks from existing `updatedAt` columns and
  reusing the existing `workspace_events` table.

### Non-Goals
- No change to the offline-first write path (DB-first `synced=false`, push via `CentralSyncService`).
- No change to per-entity incremental pull internals (batched `updatedAt > lastSync`).
- No field-level/CRDT merge. Conflict policy stays **local-unsynced-wins on pull**, last-write-wins otherwise.
- No new event types for entities that are pulled as part of a parent (decided per entity below).

---

## 3. Current State (as-is)

### 3.1 Backend (`ampairs/event` module)
- **WebSocket/STOMP**: `WebSocketConfig` (`@EnableWebSocketMessageBroker`), endpoint `/ws`,
  topics `/topic/workspace.events.{workspaceId}` and `/topic/workspace.status.{workspaceId}`,
  app prefix `/app`.
- **Auth/scoping**: `WebSocketAuthInterceptor` (handshake, JWT from `?token=` or `Authorization`)
  + `WebSocketChannelInterceptor` (per-frame `CONNECT`/`SUBSCRIBE`/`SEND`, validates the
  subscribed `workspaceId` against the token's `tenantId`, sets `TenantContextHolder` /
  `DeviceContextHolder`).
- **Event persistence + broadcast**: `WorkspaceEventListener` consumes Spring
  `ApplicationEvent`s, assigns a per-workspace monotonic `sequenceNumber`, persists a
  `WorkspaceEvent` row (with JSON `payload`), then broadcasts `WorkspaceEventResponse` via
  `SimpMessagingTemplate` (or Kafka in multi-instance mode).
- **Kafka fan-out**: optional (`SIMPLE` / `KAFKA` / `AUTO`). Topic `workspace-events`,
  per-instance consumer group so every instance receives every message and re-delivers to its
  local SimpleBroker subscribers.
- **Replay**: `GET /event/v1/events?sinceSequence=` returns events after a sequence (paginated,
  can exclude a device). There is **no** "latest per entity type" or "max updatedAt per entity"
  endpoint.
- **Coverage**: events published manually from services for **Customer, Product, Order, Invoice**
  only. Missing: CustomerGroup/Type, Product sub-entities, Tax*, Unit*, Inventory, Business, Form, File.
- **Base entity**: `BaseDomain` sets `updatedAt: Instant` on `@PrePersist`/`@PreUpdate`. No JPA
  `@PostPersist`/`@PostUpdate` event hooks — publishing is manual in services.

### 3.2 Mobile (`ampairs-app`)
- **`CentralSyncService`** (`data/sync`): stateful per-entity machine
  (`IDLE → PENDING_PUSH/PENDING_PULL → SYNCING → SUCCESS/FAILED`), persisted in Room
  `SyncStateDatabase` (`entity_sync_state`), per-entity push/pull mutexes, reactive
  re-trigger of pending states on restart, lazy delegate resolution.
- **`EventManager`** (`data/event`): Krossbow STOMP client; on workspace activation connects to
  `ApiUrlBuilder.wsUrl("ws")?token=&workspaceId=` and subscribes to
  `/topic/workspace.events.{workspaceId}`; parses `WorkspaceEvent` JSON, **filters its own
  `deviceId`**, auto-reconnects with backoff, forwards events to
  `CentralSyncService.onBackendEvent(...)` → marks entity `PENDING_PULL`. On reconnect it calls
  `onConnectionRestored()` (currently only flushes pending **pushes**).
- **Incremental pull**: each repository pulls `updatedAt > lastSync` in 100-row batches; per-entity
  `lastSyncTime` (ISO-8601 string) persisted in DataStore.
- **`SyncEntity`**: 15 types (customer, customer_group, customer_type, product, product_catalog,
  product_image, customer_image, order, invoice, business, tax, unit, inventory, form, file).

### 3.3 Gap Analysis (as-is vs. expected behavior)
| # | Expected behavior | Today | Gap |
|---|---|---|---|
| 1 | Empty sync state on launch | ✅ starts empty | none |
| 2 | On connect/reconnect: fetch max `updatedAt` per entity, mark laggards `PENDING_PULL` | ❌ no bootstrap; pulls only via manual ViewModel triggers or live events | **Backend: no markers endpoint. Mobile: no bootstrap step.** |
| 3 | No watermark ⇒ no pull | ⚠️ implicit | needs explicit "null watermark ⇒ skip" rule |
| 4 | Reconnect re-reconciles | ❌ reconnect only flushes pushes | extend `onConnectionRestored()` to re-bootstrap |
| 5 | Live change ⇒ lightweight signal to all devices ⇒ `PENDING_PULL` | ⚠️ works for Customer/Product/Order/Invoice; payload is heavy; coverage partial | broaden coverage, slim payload |
| 6 | Late subscriber gets "last event per entity type" | ❌ only sequence replay endpoint | satisfied by the new bootstrap snapshot |
| 7 | Don't store every record in Kafka | ⚠️ full `payload` persisted & broadcast | strip payload to signal-only |
| 8 | Multi-user/multi-device fan-out, no self-echo | ✅ deviceId filter | none (verify same-user-other-device) |
| 9 | No migrations | — | derive watermarks from existing `updatedAt`; reuse `workspace_events` |

---

## 4. Target Architecture

### 4.1 Core idea — Watermark Reconciliation
The authoritative sync signal is, per workspace per entity type, the **max `updatedAt` of that
entity's rows** (`Instant`, ISO-8601). Call it the *server watermark*. The client stores its
*last-synced watermark* per entity (already does, as `lastSyncTime`).

- **Bootstrap (connect/reconnect):** client GETs the full `{entityType → serverWatermark}` map in
  one call. For each entity: if `serverWatermark != null && serverWatermark > clientWatermark` →
  `PENDING_PULL`; else `IDLE`. `CentralSyncService` auto-pulls the laggards incrementally.
- **Live (while connected):** on any server-side change, broadcast a slim event carrying the
  entity type and the new watermark. Client: if `eventWatermark > clientWatermark` → `PENDING_PULL`.
- **Self-healing:** because correctness derives from the watermark comparison, *missed* live
  events are irrelevant — the next bootstrap (or any subsequent event) re-detects the lag. This
  is why no event-log replay is needed for data integrity.

### 4.2 Lifecycle

```
App launch / login
  └─ select workspace
       └─ WorkspaceManager.activateWorkspace()
            ├─ start CentralSyncService (state empty)
            ├─ EventManager.connect()  → STOMP /topic/workspace.events.{wsId}
            └─ on CONNECTED:
                 └─ Bootstrap: GET /event/v1/sync/watermarks
                      └─ for each entity: serverWM > clientWM ? PENDING_PULL : IDLE
                           └─ CentralSyncService auto-pull (incremental, updatedAt > lastSync)
                                └─ advance clientWM = max(pulled updatedAt); state → SUCCESS/IDLE

While connected (any other device writes X)
  backend service publishes change → WorkspaceEventListener
     → updates/derives watermark(X) → broadcast slim signal to /topic/workspace.events.{wsId}
        → every connected device (except origin deviceId):
             eventWM(X) > clientWM(X) ? PENDING_PULL(X) → auto-pull : ignore

Disconnect → reconnect (network blip, app resume)
  EventManager reconnects → onConnectionRestored()
     ├─ flush PENDING_PUSH (existing)
     └─ re-run Bootstrap (NEW) → reconcile anything missed while offline
```

### 4.3 Why bootstrap-via-REST (not STOMP server-push on SUBSCRIBE)
The spec's "send the last event of each entity type to a late subscriber" is satisfied by the
bootstrap snapshot. We fetch it via a **REST call immediately after CONNECTED** rather than a
server-initiated push on SUBSCRIBE because:
- Deterministic & testable; no race between SUBSCRIBE ack and the pushed snapshot.
- Works identically for first connect and every reconnect.
- No per-session user-queue plumbing in the broker.
*(Optional future enhancement: also push the snapshot to the session on SUBSCRIBE for one fewer
round-trip. Not required for v1.)*

---

## 5. Functional Requirements

### FR-1 Sync Watermarks endpoint (backend, NEW)
`GET /event/v1/sync/watermarks` (workspace-scoped via `X-Workspace-ID`).
Returns the max `updatedAt` per syncable entity type for the current workspace.

```json
{
  "success": true,
  "data": {
    "watermarks": {
      "customer":        "2026-06-04T10:30:45.123Z",
      "customer_group":  "2026-05-20T08:00:00.000Z",
      "product":         null,
      "order":           "2026-06-03T17:12:09.000Z",
      "invoice":         null,
      "tax":             "2026-04-01T00:00:00.000Z",
      "unit":            "2026-04-01T00:00:00.000Z"
    },
    "server_time": "2026-06-04T10:31:00.000Z"
  }
}
```
- `null` ⇒ no rows for that entity in this workspace ⇒ client must **not** pull.
- Each value is the server's `MAX(updatedAt)` for that entity, derived from existing columns.
- One round-trip for the whole workspace.

### FR-2 Watermark derivation (backend, NEW, no migration)
A `SyncWatermarkService` in the `event` module aggregates watermarks by calling each domain
module's **public service interface** (cross-module access via interfaces, never direct repo).
Each owning module exposes a tiny read:
```kotlin
// owning module's public service (uses existing updatedAt column — no migration)
fun maxUpdatedAt(workspaceId: String): Instant?   // @Query("select max(e.updatedAt) from X e")
```
- Tenant context set at the controller, cleared in `finally` (per multi-tenancy rule).
- Entities pulled as part of a parent (e.g. `product_catalog`, `order_item`, `invoice_item`,
  images) do **not** get an independent watermark unless the client pulls them independently —
  see §8 entity matrix.

### FR-3 Client bootstrap on connect/reconnect (mobile, NEW)
- New `SyncBootstrapService` (or extend `EventConnectionManager`): on `ConnectionState.Connected`,
  call the watermarks endpoint, then for each `SyncEntity`:
  - `serverWM == null` → ensure `IDLE`, no pull.
  - `serverWM > clientLastSync` → `CentralSyncService.markPendingPull(entity)` → auto-pull.
  - else → `IDLE`.
- `onConnectionRestored()` must now run **both** the existing push-flush **and** the bootstrap.
- Bootstrap must be idempotent and safe to run repeatedly.

### FR-4 Slim live event contract (backend + mobile)
The broadcast envelope is reduced to a signal:
```json
{
  "uid": "EVT_...",
  "event_type": "CUSTOMER_UPDATED",
  "entity_type": "customer",
  "entity_id": "CUS_123",          // optional; for targeted single-record refresh
  "watermark": "2026-06-04T10:30:45.123Z",  // entity.updatedAt that triggered this event
  "device_id": "DEV_A",            // origin; receivers ignore their own
  "user_id": "USR_1",
  "sequence_number": 42,
  "workspace_id": "WS_1",
  "created_at": "2026-06-04T10:30:45.200Z"
}
```
- **No full entity `payload`.** The client reacts by pulling, not by trusting embedded data.
- Mobile `WorkspaceEvent` model gains `watermark` (nullable). On receipt: if
  `watermark == null || watermark > clientLastSync(entity)` → `PENDING_PULL`. The `null` case
  preserves today's "always pull on event" behavior as a safe fallback.

### FR-5 Storage minimization (backend)
- Stop persisting heavy `payload` JSON in `workspace_events` for sync events (write `null`/empty
  or a tiny `{watermark}` only). The full record is always re-fetchable via the entity's own API.
- Keep the existing daily cleanup of consumed events > 30 days. Because the event row is now tiny
  and correctness no longer depends on the log, retention can be aggressive.
- No new table. (If, in a later phase, a `MAX(updatedAt)` GROUP-BY scan over `workspace_events`
  is preferred over per-module queries, that is also migration-free — see §7 Decision D2.)

### FR-6 Event coverage parity (backend)
Publish create/update/delete events for **every client-syncable entity** that has an independent
`SyncEntity` watermark (§8). Fill the gaps: CustomerGroup, CustomerType, Tax*, Unit*, Business,
Inventory, Form. Use the existing manual `ApplicationEventPublisher` pattern in each service.

### FR-7 Multi-user / multi-device semantics
- Broadcast is per-workspace; every connected device receives it.
- Origin device suppresses its own event by `device_id` (already implemented) — this prevents a
  redundant pull right after its own push, but a **different device of the same user** still
  receives and pulls. Verify and add a regression test.
- Watermark comparison makes duplicate/late events harmless (idempotent).

---

## 6. API & Message Contracts (summary)

| Contract | Direction | Transport | Change |
|---|---|---|---|
| `GET /event/v1/sync/watermarks` | client→server | REST | **NEW** |
| `/topic/workspace.events.{wsId}` | server→clients | STOMP | **slim payload + `watermark` field** |
| `GET /event/v1/events?sinceSequence=` | client→server | REST | unchanged (kept for audit/debug) |
| Per-entity incremental pull `updatedAt > lastSync` | client→server | REST | unchanged |

---

## 7. No-Migration Strategy & Key Decisions

- **No new columns/tables.** Watermarks come from existing `updatedAt` (`Instant`, already on
  `BaseDomain`). The `workspace_events` table is reused; we only write smaller rows.
- **D1 — Bootstrap transport:** REST snapshot on CONNECTED (recommended) vs STOMP push on
  SUBSCRIBE. → *Recommend REST for v1.*
- **D2 — Watermark source:** per-module `MAX(updatedAt)` via service interfaces (accurate,
  matches the client's pull filter) vs `MAX(created_at)` GROUP BY over `workspace_events` (one
  query, but event-time skews ahead of entity `updatedAt` and risks the client skipping the very
  record that fired the event). → *Recommend per-module `MAX(updatedAt)`.*
- **D3 — Event payload:** signal-only (recommended) vs keep full payload. → *Signal-only.*
- **D4 — Late-subscriber catch-up:** rely solely on bootstrap reconciliation (recommended) vs
  also replay `workspace_events` by sequence. → *Bootstrap only; sequence replay stays as
  audit/debug.*

---

## 8. Entity Watermark Matrix (proposed)

| SyncEntity | Independent watermark? | Backend module exposes `maxUpdatedAt`? | Notes |
|---|---|---|---|
| customer | ✅ | yes (add) | |
| customer_group | ✅ | add | event coverage missing today |
| customer_type | ✅ | add | event coverage missing today |
| customer_image | ✅ | add | image upload sync |
| product | ✅ | yes (add) | |
| product_catalog | ⚠️ | via product | pulled with product; or own watermark if pulled independently |
| product_image | ✅ | add | |
| order | ✅ | add (`getOrders(lastUpdated)` exists) | order_item rides along |
| invoice | ✅ | add (`getInvoices(lastUpdated)` exists) | invoice_item rides along |
| business | ✅ | add | |
| tax | ✅ | add | all tax sub-entities missing events today |
| unit | ✅ | add | |
| inventory | ✅ | add | |
| form | ✅ | add | |
| file | ✅ | add | |

*Confirm per entity whether the client pulls it independently before granting an independent watermark.*

---

## 9. Implementation Plan (by repo)

### 9.1 Backend (`ampairs`)
1. **`SyncWatermarkService`** (event module) — aggregates per-entity `maxUpdatedAt(workspaceId)`.
2. **`maxUpdatedAt` reads** in each owning module's public service + repo (`@Query` over existing
   `updatedAt`). No migration.
3. **`GET /event/v1/sync/watermarks`** controller (`ApiResponse<WatermarksResponse>`, tenant
   context set/cleared at controller).
4. **Slim the broadcast**: add `watermark` to `WorkspaceEventResponse`, stop writing heavy
   `payload` in `WorkspaceEventListener.persistAndBroadcast()`.
5. **Coverage**: publish create/update/delete `ApplicationEvent`s for the missing entities (§8).
6. Tests: watermark accuracy, tenant isolation, slim-payload broadcast, multi-instance Kafka path.

### 9.2 Mobile (`ampairs-app`)
1. **`WorkspaceEvent`** model: add nullable `watermark`; bump `onBackendEvent` to compare.
2. **`CentralSyncService.markPendingPull(entity)`** public entry (mirror of `markPendingPush`).
3. **Bootstrap**: new step run on `ConnectionState.Connected` (in `EventConnectionManager` /
   `SyncBootstrapService`) — call watermarks API, compare vs DataStore `lastSyncTime`, mark
   laggards `PENDING_PULL`.
4. **`onConnectionRestored()`**: also run bootstrap (not just push-flush).
5. Watermarks API client (`data/sync` or `data/event`) + DTO with snake_case `@SerialName`.
6. Validate all 3 targets: `shared:compileKotlinIosSimulatorArm64`,
   `androidApp:compileDebugKotlinAndroid`, `desktopApp:compileKotlin`.

---

## 10. Edge Cases & Failure Handling
- **Bootstrap call fails** (offline/5xx): leave states unchanged; retry on next reconnect; do not
  wedge into `SYNCING`.
- **Clock skew**: comparisons use the server-authoritative `updatedAt`; client never uses its own
  clock as a watermark (consistent with existing offline-sync rule).
- **Event with `watermark=null`**: treat as "pull" (safe fallback) — preserves current behavior.
- **Same-user second device**: must still pull (filter is by `device_id`, not `user_id`).
- **Race: live event arrives mid-bootstrap**: idempotent — both paths converge to `PENDING_PULL`
  then a single serialized pull (per-entity mutex).
- **Entity with no data**: `null` watermark ⇒ no pull, state stays `IDLE`.
- **Reconnect storm**: bootstrap must be debounced/idempotent; pulls are serialized by mutex.

---

## 11. Acceptance Criteria / Test Scenarios
1. Fresh login, empty workspace → bootstrap returns all `null` → **zero pulls**, all `IDLE`.
2. Fresh login, workspace with data → bootstrap marks every non-null entity `PENDING_PULL` → each
   pulls once → states `IDLE`.
3. 2 users × 2 devices all live; user1/device1 adds a customer → device1 does **not** pull;
   user1/device2, user2/device1, user2/device2 all receive the signal and pull the new customer.
4. Device offline during 3 changes, then reconnects → single bootstrap reconciles all 3 (no
   per-event replay) → exactly the lagging entities pull.
5. WebSocket drops & auto-reconnects → bootstrap re-runs; nothing pulls if already in sync.
6. Event channel carries no full record payload (verify wire + DB row size).
7. `workspace_events` rows for sync events contain no heavy payload; cleanup still runs.
8. New-coverage entities (tax, unit, customer_group, …) trigger `PENDING_PULL` on other devices.

---

## 12. Open Questions
1. Confirm the final independent-watermark set in §8 (esp. `product_catalog`, images).
2. D1: accept REST-on-connect bootstrap for v1 (vs STOMP push)?
3. Retention for the now-slim `workspace_events` log — keep 30-day cleanup or shorten?
4. Should the slim event's `watermark` ever be trusted to advance `lastSyncTime` without a pull
   (optimization), or always pull? (Recommend always pull in v1 for simplicity.)
5. Any entity where embedding a tiny payload (to avoid an extra fetch) is worth the size?

---

## 13. Rollout Phases
- **Phase 1 (backend, additive, no migration):** watermarks endpoint + per-module `maxUpdatedAt`.
- **Phase 2 (mobile):** bootstrap on connect/reconnect consuming Phase 1.
- **Phase 3 (backend):** slim event payload + `watermark` field; mobile reads it.
- **Phase 4 (backend):** event coverage parity for all syncable entities.
- **Phase 5:** storage minimization tuning + tests + multi-instance Kafka validation.

Each phase is independently shippable; Phase 1+2 alone delivers the core "stateful reconcile on
connect/reconnect" behavior.
