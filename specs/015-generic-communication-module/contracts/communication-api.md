# API Contracts — Generic Communication Module

Base path **`/communication/v1`**. All responses are `ApiResponse<T>`. Workspace-scoped endpoints require `X-Workspace-ID` (set tenant at controller level). Snake_case JSON throughout. `/sync` endpoints follow `docs/guides/offline-sync-contract.md` verbatim.

Legend: 🔁 offline-sync resource · ▶️ action · 🌐 public (no workspace header) · 🪝 webhook (in `notification`).

---

## 🔁 Templates — aggregate-grained `/sync` (like `form`)

```
GET  /communication/v1/templates/sync?last_sync&page&size&sort_by&sort_dir
     → ApiResponse<PageResponse<TemplateAggregateResponse>>   (includes inactive; variants bundled)
POST /communication/v1/templates/sync
     body: List<TemplateAggregateRequest>   (UID-keyed; base_version optimistic concurrency)
     → ApiResponse<List<TemplateAggregateResponse>>
```

`TemplateAggregateRequest` = template header (`uid, code, name, category, default_locale, description, base_version, active`) + `variants: List<TemplateVariantRequest>`. **Delete-by-absence**: a variant omitted from the pushed aggregate is soft-deleted server-side. Stale `base_version` → `409`-style typed error in `ApiResponse.error`; client re-pulls, re-applies, retries.

`TemplateVariantRequest/Response`: `uid, channel, locale, subject?, html_body?, text_body?, provider_template_id?, provider_params_json?, active`.

## 🔁 Event→template bindings — standard `/sync`

```
GET  /communication/v1/bindings/sync?...   → ApiResponse<PageResponse<BindingResponse>>
POST /communication/v1/bindings/sync       → ApiResponse<List<BindingResponse>>
```
`BindingRequest`: `uid, event_type, template_uid, channels, enabled, active`. Drives the transactional listener: which template + channels fire for `INVOICE_CREATED` / `ORDER_CREATED` / `PAYMENT_RECEIVED` (FR-015). Unique `(workspace, event_type)`.

## 🔁 Schedules — standard `/sync`

```
GET  /communication/v1/schedules/sync?...   → ApiResponse<PageResponse<ScheduleResponse>>
POST /communication/v1/schedules/sync       → ApiResponse<List<ScheduleResponse>>
```
`ScheduleRequest`: `uid, name, template_uid, channels, audience_type, audience_ref?, variables_json?, frequency, interval, day_of_week?, day_of_month?, time_of_day, start_date?, end_date?, paused, active`. Server owns `next_run_at`/`last_run_at` (read-only in responses; ignored on write — recomputed).

## 🔁 Campaigns — standard `/sync` (authoring) + ▶️ lifecycle actions

```
GET  /communication/v1/campaigns/sync?...   → ApiResponse<PageResponse<CampaignResponse>>
POST /communication/v1/campaigns/sync       → ApiResponse<List<CampaignResponse>>
```
`CampaignRequest`: `uid, name, template_uid, channel, audience_type, audience_ref, variables_json?, scheduled_at?, throttle_per_minute?, active` (+ `status` read-only — driven by actions, not sync). `CampaignResponse` adds the rollup: `status, targeted_count, sent_count, delivered_count, failed_count, skipped_count, started_at?, completed_at?`.

```
▶️ POST /communication/v1/campaigns/{uid}/start    → ApiResponse<CampaignResponse>   (DRAFT/SCHEDULED → RUNNING; resolves audience, sets targeted_count)
▶️ POST /communication/v1/campaigns/{uid}/pause    → ApiResponse<CampaignResponse>   (RUNNING → PAUSED)
▶️ POST /communication/v1/campaigns/{uid}/resume   → ApiResponse<CampaignResponse>   (PAUSED → RUNNING)
```

## 🔁 Preferences (consent) — standard `/sync`

```
GET  /communication/v1/preferences/sync?...  → ApiResponse<PageResponse<PreferenceResponse>>
POST /communication/v1/preferences/sync      → ApiResponse<List<PreferenceResponse>>
```
`PreferenceRequest`: `uid, customer_uid, channel, category, opted_in, source?, active`.

## 🔁 Logs — pull-only `/sync`

```
GET  /communication/v1/logs/sync?...   → ApiResponse<PageResponse<CommunicationLogResponse>>
POST /communication/v1/logs/sync       → 405 / no-op (server-authored; never pushed)
```
`CommunicationLogResponse`: `uid, request_uid, customer_uid?, channel, recipient_address, category, status, skip_reason?, provider_message_id?, error_message?, sent_at?, delivered_at?, created_at, updated_at`.

---

## ▶️ Send (manual/transactional) & preview

```
POST /communication/v1/requests
     body: SendRequest { template_code, channels[], audience_type, audience_ref?,
                         recipients?[], variables{} }
     → ApiResponse<CommunicationRequestResponse>   (uid + per-log status snapshot)
```
Synchronous-enqueue: persists the request + fans out logs (QUEUED) + dispatches via `NotificationDispatchService`. Transactional category ⇒ bypasses consent/quiet-hours; honors hard-bounce suppression.

```
POST /communication/v1/templates/{code}/preview
     body: PreviewRequest { channel, locale?, variables{} }
     → ApiResponse<PreviewResponse { subject?, rendered_html?, rendered_text, missing_variables[] }>
```
Renders the selected variant with sample data; `rendered_html` is the email body as the recipient will see it (FR-006); `missing_variables` lists placeholders without a value.

---

## 🔐 Provider credentials — workspace sender identity (write-only secrets; NOT on `/sync`)

Authenticated, workspace-scoped (`X-Workspace-ID`). Secrets are **write-only** — accepted on write, **never returned** (masked on read). Owned by `notification`; surfaced under the communication-settings UI.

```
GET    /communication/v1/credentials                 → ApiResponse<List<CredentialResponse>>   (masked)
POST   /communication/v1/credentials                  body: CredentialRequest   → ApiResponse<CredentialResponse>
PUT    /communication/v1/credentials/{uid}            body: CredentialRequest   → ApiResponse<CredentialResponse>
DELETE /communication/v1/credentials/{uid}            → ApiResponse<Unit>        (soft-delete)
▶️ POST /communication/v1/credentials/{uid}/validate  → ApiResponse<CredentialResponse>   (provider probe; sets status + last_validated_at)
```
`CredentialRequest`: `channel, provider, sender_ref, display_name?, secret, config_json?, allow_platform_fallback`.
`CredentialResponse`: `uid, channel, provider, sender_ref, display_name?, secret_last4?, config_json?, allow_platform_fallback, status, last_validated_at?, active` — **no `secret`**.

## 📊 Usage / billing report

```
GET /communication/v1/usage?from&to&group_by=channel,credential,billing_mode
    → ApiResponse<UsageReportResponse>
```
`UsageReportResponse`: rows of `{ channel, credential_uid?, provider_account_ref?, billing_mode, message_count, cost_units, cost_category? }` + totals. Reconciles 1:1 with SENT/DELIVERED logs (SC-010). Consumed by the billing system to invoice `PLATFORM`-mode usage; `CLIENT_OWN` usage is informational (the client pays their provider directly).

## 🌐 Public unsubscribe (no workspace header — token-scoped)

```
GET  /communication/v1/unsubscribe?token={signed}     → minimal confirm page/payload
POST /communication/v1/unsubscribe { token }          → ApiResponse<Unit>
```
The signed token encodes `(workspace, customer_uid, channel)`; processing flips the matching `CommunicationPreference` to opted-out and records a `CommunicationSuppression(UNSUBSCRIBE)` (FR-030). Tenant resolved from the token, not a header.

---

## 🪝 Provider webhooks (in `notification`, normalized into delivery events)

```
POST /notification/v1/webhooks/{provider}    (provider ∈ ses | sns | whatsapp)
     → 200 (provider-shaped ack)
```
Verified by provider signature (not `X-Workspace-ID`). Maps the payload to a `notification_queue` status update (monotonic — no regression, FR-010), then republishes `NotificationDeliveryUpdatedEvent(source_module, source_ref, status, provider_message_id, error)`. `communication` listens and updates `CommunicationLog`; `HARD_BOUNCE`/`COMPLAINT` → `CommunicationSuppression`.

---

## Cross-module dispatch interface (in `notification`, consumed by `communication`)

```kotlin
interface NotificationDispatchService {
    fun enqueue(req: DispatchRequest): String   // returns notification_queue uid
}
data class DispatchRequest(
    val channel: NotificationChannel,
    val recipient: String,
    val subject: String? = null,
    val body: String,                 // rendered text/HTML per channel
    val textBody: String? = null,     // email plain-text alternative
    val title: String? = null,        // push
    val dataPayload: Map<String,String> = emptyMap(),
    val providerTemplateId: String? = null,
    val params: List<String> = emptyList(),
    val category: String,             // TRANSACTIONAL | PROMOTIONAL
    val sourceModule: String,         // "communication"
    val sourceRef: String,            // communication_log.uid
)
// Published by notification on terminal status (carries credential attribution for billing):
data class NotificationDeliveryUpdatedEvent(
    val sourceModule: String, val sourceRef: String,
    val status: String, val providerMessageId: String?, val error: String?,
    val credentialUid: String?, val providerAccountRef: String?,
    val billingMode: String?,        // CLIENT_OWN | PLATFORM
    val costUnits: Int?, val costCategory: String?,
)
```

`communication` injects `NotificationDispatchService` via `ObjectProvider` (optional in isolated tests) and listens to `NotificationDeliveryUpdatedEvent` with `@EventListener @Async`.

---

## Standard `/sync` query params (all 🔁 endpoints)

`last_sync` (ISO-8601 `Instant`, optional), `page=0`, `size=100`, `sort_by=updatedAt`, `sort_dir=ASC`. Pull feed **includes inactive/soft-deleted rows**. Push is UID-keyed bulk upsert with in-band soft-delete. Wrappers: pull → `ApiResponse<PageResponse<T>>`; push → `ApiResponse<List<T>>`.
