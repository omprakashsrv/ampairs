# Ampairs Backend

## Overview
The `ampairs-backend` directory contains the multi-module Kotlin and Spring Boot services that power the Ampairs platform. Each module isolates a specific business domain (workspaces, authentication, products, orders, taxation, etc.) while sharing foundational infrastructure exposed by the `core` module. The runnable application lives in `ampairs_service`, which aggregates all domain modules into a single deployable service.

All modules target Kotlin 2.2, Spring Boot 3.5, and Java 25 via Gradle. Multi-tenancy, security, persistence, and API contracts are enforced consistently across modules to keep the codebase composable and maintainable.

## Directory Layout
```
ampairs-backend/
├── ampairs_service/   # Main Spring Boot application (see ampairs_service/README.md)
├── auth/              # Authentication, user identity, and JWT management
├── business/          # Workspace-level business profile management
├── core/              # Shared configuration, base entities, utilities, multi-tenancy
├── customer/          # Customer CRM with GST-aware addressing
├── event/             # Real-time workspace event streaming and WebSocket infrastructure
├── form/              # Dynamic entity form and attribute configuration
├── invoice/           # Invoice generation, GST compliance, and order conversion
├── notification/      # Multi-channel notification orchestration
├── order/             # Order lifecycle management and pricing logic
├── product/           # Product catalog, inventory, and media management
├── tax/               # GST tax configuration, HSN catalog, and calculation engine
└── workspace/         # Workspace tenancy, invitations, RBAC, and membership
```

Build output lives in each module’s `build/` folder, and database migration scripts for shared schemas are colocated with their owning modules.

## Module Catalogue
Each module exposes its own README with deeper usage notes. The summaries below outline their primary responsibilities and integration touchpoints.

### ampairs_service (`ampairs_service/`)
- Aggregates every backend module into the runnable Spring Boot service (`AmpairsApplication`).
- Hosts cross-cutting configuration such as global security filters, CORS, and feature toggles.
- Coordinates module auto-configuration and is the target for `bootRun`/`bootJar`.
- See [ampairs_service/README.md](ampairs_service/README.md) for full details.

### auth (`auth/`)
- Provides authentication flows, OTP verification, JWT token issuance, and session management.
- Includes user profile APIs, role resolution, and integration with the workspace security context.
- Shares core abstractions from `core` (exceptions, responses) and publishes events consumed by `event`.
- See [auth/README.md](auth/README.md).

### business (`business/`)
- Manages the single business profile associated with a workspace, including legal details, addresses, hours, and regulatory metadata.
- Enforces multi-tenancy via `TenantContextHolder`, validates business hours, and centralises address formatting.
- Exposes REST endpoints under `/api/v1/business` for profile CRUD and introspection.
- See [business/README.md](business/README.md).

### core (`core/`)
- Supplies foundational infrastructure: base domain entities, shared DTOs, multi-tenancy, AWS integration, exception handling, caching, and rate limiting.
- Other modules depend on `core` for persistence primitives, tenant scoping, and utility services.
- See [core/README.md](core/README.md).

### customer (`customer/`)
- Implements CRM capabilities with rich address handling, GST fields, pagination, and search.
- Integrates with `workspace` for tenant scoping and publishes customer lifecycle events consumed by `event`.
- See [customer/README.md](customer/README.md).

### event (`event/`)
- Captures domain events from orders, invoices, products, and customers, persists them, and streams them to clients via WebSocket/STOMP.
- Provides device heartbeat APIs, event replay, and scheduled cleanup of consumed events.
- Bridges message delivery through RabbitMQ/STOMP relay when enabled.
- See [event/README.md](event/README.md).

### form (`form/`)
- Stores dynamic entity form definitions (field configs and attribute definitions) so the web and mobile clients can render configurable UI.
- Auto-seeds default schemas per entity type, supports granular CRUD, and versions updates with timestamps.
- Leverages `workspace` for tenant isolation and Jackson for JSON-driven validation rules.
- See [form/README.md](form/README.md).

### invoice (`invoice/`)
- Generates invoices from orders, applies GST rules, tracks invoice statuses, and produces PDF-ready payloads.
- Integrates with `product`, `order`, and `tax` modules for pricing and compliance data.
- See [invoice/README.md](invoice/README.md).

### notification (`notification/`)
- Delivers multi-channel notifications (SMS, email, push, WhatsApp) with provider failover, retry policies, and monitoring.
- Exposes queue management APIs and abstracts provider-specific logic behind channel interfaces.
- See [notification/README.md](notification/README.md).

### order (`order/`)
- Owns the full order lifecycle, including status transitions, discounting, tax computation hooks, and auditing.
- Utilises `product` inventory data, `customer` context, and delegates tax math to the `tax` module.
- See [order/README.md](order/README.md).

### product (`product/`)
- Maintains product catalogues, categories, units of measure, and inventory adjustments.
- Integrates with AWS S3 through `core` for media storage and publishes stock change events.
- See [product/README.md](product/README.md).

### tax (`tax/`)
- Centralises GST tax configuration, HSN/SAC catalogues, rate schedules, and the GST calculation service used by orders and invoices.
- Provides administrative APIs for maintaining tax rates, configurations, and business type classifications.
- See [tax/README.md](tax/README.md).

### workspace (`workspace/`)
- Implements tenant management: workspace creation, membership, invitations, teams, role assignments, and module enablement.
- Supplies filters and security adapters that other modules consume to determine workspace context.
- See [workspace/README.md](workspace/README.md).

## Build & Test
- **Build the entire backend**: `./gradlew build` (from `ampairs-backend/`)
- **Run all module tests**: `./gradlew test`
- **Run the full Ampairs service locally**: `./gradlew :ampairs_service:bootRun`
- **Package the runnable JAR**: `./gradlew :ampairs_service:bootJar`
- **Module-specific build/test**: `./gradlew :<module>:build` or `./gradlew :<module>:test`

Modules inherit shared Gradle conventions (Java 25 toolchain, ktlint, Spring dependency management). Testcontainers-backed tests require Docker running locally.

## Shared Patterns
- **Multi-tenancy**: `TenantContextHolder` (from `core`) plus workspace headers drive entity scoping across repositories.
- **API Envelope**: Controllers return `ApiResponse<T>` for consistent response shapes and error semantics.
- **Security**: `auth` defines JWT verification, while `workspace` and `core` provide permission evaluators and request filters.
- **Messaging**: Domain services publish events (Spring ApplicationEvents), captured by the `event` module for persistence and streaming.
- **Configuration**: Environment and profile management flow through `core` configuration classes; sensitive settings live in externalised env vars.

Refer to individual module READMEs for deeper API examples, data model diagrams, and troubleshooting tips.
