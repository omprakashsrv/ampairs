# Phase 0 Research — Regional-Language Localization (i18n)

All Technical Context unknowns are resolved below. Each item: **Decision · Rationale · Alternatives
considered**. These supersede the inline assumptions in `spec.md` with concrete technical choices.

The app is English-only today even though most of its plumbing already exists: `LocaleManager`
(language preference in DataStore), `LocaleProvider` (forces `stringResource` re-read via
`key(languageCode)`), Compose `composeResources/values/strings.xml` in ~10 modules, and a partial
Hindi set (`values-hi/`) already present in `shared`, `auth`, and `workspace`. `LocalAppLocale`
already carries currency/timezone/date-format per-workspace. This feature finishes the job for
Tier-2/3 Bharat: a curated set of regional languages, full string coverage, **server-driven content
localization** (so product/category names and notification bodies translate, not just chrome), ICU
pluralization, RTL readiness, and a deterministic fallback chain.

The single most important framing: **app chrome localization (Compose resources) and business-data
localization (server content) are two different problems with two different mechanisms.** Conflating
them is the trap this research exists to prevent.

---

## R1. The two-axis localization model — chrome (app) vs content (server)

- **Decision**: Split localization into two independent axes.
  1. **App chrome** — every fixed, ships-with-the-binary string (buttons, labels, errors, screen
     titles). Mechanism: **Compose Multiplatform resources** (`composeResources/values-{lang}/
     strings.xml`, read via `stringResource`/`getString`). Changeable only with an app release.
  2. **Server content** — tenant/catalog data the *user* typed (product names, category names,
     unit names, notification/print templates). Mechanism: **backend-stored per-locale translations**
     served via `Accept-Language`, synced into Room. Changeable without an app release.
- **Rationale**: They have different ownership (Ampairs translators vs the workspace owner / a
  central master catalog), different change cadence (release-gated vs live), and different storage
  (binary resources vs DB rows on the `/sync` feed). A Hindi UI that still shows "Detergent Powder"
  for a product is half-localized and reads as broken to a Hindi-first shopkeeper — content must
  localize too, but it cannot live in `strings.xml`.
- **Alternatives considered**: Put everything in Compose resources (rejected — can't translate
  user-entered catalog data, and forces an app release for every catalog wording change). Translate
  everything server-side and have the app render no static strings (rejected — kills offline chrome,
  adds a network hop to every label, abandons the working `stringResource` foundation).

## R2. Where the language preference lives — device/user, not workspace

- **Decision**: Language is a **per-user, per-device preference**, persisted in the existing
  `DataStore` (`language_preference` key, already present) and owned by the existing app-scoped
  `LocaleManager`. It is **independent** of `LocalAppLocale` (which is per-*workspace* business
  locale: currency/timezone/date-format). The two are deliberately separate concerns.
- **Rationale**: The reader's language is a property of the *person holding the phone*, not the
  business. A Tamil-speaking salesman and a Hindi-speaking owner can share one workspace; each picks
  their own UI language while both see the same INR currency and IST timezone from the workspace's
  `BusinessLocaleProvider`. Reusing the already-wired `LocaleManager`/DataStore means zero new
  persistence infrastructure (project rule: never create a second `DataStore`).
- **Alternatives considered**: Bind language to the workspace business profile like
  currency/timezone (rejected — wrong granularity; co-located multi-lingual staff is the common
  Bharat case). Bind to the OS locale only with no in-app override (rejected — users frequently want
  the app in their language while the phone is in English, and vice-versa; we still seed the default
  from the OS locale on first launch).

## R3. Runtime locale switch without restart — the `key(languageCode)` mechanism

- **Decision**: Keep and harden the existing pattern: `LocaleProvider` wraps the app, sets
  `Locale.setDefault(...)` via the platform `PlatformLocaleConfiguration` actual, and **forces a
  subtree recomposition with `key(languageCode)`** so every `stringResource` re-reads
  `Locale.getDefault()`. Switching language updates the DataStore flow → `LocaleManager` StateFlow →
  `key` changes → instant re-render, no process restart.
- **Rationale**: Compose MP selects the resource variant from `Locale.getDefault()` at read time;
  `LocalComposeEnvironment` is `internal` in CMP, so `key(...)`-forced recomposition is the
  sanctioned workaround (already documented in the codebase). It already works for Hindi today — the
  job is to make it correct on all three platforms and survive a workspace switch.
- **Alternatives considered**: Restart-to-apply (Android per-app locales / `AppCompatDelegate`)
  (rejected — jarring, and not portable to iOS/Desktop in `commonMain`). Reading language from a
  `CompositionLocal` and threading it into every `stringResource` call manually (rejected — CMP's
  resource accessor reads `Locale.getDefault()`, not a custom local; fighting that is brittle).
- **Platform note**: Android needs `createConfigurationContext` + `LocalConfiguration`/`LocalContext`
  override (already done); iOS sets `AppleLanguages` in `NSUserDefaults`; Desktop sets the JVM
  default `Locale`. All three actuals already exist — extend their language tables, don't rewrite.

## R4. The curated language set and locale-tag discipline

- **Decision**: Phase 1 ships **Hindi (hi)**; Phase 2 adds the high-population set — **Tamil (ta),
  Telugu (te), Kannada (kn), Marathi (mr), Gujarati (gu), Bengali (bn)**; Phase 3 widens (Malayalam
  ml, Punjabi pa, Odia or, Assamese as, Urdu ur). Each language is a `Language` enum entry
  (`code`, English name, **native endonym** for the picker, e.g. `தமிழ்`, `ಕನ್ನಡ`) and a
  `composeResources/values-{tag}/` directory **in every module that has a `values/` directory**.
- **Rationale**: Endonyms in the picker are non-negotiable — a Kannada user looks for "ಕನ್ನಡ", not
  "Kannada". Phasing by population/market priority lets the translation pipeline (R10) ramp without
  blocking the engineering work. The set maps to BCP-47 / Android resource qualifiers cleanly
  (`hi`, `ta`, `te`, …), which is what Compose MP's `values-{tag}` expects.
- **Alternatives considered**: Ship all ~12 at once (rejected — translation capacity is the
  bottleneck, not code). Region-qualified tags (`bn-IN` vs `bn-BD`) (rejected for Phase 1 — single
  India variant suffices; revisit only if a script/region divergence appears).

## R5. Fallback chain — never show a resource key to a user

- **Decision**: Resolution order is **selected language → English (`values/`, the base) → the raw
  key as last resort**. Compose MP gives the language→base fallback for free (an untranslated key
  falls through to `values/strings.xml`). We add a **CI coverage gate** so a key present in base but
  missing from a shipped language is a build warning, and a lint that flags hardcoded literals in
  `Text(...)`. For **server content**, the fallback is **requested locale → workspace default
  language → source/base text** (the original user-entered string).
- **Rationale**: A half-translated screen must degrade to readable English, never to
  `customer_image_primary_label`. Per-key fallback (not per-file) means a 60%-translated language is
  still shippable and improves incrementally. The server content fallback mirrors this so a product
  with only an English name still renders.
- **Alternatives considered**: All-or-nothing per language (rejected — blocks incremental rollout).
  Falling back to the OS locale (rejected — unpredictable; base English is the deterministic floor).

## R6. Server-driven content localization — storage and transport

- **Decision**: Localizable server content gets a **side translation table** keyed by
  `(entity_type, entity_uid, field, locale)` → `translated_text`, owned by a small new **`i18n`
  backend module** exposing a translation service + the canonical `/sync` feed
  (`GET/POST /i18n/v1/translations/sync`). Source entities (product, category, unit) are **not**
  reshaped — their existing column holds the source/base string; translations are additive overlay
  rows. The app pulls translations into a Room `translation` table and overlays them at render time;
  the read path is `translationDao.get(type, uid, field, currentLanguage) ?: entity.sourceField`.
- **Rationale**: An overlay table is additive (no migration to every localizable entity, no
  cross-module schema coupling — respects module boundaries), rides the existing offline `/sync`
  contract unchanged, and keeps the source text authoritative for search/sort/legal documents.
  Multi-tenant: translation rows extend `OwnableBaseDomain` for tenant-authored content; a global
  master-catalog translation set (HSN names, system categories) is `BaseDomain` and shared.
- **Alternatives considered**: A JSON `Map<locale,String>` column on each entity (rejected — schema
  change per entity, breaks the SNAKE_CASE flat-field convention, bloats every sync row, can't share
  a master-catalog translation across tenants). A separate translation microservice (rejected —
  over-engineered; a module + overlay table fits the monolith and the sync engine).

## R7. `Accept-Language` — header semantics and who sends it

- **Decision**: The app sends **`Accept-Language: {currentLanguageCode}`** (e.g. `ta`, with `q`-value
  fallback `ta, hi;q=0.8, en;q=0.5`) on every request via the Ktor `defaultRequest` plugin, sourced
  from `LocaleManager.currentLanguageCode`, alongside the existing `X-Workspace-ID`. Backend endpoints
  that return localizable content resolve a single best-match locale from the header and apply the R5
  fallback. The **`/sync` translation feed ignores `Accept-Language`** and returns **all** locales'
  rows (so every device has the full overlay offline) — `Accept-Language` only shapes
  *non-sync, server-rendered* responses (e-invoice PDFs, server-sent notifications, WhatsApp message
  bodies).
- **Rationale**: Offline-first means the device must hold every translation, not just the current
  one (the user can switch language on a plane). So bulk sync is locale-agnostic; `Accept-Language`
  is reserved for content the *server* renders and the client can't (push notifications, server PDFs).
  This cleanly divides "data the client localizes itself" from "text the server localizes for us".
- **Alternatives considered**: `Accept-Language`-filter the sync feed (rejected — a language switch
  would need a full re-pull and breaks offline). A custom `X-Locale` header (rejected — `Accept-
  Language` is the standard, already understood by proxies/CDNs and HTTP tooling).

## R8. Pluralization and parameterized strings — ICU plural rules

- **Decision**: Use Compose MP **`pluralStringResource`** backed by `<plural>` resources for
  count-bearing strings ("1 item" / "2 items" / Hindi/Tamil plural forms), and positional
  placeholders (`%1$s`, `%1$d`) for interpolation — **never Kotlin string concatenation** of
  translatable fragments. Plural categories follow CLDR/ICU (`one`, `other`, plus `few`/`many` where
  the language defines them).
- **Rationale**: Plural rules differ by language (Hindi/English are `one`/`other`; some languages add
  `few`/`many`); concatenating "$count " + label produces ungrammatical output and is untranslatable
  because word order varies. ICU plural categories + positional args let a translator reorder freely.
- **Alternatives considered**: `if (count == 1) singular else plural` in Kotlin (rejected — wrong for
  languages with >2 plural forms, and embeds English grammar in code). A custom MessageFormat library
  (rejected — Compose MP plurals already implement CLDR; no new dependency needed).

## R9. RTL readiness (Urdu) — layout direction, not just translation

- **Decision**: Treat RTL as a **Phase 3 concern gated behind Urdu (`ur`)**, but build for it now:
  use **direction-agnostic modifiers** (`PaddingValues` with `start`/`end`, never `left`/`right`;
  `Arrangement.Start/End`), let CMP derive `LocalLayoutDirection` from the locale, and audit
  hard-coded `Alignment.CenterStart`/icon mirroring. No Urdu strings ship until the layout audit
  passes.
- **Rationale**: Retrofitting RTL after the UI is built LTR-assuming is expensive; cheap discipline
  now (start/end modifiers) makes Urdu a translation-only effort later. CMP flips `LayoutDirection`
  from `Locale.getDefault()` automatically once the locale is RTL, so most of it is free *if* the
  modifiers are direction-agnostic.
- **Alternatives considered**: Ship Urdu in Phase 2 with LTR layout (rejected — reads as broken RTL).
  Force-flip layout direction manually per screen (rejected — fights the framework; the locale-driven
  `LayoutDirection` is the correct lever).

## R10. Translation workflow and ownership

- **Decision**: **App chrome** strings are owned by Ampairs: base English `values/strings.xml` is the
  source of truth; translations are produced out-of-band (translation vendor / community) and
  checked into `values-{lang}/`. A **string-extraction lint** enforces "all user-visible text via
  resources"; a CI coverage report lists missing keys per language. **Server content**: tenant-typed
  content is translated by the workspace owner in-app (optional, best-effort overlay rows);
  master-catalog content (HSN, system categories, notification templates) is translated centrally and
  shipped as `BaseDomain` translation rows.
- **Rationale**: Two content owners need two pipelines. Chrome can't be crowdsourced per-tenant
  (it's shared binary); catalog wording is the tenant's prerogative. The lint + coverage gate keeps
  English the canonical key set so translators always have a complete source.
- **Alternatives considered**: In-app crowd translation of chrome (rejected — quality/consistency and
  release-coupling problems). Machine-translation at runtime (rejected for chrome — quality;
  acceptable later as a *seed* for human review of catalog content, flagged but out of scope).

## R11. Number/date/currency — already solved, do not duplicate

- **Decision**: Reuse `LocalAppLocale` + `com.ampairs.common.locale.formatMoney/formatDate` as-is.
  This feature does **not** touch number/date/currency formatting — those follow the *workspace
  business locale*, not the *user UI language*, and are already correct per spec 002/cmp-practices
  §12.
- **Rationale**: Currency grouping and timezone are business facts (a Tamil UI still shows ₹ with
  Indian grouping and IST); decoupling them from UI language is intentional and already implemented.
  Re-deriving them from the UI language would regress multi-locale workspaces.
- **Alternatives considered**: Drive number/date format from UI language (rejected — wrong axis;
  would show US grouping to an English-UI Indian business).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Chrome vs content localization | Two axes: Compose resources vs server overlay (R1) |
| Where language preference lives | Per-user/device, existing DataStore + `LocaleManager` (R2) |
| Runtime switch without restart | `key(languageCode)` subtree recompose, existing pattern (R3) |
| Language set / tags | hi → ta/te/kn/mr/gu/bn → ml/pa/or/as/ur; endonyms in picker (R4) |
| Fallback chain | lang → base English → key; server: lang → workspace default → source (R5) |
| Server content storage/transport | `i18n` module, `(type,uid,field,locale)` overlay table on `/sync` (R6) |
| `Accept-Language` semantics | Sent on all requests; shapes server-rendered output only; sync feed is locale-agnostic (R7) |
| Pluralization | `pluralStringResource` + ICU/CLDR plural categories, positional args (R8) |
| RTL (Urdu) | Phase 3; build now with start/end modifiers + locale-driven `LayoutDirection` (R9) |
| Translation workflow/ownership | Ampairs owns chrome; tenant/central own content; lint + CI coverage gate (R10) |
| Number/date/currency | Untouched — `LocalAppLocale` (business locale), not UI language (R11) |
