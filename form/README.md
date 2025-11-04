# Form Module

## Overview
The Form module powers Ampairs’ dynamic form engine. It stores per-entity field configurations and custom attribute definitions so that web and mobile clients can render configurable UI without code changes. The module supports automatic seeding of defaults, granular CRUD operations, and workspace isolation for tenant-specific customisations.

## Architecture
### Package Structure
```
com.ampairs.form/
├── controller/              # REST endpoints under /api/v1/form/config
├── domain/                  # Domain models, DTOs, and services
│   ├── dto/                 # Request/response payloads and mapping helpers
│   ├── model/               # JPA entities for FieldConfig and AttributeDefinition
│   └── repository/          # Spring Data repositories
└── domain/service/          # `ConfigService` orchestrating schema operations
```

## Key Components
- **`ConfigController`** – Exposes APIs to fetch, create, update, or delete field configurations and attribute definitions, plus endpoints to fetch all schemas for bulk synchronisation.
- **`ConfigService`** – Centralises business rules: auto-seeds defaults on first access, manages transactional saves, and handles combined schema updates across fields and attributes.
- **`FieldConfig` / `AttributeDefinition` entities** – Store UI metadata such as display order, visibility flags, validation rules, default values, enum options, and helper text.
- **Repository interfaces** – Provide workspace-scoped queries (`findByEntityType…`) and allow bulk retrieval for all entity types.
- **DTO mappers (`FieldConfigDTOs`, `AttributeDefinitionDTOs`)** – Convert between persistence models and API responses with consistent timestamp handling.

## Features
- **Dynamic schema retrieval**: clients request form definitions per entity type (e.g., customers, products) and receive field metadata with validation hints.
- **Auto-seeding**: first-time access to an entity type triggers default configuration seeding so tenants start with sensible defaults.
- **Bulk operations**: `/bulk` endpoints allow saving a complete schema snapshot, simplifying admin UI workflows.
- **Fine-grained updates**: individual endpoints let admins toggle visibility, mark fields mandatory, or adjust helper text without touching other fields.
- **Version awareness**: responses include `lastUpdated` timestamps so clients can cache schemas and only refresh when necessary.
- **Validation support**: JSON-encoded validation parameters and enum lists allow rich client-side validation without redeployments.

## API Highlights
- `GET /api/v1/form/config/{entityType}` – Fetch a specific entity’s configuration schema (auto-seeds defaults if none exist).
- `GET /api/v1/form/config` – Return all schemas for the tenant.
- `POST /api/v1/form/config/fields` – Create or update a single field configuration.
- `POST /api/v1/form/config/attributes` – Create or update a custom attribute definition.
- `DELETE /api/v1/form/config/fields/{entityType}/{fieldName}` – Remove a field configuration.
- `DELETE /api/v1/form/config/attributes/{entityType}/{attributeKey}` – Remove an attribute definition.
- `POST /api/v1/form/config/bulk` – Persist an entire schema (fields + attributes) in one call.

All endpoints use the shared `ApiResponse<T>` wrapper and expect the active workspace to be provided via tenancy headers.

## Integration Points
- Depends on `core` for base domain traits, JSON utilities, and API responses.
- Resolves workspace context through `workspace` filters, ensuring tenant-level separation of form definitions.
- Consumed by front-end applications (Angular and Compose) to render dynamic forms for customers, products, orders, and other entities.
- Complementary to `business`, `customer`, `product`, and `order` modules which reference these configuration schemas to validate incoming data.

## Build & Test
```bash
# From ampairs-backend/
./gradlew :form:build
./gradlew :form:test
```

Run `./gradlew :ampairs_service:bootRun` for an end-to-end environment that serves the form configuration APIs alongside the rest of the backend.
