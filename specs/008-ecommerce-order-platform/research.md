# Research: Ecommerce Order Platform

**Phase**: 0 — Unknowns resolved before design  
**Branch**: `008-ecommerce-order-platform`

---

## 1. New Module vs Existing Module

**Decision**: New `ecom` module at `ecom/src/main/kotlin/com/ampairs/ecom/`.

**Rationale**: The spec is a distinct bounded context (storefront, catalog sync, cart, ecom orders, customer identity) that does not belong in any existing module. Modules in this repo are independently testable Spring Boot modules assembled by `ampairs_service`. Adding an `ecom` module follows the same pattern as `order`, `product`, `customer`, etc.

**Alternatives considered**:
- Extend `order` module: rejected — ecom orders, carts, and storefronts are a separate domain from B2B/in-store orders. Mixing them would couple two distinct order lifecycles.
- New standalone service: rejected — the spec explicitly states "module within the existing monorepo, running as part of `ampairs_service`", designed for future extraction.

---

## 2. Kafka for Catalog Sync and Order Events

**Decision**: Dedicated Kafka producer/consumer in the `ecom` module using three topics:
- `ecom-catalog-events` — management (product module) → ecom (listed product sync)
- `ecom-order-placed` — ecom → management (order module, for inventory deduction)
- `ecom-order-status` — management (order module) → ecom (status propagation)

**Rationale**: The spec mandates Kafka from day 1 with the same topic names and schemas that would be used post-extraction. The existing `event` module's Kafka setup is dedicated to WebSocket fan-out with a unique consumer group per instance; ecom events need a stable durable consumer group per deployed instance. A separate `EcomKafkaConfig` in the `ecom` module keeps the two Kafka contexts isolated.

**Consumer group strategy**:
- `ecom-catalog-consumer` — single stable group (all instances share; one instance processes each message)
- `ecom-order-status-consumer` — same pattern

**Alternatives considered**:
- Spring Application Events (in-process): rejected — defeats the "Kafka from day 1" requirement and cannot survive future extraction.
- Reuse WebSocket Kafka config: rejected — WebSocket config uses a unique group per instance for fan-out; catalog and order events need exactly-once processing per topic partition.

---

## 3. Product Search Implementation

**Decision**: PostgreSQL native full-text search using a `tsvector` generated column with a GIN index on `ecom_listed_product`.

**Rationale**: The project uses PostgreSQL as the primary database. PostgreSQL's built-in `tsvector` + GIN index provides sub-1s ranked full-text search with no additional infrastructure. A stored generated column keeps the index always current without application-side maintenance. When Elasticsearch is introduced later, the Kafka catalog event log enables a full replay to build the ES index with zero application changes.

**Alternatives considered**:
- Elasticsearch from day 1: rejected — adds a new infrastructure dependency, increases operational complexity, and is unnecessary at v1 scale.
- `ILIKE '%query%'` queries: rejected — no index support, full table scans, no relevance ranking; fails SC-002 under load.

**Index definition** (PostgreSQL):
```sql
ALTER TABLE ecom_listed_product
  ADD COLUMN search_vector tsvector
  GENERATED ALWAYS AS (
    to_tsvector('english',
      coalesce(name, '') || ' ' ||
      coalesce(brand, '') || ' ' ||
      coalesce(category, '') || ' ' ||
      coalesce(subcategory, '')
    )
  ) STORED;

CREATE INDEX idx_ecom_product_search ON ecom_listed_product USING GIN (search_vector);
```

**Query pattern** (in `EcomListedProductRepository`):
```sql
SELECT * FROM ecom_listed_product
WHERE storefront_id = :storefrontId
  AND is_visible = true
  AND search_vector @@ plainto_tsquery('english', :query)
ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC
```

---

## 4. Cart Persistence Strategy

**Decision**: JPA entity (`EcomCart` + `EcomCartItem`) stored in PostgreSQL. Guest carts are identified by a server-issued opaque `session_token` (UUID, returned at cart creation). Cart TTL enforced at row level via `expires_at` column + scheduled cleanup job.

**Rationale**: Redis is not confirmed to be in the stack (not seen in `docker-compose.yml`). Using JPA stays consistent with every other module. Guest cart tokens are opaque UUIDs — they are not JWTs. A scheduled cleanup task purges expired carts to keep the table lean.

**Cart lifecycle**:
- Guest creates cart → server returns `session_token` stored in browser (localStorage/cookie)
- Cart persists for 24 hours (`expires_at = now() + 24h`)
- Customer logs in → merge guest cart items into the customer's persistent cart
- Order confirmed → cart status set to `CONVERTED`

**Alternatives considered**:
- Redis session: rejected — Redis not confirmed in stack; adds infrastructure dependency.
- Browser-only cart (no server state): rejected — violates FR-014 (24hr persistence requirement tied to session, not browser).

---

## 5. End Customer Identity

**Decision**: Add `user_type` enum column (`MERCHANT_USER`, `END_CUSTOMER`) to the existing `app_user` table (managed by the `user` module). End customers are registered via a new `/api/v1/ecom/auth/register` endpoint that delegates to the existing auth infrastructure (JWT signing, refresh tokens, device sessions) and sets `userType = END_CUSTOMER`. No workspace roles are assigned.

**Rationale**: The spec requires reuse of the existing `auth` module — end customers are a distinct user type within the same auth infrastructure. The User entity is in the `user` module; adding a `userType` column is the minimal, non-breaking change. Existing merchant users default to `MERCHANT_USER`. No new auth service is introduced.

**JWT claims**: End customers receive the same JWT format; `workspace_id` claim is absent. `user_type` claim is added to distinguish end customers in security filters.

**Alternatives considered**:
- Separate `ecom_customer` table: rejected — creates a parallel auth system, duplicates JWT/refresh token infrastructure, violates spec assumption.
- Use roles/authorities for type discrimination: rejected — roles in this system are workspace-scoped RBAC roles, not user type markers.

---

## 6. Cross-Tenant Cart and Storefront Queries

**Decision**: `EcomCart`, `EcomCartItem`, `EcomOrder`, `EcomOrderLineItem`, and `CustomerAddress` extend `BaseDomain` (not `OwnableBaseDomain`) because:
- Guest carts exist without a workspace context (no `X-Workspace-ID` header at cart creation time)
- Customer addresses are platform-wide (not workspace-scoped)
- These entities are identified by explicit foreign keys (`storefront_id`, `customer_id`) rather than `@TenantId`

`Storefront` and `EcomListedProduct` extend `OwnableBaseDomain` (workspace-scoped via `@TenantId`) because they are directly scoped to a merchant's workspace and all queries on them happen within a workspace context (either merchant management requests with `X-Workspace-ID` or storefront public requests where the slug resolves to a workspace).

**Public storefront requests**: The slug is resolved to a `workspaceId` before any tenant-scoped repository access. The controller sets `TenantContextHolder.setCurrentTenant(workspaceId)` in try/finally, then uses tenant-scoped repos for `Storefront` and `EcomListedProduct`.

---

## 7. Flyway Migration Versioning

**Decision**: New `ecom` module migrations start at `V1.0.27__...` (next after `V1.0.26` in workspace module). Each new migration increments the patch version globally.

**Rationale**: The project uses a global version namespace across modules. The highest applied migration seen is `V1.0.26`. The ecom module will introduce: storefronts, listed products, carts, cart items, ecom orders, ecom order line items, customer addresses (6 migrations + user_type column = 7 migration files spread across `ecom` and `user` modules).

---

## 8. Management-Side Changes

**Decision**: Minimal, surgical additions to existing modules to support ecom integration:

**`product` module**:
- Add `is_ecom_listed: Boolean` column to `product` table
- New service method `listProductOnEcom(productId)` / `unlistProductFromEcom(productId)` — publishes `EcomCatalogEvent` to Kafka
- Existing price/stock/detail update methods also publish `EcomCatalogEvent` when `isEcomListed = true`

**`order` module**:
- Add `PENDING_MERCHANT_REVIEW` to `OrderStatus` enum
- Add `ecom_order_ref` nullable column to `customer_order` table
- Add `order_type` value `ECOM` (already exists as String field, no schema change)
- New Kafka consumer `EcomOrderPlacedConsumer` processes `EcomOrderPlaced` events

**`user` module**:
- Add `user_type` column to `app_user` table with default `MERCHANT_USER`

**`event` module**:
- Add `EcomCatalogEvent`, `EcomOrderPlacedEvent`, `EcomOrderStatusEvent` domain event classes (Kafka payload DTOs)

---

## 9. Storefront URL Routing

**Decision**: Storefront public API is served under `/api/v1/store/{slug}/...`. The slug identifies the storefront. The web frontend (`ampairs-web` or a dedicated storefront SPA) handles the `store.ampairs.com/{slug}` routing. The backend does not need a special virtual host configuration — the slug is a path segment.

**Rationale**: This matches the single-deployment-region assumption. Virtual host routing (`store.ampairs.com`) is a CDN/reverse-proxy concern, not a Spring application concern.

---

## 10. Order Reference Number Generation

**Decision**: Reuse the existing `BaseDomain.uid` pattern with a new prefix `ECO-`. The `EcomOrder.obtainSeqIdPrefix()` returns `"ECO"` to generate references like `ECO-A1B2C3D4`. This is consistent with how `ORDER`, `INV`, etc. are generated across the codebase.
