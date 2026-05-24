# Feature Specification: Ampairs Platform Transformation

**Feature Branch**: `006-006-ampairs-platform`
**Created**: 2026-05-16
**Status**: Draft
**Input**: Transform Ampairs from a modular business management system into an open platform with OAuth 2.0, webhooks, versioned public API, app marketplace, dynamic schema, and workflow automation.

---

## User Scenarios & Testing *(mandatory)*

### User Story 0 — Permission Scope System (Priority: P0 — prerequisite)

Before any API key or OAuth token can declare scopes, the platform needs to define what scopes exist. The Ampairs team designs and publishes a complete catalog of permission scopes — one scope per resource per operation (e.g., `read:orders`, `write:orders`, `read:customers`). This catalog is then hardcoded into the platform's authorization layer, which checks every public API request against the caller's granted scopes before any data is returned or written.

This is a greenfield build: Ampairs currently uses workspace-level JWT access only — there is no resource-level permission system, no scope catalog, and no enforcement layer.

**Why this priority**: Every other platform feature — API keys, OAuth, app installs, marketplace consent screens — depends on knowing what a "scope" is and having something that enforces it. Without this, scopes are just labels.

**Independent Test**: Call a public API endpoint with a credential that has no matching scope and confirm it returns HTTP 403. Call the same endpoint with a credential that has the correct scope and confirm it returns the data. No UI required.

**Acceptance Scenarios**:

1. **Given** the platform has a published scope catalog, **When** a developer views the API documentation, **Then** they can see a complete list of all available scopes with their resource, operation, and plain-language description.
2. **Given** a request is made with a valid credential that has `read:orders` scope, **When** it calls the orders listing endpoint, **Then** the platform returns the orders data.
3. **Given** a request is made with a valid credential that has only `read:orders` scope, **When** it calls any non-orders endpoint (e.g., customers, invoices, products), **Then** the platform returns HTTP 403 with `insufficient_scope` — regardless of how the credential was issued.
4. **Given** a request is made with no credential at all, **When** it calls any public API endpoint, **Then** the platform returns HTTP 401 with `missing_credentials`.
5. **Given** the scope catalog covers all Phase 1 resources, **When** a new resource is added to the public API, **Then** it MUST also have corresponding scopes registered in the catalog before it can be accessed.

---

### User Story 1 — Developer API Key & First API Call (Priority: P1)

An internal developer needs to pull order data into a custom report. They open workspace settings, navigate to the Developer section, and generate an API key named "Reporting Integration" scoped to `read:orders` and `read:customers`. They copy the key and make their first authenticated GET request to the versioned orders endpoint. The response is identical in shape across all future API calls until a new breaking version is released.

**Why this priority**: Without a stable, authenticated public API, no integration can be built. This is the foundation every other platform feature depends on.

**Independent Test**: A developer can create a key, call `GET /public/v1/orders`, receive a valid paginated response, and see the key listed in workspace settings — all without any other platform feature being built.

**Acceptance Scenarios**:

1. **Given** a workspace admin is on the Developer Settings page, **When** they create an API key with name "Tally Sync" and scopes `read:invoices, read:customers`, **Then** the key is displayed once in full for copying, stored as a non-recoverable hash, and listed in settings with creation date and scope summary.
2. **Given** a valid API key, **When** a request includes it in the `Authorization` header, **Then** the platform returns the requested resource data with rate-limit headers in the response.
3. **Given** a valid API key with only `read:orders` scope, **When** it is used to request `GET /public/v1/customers`, **Then** the platform returns HTTP 403 with machine-readable error code `insufficient_scope`.
4. **Given** an API key is revoked by the workspace admin, **When** a subsequent request is made using that key, **Then** the platform returns HTTP 401 immediately with error code `revoked_key`.

---

### User Story 2 — Outbound Webhook Subscription (Priority: P5)

A workspace admin wants their Tally accounting system to automatically receive invoice data when invoices are finalized. They register a webhook endpoint URL and select the `invoice.finalized` event type. When the next invoice is finalized, Ampairs sends a signed HTTP POST to that URL with the invoice payload. The admin can view the delivery log and manually retry failed deliveries.

**Why this priority**: Integrations can poll the public API as a viable alternative until webhooks are built. Every other Phase 1 feature (scopes, API keys, OAuth, versioned endpoints) delivers standalone value without real-time push. Webhooks add operational complexity — retry logic, signature verification, endpoint health monitoring — that is worth doing properly but only after the core API surface is stable.

**Independent Test**: Register a webhook URL, finalize a test invoice, and confirm the signed payload arrives within 30 seconds with the correct event structure — no other platform feature required.

**Acceptance Scenarios**:

1. **Given** a webhook endpoint is registered for `invoice.finalized`, **When** an invoice status changes to finalized, **Then** Ampairs delivers a POST request within 30 seconds containing the invoice payload and an `X-Ampairs-Signature` header.
2. **Given** a webhook delivery fails with HTTP 5xx, **When** 5 minutes have passed, **Then** Ampairs retries the delivery automatically and the delivery log records the retry attempt with its response.
3. **Given** a webhook endpoint has been unreachable for 24 hours across all retry attempts, **When** the final retry fails, **Then** the endpoint is automatically disabled and the workspace admin receives an email notification.
4. **Given** a workspace admin views the webhook delivery log, **When** they select a failed delivery, **Then** they see the full request payload, the HTTP response body, and a "Retry Now" button.

---

### User Story 3 — OAuth 2.0 Authorization for Third-Party Integrations (Priority: P2)

A Shopify integration needs to sync product catalog and orders for a workspace. The integration redirects the workspace admin to Ampairs' authorization screen, which shows the integration's name and the exact permissions being requested (`read:products`, `read:orders`) in plain language. The admin reviews and approves. The integration receives a scoped access token and begins syncing data.

**Why this priority**: OAuth 2.0 is the security model that underpins the consent screen for the marketplace — without it, app installation in Phase 2 cannot exist. Building it early in Phase 1 means the marketplace can be built directly on top of a proven authorization layer rather than retrofitting it later.

**Independent Test**: Register a test OAuth app, trigger the authorization code flow, complete the consent screen, exchange the code for a token, and call a scoped endpoint successfully.

**Acceptance Scenarios**:

1. **Given** an OAuth application is registered with `read:products` scope, **When** the admin is redirected to the authorization URL, **Then** the consent screen shows the app name, requested scopes in plain language, and Approve/Deny buttons.
2. **Given** the admin approves consent, **When** the authorization code is exchanged for tokens, **Then** the integration receives an access token and refresh token; the workspace admin sees the app listed under "Authorized Applications".
3. **Given** a valid access token, **When** the integration calls an endpoint outside its granted scope, **Then** it receives HTTP 403 with error code `insufficient_scope`.
4. **Given** an admin revokes an authorized application from workspace settings, **When** the integration uses its existing token, **Then** all subsequent requests return HTTP 401 with `authorization_revoked`.

---

### User Story 4 — ISV App Publishing (Priority: P3)

An ISV has built a WhatsApp Business integration. They visit the ISV portal, create an app listing with name, description, category, required OAuth scopes (`read:customers`, `read:orders`), subscribed webhook events (`order.created`, `order.status_changed`), and a callback URL. They submit it for review. After Ampairs approves it, the app appears in the marketplace.

**Why this priority**: The ISV submission pipeline gates the marketplace. It can be built and tested internally before any external ISV exists.

**Independent Test**: An internal team member submits a test app through the ISV portal, an Ampairs admin approves it, and it appears in the marketplace — no external ISV involvement needed.

**Acceptance Scenarios**:

1. **Given** an ISV completes the app submission form with all required fields, **When** they submit, **Then** the app status becomes "Under Review" and the ISV receives a confirmation email.
2. **Given** an Ampairs reviewer approves the submission, **When** approval is saved, **Then** the app becomes "Listed" in the marketplace and the ISV receives an approval email.
3. **Given** a reviewer rejects a submission with a stated reason, **When** rejection is saved, **Then** the ISV receives a rejection email with the reason and can edit and resubmit.

---

### User Story 5 — Workspace Admin App Discovery & Installation (Priority: P3)

A workspace admin browses the marketplace, finds the WhatsApp Business app, reads its description, and clicks "Install". A consent screen lists exactly what data the app will access. After approving, the app appears in "Installed Apps" and the ISV's integration begins receiving webhook events for that workspace.

**Why this priority**: App installation is the core action that drives ISV value and is the primary engagement metric for the marketplace.

**Independent Test**: An admin installs a pre-approved test app from the marketplace, the consent screen appears, approval succeeds, the app is listed as installed, and a test webhook event arrives at the app's callback URL.

**Acceptance Scenarios**:

1. **Given** an approved app is listed in the marketplace, **When** the admin clicks "Install", **Then** a consent screen lists each scope and what data it accesses, with Install and Cancel buttons.
2. **Given** the admin approves installation, **When** installation completes, **Then** the app appears in "Installed Apps" with the install date, and the ISV's webhook subscriptions become active for that workspace.
3. **Given** an admin uninstalls an app, **When** uninstallation completes, **Then** all OAuth tokens issued to that app for that workspace are immediately invalidated and webhook deliveries stop.

---

### User Story 6 — Custom Object Type Creation (Priority: P4)

A workspace admin at a manufacturing company needs to track equipment service records — a concept that doesn't exist in the standard data model. They navigate to Settings > Custom Objects, define a type called "Service Record" with fields: asset name (text, required), service date (date, required), technician name (text), cost (number), and notes (text). They then create the first record.

**Why this priority**: Custom objects unlock vertical use cases the standard data model cannot serve — a key differentiator for platform stickiness across industries.

**Independent Test**: Create a "Machinery" object type with 4 fields, add 2 records, retrieve them via the API, and update one — without modifying any core module.

**Acceptance Scenarios**:

1. **Given** an admin defines a custom object type "Service Record" with 3 fields, **When** they save it, **Then** the type appears in the custom objects list and the API accepts create/read/update/delete requests for that object type.
2. **Given** a custom object type exists, **When** an admin submits a new record with all required fields filled, **Then** the record is saved and appears in the list with its field values.
3. **Given** a required field is left empty when creating a record, **When** the admin submits, **Then** validation fails with an error identifying the missing required field.
4. **Given** a custom object type has existing records, **When** an admin tries to change the type of an existing required field, **Then** the change is rejected with an error explaining why.

---

### User Story 7 — Workflow Automation Rule (Priority: P4)

A workspace admin wants to send a WhatsApp confirmation when a large order is placed. They create an automation rule: trigger = "Order Created", filter = order total > ₹10,000, action = "Send WhatsApp notification" using a pre-configured template. The rule activates. When the next qualifying order is placed, the message is sent automatically and appears in the execution history.

**Why this priority**: Automation rules surface the compounding value of the platform — connecting events to actions across features in ways no pre-built screen can anticipate.

**Independent Test**: Create a rule for "Customer Created → Send SMS", create a test customer, and confirm the SMS action appears in the execution history with "Success" status.

**Acceptance Scenarios**:

1. **Given** an active automation rule, **When** a triggering event occurs and all filter conditions match, **Then** the configured action executes within 60 seconds and the execution is logged with "Success" status.
2. **Given** a trigger event occurs but filter conditions do not match, **When** the rule evaluates, **Then** no action is taken and the execution log records "Filtered Out" with the unmatched condition.
3. **Given** an action fails due to an invalid configuration, **When** execution completes, **Then** the log shows "Failed" with the specific error, and the admin can view the full error detail.
4. **Given** an automation rule would create a circular trigger loop, **When** the admin attempts to save it, **Then** the platform rejects the save with an error explaining the circular dependency.

---

### Edge Cases

- What happens when a webhook endpoint is unreachable for 24 hours? → Endpoint is auto-disabled; the workspace admin is notified by email with steps to re-enable.
- What if a workspace admin revokes an installed app — do in-flight API calls fail immediately? → Yes; token invalidation is synchronous and affects all calls in progress.
- What happens when a custom object field type is changed after records exist? → Type changes are rejected; only new optional fields may be added to an existing type.
- Can a workflow create a circular trigger loop? → Detected at rule-save time; the rule is rejected with a plain-language error explaining the loop.
- What if an ISV's app is removed from the marketplace? → Existing installations remain active; new installations are blocked.
- What happens when an OAuth access token expires mid-session? → The integration receives HTTP 401; it must use its refresh token to obtain a new access token.
- How are rate limits communicated when exceeded? → HTTP 429 with a `Retry-After` header and rate-limit headers showing the reset timestamp.
- What if a bulk custom object import has rows with missing required fields? → Import fails with per-row validation errors listing which field is missing for each failed row.

---

## Requirements *(mandatory)*

### Functional Requirements

**Phase 1 — Permission Scope System (prerequisite to all other Phase 1 work)**

- **FR-P0-001**: The platform MUST define and publish a complete permission scope catalog covering all public API resources and operations before any credential-based access is enabled
- **FR-P0-002**: The scope catalog MUST use a consistent `{operation}:{resource}` naming pattern (e.g., `read:orders`, `write:customers`, `read:invoices`) for every resource exposed on the public API
- **FR-P0-003**: Every scope MUST have a plain-language description suitable for display on consent screens and developer documentation (e.g., "Read order history and order details")
- **FR-P0-004**: The platform's authorization layer MUST validate every inbound public API request against the caller's granted scopes before any business logic executes
- **FR-P0-005**: The scope enforcement layer MUST be the single point of authorization for all public API access — no endpoint may bypass it or implement its own scope check

**Phase 1 — Public API & Developer Platform**

- **FR-001**: Workspace admins MUST be able to create named API keys with a declared set of resource permission scopes from within workspace settings
- **FR-002**: API keys MUST be shown in full only once at creation time; subsequent views show only the last 4 characters as an identifier hint
- **FR-003**: Workspace admins MUST be able to revoke any API key from the settings list, taking effect immediately
- **FR-004**: The versioned public API MUST expose all core domain resources — customers, products, orders, invoices, inventory, and units — under stable versioned paths
- **FR-005**: All public API error responses MUST include a machine-readable error code, a human-readable message, and a link to relevant documentation
- **FR-006**: All public API responses MUST include rate-limit headers: remaining allowed requests, the limit ceiling, and the reset timestamp
- **FR-007**: The platform MUST enforce per-workspace, per-API-key rate limits and return HTTP 429 with a `Retry-After` header when a limit is exceeded
- **FR-008**: Workspace admins MUST be able to register outbound webhook endpoints with a target URL, selected event types, and a description
- **FR-009**: The platform MUST deliver a signed HTTP POST to all registered, subscribed endpoints within 30 seconds of a qualifying domain event occurring
- **FR-010**: Webhook payloads MUST be signed using HMAC-SHA256 with a per-endpoint secret, delivered in the `X-Ampairs-Signature` request header
- **FR-011**: Failed webhook deliveries MUST be automatically retried with exponential backoff across 5 attempts: immediately, 5 min, 30 min, 2 hrs, 24 hrs
- **FR-012**: Workspace admins MUST be able to view webhook delivery history — status, event type, timestamp, request payload, and response — for the last 30 days
- **FR-013**: Workspace admins MUST be able to manually trigger a retry for any failed delivery from the delivery log
- **FR-014**: A webhook endpoint with no successful delivery for 24 consecutive hours MUST be automatically disabled; the workspace admin MUST be notified by email
- **FR-015**: The platform MUST support OAuth 2.0 Authorization Code flow for user-facing applications and Client Credentials flow for server-to-server integrations
- **FR-016**: Users MUST be shown a consent screen listing the requesting application's name and each requested permission in plain language before authorizing
- **FR-017**: Workspace admins MUST be able to view all authorized OAuth applications and revoke access for any of them from workspace security settings

**Phase 2 — App Marketplace**

- **FR-018**: Developers MUST be able to register on the ISV portal and create app listings with required scopes, webhook event subscriptions, category, name, description, and support contact
- **FR-019**: App submissions MUST enter a review queue visible to the Ampairs team before the app appears in the marketplace
- **FR-020**: Developers MUST receive email notifications when their submission is approved or rejected; rejection emails MUST include the reviewer's stated reason
- **FR-021**: Workspace admins MUST be able to browse, search by name, and filter the marketplace by category
- **FR-022**: App installation MUST present a consent screen showing all scopes the app requests, what data each scope accesses, and an explicit Install/Cancel choice
- **FR-023**: Installed apps MUST be restricted to the scopes granted at install time; any request outside those scopes MUST return HTTP 403
- **FR-024**: Workspace admins MUST be able to uninstall any installed app, immediately invalidating all its tokens and stopping all its webhook subscriptions for that workspace
- **FR-025**: Installed apps MUST receive webhook deliveries only for event types declared in their listing and only for workspaces where they are installed and active

**Phase 3 — Dynamic Schema & Workflow Automation**

- **FR-026**: Workspace admins MUST be able to define custom object types with a name, description, and typed fields: text, number, date, boolean, single-select enum, and relation to an existing core entity
- **FR-027**: Each custom object type MUST be accessible via the public API for create, read, update, and delete operations using a URL slug derived from the type name
- **FR-028**: Adding new optional fields to an existing custom object type MUST be supported; changing the type of an existing field once records exist MUST be rejected
- **FR-029**: Workspace admins MUST be able to define automation rules with one trigger event type, optional field-level filter conditions, and one or more sequential actions
- **FR-030**: Available trigger events MUST cover all core domain events: order created/updated/completed, invoice created/finalized, customer created/updated, product created/updated
- **FR-031**: Available automation actions MUST include: send WhatsApp/SMS/push notification using a configured template, update a field value on a core entity, create a custom object record, and call an external webhook URL
- **FR-032**: Automation rules MUST execute within 60 seconds of the triggering event
- **FR-033**: The platform MUST detect circular automation rule dependencies at save time and reject the rule with an explanatory error
- **FR-034**: Workspace admins MUST be able to view execution history per rule: trigger timestamp, filter result (matched/filtered), per-action outcomes, and error details for any failure

### Key Entities

- **PermissionScope**: A platform-defined declaration of a specific access right — identified by a `{operation}:{resource}` key, with a plain-language description used in consent screens and documentation. Maintained as a platform catalog, not per-workspace.
- **ApiKey**: A long-lived workspace credential with a name, declared permission scopes, creation date, creator identity, and active/revoked status; the key value is hashed at rest.
- **WebhookEndpoint**: A registered URL with subscribed event types, a per-endpoint signing secret, description, and enabled/disabled status.
- **WebhookDelivery**: A single delivery attempt recording event type, request payload (JSON snapshot), HTTP response code and body, attempt number, and final status.
- **OAuthApplication**: A registered client with client ID/secret pair, name, declared scopes, redirect URIs, and review status (pending/approved/rejected).
- **OAuthGrant**: An authorization record linking an OAuthApplication to a specific workspace, capturing granted scopes, token expiry, and revoked status.
- **AppListing**: A marketplace entry with name, description, category, required scopes, subscribed events, support contact, review status, and ISV owner.
- **AppInstallation**: Records that a workspace has installed a specific app, with the scopes granted at install time, install timestamp, and active/revoked status.
- **CustomObjectType**: A workspace-defined entity schema with a name, URL slug, description, and ordered list of field definitions.
- **CustomObjectField**: A single typed field definition: name, slug, type (text/number/date/boolean/enum/relation), required flag, and enum options where applicable.
- **CustomObjectRecord**: An individual record conforming to a CustomObjectType, holding typed field values indexed by field slug, with standard created/updated timestamps.
- **AutomationRule**: A workspace-defined rule with a trigger event type, optional filter expression, ordered list of action definitions, enabled status, and name.
- **AutomationExecution**: A single run log for an AutomationRule recording trigger event ID, filter evaluation result, per-action outcomes, overall status, and error messages.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer can create an API key, read the quickstart guide, and make their first successful authenticated API call in under 5 minutes
- **SC-002**: 95% of outbound webhook deliveries reach registered endpoints within 30 seconds of the triggering domain event
- **SC-003**: The webhook retry mechanism successfully delivers at least 90% of initially-failed events within 24 hours without manual intervention
- **SC-004**: An OAuth Authorization Code flow — from redirect to consent approval to a usable access token — completes in under 60 seconds
- **SC-005**: A workspace admin can discover an app in the marketplace, review its permissions, and complete installation in under 3 minutes
- **SC-006**: A workspace admin can define a new custom object type with 5 fields and create the first record in under 10 minutes
- **SC-007**: A workspace admin can create an automation rule (trigger → filter → action), activate it, and verify a successful run in under 5 minutes
- **SC-008**: 100% of API requests using scopes not granted to the key or token are rejected — no authorization bypass is possible
- **SC-009**: The versioned public API maintains 99.9% uptime monthly; a released version remains available for at least 12 months before deprecation
- **SC-010**: A workspace can operate with 50 installed apps and 100 active automation rules without measurable degradation in core business operation response times
- **SC-011**: An ISV developer can go from first visit to the ISV portal to having their app available for internal installation in under one business day (excluding review time)

---

## Assumptions

1. **Permission system is greenfield**: Ampairs currently has no resource-level authorization — only workspace-scoped JWT access. The scope catalog and enforcement layer are net-new builds and must be completed before any other Phase 1 work can ship.
2. **Custom objects are workspace-scoped**: Each workspace independently defines its own object types; schemas are not shared across workspaces (Salesforce model).
3. **OAuth grant types**: Authorization Code (user-facing apps) and Client Credentials (server-to-server, e.g., Tally nightly sync) are both supported. OAuth is P2 in Phase 1 — built early to underpin the marketplace consent model. Webhooks are the lowest Phase 1 priority as integrations can poll the public API in the interim.
4. **Marketplace starts internal-first**: Phase 2 marketplace and ISV portal are built for the internal team; opening to external ISVs is a policy change, not a rebuild.
5. **Workflow automation scope**: Covers trigger-based event→action automation rules. Multi-step approval chains are a separate feature not in this spec.
6. **API versioning strategy**: URL-path versioning (`/public/v1/`, `/public/v2/`); minimum 12-month deprecation notice before removing a version.
7. **Rate limits are tier-configured**: Specific limits (requests per minute) are set by Ampairs ops per workspace subscription tier, not hardcoded.
8. **Webhook replay is manual**: 30-day delivery log enables manual retries; automated event replay beyond the retry window is out of scope for Phase 1.
9. **App review is manual**: Marketplace submissions are reviewed by the Ampairs team; automated security scanning is a future enhancement.
10. **Phase 3 analytics are internal-only**: Platform usage metrics (API volumes, webhook rates, app installs) are visible to the Ampairs team; ISV-facing analytics dashboards are a future initiative.

## Out of Scope

- First-party clients (KMP mobile app, Angular web app) — these use the existing session JWT on internal API paths and are entirely unaffected by the scope system. The scope system applies only to the new `/public/v1/` surface consumed by API keys and OAuth tokens. The KMP app's offline-first sync model, local cache, and device-scoped refresh token flow are unchanged.
- Payment processing or revenue share for paid marketplace apps — Phase 2 covers free app distribution only
- White-labeling the developer portal or marketplace for ISVs
- Mobile SDK for building marketplace apps
- Data export connectors or BI integration
- Multi-step approval workflow chains (separate feature)
- Automated security scanning of ISV app source code or declared scope verification
- ISV-facing analytics dashboards or developer metrics portal
