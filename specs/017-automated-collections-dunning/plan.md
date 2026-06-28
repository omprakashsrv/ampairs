# Implementation Plan: Automated Collections & Dunning

**Branch**: `claude/indian-retail-ecosystem-877med` (spec dir `017-automated-collections-dunning`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/017-automated-collections-dunning/spec.md`

## Summary

Automated payment reminders / **dunning** layered on the existing payment ledger. A per-workspace
`ReminderPolicy` defines, per **aging bucket** (0-30 / 31-60 / 61-90 / 90+), an escalation ladder of
`ReminderStep`s (GENTLE → FIRM → FINAL → HANDOVER) that fire across channels (WhatsApp / SMS / email /
push). A daily, tenant-scoped **evaluator** reads each party's outstanding from `payment`'s public
`AgingService` / `OutstandingService`, renders a per-workspace `ReminderTemplate` with the live amount
and an **embedded pay-now link** from feature 016 (`collection`), and delivers it through the existing
`notification` module — recording every send in a `ReminderDispatch` ledger that makes reminders
**at-most-once per step per period**. Opt-out, quiet hours (workspace timezone, spec 002) and a per-week
cap gate every send; suppressions are recorded with a reason, never silently dropped.

Technical approach: a new backend bounded context (`dunning` module) that is **pure orchestration** — it
owns reminder *policy, scheduling and run-state*, but reads dues from `payment`, generates links via
`collection`, and sends via `notification`, all through public service interfaces. It posts no ledger
entries and sends no channel message directly. The firing engine is backend-only (connectivity- and
aging-bound); the mobile app surfaces dunning as **offline-editable synced config** (policy, templates,
opt-out on the canonical `/sync` contract) plus **pull-only dispatch history**. Full design rationale in
[research.md](./research.md).

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), Spring scheduling
(daily evaluator), `core` (`OwnableBaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`);
reads `payment` (`AgingService`, `OutstandingService`), `customer` (contact identifiers), optionally
`collection` (feature 016 pay link via `ObjectProvider`), sends via `notification`
(`NotificationService`), uses workspace timezone (spec 002) + `setting`. Mobile — Room KMP, Ktor, Metro
DI, Navigation3, existing `data/sync`, `data/common`.
**Storage**: Backend — PostgreSQL/MySQL via Flyway; timestamps `TIMESTAMPTZ`/`TIMESTAMP`; template
bodies `TEXT`. Mobile — Room (workspace-scoped DB `dunning`) for synced policy/templates/preferences.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :dunning:test`) incl. **idempotent evaluation**
(re-run → no double-send), quiet-hours/opt-out suppression, bucket→step selection, template rendering
with `{{pay_link}}`. Mobile — `./gradlew :feature:dunning:check` + 3-target compile.
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM).
**Project Type**: Mobile + API — new backend module + KMP feature module.
**Performance Goals**: Daily evaluator processes thousands of overdue parties within its window;
at-most-once send per step/period under re-runs/restarts; reminder send latency bounded by
`notification` (async, queued).
**Constraints**: No double-sends (persisted dispatch dedupe); respect opt-out + quiet hours
(workspace TZ); reminders quote the **live** ledger amount (never stale); dunning posts no money and
sends no message directly; workspace isolation.
**Scale/Scope**: Per workspace: hundreds–thousands of overdue parties. ~4 backend entities
(`ReminderPolicy`, `ReminderStep`, `ReminderTemplate`, `ReminderDispatch`, `DunningPreference`), ~3-4
synced config entities + pull-only history, ~3 mobile screens (policy editor, templates, history).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP` (sentAt, nextEligibleAt); quiet-hours computed in the workspace timezone (spec 002). No money math in this module. |
| II. DTO & Contract Isolation | ✅ PASS | Request/Response DTOs in `dunning/domain/dto/`; entities never exposed; converters with validation. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Global Jackson SNAKE_CASE; clients trust snake_case. |
| IV. Multi-Tenant Isolation | ✅ PASS | Entities extend `OwnableBaseDomain`; app requests set tenant via `X-Workspace-ID`; the scheduled evaluator iterates workspaces and sets tenant per run in try/finally (controller-equivalent boundary). |
| V. API Response Standardization | ✅ PASS | All endpoints return `ApiResponse<T>`; sync pull → `ApiResponse<PageResponse<T>>`. |
| VI. Centralized Exception Handling | ✅ PASS | Typed exceptions bubble; no business try/catch in controllers; evaluator handles per-party failure without aborting the run. |
| VII. Efficient Data Loading | ✅ PASS | `@NamedEntityGraph` for policy→steps; derived queries; `@Query` only for the sync feed and dispatch-dedupe lookups. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web deferred; tracked follow-up. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `dunning` context; reads `payment`/`customer`, sends via `notification`, links via `collection` — all public service interfaces, never repositories. |
| X. Compose Multiplatform Parity | ✅ PASS | Shared config UI/logic in `feature/dunning/src/commonMain`; thin platform DI. |
| XI. Security & Secrets Hygiene | ✅ PASS | No new secrets (messaging creds live in `notification`/`collection`); STOP/unsubscribe webhook (if used) verified there. |
| Flyway | ✅ PASS | Migration in **both** `mysql/` and `postgresql/`; `dunning` added to `migrationModules`; next version after `V1.0.104`. |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on evaluation/idempotency/suppression; mobile `check` + 3-target compile. |

**Result**: PASS — no violations. Complexity Tracking not required.

## Project Structure

### Documentation (this feature)

```
specs/017-automated-collections-dunning/
├── plan.md              # This file
├── spec.md
├── research.md          # Phase 0 — design decisions + rationale
├── data-model.md        # Phase 1 — policy/step/template/dispatch/preference + state machines
├── quickstart.md        # Phase 1 — configure a ladder, run the evaluator, see a dispatch
├── contracts/
│   ├── README.md
│   ├── dunning-sync.md           # policy / steps / templates / preferences (synced config)
│   └── dunning-actions.md        # dispatch history, manual send, opt-out toggle, evaluator trigger
├── checklists/requirements.md
└── tasks.md             # Phase 2 (NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
dunning/
└── src/main/
    ├── kotlin/com/ampairs/dunning/
    │   ├── domain/
    │   │   ├── model/      # ReminderPolicy, ReminderStep, ReminderTemplate, ReminderDispatch, DunningPreference
    │   │   ├── enums/      # EscalationLevel, DispatchStatus, ReminderChannel, AgingBucket
    │   │   └── dto/        # request/response DTOs + converters
    │   ├── repository/     # Spring Data repos (+ sync feed, dispatch-dedupe queries)
    │   ├── service/        # DunningEvaluator (@Scheduled daily), DispatchOrchestrator (render+send),
    │   │                   #   SuppressionService (opt-out/quiet-hours/cap), TemplateRenderer,
    │   │                   #   DunningSettingDefinitions
    │   ├── controller/     # DunningController (sync config + history + manual actions)
    │   ├── event/          # listens to CollectionSettledEvent (016) → close reminders for a paid party
    │   └── config/         # Constants
    └── resources/db/migration/
        ├── mysql/V1.0.105__create_dunning_tables.sql
        └── postgresql/V1.0.105__create_dunning_tables.sql
# wiring: settings.gradle.kts (include "dunning"); ampairs_service/build.gradle.kts
#         (implementation(project(":dunning")) + "dunning" in migrationModules);
#         payment exposes AgingService/OutstandingService publicly (already);
#         collection (016) exposes a public createPaymentLink interface (resolved via ObjectProvider)

# Mobile — ampairs-app/ (sibling repo)
feature/dunning/src/
├── commonMain/kotlin/com/ampairs/dunning/
│   ├── data/api/          # DunningApi(+Impl), ApiUrlBuilder.dunningUrl
│   ├── data/db/           # Room policy/step/template/preference + dispatch-history mirror + DAOs + DB
│   ├── data/repository/   # DunningRepository (local-only) + syncStateDao.markPendingPush for config
│   ├── domain/            # models, enums, ladder helpers
│   ├── di/                # DunningModule.kt
│   ├── sync/              # ReminderPolicy/Step/Template/Preference SyncDelegates (read-write config);
│   │                      #   ReminderDispatchSyncDelegate (pull-only history)
│   └── ui/                # ladder/policy editor, template editor, party opt-out, dispatch history, VMs
├── androidMain/ iosMain/ desktopMain/   # DunningModule.{platform}.kt (@SingleIn(WorkspaceScope::class))
# wiring: SyncEntity additions; ApiUrlBuilder.dunningUrl; entry from settings + party detail
```

**Structure Decision**: Mobile + API. The backend `dunning/` module mirrors existing bounded contexts;
the mobile `feature/dunning/` is **offline-first for config** (policy/templates/opt-out ride the
canonical `/sync` push like `setting`/`form`) and **pull-only for dispatch history** — the firing engine
is server-only.

## Phased Implementation

### Phase 1 — MVP: per-bucket ladder + email/SMS reminders

- **Entities**: `ReminderPolicy` + `ReminderStep` (bucket, dayOffset, channel, templateRef,
  escalationLevel); `ReminderTemplate` (channel, level, body); `ReminderDispatch` (partyUid, stepKey,
  periodKey, status, sentAt) with unique `(owner_id, party_uid, step_key, period_key)`;
  `DunningPreference` (party opt-out). Flyway `V1.0.105` both vendors.
- **Engine**: `DunningEvaluator` (`@Scheduled`, per-workspace tenant context) → for each overdue party,
  read aging via `payment.AgingService`, pick due steps, dedupe against `ReminderDispatch`, enqueue;
  `DispatchOrchestrator` renders template + sends via `notification`.
- **Endpoints**: `GET/POST /dunning/v1/policies/sync`, `/steps/sync`, `/templates/sync`,
  `/preferences/sync` (synced config); `GET /dunning/v1/dispatches/sync` (pull-only history);
  `POST /dunning/v1/parties/{uid}/remind-now` (manual). `DunningSettingDefinitions`.
- **Mobile**: ladder/template editors as synced config; party opt-out toggle; history list.

### Phase 2 — WhatsApp channel, quiet hours, embedded pay link

- Add WhatsApp via `notification` (channel `WHATSAPP`); quiet-hours suppression in workspace TZ;
  per-week cap; suppression reasons recorded as `SUPPRESSED` dispatches.
- `TemplateRenderer` substitutes `{{pay_link}}` from `collection` (016) at send time; graceful fallback
  CTA when 016 isn't enabled.

### Phase 3 — Escalation, handover & feedback loop

- FINAL/HANDOVER tier at the `90+` bucket (notify the owner / flag for manual collection).
- Listen to `CollectionSettledEvent` (016) and ledger receipt events to **auto-close** a party's
  reminder cycle the moment they pay (stop the ladder mid-flight).
- STOP/unsubscribe inbound (messaging-provider webhook) flips `DunningPreference.optedOut`;
  effectiveness reporting (sent → paid conversion) by bucket/template.

### Mobile / offline considerations

- The **firing engine never runs on-device** — it needs current aging and live messaging providers,
  both server-side.
- **Config is offline-first**: editing a policy, ladder or template, or toggling a party's opt-out, is a
  normal synced write (`synced = false` + `markPendingPush`) that reconciles via the canonical `/sync`
  contract — a user can tune templates offline and they take effect after sync.
- **Dispatch history is pull-only** — it reflects what the server actually sent.

## Complexity Tracking

*No constitution violations — section intentionally empty. The split between offline-editable synced
config and a server-only firing engine is a deliberate, documented design (research R8), not a
deviation.*
