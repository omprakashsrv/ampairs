# Phase 0 Research — GST E-Invoicing (IRN) & E-Way Bill

All Technical Context unknowns are resolved below. Each item: **Decision · Rationale · Alternatives
considered**. These supersede the inline assumptions in `spec.md` with concrete technical choices.
The feature bolts GST e-invoice (IRN generation via the IRP) and e-way bill generation onto finalized
invoices in the existing `invoice` module without reshaping the invoice document model.

---

## R1. Module boundary — new `einvoice` module vs extend `invoice`

- **Decision**: A **new backend bounded context `einvoice`** owns IRN/e-way-bill state, the GSP/IRP
  provider abstraction, the outbound queue and the schema (INV-01 / EWB) builders. It reads finalized
  invoices through the existing `InvoiceService` public interface and reacts to `InvoiceFinalizedEvent`
  / `InvoiceCancelledEvent` (the same events `payment` already consumes). The `invoice` module is
  touched only additively (it already publishes those events).
- **Rationale**: E-invoicing is a distinct compliance concern with its own external integrations,
  secrets, retry lifecycle and persistence — exactly the "new bounded context gets its own module"
  rule (Principle IX). Keeping it out of `invoice` avoids bloating the document model with GSP
  credentials, ack numbers and signed payloads, and lets the IRP integration evolve independently.
- **Alternatives considered**: Add `irn`/`signed_qr`/`ack_no` columns + GSP client directly on the
  `invoice` module (rejected — couples a regulated external integration to the core document context,
  and would force every invoice reader to drag in GSP config). A shared `compliance` mega-module
  (rejected — premature; e-way bill and e-invoice share enough to live together but not with future
  TDS/RCM).

## R2. GSP/IRP provider abstraction

- **Decision**: A single `EInvoiceProvider` port interface (`generateIrn`, `cancelIrn`,
  `generateEwayBill`, `cancelEwayBill`, `getIrnByDoc`, `authenticate`) with pluggable implementations
  per GSP (e.g. `MasterIndiaProvider`, `ClearTaxProvider`, `NicDirectProvider` for the NIC sandbox).
  Provider selection is per-workspace config resolved by an `EInvoiceProviderResolver`; credentials
  (GSP client id/secret, GSTIN-scoped username/password, the NIC API auth token) come from environment
  + an encrypted per-workspace credential row, never from source. The IRP itself (NIC) is the source of
  truth for the IRN; the GSP is just the transport.
- **Rationale**: India has many GSPs over one NIC IRP; a port lets us swap GSPs (pricing, uptime, SLA)
  without touching call sites, exactly mirroring how `notification` abstracts SMS providers
  (`NotificationProvider` with MSG91 primary / SNS fallback) and how `payment` abstracts numbering.
  Each GSP differs only in auth handshake, base URL and minor field framing around the common NIC
  INV-01 / EWB JSON.
- **Alternatives considered**: Hardcode one GSP (rejected — vendor lock-in, no fallback for an outage
  that would block compliant billing). Call NIC directly only (rejected — production access is
  GSP-mediated; direct NIC is sandbox-only). Abstract at the HTTP-client level (rejected — leaks
  provider-specific auth/session-token quirks to callers).

## R3. Where IRN/e-way state lives — on the invoice or a sidecar entity

- **Decision**: A **sidecar `EInvoiceDocument`** entity (1:1 with an invoice `uid`) holds `irn`,
  `ackNo`, `ackDate`, `signedInvoice` (the IRP-signed JWS), `signedQrCode` (the signed QR string),
  `irnStatus`, `gspProvider`, `irpRequestPayload`/`irpResponsePayload` (audit), `cancelReason`,
  `cancelledAt`. A separate **`EwayBill`** entity (1:1 with the invoice, optionally many over time)
  holds `ewbNo`, `ewbDate`, `validUpto`, transporter/vehicle/distance fields, `ewbStatus`. The invoice
  document stays untouched; clients join by invoice `uid`.
- **Rationale**: The invoice is a frozen tax document; compliance artifacts have their own lifecycle
  (queued → generated → cancelled) and large signed blobs that should not bloat the invoice table or
  its `/sync` feed. A sidecar keeps the invoice model stable and the compliance payloads queryable and
  separately syncable.
- **Alternatives considered**: Columns on `invoice` (rejected — see R1; also drags signed JWS blobs
  into invoice sync). Storing only the QR and discarding the signed payload (rejected — the signed
  invoice JWS is the legal artifact and must be retained for audit and reprint).

## R4. Offline behaviour — IRN generation needs connectivity

- **Decision**: IRN and e-way bill generation are **online-only operations queued for retry**. On
  invoice finalize, the client/server records the invoice as `EInvoiceDocument(irnStatus = PENDING)`
  and enqueues an `EInvoiceJob`. A backend worker (`@Scheduled` poller over a `PENDING`/`FAILED` queue
  with exponential backoff) drives generation when connectivity and GSP are available. The mobile app
  treats IRN as **pull-only** state: it never authors an IRN, it only displays whatever the backend has
  produced. The local Room mirror shows `PENDING`/`GENERATED`/`FAILED` so a field user sees "IRN
  pending — will generate when online".
- **Rationale**: The IRP mints the IRN; it cannot be generated offline by construction. Queue + retry
  is the only correct model — analogous to how `notification` queues with retry and how the brief's UPI
  webhooks need connectivity. Making the app author IRNs would be wrong (no offline authority) and
  unsafe (duplicate IRN attempts). Server-owned generation also centralizes the single GSP session and
  rate limits.
- **Alternatives considered**: Generate IRN synchronously inside the finalize call (rejected — blocks
  finalize on a slow/down external system; a finalize must always succeed offline-first). Let the app
  call the IRP directly (rejected — GSP credentials must never ship to devices; no per-device rate
  control). Block invoice finalize until IRN exists (rejected — breaks offline-first and the existing
  finalize → ledger flow).

## R5. Idempotency & duplicate-IRN protection

- **Decision**: One IRN per invoice document is enforced two ways: (1) a **unique constraint** on
  `EInvoiceDocument(owner_id, invoice_uid)` and on `irn`; (2) **before generating**, the worker calls
  the IRP `GET IRN by document` (Get IRN Details by Doc Type/No/Date) — if NIC already has an IRN for
  this GSTIN+docno+date+fy, persist that response instead of re-submitting. The job carries a stable
  `requestId` so a GSP that supports idempotency keys dedupes server-side. NIC's own "duplicate IRN"
  error response (3029) is treated as success and the returned existing IRN is stored.
- **Rationale**: Retries (network timeouts where NIC actually persisted the IRN) are the classic source
  of duplicate-submission bugs; the IRP rejects a second IRN for the same doc but returns the original,
  so we must parse that path as success. This matches the idempotency rigor required for the UPI/webhook
  features (feature 016) and avoids the most common e-invoice integration failure.
- **Alternatives considered**: Trust local state only (rejected — a lost ack leaves us unsure; must
  reconcile against NIC). Generate a fresh IRN per attempt (rejected — NIC forbids it; produces 3029
  loops).

## R6. INV-01 JSON schema generation from the existing invoice model

- **Decision**: An `Inv01PayloadBuilder` maps the existing `Invoice` + `InvoiceItem` to the NIC
  **INV-01 e-invoice schema v1.1** (`Version`, `TranDtls`, `DocDtls`, `SellerDtls`, `BuyerDtls`,
  `ItemList`, `ValDtls`). It reuses the invoice's already-modelled GST fields: `placeOfSupply` /
  `sellerPlaceOfSupply` decide IGST vs CGST+SGST, `sellerGst`/`customerGst` populate the party GSTINs,
  `taxInfos`/line `totalTax` populate `ItemList` tax amounts, `totalCost`/`basePrice`/`totalTax`
  populate `ValDtls`. Money is converted to the IRP's expected 2-dp `BigDecimal` once at build time;
  a `ValDtls.RndOffAmt` line absorbs residue so `TotInvVal` foots exactly.
- **Rationale**: The invoice already models everything INV-01 needs (R-context in CLAUDE: GST
  CGST/SGST/IGST, place-of-supply, gap-free numbers). The only work is a field mapper + strict
  validation (HSN present, GSTIN format, pin codes, `TotInvVal` tolerance ±1). Building from the live
  invoice avoids a parallel data model.
- **Alternatives considered**: A separate e-invoice data-entry form (rejected — duplicates the invoice;
  the brief says integrate with the existing model). Send the invoice DTO as-is (rejected — INV-01 has
  a strict required-field/format contract NIC validates; an unmapped payload is rejected with cryptic
  error codes).

## R7. E-way bill — relationship to IRN and its extra fields

- **Decision**: E-way bill generation is a **separate, optional step** that can be done (a) bundled
  with IRN generation when `GenerateEwb=true` and transport details are present, or (b) standalone
  later via `POST /einvoice/v1/eway-bills`. It captures transporter id/name, `transMode`
  (road/rail/air/ship), vehicle no, `transDistance` (km), `transDocNo`/date, and `vehicleType`
  (regular/ODC). When an IRN exists, the e-way bill is generated **off the IRN** (NIC's "Generate EWB
  by IRN") so the document detail is not re-keyed.
- **Rationale**: E-way bill is required only above the consignment-value threshold and only when goods
  move; many compliant invoices need an IRN but no EWB (services, counter sales). Generating EWB off
  the IRN is the supported, less error-prone NIC path. Keeping it standalone supports the common case of
  generating the EWB when the vehicle is assigned, hours after the invoice.
- **Alternatives considered**: Always generate EWB with IRN (rejected — wrong for sub-threshold /
  no-movement invoices; forces fake transport data). Model EWB inside `EInvoiceDocument` (rejected — its
  own lifecycle, validity window and Part-B vehicle updates justify a separate entity).

## R8. Cancellation windows & lifecycle

- **Decision**: Encode NIC's hard rules as state-machine guards. **IRN cancellation** is allowed only
  within **24 hours** of `ackDate` and only with a NIC cancel reason code (1=duplicate, 2=data entry
  error, etc.); after 24h the only remedy is a credit note (out of scope, points to `payment`
  adjustments / a future credit-note flow). **E-way bill cancellation** is allowed within **24 hours**
  of generation and not after the goods have been verified in transit; **Part-B/vehicle update** and
  **validity extension** are separate transitions. State machine:
  `PENDING → GENERATED → CANCELLED` (IRN); `NONE → GENERATED → (UPDATED)* → CANCELLED/EXPIRED` (EWB).
  Once IRN is cancelled, the linked invoice is flagged so the UI blocks reprint of a void e-invoice.
- **Rationale**: These windows are statutory; enforcing them client-and-server-side prevents illegal
  cancel attempts that NIC would reject anyway, and gives the user a correct, early error. Tying invoice
  display to IRN status prevents handing a customer a cancelled e-invoice.
- **Alternatives considered**: Allow cancel any time and rely on NIC rejection (rejected — poor UX,
  wastes GSP calls, and risks inconsistent local state). Auto-issue a credit note on late cancel
  (rejected — credit notes are a separate feature with their own numbering/ledger impact).

## R9. Money & rounding parity with INV-01 tolerance

- **Decision**: Backend builds the INV-01/EWB payload in **`BigDecimal` scale 2, half-up**, validating
  NIC's `TotInvVal` tolerance (line value sums must reconcile within ±1 of the header). The invoice's
  legacy `Double` totals are converted **once** at payload build. Mobile never computes compliance
  money — it displays server values as strings.
- **Rationale**: NIC validates arithmetic on the payload; floating-point drift across many lines fails
  validation. Converting once (mirroring spec 013 R5) avoids accumulation error. Keeping mobile
  display-only sidesteps minor-unit↔INV-01 conversion entirely.
- **Alternatives considered**: Send the invoice's `Double` directly (rejected — precision/tolerance
  failures). Round per line independently (rejected — header may not reconcile without an explicit
  round-off line).

## R10. Mobile integration — display & print only

- **Decision**: A small **`feature/einvoice` app module** (offline-first, workspace-scoped Room DB)
  that is **pull-only** for `EInvoiceDocument` and `EwayBill` via two read `SyncDelegate`s on the
  canonical `/sync` contract (no client-authored push). The invoice detail screen shows IRN, ack no,
  the signed QR (rendered from `signedQrCode`), and an EWB chip; the invoice PDF/print template embeds
  the QR + IRN + EWB no. Transporter/vehicle entry for a standalone EWB is a **command** (`POST` action,
  online-only), not a synced entity. A "Generate IRN now" button triggers the backend action when
  online; otherwise the UI shows the pending state.
- **Rationale**: IRN/EWB are server-authored compliance artifacts; the app's job is to surface and
  print them, not to own them. Pull-only delegates fit the existing sync engine; the QR is just a
  string the app renders to a QR bitmap locally (no connectivity needed once pulled). The print path
  reuses the existing invoice PDF/`printing` template.
- **Alternatives considered**: Author IRN state on-device and push (rejected — violates offline
  authority; IRN can't be minted offline). Re-fetch QR from server at print time (rejected — must print
  offline; store the QR string locally).

## R11. Threshold & applicability gating (₹2 Cr from Oct 2025)

- **Decision**: E-invoicing applicability is a **per-workspace setting** (`einvoice_enabled`,
  `einvoice_provider`, `einvoice_aato_threshold`, `eway_enabled`, `eway_value_threshold` default
  ₹50,000) declared via the existing `setting` module (an `EInvoiceSettingDefinitions` provider gated
  by an installed `einvoice` module), not a hardcoded turnover check. The backend will *not* attempt
  IRN for a workspace that hasn't enabled it; per-invoice it can skip B2C/sub-threshold documents.
- **Rationale**: Applicability depends on the business's Aggregate Annual Turnover (AATO), which the app
  doesn't reliably know; the threshold has stepped down repeatedly (₹10Cr→₹5Cr→₹2Cr Oct 2025) so it must
  be config, not code. Reusing `setting` matches how `payment`/`invoice` expose toggles.
- **Alternatives considered**: Hardcode the ₹2Cr threshold and auto-enable (rejected — wrong for
  exempt categories, SEZ, and changes every budget). Always attempt IRN (rejected — would error for
  non-applicable B2C/sub-threshold invoices).

## R12. Security of GSP credentials & signed payloads

- **Decision**: Per-workspace GSP/IRP credentials are stored encrypted (AES via a KMS-backed key,
  env-provided) in an `einvoice_credential` row, never in `keys/` or source; the NIC session
  auth-token (short-lived) is cached server-side per GSTIN with its expiry and refreshed on demand.
  Signed payloads (`signedInvoice` JWS, `signedQrCode`) are retained but never exposed in list DTOs —
  only on the single-document detail endpoint. Webhooks/callbacks from a GSP (if used for async
  results) are signature-verified.
- **Rationale**: GST credentials are sensitive (rule 10-security); they must stay server-side and
  encrypted, and the signed legal artifacts must be access-controlled. Session-token caching avoids
  re-authenticating per call (NIC rate-limits auth).
- **Alternatives considered**: Plaintext credentials in config (rejected — security rule violation).
  Re-auth on every IRP call (rejected — NIC throttles; wastes the 6-hour session token).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Module placement | New `einvoice` bounded context; reads invoice via service + events (R1) |
| GSP/IRP abstraction | `EInvoiceProvider` port + per-workspace resolver, env/encrypted creds (R2) |
| Where IRN/EWB state lives | Sidecar `EInvoiceDocument` + `EwayBill`, invoice untouched (R3) |
| Offline behaviour | Online-only, queued + retry; mobile pull-only display (R4) |
| Idempotency / duplicate IRN | Unique constraint + IRP get-by-doc pre-check; 3029 = success (R5) |
| INV-01 generation | `Inv01PayloadBuilder` from existing GST fields; round-off line (R6) |
| E-way bill | Separate optional entity; generate off IRN; transport fields (R7) |
| Cancellation windows | 24h state-machine guards + NIC reason codes (R8) |
| Money / rounding | `BigDecimal` scale 2 half-up, ±1 tolerance, convert once (R9) |
| Mobile integration | `feature/einvoice` pull-only + QR render + PDF embed (R10) |
| Threshold gating | Per-workspace `setting` flags, not hardcoded turnover (R11) |
| Credential security | Encrypted per-workspace creds, cached NIC token, signed-blob ACL (R12) |
