# event module

Domain event capture + WebSocket/STOMP delivery. Device presence tracking.

## Flow
Domain service → `ApplicationEvent` → `WorkspaceEventListener` → persisted to `workspace_events` → delivered via STOMP to `/topic/workspace/{workspaceId}`

## Event types published by other modules
`OrderEvents`, `InvoiceEvents`, `ProductEvents`, `CustomerEvents`, `MemberEvents`

## Key entity
- `WorkspaceEvent` — eventType, entityType, entityId, payload (JSON snapshot), sequenceNumber, consumed

## WebSocket broker
`WEBSOCKET_BROKER_TYPE`: `SIMPLE` (dev), `RABBITMQ` (prod), `AUTO` (recommended)
RabbitMQ STOMP port: `61613` (not AMQP `5672`)

## Base path
`/api/v1/events/**`, `/api/v1/devices/**`, WS `/app/heartbeat`

## Migrations
`V1.0.3`

## Full docs
`docs/modules/event.md`
