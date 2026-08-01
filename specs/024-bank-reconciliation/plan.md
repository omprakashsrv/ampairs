# Implementation Plan: Bank Reconciliation

**Branch**: `claude/indian-retail-ecosystem-877med` (spec dir `024-bank-reconciliation`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/024-bank-reconciliation/spec.md`

## Summary

Import bank statements (**CSV / MT940 / account-aggregator**) and **auto-match** their lines against the
spec-013 payment ledger's receipts and payments, so a business owner sees which bank credits/debits
correspond to which `PaymentVoucher` and resolves the rest. A pluggable `StatementParser` normalizes
each format to canonical `BankStatementLine`s (idempotent on a deterministic fingerprint so re-imports
don't duplicate). A multi-pass `MatchingEngine` scores candidates — **exact ref/UTR** (the UTR from
feature 016 UPI collections is the strongest signal) → **amount + date window** → **fuzzy
amount+narration** — assigning a **confidence**; high-confidence single candidates auto-match, the rest
become **suggestions** for manual review. Confirming a match **annotates** the linked voucher as
bank-reconciled (it never posts to the ledger). A detected **cheque/NACH return** routes to `payment`'s
existing **bounce** flow (which posts the reversal). An exception report ties out bank-only vs
books-only lines per account, and supports **multi-account**.

Technical approach: a new backend bounded context (`banking` module) that is a **reconciliation overlay**
— it owns accounts, imported lines, the matching engine and `BankMatch` records, reading the ledger via
`payment`'s public services and annotating vouchers through a public interface, never authoring ledger
entries (preserving spec 013's foot-to-zero invariant). Import + matching are **server-side**; the
mobile `feature/banking` offers offline-editable account config, an online statement **upload** (like
`file` multipart), and **pull-only** review/confirm of suggested matches. Full design rationale in
[research.md](./research.md).

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), an MT940/CSV parsing
approach, `core` (`OwnableBaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`); reads
`payment` (receipts/payments by date/amount/UTR; `bounce` command), `customer` (party names for fuzzy
match), `setting`. Mobile — Room KMP, Ktor, Metro DI, Navigation3, FileKit (file pick + multipart
upload), existing `data/sync`, `data/common`, `feature/payment` (voucher display).
**Storage**: Backend — PostgreSQL/MySQL via Flyway; `amountMinor` BIGINT; narration `TEXT`; timestamps
`TIMESTAMPTZ`/`TIMESTAMP`. Mobile — Room (workspace-scoped DB `banking`), `Long` minor units.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :banking:test`) incl. parser golden tests
(CSV profiles / MT940 / AA), **import idempotency** (re-import → no dup lines), matching-engine
confidence/N:M, tie-out invariant, bounce routing. Mobile — `./gradlew :feature:banking:check` +
3-target compile.
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM).
**Project Type**: Mobile + API — new backend module + KMP feature module.
**Performance Goals**: Import + match a typical monthly statement (hundreds–low-thousands of lines)
within seconds; candidate search bounded by ledger indexes (date/amount/ref); auto-match precision
prioritised over recall (no silent wrong matches).
**Constraints**: Import idempotent (deterministic fingerprint); matching annotates, never posts (ledger
invariant intact); reconciliation report ties out per account; multi-account first-class; binary upload
is online-only (off the JSON sync contract); workspace isolation.
**Scale/Scope**: Per workspace: several accounts, tens of thousands of statement lines/year. ~4 backend
entities (`BankAccount`, `BankStatementLine`, `BankMatch`, `StatementImport`), ~3 pull-only + 1 synced
config entity, ~3-4 mobile screens.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP` (valueDate, txnDate, matchedAt); money `Long` paise + tolerance compare — no floating point. |
| II. DTO & Contract Isolation | ✅ PASS | Request/Response DTOs in `banking/domain/dto/`; entities never exposed; raw uploaded file bytes never returned. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Global Jackson SNAKE_CASE; CSV column-mapping handled in the parser, not via response casing. |
| IV. Multi-Tenant Isolation | ✅ PASS | Entities extend `OwnableBaseDomain`; controllers set tenant via `X-Workspace-ID`; matching candidate search is workspace-scoped by `@TenantId`. |
| V. API Response Standardization | ✅ PASS | All endpoints return `ApiResponse<T>`; sync pull → `ApiResponse<PageResponse<T>>`. |
| VI. Centralized Exception Handling | ✅ PASS | Typed `StatementParseException`/`ReconciliationException` bubble; no business try/catch in controllers; per-line parse failures collected into the import report, not thrown per row. |
| VII. Efficient Data Loading | ✅ PASS | Indexed candidate queries (owner_id, value_date, amount, ref); `@NamedEntityGraph` for match→lines/vouchers; `@Query` for sync feed + candidate search. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web deferred; tracked follow-up. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `banking` context; reads `payment`/`customer` via public service interfaces; routes bounces to `payment`'s public bounce command; never touches their repositories. |
| X. Compose Multiplatform Parity | ✅ PASS | Shared logic/UI in `feature/banking/src/commonMain`; thin platform DI; file pick via FileKit. |
| XI. Security & Secrets Hygiene | ✅ PASS | No new secrets in Phase 1 (CSV/MT940 upload). Account-aggregator consent tokens (Phase 3) env-provided + encrypted; masked account numbers stored. |
| Flyway | ✅ PASS | Migration in **both** `mysql/` and `postgresql/`; `banking` added to `migrationModules`; next version after `V1.0.104`. |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on parsers + matching + idempotency; mobile `check` + 3-target compile. |

**Result**: PASS. The statement-upload endpoint is multipart (off the JSON `/sync` push contract) by
necessity — documented in Complexity Tracking, consistent with how `file` sits off the contract.

## Project Structure

### Documentation (this feature)

```
specs/024-bank-reconciliation/
├── plan.md              # This file
├── spec.md
├── research.md          # Phase 0 — design decisions + rationale
├── data-model.md        # Phase 1 — accounts/lines/matches/imports + state machines
├── quickstart.md        # Phase 1 — import a CSV, run matching, confirm a match
├── contracts/
│   ├── README.md
│   ├── banking-sync.md           # accounts (synced config) + lines/matches/imports (pull-only)
│   ├── banking-import.md         # multipart statement upload + parse/import
│   └── banking-actions.md        # run-match, confirm/reject match, spawn-voucher, report
├── checklists/requirements.md
└── tasks.md             # Phase 2 (NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
banking/
└── src/main/
    ├── kotlin/com/ampairs/banking/
    │   ├── domain/
    │   │   ├── model/      # BankAccount, BankStatementLine, BankMatch, StatementImport
    │   │   ├── enums/      # DrCr, MatchStatus, MatchType, Confidence, StatementFormat, ImportStatus
    │   │   └── dto/        # request/response DTOs + converters; ColumnMapping profile, ParsedLine
    │   ├── repository/     # Spring Data repos (+ candidate-search, sync feed, fingerprint upsert)
    │   ├── service/        # ImportService, MatchingEngine (multi-pass), ReconciliationReportService,
    │   │                   #   BounceDetector, BankingSettingDefinitions
    │   ├── parser/         # StatementParser port + CsvStatementParser/Mt940Parser/AccountAggregatorParser
    │   ├── controller/     # BankingController (sync + actions) + StatementImportController (multipart)
    │   └── config/         # Constants
    └── resources/db/migration/
        ├── mysql/V1.0.105__create_banking_tables.sql
        └── postgresql/V1.0.105__create_banking_tables.sql
# wiring: settings.gradle.kts (include "banking"); ampairs_service/build.gradle.kts
#         (implementation(project(":banking")) + "banking" in migrationModules);
#         payment exposes a public "annotate voucher reconciled" + existing bounce command

# Mobile — ampairs-app/ (sibling repo)
feature/banking/src/
├── commonMain/kotlin/com/ampairs/banking/
│   ├── data/api/          # BankingApi(+Impl, incl. multipart upload), ApiUrlBuilder.bankingUrl
│   ├── data/db/           # Room accounts (synced) + lines/matches/imports (pull-only) + DAOs + DB
│   ├── data/repository/   # BankingRepository (account config local-only; review actions = online)
│   ├── domain/            # models, enums, confidence helpers
│   ├── di/                # BankingModule.kt
│   ├── sync/              # BankAccountSyncDelegate (read-write config);
│   │                      #   BankStatementLine/BankMatch/StatementImport SyncDelegates (pull-only)
│   └── ui/                # account setup, statement upload, match review/confirm, reconciliation report, VMs
├── androidMain/ iosMain/ desktopMain/   # BankingModule.{platform}.kt (@SingleIn(WorkspaceScope::class))
# wiring: SyncEntity additions; ApiUrlBuilder.bankingUrl; entry from finance/settings
```

**Structure Decision**: Mobile + API. The backend `banking/` module mirrors existing bounded contexts;
the mobile `feature/banking/` is **offline-first for account config**, **online** for statement upload
(multipart, like `feature/file`), and **pull-only** for imported lines, matches and the report — the
import + matching engine is server-only.

## Phased Implementation

### Phase 1 — MVP: CSV import + amount/date matching + manual review

- **Entities**: `BankAccount` (synced config); `BankStatementLine` (immutable, fingerprint uid
  `BSL_<hash>`); `BankMatch` (line↔voucher join, confidence, status); `StatementImport` (file, account,
  counts). Flyway `V1.0.105` both vendors.
- **Parser**: `StatementParser` port + `CsvStatementParser` driven by a per-workspace `ColumnMapping`
  profile.
- **Engine**: `MatchingEngine` Pass 1 (exact ref/UTR) + Pass 2 (amount + date window); high-confidence
  single candidate auto-matches, else `SUGGESTED`.
- **Endpoints**: `POST /banking/v1/imports` (multipart upload + parse); `GET/POST /banking/v1/accounts/sync`
  (config); `GET /banking/v1/statement-lines/sync`, `/matches/sync`, `/imports/sync` (pull-only);
  `POST /banking/v1/matches/{uid}/confirm`, `/reject`; `POST /banking/v1/accounts/{uid}/run-match`.
  `BankingSettingDefinitions`.
- **Mobile**: account setup; CSV upload (FileKit multipart); match-review list (confirm/reject online);
  the confirmed match annotates the voucher in `feature/payment`.

### Phase 2 — MT940, fuzzy matching, exception report & bounce

- `Mt940Parser`; Pass 3 fuzzy (amount tolerance + narration↔party token overlap, MEDIUM confidence).
- `ReconciliationReportService`: three exception buckets (bank-only / books-only / suggested) + tie-out
  invariant; "create voucher/adjustment from orphan line" → `payment`.
- `BounceDetector`: narration-pattern detection of cheque/NACH returns → route to `payment`'s existing
  `POST /vouchers/{uid}/bounce` (reversal posts there); return-charge line reconciled as bank charge.

### Phase 3 — Account aggregator + N:M settlement matching

- `AccountAggregatorParser` (RBI AA consent-driven JSON pull) — consent tokens env/encrypted.
- N:M matching for batched settlements: one bank credit (a UPI/PSP payout from feature 016, keyed by
  UTR) matched to many receipts; settlement-aware reconciliation joining on the UTR feature 016 records.
- Multi-account dashboards and per-account closing-balance verification.

### Mobile / offline considerations

- Statement **import and matching run on the server** — parsing and a ledger-wide N:M candidate search
  aren't meaningful on a partial offline copy.
- **Account config is offline-first** (synced like `setting`); the statement **upload is an online
  multipart command** (off the JSON `/sync` contract by necessity, exactly as `feature/file` uploads
  images); imported lines, matches and the report are **pull-only**.
- Match **confirm/reject** are online actions; the resulting voucher annotation (and any bounce reversal)
  surface in `feature/payment` via its existing sync.

## Complexity Tracking

| Deviation | Why needed | Simpler alternative rejected because |
|---|---|---|
| Statement upload is multipart, off the JSON `/sync` push contract | Bank files (CSV/MT940) are binary/large and can't ride a `List<T>` JSON sync body | Same reason `file` sits off the contract — a JSON push can't carry an arbitrary statement file |
| `banking` annotates `payment` vouchers rather than posting ledger entries | Single ledger source of truth (spec 013 foot-to-zero invariant) | A new "bank receipt" entry would double-post against the existing receipt voucher |
| Bounce reversal delegated to `payment`'s existing bounce command | One reversal implementation, preserved audit | Reversing inside `banking` would duplicate ledger logic and risk drift |
