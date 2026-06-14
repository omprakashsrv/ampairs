# event module

Captures domain events from all modules, persists them to the database, and delivers them to connected clients via WebSocket (STOMP). Provides device heartbeat tracking for presence awareness.

## Responsibilities

- Capture domain change events from all modules and collapse them to a per-(workspace, entity_type)
  watermark — one row per entity type, replaced on every new event
- Stream the latest watermark to workspace clients over WebSocket
- Device presence tracking (ONLINE / AWAY / OFFLINE)
- Heartbeat endpoint for connection keep-alive

The watermark model is intentional: the client uses it as a nudge to refetch via the per-module
`/sync` endpoint, which is the authoritative source. The event row never carries the entity payload.

## REST Endpoints

### Events (`/api/v1/events`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/events` | Watermarks newer than `sinceSequence` (catch-up) |
| GET | `/api/v1/events/all` | All current watermarks for the workspace |
| GET | `/api/v1/events/{eventId}` | Get event by UID |

### Device Status (`/api/v1/devices`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/devices/active` | Active devices in workspace |
| GET | `/api/v1/devices/active/user/{userId}` | Active devices for a user |
| GET | `/api/v1/devices/active/count` | Count of active devices |

### WebSocket Endpoints

| Endpoint | Protocol | Description |
|----------|----------|-------------|
| `/app/heartbeat` | STOMP | Device heartbeat (keep session alive) |
| `/app/ping` | STOMP | Connectivity ping |
| `/topic/workspace/{workspaceId}` | STOMP subscribe | Receive workspace events |

## Domain Events Published

Events are Spring `ApplicationEvent` objects published by domain services and captured by `WorkspaceEventListener`:

| Event class | Trigger |
|-------------|---------|
| `OrderEvents.*` | Order created, updated, status changed |
| `InvoiceEvents.*` | Invoice created, updated, status changed |
| `ProductEvents.*` | Product created, updated, deleted |
| `CustomerEvents.*` | Customer created, updated, deleted |
| `MemberEvents.*` | Member added, removed, role changed |

## Key Entities

### WorkspaceEvent

Holds **one row per `(workspace_id, entity_type)`** — the latest change watermark for that type
in that workspace. UPSERT on every event: overwrite `entity_id`, `event_type`, `sequence_number`,
and `payload` (a `last_updated_at` stamp); preserve the original `uid` and `created_at`.

```kotlin
class WorkspaceEvent : OwnableBaseDomain() {
    val eventType: EventType           // latest change kind (ORDER_CREATED, INVOICE_PAID, …)
    val entityType: String             // "order", "invoice", "product", … — part of the unique key
    val entityId: String               // UID of the most recently changed entity of this type
    val payload: String                // {"last_updated_at": "<ISO instant>"} — no entity payload
    val deviceId: String               // device that triggered the latest change
    val userId: String
    val workspaceId: String            // part of the unique key
    val sequenceNumber: Long           // vended atomically from workspace_event_sequence
}
```

### WorkspaceEventSequence

Single-row-per-workspace counter table backing atomic sequence number generation. Replaces the
prior racy `MAX(sequence_number) + 1` lookup that was tripping `uk_workspace_sequence` under
concurrent writes.

```sql
workspace_event_sequence (
    workspace_id VARCHAR(40) PRIMARY KEY,
    current_seq  BIGINT NOT NULL DEFAULT 0,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
)
```

### WebSocketSession

```kotlin
class WebSocketSession : BaseDomain() {
    val workspaceId: String
    val userId: String
    val deviceId: String
    val deviceName: String?
    val sessionId: String              // STOMP session ID
    val status: DeviceStatus           // ONLINE, AWAY, OFFLINE
    val lastHeartbeat: Instant
    val connectedAt: Instant
    val disconnectedAt: Instant?
}
```

## WebSocket Broker Configuration

Controlled via `WEBSOCKET_BROKER_TYPE` environment variable:

| Mode | Description | Use case |
|------|-------------|---------|
| `SIMPLE` | In-memory SimpleBroker | Single-instance dev/test |
| `RABBITMQ` | External RabbitMQ STOMP relay | Production multi-instance |
| `AUTO` | Auto-detect, fall back to SIMPLE | Recommended |

RabbitMQ STOMP port: `61613` (not the AMQP port `5672`).

## Heartbeat

```
Environment variable: WEBSOCKET_HEARTBEAT_INTERVAL (default: 15000ms)
Note: For SimpleBroker, set to 0 — application-level heartbeat is used instead.
      RabbitMQ supports standard STOMP heartbeats.
```

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.3__create_event_system_tables.sql` | workspace_events, websocket_sessions tables |
| `V1.0.57__fix_websocket_session_timestamp_types.sql` | TIMESTAMP → TIMESTAMPTZ on device_sessions |
| `V1.0.58__collapse_events_to_entity_type_watermark.sql` | Drop per-row uk_workspace_sequence, truncate workspace_events, add unique (workspace_id, entity_type), introduce workspace_event_sequence counter |

## Package Structure

```
com.ampairs.event
├── config/         — WebSocketConfig, WebSocketConfigProperties, Constants
├── controller/     — WorkspaceEventController, DeviceStatusController, HeartbeatController
├── domain/
│   ├── dto/        — WorkspaceEventResponse, DeviceSessionResponse, UserStatusEvent
│   ├── events/     — BaseEntityEvent, OrderEvents, InvoiceEvents, ProductEvents,
│   │                  CustomerEvents, MemberEvents
│   └── model/      — WorkspaceEvent, WebSocketSession, EventType (enum), DeviceStatus (enum)
├── listener/       — WorkspaceEventListener, WebSocketEventListener
├── repository/     — WorkspaceEventRepository, WebSocketSessionRepository
└── service/        — WorkspaceEventService, DeviceStatusService
```
