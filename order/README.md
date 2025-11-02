# Order Module

## Overview
The order module persists workspace orders, their line items, and basic lifecycle state. Clients typically sync orders in bulk using timestamp filters, while write operations accept full payloads that can create or update both the order header and its items. The service publishes order events so other modules (inventory, notifications, analytics) can react to changes and optionally generates invoices by delegating to the invoice module.

## Architecture
### Package Structure
```
com.ampairs.order/
├── controller/   # REST endpoints under /order/v1
├── domain/
│   ├── dto/      # DTOs and mappers for orders and items
│   ├── enums/    # Order and item status definitions
│   └── model/    # Entities (Order, OrderItem)
├── repository/   # OrderRepository, OrderItemRepository, OrderPagingRepository
└── service/      # OrderService orchestrating persistence and integrations
```

## Core Responsibilities
- Accept full order payloads (`OrderUpdateRequest`) and persist both header and line items.
- Maintain incremental “last updated” feeds for mobile sync clients.
- Generate sequential order numbers when none are supplied.
- Bridge to the invoice module when `create_invoice` is invoked.
- Emit `OrderCreatedEvent`, `OrderUpdatedEvent`, and `OrderStatusChangedEvent` with user/device context.

## Feature Highlights
- Tenant-aware repositories filtering by `ownerId`.
- Upsert behaviour for both orders and items using UID/refId reconciliation.
- Automatic invoice linkage (`invoiceRefId`) when an order is turned into an invoice.
- Event publishing for workspace-level WebSocket streams powered by the event module.
- Page-limited sync endpoint (`PageRequest` over `lastUpdated`) to keep payload sizes manageable.

## API Highlights
| Endpoint | Description |
|----------|-------------|
| `POST /order/v1` | Create or update an order and its items in one transaction. |
| `POST /order/v1/create_invoice` | Persist/update an order, then call the invoice module to produce a linked invoice. |
| `GET /order/v1?last_updated=` | Return orders changed since the supplied timestamp (milliseconds). |

All endpoints return `ApiResponse<T>` payloads with domain DTOs; sync responses include lists of `OrderResponse`.

## Integration Points
- **Invoice** – `OrderService.createInvoice` converts an order into an invoice via `InvoiceService`.
- **Event** – Order lifecycle changes publish events consumed by the event module.
- **Core** – Uses tenant context (`TenantContextHolder`), device context, and authentication helpers for audit data.
- **Customer/Product Modules** – Orders reference customer and product identifiers maintained by their respective modules.

## Build & Test
```bash
# From ampairs-backend/
./gradlew :order:build
./gradlew :order:test
```

Execute `./gradlew :ampairs_service:bootRun` to exercise the order APIs with the assembled service.
