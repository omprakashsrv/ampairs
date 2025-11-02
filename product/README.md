# Product Module

## Overview
The product module manages catalog data for each workspace: product records, groups/categories/brands, unit definitions, and associated media. It exposes sync-friendly endpoints used by mobile and web clients, supports bulk upserts keyed by UID/refId, and publishes product lifecycle events so downstream services can react to catalog changes.

## Architecture
### Package Structure
```
com.ampairs.product/
├── controller/            # REST endpoints under /product/v1
├── domain/
│   ├── dto/               # DTOs and mappers (product, group, unit)
│   ├── enums/             # Enumeration types
│   └── model/             # Entities for products, taxonomy, units
├── repository/            # Spring Data repositories + paging projections
└── service/               # ProductService orchestrating catalog logic
```

## Core Responsibilities
- **Catalog sync** – `updateProducts`, `updateGroups`, `updateCategories`, etc. upsert lists of records while preserving IDs/UIDs for offline clients.
- **Taxonomy management** – expose groups, categories, brands, and subcategories; maintain display order metadata.
- **Unit management** – provide reference data for units of measure and accept bulk updates.
- **Media upload** – proxy product image uploads through the shared `file` module’s `FileService`.
- **Retail helpers** – additional endpoints for creating/updating a single product, search with pagination, and SKU lookups.
- **Event emission** – fire `ProductCreatedEvent`/`ProductUpdatedEvent`/`ProductDeletedEvent` for real-time consumers.

## Feature Highlights
- Tenant-aware repositories powered by `TenantContextHolder`.
- UID/refId reconciliation so mobile sync clients can upsert without colliding with server-generated IDs.
- Optional SKU auto-generation and uniqueness validation on retail endpoints.
- Search endpoint with filters (name, category, brand, price range) and pageable responses.
- Category bundle endpoint returning groups, categories, brands, and subcategories in a single payload.
- Image uploads stored beneath `products/{workspaceId}` for isolation.

## API Highlights
| Endpoint | Description |
|----------|-------------|
| `GET /product/v1?last_updated=` | Return products updated after the provided timestamp (sync feed). |
| `POST /product/v1/products` | Bulk upsert product records using DTOs mapped from clients. |
| `POST /product/v1/groups` / `/brands` / `/categories` / `/sub_categories` | Bulk upsert taxonomy entities. |
| `GET /product/v1/all_groups_category` | Fetch groups, categories, brands, and subcategories in one response. |
| `POST /product/v1/units` / `GET /product/v1/units` | Manage allowed units of measure. |
| `POST /product/v1/upload_image` | Upload product media and receive a `FileResponse` with storage metadata. |
| `POST /product/v1` / `PUT /product/v1/{productId}` | Retail-oriented create/update helpers with validation and event publishing. |
| `GET /product/v1/list` | Search products with pagination, sorting, and optional filters. |
| `GET /product/v1/sku/{sku}` | Retrieve a product by SKU. |

All endpoints respond with `ApiResponse<T>`; paginated data includes a `pagination` block or uses `PageResponse<T>` when applicable.

## Integration Points
- **Core** – Provides API envelopes, tenant context helpers, and authentication utilities.
- **File** – Supplies storage (`FileService`), validation, and thumbnail helpers for media assets.
- **Event** – Receives product lifecycle events for WebSocket streaming and audit feeds.
- **Workspace** – Supplies tenant headers; products inherit workspace ownership for security checks.
- **Tax/Order/Invoice** – Consume product metadata (SKU, units, prices) during pricing and document generation.

## Build & Test
```bash
# From ampairs-backend/
./gradlew :product:build
./gradlew :product:test
```

Run `./gradlew :ampairs_service:bootRun` to validate catalogue APIs alongside the rest of the backend.
