# Invoice Module

## Overview
The invoice module stores workspace invoices and their line items, provides timestamp-based sync feeds, and raises domain events whenever an invoice is created, updated, or its status changes. It receives payloads from both UI clients and the order module, performs UID/refId reconciliation, and keeps invoice numbers sequential per tenant.

## Architecture
### Package Structure
```
com.ampairs.invoice/
├── controller/   # REST endpoints under /invoice/v1
├── domain/
│   ├── dto/      # DTOs and mappers for invoices and invoice items
│   ├── enums/    # Invoice and item status enumerations
│   └── model/    # Entities (Invoice, InvoiceItem)
├── repository/   # InvoiceRepository, InvoiceItemRepository, InvoicePagingRepository
└── service/      # InvoiceService handling persistence and event publishing
```

## Core Responsibilities
- Upsert invoices (`InvoiceUpdateRequest`) and their items in one transaction.
- Generate incrementing invoice numbers when not provided by the client.
- Maintain incremental feeds via `last_updated` so mobile clients can sync in batches.
- Emit `InvoiceCreatedEvent`, `InvoiceUpdatedEvent`, and `InvoiceStatusChangedEvent` including workspace, user, and device metadata.

## Feature Highlights
- Tenant-aware storage enforced through `ownerId` and `TenantContextHolder`.
- UID and refId mapping so existing invoices can be updated without duplicating records.
- Automatic association of line items back to the parent invoice before persisting.
- Event-driven integration used by the event module for WebSocket streaming and audit trails.
- `InvoiceService.getInvoice` helper when other modules need the persistence model.

## API Highlights
| Endpoint | Description |
|----------|-------------|
| `POST /invoice/v1` | Create or update an invoice with its line items; returns `InvoiceResponse`. |
| `GET /invoice/v1?last_updated=` | Fetch invoices changed since a timestamp (milliseconds). |

Both endpoints respond using `ApiResponse<T>` to maintain consistent API envelopes.

## Integration Points
- **Order Module** – Calls `InvoiceService.updateInvoice` when `create_invoice` is triggered from `OrderService`.
- **Event Module** – Receives invoice lifecycle events for real-time delivery to clients.
- **Core** – Provides tenant context, authentication helpers, and base domain classes.
- **Tax Module** – Invoice DTOs carry tax amounts calculated by the tax module before persistence.

## Build & Test
```bash
# From ampairs-backend/
./gradlew :invoice:build
./gradlew :invoice:test
```

Run `./gradlew :ampairs_service:bootRun` to interact with the invoice APIs inside the aggregated backend.
