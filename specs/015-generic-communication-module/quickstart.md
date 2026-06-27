# Quickstart — Generic Communication Module

How to wire, run, and try the module locally. Backend lives in `ampairs` (this repo); the mobile feature module is a separate `ampairs-app` PR.

## 1. Register the new module

- `settings.gradle.kts` → add `include("communication")`.
- `ampairs_service/build.gradle.kts` → add `"communication"` to the `migrationModules` list and add `implementation(project(":communication"))` to the aggregator.
- `communication/build.gradle.kts` → depend on `:core`, `:event`, `:customer`, `:notification`.
- No extra component-scan config — `com.ampairs` default scan discovers it (like `printing`).

## 2. Migrations (both vendors — required)

Write `communication/src/main/resources/db/migration/{mysql,postgresql}/V1.0.x__communication_init.sql` creating the 11 tables in data-model.md (`message_template`, `message_template_variant`, `communication_request`, `communication_log`, `communication_schedule`, `communication_occurrence`, `campaign`, `communication_preference`, `communication_suppression`, `communication_config`, `communication_usage`). Add a separate `notification` migration that (a) adds `subject`, `source_module`, `source_ref`, `credential_uid`, `billing_mode` to `notification_queue` and (b) creates the `workspace_channel_credential` table.

```bash
./gradlew :ampairs_service:flywayInfo      # pick next free V-number, check both vendors
./gradlew :ampairs_service:dbMigrate
```

Postgres columns `TIMESTAMPTZ`; MySQL `TIMESTAMP`. A mysql-only migration silently won't run on Postgres — write both.

## 3. Configuration

`application.yml` (env-driven; never commit secrets):

```yaml
notification:
  email:
    enabled: true
    transport: SMTP          # or SES
    from: "no-reply@ampairs.com"
    # SMTP: host/port/username/password via env;  SES: region + AWS creds via env
  whatsapp:
    enabled: true
    provider: META_CLOUD     # or MSG91 / TWILIO
    phone_number_id: ${WHATSAPP_PHONE_NUMBER_ID}
    access_token: ${WHATSAPP_ACCESS_TOKEN}
communication:
  scheduler:
    enabled: true
    tick-seconds: 60
  campaign:
    default-throttle-per-minute: 60
  credentials:
    encryption-key: ${COMM_CRED_ENCRYPTION_KEY}   # AES-GCM master key for per-workspace secrets (env only)
```

The `notification.email`/`notification.whatsapp` blocks above are the **platform/shared** credentials (used only where a channel allows fallback). A workspace's **own** sender identity is configured at runtime via the credential API (below), stored encrypted — never in `application.yml`.

## 4. Try it — transactional (Phase A)

1. Create a template via `/sync` push:
   ```
   POST /communication/v1/templates/sync
   [{ "uid":"CTPL…","code":"INVOICE_READY","category":"TRANSACTIONAL","default_locale":"en","base_version":1,"active":true,
      "variants":[
        {"uid":"CTPV1","channel":"EMAIL","locale":"en","subject":"Invoice {{invoice_number}} is ready",
         "html_body":"<p>Hi {{customer_name}}, your invoice for {{total_amount}} is ready.</p>","active":true},
        {"uid":"CTPV2","channel":"SMS","locale":"en","text_body":"Invoice {{invoice_number}} ({{total_amount}}) is ready.","active":true}
      ]}]
   ```
2. Bind `InvoiceCreatedEvent` → `INVOICE_READY` on `[EMAIL, SMS]` (workspace binding config).
3. Create an invoice for a test customer → `InvoiceCreatedEvent` fires → `TransactionalEventListener` renders + dispatches → check `GET /communication/v1/logs/sync` for two QUEUED→SENT→DELIVERED rows.
4. Preview without sending:
   ```
   POST /communication/v1/templates/INVOICE_READY/preview
   { "channel":"EMAIL","locale":"en","variables":{"invoice_number":"INV-1","customer_name":"Asha","total_amount":"₹999"} }
   → rendered_html + missing_variables:[]
   ```

## 5. Try it — recurring (Phase B)

```
POST /communication/v1/schedules/sync
[{ "uid":"CSCH…","name":"Monthly statement","template_uid":"CTPL…","channels":"EMAIL",
   "audience_type":"SEGMENT","audience_ref":"<all-active-group>","frequency":"MONTHLY","interval":1,
   "day_of_month":1,"time_of_day":"09:00","paused":false,"active":true }]
```
The sweeper computes `next_run_at` from the **workspace business timezone**; at 09:00 business-local on the 1st it materializes one request → per-customer logs. Re-running the sweeper does not duplicate (unique `(schedule_uid, occurrence_key)`).

## 6. Try it — promotional (Phase C)

```
POST /communication/v1/campaigns/sync        # create DRAFT
POST /communication/v1/campaigns/{uid}/start  # → RUNNING; resolves audience, gates consent/quiet-hours, throttles
GET  /communication/v1/campaigns/sync         # rollup: targeted = sent + failed + skipped
```
Opt a customer out (`/preferences/sync` with `opted_in=false`, or hit `/unsubscribe?token=…`) and confirm they are SKIPPED (`OPTED_OUT`) on the next campaign.

## 6b. Try it — workspace sender identity & usage billing

```
# Configure the client's own WhatsApp number (secret is write-only — never returned)
POST /communication/v1/credentials
{ "channel":"WHATSAPP","provider":"META_CLOUD","sender_ref":"<client phone_number_id>",
  "secret":"<client access token>","allow_platform_fallback":false }
POST /communication/v1/credentials/{uid}/validate    # provider probe → status VALID

# Send a WhatsApp message → goes out from the CLIENT's number; log records credential + billing_mode=CLIENT_OWN
# Delete the credential and resend → SKIPPED with reason NO_CREDENTIAL (never platform fallback for WhatsApp)

# Usage for billing
GET /communication/v1/usage?from=2026-06-01&to=2026-06-30&group_by=channel,credential,billing_mode
# → counts per channel × credential × billing_mode; PLATFORM rows are what you bill the client
```
Verify `GET /communication/v1/credentials` returns `secret_last4`/masked only (never the secret), and that no secret appears in logs (SC-011).

## 7. Validate

```bash
./gradlew :communication:test :notification:test     # unit (renderer, recurrence, consent gate, providers)
./gradlew :communication:compileKotlin               # quick compile
./gradlew ciBuild                                    # CI gate (Docker for integration/testAll)
```

Key tests to add: `TemplateRendererTest` (placeholder substitution + missing-var warnings + HTML/text), `RecurrenceCalculatorTest` (business-tz, month-overflow day 31, midnight quiet-hours), `ConsentGateTest` (opt-out skip, transactional bypass, hard-bounce suppression), `EmailNotificationProviderTest` / `WhatsAppNotificationProviderTest` (mirror `Msg91SmsProviderTest`), `ScheduleSweeperTest` (at-most-once under overlap), `CredentialCryptoServiceTest` (encrypt/decrypt round-trip; secret never in `toString`), `WorkspaceChannelCredentialResolverTest` (client-owned vs platform-fallback policy; WhatsApp blocked with `NO_CREDENTIAL`), `UsageLedgerTest` (one row per SENT/DELIVERED; report reconciles with logs).

## 8. Mobile (separate `ampairs-app` PR)

`feature/communication` KMP module: WorkspaceScope Room DB + `CommunicationSyncDelegate` per resource (templates aggregate / schedules / campaigns / preferences / logs pull-only), Metro DI, Navigation3 entry provider, `ModuleRegistry "communication-management" → Route.Communication`. Sending stays server-side; the app authors templates/schedules/campaigns and views delivery status. Plan/tasks tracked there.
