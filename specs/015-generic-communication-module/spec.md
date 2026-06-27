# Feature Specification: Generic Communication Module

**Feature Branch**: `015-generic-communication-module`
**Created**: 2026-06-27
**Status**: Draft
**Input**: User description: "Generic communication module — a channel-agnostic engine for sending business and customer messages over email, SMS, WhatsApp, and push/in-app, supporting transactional, recurring, and promotional sends with reusable templates, audiences, schedules, and consent handling."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Transactional message on a business event (Priority: P1)

A business owner wants customers to automatically receive a confirmation the moment a meaningful event happens — an order is placed, an invoice is issued, a payment is received. The owner sets up a reusable message once (e.g. "Invoice ready") and chooses which channels it goes out on. From then on, whenever that event occurs, the matching customer is messaged immediately on the selected channels, with the message text personalized from the event's data (customer name, invoice number, amount, due date).

**Why this priority**: Transactional messaging is the highest-value, lowest-risk slice — it is expected by customers, is exempt from marketing opt-out, and proves the whole content→channel→audience→delivery pipeline end to end. Without it nothing else is useful.

**Independent Test**: Configure one template ("Invoice ready") bound to the invoice-created event on email + SMS, trigger an invoice creation for a test customer, and confirm both messages are delivered with correctly substituted values and a recorded delivery status.

**Acceptance Scenarios**:

1. **Given** a template bound to the "invoice created" event on email and SMS, **When** an invoice is created for a customer who has an email and phone on file, **Then** the customer receives both messages within seconds, each with the invoice number, amount, and customer name filled in, and each send is logged with a delivery outcome.
2. **Given** a customer with only a phone number on file (no email), **When** the same event fires, **Then** the SMS is sent, the email is skipped as "no address", and the skip is recorded — the event is not treated as a failure.
3. **Given** a customer who has opted out of *promotional* messages, **When** a *transactional* event fires, **Then** the message is still sent (transactional messages bypass marketing opt-out).
4. **Given** a delivery provider temporarily rejects a message, **When** the send is attempted, **Then** the system retries on a backoff schedule and surfaces the final outcome (delivered, failed, or exhausted) in the message log.

---

### User Story 2 - Recurring scheduled communication (Priority: P2)

A business owner wants certain messages to go out on a repeating schedule without anyone clicking "send" — a monthly account statement to all active customers on the 1st, a weekly low-stock reminder to staff every Monday morning, a payment-due reminder a set number of days before a due date. The owner defines the message, the audience, and a recurrence (frequency, day, time) once; the system then sends it automatically every period at the correct local business time.

**Why this priority**: Recurring sends are the headline "set it and forget it" value, but they depend on the content/channel/audience foundation from Story 1, so they come second.

**Independent Test**: Create a schedule "Monthly statement, 1st of month at 9:00 AM business time, to all active customers, on email", advance the clock to that moment, and confirm one message per eligible customer is generated and sent, and that the next occurrence is computed correctly.

**Acceptance Scenarios**:

1. **Given** a schedule set to "monthly, day 1, 09:00" in a workspace whose business timezone differs from the server's, **When** the 1st of the month reaches 09:00 in the *business* timezone, **Then** the batch is generated and sent — not at 09:00 server time.
2. **Given** a recurring schedule, **When** an occurrence completes, **Then** the system records that occurrence as done and schedules the next one, so the same occurrence is never sent twice even if the scheduler runs repeatedly.
3. **Given** a "monthly on day 31" schedule, **When** the month has fewer than 31 days, **Then** the occurrence fires on the month's last day rather than being skipped.
4. **Given** a schedule with an end date, **When** the end date passes, **Then** no further occurrences are generated.
5. **Given** a schedule is paused by the owner, **When** the next occurrence time arrives, **Then** nothing is sent until the schedule is resumed.

---

### User Story 3 - Promotional campaign to an audience with consent (Priority: P3)

A business owner wants to run a one-off or scheduled promotion — "20% off this weekend" — to a chosen audience (e.g. a customer group), respecting each customer's marketing preferences and quiet hours, and watch how it performed. The owner picks the audience, the message, and the channel; the system sends only to customers who have not opted out of promotions on that channel, avoids sending during configured quiet hours, paces the sends so providers are not overwhelmed, and shows a live rollup of how many were sent, delivered, failed, and skipped.

**Why this priority**: Promotional/bulk messaging carries the most compliance and reputational risk (opt-out, quiet hours, throttling), so it is built last on top of a proven engine.

**Independent Test**: Create a campaign to a 100-customer group on SMS where 10 have opted out of promotions, start it, and confirm exactly 90 are targeted, opt-outs are skipped with a reason, sends respect quiet hours and pacing, and the rollup totals reconcile (targeted = sent + failed + skipped).

**Acceptance Scenarios**:

1. **Given** a campaign to a customer group on a channel, **When** the campaign runs, **Then** customers who opted out of promotions on that channel are excluded and counted as "skipped — opted out", and everyone else is targeted.
2. **Given** quiet hours configured for the audience/workspace, **When** the campaign runs during quiet hours, **Then** sends are deferred until quiet hours end rather than delivered immediately.
3. **Given** a running campaign, **When** the owner pauses it, **Then** no further messages go out until resumed, and already-sent messages are unaffected.
4. **Given** a completed campaign, **When** the owner views it, **Then** a per-recipient rollup shows totals for sent, delivered, failed, and skipped, and the numbers reconcile against the audience size.
5. **Given** a customer who replies STOP / unsubscribes, **When** the system processes that, **Then** their preference is updated and they are excluded from future promotions on that channel.

---

### User Story 4 - Author and reuse a multi-channel, multi-language message template (Priority: P2)

A business owner (or staff member) wants to write a message once and reuse it everywhere, with the right wording per channel (a short SMS vs. a richer email with a subject line) and per language, using placeholders that get filled in at send time. They want to preview how it will look with sample data before relying on it.

**Why this priority**: Templates are the shared "content" backbone that Stories 1–3 all reference; they are a prerequisite for transactional sends, so they ride alongside P1/P2.

**Independent Test**: Create a template with an email variant (subject + body) and an SMS variant in two languages, each using placeholders, preview it with sample values, and confirm the preview substitutes correctly and that the email subject is required while SMS has no subject.

**Acceptance Scenarios**:

1. **Given** a new template, **When** the owner adds an email variant with a subject and body containing placeholders and an SMS variant with body only, **Then** both are saved and the template can be referenced by sends and campaigns.
2. **Given** a template variant, **When** the owner previews it with sample data, **Then** every placeholder is replaced with the sample value and any placeholder lacking sample data is clearly flagged.
3. **Given** a customer with a preferred language, **When** a message using a multi-language template is sent, **Then** the variant matching the customer's language is used, falling back to the default language when none matches.
4. **Given** a channel that requires provider pre-approved templates (e.g. WhatsApp), **When** a variant is created for that channel, **Then** the owner can record the approved template identifier and its parameter mapping.

---

### User Story 5 - Manage communications offline on mobile (Priority: P3)

A staff member using the mobile app wants to draft and manage templates, schedules, and campaigns even with no connectivity, and have their work sync up automatically when back online; the actual sending happens on the server.

**Why this priority**: Mobile management is a reach/usability multiplier but is not required to prove the core sending value, so it is P3.

**Independent Test**: On a device in airplane mode, create a template and a recurring schedule, restore connectivity, and confirm both appear server-side and start taking effect without re-entry.

**Acceptance Scenarios**:

1. **Given** the mobile app is offline, **When** a staff member creates or edits a template/schedule/campaign draft, **Then** it is saved locally and marked pending.
2. **Given** pending local changes, **When** connectivity is restored, **Then** the changes are synchronized to the server automatically and reflected on other devices.
3. **Given** a message has been sent server-side, **When** the staff member opens the app, **Then** they can see the delivery status of recent communications.

---

### Edge Cases

- **Missing address**: a targeted recipient has no address for the chosen channel → that channel is skipped for that recipient and recorded as "skipped — no address"; other channels still proceed.
- **Duplicate suppression**: the same logical message (same recipient, template, occurrence) must not be sent twice even if a trigger fires twice or the scheduler overlaps.
- **Partial channel failure**: a multi-channel send where one channel succeeds and another fails is reported per channel, not as a single all-or-nothing outcome.
- **Template referenced by an active schedule/campaign is deleted/changed**: in-flight and future sends must resolve to a valid template or be blocked with a clear reason rather than send blank content.
- **Workspace timezone change**: changing the business timezone must not double-fire or skip a recurring occurrence around the change.
- **Provider delivery feedback arrives late or out of order** (e.g. a "read" before a "delivered"): the log reflects the furthest-progressed status and does not regress.
- **Audience shrinks/grows between scheduling and send** (a customer is added to or removed from a group): the audience is resolved at send time, and the rollup reflects the audience actually targeted.
- **Quiet-hours window spans midnight**: deferral logic handles windows that cross the day boundary.
- **Hard bounce / invalid recipient**: repeated hard bounces for an address mark it undeliverable so it is not retried indefinitely.
- **Unsubscribe link/keyword**: an opt-out received on any channel updates the customer's preference for that channel.

## Requirements *(mandatory)*

### Functional Requirements

#### Content & templates
- **FR-001**: The system MUST let a workspace define reusable message templates identified by a stable code, each carrying a category of either *transactional* or *promotional*.
- **FR-002**: Each template MUST support one or more per-channel variants (email, SMS, WhatsApp, push/in-app), where the variant holds the channel-appropriate content (e.g. subject + body for email, body only for SMS).
- **FR-003**: Templates MUST support multiple languages per channel, with a defined default language used when a recipient's language has no matching variant.
- **FR-004**: Template content MUST support named placeholders that are substituted with recipient/context data at send time.
- **FR-005**: For channels that require provider pre-approved templates, a variant MUST be able to record the approved template identifier and a mapping of its parameters.
- **FR-006**: The system MUST let a user preview a template variant with sample data, showing the fully substituted result and flagging any placeholder without a value.

#### Channels & delivery
- **FR-007**: The system MUST be able to deliver messages over email, SMS, WhatsApp, and push/in-app in the first release.
- **FR-008**: Delivery MUST be durable: every send is queued, retried on transient failure using a backoff schedule up to a configured limit, and finally marked delivered, failed, or exhausted.
- **FR-009**: The system MUST record a per-message delivery log including channel, recipient, outcome, provider reference, timestamps, and failure reason.
- **FR-010**: The system MUST update delivery status from provider feedback (e.g. delivery receipts, read receipts, bounces) without status regressing to a less-progressed state.
- **FR-011**: A single logical communication targeting multiple channels MUST report the outcome of each channel independently.

#### Audience
- **FR-012**: A send MUST be able to target a single recipient, an explicit list of recipients, or a segment (such as a customer group).
- **FR-013**: Audiences referencing a segment MUST be resolved to concrete recipients at send time.
- **FR-014**: When a targeted recipient lacks an address for the chosen channel, that channel MUST be skipped for that recipient and recorded as skipped, without failing the overall send.

#### Triggers — transactional
- **FR-015**: The system MUST send a transactional message immediately when a configured business event occurs (e.g. order placed, invoice created, payment received) or when explicitly requested via the management interface.
- **FR-016**: Transactional messages MUST bypass promotional opt-out and quiet-hours suppression.

#### Triggers — recurring
- **FR-017**: The system MUST let a user define a recurring schedule with a frequency (daily, weekly, monthly), an interval, a day selector (day-of-week or day-of-month), and a time of day.
- **FR-018**: All recurrence calculation and time-of-day evaluation MUST use the workspace's business timezone, never the server's timezone.
- **FR-019**: Each schedule occurrence MUST be sent at most once; the system MUST track completed occurrences and compute the next one so overlapping scheduler runs do not duplicate sends.
- **FR-020**: A monthly schedule whose target day exceeds the days in a given month MUST fire on that month's last day.
- **FR-021**: Schedules MUST support an optional start date and end date, with no occurrences generated outside that window.
- **FR-022**: A user MUST be able to pause and resume a schedule; a paused schedule generates no occurrences until resumed.

#### Triggers — promotional / campaigns
- **FR-023**: The system MUST let a user create a promotional campaign targeting an audience on a channel, runnable immediately or on a schedule, with a lifecycle of draft, scheduled, running, paused, and done.
- **FR-024**: Campaign sends MUST exclude recipients who have opted out of promotions for that channel and record them as skipped with a reason.
- **FR-025**: Campaign sends MUST respect configured quiet hours by deferring delivery until quiet hours end, including quiet-hour windows that span midnight.
- **FR-026**: Campaign sends MUST be paced/throttled to a configurable rate to avoid overwhelming providers.
- **FR-027**: A user MUST be able to pause and resume a running campaign; pausing stops further sends without affecting already-sent messages.
- **FR-028**: Each campaign MUST provide a per-recipient delivery rollup (targeted, sent, delivered, failed, skipped) whose totals reconcile against the resolved audience.

#### Consent & preferences
- **FR-029**: The system MUST maintain per-customer communication preferences at the granularity of channel and category (transactional vs promotional), defaulting transactional to allowed.
- **FR-030**: The system MUST honor an opt-out received on any channel (e.g. an unsubscribe keyword or link) by updating the corresponding preference and excluding the customer from future promotional sends on that channel.
- **FR-031**: An address that repeatedly hard-bounces MUST be marked undeliverable and excluded from further automatic retries.

#### Multi-tenancy, management & mobile
- **FR-032**: All templates, schedules, campaigns, preferences, and logs MUST be scoped to a workspace and isolated between workspaces.
- **FR-033**: Templates, schedules, campaigns, and preferences MUST be manageable from the mobile app while offline, with local changes synchronized to the server and across devices when connectivity is restored.
- **FR-034**: Message sending MUST be performed server-side; the mobile app composes and manages but does not deliver.
- **FR-035**: Users MUST be able to view recent communication delivery status from the management interface, including on mobile.

### Key Entities *(include if feature involves data)*

- **Message Template**: a reusable, named, categorized (transactional/promotional) piece of content owned by a workspace; parent of its channel variants.
- **Template Variant**: the channel- and language-specific rendering of a template (content + optional subject + optional provider-approved template identifier and parameter mapping).
- **Communication Request**: one logical message to be sent — references a template, the resolved variable values, the audience, and the chosen channel(s); fans out into individual per-recipient, per-channel deliveries.
- **Communication Schedule**: a recurrence definition (frequency, interval, day selector, time of day, business timezone, optional start/end, pause state) bound to a template and audience; produces communication requests on each occurrence.
- **Campaign**: a promotional send to an audience on a channel with a lifecycle, pacing, quiet-hours and consent gating, and an aggregated delivery rollup.
- **Communication Preference / Consent**: a customer's per-channel, per-category opt-in/opt-out state plus undeliverable/bounce flags.
- **Communication Log**: the durable record of each delivery attempt and its outcome, updated by provider feedback.
- **Audience**: the targeting definition (single recipient, explicit list, or segment such as a customer group) resolved to concrete recipients at send time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: When a configured business event occurs, the matching transactional message is delivered to the recipient on all selected channels within 30 seconds at least 99% of the time.
- **SC-002**: A workspace owner can create a reusable template and send a personalized message to a customer through the management interface in under 3 minutes without assistance.
- **SC-003**: Recurring schedules fire at the correct business-local time with zero duplicate or skipped occurrences across a full month of operation, including for workspaces whose business timezone differs from the server.
- **SC-004**: Zero promotional messages are delivered to customers who have opted out of that channel, and zero promotional messages are delivered during configured quiet hours.
- **SC-005**: For any campaign, the delivery rollup totals (sent + failed + skipped) reconcile exactly with the resolved audience size.
- **SC-006**: A duplicate trigger or overlapping scheduler run never results in the same logical message being delivered to the same recipient more than once.
- **SC-007**: Staff can author templates, schedules, and campaigns on mobile while offline, and 100% of those drafts synchronize to the server and other devices within one minute of connectivity being restored.
- **SC-008**: At least 95% of sent messages reach a terminal delivery status (delivered/failed/exhausted) in the log within their retry window, with an accurate failure reason where applicable.

## Assumptions

- The existing notification capability (per-message channel delivery, queue, retry, delivery status for SMS and push) is reused as the low-level transport; this feature adds the orchestration layer and the new email and WhatsApp delivery capabilities on top of it.
- Recipients (customers) and segments (customer groups) already exist in the system and provide the addresses (email/phone) and language preferences used for targeting and rendering.
- Domain events for order, invoice, and payment lifecycle are already published and can be subscribed to as transactional triggers.
- Each workspace already has a business timezone (and locale) available for scheduling and rendering.
- Provider accounts and credentials for each channel (email, SMS, WhatsApp) are configured per workspace or per environment by an administrator; provider selection/failover is an operational concern outside this spec.
- WhatsApp (and some SMS routes) require provider pre-approved message templates; producing and approving those with the provider is an administrative prerequisite, while this feature records and references the approved identifiers.
- "Quiet hours" and promotional throttling rates are configurable per workspace with sensible defaults.
- Message content retention and delivery-log retention follow standard practices for the domain unless an administrator configures otherwise.

## Dependencies

- Customer/segment data (addresses, language, group membership) for audience resolution.
- Workspace business timezone/locale for scheduling and content rendering.
- Existing domain-event stream (order/invoice/payment) for transactional triggers.
- Existing low-level multi-channel delivery (queue, retry, status) for SMS and push, extended with email and WhatsApp delivery.
- Externally provisioned channel provider accounts and any provider-side pre-approved templates.

## Out of Scope (this feature)

- Two-way conversational messaging / inbound chat handling beyond processing opt-out (STOP/unsubscribe) signals.
- Visual drag-and-drop email design tooling (templates are content + placeholders, not a WYSIWYG builder).
- Advanced audience segment building beyond existing customer groups / explicit lists (e.g. behavioral segmentation, A/B testing).
- Provider billing, cost optimization, and least-cost routing.
- Analytics beyond per-campaign delivery rollups and per-message delivery status (e.g. click-through funnels, attribution).
