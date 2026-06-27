# Phase 0 Research & Decisions — Generic Communication Module

Each decision is stated as **Decision / Rationale / Alternatives rejected**, grounded in the existing codebase.

---

## D1. New `communication` module vs. extending `notification`

**Decision**: Create a new top-level bounded context `communication`. `notification` stays the low-level *transport* (per-message channel providers, `notification_queue`, retry/backoff, delivery status). `communication` owns *orchestration* (templates, audiences, schedules, campaigns, consent, logs).

**Rationale**: Constitution Principle IX requires new bounded contexts to be their own module and forbids piling unrelated logic into an existing one. `notification` today is a thin, stable transactional plumbing layer (OTP, push) with no notion of templates, audiences, recurrence, or consent. Mixing campaign/consent logic into it would couple the OTP-critical path to promotional concerns. Keeping them separate also means the only change to `notification` is *additive* (two providers + a structured-enqueue API), preserving the proven OTP/push path.

**Alternatives rejected**: (a) Extend `notification` — rejected: violates module-boundary intent, risks the OTP path, and overloads one module with two bounded contexts. (b) Put orchestration in `setting`/`agent` — rejected: wrong ownership; neither is a messaging context.

---

## D2. HTML email templating — storage & rendering (the key decision)

**Decision**: **Keep communication templates independent of `printing`** with a **structured, server-rendered** model:

- `MessageTemplate` (header: code, category, default locale) → many `TemplateVariant` (one per channel × locale).
- An email variant stores **`subject`, `html_body` (rich HTML), and an optional `text_body` (plain-text alternative)** as separate first-class columns — *not* an opaque blob.
- Rendering is **server-side** using a **logic-less Mustache-style renderer** (`{{variable}}` interpolation only — no loops/conditionals/arbitrary expressions). The same renderer fills the subject, the HTML body, the plain-text body, SMS/WhatsApp/push bodies, and WhatsApp approved-template parameters.
- When `text_body` is absent, derive a plain-text alternative from the HTML at send time (HTML-strip) so every email is effectively multipart (deliverability/spam-score).
- For **promotional** email, the renderer appends a workspace-configured **unsubscribe footer** (`CommunicationConfig.promotional_footer_html`) containing a tokenized unsubscribe link (FR-002b). Transactional email never gets the footer.

**Rationale**:
- `printing` deliberately stores its template layout as an **opaque `template_json` blob that the backend never parses — all rendering happens in the app** (confirmed in `printing/CLAUDE.md`). That model is the *opposite* of what email needs: FR-034 requires **sending (and therefore rendering) to happen server-side**, and we need the subject, variables, channel, and locale to be **structured and queryable** (variant selection by locale, preview, approved-template-id mapping). Reusing `printing`'s opaque-blob storage would force the server to parse a blob it is designed never to parse — a direct contradiction.
- A **logic-less** renderer (vs. a full template engine) eliminates server-side template-injection / SSTI risk: business users author content, and only a fixed variable context is interpolated. It is also trivially portable to the mobile preview if ever needed.
- Structured columns make multi-language selection (FR-003), preview with missing-placeholder detection (FR-006), and WhatsApp parameter mapping (FR-005) straightforward, and they sync cleanly over the `/sync` contract.

**What is shared with `printing`**: only the *pattern*, not the storage — both are workspace-scoped `OwnableBaseDomain` templates exposed over offline-sync. We mirror `printing`'s sync controller shape, not its opaque-blob column.

**Renderer choice**: a minimal Mustache-style interpolator. Prefer the tiny, well-known JMustache (`com.github.spullara.mustache.java`) restricted to variable interpolation, or a ~30-line in-house regex resolver if we want zero new dependencies. Either is acceptable; pick one in Phase A and add to the version catalog. Missing-variable behavior: render empty **and** collect the missing key into a warnings list (surfaced by preview; for a real send a missing *required* variable fails the render with a typed exception rather than sending a blank).

**Alternatives rejected**:
- *Reuse `printing` storage / share one template table* — rejected: opaque-blob, app-rendered model contradicts server-side email rendering and structured querying.
- *Full template engine (Thymeleaf/Pebble/Freemarker) with logic* — rejected: SSTI surface, heavier, and unnecessary — we only need variable substitution.
- *Store only one rendered body, no plain-text* — rejected: hurts deliverability; most providers want multipart, and FR-002a calls for a plain-text alternative.

---

## D3. Bridge from `communication` to `notification` (dispatch + status feedback)

**Decision**: Add a public `NotificationDispatchService` interface to `notification` that accepts a **structured enqueue request** — `(channel, recipientAddress, subject?, body, textBody?, title?, dataPayload?, providerTemplateId?, params?, category, sourceModule, sourceRef)` — and returns the `notification_queue` uid. `communication` injects it via `ObjectProvider` (mirroring how `notification` already optionally injects `DevicePushTokenPort`). On a terminal status change, `notification` publishes a Spring `NotificationDeliveryUpdatedEvent(sourceModule, sourceRef, status, providerMessageId, error)`; `communication` listens with `@EventListener @Async` and updates the matching `CommunicationLog` (and, on hard bounce, the suppression list).

**Schema touch to `notification_queue`**: add `subject`, `source_module`, `source_ref` columns (the `channel` enum already includes `EMAIL`/`WHATSAPP`). New Flyway version under both vendors.

**Rationale**: This keeps `notification` the single owner of providers/queue/status and `communication` the owner of business state, with a clean event-based feedback loop — the same in-process `ApplicationEvent` pattern the `event` module already relies on. `ObjectProvider` keeps `communication` unit-testable without a running `notification`.

**Alternatives rejected**: (a) `communication` calls providers directly — rejected: duplicates queue/retry, violates D1. (b) `communication` polls `notification_queue` — rejected: cross-module repository access is banned (Principle IX) and polling is wasteful vs. an event.

---

## D4. Transactional trigger wiring

**Decision**: A `TransactionalEventListener` in `communication` subscribes with `@EventListener @Async` to the existing `event`-module domain events (`InvoiceCreatedEvent`, `OrderCreatedEvent`, payment events), exactly as `WorkspaceEventListener` does. Each event type maps (via a small workspace-configurable binding) to a template code + channel set; the listener resolves the customer (audience = the event's entity), renders, and dispatches. `communication` depends on `:event` for the event classes.

**Rationale**: These events already extend `BaseEntityEvent : ApplicationEvent` and carry `workspaceId`, `entityId`, `userId`, plus useful fields (`InvoiceCreatedEvent` has `invoiceNumber`, `customerName`, `totalAmount`) — enough to seed the template context, with `entityId` available to fetch more via a public service if needed. In-process Spring events are synchronous-publish/async-handle, so no broker dependency is added for P1.

**Alternatives rejected**: (a) Each domain module calls `communication` directly — rejected: couples `invoice`/`order` to `communication`. (b) Consume the Kafka `workspace-events` topic — rejected: heavier than needed for P1; revisit only if cross-instance fan-out demands it.

---

## D5. Recurrence engine — business-timezone correctness & at-most-once

**Decision**: A `@Scheduled(fixedDelay≈60s)` `ScheduleSweeper` (same pattern as `NotificationService`'s existing scheduled poller). Each `CommunicationSchedule` stores `next_run_at` as a **UTC `Instant`** computed by `RecurrenceCalculator`, which takes the schedule's `frequency/interval/day_of_week/day_of_month/time_of_day` and the **workspace business timezone** (via a `BusinessTimezonePort`) and converts the next wall-clock occurrence to an `Instant`. The sweeper selects rows where `next_run_at <= now()` and `paused = false`, **claims** each via a conditional update (optimistic `version`), materializes a `CommunicationRequest`, then advances `next_run_at`/`last_occurrence_key`. **At-most-once** is guaranteed by a unique constraint on a `communication_occurrence` ledger keyed `(schedule_uid, occurrence_key)` — even if two instances race, the second insert fails and is skipped.

Month-overflow (FR-020): "day 31" on a short month clamps to the month's last day. End-date (FR-021): no `next_run_at` advanced past `end_date`. Pause (FR-022): `paused = true` excludes the row.

**Rationale**: The CMP locale notes flag exactly this trap — bucketing time with the device/server zone lands on the wrong day/month; recurrence must use `TimeZone.of(businessTz)`. Storing `next_run_at` as UTC keeps Principle I intact while the *computation* honors the business zone. The occurrence ledger (not just the claim) is the true idempotency guard, surviving restarts and overlap.

**Alternatives rejected**: (a) Quartz/`ShedLock` — rejected: not currently used; `@Scheduled` + a DB claim + unique ledger is sufficient at this scale and matches existing code. (b) Compute occurrences in server zone — rejected: violates FR-018. (c) Rely on the claim alone for idempotency — rejected: a crash between claim and send could double-fire; the unique ledger closes that gap.

---

## D6. Audience resolution (cross-module to `customer`)

**Decision**: Define a `CustomerAudiencePort` in `communication/port` and consume `customer` through it: `resolve(audienceType, ref) → List<Recipient(customerUid, email?, phone?, pushTokens?, locale?)>` for the three audience kinds (single, explicit list, customer-group segment). Implement the port as an adapter backed by `customer`'s public service interfaces; inject via `ObjectProvider` for test isolation. Audience is **resolved at send time** (FR-013) so group membership changes are reflected.

**Rationale**: Principle IX — cross-module access only via public service interfaces, never foreign repositories. A port keeps `communication` decoupled and testable, mirroring `notification`'s `DevicePushTokenPort` approach. (If `customer` lacks a suitable read interface, add a minimal one there — `communication` must not reach into `customer` repositories.)

**Alternatives rejected**: direct `customer` repository injection — rejected by Principle IX.

---

## D7. Offline-sync surface (which resources, and how)

**Decision**:
- **Templates** → **aggregate-grained `/sync`**, modeled on `form` (spec 011): one feed `GET/POST /communication/v1/templates/sync` carries a `MessageTemplate` aggregate (uid = template) bundling its `TemplateVariant`s; **delete-by-absence** for variants; **`base_version` optimistic concurrency** with re-pull/retry. Templates are edited as a unit (header + variants), so the aggregate model fits.
- **Schedules**, **Campaigns**, **Preferences** → **standard `/sync` contract** (UID-keyed bulk upsert, in-band soft-delete, pull feed includes inactive). Each is an independent row authored on-device.
- **Logs** → **pull-only** feed `GET /communication/v1/logs/sync` (server-authored; the POST is rejected/no-op) so the app can show delivery status offline.

**Rationale**: Matches the canonical contract (`docs/guides/offline-sync-contract.md`) so the app's generic `CentralSyncService` + one `SyncDelegate` per entity drives them unchanged. Templates' header+children editing maps to the proven `form` aggregate pattern; the rest are flat rows like `printing`/`customer`.

**Alternatives rejected**: flat per-variant sync for templates — rejected: variants have no independent lifecycle and need delete-by-absence, exactly what the aggregate model provides.

---

## D8. Consent, quiet hours, throttling, suppression

**Decision**:
- `CommunicationPreference` — unique `(customer_uid, channel, category)`, default **transactional = allowed**; promotional defaults to allowed-unless-opted-out (opt-out model) configurable.
- `CommunicationSuppression` — address-level list `(channel, address, reason: HARD_BOUNCE|COMPLAINT|UNSUBSCRIBE)`; blocks all future sends to that address (transactional included for hard bounces, per FR-031).
- `CommunicationConfig` — one row/workspace: `quiet_hours_start/end` (wall-clock in business tz, may span midnight), `default_throttle_per_minute`, `promotional_footer_html`, `unsubscribe_base_url`.
- `ConsentGate` runs for **promotional** sends only (transactional bypasses opt-out + quiet hours, FR-016): excludes opted-out recipients (skip reason recorded), defers quiet-hours sends, and the `Throttler` paces dispatch to `throttle_per_minute`.

**Rationale**: Satisfies FR-024/025/026/029/030/031. Quiet-hours evaluated in business tz with explicit midnight-spanning handling (edge case). Hard bounces suppress even transactional to avoid indefinite retries.

**Alternatives rejected**: per-message opt-out flags on the customer entity — rejected: cross-cuts the `customer` bounded context; consent is a communication concern.

---

## D9. Provider delivery webhooks

**Decision**: `notification` owns provider webhooks at `POST /notification/v1/webhooks/{provider}` (SES/SNS bounce+complaint, WhatsApp status callbacks). The handler maps the provider payload to a `notification_queue` status update (no status regression — FR-010) and republishes `NotificationDeliveryUpdatedEvent`; `communication` reacts (update log; bounce/complaint → suppression). Endpoints are unauthenticated by `X-Workspace-ID` and instead verified by provider signature and correlated via `source_ref`.

**Rationale**: Providers live in `notification`, so their callbacks belong there; `communication` stays provider-agnostic and only consumes normalized delivery events.

**Alternatives rejected**: webhooks landing in `communication` — rejected: would leak provider specifics across the boundary.

---

## D10. Mobile module (high level, separate repo)

**Decision**: A new `feature/communication` KMP module in `ampairs-app` following the standard stack — Metro DI with `@SingleIn(WorkspaceScope)` Room DB + `WorkspaceClosableRegistry`, an offline-sync `CommunicationSyncDelegate` per resource, a Navigation3 entry provider, and `ModuleRegistry` mapping `"communication-management" → Route.Communication`. UI: compose/manage templates (incl. HTML body editing as raw HTML + preview rendered server-side), schedules, campaigns, and a delivery-status list. **Sending stays server-side**; the app only authors and observes.

**Rationale**: Matches `/metro-di` and `/offline-sync` skills and the existing feature-module layout. Detailed plan/tasks are a separate `ampairs-app` PR; here we only freeze the `/sync` contract + DTO shapes so both repos align.

**Open follow-ups** (non-blocking): exact WhatsApp provider vendor (Meta Cloud API vs. MSG91/Twilio) and email transport (SMTP vs. SES) are config-time choices behind the provider interface; both are wired in Phase A/C and selected via `CommunicationProperties`/`NotificationProperties`.
