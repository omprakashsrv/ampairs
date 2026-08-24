# Developer Documentation

All developer documentation for Ampairs, organized by concern.

---

## Start here

| Document | Description |
|----------|-------------|
| [getting-started.md](getting-started.md) | Clone, run locally, profiles, build commands |
| [architecture.md](architecture.md) | System design, request lifecycle, security layers, data model |

## AI Business Operations Manager

The AI agent that reviews a workspace's data, auto-fixes high-confidence issues, and escalates the rest.
**Orchestration runs app-side** (ADR 0005); the backend is a deferred tier. Start at the roadmap.

| Document | Description |
|----------|-------------|
| [ai-ops-manager/README.md](ai-ops-manager/README.md) | **Program roadmap** — the spine: artifact map, epics, backlog, method |
| [ai-ops-manager/framework.md](ai-ops-manager/framework.md) | Engine design — SPI, decision engine, data model, walkthroughs |
| [adr/0005-…](adr/0005-ai-ops-manager-app-side-orchestration.md) | Decision: app-side orchestration; backend deferred |
| [adr/0006-…](adr/0006-aiops-engine-shared-kmp-module.md) | Decision: host-agnostic shared KMP engine |
| [spikes/koog-agent-backend-spike.md](spikes/koog-agent-backend-spike.md) | Spike: Koog backend tool-calling (deferred tier) |
| specs [030](../specs/030-koog-agent-assistant/spec.md) · [031](../specs/031-ai-ops-manager-foundation/spec.md) | Backend Q&A + `:aiops` foundation (Epic 2, deferred) |

## Modules

Complete documentation for each backend module — entities, endpoints, migrations, package structure.

| Module | Purpose |
|--------|---------|
| [modules/core.md](modules/core.md) | Shared infrastructure, base entities, API keys, exception handling |
| [modules/auth.md](modules/auth.md) | Authentication, JWT, OTP, device sessions, user profiles |
| [modules/workspace.md](modules/workspace.md) | Tenants, members, teams, invitations, RBAC, `SessionUserFilter` |
| [modules/product.md](modules/product.md) | Product catalog, variants, multi-warehouse inventory |
| [modules/subscription.md](modules/subscription.md) | Plans, billing, payments (Razorpay/Stripe/Play/AppStore), invoices |
| [modules/customer.md](modules/customer.md) | CRM, customer groups, types, images |
| [modules/tax.md](modules/tax.md) | GST config, HSN/SAC codes, tax rules |
| [modules/business.md](modules/business.md) | Business profile, logo, gallery |
| [modules/order.md](modules/order.md) | Order lifecycle, line items, discounts |
| [modules/invoice.md](modules/invoice.md) | GST-compliant invoices, payment status |
| [modules/event.md](modules/event.md) | Domain event streaming, WebSocket/STOMP, device presence |
| [modules/notification.md](modules/notification.md) | SMS (MSG91/SNS), push notifications, retry queue |
| [modules/unit.md](modules/unit.md) | Units of measure and conversion rules |
| [modules/setting.md](modules/setting.md) | Central workspace settings registry (cross-module toggles, offline-sync) |
| [modules/form.md](modules/form.md) | Dynamic field config and custom attributes per entity type |
| [modules/file.md](modules/file.md) | Object storage — S3, MinIO, local filesystem |
| [modules/user.md](modules/user.md) | User identity, profile, account deletion lifecycle |
| [modules/supplier.md](modules/supplier.md) | Supplier management |
| [modules/purchase.md](modules/purchase.md) | Purchase recording (bills + purchase items) |
| [modules/payment.md](modules/payment.md) | Payment recording and allocation |
| [modules/pricing.md](modules/pricing.md) | Price lists, tiers, offers/coupons, geo zones |
| [modules/sequence.md](modules/sequence.md) | Document number sequences — definitions + allocations |
| [modules/ecom.md](modules/ecom.md) | Storefronts — public catalog, cart, checkout, buyer accounts |
| [modules/printing.md](modules/printing.md) | Print-template storage + offline-sync |
| [modules/agent.md](modules/agent.md) | AI model manifest/download proxy + chat telemetry |

## API Reference

| Document | Description |
|----------|-------------|
| [api/authentication.md](api/authentication.md) | API key authentication — setup, usage, rotation |
| [api/api-key-testing.md](api/api-key-testing.md) | Testing API key authentication flows |
| [api/app-updates.md](api/app-updates.md) | Desktop app update API — endpoints, versioning, checksums |

## Deployment

| Document | Description |
|----------|-------------|
| [deployment/deployment.md](deployment/deployment.md) | Production deployment — server setup, SSH, service config |
| [deployment/production-validation.md](deployment/production-validation.md) | Post-deployment validation checklist |
| [deployment/cicd-app-updates.md](deployment/cicd-app-updates.md) | CI/CD pipeline for desktop app release automation |

## Guides

| Document | Description |
|----------|-------------|
| [guides/database-migrations.md](guides/database-migrations.md) | Flyway workflow, naming conventions, rollback |
| [guides/configuration.md](guides/configuration.md) | Flyway DDL, Hibernate, environment variable reference |
| [guides/spring-security.md](guides/spring-security.md) | Spring Security configuration reference |
| [guides/file-storage.md](guides/file-storage.md) | File storage, S3 integration, MIME type handling |

## Features

| Document | Description |
|----------|-------------|
| [features/account-deletion.md](features/account-deletion.md) | Account deletion — architecture, API, Google Play compliance |

---

## Root-level docs

| Document | Description |
|----------|-------------|
| [README.md](../README.md) | Project overview, quick start, structure |
| [CLAUDE.md](../CLAUDE.md) | Coding standards and architectural patterns |
| [AGENTS.md](../AGENTS.md) | Guidelines for AI agents working in this repo |
