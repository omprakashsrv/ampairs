# Tasks: Generic Communication Module

**Input**: Design documents from `/specs/015-generic-communication-module/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/communication-api.md, quickstart.md
**Branch**: dev on `claude/generic-communication-module-qet5o1`

**Tests**: INCLUDED — the project's constitution Testing gate, the offline-sync/provider test precedents, and quickstart §7 make tests part of done. Each story carries its own test tasks.

## Conventions

- **[P]** = parallelizable (different files, no dependency). **[USx]** = owning user story.
- Backend module root: `communication/src/main/kotlin/com/ampairs/communication/` (abbrev. `comm/`); tests `communication/src/test/kotlin/com/ampairs/communication/`.
- Notification additions: `notification/src/main/kotlin/com/ampairs/notification/` (abbrev. `notif/`).
- Migrations under **both** `…/db/migration/mysql/` and `…/db/migration/postgresql/` (a mysql-only file silently won't run on Postgres). Run `:ampairs_service:flywayInfo` to pick the next free `V1.0.x`.
- Validate after a phase: `./gradlew :communication:test :notification:test` and `:ampairs_service:ciBuild`.

---

## Phase 1: Setup (Shared Infrastructure)

- [X] **T001** Create the `communication` Gradle module: `communication/build.gradle.kts` depending on `:core`, `:event`, `:customer`, `:notification`; add `include("communication")` to `settings.gradle.kts`; add `"communication"` to `migrationModules` in `ampairs_service/build.gradle.kts` and `implementation(project(":communication"))` to the aggregator.
- [X] **T002** [P] Create the package skeleton under `comm/`: `config/`, `domain/{model,dto,enums}/`, `repository/`, `service/{template,audience,send,trigger,schedule,campaign,consent,usage}/`, `port/`, `controller/`; add `communication/CLAUDE.md` (mirror `printing/CLAUDE.md`, base path `/communication/v1/**`).
- [X] **T003** [P] Add `comm/config/Constants.kt` (UID prefixes CTPL/CTPV/CREQ/CLOG/CSCH/CCMP/CPRF/CSUP/CCFG + base path) and `comm/config/CommunicationProperties.kt` (`scheduler.enabled/tick-seconds`, `campaign.default-throttle-per-minute`, `credentials.encryption-key`).
- [X] **T004** [P] Add all enums in `comm/domain/enums/`: `Channel`, `MessageCategory`, `TriggerType`, `AudienceType`, `DeliveryStatus`, `SkipReason` (incl. `NO_CREDENTIAL`), `Frequency`, `CampaignStatus`, `SuppressionReason`, `BillingMode`, `CredentialStatus`.

---

## Phase 2: Foundational (Blocking Prerequisites)

**⚠️ No user story can start until this phase is complete.** This builds the shared send engine, schema, dispatch bridge, providers, and credential-resolution path that every story uses.

### Schema (both vendors)

- [X] **T005** Write `communication/src/main/resources/db/migration/{mysql,postgresql}/V1.0.x__communication_init.sql` creating the 12 tables from data-model.md: `message_template`, `message_template_variant`, `communication_request`, `communication_log`, `communication_schedule`, `communication_occurrence`, `campaign`, `communication_preference`, `communication_suppression`, `communication_config`, `event_template_binding`, `communication_usage`. `TIMESTAMPTZ` (pg) / `TIMESTAMP` (mysql); unique constraints per data-model (`(owner_id,code)`, `(template_uid,channel,locale)`, `(schedule_uid,occurrence_key)`, `(owner_id,dedup_key)`, `(customer_uid,channel,category)`, `(owner_id,channel,address)`, `(owner_id,event_type)`, `(communication_log_uid)`); indexes `(paused,next_run_at)`, `(request_uid)`, `(notification_uid)`.
- [X] **T006** Write `notification/src/main/resources/db/migration/{mysql,postgresql}/V1.0.y__notification_credentials.sql`: ALTER `notification_queue` ADD `subject`, `source_module`, `source_ref`, `credential_uid`, `billing_mode`; CREATE TABLE `workspace_channel_credential` per data-model §12 (unique `(owner_id,channel,provider)`).
- [X] **T007** Verify both vendors migrate cleanly: `./gradlew :ampairs_service:flywayInfo` then `:ampairs_service:dbValidate` (and `dbMigrate` against a local Postgres+MySQL).

### Core shared entities, repos, renderer (comm)

- [X] **T008** [P] `comm/domain/model/MessageTemplate.kt` + `TemplateVariant.kt` (extend `OwnableBaseDomain`; variants keyed by `template_uid`, not a JPA relationship — the service batch-loads via `findByTemplateUidIn` to avoid N+1, no `@NamedEntityGraph`); repos `comm/repository/MessageTemplateRepository.kt`, `TemplateVariantRepository.kt`.
- [X] **T009** [P] `comm/domain/model/CommunicationRequest.kt` + `CommunicationLog.kt` (incl. `credential_uid`/`provider_account_ref`/`billing_mode`/`occurrence_key`); repos `CommunicationRequestRepository.kt`, `CommunicationLogRepository.kt`.
- [X] **T010** [P] `comm/domain/model/CommunicationConfig.kt` + `CommunicationUsage.kt`; repos + a `CommunicationConfigService` that lazily creates the per-workspace defaults row.
- [X] **T011** [P] `comm/service/template/TemplateRenderer.kt` — logic-less Mustache-style `{{var}}` interpolation over subject/HTML/text; returns rendered output + `missingVariables`; required-missing throws a typed `TemplateRenderException`. Include HTML→plain-text derivation for the email plain-text fallback.

### Notification dispatch bridge + providers (notif)

- [X] **T012** `notif/service/NotificationDispatchService.kt` — public interface + `DispatchRequest` data class (channel, recipient, subject?, body, textBody?, title?, dataPayload, providerTemplateId?, params, category, sourceModule, sourceRef) returning the `notification_queue` uid; implementation enqueues a `NotificationQueue` row with the new columns.
- [X] **T013** `notif/event/NotificationDeliveryUpdatedEvent.kt` — Spring `ApplicationEvent` carrying `sourceModule, sourceRef, status, providerMessageId, error, credentialUid, providerAccountRef, billingMode, costUnits, costCategory`; publish it from the queue's terminal-status transitions (monotonic, no regression — FR-010).
- [X] **T014** [P] `notif/provider/email/EmailNotificationProvider.kt` implementing `NotificationProvider` (SMTP via Spring Mail and/or SES via AWS SDK, selected by `NotificationProperties.email.transport`); multipart HTML + plain-text; returns provider message id. Extend `NotificationProperties` with the `email` block.
- [X] **T015** [P] `notif/provider/whatsapp/WhatsAppNotificationProvider.kt` implementing `NotificationProvider` (Cloud API via `RestClient`; approved-template send with `providerTemplateId` + ordered `params`). Extend `NotificationProperties` with the `whatsapp` block.

### Per-workspace credential foundation (notif) — on the send path

- [X] **T016** [P] `notif/credential/WorkspaceChannelCredential.kt` (`OwnableBaseDomain`; `secret_ciphertext`, `secret_last4`, `allow_platform_fallback`, `status`, `last_validated_at`) + repository.
- [X] **T017** [P] `notif/credential/CredentialCryptoService.kt` — AES-GCM encrypt/decrypt with the master key from env (`COMM_CRED_ENCRYPTION_KEY`); never logs/echoes plaintext; `toString()` redacted.
- [X] **T018** `notif/credential/WorkspaceChannelCredentialResolver.kt` — resolves the current-tenant credential for a channel: valid credential → `CLIENT_OWN`; none + `allow_platform_fallback` → `PLATFORM` (platform config); none + client-owned channel (WhatsApp) → throw `NoCredentialException` (→ `NO_CREDENTIAL` skip). Returns `(credentialUid, providerAccountRef, billingMode)`. Wire providers (T014/T015 + existing SMS/push) to call the resolver and decrypt the secret only here.
- [X] **T019** `notif/credential/WorkspaceChannelCredentialService.kt` — public interface (CRUD + `validate`) returning **masked** responses (no secret); used later by US6's controller. Secrets write-only.

### Send orchestration + ports (comm)

- [X] **T020** `comm/port/CustomerAudiencePort.kt` + `comm/service/audience/CustomerAudienceAdapter.kt` — `resolve(audienceType, ref)` → `List<Recipient(customerUid, email?, phone?, pushTokens?, locale?)>`; implement **SINGLE** + **LIST** now via `customer`'s public service (inject via `ObjectProvider`); **SEGMENT** stubbed with a `TODO(US3)`. Add a minimal read interface to the `customer` module if one is missing (no foreign-repo access).
- [X] **T021** `comm/port/BusinessTimezonePort.kt` + adapter — resolve a workspace's business timezone (reuse `business`/locale source); used by US2/US3.
- [X] **T022** `comm/service/send/CommunicationDispatchService.kt` — the engine: select variant (channel+locale, default-locale fallback), render (T011), per recipient resolve address → if missing record `SKIPPED(NO_ADDRESS)`; honor hard-bounce suppression; call `NotificationDispatchService` (T012); persist `CommunicationLog` (QUEUED); on `NotificationDeliveryUpdatedEvent` (T013) update the log and **append one `communication_usage` row on first SENT/DELIVERED** with credential attribution. Inject the dispatch service via `ObjectProvider`. **For `PROMOTIONAL`-category email, append the workspace unsubscribe footer** (`CommunicationConfig.promotional_footer_html`, with a per-recipient signed unsubscribe token/link) to the rendered HTML + a text equivalent to the plain-text body (FR-002b); transactional email gets no footer. (Token verification + opt-out handling is T050.)
- [X] **T023** [P] Foundational tests: `TemplateRendererTest` (substitution, missing-var warnings, HTML+text, required-missing throws), `CredentialCryptoServiceTest` (round-trip; secret absent from `toString`), `WorkspaceChannelCredentialResolverTest` (CLIENT_OWN vs PLATFORM policy; WhatsApp → `NO_CREDENTIAL`).

**Checkpoint**: send engine + schema + providers + credential resolution ready. User stories can begin.

---

## Phase 3: User Story 1 — Transactional message on a business event (P1) 🎯 MVP

**Goal**: A configured business event (start with invoice-created) immediately sends a personalized message on the selected channels; transactional bypasses promotional opt-out/quiet-hours but honors hard-bounce suppression.

**Independent Test**: Seed an `INVOICE_READY` template (email+SMS), bind it to `InvoiceCreatedEvent`, create an invoice for a test customer → both messages dispatched with substituted values and logged through to a terminal status.

- [X] **T024** [US1] `comm/domain/model/EventTemplateBinding.kt` (`OwnableBaseDomain`; `event_type`, `template_uid`, `channels`, `enabled`; unique `(owner_id,event_type)` — created by the T005 migration) + repo + `BindingService` (bulk upsert) + `comm/controller/BindingSyncController.kt` exposing `GET/POST /communication/v1/bindings/sync` (standard contract) + DTOs/converters in `comm/domain/dto/`.
- [X] **T025** [US1] `comm/service/trigger/TransactionalEventListener.kt` — `@EventListener @Async` for `InvoiceCreatedEvent` (extensible to `OrderCreatedEvent`/payment events); builds the variable context from event fields (`invoiceNumber`, `customerName`, `totalAmount`), resolves audience = the event's customer, marks category `TRANSACTIONAL` (consent/quiet-hours bypassed), calls `CommunicationDispatchService`.
- [X] **T026** [US1] `comm/controller/CommunicationRequestController.kt` — `POST /communication/v1/requests` (manual send: template_code, channels[], audience, variables) returning `ApiResponse<CommunicationRequestResponse>`; request/response DTOs + converters; tenant set at controller level.
- [X] **T027** [US1] Wire and verify the **delivery-event → log update** path end-to-end: `CommunicationDispatchService` listens to `NotificationDeliveryUpdatedEvent` (T013/T022), updates the matching `CommunicationLog` status, and writes the usage row on first SENT/DELIVERED. (Does NOT add an endpoint — the `GET /communication/v1/logs/sync` read surface is owned solely by T033 in US4 to avoid a duplicate interim controller.)
- [X] **T028** [P] [US1] Tests: `TransactionalEventListenerTest` (invoice event → 2 dispatch calls, substituted vars), `CommunicationDispatchServiceTest` (missing email → `SKIPPED(NO_ADDRESS)`, SMS still sent; transactional bypasses opt-out; honors suppression), `EmailNotificationProviderTest`/`WhatsAppNotificationProviderTest` (mirror `Msg91SmsProviderTest`).

- [X] **T060** [US1] Wire the event path to real recipients (closes the FR-015 event→contact gap): enrich `InvoiceCreatedEvent`/`OrderCreatedEvent` with `customerId` (event module) + pass it from `InvoiceService`/`OrderService`; add a public `CustomerContactProvider` (+impl, `byUid`/`byGroup`, excludes DELETED) in the customer module backed by `CustomerRepository.findByCustomerGroupAndStatusNot`; implement `CustomerAudienceAdapter` in communication consuming it via `ObjectProvider` (SINGLE→customer uid, SEGMENT→group, LIST→explicit); listener passes `event.customerId` and handles `OrderCreatedEvent`. Requires `customer.locale` (added, V1.0.107). **Module wiring = Option A** (communication already depends on customer; no inversion/cycle).

**Checkpoint**: MVP — transactional sends work end to end and are billable/attributed.

---

## Phase 4: User Story 4 — Author & reuse multi-channel, multi-language templates (P2)

**Goal**: Create/manage templates with per-channel, per-language variants (email subject + rich HTML body + plain-text alternative; provider-approved ids for WhatsApp), preview with sample data, and sync offline-first.

**Independent Test**: Create a template with email (subject+HTML, 2 languages) + SMS variants via `/templates/sync`, preview with sample data → correct substitution and missing-placeholder flags; email requires subject, SMS does not.

- [X] **T029** [US4] `comm/service/template/TemplateService.kt` — aggregate bulk-upsert (header + variants), **delete-by-absence** for variants, **`base_version`** optimistic concurrency (stale → typed conflict for client re-pull/retry), variant validation (EMAIL ⇒ subject+html_body; WHATSAPP w/ provider_template_id ⇒ params shape).
- [X] **T030** [US4] `comm/controller/TemplateSyncController.kt` — `GET/POST /communication/v1/templates/sync` (aggregate-grained, includes inactive; `last_sync/page/size/sort_by/sort_dir`); `TemplateAggregateRequest/Response` + `TemplateVariantRequest/Response` DTOs (snake_case) + converters. Follow `docs/guides/offline-sync-contract.md`.
- [X] **T031** [P] [US4] `comm/service/template/PreviewService.kt` + `POST /communication/v1/templates/{code}/preview` → `{subject?, rendered_html?, rendered_text, missing_variables[]}` (FR-006).
- [X] **T032** [P] [US4] Multi-language variant selection in `CommunicationDispatchService`: pick `(channel, recipient.locale)` else `default_locale` (FR-003); unit-tested.
- [X] **T033** [US4] Full `comm/controller/LogSyncController.kt` — pull-only `GET /communication/v1/logs/sync` (POST → 405/no-op); `CommunicationLogResponse` DTO.
- [X] **T034** [P] [US4] Tests: `TemplateServiceTest` (aggregate upsert, delete-by-absence, base_version conflict), `TemplateSyncContractTest` (sync feed includes inactive; UID-keyed upsert), `PreviewServiceTest` (substitution + missing vars + email-subject-required).

**Checkpoint**: Templates fully authored/previewed/synced; US1 now seedable purely via API.

---

## Phase 5: User Story 6 — Workspace sender identity & usage billing (P2)

**Goal**: Configure the client's own provider credentials (secrets write-only/encrypted), send from the client's sender, and report usage attributed by credential/billing-mode. (Resolver + attribution + usage write already foundational; this surfaces management + reporting.)

**Independent Test**: Configure a WhatsApp credential → send goes from the client's number (`CLIENT_OWN` in the log); delete it → WhatsApp send `SKIPPED(NO_CREDENTIAL)`; usage report breaks down by channel/credential/billing-mode and reconciles with logs; `GET` returns masked secrets only.

- [X] **T035** [US6] `comm/port/WorkspaceCredentialPort.kt` + adapter delegating to notif `WorkspaceChannelCredentialService` (T019); `comm/controller/CredentialController.kt` — `GET/POST/PUT/DELETE /communication/v1/credentials` + `POST …/{uid}/validate`; `CredentialRequest` (write `secret`) / `CredentialResponse` (masked `secret_last4`, **no secret**).
- [X] **T036** [US6] Implement `WorkspaceChannelCredentialService.validate` (T019) — provider-side probe (e.g. WhatsApp token check) setting `status` + `last_validated_at`.
- [X] **T037** [P] [US6] `comm/service/usage/UsageReportService.kt` + `GET /communication/v1/usage?from&to&group_by` → aggregate by channel × credential × billing_mode (+ totals); reconciles with SENT/DELIVERED logs.
- [X] **T038** [P] [US6] Tests: `CredentialControllerTest` (secret never returned; masked read; write-only), `CredentialValidateTest`, `UsageReportTest` (one usage row per sent message; totals reconcile with logs; CLIENT_OWN vs PLATFORM split), `secretsNotLoggedTest` (no secret in log/`toString`).

**Checkpoint**: Clients send from their own identity; usage is billable and reconciles.

---

## Phase 6: User Story 2 — Recurring scheduled communication (P2)

**Goal**: Define a recurrence (daily/weekly/monthly) that materializes sends at the correct business-local time, at-most-once per occurrence, with pause/start/end support.

**Independent Test**: Create a monthly schedule (day 1, 09:00) in a non-server timezone; advance to that business-local moment → one request per eligible customer; re-run the sweeper → no duplicates; day-31 clamps to month end.

- [X] **T039** [US2] `comm/domain/model/CommunicationSchedule.kt` + `CommunicationOccurrence.kt` + repos (`(paused,next_run_at)` index; unique `(schedule_uid,occurrence_key)`).
- [X] **T040** [US2] `comm/controller/ScheduleSyncController.kt` — `GET/POST /communication/v1/schedules/sync` (standard contract); `ScheduleRequest/Response` (server owns `next_run_at`, read-only) + converters.
- [X] **T041** [US2] `comm/service/schedule/RecurrenceCalculator.kt` — compute next occurrence in the **workspace business timezone** (via `BusinessTimezonePort`), convert to UTC `Instant`; day-of-month overflow clamps to month end (FR-020); honor start/end (FR-021).
- [X] **T042** [US2] `comm/service/schedule/ScheduleSweeper.kt` — `@Scheduled(fixedDelay=tick)` selecting `paused=false AND next_run_at<=now`, claim via optimistic `version`, insert `communication_occurrence` (unique key = at-most-once guard), materialize a `CommunicationRequest`, advance `next_run_at`/`last_occurrence_key`. Guarded by `communication.scheduler.enabled`.
- [X] **T043** [P] [US2] Tests: `RecurrenceCalculatorTest` (business-tz vs server-tz, day-31 clamp, weekly interval, end-date stop), `ScheduleSweeperTest` (at-most-once under overlapping runs; pause skips; next_run advances), `ScheduleSyncContractTest`.

**Checkpoint**: Recurring sends fire business-tz-correctly with no duplicates.

---

## Phase 7: User Story 3 — Promotional campaign with consent (P3)

**Goal**: Bulk send to a segment with a DRAFT→…→DONE lifecycle, gated by per-customer/channel/category consent, quiet hours, throttling, with a reconciling rollup and opt-out handling.

**Independent Test**: Campaign to a 100-customer group on SMS with 10 opted out → 90 targeted, 10 `SKIPPED(OPTED_OUT)`; quiet hours defer; pause stops sends; rollup `sent+failed+skipped == targeted`.

- [X] **T044** [P] [US3] `comm/domain/model/Campaign.kt` + repo (state machine DRAFT/SCHEDULED→RUNNING↔PAUSED→DONE; rollup counts derived from logs).
- [X] **T045** [P] [US3] `comm/domain/model/CommunicationPreference.kt` + `CommunicationSuppression.kt` + repos.
- [X] **T046** [US3] Complete **SEGMENT** resolution in `CustomerAudienceAdapter` (T020) — resolve customer-group membership at send time (FR-013).
- [X] **T047** [US3] `comm/service/campaign/ConsentGate.kt` + `QuietHours.kt` + `Throttler.kt` — exclude opted-out (`SKIPPED(OPTED_OUT)`); defer quiet hours (business-tz, midnight-spanning); pace to `throttle_per_minute`. Transactional path stays exempt.
- [X] **T048** [US3] `comm/service/campaign/CampaignRunner.kt` + `comm/controller/CampaignController.kt` — `GET/POST /communication/v1/campaigns/sync` + `POST …/{uid}/start|pause|resume`; resolves audience + `targeted_count` on start; `CampaignResponse` rollup (targeted/sent/delivered/failed/skipped).
- [X] **T049** [US3] `comm/controller/PreferenceSyncController.kt` — `GET/POST /communication/v1/preferences/sync` (standard contract) + DTOs.
- [X] **T050** [US3] `comm/service/consent/UnsubscribeService.kt` + public `GET/POST /communication/v1/unsubscribe?token=` (token-scoped tenant, no header) — flip preference + record `CommunicationSuppression(UNSUBSCRIBE)` (FR-030).
- [X] **T051** [US3] `notif/controller/NotificationWebhookController.kt` — `POST /notification/v1/webhooks/{provider}` (ses|sns|whatsapp), signature-verified; map to monotonic queue-status update; republish `NotificationDeliveryUpdatedEvent`; communication listener records hard-bounce/complaint → `CommunicationSuppression` and updates usage attribution.
- [X] **T052** [P] [US3] Tests: `ConsentGateTest` (opt-out skip; transactional bypass; hard-bounce suppression), `QuietHoursTest` (midnight-spanning defer), `CampaignRunnerTest` (lifecycle + rollup reconciles `targeted == sent+failed+skipped`), `UnsubscribeTest`, `WebhookTest` (no status regression; bounce → suppression).

**Checkpoint**: Promotional campaigns are compliant, paced, and reconcile.

---

## Phase 8: User Story 5 — Mobile management (P3) — separate `ampairs-app` repo/PR

High-level only here; full plan/tasks tracked in the mobile repo.

- [X] **T053** [US5] New `feature/communication` KMP module: WorkspaceScope Room DB (`CommunicationDatabase` — templates/bindings/schedules/campaigns/preferences + pull-only logs) + Metro DI (`@ContributesTo(WorkspaceScope)` per platform + `CommunicationDaoModule`), per `/metro-di`. Wired into `settings.gradle.kts`, `shared/build.gradle.kts`, `SyncEntity` (COMM_*), `ApiUrlBuilder.communicationUrl`.
- [X] **T054** [US5] `CommunicationSyncDelegates`: templates (aggregate header+variants), bindings, schedules, campaigns, preferences (standard `/sync`), logs (pull-only); per `/offline-sync`. Repos local-only (`markPendingPush`); `CommunicationSyncApi` owns push/pull. Credentials (CRUD+validate) and usage via authenticated `CommunicationActionApi` (non-sync); secrets write-only, never stored on device.
- [X] **T055** [US5] Compose UI: hub + templates (raw-HTML body + server-rendered preview), schedules (pause/resume), campaigns (start/pause/resume), credential settings, usage report, delivery-status logs. Navigation3 `CommunicationEntryProvider` + `Route.Communication` redirect + Nav3Config registration; `ModuleCodes.COMMUNICATION = "communication-management" → Route.Communication` wired in `DynamicModuleNavigationService`, `AppBottomNavigation`, `AppNavigationNav3`.

**Checkpoint**: Staff manage comms offline; sending stays server-side.

---

## Phase 9: Polish & Cross-Cutting

- [X] **T056** [P] `communication/CLAUDE.md` + `docs/modules/communication.md`; update root `docs/guides/offline-sync-contract.md` "Resources on the contract" to list communication templates(aggregate)/bindings/schedules/campaigns/preferences and note logs pull-only + credentials off-contract.
- [X] **T057** [P] `NO_MIGRATION_NEEDED.md`/version bookkeeping; confirm `migrationModules` + `flywayInfo` clean on both vendors.
- [X] **T058** Security pass: grep for any secret in logs/responses/`toString`; confirm `COMM_CRED_ENCRYPTION_KEY` env-only; credentials absent from `/sync`.
- [X] **T059** [P] `:ampairs_service:ciBuild` green on CI (commit `c443dc6` — Unit & Integration Tests + Flyway migrate/validate on PostgreSQL); `:communication`/`:notification` compile + tests pass locally (JDK 21). quickstart.md flows (transactional → recurring → credential/usage → promotional) validated structurally via the integration test suite; live end-to-end send against real SMTP/WhatsApp providers is a deploy-time/staging check (cannot dispatch live messages from the sandbox — no running app/DB/provider credentials).

---

## Dependencies & Execution Order

- **Setup (P1)** → **Foundational (P2)** blocks everything.
- **US1 (P1)** depends only on Foundational → **MVP**.
- **US4, US6, US2 (P2)** each depend on Foundational; independently testable. US4 makes US1 fully API-seedable; US6 surfaces the already-foundational credential/usage path; US2 adds scheduling.
- **US3 (P3)** depends on Foundational; reuses US4 templates + the consent/suppression entities it introduces; completes SEGMENT audience.
- **US5 (P3)** consumes the frozen `/sync` + DTO contracts; separate repo.
- **Polish (P9)** last.

### Within a story
Models → services → controllers → tests (tests may be written alongside; the project gates on them passing). Same-file tasks are sequential; `[P]` tasks touch different files.

---

## Parallel Opportunities

- Setup: T002/T003/T004 in parallel.
- Foundational: after schema (T005–T007), the entity/renderer tasks T008/T009/T010/T011 run in parallel; provider tasks T014/T015 and credential tasks T016/T017 in parallel; T023 tests in parallel.
- Across stories: once Foundational is done, **US1, US4, US6, US2 can be staffed in parallel**; US3 after US4 (templates) is available.

---

## Implementation Strategy

1. **MVP** = Setup + Foundational + **US1** → transactional invoice email/SMS, attributed and billable. Stop, validate, demo.
2. **+US4** templates authoring/preview/sync (makes everything self-serve).
3. **+US6** client sender identity + usage billing.
4. **+US2** recurring schedules.
5. **+US3** promotional campaigns + consent + webhooks.
6. **+US5** mobile (separate PR).

**MVP scope suggestion**: Phases 1–3 (T001–T028).

---

## Notes

- Backend builds/tests locally on system JDK 21 (`./gradlew :communication:test :notification:test`); the KMP app (US5) validates via CI.
- Every `/sync` endpoint must match `docs/guides/offline-sync-contract.md` exactly so the app's generic sync engine drives it unchanged.
- Two idempotency guards carry SC-006: `(schedule_uid, occurrence_key)` and `(owner_id, dedup_key)`.
- Commit per task or logical group; push updates PR #164.
