# Phase 1 Data Model — Generic Communication Module

All entities are in the `communication` module, package `com.ampairs.communication.domain.model`, extend **`OwnableBaseDomain`** (tenant-scoped via `@TenantId ownerId`, `uid`, `active`, `createdAt`/`updatedAt` as `Instant`), and serialize **snake_case**. Columns below omit the inherited `OwnableBaseDomain` fields except where behavior matters. Migrations are written for **both** `db/migration/mysql/` and `db/migration/postgresql/` (`TIMESTAMP` vs `TIMESTAMPTZ`).

UID prefixes (client-authored on mobile; server prefix for server-minted rows): `CTPL` template, `CTPV` variant, `CREQ` request, `CLOG` log, `CSCH` schedule, `CCMP` campaign, `CPRF` preference, `CSUP` suppression, `CCFG` config, `CETB` event-template binding, `CUSG` usage.

---

## Enums (`domain/enums`)

- **Channel**: `EMAIL`, `SMS`, `WHATSAPP`, `PUSH` *(aligns with notification `NotificationChannel`)*
- **MessageCategory**: `TRANSACTIONAL`, `PROMOTIONAL`
- **TriggerType**: `EVENT`, `MANUAL`, `SCHEDULE`, `CAMPAIGN`
- **AudienceType**: `SINGLE`, `LIST`, `SEGMENT`
- **DeliveryStatus**: `QUEUED`, `SENT`, `DELIVERED`, `READ`, `FAILED`, `SKIPPED`, `EXHAUSTED` *(monotonic; never regresses)*
- **SkipReason**: `NO_ADDRESS`, `OPTED_OUT`, `SUPPRESSED`, `QUIET_HOURS_EXPIRED`, `NO_VARIANT`, `NO_CREDENTIAL`
- **BillingMode**: `CLIENT_OWN` *(sent on the workspace's own credential — client's provider cost)*, `PLATFORM` *(sent on the shared platform credential — billable to the client)*
- **CredentialStatus**: `UNVERIFIED`, `VALID`, `INVALID`, `EXPIRED`
- **Frequency**: `DAILY`, `WEEKLY`, `MONTHLY`
- **CampaignStatus**: `DRAFT`, `SCHEDULED`, `RUNNING`, `PAUSED`, `DONE`
- **SuppressionReason**: `HARD_BOUNCE`, `COMPLAINT`, `UNSUBSCRIBE`

---

## 1. `message_template` (MessageTemplate) — aggregate root

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | client-authored id (`CTPL…`); unique |
| `code` | varchar | workspace-unique business key (e.g. `INVOICE_READY`) |
| `name` | varchar | display name |
| `category` | enum(MessageCategory) | TRANSACTIONAL / PROMOTIONAL |
| `default_locale` | varchar | fallback locale (e.g. `en`) |
| `description` | text? | optional |
| `base_version` | int | optimistic-concurrency counter for aggregate `/sync` (mirrors `form`) |
| `active` | bool | soft-delete |

- `@NamedEntityGraph("MessageTemplate.withVariants")` fetching `variants`.
- Unique: `(owner_id, code)`.
- Aggregate on `/sync`: pushed with its variants; LWW by `updatedAt`, conflict by `base_version`.

## 2. `message_template_variant` (TemplateVariant) — child of template

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | `CTPV…` |
| `template_uid` | varchar | FK → `message_template.uid` |
| `channel` | enum(Channel) | EMAIL / SMS / WHATSAPP / PUSH |
| `locale` | varchar | e.g. `en`, `hi` |
| `subject` | varchar? | **required for EMAIL**, null otherwise |
| `html_body` | text? | **EMAIL**: rich HTML with `{{vars}}`. null for non-email |
| `text_body` | text? | plain-text body (SMS/WhatsApp/push) OR email plain-text alternative; derived from `html_body` if blank for email |
| `provider_template_id` | varchar? | WhatsApp/SMS provider-approved template id (FR-005) |
| `provider_params_json` | text? | ordered param mapping for the approved template |
| `active` | bool | delete-by-absence within the aggregate |

- Unique: `(template_uid, channel, locale)`.
- Validation: EMAIL ⇒ `subject` + `html_body` non-blank; WHATSAPP with `provider_template_id` ⇒ `provider_params_json` shape matches.

## 3. `communication_request` (CommunicationRequest) — one logical send

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | `CREQ…` |
| `template_uid` | varchar | FK → template (resolved at creation) |
| `trigger_type` | enum(TriggerType) | EVENT / MANUAL / SCHEDULE / CAMPAIGN |
| `source_ref` | varchar? | schedule uid / campaign uid / event id |
| `channels` | varchar | CSV of Channel |
| `audience_type` | enum(AudienceType) | |
| `audience_ref` | varchar? | customer uid / list id / group uid |
| `variables_json` | text | resolved context for `{{vars}}` |
| `dedup_key` | varchar? | unique idempotency key (event id, or `schedule_uid:occurrence_key`) |
| `status` | varchar | overall rollup (derived/cached) |

- Unique: `(owner_id, dedup_key)` when `dedup_key` present — blocks double-trigger (SC-006).
- Fans out into `communication_log` rows.

## 4. `communication_log` (CommunicationLog) — per recipient × channel delivery

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | `CLOG…` |
| `request_uid` | varchar | FK → request |
| `customer_uid` | varchar? | resolved recipient (null for ad-hoc address) |
| `channel` | enum(Channel) | |
| `recipient_address` | varchar | email / phone / token |
| `category` | enum(MessageCategory) | copied from template |
| `status` | enum(DeliveryStatus) | monotonic |
| `skip_reason` | enum(SkipReason)? | when SKIPPED |
| `notification_uid` | varchar? | `notification_queue.uid` returned by dispatch |
| `provider_message_id` | varchar? | from provider/webhook |
| `error_message` | text? | terminal failure reason |
| `occurrence_key` | varchar? | schedule occurrence (idempotency, denormalized) |
| `credential_uid` | varchar? | which `workspace_channel_credential` was used (D11; null when none/skipped) |
| `provider_account_ref` | varchar? | the sender used (WhatsApp number / from-domain / SMS sender id) |
| `billing_mode` | enum(BillingMode)? | CLIENT_OWN / PLATFORM (FR-040) |
| `sent_at` / `delivered_at` | Instant? | |

> New `SkipReason` value `NO_CREDENTIAL` (client-owned channel with no valid workspace credential — FR-037).

- Index: `(request_uid)`, `(notification_uid)`, `(customer_uid, channel)`.
- Campaign rollup (FR-028) = `GROUP BY status` over logs where `request.source_ref = campaign.uid`.

## 5. `communication_schedule` (CommunicationSchedule)

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | `CSCH…` |
| `name` | varchar | |
| `template_uid` | varchar | FK |
| `channels` | varchar | CSV of Channel |
| `audience_type` / `audience_ref` | enum / varchar? | |
| `variables_json` | text? | static context (per-recipient vars resolved at materialize) |
| `frequency` | enum(Frequency) | DAILY / WEEKLY / MONTHLY |
| `interval` | int | every N periods |
| `day_of_week` | int? | 1–7 (WEEKLY) |
| `day_of_month` | int? | 1–31 (MONTHLY; clamps to month end — FR-020) |
| `time_of_day` | varchar | `HH:mm` wall-clock in business tz |
| `start_date` | varchar? | ISO date (business tz) |
| `end_date` | varchar? | ISO date; no occurrences after (FR-021) |
| `paused` | bool | FR-022 |
| `next_run_at` | **Instant (UTC)** | computed by RecurrenceCalculator from business tz |
| `last_run_at` | Instant? | |
| `last_occurrence_key` | varchar? | |
| `version` | int | optimistic claim by sweeper |

- Index: `(paused, next_run_at)` for the sweeper scan.

## 6. `communication_occurrence` (OccurrenceLedger) — at-most-once guard

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | server-minted |
| `schedule_uid` | varchar | FK |
| `occurrence_key` | varchar | e.g. `2026-07-01T09:00` (business-tz wall-clock of the occurrence) |
| `materialized_at` | Instant | |

- **Unique: `(schedule_uid, occurrence_key)`** — the real idempotency guard; a racing/duplicate insert fails and the send is skipped (SC-006). Not synced to the app (server-internal).

## 7. `campaign` (Campaign)

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | `CCMP…` |
| `name` | varchar | |
| `template_uid` | varchar | FK |
| `channel` | enum(Channel) | single channel per campaign |
| `audience_type` / `audience_ref` | enum / varchar | typically SEGMENT (customer group) |
| `variables_json` | text? | static context |
| `status` | enum(CampaignStatus) | DRAFT→SCHEDULED→RUNNING→PAUSED→DONE |
| `scheduled_at` | Instant? | when SCHEDULED |
| `throttle_per_minute` | int? | overrides config default |
| `started_at` / `completed_at` | Instant? | |
| `targeted_count` | int | resolved audience size (set at run) |

- Rollup totals (sent/delivered/failed/skipped) computed from `communication_log`; `targeted = sent + failed + skipped` must reconcile (SC-005).
- State machine: only `DRAFT/SCHEDULED → RUNNING`; `RUNNING ↔ PAUSED`; `RUNNING/PAUSED → DONE`.

## 8. `communication_preference` (CommunicationPreference) — consent

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | `CPRF…` |
| `customer_uid` | varchar | |
| `channel` | enum(Channel) | |
| `category` | enum(MessageCategory) | |
| `opted_in` | bool | default true; transactional always effectively true |
| `source` | varchar? | how set (UNSUBSCRIBE_LINK / STOP / MANUAL / IMPORT) |

- Unique: `(customer_uid, channel, category)`. Promotional opt-out flips `opted_in=false` (FR-030).

## 9. `communication_suppression` (CommunicationSuppression) — address block list

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | `CSUP…` |
| `channel` | enum(Channel) | |
| `address` | varchar | email/phone |
| `reason` | enum(SuppressionReason) | HARD_BOUNCE / COMPLAINT / UNSUBSCRIBE |

- Unique: `(owner_id, channel, address)`. Hard bounce suppresses even transactional (FR-031).

## 10. `communication_config` (CommunicationConfig) — one row per workspace

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | `CCFG…` |
| `quiet_hours_start` | varchar? | `HH:mm` business tz |
| `quiet_hours_end` | varchar? | `HH:mm`; may be < start (spans midnight) |
| `default_throttle_per_minute` | int | default e.g. 60 |
| `promotional_footer_html` | text? | appended to promotional email (FR-002b) |
| `unsubscribe_base_url` | varchar? | tokenized unsubscribe link base |

---

## 11. `event_template_binding` (EventTemplateBinding) — transactional trigger map

Workspace-scoped mapping that tells the `TransactionalEventListener` which template + channels to fire for a given domain event. Without it, a business event has nothing to send (FR-015).

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | `CETB…` |
| `event_type` | varchar | e.g. `INVOICE_CREATED`, `ORDER_CREATED`, `PAYMENT_RECEIVED` (matches the `event` module's published types) |
| `template_uid` | varchar | FK → `message_template.uid` (or `template_code`) |
| `channels` | varchar | CSV of Channel to send on |
| `enabled` | bool | toggle without deleting |
| `active` | bool | soft-delete |

- Unique: `(owner_id, event_type)` (one binding per event type per workspace; extend to `(owner_id, event_type, template_uid)` if multiple templates per event are needed later).
- Managed via the standard `/sync` contract (`/communication/v1/bindings/sync`) so the app can configure it offline.

## 12. `communication_usage` (UsageRecord) — append-only billing ledger *(communication module)*

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | server-minted |
| `communication_log_uid` | varchar | FK → log (one usage row per billable send) |
| `channel` | enum(Channel) | |
| `credential_uid` | varchar? | which credential sent it (null only for PLATFORM with no row) |
| `provider_account_ref` | varchar? | sender used |
| `billing_mode` | enum(BillingMode) | CLIENT_OWN / PLATFORM |
| `provider_message_id` | varchar? | |
| `cost_units` | int | provider cost units (SMS segments / WhatsApp conversation = 1 / email = 1) |
| `cost_category` | varchar? | e.g. WhatsApp conversation category (MARKETING/UTILITY/…) |
| `occurred_at` | Instant | when the message went out (first SENT/DELIVERED) |

- Written once when a `communication_log` first reaches `SENT`/`DELIVERED` (never on QUEUED/SKIPPED). **Append-only** (immutable) — survives log retention pruning.
- Unique: `(communication_log_uid)` — exactly one usage row per sent message (SC-010 attribution).
- Billing report = aggregate over `(channel, credential_uid, billing_mode)` for a period; reconciles 1:1 with sent logs. Not synced to the app.

---

## `notification` module additions

### `notification_queue` (existing entity) gains:

| Column | Type | Notes |
|---|---|---|
| `subject` | varchar? | EMAIL subject (NEW) |
| `source_module` | varchar? | e.g. `communication` (NEW) |
| `source_ref` | varchar? | `communication_log.uid` correlation (NEW) |
| `credential_uid` | varchar? | resolved workspace credential used (NEW) |
| `billing_mode` | varchar? | CLIENT_OWN / PLATFORM (NEW) |

`channel` already supports `EMAIL`/`WHATSAPP`. New providers (`EmailNotificationProvider`, `WhatsAppNotificationProvider`), `NotificationDispatchService`, and `NotificationDeliveryUpdatedEvent` (now also carrying `credentialUid`, `providerAccountRef`, `billingMode`, `costUnits`).

### 13. `workspace_channel_credential` (WorkspaceChannelCredential) — NEW, `notification` module, `OwnableBaseDomain`

| Column | Type | Notes |
|---|---|---|
| `uid` | varchar | server-minted (`WCC…`) |
| `channel` | enum(NotificationChannel) | EMAIL / SMS / WHATSAPP / PUSH |
| `provider` | varchar | e.g. `META_CLOUD`, `MSG91`, `SES`, `SMTP` |
| `sender_ref` | varchar | client sender identity — WhatsApp `phone_number_id` / from-domain / SMS sender id (non-secret, returnable) |
| `display_name` | varchar? | admin label |
| `secret_ciphertext` | text | **AES-GCM ciphertext** of token/password/key (D12). NEVER returned |
| `secret_last4` | varchar? | masked hint for the UI (non-secret) |
| `config_json` | text? | non-secret extra config (region, api url, fallback hints) |
| `allow_platform_fallback` | bool | per-credential/channel policy; WhatsApp defaults false (FR-037/FR-038) |
| `status` | enum(CredentialStatus) | UNVERIFIED / VALID / INVALID / EXPIRED |
| `last_validated_at` | Instant? | from the validate action |
| `active` | bool | soft-delete |

- Unique: `(owner_id, channel, provider)`. Resolver picks the active credential for the tenant+channel.
- **Not on `/sync`** (FR-043) — managed via authenticated write-only API; secrets never leave the server.
- New Flyway version under **both** vendors (mysql + postgresql).

---

## Relationships (text ER)

```
MessageTemplate 1───* TemplateVariant            (aggregate; /sync together)
MessageTemplate 1───* CommunicationRequest        (by template_uid)
CommunicationRequest 1───* CommunicationLog        (fan-out per recipient×channel)
CommunicationLog *───1 notification_queue          (notification_uid; cross-module by id only)
CommunicationLog 1───1 CommunicationUsage           (one usage row per SENT/DELIVERED message)
WorkspaceChannelCredential (notification) ──used-by──> send path; attribution flows back to CommunicationLog + CommunicationUsage
CommunicationSchedule 1───* CommunicationOccurrence (unique occurrence_key)
EventTemplateBinding ──(maps event_type → template+channels)──> TransactionalEventListener ──> CommunicationRequest
CommunicationSchedule ──(materializes)──> CommunicationRequest
Campaign ──(materializes)──> CommunicationRequest ──> CommunicationLog (rollup)
Customer (other module) 1───* CommunicationPreference   (by customer_uid, via port)
Address ───* CommunicationSuppression               (channel+address)
Workspace 1───1 CommunicationConfig
```

## Key validation & state rules

- Email variant: `subject` and `html_body` required; `text_body` optional (auto-derived).
- DeliveryStatus transitions are monotonic; webhook updates never move backward (FR-010).
- Transactional sends skip ConsentGate/QuietHours but still honor `HARD_BOUNCE` suppression.
- Schedule `next_run_at` is always UTC `Instant`; all wall-clock math uses the workspace business timezone.
- `(schedule_uid, occurrence_key)` and `(owner_id, dedup_key)` uniqueness are the two idempotency guarantees behind SC-006.
- Credential resolution (per send): client-owned-sender channel (WhatsApp) with no valid credential → `SKIPPED`/`FAILED` with `NO_CREDENTIAL`, never platform fallback (FR-037). Channel allowing fallback + no credential → PLATFORM. Credential present + valid → CLIENT_OWN.
- `secret_ciphertext` is the only secret column; it is AES-GCM encrypted, never returned by any API, never logged (FR-039/SC-011). Decryption occurs only inside the provider on the send path.
- Exactly one `communication_usage` row per message that reaches SENT/DELIVERED → every billable send is attributable to one credential + one billing mode (SC-010).
