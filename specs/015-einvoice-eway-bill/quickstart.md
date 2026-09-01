# Quickstart — Exercise IRN & E-Way Bill against the NIC sandbox

Goal: validate the end-to-end e-invoice pipeline (finalize → queue → INV-01 build → IRP submit →
IRN/QR persisted → pull/display) and the bounded-retry / cancellation behaviour, using the **NIC
sandbox** before any production GSP. This is a developer/QA walkthrough, not a user manual.

## Prerequisites

- Backend running with Docker (Testcontainers for tests): `cd /home/user/ampairs`.
- NIC e-invoice **sandbox** credentials (GSTIN-scoped username/password + GSP client id/secret) provided
  via environment variables — never committed (constitution XI). Stored encrypted in
  `einvoice_credential` for the test workspace.
- A workspace with `einvoice_enabled = true` and `einvoice_provider` set (e.g. `NIC_DIRECT`) via the
  `setting` module.
- The `einvoice` module wired: `settings.gradle.kts` includes `einvoice`; `ampairs_service/build.gradle.kts`
  has `implementation(project(":einvoice"))` and `"einvoice"` in `migrationModules`.

## 0. Migrations

Pick the next global Flyway version (current max is `V1.0.112` — **verify** before choosing):
```bash
./gradlew :ampairs_service:flywayInfo
```
Write `V1.0.113__create_einvoice_tables.sql` (or the next free number) in **both**
`einvoice/src/main/resources/db/migration/postgresql/` and `.../mysql/`. Apply:
```bash
./gradlew :ampairs_service:flywayMigrate
```

## 1. Build & test the module

```bash
./gradlew :einvoice:compileKotlin
./gradlew :einvoice:test          # INV-01 golden tests, idempotency, cancel-window, retry-window, authz
```
Backend target coverage ≥80% on `Inv01PayloadBuilder`, idempotency, cancellation-window and
retry-window logic.

## 2. Finalize an invoice → automatic IRN

1. Create and finalize a **B2B** invoice (status → `INVOICED`) with a valid buyer GSTIN and HSN codes
   on every line. This publishes `InvoiceFinalizedEvent`.
2. The listener upserts `EInvoiceDocument(PENDING)` and enqueues a `GENERATE_IRN` job.
3. The `@Scheduled` `EInvoiceQueueWorker` picks it up, runs the get-by-doc idempotency pre-check, builds
   INV-01, submits to the NIC sandbox, and persists `irn` / `ack_no` / `ack_date` / `signed_qr_code` /
   `signed_invoice`, status → `GENERATED`.

Verify via detail:
```bash
curl -H "X-Workspace-ID: $WS" $BASE/einvoice/v1/documents/$INVOICE_UID | jq '.data.irn_status, .data.irn'
```

## 3. Pull feed (what the mobile app mirrors)

```bash
curl -H "X-Workspace-ID: $WS" "$BASE/einvoice/v1/documents/sync?size=50" | jq '.data.content[] | {invoice_uid, irn_status, irn}'
```
Confirm `signed_invoice` and audit payloads are **absent** here (detail-only), but `signed_qr_code` is
present so a client can render the QR offline.

## 4. Idempotency / duplicate IRN

Re-trigger generation for the same invoice:
```bash
curl -X POST -H "X-Workspace-ID: $WS" $BASE/einvoice/v1/documents/$INVOICE_UID/generate
```
Expect the **same** IRN returned (NIC 3029 duplicate handled as success). No second `EInvoiceDocument`
row, no new IRN.

## 5. Bounded retry on outage

Point the provider at an unreachable sandbox URL, finalize an invoice → doc stays `PENDING`, job
`attempt_count` climbs with exponential `next_attempt_at`. Restore connectivity within 48h → it
completes automatically. Simulate window expiry (set `window_expires_at` in the past) → next poll moves
it to `FAILED` with reason; the doc appears in:
```bash
curl -H "X-Workspace-ID: $WS" $BASE/einvoice/v1/documents/failed | jq '.data.content[] | {invoice_uid, failure_reason}'
```

## 6. Validation failure (permanent, no retry)

Finalize an invoice missing a line HSN (or with a `TotInvVal` mismatch beyond ±1). Generation →
`FAILED` immediately (no retry attempts), with a human-readable `failure_reason` (e.g. "HSN missing on
line 2"). The invoice itself stays valid locally.

## 7. Cancellation window

```bash
# within 24h of ack_date — admin/owner token required
curl -X POST -H "X-Workspace-ID: $WS" -H "Content-Type: application/json" \
  -d '{"cancel_reason":"DUPLICATE","cancel_remarks":"dup"}' \
  $BASE/einvoice/v1/documents/$INVOICE_UID/cancel | jq '.data.irn_status'   # → "CANCELLED"
```
Repeat after simulating >24h → expect `409` with a message pointing to the credit-note remedy.
Confirm a non-admin token gets `403`.

## 8. E-Way bill (Phase 2)

```bash
curl -X POST -H "X-Workspace-ID: $WS" -H "Content-Type: application/json" \
  -d '{"invoice_uid":"'$INVOICE_UID'","trans_mode":"ROAD","vehicle_no":"KA01AB1234","trans_distance":320,"vehicle_type":"REGULAR"}' \
  $BASE/einvoice/v1/eway-bills | jq '.data.ewb_no, .data.valid_upto'
```
Then exercise `update-vehicle` (no new `ewb_no`) and `cancel` (within 24h).

## 9. Mobile display (ampairs-app)

With the workspace synced, the invoice detail screen shows IRN / ack / QR (rendered from
`signed_qr_code`) and an EWB chip; the invoice PDF/print embeds the QR + IRN + EWB no. Verify the QR
renders and prints with the **device offline** (the QR string was pulled and stored). Compile gates:
```bash
./gradlew :feature:einvoice:check
./gradlew shared:compileKotlinIosSimulatorArm64 androidApp:compileDebugKotlinAndroid desktopApp:compileKotlin
```
