# Implementation Plan: Unified Schema-Driven Dynamic Forms

**Branch**: `011-unified-schema-driven` | **Date**: 2026-06-09 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/011-unified-schema-driven/spec.md`

## Summary

Collapse the backend's two parallel form-config concepts (`FieldConfig` + `AttributeDefinition`) and the app's mirror of them into **one unified field model** plus a **first-class section model**, expose them over a **single canonical `/sync` feed with soft-delete**, derive standard fields from a **per-domain registry** (one source of truth, replacing ~700 lines of hardcoded backend seeding and the app's 530-line duplicate), and build the **missing runtime**: a `DynamicFormRenderer` in the app's `commonMain` that every domain's entry screen renders through. Choice fields support static *and* dynamic (workspace-data-bound) options; complex inputs use a config-driven **custom-widget escape hatch**. Rollout is incremental (customer reference first → product, order, invoice, business) and backward-compatible: the legacy two `/sync` feeds keep serving older app builds as adapters over the unified store during transition.

## Technical Context

**Language/Version**: Backend — Kotlin 2.3 / Java 21 (Spring Boot 4.0). App — Kotlin 2.4.0 KMP (Compose Multiplatform 1.11.1).
**Primary Dependencies**: Backend — Spring Data JPA, Jackson (global SNAKE_CASE), Flyway, `core` (`ApiResponse`, `PageResponse`, `OwnableBaseDomain`, `TenantContextHolder`). App — Room KMP 2.8.4, Ktor 3.5.0, Metro DI 1.1.1, Navigation3, Coil, kotlinx.serialization/datetime, Kermit.
**Storage**: Backend — PostgreSQL (runtime) + MySQL (Flyway parity), JSON columns for validation/options. App — Room (workspace-scoped DB `form`), DataStore for prefs (unchanged).
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :form:test`, `testAll`). App — `./gradlew :feature:form:check`; compile-gate all targets (`androidApp:compileDebugKotlinAndroid`, `shared:compileKotlinIosSimulatorArm64`, `desktopApp:compileKotlin`).
**Target Platform**: Backend service; app on Android / iOS / Desktop (JVM).
**Project Type**: Mobile + API (two repos: `/home/user/ampairs` backend, `/home/user/ampairs-app` KMP).
**Performance Goals**: Schema fetch/render imperceptible (<16ms frame cost for a typical 20–40 field form); sync batches of 100/page, ≤10k/cycle (existing engine limits). No new latency budget beyond existing `/sync`.
**Constraints**: Offline-first (render from last-synced schema); per-workspace tenant isolation; canonical `/sync` contract preserved; KMP-pure `commonMain` (no `java.*`/`android.*`); Metro `WorkspaceScope` for DB; no hardcoded UI strings.
**Scale/Scope**: 5 target entity types; per workspace typically 20–60 standard fields + 0–30 custom fields + a handful of sections per entity. Two repos, ~1 new backend table set + registry SPI, 1 app renderer module + per-domain integration.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ | New `created_at`/`updated_at` are `Instant`/`TIMESTAMPTZ`; client uses ISO-8601 strings. |
| II. DTO & Contract Isolation | ✅ | New `FormFieldRequest/Response`, `FormSectionRequest/Response` in `form/domain/dto/`; entity↔DTO via extension fns. No JPA entity exposed. |
| III. Global JSON SNAKE_CASE | ✅ | No `@JsonProperty` for standard fields; app `@SerialName` mirrors snake_case. |
| IV. Multi-Tenant Isolation | ✅ | `form_field`/`form_section` extend `OwnableBaseDomain` (`@TenantId`); tenant set at controller. |
| V. API Response Standardization | ✅ | All endpoints return `ApiResponse<T>`; pull → `PageResponse`. |
| VI. Centralized Exception Handling | ✅ | No try/catch in controllers; validation/integrity errors bubble. |
| VII. Efficient Data Loading | ✅ | Schema read is per-`entityType` indexed query; `@NamedEntityGraph` only if relationships added (sections referenced by id, no eager graph needed). |
| VIII–IX. Web M3 / Compose | ✅ | App: Material 3, `commonMain` shared, launchers thin. Renderer lives in shared. |
| Flyway | ✅ | Paired mysql+postgresql migrations; new version via `flywayInfo`; add `form` already in `migrationModules`. |
| Offline-sync canonical contract | ✅ | New unified `/sync` follows contract incl. soft-delete; legacy feeds retained as adapters (documented compatibility, not a deviation). |

**Result: PASS.** No violations; Complexity Tracking not required. One deliberate, contract-compliant addition — temporary backward-compatible legacy `/sync` adapters — is justified under FR-026 (no break for un-migrated clients) and removed at end of rollout.

## Project Structure

### Documentation (this feature)

```
specs/011-unified-schema-driven/
├── plan.md              # This file
├── research.md          # Phase 0 — decisions & rationale
├── data-model.md        # Phase 1 — unified entities, fields, transitions
├── quickstart.md        # Phase 1 — how to add a domain to the system
├── contracts/
│   └── form-sync-api.md # Phase 1 — unified /sync contract + legacy adapters
└── tasks.md             # Phase 2 — created by /speckit.tasks (NOT here)
```

### Source Code (both repositories)

```
# BACKEND  /home/user/ampairs/form/src/main/kotlin/com/ampairs/form/
├── domain/model/
│   ├── FormField.kt              # NEW unified entity (replaces FieldConfig+AttributeDefinition)
│   ├── FormSection.kt            # NEW first-class section entity
│   ├── EntityType.kt             # NEW enum (customer/product/order/invoice/business)
│   ├── FieldDataType.kt          # NEW enum (TEXT/TEXTAREA/NUMBER/BOOLEAN/DATE/CHOICE/CUSTOM)
│   ├── FieldSource.kt            # NEW enum (STANDARD/CUSTOM)
│   └── validation/ValidationRule.kt  # NEW typed rule model (+ ChoiceOptionSource)
├── domain/dto/
│   ├── FormFieldDTOs.kt          # Request/Response/Sync + mappers
│   ├── FormSectionDTOs.kt
│   └── EntityConfigSchemaResponse.kt   # reshaped (fields+sections)
├── domain/repository/
│   ├── FormFieldRepository.kt
│   └── FormSectionRepository.kt
├── domain/service/
│   ├── FormConfigService.kt      # unified CRUD + bulk upsert + registry-driven defaults
│   └── registry/                 # NEW Standard Field Registry SPI
│       ├── StandardFieldProvider.kt   # interface (one per domain implements)
│       └── FormFieldRegistry.kt       # aggregates providers, validates fieldName/entityType
├── controller/ConfigController.kt     # +unified /config/schema/sync; legacy feeds → adapters
├── sync/FormCheckpointContributor.kt  # unchanged checkpoint key "form"
└── src/main/resources/db/migration/{mysql,postgresql}/
    └── V1.0.x__unify_form_field_model.sql   # NEW: create form_field/form_section, backfill, soft-delete

# Per-domain registry contributions (each domain module, respecting module boundaries):
customer/.../service/CustomerStandardFieldProvider.kt   # implements StandardFieldProvider
product/.../service/ProductStandardFieldProvider.kt
order/.../...   invoice/.../...   workspace(business)/.../...

# APP  /home/user/ampairs-app/feature/form-api/  (shared domain, consumed by domains)
├── domain/
│   ├── FormField.kt              # @Serializable unified model (replaces EntityFieldConfig + EntityAttributeDefinition)
│   ├── FormSection.kt
│   ├── EntityType.kt  FieldDataType.kt  FieldSource.kt
│   ├── validation/ValidationRule.kt + ValidationEngine.kt   # shared client validation
│   └── EntityConfigSchema.kt     # fields + sections + helpers
├── render/                       # NEW — the runtime
│   ├── DynamicFormRenderer.kt    # @Composable: schema + value map → full form
│   ├── FieldRenderers.kt         # text/number/date/boolean/choice composables
│   ├── DynamicOptionProvider.kt  # interface: named source → options (domains register)
│   ├── CustomFieldWidget.kt      # escape-hatch registry (widgetKey → @Composable)
│   └── FormValueState.kt         # two-way binding + validation state holder
└── repository/ConfigLookup.kt    # observeConfigSchema/refresh (unchanged surface)

# APP  /home/user/ampairs-app/feature/form/
├── data/db/  FormField/Section entities, DAOs, FormDatabase (v2 + migration)
├── data/api/ ConfigApi(+Impl)    # unified /sync feed methods
├── data/repository/ConfigRepository.kt   # local-only writes, markPendingPush(FORM)
├── sync/FormSyncDelegate.kt      # single unified feed, soft-delete aware
└── ui/   FormConfigScreen + sub-screens (Field settings | Advanced) + FormConfigViewModel
```

**Structure Decision**: Mobile + API. Backend changes are confined to the `form` module plus thin `StandardFieldProvider` implementations contributed by each domain module (cross-module via public SPI, per Principle/module-boundary rules). App changes concentrate in `feature/form-api` (shared model + renderer) and `feature/form` (storage/sync/admin UI); consuming domains swap their hand-built forms for `DynamicFormRenderer`, customer first.

## Phase 0 — Research

See [research.md](./research.md). All Technical Context items are known; research captures the **design decisions** (storage unification strategy, soft-delete flag, backward-compat for the sync contract, registry SPI, dynamic-option binding, custom-widget escape hatch, validation engine sharing) with rationale and rejected alternatives.

## Phase 1 — Design & Contracts

- [data-model.md](./data-model.md) — `FormField`, `FormSection`, enums, `ValidationRule`, `ChoiceOptionSource`, `EntityConfigSchema`; backend ↔ app field parity; soft-delete + sync columns; migration/backfill rules; state transitions.
- [contracts/form-sync-api.md](./contracts/form-sync-api.md) — unified `GET/POST /form/v1/config/schema/sync`; legacy `field-configs/sync` & `attribute-definitions/sync` retained as read/write adapters during rollout; request/response shapes; in-band soft-delete.
- [quickstart.md](./quickstart.md) — step-by-step to put a new domain on the system (register standard fields → render via `DynamicFormRenderer` → wire dynamic options + custom widgets).

## Complexity Tracking

*Not required — Constitution Check passed with no unjustified violations.*
