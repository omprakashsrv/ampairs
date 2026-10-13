# Feature Specification: Automated Collections & Dunning

**Feature Branch**: `claude/automated-collections-dunning-14c740` (spec dir `017-automated-collections-dunning`)
**Created**: 2026-06-28
**Status**: Draft
**Input**: User description: "Automated collections & dunning. On top of the party payment ledger, let a workspace owner stop chasing overdue customers by hand. The owner defines, per aging bucket (0-30 / 31-60 / 61-90 / 90+ days overdue), an escalating ladder of reminders — a gentle nudge, then firm, then a final notice, then handover — sent automatically across channels (WhatsApp / SMS / email / push) using per-workspace, branded message templates that quote the customer's live outstanding amount and carry a one-tap pay-now link. The system reminds at most once per step, respects opt-out, quiet hours and a per-week cap, records every send (and every suppression, with a reason), and stops a customer's reminder cycle the moment they pay."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configure an escalating reminder ladder per aging bucket (Priority: P1)

A workspace owner sets up, once, how overdue customers should be chased: for each aging bucket (0-30,
31-60, 61-90, 90+ days overdue) they define a ladder of reminder steps — when each step fires (days
overdue), through which channel, at what escalation tone (gentle → firm → final → handover), and which
message template it uses. Quiet hours, a per-week cap, and the default channel order are set here too.

**Why this priority**: Nothing fires until the owner has expressed *who gets reminded, when, and how*.
This configuration is the foundation the engine reads on every run, and it is the one thing only the
owner can decide. It is independently valuable even before a single reminder is sent — it captures the
business's collection policy in one place.

**Independent Test**: With a fresh workspace, define a policy with at least one step in the 0-30 bucket
(e.g. "gentle email at 7 days overdue") and one in the 90+ bucket ("final WhatsApp at 90 days"); save
it; reopen and confirm the ladder, channels, tones, quiet hours and weekly cap persist exactly as
entered, and that the policy can be edited offline and later synced.

**Acceptance Scenarios**:

1. **Given** a new workspace with no reminder policy, **When** the owner creates a policy with a
   "gentle email at 7 days" step and a "firm SMS at 40 days" step, **Then** the policy is saved with
   both steps mapped to their buckets (0-30 and 31-60) and shown back on reopen.
2. **Given** an existing policy, **When** the owner changes the 90+ step's channel from SMS to WhatsApp
   and its tone to "final notice", **Then** the change is persisted and applies to the next evaluator run.
3. **Given** the owner is offline, **When** they edit a step's day-offset and reopen the editor, **Then**
   the edit is retained locally and is reflected on the server after the device next syncs.
4. **Given** a policy with overlapping steps, **When** the owner saves two steps in the same bucket with
   the same day-offset and channel, **Then** the system rejects or flags the duplicate so the ladder
   stays unambiguous.

---

### User Story 2 - Overdue customers are reminded automatically, at most once per step (Priority: P1)

Each day, without any manual action, the system finds every party with an overdue balance, works out
which reminder step is due based on how overdue their oldest bill is, and sends that reminder — quoting
the customer's current outstanding amount and a one-tap pay-now link. The same step never fires twice
for the same customer in the same period, even if the daily run is repeated or the service restarts.

**Why this priority**: This is the entire point of the feature — turning a configured ladder into
reminders that actually go out, reliably and without double-sending. Combined with Story 1 it is the
minimum viable product: a workspace can configure a ladder and watch reminders fire correctly.

**Independent Test**: Seed one party who is 8 days overdue with a known outstanding amount and a policy
whose 0-30 bucket has a step at 7 days; run the daily evaluation; confirm exactly one reminder is
recorded as sent, carrying the live amount and a pay link; run the evaluation again the same day and
confirm no second reminder is sent.

**Acceptance Scenarios**:

1. **Given** a party 8 days overdue and a step "gentle email at 7 days", **When** the daily evaluation
   runs, **Then** exactly one email reminder is sent quoting that party's current outstanding and a
   pay-now link, and a dispatch record marks the step as sent.
2. **Given** the same party and step already reminded today, **When** the daily evaluation runs again
   (or after a restart), **Then** no duplicate reminder is sent.
3. **Given** a party whose outstanding changed (a partial payment landed) between configuration and
   send, **When** the reminder fires, **Then** it quotes the *current* outstanding amount, never a stale
   figure.
4. **Given** a party with nothing overdue, **When** the daily evaluation runs, **Then** no reminder is
   sent to them.
5. **Given** a party who ages from the 31-60 into the 61-90 bucket, **When** the next due step belongs
   to the new bucket, **Then** the reminder escalates to that bucket's step (and tone) rather than
   repeating the earlier one.

---

### User Story 3 - Reminders respect opt-out, quiet hours and a frequency cap (Priority: P2)

Before any reminder goes out, the system honours the customer's right not to be contacted and basic
courtesy: a customer who has opted out (or replied STOP) is never messaged; nothing is sent during the
workspace's quiet hours; and no customer is reminded more times than the per-week cap. Every reminder
that is held back is recorded with the reason, so the owner can see why a customer wasn't chased.

**Why this priority**: Sending at 3am, double-chasing, or messaging someone who opted out destroys
customer trust and can breach messaging regulations. These guardrails are required for the feature to
be safe to switch on, but they layer on top of the core engine (Stories 1-2) rather than blocking it.

**Independent Test**: With a due reminder pending, (a) mark the party opted-out and confirm nothing is
sent and a suppression is recorded with reason "opted out"; (b) set the current time inside quiet hours
and confirm the reminder defers rather than sends; (c) exceed the weekly cap and confirm further
reminders are suppressed with reason "weekly cap".

**Acceptance Scenarios**:

1. **Given** a party who has opted out of reminders, **When** a step is due for them, **Then** no
   message is sent and a suppression is recorded with reason "opted out".
2. **Given** it is inside the workspace's quiet-hours window, **When** a step becomes due, **Then** the
   reminder is held and sent in the next allowed slot, not during quiet hours.
3. **Given** a party has already received the maximum reminders allowed this week, **When** another step
   is due, **Then** it is suppressed with reason "weekly cap" and recorded for the owner to see.
4. **Given** a customer replies STOP to a reminder, **When** the next step would be due, **Then** they
   are treated as opted-out and not contacted.
5. **Given** a party has no contact identifier for a step's channel (e.g. no email), **When** that step
   is due, **Then** the system falls through to the next available channel in the order rather than
   failing the reminder.

---

### User Story 4 - Branded, per-workspace templates with an embedded pay link (Priority: P2)

The owner writes their own reminder messages, per channel and per escalation tone, using placeholders
for the customer's name, amount due, oldest invoice, days overdue and a pay-now link. At send time the
system fills in the live values and a fresh pay link, so each customer gets a personalised, on-brand
message they can act on in one tap.

**Why this priority**: A generic reminder without the business's voice and without a pay path collects
little. Personalised, branded messages with a working pay link are what convert a reminder into a
payment — but they build on the engine and guardrails already in place.

**Independent Test**: Author a template containing the amount and pay-link placeholders; trigger a
reminder for a party with a known outstanding; confirm the delivered message shows that party's name,
their current amount formatted in the workspace currency, and a working pay-now link.

**Acceptance Scenarios**:

1. **Given** a template with `{{party_name}}`, `{{amount_due}}` and `{{pay_link}}`, **When** a reminder
   is sent to a party, **Then** the message shows that party's name, their current outstanding in the
   workspace's currency format, and a one-tap pay link.
2. **Given** the workspace has a pay-link rail enabled, **When** the reminder is generated, **Then** the
   embedded link is generated at send time for the current amount (not pre-generated and possibly stale).
3. **Given** the workspace has no pay-link rail enabled, **When** the reminder is generated, **Then** it
   still sends with a graceful fallback call-to-action (e.g. "pay at counter" / merchant payment detail)
   instead of a broken link.
4. **Given** templates exist for multiple tones, **When** a final-notice step fires, **Then** the
   final-notice template (not the gentle one) is used.

---

### User Story 5 - Escalation to handover, auto-stop on payment, and a visible dispatch history (Priority: P3)

When a customer reaches the most-overdue bucket, the ladder escalates to a final/handover step that
flags them for manual collection. The moment a customer pays, their reminder cycle stops automatically
so they are never chased for a settled bill. Throughout, the owner can see a history of every reminder
sent and every one suppressed, per customer.

**Why this priority**: Closing the loop — handover for the worst cases, auto-stop on payment, and an
audit trail — makes the system trustworthy over time, but the feature already delivers value without
it. It is the natural follow-on once the engine, guardrails and templates exist.

**Independent Test**: Take a party in the 90+ bucket, run evaluation, and confirm a handover step is
recorded/flagged; then record a payment that clears their balance and confirm no further reminders fire;
open the customer's reminder history and confirm both the sent reminders and the handover are listed.

**Acceptance Scenarios**:

1. **Given** a party reaches the 90+ bucket, **When** evaluation runs, **Then** the handover step fires
   (notifying the owner / flagging the party for manual collection) rather than another routine nudge.
2. **Given** a party mid-ladder, **When** they pay and their outstanding reaches zero, **Then** their
   pending reminder cycle stops and no further reminders are sent.
3. **Given** any party, **When** the owner opens their reminder history, **Then** every reminder sent
   (with date, channel, tone) and every suppression (with reason) is listed.
4. **Given** the owner wants to chase a specific customer immediately, **When** they trigger a manual
   "remind now", **Then** a reminder is sent subject to the same opt-out / quiet-hours / cap guardrails.

---

### Edge Cases

- **Repeated / restarted runs**: the daily evaluation re-running, or the service restarting mid-run, must
  never cause a second send of the same step for the same party in the same period.
- **Amount changes between schedule and send**: the amount quoted is always the live outstanding at send
  time; a partial payment that lands first reduces the quoted figure (or stops the reminder if cleared).
- **Customer pays after the message is queued but before delivery**: the cycle stops; no further steps
  fire even if already due.
- **No contact identifier for the chosen channel**: fall through the channel order; if no channel is
  reachable, record a suppression with reason rather than failing silently.
- **Quiet-hours boundary**: a step due just inside quiet hours defers to the next allowed slot; one due
  just outside sends normally — evaluated in the workspace's timezone, not the device/server timezone.
- **Opt-out arriving mid-ladder**: an opt-out (manual or STOP reply) takes effect immediately for all
  subsequent steps.
- **Multiple steps due on the same day**: the ladder resolves to the single appropriate step for the
  party's oldest overdue bucket rather than firing several at once.
- **Per-party delivery failure**: a failure for one customer must not abort the run for the rest.
- **Pay-link rail unavailable**: reminders still send with a fallback call-to-action.
- **Workspace with the feature disabled**: no reminders are evaluated or sent.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST let a workspace define a single active reminder policy consisting of an
  escalation ladder of steps grouped by aging bucket (0-30, 31-60, 61-90, 90+ days overdue).
- **FR-002**: Each reminder step MUST capture: the aging bucket, the day-offset at which it fires, the
  delivery channel (WhatsApp, SMS, email, push), an escalation tone (gentle, firm, final, handover), and
  the message template it uses.
- **FR-003**: The system MUST evaluate overdue parties automatically on a recurring daily schedule
  without any manual trigger.
- **FR-004**: The system MUST determine each party's due step from their *current* aging — read from the
  authoritative payment ledger — and MUST NOT recompute or snapshot outstanding amounts independently.
- **FR-005**: The system MUST send at most one reminder per (party, step, period) and MUST be safe
  against repeated runs and restarts (no double-sends).
- **FR-006**: Every reminder MUST quote the party's live outstanding amount at send time, formatted in
  the workspace's business currency/locale.
- **FR-007**: The system MUST embed a one-tap pay-now link for the party's outstanding when a pay-link
  rail is enabled for the workspace, generated at send time.
- **FR-008**: When no pay-link rail is enabled, the system MUST still send the reminder with a graceful
  fallback call-to-action instead of a broken or missing link.
- **FR-009**: The system MUST suppress reminders to any party who has opted out (including via an inbound
  STOP/unsubscribe), and MUST never contact an opted-out party.
- **FR-010**: The system MUST NOT send reminders during the workspace's configured quiet hours,
  evaluated in the workspace timezone, deferring due reminders to the next allowed slot.
- **FR-011**: The system MUST enforce a per-workspace cap on the number of reminders sent to a single
  party per week.
- **FR-012**: The system MUST record every reminder send and every suppression, the latter with a reason
  (e.g. opted out, quiet hours, weekly cap, no reachable channel), so nothing is silently dropped.
- **FR-013**: The system MUST let the owner author per-workspace message templates, per channel and per
  escalation tone, with placeholders for at least party name, amount due, oldest overdue invoice, days
  overdue and pay link.
- **FR-014**: The system MUST escalate a party who reaches the most-overdue bucket to a final/handover
  step that flags them for manual collection (and/or notifies the owner) rather than repeating routine
  nudges.
- **FR-015**: The system MUST stop a party's reminder cycle automatically as soon as their outstanding
  is settled.
- **FR-016**: When a step's channel has no contact identifier for the party, the system MUST fall through
  the configured channel order rather than failing the reminder.
- **FR-017**: The owner MUST be able to view, per party, a history of reminders sent and suppressed
  (with date, channel, tone, and suppression reason).
- **FR-018**: The owner MUST be able to manually trigger an immediate reminder ("remind now") for a
  party, subject to the same opt-out, quiet-hours and cap guardrails.
- **FR-019**: The owner MUST be able to toggle a party's opt-out preference.
- **FR-020**: Reminder policy, steps, templates and per-party opt-out preferences MUST be editable
  offline on the mobile app and reconcile with the server when connectivity returns; dispatch history is
  read-only on the device.
- **FR-021**: A per-party delivery failure MUST NOT abort the evaluation run for other parties.
- **FR-022**: All reminder configuration and history MUST be isolated per workspace.
- **FR-023**: A workspace MUST be able to enable or disable the whole automated-reminder feature; when
  disabled, no evaluation or sending occurs.

### Key Entities *(include if feature involves data)*

- **Reminder Policy**: The workspace's collection policy — one active per workspace. Groups the
  escalation ladder and references workspace-level reminder settings (quiet hours, weekly cap, default
  channel order).
- **Reminder Step**: A single rung of the ladder — its aging bucket, day-offset, channel, escalation
  tone, and the template to use.
- **Reminder Template**: A per-workspace, per-channel, per-tone message body with placeholders
  (party name, amount due, oldest invoice, days overdue, pay link). Branded and editable by the owner.
- **Reminder Dispatch**: The record that a given step fired (or was suppressed) for a given party in a
  given period — carries status (scheduled, sent, delivered, failed, suppressed), suppression reason,
  channel, and send time. This record is what guarantees at-most-once sending and forms the audit
  history.
- **Dunning Preference**: A party-level opt-out / suppressed-channels record, flipped by a manual toggle
  or an inbound STOP/unsubscribe.
- **Party Outstanding / Aging** *(read-only, owned by the payment ledger)*: The authoritative source of
  each party's overdue amount and aging bucket; dunning reads it and never recomputes it.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An owner can configure a complete per-bucket reminder ladder (all four buckets) in under
  10 minutes without assistance.
- **SC-002**: 100% of reminders sent quote an outstanding amount that matches the party's live ledger
  balance at send time (zero stale-amount reminders).
- **SC-003**: Zero duplicate reminders: across repeated daily runs and service restarts, no (party,
  step, period) is ever sent more than once.
- **SC-004**: 100% of reminders to opted-out parties are suppressed, and zero reminders are delivered
  during a workspace's quiet hours.
- **SC-005**: Every reminder attempt is accounted for — every send and every suppression is recorded
  with a reason; the owner can see, per party, why a reminder did or did not go out.
- **SC-006**: Once a party settles their balance, no further reminder is sent to them for that cycle
  (0% reminders to fully-paid parties).
- **SC-007**: The daily evaluation completes within its scheduled window for a workspace with thousands
  of overdue parties.
- **SC-008**: After enabling the feature, the share of overdue parties that receive at least one
  reminder without manual effort reaches ~100% of those eligible (i.e. manual chasing for routine
  reminders drops to near zero).
- **SC-009**: Reminders carrying a one-tap pay link measurably increase the rate at which reminded
  parties pay versus no-link reminders (tracked as sent → paid conversion by bucket/template).

## Assumptions

- **Authoritative dues**: Outstanding amounts and aging buckets come from the existing party payment
  ledger; dunning is orchestration only and posts no money and recomputes no balances.
- **Aging buckets**: The 0-30 / 31-60 / 61-90 / 90+ day buckets follow the buckets the ledger already
  exposes; the bucket that drives a reminder is the party's *oldest* overdue bucket at evaluation time.
- **Delivery**: Messages are delivered through the workspace's existing notification channels; dunning
  decides *what to send to whom and when*, not the transport.
- **Pay link**: The embedded pay-now link reuses the workspace's existing collection/pay-link rail and
  is optional — its absence degrades gracefully to a fallback call-to-action.
- **Timezone**: Quiet hours and "today" are evaluated in the workspace's configured timezone.
- **Quiet-hours default**: A sensible default window (e.g. 9pm-8am workspace-local) applies until the
  owner changes it.
- **Where it runs**: The firing engine runs server-side (it needs current aging and live messaging
  providers); the mobile app provides offline-editable configuration and read-only history.
- **Opt-out / STOP**: Inbound STOP handling depends on the messaging provider's unsubscribe signal;
  where unavailable, opt-out is driven by the manual toggle.
- **One active policy per workspace**: A workspace runs a single active ladder at a time.

## Out of Scope

- Posting payments or any ledger entries (owned by the payment & collection module).
- The pay-link / collection rail itself (owned by the UPI collection feature) — dunning only embeds a
  link it requests.
- The underlying message-delivery transport and provider integrations (owned by the notification module).
- Aging computation and the outstanding/statement views (owned by the payment ledger).
- Multi-policy A/B testing, predictive "best time to send" optimisation, and customer credit scoring.
- Legal/agency debt-collection workflows beyond flagging a party for manual handover.
- A web (Angular) console for dunning (tracked as a follow-up).
