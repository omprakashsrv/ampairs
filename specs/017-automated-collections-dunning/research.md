# Phase 0 Research — Automated Collections & Dunning

All Technical Context unknowns are resolved below. Each item: **Decision · Rationale · Alternatives
considered**. These supersede the inline assumptions in `spec.md` with concrete technical choices.
The feature is **orchestration over existing infrastructure**: it reads outstanding/aging from the
spec-013 payment ledger, embeds a pay-now link from feature 016, and delivers reminders through the
existing `notification` module. It owns reminder *policy and scheduling*, not money and not delivery.

---

## R1. Module boundary — new `dunning` module vs extend `payment`/`notification`

- **Decision**: A **new backend bounded context `dunning`** owns reminder schedules, the escalation
  ladder, per-workspace templates, opt-out and quiet-hours, and the per-party reminder run-state. It is
  **pure orchestration**: it reads outstanding/aging via `payment`'s public `OutstandingService` /
  `AgingService`, asks `collection` (feature 016) for a pay-now link, and delivers via
  `notification`'s public `NotificationService`. It never posts ledger entries and never sends a
  channel message directly.
- **Rationale**: Dunning policy (who gets reminded, when, via which ladder) is a distinct concern from
  the ledger and from message delivery — Principle IX. Keeping it separate means the ledger stays the
  source of truth, `notification` stays the delivery engine, and dunning just decides *what to send to
  whom and when*. This is the cleanest mapping of "mostly orchestration over existing ledger +
  notification."
- **Alternatives considered**: Add a scheduler into `payment` (rejected — couples reminder cadence to
  the ledger context). Add templates/schedules into `notification` (rejected — `notification` is a
  channel-delivery utility, not a policy engine; it shouldn't know about aging buckets). A cron of
  ad-hoc queries (rejected — no auditable run-state, no opt-out/quiet-hours guardrails).

## R2. Where aging/outstanding comes from (single source of truth)

- **Decision**: Dunning **never recomputes** dues. The scheduler calls `payment`'s `AgingService`
  (`GET /payment/v1/aging` logic) and `OutstandingService` (`/parties/{uid}/open-bills`) to get each
  party's aging-bucketed outstanding (0-30 / 31-60 / 61-90 / 90+ — the buckets `payment` already
  exposes via the `aging_buckets` setting). The reminder that fires is keyed to the party's **oldest
  overdue bucket** at evaluation time.
- **Rationale**: The payment ledger is authoritative (spec 013 R3/R8 — derived, cached, recomputable).
  Re-deriving aging in dunning would drift from the ledger and could remind for already-paid bills.
  Reusing `AgingService` guarantees a reminder reflects the same number the user sees on the dashboard.
- **Alternatives considered**: Snapshot dues into a dunning table (rejected — staleness; a payment that
  landed via feature 016 must immediately stop reminders). Query invoices directly (rejected — ignores
  on-account receipts/advances that the ledger nets; crosses module boundary).

## R3. Schedule model — per aging bucket + escalation ladder

- **Decision**: A `ReminderPolicy` (per workspace, one active) defines, per **aging bucket**, a set of
  `ReminderStep`s: `dayOffset` (days into the bucket / days overdue), `channel`
  (WhatsApp/SMS/email/push), `templateRef`, and `escalationLevel` (GENTLE → FIRM → FINAL → HANDOVER).
  Evaluation is **idempotent per (party, step, period)**: a `ReminderDispatch` row records that step S
  fired for party P on date D so the daily evaluator never double-sends. The ladder advances as the
  oldest bucket ages; reaching `90+` triggers the FINAL/HANDOVER tier.
- **Rationale**: Aging-bucket-driven escalation is exactly how SMB collection works (gentle nudge at 0-30,
  firm at 31-60, final notice at 90+). Modelling steps as data (not code) lets each workspace tune cadence
  without a release. The `ReminderDispatch` dedupe key is the same idempotency discipline used across
  features 015/016 — the evaluator can run repeatedly (or after a restart) without spamming.
- **Alternatives considered**: A single fixed reminder cadence (rejected — the brief asks for per-bucket
  ladders). Cron expressions per workspace (rejected — too low-level for a business owner; bucket+offset
  is the right abstraction). Storing the schedule in code (rejected — per-workspace templates require
  data).

## R4. Idempotency & the daily evaluator

- **Decision**: A single `@Scheduled` **daily evaluator** (per workspace, run in tenant context) walks
  parties with overdue dues, computes the due step(s) for each, and **enqueues** a `ReminderDispatch`
  only if no dispatch exists for that `(partyUid, stepKey, evaluationWindow)`. A unique constraint on
  `(owner_id, party_uid, step_key, period_key)` backstops the in-memory check. Sending is then handed to
  `notification` asynchronously; the dispatch row tracks `SCHEDULED → SENT → DELIVERED | FAILED |
  SUPPRESSED`.
- **Rationale**: A reminder system that double-sends destroys trust instantly. A persisted dispatch
  ledger keyed by step+period makes "send at most once per step per period" provable and restart-safe,
  mirroring spec 013's deterministic-uid approach. Decoupling evaluation from delivery lets delivery
  failures retry via `notification` without re-evaluating policy.
- **Alternatives considered**: Fire-and-forget at evaluation (rejected — no record, double-sends on
  re-run). Let `notification` dedupe (rejected — it has no notion of dunning steps/periods).

## R5. Opt-out, quiet hours & channel consent

- **Decision**: Three suppression gates applied **before** enqueue: (1) **opt-out** — a party-level
  `DunningPreference(optedOut, suppressedChannels)`; an inbound STOP/unsubscribe (via a webhook from
  the messaging provider, or a manual toggle) flips it. (2) **Quiet hours** — a per-workspace window
  (default 9pm–8am in the workspace timezone, spec 002) during which nothing sends; due reminders defer
  to the next allowed slot. (3) **Channel availability** — skip a channel the party has no identifier for
  (no email → fall through the ladder's channel order). Suppressed dispatches are recorded as
  `SUPPRESSED` with a reason (audit), not silently dropped.
- **Rationale**: Regulatory/TRAI and basic courtesy require opt-out and quiet hours; recording
  suppressions (rather than skipping silently) gives the owner visibility into why a party wasn't
  reminded. Timezone correctness reuses spec 002's per-workspace timezone — sending at the wrong local
  hour is a known foot-gun.
- **Alternatives considered**: Global opt-out only (rejected — per-channel consent differs). Ignore quiet
  hours (rejected — sending at 3am is unacceptable and may breach DLT norms). Drop suppressed silently
  (rejected — no audit; owner can't tell reminders aren't going out).

## R6. Per-workspace templates & the embedded pay link

- **Decision**: `ReminderTemplate` (per workspace, per channel, per escalation level) holds a body with
  placeholders (`{{party_name}}`, `{{amount_due}}`, `{{oldest_invoice_no}}`, `{{days_overdue}}`,
  `{{pay_link}}`). At dispatch, the orchestrator renders the template, requests a **pay-now link from
  `collection` (feature 016)** for the party's outstanding, substitutes `{{pay_link}}`, and hands the
  final message to `notification`. Templates fall back to workspace-language defaults (spec 025 i18n
  hook). Money is rendered by the workspace business locale.
- **Rationale**: Personalised, branded reminders with a one-tap pay link are the entire point — a
  reminder without a pay path collects nothing. Generating the link at send time (not schedule time)
  ensures the amount is current and the link is fresh/unexpired. Reusing 016's link keeps a single
  collection rail.
- **Alternatives considered**: Hardcoded English templates (rejected — per-workspace branding/language;
  the brief asks for per-workspace templates). Pre-generate links at schedule time (rejected — amount may
  change before send; link could expire).

## R7. Money & locale rendering

- **Decision**: Amounts in reminder bodies come straight from `payment` (`BigDecimal` outstanding) and
  are formatted with the **workspace business currency/locale** (the `formatMoney` convention) — the
  reminder is display-only text; no money math happens in `dunning`.
- **Rationale**: Dunning does no arithmetic — it quotes the ledger's number. Locale-correct formatting
  (₹ grouping, etc.) matters for a customer-facing message; the workspace business locale is already the
  app/web convention.
- **Alternatives considered**: Recompute/round in dunning (rejected — no reason to; risks disagreeing
  with the ledger). Hardcode `₹` (rejected — multi-currency workspaces).

## R8. Offline behaviour & where the engine runs

- **Decision**: The dunning **engine runs entirely backend-side** (scheduler + ledger reads + provider
  sends) — it is **not** an offline-capable feature. The mobile app surfaces dunning as **pull-only
  config + history**: view/edit the `ReminderPolicy` and `ReminderTemplate`s (these *are* offline-editable
  synced settings on the canonical `/sync` contract, since they're workspace config, not real-money
  events), set a party's opt-out, and view the `ReminderDispatch` history (pull-only). Actual reminder
  *firing* never happens on-device.
- **Rationale**: Reminders depend on current aging + live messaging providers — both server-side and
  connectivity-bound. The policy/templates/opt-out, however, are plain workspace config that fits the
  offline-first sync model perfectly (edit a template on the train, it syncs). Separating editable config
  (synced) from the firing engine (server) is the honest split.
- **Alternatives considered**: Run reminders from the device (rejected — no reliable background
  scheduling across platforms; aging must be server-current; can't send from a phone at scale).
  Make everything pull-only including config (rejected — losing offline template editing is a needless
  downgrade).

## R9. Integration with feature 016 (collection) availability

- **Decision**: The embedded pay link is **optional and graceful**: if `collection` (016) is not
  installed/enabled for the workspace, the reminder still sends with a fallback CTA (the merchant VPA /
  "pay at counter" text) and no `{{pay_link}}`. Dunning depends on `collection` through a public
  interface guarded by `ObjectProvider`/optional bean, exactly as `notification` optionally resolves the
  push-token port.
- **Rationale**: Dunning must work for workspaces that haven't adopted UPI rails yet; a hard dependency
  would block the feature. Optional resolution matches an existing codebase pattern
  (`NotificationService`'s `ObjectProvider<DevicePushTokenPort>`).
- **Alternatives considered**: Hard-require feature 016 (rejected — blocks dunning for non-UPI
  workspaces). Inline a second link generator (rejected — duplicates 016).

## R10. Settings & enablement

- **Decision**: Reuse the `setting` module via a `DunningSettingDefinitions` provider gated by an
  installed `dunning` module: `dunning_enabled`, `quiet_hours_start`/`quiet_hours_end`,
  `default_channel_order`, `max_reminders_per_party_per_week`, `handover_bucket` (e.g. `90+`),
  `evaluator_run_hour`.
- **Rationale**: Matches spec 013/016; no new settings infra. A per-week cap is a cheap guard against
  accidental spam from misconfigured steps.
- **Alternatives considered**: Hardcode quiet hours/cadence (rejected — workspaces differ; the brief
  requires per-workspace control).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Module placement | New `dunning` orchestration context (R1) |
| Aging source | `payment` `AgingService`/`OutstandingService` — never recompute (R2) |
| Schedule model | `ReminderPolicy` → per-bucket `ReminderStep`s + escalation ladder (R3) |
| Idempotency | Daily evaluator + `ReminderDispatch` unique on (party, step, period) (R4) |
| Opt-out / quiet hours | Three pre-enqueue gates; suppressions recorded with reason (R5) |
| Templates / pay link | Per-workspace `ReminderTemplate`; link from `collection` at send time (R6) |
| Money rendering | Quote ledger `BigDecimal`, format by workspace locale (R7) |
| Offline / where it runs | Backend engine; mobile = synced config + pull-only history (R8) |
| Collection (016) dependency | Optional via `ObjectProvider`; graceful fallback CTA (R9) |
| Settings | `StoreSetting` + `DunningSettingDefinitions` (R10) |
