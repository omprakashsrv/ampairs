# Implementation Plan: Generic Communication Module

**Branch**: `015-generic-communication-module` (dev on `claude/generic-communication-module-qet5o1`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/015-generic-communication-module/spec.md`

## Summary

Add a new `communication` backend bounded context that **orchestrates** multi-channel messaging (templates, audiences, schedules, campaigns, consent, delivery logs) and delegates **actual delivery** to the existing `notification` module (channel providers, queue, retry, status). `notification` gains two new providers — **Email** (SMTP/SES) and **WhatsApp** (Cloud API) — implementing its existing `NotificationProvider` interface alongside the current SMS/push providers.

Three trigger modes, built in spec-priority order:

1. **Transactional (P1)** — an `@EventListener` subscribes to `event`-module domain events (`InvoiceCreatedEvent`, `OrderCreatedEvent` — both implemented; payment-received needs a new `PaymentReceivedEvent` first, out of current scope) and to a manual API call; renders a template and dispatches immediately; bypasses promotional opt-out and quiet hours.
2. **Recurring (P2)** — `CommunicationSchedule` rows with a recurrence rule, materialized by a `@Scheduled` sweeper that buckets all time in the **workspace business timezone**, claims due rows, and guarantees at-most-once per occurrence via a unique occurrence ledger.
3. **Promotional (P3)** — `Campaign` rows with a `DRAFT→SCHEDULED→RUNNING→PAUSED→DONE` lifecycle, gated on per-customer/channel/category consent, quiet hours, and throttling, producing a reconciling delivery rollup.

Templates, schedules, campaigns, and preferences are exposed over the canonical offline-sync `/sync` contract so the mobile app manages them offline-first (sending stays server-side). The mobile `feature/communication` KMP module is planned at a high level here and delivered in the `ampairs-app` repo under a separate plan/PR.

**Per-workspace sender identity & usage billing**: `notification` also gains a `WorkspaceChannelCredential` store so each workspace sends from **its own** provider account (the client's WhatsApp number, email domain, SMS sender ID) — resolved per-tenant on the send path. WhatsApp is client-owned-sender only (no platform fallback); email/SMS may fall back per a policy flag. Secrets are AES-GCM encrypted at rest with an env-supplied key and are write-only over the API (never returned/logged). Every sent message is attributed to the credential + a `billing_mode` (CLIENT_OWN vs PLATFORM) and recorded in an append-only `communication_usage` ledger that the billing system consumes.

## Technical Context

**Language/Version**: Kotlin 2.3 / Java 21 (backend); Kotlin 2.4 KMP (mobile, separate repo)
**Primary Dependencies**: Spring Boot 4.0, Spring Data JPA, Spring `@Scheduled`/`ApplicationEvent`, Spring Mail (SMTP) and/or AWS SDK SES, Spring `RestClient` (WhatsApp Cloud API), Jackson, Flyway; a logic-less templating renderer (Mustache-style) for HTML/text
**Storage**: PostgreSQL (runtime/dev) + MySQL (Flyway parity) — both migration vendors required
**Testing**: JUnit5 + Mockito (`:communication:test`, `:notification:test`), Testcontainers for integration (`testAll` needs Docker), provider unit tests mirroring `Msg91SmsProviderTest`
**Target Platform**: Linux server (`ampairs_service` aggregator); mobile Android/iOS/Desktop (separate repo)
**Project Type**: Web/service backend module + (high-level) mobile feature module
**Performance Goals**: transactional message dispatched < 30 s p99 (SC-001); campaign throttle configurable per-minute; sweeper tick ~1 min
**Constraints**: multi-tenant isolation (`X-Workspace-ID`); business-timezone-correct recurrence (never server zone); at-most-once sends; server-side rendering (the app never renders email)
**Scale/Scope**: per-workspace template/schedule/campaign counts in the 10s–100s; campaign audiences up to customer-group size (1k–10k); 12 communication tables + 1 new notification credential table + notification provider/column additions

## Constitution Check

*GATE: must pass before Phase 0 and re-checked after Phase 1.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. `Instant` timestamps / `TIMESTAMPTZ` | ✅ | All entity timestamps `Instant`; recurrence `next_run_at` stored as UTC `Instant` computed from business-tz wall-clock; migrations create `TIMESTAMPTZ` (pg) / `TIMESTAMP` (mysql) |
| II. DTO isolation | ✅ | Request/Response DTOs in `communication/domain/dto/`; entity↔DTO via extension fns; no JPA entity leaves a controller |
| III. Global snake_case JSON | ✅ | No `@JsonProperty` on standard fields; `htmlBody`→`html_body` automatically |
| IV. Multi-tenancy | ⚠️ exception | All entities extend `OwnableBaseDomain`; tenant set by `SessionUserFilter` at controller level. **Two endpoints resolve tenant without `X-Workspace-ID`** — see Complexity Tracking: the provider webhook (verified by provider signature, correlated via `source_ref`) and the public unsubscribe link (tenant from a signed token). Both establish tenant context server-side before any repository access |
| V. `ApiResponse<T>` | ⚠️ exception | Workspace-scoped endpoints + the unsubscribe JSON `POST` return `ApiResponse` (`/sync` pull → `ApiResponse<PageResponse<T>>`). **Two endpoints deviate by necessity** — see Complexity Tracking: provider webhooks return the provider-required ack shape; the public unsubscribe `GET` returns a minimal confirmation page |
| VI. Centralized exceptions | ✅ | No try/catch for business errors in controllers; typed domain exceptions bubble |
| (Security rule #10) Secrets | ✅ | Per-tenant provider secrets stored **encrypted at rest** (AES-GCM) with the master key from env (`COMM_CRED_ENCRYPTION_KEY`) — never in source; secrets are write-only over the API (masked on read) and excluded from logs/`toString()`; credentials are **not** on the `/sync` feed |
| VII. `@EntityGraph` | ✅ | Template→variants fetched via `@NamedEntityGraph` to avoid N+1 on the aggregate `/sync` feed |
| VIII. Angular M3 | ➖ N/A | No web UI in this feature |
| IX. Module boundaries | ✅ | New bounded context `communication`; cross-module access only via public service interfaces / ports (`event` listeners, a customer audience port, a notification dispatch service) — never foreign repositories |
| X. Compose parity | ➖ High-level | Mobile module planned here; built in `ampairs-app` repo per CMP rules |

**Result: PASS with two documented exceptions** (Principles IV & V, both on the same two non-user-facing endpoints — see Complexity Tracking). Every workspace-scoped, user-facing endpoint complies fully. (One further judgment call — a new module vs. extending `notification` — is justified under Principle IX: orchestration is a distinct bounded context from transport. See research.md.)

## Project Structure

### Documentation (this feature)

```
specs/015-generic-communication-module/
├── spec.md              # /speckit.specify output (done)
├── plan.md              # this file
├── research.md          # Phase 0 — decisions (HTML templating, recurrence, dispatch bridge, audience)
├── data-model.md        # Phase 1 — entities, columns, state machines
├── contracts/
│   └── communication-api.md   # Phase 1 — endpoint contracts (/sync + actions + webhooks)
├── quickstart.md        # Phase 1 — local run / try-it walkthrough
└── tasks.md             # /speckit.tasks output (NOT created here)
```

### Source Code (backend — new module mirrors `printing`/`customer` layout)

```
communication/
├── build.gradle.kts                    # depends on :core, :event, :customer, :notification
├── CLAUDE.md
└── src/
    ├── main/kotlin/com/ampairs/communication/
    │   ├── config/                      # Constants, CommunicationProperties, scheduler config
    │   ├── domain/
    │   │   ├── model/                   # MessageTemplate, TemplateVariant, CommunicationRequest,
    │   │   │                            #   CommunicationLog, CommunicationSchedule, Campaign,
    │   │   │                            #   CommunicationPreference, CommunicationSuppression, CommunicationConfig
    │   │   ├── dto/                      # request/response DTOs + extension converters
    │   │   └── enums/                    # Channel, MessageCategory, TriggerType, DeliveryStatus, Frequency, CampaignStatus
    │   ├── repository/                   # Spring Data repos (per entity)
    │   ├── service/
    │   │   ├── template/                # TemplateService, TemplateRenderer (Mustache-style), PreviewService
    │   │   ├── audience/                # AudienceResolver (uses CustomerAudiencePort)
    │   │   ├── send/                     # CommunicationDispatchService (→ NotificationDispatchService)
    │   │   ├── trigger/                  # TransactionalEventListener (event-module @EventListener)
    │   │   ├── schedule/                # ScheduleSweeper (@Scheduled), RecurrenceCalculator, OccurrenceLedger
    │   │   ├── campaign/                # CampaignRunner, ConsentGate, QuietHours, Throttler
    │   │   ├── consent/                 # PreferenceService, SuppressionService, UnsubscribeService
    │   │   └── usage/                   # UsageLedgerService (writes communication_usage), UsageReportService
    │   ├── port/                        # CustomerAudiencePort, NotificationDispatchPort, BusinessTimezonePort,
    │   │                                #   WorkspaceCredentialPort (delegates to notification's credential service)
    │   └── controller/                  # template/schedule/campaign/preference (/sync) + action + public unsubscribe
    │                                    #   + credentials (write-only) + usage report
    ├── main/resources/db/migration/
    │   ├── mysql/V1.0.x__communication_init.sql
    │   └── postgresql/V1.0.x__communication_init.sql
    └── test/kotlin/...                  # service + renderer + recurrence + consent gate tests

# notification module additions (existing module, extended)
notification/src/main/kotlin/com/ampairs/notification/
├── provider/email/EmailNotificationProvider.kt        # NEW (SMTP/SES)
├── provider/whatsapp/WhatsAppNotificationProvider.kt  # NEW (Cloud API)
├── service/NotificationDispatchService.kt             # NEW public structured-enqueue API + delivery event
├── credential/WorkspaceChannelCredential.kt           # NEW entity (per-workspace sender identity, encrypted secret)
├── credential/WorkspaceChannelCredentialResolver.kt   # NEW per-tenant resolver + platform-fallback policy
├── credential/CredentialCryptoService.kt              # NEW AES-GCM encrypt/decrypt (env master key)
├── credential/WorkspaceChannelCredentialService.kt    # NEW public interface (CRUD + validate); communication's controller delegates here via WorkspaceCredentialPort
├── controller/NotificationWebhookController.kt        # NEW /notification/v1/webhooks/{provider}
└── (migration) notification_queue += subject/source_module/source_ref/credential_uid/billing_mode;
                NEW table workspace_channel_credential (both vendors)
```

### Source Code (mobile — high level, delivered separately in `ampairs-app`)

```
feature/communication/                  # new KMP feature module (separate plan/PR)
└── src/{commonMain,androidMain,iosMain,desktopMain}/kotlin/com/ampairs/communication/
    ├── data/{api,db,repository}/        # Room (WorkspaceScope DB), offline-sync delegate
    ├── di/                              # Metro @ContributesTo(WorkspaceScope) modules
    └── ui/                             # compose/manage templates, schedules, campaigns; delivery status
# + register "communication-management" → Route.Communication in ModuleRegistry; Navigation3 entry provider
```

**Cross-module touchpoints** (additive, for the transactional path): `customer` gains a public `CustomerContactProvider` (`byUid`/`byGroup`) consumed by communication's `CustomerAudienceAdapter` + a `customer.locale` column; `event`/`invoice`/`order` enrich `InvoiceCreatedEvent`/`OrderCreatedEvent` with `customerId`. Direction stays communication→customer/event (Option A; no cycle).

**Structure Decision**: New top-level backend Gradle module `communication/` (added to `settings.gradle.kts` and to `migrationModules` in `ampairs_service/build.gradle.kts`), discovered by the default `com.ampairs` component scan like `printing`. The mobile counterpart is a new `feature/communication` KMP module in the `ampairs-app` repo, scoped to its own plan/PR; this plan only fixes its contract surface (the `/sync` resources + DTO shapes) so the two repos stay in lockstep.

## Phasing (follows spec priority)

- **Phase A — Transactional + sender identity foundation (P1/P2)**: module skeleton; `MessageTemplate`+`TemplateVariant` with the Mustache-style renderer + preview; `CommunicationLog`; `notification` Email provider + `NotificationDispatchService` + delivery-event feedback; `TransactionalEventListener` (start with `InvoiceCreatedEvent`); consent bypass for transactional; templates `/sync` (aggregate-grained); migrations. **Credential foundation** (required for any real WhatsApp/email send): `WorkspaceChannelCredential` entity + `CredentialCryptoService` + per-tenant `WorkspaceChannelCredentialResolver` with the platform-fallback policy (WhatsApp = client-owned only); capture `credential_uid`/`provider_account_ref`/`billing_mode` on the log; write the `communication_usage` ledger row on SENT/DELIVERED. WhatsApp provider wired (sends from the workspace credential; gated on an approved template id).
- **Phase B — Recurring + credential management + template polish (P2)**: `CommunicationSchedule` + `ScheduleSweeper` + `RecurrenceCalculator` (business-tz) + occurrence ledger; schedules `/sync`; multi-language variant selection + default-locale fallback. **Credential write-only API + `/validate` action**; usage **report endpoint**.
- **Phase C — Promotional + mobile (P3)**: `Campaign` + `AudienceResolver` (customer groups) + `ConsentGate` + `QuietHours` + `Throttler` + `CommunicationSuppression` + preferences `/sync` + public unsubscribe + provider delivery webhooks (bounce → suppression; webhook carries credential attribution into usage); mobile `feature/communication` (separate repo/PR) incl. a credential-settings + usage screen.

## Complexity Tracking

Two endpoints take a documented exception to Principles IV (`X-Workspace-ID`) and V (`ApiResponse<T>`). Both are non-user-facing integration endpoints whose response shape and auth model are dictated by an external party, so the standard envelope/header cannot apply. They establish tenant context server-side before any repository access (Principle IV's intent is preserved).

| Violation | Why needed | Simpler alternative rejected because |
|---|---|---|
| **Provider webhook** `POST /notification/v1/webhooks/{provider}` returns the **provider-required ack shape** (not `ApiResponse`) and is authed by **provider signature**, not `X-Workspace-ID` | SES/SNS/WhatsApp define the exact HTTP response they expect (e.g. SNS subscription-confirmation handshake, WhatsApp `200` echo); wrapping in `ApiResponse` or demanding a workspace header would break delivery-receipt ingestion. Tenant is resolved from the stored `source_ref` correlation before any write. | Wrapping in `ApiResponse` — rejected: providers reject/!retry on unexpected bodies. Requiring `X-Workspace-ID` — rejected: providers can't send it; the correlation row already binds the row to its workspace. |
| **Public unsubscribe** `GET /communication/v1/unsubscribe` returns a **minimal confirmation page** and is authed by a **signed token**, not `X-Workspace-ID` | The link is opened by an end recipient in a browser/email client with no session and no workspace header; it must render a human page, and the tenant is encoded in (and verified from) the signed token. | Requiring `X-Workspace-ID` — rejected: recipients have no header/session. **Note:** the JSON `POST /unsubscribe` companion **does** return `ApiResponse<Unit>` — only the human `GET` page deviates. |
