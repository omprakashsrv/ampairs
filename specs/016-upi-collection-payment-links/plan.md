# Implementation Plan: UPI Collection & Payment Links

**Branch**: `claude/indian-retail-ecosystem-877med` (spec dir `016-upi-collection-payment-links`) | **Date**: 2026-06-27 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/016-upi-collection-payment-links/spec.md`

## Summary

Turn UPI from a manual payment-mode label into **real collection rails**. Today UPI is only a
`PaymentMode.UPI` value recorded on a manual `PaymentVoucher` (spec 013). This feature adds: **dynamic
UPI QR** (intent + collect), **UPI collect requests** (push to a payer VPA), **shareable payment links**
(WhatsApp/SMS/email via the `notification` module), and **webhook-driven auto-reconciliation** that, on a
verified PSP webhook, posts a receipt into the existing payment ledger — clearing the party's
outstanding immediately. A pluggable **PSP provider** (Razorpay/Cashfree/PhonePe) is resolved
per-workspace.

Technical approach: a new backend bounded context (`collection` module) owns the gateway integration,
the `CollectionRequest` lifecycle and the inbound webhook; it never owns money — on settlement it calls
the existing `payment` module's `PaymentVoucherService` to post a receipt `PaymentVoucher`
(→ `LedgerEntry` PAYMENT_IN CR) with a **deterministic uid** `RCP_<providerPaymentId>` so a duplicate or
out-of-order webhook posts **exactly once**. Webhooks are public, raw-body **signature-verified**, with
the workspace carried in PSP notes. Money is `Long` paise in the gateway, converted once to
`BigDecimal(19,4)` at the ledger boundary. Collection is **online-only** (PSP confirms it); the mobile
`feature/collection` is **pull-only** for the pending-collections feed and exposes online command
actions, while the resulting receipt arrives through `feature/payment`'s existing sync. Full design
rationale in [research.md](./research.md).

## Technical Context

**Language/Version**: Backend Kotlin 2.3 / Java 21 (Spring Boot 4.0); Mobile Kotlin Multiplatform 2.4
(Compose Multiplatform 1.11).
**Primary Dependencies**: Backend — Spring Data JPA, Flyway, Jackson (SNAKE_CASE), Spring scheduling
(expiry sweeper + status-poll reconciler), an HTTP client for PSP calls, `core`
(`OwnableBaseDomain`, `ApiResponse`, `PageResponse`, `TenantContextHolder`); calls `payment`
(`PaymentVoucherService`), `customer` (party/VPA), `invoice` (optional invoice link), `notification`
(`NotificationService`), `setting`. Mobile — Room KMP, Ktor, Metro DI, Navigation3, a QR-render lib in
`commonMain`, existing `data/sync`, `data/common` (`ApiUrlBuilder`), `feature/payment` (receipt
display).
**Storage**: Backend — PostgreSQL/MySQL via Flyway; `amountMinor` BIGINT; provider payloads `TEXT`;
timestamps `TIMESTAMPTZ`/`TIMESTAMP`. Mobile — Room (workspace-scoped DB `collection`), `Long` minor
units.
**Testing**: Backend — JUnit/Testcontainers (`./gradlew :collection:test`) incl. **idempotent
reconciliation** (duplicate/out-of-order webhook → one receipt), signature verification, expiry sweep,
amount-mismatch rejection. Mobile — `./gradlew :feature:collection:check` + 3-target compile.
**Target Platform**: Backend service (Linux); Mobile Android (minSdk 24) / iOS / Desktop (JVM).
**Project Type**: Mobile + API — new backend module + KMP feature module.
**Performance Goals**: QR/link/collect creation < 1.5 s; webhook → ledger receipt posted < 2 s; receipt
appears on mobile by the next sync; reconciliation is exactly-once under at-least-once webhooks.
**Constraints**: Online-only collection (PSP-confirmed); webhook posting **exactly-once** and
**signature-verified**; receivable clears at capture, not settlement; PSP secrets server-side encrypted;
workspace isolation; no double-posting into the payment ledger.
**Scale/Scope**: Per workspace: thousands of collection requests/month. ~2 backend entities
(`CollectionRequest`, `CollectionCredential`; optional `Settlement`), ~1 pull-only sync entity + ~5
action/webhook endpoints, ~3 mobile screens/sheets.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | How this plan complies |
|---|---|---|
| I. Type Safety (Instant/TIMESTAMPTZ) | ✅ PASS | All timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP` (expiresAt, paidAt, settledAt); money `Long` paise + `BigDecimal` at the ledger boundary — no floating point. |
| II. DTO & Contract Isolation | ✅ PASS | Request/Response DTOs in `collection/domain/dto/`; entities never exposed; PSP raw payloads kept out of client DTOs. |
| III. Global JSON SNAKE_CASE | ✅ PASS | Internal API uses global SNAKE_CASE. PSP request/response models are isolated in the provider layer (each PSP's own casing handled there, not leaked). |
| IV. Multi-Tenant Isolation | ✅ PASS | Entities extend `OwnableBaseDomain`; normal requests set tenant via `X-Workspace-ID`. The **webhook** has no JWT — tenant is resolved from PSP notes and set explicitly at the controller (try/finally), documented as the sanctioned exception. |
| V. API Response Standardization | ✅ PASS | App-facing endpoints return `ApiResponse<T>`; sync pull returns `ApiResponse<PageResponse<T>>`. (The webhook returns a bare 200/400 the PSP expects — documented.) |
| VI. Centralized Exception Handling | ✅ PASS | Typed `CollectionException`/`SignatureException` bubble to the module handler; reconciliation logic in the service, not the controller. |
| VII. Efficient Data Loading | ✅ PASS | Derived queries; `@Query` only for the sync feed, expiry sweep and pending-poll; `@NamedEntityGraph` where a request loads its settlement. |
| VIII. Angular Material 3 Exclusivity | ✅ N/A (this phase) | Web deferred; tracked follow-up. |
| IX. Domain-Driven Module Boundaries | ✅ PASS | New `collection` context; posts receipts via `payment`'s public `PaymentVoucherService` + emits `CollectionSettledEvent`; delivers links via `notification` — never cross-module repositories. |
| X. Compose Multiplatform Parity | ✅ PASS | Shared logic/UI in `feature/collection/src/commonMain`; QR render in `commonMain`; thin platform DI. |
| XI. Security & Secrets Hygiene | ✅ PASS | PSP key/secret + webhook secret env-provided + encrypted per-workspace; raw-body signature verification; no secrets in source. |
| Flyway | ✅ PASS | Migration in **both** `mysql/` and `postgresql/`; `collection` added to `migrationModules`; next version after `V1.0.104`. |
| Testing & Quality Gates | ✅ PASS | Backend ≥80% on reconciliation + signature + expiry; mobile `check` + 3-target compile. |

**Result**: PASS. The two deviations (webhook tenant-from-notes; webhook returns bare 200) are
PSP-protocol requirements, documented in Complexity Tracking — not principle violations.

## Project Structure

### Documentation (this feature)

```
specs/016-upi-collection-payment-links/
├── plan.md              # This file
├── spec.md
├── research.md          # Phase 0 — design decisions + rationale
├── data-model.md        # Phase 1 — CollectionRequest, states, reconciliation
├── quickstart.md        # Phase 1 — create QR/link, simulate a webhook, see the receipt
├── contracts/
│   ├── README.md
│   ├── collection-actions.md     # create QR/collect/link, cancel, fetch-status
│   ├── collection-webhooks.md    # POST /webhooks/{provider} signature contract
│   └── collection-sync.md        # pull-only request feed
├── checklists/requirements.md
└── tasks.md             # Phase 2 (NOT created here)
```

### Source Code (repository root)

```
# Backend — ampairs/ (this repo)
collection/
└── src/main/
    ├── kotlin/com/ampairs/collection/
    │   ├── domain/
    │   │   ├── model/      # CollectionRequest, CollectionCredential, (Settlement)
    │   │   ├── enums/      # CollectionType, CollectionStatus, SettlementStatus
    │   │   └── dto/        # request/response DTOs + converters
    │   ├── repository/     # Spring Data repos (+ sync feed, expiry/poll queries)
    │   ├── service/        # CollectionService, ReconciliationService (exactly-once posting),
    │   │                   #   ExpirySweeper (@Scheduled), StatusPollReconciler, CollectionSettingDefinitions
    │   ├── provider/       # UpiCollectionProvider port + Razorpay/Cashfree/PhonePe impls,
    │   │                   #   CollectionProviderResolver, signature verifiers
    │   ├── controller/     # CollectionController (actions + sync) + WebhookController (public)
    │   ├── event/          # CollectionSettledEvent (published after receipt posts)
    │   └── config/         # Constants, credential encryption, webhook security
    └── resources/db/migration/
        ├── mysql/V1.0.105__create_collection_tables.sql
        └── postgresql/V1.0.105__create_collection_tables.sql
# wiring: settings.gradle.kts (include "collection"); ampairs_service/build.gradle.kts
#         (implementation(project(":collection")) + "collection" in migrationModules);
#         payment module exposes PaymentVoucherService.postReceipt(...) public interface (additive)

# Mobile — ampairs-app/ (sibling repo)
feature/collection/src/
├── commonMain/kotlin/com/ampairs/collection/
│   ├── data/api/          # CollectionApi(+Impl), ApiUrlBuilder.collectionUrl
│   ├── data/db/           # Room CollectionRequest mirror + DAO + DB
│   ├── data/repository/   # CollectionRepository (local-only, pull-mirror)
│   ├── domain/            # display models, QR render, status enums
│   ├── di/                # CollectionModule.kt
│   ├── sync/              # CollectionRequestSyncDelegate (pull-only)
│   └── ui/                # "Collect via UPI" sheet (QR + send-link), pending-collections list, VMs
├── androidMain/ iosMain/ desktopMain/   # CollectionModule.{platform}.kt (@SingleIn(WorkspaceScope::class))
# wiring: SyncEntity.COLLECTION_REQUEST; ApiUrlBuilder.collectionUrl; entry from invoice/party detail
```

**Structure Decision**: Mobile + API. The backend `collection/` module mirrors `payment`/`einvoice`; the
mobile `feature/collection/` is a pull-only feature with online command actions, mirroring the pull side
of `feature/payment`. The `payment` module gains only an additive public `postReceipt` entry point.

## Phased Implementation

### Phase 1 — MVP: dynamic QR + webhook reconciliation (single PSP)

- **Entities**: `CollectionRequest` (type, partyUid, invoiceUid?, amountMinor, status, providerOrderId,
  providerPaymentId, expiresAt, paymentVoucherUid); `CollectionCredential` (encrypted per-workspace PSP
  key/secret/webhook secret). Flyway `V1.0.105` both vendors.
- **Provider**: `UpiCollectionProvider` port + one impl (Razorpay) + `CollectionProviderResolver` +
  signature verifier.
- **Actions**: `POST /collection/v1/requests` (create — type QR/collect/link), `GET /requests/{uid}`
  (status), `POST /requests/{uid}/cancel`. `GET /collection/v1/requests/sync` (pull-only).
- **Webhook**: `POST /collection/v1/webhooks/razorpay` — raw-body verify → `ReconciliationService`
  resolves workspace from notes, sets tenant, posts `RCP_<providerPaymentId>` receipt via
  `payment.PaymentVoucherService` (exactly-once), marks request `PAID`, emits `CollectionSettledEvent`.
- **Status poll**: `StatusPollReconciler` (`@Scheduled`) heals lost webhooks via the same reconcile path.
- **Mobile**: `CollectionRequestSyncDelegate` (pull-only); "Collect via UPI" sheet renders the QR;
  pending-collections list; the resulting receipt shows up in `feature/payment`.

### Phase 2 — Payment links + send via notification + expiry

- Link creation returns a short URL; `POST /requests/{uid}/share` delegates to `notification`
  (WhatsApp/SMS/email) with a templated message embedding the link.
- `ExpirySweeper` marks lapsed collect requests/links `EXPIRED`, closes the PSP order; re-share
  regenerates for an active request.
- Settings: `default_link_expiry_days`, `collect_request_ttl_minutes`, `merchant_vpa`,
  `auto_send_channel`.

### Phase 3 — Multi-PSP, settlement & refunds

- Add Cashfree + PhonePe providers; per-workspace provider switch + fallback.
- Settlement tracking (`settlementStatus`, `utr`, `settledAt`) from the PSP settlement webhook/report —
  the UTR is the join key feature 024 (bank reconciliation) matches against; receipt still posts at
  capture.
- Refund path (`POST /requests/{uid}/refund`) posting a reversal/`PAYMENT_OUT` adjustment in `payment`.

### Mobile / offline considerations

- Collection is **online-only** — creating a request and confirming payment both need the PSP. The app
  exposes online commands and a pull-only feed; it never authors a "payment received" voucher for these
  rails (that remains the manual receipt flow in `feature/payment`, spec 013).
- A QR string, once created and pulled, **renders offline** for in-person scanning; only confirmation
  needs connectivity.
- The receipt's authority is the verified server-side webhook; the mobile app pulls the resulting
  `PaymentVoucher`/`LedgerEntry` — no second posting path.

## Complexity Tracking

| Deviation | Why needed | Simpler alternative rejected because |
|---|---|---|
| Webhook resolves tenant from PSP notes (not `X-Workspace-ID`) and sets it at the controller | PSPs cannot send our JWT/header; tenant must be carried in PSP metadata | A JWT-authenticated webhook is impossible — the PSP is an external system |
| Webhook returns a bare 200/400 (not `ApiResponse<T>`) | PSPs expect a plain 2xx ack and retry on anything else | Wrapping in `ApiResponse` could cause PSPs to misinterpret and retry-storm |
| `collection` posts into `payment` rather than owning the ledger | Single ledger source of truth (spec 013 invariant) | A second ledger would risk the foot-to-zero guarantee and duplicate state |
