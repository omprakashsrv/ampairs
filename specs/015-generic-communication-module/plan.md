# Implementation Plan: Generic Communication Module

**Branch**: `015-generic-communication-module` (dev on `claude/generic-communication-module-qet5o1`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/015-generic-communication-module/spec.md`

## Summary

Add a new `communication` backend bounded context that **orchestrates** multi-channel messaging (templates, audiences, schedules, campaigns, consent, delivery logs) and delegates **actual delivery** to the existing `notification` module (channel providers, queue, retry, status). `notification` gains two new providers — **Email** (SMTP/SES) and **WhatsApp** (Cloud API) — implementing its existing `NotificationProvider` interface alongside the current SMS/push providers.

Three trigger modes, built in spec-priority order:

1. **Transactional (P1)** — an `@EventListener` subscribes to `event`-module domain events (`InvoiceCreatedEvent`, `OrderCreatedEvent`, payment events) and to a manual API call; renders a template and dispatches immediately; bypasses promotional opt-out and quiet hours.
2. **Recurring (P2)** — `CommunicationSchedule` rows with a recurrence rule, materialized by a `@Scheduled` sweeper that buckets all time in the **workspace business timezone**, claims due rows, and guarantees at-most-once per occurrence via a unique occurrence ledger.
3. **Promotional (P3)** — `Campaign` rows with a `DRAFT→SCHEDULED→RUNNING→PAUSED→DONE` lifecycle, gated on per-customer/channel/category consent, quiet hours, and throttling, producing a reconciling delivery rollup.

Templates, schedules, campaigns, and preferences are exposed over the canonical offline-sync `/sync` contract so the mobile app manages them offline-first (sending stays server-side). The mobile `feature/communication` KMP module is planned at a high level here and delivered in the `ampairs-app` repo under a separate plan/PR.

## Technical Context

**Language/Version**: Kotlin 2.3 / Java 21 (backend); Kotlin 2.4 KMP (mobile, separate repo)
**Primary Dependencies**: Spring Boot 4.0, Spring Data JPA, Spring `@Scheduled`/`ApplicationEvent`, Spring Mail (SMTP) and/or AWS SDK SES, Spring `RestClient` (WhatsApp Cloud API), Jackson, Flyway; a logic-less templating renderer (Mustache-style) for HTML/text
**Storage**: PostgreSQL (runtime/dev) + MySQL (Flyway parity) — both migration vendors required
**Testing**: JUnit5 + Mockito (`:communication:test`, `:notification:test`), Testcontainers for integration (`testAll` needs Docker), provider unit tests mirroring `Msg91SmsProviderTest`
**Target Platform**: Linux server (`ampairs_service` aggregator); mobile Android/iOS/Desktop (separate repo)
**Project Type**: Web/service backend module + (high-level) mobile feature module
**Performance Goals**: transactional message dispatched < 30 s p99 (SC-001); campaign throttle configurable per-minute; sweeper tick ~1 min
**Constraints**: multi-tenant isolation (`X-Workspace-ID`); business-timezone-correct recurrence (never server zone); at-most-once sends; server-side rendering (the app never renders email)
**Scale/Scope**: per-workspace template/schedule/campaign counts in the 10s–100s; campaign audiences up to customer-group size (1k–10k); 8 new entities + notification provider/column additions

## Constitution Check

*GATE: must pass before Phase 0 and re-checked after Phase 1.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. `Instant` timestamps / `TIMESTAMPTZ` | ✅ | All entity timestamps `Instant`; recurrence `next_run_at` stored as UTC `Instant` computed from business-tz wall-clock; migrations create `TIMESTAMPTZ` (pg) / `TIMESTAMP` (mysql) |
| II. DTO isolation | ✅ | Request/Response DTOs in `communication/domain/dto/`; entity↔DTO via extension fns; no JPA entity leaves a controller |
| III. Global snake_case JSON | ✅ | No `@JsonProperty` on standard fields; `htmlBody`→`html_body` automatically |
| IV. Multi-tenancy | ✅ | All entities extend `OwnableBaseDomain`; tenant set by `SessionUserFilter` at controller level; public unsubscribe + provider-webhook endpoints resolve tenant from a signed token / stored row, not a header |
| V. `ApiResponse<T>` | ✅ | All endpoints return `ApiResponse`; `/sync` pull → `ApiResponse<PageResponse<T>>` |
| VI. Centralized exceptions | ✅ | No try/catch for business errors in controllers; typed domain exceptions bubble |
| VII. `@EntityGraph` | ✅ | Template→variants fetched via `@NamedEntityGraph` to avoid N+1 on the aggregate `/sync` feed |
| VIII. Angular M3 | ➖ N/A | No web UI in this feature |
| IX. Module boundaries | ✅ | New bounded context `communication`; cross-module access only via public service interfaces / ports (`event` listeners, a customer audience port, a notification dispatch service) — never foreign repositories |
| X. Compose parity | ➖ High-level | Mobile module planned here; built in `ampairs-app` repo per CMP rules |

**Result: PASS.** No violations; Complexity Tracking not required. (One judgment call — a new module vs. extending `notification` — is justified under Principle IX: orchestration is a distinct bounded context from transport. See research.md.)

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
    │   │   └── consent/                 # PreferenceService, SuppressionService, UnsubscribeService
    │   ├── port/                        # CustomerAudiencePort, NotificationDispatchPort, BusinessTimezonePort
    │   └── controller/                  # template/schedule/campaign/preference (/sync) + action + public unsubscribe
    ├── main/resources/db/migration/
    │   ├── mysql/V1.0.x__communication_init.sql
    │   └── postgresql/V1.0.x__communication_init.sql
    └── test/kotlin/...                  # service + renderer + recurrence + consent gate tests

# notification module additions (existing module, extended)
notification/src/main/kotlin/com/ampairs/notification/
├── provider/email/EmailNotificationProvider.kt        # NEW (SMTP/SES)
├── provider/whatsapp/WhatsAppNotificationProvider.kt  # NEW (Cloud API)
├── service/NotificationDispatchService.kt             # NEW public structured-enqueue API + delivery event
├── controller/NotificationWebhookController.kt        # NEW /notification/v1/webhooks/{provider}
└── (migration) add subject/source_module/source_ref columns to notification_queue
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

**Structure Decision**: New top-level backend Gradle module `communication/` (added to `settings.gradle.kts` and to `migrationModules` in `ampairs_service/build.gradle.kts`), discovered by the default `com.ampairs` component scan like `printing`. The mobile counterpart is a new `feature/communication` KMP module in the `ampairs-app` repo, scoped to its own plan/PR; this plan only fixes its contract surface (the `/sync` resources + DTO shapes) so the two repos stay in lockstep.

## Phasing (follows spec priority)

- **Phase A — Transactional (P1)**: module skeleton; `MessageTemplate`+`TemplateVariant` with the Mustache-style renderer + preview; `CommunicationLog`; `notification` Email provider + `NotificationDispatchService` + delivery-event feedback; `TransactionalEventListener` (start with `InvoiceCreatedEvent`); consent bypass for transactional; templates `/sync` (aggregate-grained); migrations. WhatsApp + push providers wired but transactional WhatsApp gated on an approved template id.
- **Phase B — Recurring + template polish (P2)**: `CommunicationSchedule` + `ScheduleSweeper` + `RecurrenceCalculator` (business-tz) + occurrence ledger; schedules `/sync`; multi-language variant selection + default-locale fallback.
- **Phase C — Promotional + mobile (P3)**: `Campaign` + `AudienceResolver` (customer groups) + `ConsentGate` + `QuietHours` + `Throttler` + `CommunicationSuppression` + preferences `/sync` + public unsubscribe + provider delivery webhooks (bounce → suppression); mobile `feature/communication` (separate repo/PR).

## Complexity Tracking

No constitution violations — section intentionally empty.
