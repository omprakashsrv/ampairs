# Event Module

## Overview
The Event module delivers real-time collaboration for Ampairs workspaces. It persists domain events emitted by orders, products, customers, and invoices; broadcasts updates over WebSocket/STOMP; and exposes REST endpoints for replaying or querying the event log. The module is multi-tenant aware, integrates with RabbitMQ for distributed messaging, and provides device heartbeat tracking to keep client connections in sync.

## Architecture
### Package Structure
```
com.ampairs.event/
├── config/                 # STOMP/WebSocket configuration and constants
├── controller/             # REST APIs for events, device status, and heartbeats
├── domain/                 # Event aggregate, DTOs, enums, and device/session models
│   └── events/             # Spring ApplicationEvent payloads emitted by other modules
├── listener/               # ApplicationEvent listeners that persist and broadcast events
├── repository/             # Spring Data repositories for event store and sessions
└── service/                # Workspace event querying, device status, and cleanup
```

## Key Components
- **`WebSocketConfig`** – Configures the STOMP broker, application destination prefixes, and optionally relays traffic through RabbitMQ for clustered deployments.
- **`WorkspaceEventListener`** – Captures application events (customer/product/order/invoice) and persists them as `WorkspaceEvent` records before publishing to WebSocket subscribers.
- **`WorkspaceEventService`** – Provides paginated event queries, sequence-based incremental polling, and scheduled cleanup of consumed events.
- **`DeviceStatusService`** – Tracks device presence, session expiry, and exposes REST endpoints for the UI to report status changes.
- **`WebSocketEventListener`** – Keeps the event store in sync with WebSocket connect/disconnect lifecycle events.
- **Repositories (`WorkspaceEventRepository`, `WebSocketSessionRepository`)** – Support tenant-scoped queries, consumption marking, and sequence number generation.

## Features
- **Real-time streaming** of workspace activity over `/topic/workspace.{workspaceId}` STOMP destinations with optional message replay.
- **Event persistence** with sequence numbers, device/user attribution, and JSON payload snapshots for auditability.
- **REST access** to events (`/api/v1/events`) for clients that cannot maintain WebSocket connections.
- **Device heartbeat management** via `/api/v1/events/devices` endpoints to track online/offline status.
- **Scheduled maintenance** that trims consumed events older than 30 days to keep the eventStore lean.
- **Multi-tenancy enforcement** through `TenantContextHolder` and `DeviceContextHolder`.

## API Highlights
- `GET /api/v1/events` – Paginated view of workspace events.
- `GET /api/v1/events/stream` – Sequence-based incremental polling with optional device exclusion.
- `POST /api/v1/events/{id}/consume` – Mark a single event as consumed.
- `POST /api/v1/events/consume` – Bulk consumption API for checkpoints.
- `GET /api/v1/events/devices` – List active device sessions and statuses.
- `POST /api/v1/events/devices/heartbeat` – Report device heartbeat to keep session alive.

Responses leverage shared DTOs (`WorkspaceEventResponse`, `DeviceSessionResponse`) and the `ApiResponse<T>` wrapper from `core`.

## Integration Points
- Depends on `core` for base entities, multi-tenancy helpers, and JSON serialization.
- Subscribes to Spring `ApplicationEvent`s emitted by the `order`, `product`, `customer`, and `invoice` modules.
- Shares security context with `auth`/`workspace` to validate tenant access to event streams.
- Optionally relays broker messages through RabbitMQ as configured via application properties.

## Build & Test
```bash
# From ampairs-backend/
./gradlew :event:build
./gradlew :event:test
```

Launch the aggregate service with `./gradlew :ampairs_service:bootRun` to exercise the WebSocket endpoints alongside the other modules.
