# Feature Specification: WhatsApp Commerce (conversational ordering)

**Feature Branch**: `023-whatsapp-commerce`
**Created**: 2026-06-28
**Status**: Draft
**Input**: User description: "specs/023-whatsapp-commerce"

## Overview

Indian customers increasingly discover and buy products through WhatsApp. This feature lets an Ampairs
merchant connect their own WhatsApp business number so that customers can **browse the merchant's
catalog, build an order, pay, and receive order updates entirely inside a WhatsApp chat** — while the
merchant fulfils those orders through the same order workflow they already use for every other channel.

The merchant's day-to-day app stays offline-first: WhatsApp orders simply appear in their normal order
list. The live conversation, catalog connection, and messaging-policy handling are managed for them
behind the scenes.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Customer browses and places an order over WhatsApp (Priority: P1)

A customer messages the merchant's WhatsApp number. The merchant's product catalog is presented as an
interactive, tappable list of items. The customer adds items to a cart, reviews it, provides a delivery
address, and confirms. A new order is created in the merchant's order list, with the customer matched (or
created) by their phone number. The conversation tells the customer their order is received.

**Why this priority**: This is the core value of the feature — turning a chat into a real, fulfillable
order inside the merchant's existing workflow. Without it, nothing else matters. It is a complete,
demonstrable MVP on its own (a customer can order; the merchant can fulfil).

**Independent Test**: From a WhatsApp account, message the connected business number, browse the catalog,
add items, confirm the order, and verify a matching order appears in the merchant's order list with the
correct items, quantities, totals, and customer.

**Acceptance Scenarios**:

1. **Given** a merchant with a connected WhatsApp number and a synced catalog, **When** a customer sends
   a message, **Then** the customer receives an interactive presentation of available products.
2. **Given** a customer browsing the catalog, **When** they select items and quantities, **Then** a cart
   is maintained for that customer across messages and can be reviewed on request.
3. **Given** a customer with items in their cart, **When** they confirm and provide a delivery address,
   **Then** a new order is created in the merchant's order list (channel = WhatsApp) with the correct
   line items, totals, and a customer matched by phone number.
4. **Given** the same confirmation is delivered to the system more than once, **When** the order is
   captured, **Then** only one order is created (no duplicates).
5. **Given** a customer references a product no longer available, **When** they try to add it, **Then**
   they are told it is unavailable and the cart is unchanged.

---

### User Story 2 - Customer pays via a link in the chat and the merchant sees it settled (Priority: P2)

After confirming an order, the customer receives a UPI payment link in the chat. When they pay, the
payment is recorded against their order, the customer receives a "payment received" confirmation, and the
merchant sees the order as paid. Cash-on-delivery is available as an alternative.

**Why this priority**: Collecting payment in-chat closes the commerce loop and is the headline reason
merchants want WhatsApp commerce, but ordering (P1) is still valuable on its own (merchant can collect
offline). This builds directly on the confirmed order from P1.

**Independent Test**: Place an order via P1, receive the payment link, complete the payment, and verify
the order shows as paid in the merchant's records and the customer receives a confirmation message.

**Acceptance Scenarios**:

1. **Given** a confirmed order, **When** the order is placed, **Then** the customer receives a payment
   link for the correct order amount (or a cash-on-delivery option).
2. **Given** a customer who pays via the link, **When** the payment settles, **Then** the payment is
   recorded against the order, the customer receives a confirmation message, and the order reflects as
   paid.
3. **Given** a customer who chooses cash-on-delivery, **When** they confirm, **Then** the order proceeds
   without a payment link and is marked for collection on delivery.

---

### User Story 3 - Customer receives order-status updates in the same thread (Priority: P2)

As the merchant progresses the order (confirmed, packed, dispatched, delivered), the customer receives a
status update in the same WhatsApp conversation, keeping them informed without leaving the chat.

**Why this priority**: Status updates greatly improve the customer experience and reduce "where is my
order?" queries, but the order can still be fulfilled without them. Reuses the order lifecycle already
created in P1.

**Independent Test**: Advance an order through its statuses in the merchant app and verify the customer
receives a corresponding message for each meaningful status change.

**Acceptance Scenarios**:

1. **Given** a placed WhatsApp order, **When** the merchant changes its status (e.g. dispatched),
   **Then** the customer receives a corresponding update message in the conversation.
2. **Given** the customer has not messaged recently (outside the allowed free-messaging window), **When**
   a status update is sent, **Then** an approved templated message is used so the update still reaches
   the customer in a policy-compliant way.

---

### User Story 4 - Merchant connects their WhatsApp number and keeps the catalog in sync (Priority: P1)

A merchant connects their WhatsApp Business number to their workspace and turns on catalog sharing. Their
listed products are made available for browsing in WhatsApp, and changes to listed products are reflected
automatically.

**Why this priority**: This is the prerequisite setup for everything else — no connection, no
conversation. It is P1 because Story 1 cannot be demonstrated without it, but it is independently testable
as a setup-and-sync flow.

**Independent Test**: From the merchant app, connect a WhatsApp Business number, enable catalog sharing,
and verify the connection status shows connected and listed products become browsable in a test chat.

**Acceptance Scenarios**:

1. **Given** a merchant without a WhatsApp connection, **When** they complete the connect flow, **Then**
   their workspace shows a connected WhatsApp number and is ready to receive customer messages.
2. **Given** a connected merchant with catalog sharing on, **When** a listed product changes (price,
   availability, name, image), **Then** the change is reflected in what customers can browse.
3. **Given** a merchant who disconnects their number, **When** they confirm, **Then** the workspace stops
   receiving and sending WhatsApp messages.

---

### User Story 5 - Consent and opt-out are respected (Priority: P2)

The merchant may only initiate messages to customers who have opted in, and a customer who sends a
stop/opt-out keyword stops receiving merchant-initiated messages. Consent and opt-out are recorded.

**Why this priority**: Required for policy compliance and to protect the merchant's number from
suspension, but the reactive in-conversation ordering flow (P1) functions on customer-initiated messages
alone, so this can follow.

**Independent Test**: Send a stop/opt-out keyword from a customer account and verify the merchant can no
longer initiate messages to that customer, and that the opt-out is recorded.

**Acceptance Scenarios**:

1. **Given** a customer who has not opted in, **When** the merchant attempts to initiate a message,
   **Then** the send is prevented.
2. **Given** a customer who sends an opt-out keyword, **When** it is received, **Then** the customer is
   marked opted-out, future merchant-initiated messages are blocked, and the event is recorded.
3. **Given** a customer who messages the merchant first, **When** the conversation starts, **Then** the
   merchant may reply freely for the duration of the allowed window.

### Edge Cases

- **Unknown / spoofed inbound traffic**: Messages that cannot be verified as genuinely from the messaging
  provider are rejected and not processed.
- **Message cannot be matched to a connected workspace**: An inbound message for a number not connected to
  any workspace is ignored safely (no error surfaced to a customer).
- **Duplicate delivery of the same message or confirmation**: Repeated deliveries of the same inbound
  message or order confirmation result in a single action (idempotent).
- **Outside the free-messaging window**: When the customer has not messaged recently, merchant-initiated
  messages fall back to approved templated messages; if no suitable approved template exists, the update
  is withheld rather than violating policy.
- **Payment never completes**: An order with an unpaid payment link remains awaiting payment; the merchant
  can still see and act on it, and cash-on-delivery remains an option.
- **Catalog item removed mid-conversation**: An item removed after being added to a chat cart is flagged
  to the customer at review/confirmation rather than silently ordered.
- **Customer phone matches an existing customer**: The order is attached to the existing customer record;
  it does not create a duplicate customer.
- **Connection revoked or credentials expire**: The merchant is shown a clear "reconnect needed" state and
  inbound/outbound messaging pauses until reconnected.

## Requirements *(mandatory)*

### Functional Requirements

**Connection & catalog**

- **FR-001**: A merchant MUST be able to connect their own WhatsApp Business number to their workspace and
  see its connection status.
- **FR-002**: The system MUST make the workspace's listed products available for customers to browse in
  WhatsApp when catalog sharing is enabled.
- **FR-003**: The system MUST keep the browsable WhatsApp catalog consistent with changes to listed
  products (price, availability, name, image, description).
- **FR-004**: A merchant MUST be able to disconnect their WhatsApp number, after which the workspace
  neither sends nor receives WhatsApp messages.

**Conversation & ordering**

- **FR-005**: The system MUST receive customer messages sent to the connected number and respond within
  the conversation.
- **FR-006**: The system MUST present products as an interactive, selectable experience (browsable list /
  product messages) rather than free-text-only.
- **FR-007**: The system MUST maintain a per-customer cart across multiple messages within a conversation,
  and let the customer review it.
- **FR-008**: The system MUST capture a confirmed chat cart as a new order in the merchant's existing
  order workflow, tagged as originating from WhatsApp, with correct line items, quantities, and totals.
- **FR-009**: The system MUST match the ordering customer to an existing customer record by phone number,
  or create one if none exists, without creating duplicates.
- **FR-010**: Order capture MUST be idempotent — a repeated confirmation of the same chat order MUST NOT
  create more than one order.
- **FR-011**: Pricing and tax applied to a WhatsApp order MUST follow the same rules as the merchant's
  other sales channels.

**Payment**

- **FR-012**: On order confirmation, the system MUST provide the customer a way to pay (a payment link for
  the order amount) and MUST support cash-on-delivery as an alternative.
- **FR-013**: When a payment for a WhatsApp order settles, the system MUST record it against that order and
  notify the customer in the conversation.

**Status updates & messaging policy**

- **FR-014**: The system MUST send the customer order-status updates in the conversation as the merchant
  advances the order through its lifecycle.
- **FR-015**: The system MUST distinguish between free-form messages (allowed within the recent-contact
  window) and approved templated messages (required outside it), and choose the correct mode automatically
  so that messaging remains policy-compliant.
- **FR-016**: The system MUST only allow merchant-initiated messaging to customers who have opted in.
- **FR-017**: The system MUST honour a customer opt-out (stop keyword), blocking further merchant-initiated
  messages, and MUST record consent and opt-out events for audit.

**Integrity & safety**

- **FR-018**: The system MUST verify that inbound messages genuinely originate from the messaging provider
  and reject any that cannot be verified.
- **FR-019**: The system MUST process each inbound message at most once, even if delivered multiple times.
- **FR-020**: The system MUST store connection credentials securely and never expose them.
- **FR-021**: All WhatsApp data MUST be isolated per workspace; one merchant MUST never see another's
  conversations, catalog mapping, or orders.

**Merchant visibility**

- **FR-022**: The merchant MUST be able to view WhatsApp conversations/inbox and the WhatsApp connection
  and catalog-sync configuration from their app.
- **FR-023**: WhatsApp-originated orders MUST appear in the merchant's normal (offline-capable) order list
  alongside orders from other channels.

### Key Entities *(include if feature involves data)*

- **WhatsApp Connection (Account)**: A workspace's link to a WhatsApp Business number — its connection
  status, the business number identity, and securely held credentials. One per workspace (initially).
- **Conversation**: An ongoing chat between the merchant's number and one customer (keyed by the customer's
  phone). Holds the current step of the ordering journey, the time of the customer's last message (for the
  free-messaging window), and the customer's opt-in/opt-out status.
- **Cart Item**: A product and quantity the customer has added within a conversation, linked to a catalog
  entry, used to build the eventual order.
- **Catalog Mapping (Item)**: The correspondence between a workspace's listed product and its representation
  in the WhatsApp browsable catalog, so a chat selection can be translated back to a real product.
- **Inbound Message Log**: A record of each received message (with its provider message identifier) used to
  guarantee each is processed only once.
- **Message Template**: A pre-approved message format (e.g. order confirmation, dispatch, payment reminder)
  used for merchant-initiated messages outside the free-messaging window, with its approval status.
- **Captured Order**: The resulting order in the merchant's order workflow, tagged as WhatsApp-originated
  and linked back to the conversation for status updates.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A customer can go from first message to a confirmed order in under 3 minutes for a small
  basket (≤5 items), without leaving the chat.
- **SC-002**: A merchant can connect their WhatsApp number and have their catalog browsable in a test chat
  in under 10 minutes, without developer assistance.
- **SC-003**: At least 95% of confirmed chat orders appear correctly in the merchant's order list with
  matching items, totals, and customer, and 0 duplicate orders are created from repeated deliveries.
- **SC-004**: For orders using the in-chat payment link, the customer receives a payment confirmation
  message within 1 minute of the payment settling.
- **SC-005**: 100% of merchant-initiated messages comply with consent and the messaging window (no message
  is sent to an opted-out customer; no out-of-window free-form message is sent).
- **SC-006**: Customers receive an order-status update for each meaningful status change for at least 95%
  of WhatsApp orders where the conversation is reachable.
- **SC-007**: No cross-workspace data exposure occurs — a workspace never sees another workspace's
  conversations, catalog mapping, or orders.

## Assumptions

- **One number per workspace** in the initial scope; multi-number / multiple business accounts per
  workspace is a later enhancement.
- **The product surface shared to WhatsApp is the merchant's already-listed (public-facing) catalog**, the
  same surface used for the online storefront — not the full internal product list.
- **Payment uses the platform's existing UPI payment-link capability**; this feature consumes it rather
  than building a new payment integration. Cash-on-delivery is supported as a non-link path.
- **The live conversation is online-only**; it is not mirrored for offline use on the merchant's device.
  Only the resulting orders flow into the merchant's offline order list.
- **Customers are identified by phone number**, consistent with how WhatsApp identifies participants.
- **Messaging-policy rules** (opt-in requirement, recent-contact window, approved templates outside it) are
  treated as fixed external constraints the system must comply with.
- **Delivery scope**: Phase 1 = connect + catalog + order capture (Stories 1 & 4); Phase 2 = payment,
  status updates, consent/opt-out (Stories 2, 3, 5); later phases = outbound campaigns and automation
  (out of scope for this spec).

## Out of Scope

- Bulk marketing campaigns, broadcast messaging, and abandoned-cart re-engagement automation.
- A web (browser) WhatsApp console for the merchant.
- Native in-WhatsApp payment (as opposed to a payment link), pending broad availability.
- Managed onboarding / number provisioning through a third-party messaging provider (the abstraction
  allows it later, but it is not part of this delivery).
- Multi-number or multiple business accounts per workspace.

## Dependencies

- An existing **order workflow** that WhatsApp orders are captured into and fulfilled through.
- An existing **listed/online catalog** to share to WhatsApp and keep in sync.
- An existing **customer directory** for phone-based customer matching.
- An existing **UPI payment-link and settlement** capability for in-chat payment (Story 2).
- An existing **outbound messaging/notification** capability used as the transport for sending WhatsApp
  messages and managing templates.
- A connected **WhatsApp Business number** per merchant (merchant-provided during onboarding).
