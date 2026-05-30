# Feature Specification: Ecommerce Order Platform

**Feature Branch**: `008-ecommerce-order-platform`
**Created**: 2026-05-30
**Status**: Draft
**Input**: User description: "Ecommerce Order Platform — separate microservice with Kafka event sync from Ampairs management monolith."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - End Customer Browses and Places an Order (Priority: P1)

A shopper visits `store.ampairs.com/{workspace-slug}` for a merchant they know. They browse the product catalog, search by name or filter by category/brand, add items to their cart, and complete checkout. They may already have an account from shopping at a different merchant on the platform.

**Why this priority**: This is the core value of the entire platform. Without a working browse-to-checkout flow, nothing else matters.

**Independent Test**: Can be fully tested by opening a storefront, searching for a product, adding it to cart, and completing a purchase — delivering a confirmed order with a reference number.

**Acceptance Scenarios**:

1. **Given** a customer visits `store.ampairs.com/merchant-a`, **When** they search for "rice 5kg", **Then** matching products from that merchant's catalog appear within 1 second, ordered by relevance.
2. **Given** a customer adds 3 items to their cart, **When** they proceed to checkout and confirm, **Then** an order is created, the customer receives an order confirmation with a reference number, and the merchant's inventory is queued for deduction.
3. **Given** a customer browses and adds items to cart without logging in, **When** they reach checkout and log in with an existing account, **Then** the guest cart is merged with their saved cart and their saved addresses are pre-filled for delivery.
4. **Given** a product has only 2 units in stock, **When** a customer tries to add 5 units to their cart, **Then** the system caps the quantity at 2 and shows an availability message.

---

### User Story 2 - Merchant Creates and Configures Their Storefront (Priority: P1)

A merchant decides to open an online storefront. From within their Ampairs management dashboard, they create a storefront, configure its name, description, and logo, and choose their workspace slug. The storefront starts in Draft state, allowing the merchant to preview it and list products before making it publicly accessible. When ready, they publish it.

**Why this priority**: Without a created and published storefront, no public product listing or order placement is possible. This is the prerequisite to all other ecom flows.

**Independent Test**: Can be fully tested by creating a storefront in management, previewing it in Draft state, then publishing it and verifying it becomes accessible at `store.ampairs.com/{slug}`.

**Acceptance Scenarios**:

1. **Given** a merchant has a workspace in Ampairs, **When** they create a storefront with a name, slug, and description, **Then** the storefront is created in Draft state and accessible for preview only to the merchant.
2. **Given** a merchant's storefront is in Draft state, **When** they publish it, **Then** the storefront becomes publicly accessible at `store.ampairs.com/{slug}`.
3. **Given** a merchant tries to create a storefront with a slug already in use by another workspace, **When** they submit, **Then** they receive an error and must choose a different slug.
4. **Given** a merchant unpublishes their storefront, **When** a customer visits the URL, **Then** they see a clear "store not available" message and cannot browse or order.

---

### User Story 3 - Merchant Publishes Products to Their Storefront (Priority: P1)

A merchant using Ampairs management marks specific products as listed on their ecom storefront. Any subsequent price, stock, name, category, brand, or subcategory changes made in the management system are automatically reflected in the storefront within a few seconds.

**Why this priority**: Without accurate product data flowing from management to the storefront, the platform cannot operate. This is the data foundation everything else depends on.

**Independent Test**: Can be tested by listing a product in management, then verifying it appears on the storefront with correct details. Update the price and verify the storefront reflects the new price within 5 seconds.

**Acceptance Scenarios**:

1. **Given** a merchant marks a product as "listed" in Ampairs management, **When** the event is processed, **Then** the product appears on `store.ampairs.com/{slug}` with correct name, price, stock, category, and brand.
2. **Given** a listed product's price is updated in management, **When** a few seconds pass, **Then** the storefront shows the new price without any manual action.
3. **Given** a product's stock drops to zero in management, **When** the update propagates, **Then** the product is shown as "Out of Stock" on the storefront and cannot be added to cart.
4. **Given** a merchant marks a product as "unlisted" in management, **When** the event is processed, **Then** the product is removed from the storefront and returns no search results.

---

### User Story 4 - End Customer Manages Their Account Across Storefronts (Priority: P2)

A customer registers once on any Ampairs-powered storefront. They can log in at any other merchant's storefront using the same credentials, view their order history per storefront, and manage their saved addresses and contact details.

**Why this priority**: Single identity across workspaces is a platform differentiator. Without it, customers must re-register for every merchant, reducing adoption.

**Independent Test**: Can be tested by registering at Merchant A's storefront, then logging in at Merchant B's storefront with the same credentials and verifying the account is recognized. Order history from Merchant A must not be visible at Merchant B.

**Acceptance Scenarios**:

1. **Given** a customer registers at `store.ampairs.com/merchant-a`, **When** they visit `store.ampairs.com/merchant-b` and log in, **Then** their profile (name, phone, saved addresses) is available but their order history shows only orders placed at Merchant B.
2. **Given** a customer updates their delivery address, **When** they shop at any merchant's storefront, **Then** the updated address is available as a saved option at checkout.
3. **Given** a customer requests to view their orders at Merchant A, **Then** only orders placed on Merchant A's storefront are shown — orders from other merchants are never exposed.

---

### User Story 5 - Merchant Receives Ecom Orders in Management (Priority: P2)

When an end customer places an order on the storefront, the merchant's Ampairs management system receives a fulfillment request. Inventory is deducted and an order record is created in management, linked to the ecom order reference.

**Why this priority**: Orders placed on the storefront must flow into the merchant's existing workflow. Without this, the ecom platform is disconnected from operations.

**Independent Test**: Can be tested by placing a test order on the storefront and verifying a corresponding order record and inventory deduction appear in the Ampairs management system within 30 seconds.

**Acceptance Scenarios**:

1. **Given** an end customer places an order for 3 units of Product A, **When** the ecom module publishes the order event, **Then** the management system deducts 3 units from Product A's stock and creates an order record referencing the ecom order ID.
2. **Given** one or more line items cannot be fulfilled when management processes the order event, **When** the issue is detected, **Then** the order enters "Pending Merchant Review" state, the merchant is notified, and the customer is informed their order is under review.
3. **Given** an order is in "Pending Merchant Review" state, **When** the merchant edits the line items and confirms fulfilment, **Then** the ecom order is updated with the confirmed quantities, the customer is notified of any changes, and fulfilment proceeds.
4. **Given** an order is successfully created in management, **When** the merchant views their orders, **Then** the ecom order is identifiable as originating from the online storefront (distinct from in-store or B2B orders).

---

### User Story 6 - Customer Tracks Their Order Status (Priority: P3)

After placing an order, the customer can view order status updates on their account page (e.g., Confirmed, Processing, Dispatched, Delivered).

**Why this priority**: Order tracking improves customer confidence and reduces merchant support load, but is not blocking for an initial launch.

**Independent Test**: Can be tested by placing an order and verifying the status page reflects updates as the merchant changes the order status in management.

**Acceptance Scenarios**:

1. **Given** a customer has placed an order, **When** they visit their order history, **Then** they see current status, order items, quantities, unit prices, and order total.
2. **Given** a merchant marks an order as dispatched in management, **When** a few seconds pass, **Then** the customer's order page reflects the updated status.

---

### Edge Cases

- What happens when a customer adds an item to their cart and it goes out of stock before checkout is completed?
- What happens if the `EcomOrderPlaced` event fails to deliver to management — is the order retried automatically?
- What happens when two customers simultaneously purchase the last unit of a product? → First confirmed wins. The ecom module accepts both orders optimistically; the management system rejects the second on inventory deduction. The second customer's order is cancelled and they are notified (FR-017).
- What happens if a product's category or brand is deleted in management after it has been listed?
- What happens when a customer tries to check out with an empty cart?
- What happens when a customer visits `store.ampairs.com/{slug}` for a storefront that has not been created or is in Draft state? → Returns a clear "store not found" response (FR-026).
- How are partially fulfillable orders handled (some items available, some not at the time of management processing)? → Order enters "Pending Merchant Review" state. Merchant edits the order (adjusts/removes unavailable line items) and confirms fulfilment. Customer is notified of changes (FR-027, FR-028). Data model supports future split shipments (FR-029).

## Requirements *(mandatory)*

### Functional Requirements

**Catalog Sync (Management → Ecom)**

- **FR-001**: The platform MUST consume product listing events from the management system and make the product visible on the corresponding merchant storefront within 5 seconds.
- **FR-002**: The platform MUST consume price update events and reflect the updated price on the storefront within 5 seconds.
- **FR-003**: The platform MUST consume stock update events and update product availability (in stock / out of stock / limited quantity) on the storefront within 5 seconds.
- **FR-004**: The platform MUST consume name, category, brand, and subcategory update events and update the storefront listing accordingly.
- **FR-005**: The platform MUST consume product unlisting events and immediately remove the product from the storefront and all search results.

**Product Discovery**

- **FR-006**: Customers MUST be able to search for products by name, brand, category, and subcategory within a merchant's storefront.
- **FR-007**: Search and browse results MUST be strictly scoped to the merchant's storefront — products from other merchants must never appear.
- **FR-008**: Customers MUST be able to filter the product listing by category, brand, and subcategory.
- **FR-009**: Out-of-stock products MUST be visually indicated and MUST NOT be addable to cart.

**Cart & Checkout**

- **FR-010**: Any visitor (unauthenticated) MUST be able to add products to a cart, update quantities, and remove items — no login required to build a cart.
- **FR-011**: Cart quantity additions MUST be capped at the current available stock for that product.
- **FR-012**: A customer MUST be prompted to log in or register only when they attempt to confirm an order at checkout. Browsing and cart interaction MUST remain accessible without an account.
- **FR-013**: Upon order confirmation, the platform MUST assign a unique order reference number and display it to the customer.
- **FR-014**: A guest cart MUST persist for at least 24 hours (via session or device token) without requiring an account. On login, the guest cart MUST be merged with any existing saved cart for that account.

**Order Flow (Ecom → Management)**

- **FR-015**: Upon order confirmation, the platform MUST publish an `EcomOrderPlaced` event to the management system containing: order reference, workspace ID, line items with quantities and unit prices, customer details, and delivery address.
- **FR-016**: The management system MUST process the `EcomOrderPlaced` event to deduct inventory and create an order record linked to the ecom order reference.
- **FR-017**: If management cannot fully fulfil an ecom order (stock unavailable, race condition, or partial availability), the order MUST be placed in a "Pending Merchant Review" state rather than auto-cancelled. The merchant is notified to review and act.
- **FR-027**: The merchant MUST be able to edit a "Pending Merchant Review" ecom order in management — adjusting line-item quantities or removing unavailable items — and then mark it as confirmed for fulfilment.
- **FR-028**: Once the merchant confirms the (possibly edited) order, the ecom module MUST reflect the updated line items and status, and the customer MUST be notified of any changes to their order.
- **FR-029**: The ecom order data model MUST support line-item level status and shipment grouping to enable partial fulfilment with split shipments in a future iteration. v1 need not expose split shipment UI, but the structure must not preclude it.
- **FR-018**: Failed event deliveries MUST be retried with backoff. Unrecoverable failures MUST be surfaced in an operational dead-letter queue.

**End-Customer Identity**

- **FR-019**: Customers MUST be able to register with an email address and password from any merchant storefront. Registration and login MUST be handled by the existing `auth` module, with end customers stored as a distinct user type (no workspace roles assigned).
- **FR-020**: A customer registered at any storefront MUST be able to log in at any other merchant's storefront using the same credentials (single platform-wide identity), leveraging the existing auth module's JWT and refresh token infrastructure.
- **FR-021**: Customer order history MUST be scoped to the storefront where orders were placed — cross-merchant order visibility is NOT permitted.
- **FR-022**: Customers MUST be able to save and manage multiple delivery addresses on their account.

**Workspace Isolation**

- **FR-023**: All product data, storefront configuration, and orders MUST be isolated per workspace — no cross-merchant data leakage is permitted under any circumstances.
- **FR-024**: A merchant MUST be able to create a storefront from within Ampairs management as an explicit setup step. The storefront URL `store.ampairs.com/{workspace-slug}` is reserved at creation time.
- **FR-025**: A storefront MUST support a Draft state (visible only to the merchant for preview) and a Published state (publicly accessible). Merchants MUST be able to publish and unpublish their storefront at any time.
- **FR-026**: Visiting a storefront URL that does not exist or is in Draft state MUST return a clear "store not found" or "coming soon" response — no partial or broken page.

### Key Entities

- **Storefront**: Represents a merchant's public-facing store. Created explicitly by the merchant as a setup step — not auto-provisioned per workspace. Tied to a single workspace by its slug. Has a lifecycle: Draft (being configured) → Published (publicly accessible) → Unpublished (taken offline). Carries display configuration (name, logo, description).
- **Listed Product**: A denormalized snapshot of a product as it appears on the storefront. Contains name, brand, category, subcategory, price, stock status, and image references. Kept in sync with the management system via events. Strictly scoped to one workspace.
- **End Customer**: A shopper with a single platform-wide identity managed by the existing `auth` module (email + password, JWT + refresh token, device session). Distinct user type with no workspace roles. Can interact with multiple merchant storefronts. Owns saved addresses and has a per-storefront order history.
- **Cart**: A collection of line items linked to a specific storefront and customer (or anonymous session). Each line item holds a product reference, quantity, and a price snapshot at time of addition.
- **Ecom Order**: An order placed by an end customer. Contains line items (each with its own status and shipment group reference), delivery address, customer reference, workspace reference, order-level status, and a unique ecom order reference. Linked to the management system's order record once created. Order-level status: Placed → Pending Merchant Review (if fulfilment issue) → Confirmed → Processing → Dispatched → Delivered / Cancelled.
- **Order Line Item**: A single product within an ecom order. Carries quantity (as ordered), quantity confirmed by merchant, unit price snapshot, line-item status, and shipment group reference. Designed to support partial fulfilment and split shipments in future iterations.
- **Order Status Update**: A status change event propagated from the management system back to the ecom module, applied at both order level and line-item level.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A customer can go from landing on a merchant's storefront to completing an order in under 3 minutes on a standard mobile connection.
- **SC-002**: Product search returns relevant results within 1 second for 95% of queries under normal load.
- **SC-003**: Price and stock changes made in the management system are visible on the storefront within 5 seconds for 99% of updates.
- **SC-004**: The platform supports at least 500 concurrent shoppers across all storefronts without visible degradation in search or page load response.
- **SC-005**: 95% of `EcomOrderPlaced` events are successfully processed by the management system within 30 seconds of order confirmation.
- **SC-006**: End customers can register once and successfully log in at any merchant storefront using the same credentials — verified by cross-storefront login test.
- **SC-007**: Zero cross-workspace data leaks — no product, order, or customer data from one merchant is accessible via another merchant's storefront, verified by automated isolation tests.
- **SC-008**: A customer's cart persists for at least 24 hours, reducing incomplete-session abandonment.

## Assumptions

- Payment processing is out of scope for this specification. Orders are confirmed without a payment gateway; payment collection is handled offline or in a future iteration.
- The ecom feature is implemented as a dedicated module within the existing Ampairs monorepo and runs as part of the `ampairs_service`. It is intentionally structured for future extraction into an independent service when scaling demands it.
- All event-driven communication between management modules and the ecom module uses Kafka from day one — the same topic names, schemas, and consumer contracts that will be used post-extraction. Local development requires Kafka running via `docker-compose`. This ensures event durability, Elasticsearch index replayability, and a zero-rework extraction path.
- Workspace slugs are unique and treated as immutable once assigned. Slug changes are a separate operational concern outside this feature.
- Product images are stored on a CDN managed by the management system. The ecom module references image URLs but does not host images.
- End-customer registration and login reuse the existing `auth` module (JWT signing, refresh tokens, device sessions). End customers are a distinct user type within the same auth infrastructure — they have no workspace roles or B2B permissions. No separate auth service is introduced.
- Delivery fee calculation and shipping provider integration are out of scope for this specification.
- The initial launch targets a single deployment region; multi-region is a future concern.

## Clarifications

### Session 2026-05-30

- Q: Is the ecom feature a standalone microservice or a module within the existing monorepo? → A: A new module within the existing Ampairs monorepo, running as part of `ampairs_service`. Designed to be extracted into an independent service when scaling requires it.
- Q: Should internal events between the management modules and the ecom module use Kafka (even within the same service) or in-process Spring Application Events? → A: Kafka from day 1 — same topics and consumer contracts regardless of deployment boundary. Enables event durability, ES index replayability, and a zero-rework extraction path.
- Q: At what point must an end customer log in or register? → A: Login required only at checkout confirmation — browsing and cart are fully public (no account needed). Account prompt appears when the customer attempts to confirm an order.
- Q: How is a merchant's storefront created and activated? → A: Merchant-initiated — a storefront does not exist until the merchant explicitly creates and configures it as a distinct setup step. Visiting an unconfigured or non-existent storefront URL returns a "store not found" response.
- Q: When two customers simultaneously confirm an order for the last unit, which conflict resolution applies? → A: First confirmed wins (optimistic). Both orders are accepted by the ecom module; the management system rejects the second when it attempts inventory deduction. The customer is notified and the order is cancelled per the existing FR-017 rejection flow. No cart-level reservation is required.
- Q: How are partially fulfillable orders handled? → A: The merchant can edit the order in management (adjust quantities, remove unavailable line items) and then mark it as fulfilled. The order model MUST be designed for future extension to partial fulfilment with split shipments — line-item level status and shipment grouping must be structurally supported even if not exposed in v1.
- Q: Should end-customer auth be a standalone system or reuse the existing auth module? → A: Reuse the existing `auth` module — end customers are registered as a distinct user type within the existing auth infrastructure (JWT signing, refresh tokens, device sessions). No separate auth system is needed.
