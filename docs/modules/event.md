# event module

Captures domain events from all modules, persists them to the database, and delivers them to connected clients via WebSocket (STOMP). Provides device heartbeat tracking for presence awareness.

## Responsibilities

- Capture and persist domain events (orders, invoices, products, customers, members)
- Stream events to workspace clients over WebSocket
- Event acknowledgement and replay
- Device presence tracking (ONLINE / AWAY / OFFLINE)
- Heartbeat endpoint for connection keep-alive

## REST Endpoints

### Events (`/api/v1/events`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/events` | Events since a sequence number (for sync) |
| GET | `/api/v1/events/all` | All events with pagination |
| GET | `/api/v1/events/unconsumed` | Unconsumed events |
| GET | `/api/v1/events/unconsumed/count` | Count of unconsumed events |
| GET | `/api/v1/events/{eventId}` | Get event by UID |
| GET | `/api/v1/events/entity/{entityType}/{entityId}` | Events for a specific entity |
| GET | `/api/v1/events/type/{eventType}` | Events by type |
| POST | `/api/v1/events/{eventId}/acknowledge` | Mark single event as consumed |
| POST | `/api/v1/events/acknowledge` | Mark multiple events as consumed |

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

```kotlin
class WorkspaceEvent : BaseDomain() {
    val eventType: EventType           // ORDER_CREATED, INVOICE_PAID, etc.
    val entityType: String             // "order", "invoice", "product", etc.
    val entityId: String               // UID of the changed entity
    val payload: String                // JSON snapshot of the change
    val deviceId: String               // device that triggered the event
    val userId: String
    val workspaceId: String
    val sequenceNumber: Long           // monotonically increasing per workspace
    val consumed: Boolean              // acknowledged by client
}
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
