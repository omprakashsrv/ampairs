# Implementation Plan: Regional-Language Localization (i18n)

**Branch**: `claude/indian-retail-ecosystem-877med` (spec dir `025-regional-language-i18n`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/025-regional-language-i18n/spec.md`

## Summary

Make the Ampairs mobile app usable in the user's own language for Tier-2/3 Bharat. Two independent
axes (see [research.md](./research.md) R1): **app chrome** localized via Compose Multiplatform
resources (`composeResources/values-{lang}/strings.xml`, read with `stringResource`/`getString`), and
**server content** (product/category/unit names, notification & print templates) localized via a new
backend **`i18n`** module that stores per-locale overlay translations on the canonical `/sync`
contract and serves server-rendered text via `Accept-Language`.

The app already has the chrome foundation: `LocaleManager` (language preference in the existing
`DataStore`), `LocaleProvider` (forces `stringResource` re-read via `key(languageCode)` — runtime
switch, no restart), and a partial Hindi set. This plan completes string coverage, adds a curated
regional-language set with native-endonym picker, ICU pluralization, a deterministic fallback chain,
RTL readiness (Urdu), and the server content-translation overlay. Number/date/currency are explicitly
**out of scope** — they already follow the per-workspace `LocalAppLocale` business locale and must not
be re-derived from UI language.

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), `core`
(`OwnableBaseDomain`, `BaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`). Mobile —
Compose MP resources (`org.jetbrains.compose.resources` — `stringResource`, `getString`,
`pluralStringResource`), existing `LocaleManager`/`LocaleProvider`/`AppPreferencesDataStore`
(`data/common`), Room KMP, Ktor, Metro DI, existing `data/sync` (`CentralSyncService`,
`SyncDelegate`), `LocalAppLocale`/`BusinessLocaleProvider` (read-only — untouched).
**Storage**: Backend — translation overlay table (PostgreSQL/MySQL via Flyway), tenant rows
`OwnableBaseDomain`, master rows `BaseDomain`, timestamps `TIMESTAMPTZ`/`TIMESTAMP`. Mobile — Room
`translation` table in a workspace-scoped `i18n` DB; language preference in the existing `DataStore`
(`language_preference` key, already present); chrome strings in `composeResources/`.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :i18n:test`), translation resolve + fallback.
Mobile — `./gradlew :feature:i18n:check`; **3-target compile gate** (Android/iOS/Desktop) after any
`commonMain` change; CI string-coverage report (missing keys per language).
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM).
**Project Type**: Mobile + API — backend module + KMP feature module + cross-cutting resource changes
in every existing app module that has a `values/` directory.
**Performance Goals**: Language switch re-renders perceived-instant (<100 ms, `key`-driven recompose);
translation overlay lookup O(1) per field via a Room index on `(entity_type, entity_uid, field,
locale)`; sync batches 100 rows/page like every other entity.
**Constraints**: Offline-first — the device holds **all** locales' translation rows (sync feed is
locale-agnostic, R7); no hardcoded user-visible strings (lint-enforced); fallback never shows a raw
key; KMP-safe (`commonMain` has no `java.*`/`android.*`); never create a second `DataStore`.
**Scale/Scope**: Phase 1 = Hindi chrome + overlay scaffolding. Phase 2 = 6 regional languages +
catalog content overlay. Phase 3 = wider set + RTL/Urdu. Cross-cutting: ~10 existing modules each gain
`values-{lang}/` directories.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | Translation rows use `Instant updatedAt` → `TIMESTAMPTZ`/`TIMESTAMP`; no `LocalDateTime`. No money in this feature. |
| II. DTO & Contract Isolation | ✅ PASS | `TranslationRequest`/`TranslationResponse` in `i18n/domain/dto/`; entities never exposed; `entity.asResponse()` / `request.toEntity()` with validation. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Flat snake_case fields (`entity_type`, `entity_uid`, `field`, `locale`, `translated_text`); no `@JsonProperty`. |
| IV. Multi-Tenant Isolation | ✅ PASS | Tenant-authored translations extend `OwnableBaseDomain` (`@TenantId`); shared master-catalog translations extend `BaseDomain`; tenant set by `SessionUserFilter`; controllers honor `X-Workspace-ID`. |
| V. API Response Standardization | ✅ PASS | All endpoints return `ApiResponse<T>`; sync pull returns `ApiResponse<PageResponse<T>>` via `PageResponse.from(page)`. |
| VI. Centralized Exception Handling | ✅ PASS | No business try/catch in controllers; typed exceptions bubble to the global handler. |
| VII. Efficient Data Loading | ✅ PASS | Composite index on `(entity_type, entity_uid, field, locale)`; derived queries; `@Query` only for the sync feed. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web UI deferred; Angular i18n (`@angular/localize`) tracked as a follow-up, M3-only when added. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `i18n` bounded context with an **overlay** table — source entities (product/customer/unit) are **not** reshaped; cross-module access via the public translation service, never repositories. |
| X. Compose Multiplatform Parity | ✅ PASS | Chrome strings in each module's `commonMain/composeResources`; shared overlay logic in `feature/i18n/commonMain`; thin platform locale actuals only. |
| XI. Security & Secrets Hygiene | ✅ PASS | No secrets; reuses JWT/workspace auth; `Accept-Language` added to the existing Ktor `defaultRequest`. |
| Flyway | ✅ PASS | Versioned migration in **both** `mysql/` and `postgresql/` (next ≥ V1.0.105 per `flywayInfo`); `i18n` added to `migrationModules`. |
| Testing & Quality Gates | ✅ PASS | Backend resolve/fallback tests; mobile `check` + 3-target compile + CI string-coverage gate; hardcoded-string lint. |

**Result**: PASS — no violations; Complexity Tracking not required. The new `i18n` module is justified
(R6): a translation overlay is a distinct bounded context that must not couple to every localizable
module's schema.

## Project Structure

### Documentation (this feature)

```
specs/025-regional-language-i18n/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 — chrome/content split, switch, fallback, Accept-Language
├── data-model.md        # Phase 1 — Translation entity, Language enum, fallback resolution
├── quickstart.md        # Phase 1 — add a language; translate a product name end-to-end
├── contracts/
│   ├── README.md
│   └── translation-sync.md   # canonical /sync feed for translations
└── tasks.md             # Phase 2 (/speckit.tasks — NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
i18n/
└── src/main/
    ├── kotlin/com/ampairs/i18n/
    │   ├── domain/
    │   │   ├── model/         # Translation (overlay row), MasterTranslation (BaseDomain, shared)
    │   │   ├── enums/         # LocalizableEntityType (PRODUCT, CATEGORY, UNIT, NOTIFICATION_TEMPLATE)
    │   │   └── dto/           # TranslationRequest/Response + converters
    │   ├── repository/        # Spring Data repos (+ sync feed query incl. soft-deleted)
    │   ├── service/           # TranslationService (resolve(type,uid,field,locale)+fallback), bulkUpsert
    │   ├── controller/        # TranslationController (/i18n/v1/translations/sync)
    │   ├── config/            # Constants, LocaleResolver (Accept-Language best-match + q-values)
    │   └── support/           # AcceptLanguageInterceptor → exposes resolved locale to server-rendered paths
    └── resources/db/migration/
        ├── mysql/V1.0.105__create_i18n_translation_tables.sql
        └── postgresql/V1.0.105__create_i18n_translation_tables.sql
# wiring: settings.gradle.kts (include "i18n"); ampairs_service/build.gradle.kts
#         (implementation(project(":i18n")) + "i18n" in migrationModules)
# notification/invoice (print) modules: localize server-rendered bodies via TranslationService + Accept-Language

# Mobile — ampairs-app/ (sibling repo)
feature/i18n/src/
├── commonMain/kotlin/com/ampairs/i18n/
│   ├── data/api/          # TranslationApi(+Impl); ApiUrlBuilder.i18nUrl(...)
│   ├── data/db/           # TranslationEntity + TranslationDao + I18nRoomDatabase
│   ├── data/repository/   # TranslationRepository (local-only: read overlay, write tenant edits)
│   ├── domain/            # LocalizedText resolver: dao.get(type,uid,field,lang) ?: source
│   ├── di/                # I18nModule.kt (DAO)
│   ├── sync/              # TranslationSyncDelegate (@SyncEntity.TRANSLATION; locale-agnostic pull)
│   └── ui/                # (optional) in-app catalog-translation editor + ViewModel
├── androidMain/ iosMain/ desktopMain/   # I18nModule.{platform}.kt (@SingleIn(WorkspaceScope::class))

# Cross-cutting (existing app modules — NOT a new module):
#   data/common/.../localization/   LocaleManager + LocaleProvider (extend Language enum: ta/te/kn/mr/gu/bn/…)
#   data/common Ktor defaultRequest: add `Accept-Language` from LocaleManager.currentLanguageCode
#   shared/ + feature/{auth,customer,product,invoice,workspace,form,order,…}/
#     src/commonMain/composeResources/values-{lang}/strings.xml   ← translated chrome per module
#   each such module's build.gradle.kts already pins `compose.resources { packageOfResClass = ... }`
#   data/sync SyncEntity enum: add TRANSLATION
```

**Structure Decision**: Mobile + API. The backend `i18n/` module is a thin **overlay** bounded
context (it never reshapes product/customer/unit schemas). The mobile change is two-part: a new
`feature/i18n` module for server-content overlay (mirroring `feature/customer` offline-first +
`SyncDelegate`), plus **cross-cutting resource additions** to every existing module that renders
chrome. The chrome plumbing (`LocaleManager`, `LocaleProvider`, DataStore key) already exists and is
extended, not rebuilt. Web (Angular `@angular/localize`) is a tracked follow-up, out of scope here.

## Phased Delivery

### P1 — Chrome MVP: Hindi end-to-end + overlay scaffold

- **Chrome**: complete `values-hi/strings.xml` across all modules that have `values/`; add the
  hardcoded-string **lint** + CI **coverage report** (missing keys per language); convert remaining
  hardcoded `Text("…")` literals to `stringResource`. Language picker (Settings) listing endonyms,
  writing `language_preference` via `LocaleManager.setLanguage(...)`; verify `key(languageCode)`
  runtime switch on all 3 targets.
- **Server scaffold (backend)**: `i18n` module + `Translation`/`MasterTranslation` entities,
  `/i18n/v1/translations/sync` (canonical GET/POST, feed includes soft-deleted, locale-agnostic),
  `TranslationService.resolve(...)` with the R5 fallback, Flyway V1.0.105 (mysql+postgresql).
- **Transport**: add `Accept-Language` (current language + q-value fallback) to the app's Ktor
  `defaultRequest` plugin.
- **Entities**: `Translation(entity_type, entity_uid, field, locale, translated_text, active,
  updated_at)` `OwnableBaseDomain`; `MasterTranslation` same shape but `BaseDomain` (shared).
- **Endpoints**: `GET/POST /i18n/v1/translations/sync`.
- **Offline note**: `feature/i18n` Room `TranslationEntity` + `TranslationSyncDelegate`
  (`@SyncEntityKey(SyncEntity.TRANSLATION)`, `@ContributesIntoMap(WorkspaceScope::class)`); **pull is
  locale-agnostic** (every row, every locale) so a language switch needs no re-pull.

### P2 — Regional languages + catalog content localization

- **Chrome**: add `Language` enum entries + `values-{ta,te,kn,mr,gu,bn}/` to every module; load
  translated `strings.xml`; ICU **`pluralStringResource`** for all count-bearing strings; positional
  args (`%1$s`) replacing any concatenation.
- **Content overlay (app)**: `LocalizedText` resolver wired into product/category/unit list & detail
  screens — render `translationRepo.resolve(PRODUCT, uid, "name", currentLanguage) ?: product.name`.
  Optional in-app editor (workspace owner) to author tenant overlay rows (local-only write →
  `markPendingPush(TRANSLATION)`).
- **Server**: master-catalog translation seeding (HSN names, system categories) as `MasterTranslation`
  rows; notification-template localization resolved via `Accept-Language` on server-sent bodies.
- **Mobile/offline note**: tenant-authored overlay edits follow the standard local-only repo pattern
  (`synced=false` + `markPendingPush`); `TranslationSyncDelegate` owns push/pull; list ViewModels read
  the reactive overlay `Flow` so a language switch re-renders names from the already-synced rows.

### P3 — Wider set, RTL/Urdu, server-rendered docs

- **Languages**: ml, pa, or, as, then **ur (RTL)** after the layout audit (R9 — start/end modifiers,
  locale-driven `LocalLayoutDirection`).
- **Server-rendered**: e-invoice PDF / WhatsApp message bodies localized server-side via
  `Accept-Language` (ties to specs 015/023).
- **Web**: Angular `@angular/localize` follow-up (separate repo, M3-only).

## Complexity Tracking

*No constitution violations — section intentionally empty. The new `i18n` module is the
constitution-preferred way to add a bounded context (overlay table) without coupling to every
localizable module's schema.*
