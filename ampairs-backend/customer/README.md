# Customer Module

## Overview
The Customer module delivers CRM capabilities for each workspace. It manages customer master data, segmentation (groups/types), GST validation, address details, credit tracking, and media assets such as profile images. All operations are tenant-aware via `TenantContextHolder`, emit workspace events for downstream consumers, and expose pagination-friendly APIs for sync workflows.

## Architecture
### Package Structure
```
com.ampairs.customer/
├── config/                   # Module constants
├── controller/               # REST endpoints (customers, groups, types, states, images)
├── domain/                   # Entities, DTOs, and domain services
│   ├── dto/                  # API payloads/mappers (customers, groups, types, images, states)
│   ├── model/                # JPA entities for customers, segmentation, media, reference data
│   ├── repository/           # Domain repositories for customer media
│   └── service/              # Domain-focused service layer
├── exception/                # API error handling
├── repository/               # Customer + lookup repositories (paging, search)
└── service/                  # Cross-cutting services (thumbnail maintenance)
```

## Key Components
- **`CustomerController`** – Sync-friendly CRUD (`upsert`, bulk update), pageable listing with `last_sync` filtering, GST validation, outstanding balance updates, and retail-focused helper endpoints.
- **`CustomerGroupController` / `CustomerTypeController`** – Manage segmentation metadata with pagination, priority ordering, search, credit defaults, and statistics.
- **`MasterStateController`** – Distribute canonical state metadata for address forms.
- **`CustomerImageController`** – Upload, list, fetch, and download customer images with cache headers, optional primary image selection, and workspace-specific storage paths.
- **Services (`CustomerService`, `CustomerGroupService`, `CustomerTypeService`, `CustomerImageService`, `MasterStateService`)** – Enforce tenant scoping, emit domain events (`CustomerCreated/Updated/DeletedEvent`), and orchestrate persistence.
- **`ThumbnailMaintenanceService`** – Scheduled maintenance for image thumbnails (cleanup, regeneration, health checks) backed by the shared `ThumbnailCacheService`.

## Features
- **Customer lifecycle**
  - Upsert operations keyed by UID/refId for mobile sync clients.
  - Rich profile data: contact info, GST/PAN, credit terms, custom attributes.
  - Outstanding balance adjustments with payment vs. charge distinction.
  - Search helpers for type, city/state, credit status, and general text queries.
- **Segmentation**
  - Workspace-specific customer groups and types with display ordering and metadata.
  - Dedicated APIs for discount-enabled groups and credit-enabled types.
  - Aggregated statistics endpoints for dashboard insights.
- **Location & compliance**
  - Master state catalog shared across tenants.
  - GST format validation and uniqueness enforcement.
  - Events emitted on create/update/delete for real-time consumers.
- **Media management**
  - Multipart upload with optional primary flag and display order.
  - Signed downloads with cache-control headers and ETag support.
  - Scheduled thumbnail cache upkeep (daily cleanup, orphan removal, regeneration).

## API Highlights
| Endpoint | Description |
|----------|-------------|
| `POST /customer/v1` | Upsert a single customer record via DTO mapping. |
| `POST /customer/v1/customers` | Bulk update customers (mobile sync). |
| `GET /customer/v1` | Paginated list with `last_sync`, sorting, and standard filters. |
| `GET /customer/v1/{customerId}` | Fetch a single customer by UID. |
| `PUT /customer/v1/{customerId}` | Partial profile update with change tracking. |
| `GET /customer/v1/gst/{gst}` | Lookup by GST number. |
| `POST /customer/v1/validate-gst` | Validate GST format. |
| `PUT /customer/v1/{customerId}/outstanding` | Adjust outstanding balance (charge/payment). |
| `GET /customer/v1/states` | Return master state catalog. |
| `.../groups/**` & `.../types/**` | CRUD, search, priority ordering, and statistics for segmentation. |
| `.../images/**` | Upload/download/list customer images with metadata and stats. |

Responses wrap data in `ApiResponse<T>` and frequently use `PageResponse<T>` for pageable collections.

## Data & Events
- Persistent entities extend `OwnableBaseDomain` to inherit tenant and audit fields.
- Repository layer offers pageable, search, and aggregation queries (e.g., `CustomerPagingRepository`, custom finders).
- `CustomerService` publishes Spring events that the `event` module captures for WebSocket streaming and audit logs.
- Thumbnail maintenance jobs rely on `StorageProperties` toggles (`ampairs.storage.image.thumbnails.*`).

## Integration Points
- **Core** – Shared DTO wrappers, storage configuration, multi-tenancy helpers, thumbnail cache service.
- **Workspace** – Supplies the active workspace header consumed by controllers/services.
- **Event** – Consumes emitted customer lifecycle events for real-time updates.
- **Product/Order/Invoice** – Downstream consumers rely on customer data for transactions and invoicing.
- **AWS S3 (via core services)** – Default storage backend for customer images and thumbnails.

## Build & Test
```bash
# From ampairs-backend/
./gradlew :customer:build
./gradlew :customer:test
```

Run `./gradlew :ampairs_service:bootRun` to exercise the customer APIs end-to-end with the assembled backend.
