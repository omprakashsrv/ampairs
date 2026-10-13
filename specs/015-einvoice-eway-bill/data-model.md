# Phase 1 Data Model — GST E-Invoicing (IRN) & E-Way Bill

Derived from [spec.md](./spec.md) (incl. the 2026-06-28 clarifications) and [research.md](./research.md).
Grounded against the live `invoice` model (`invoice/.../domain/model/Invoice.kt`,
`InvoiceItem.kt`) and the existing `event` module (`InvoiceFinalizedEvent` / `InvoiceCancelledEvent`,
both keyed by `entityId = invoice.uid`).

All entities are **backend** (`einvoice` module) and extend `OwnableBaseDomain` (→ `uid`, `@TenantId
ownerId`, `createdAt`/`updatedAt` `Instant`, `active`). Mobile mirrors `EInvoiceDocument` + `EwayBill`
as **read-only** Room rows (pull-only). Timestamps are `Instant` → `TIMESTAMPTZ`/`TIMESTAMP`; money in
payload builders is `BigDecimal` scale 2 (never floating point in the IRP payload).

---

## 1. Entity: `EInvoiceDocument` (1:1 with an invoice)

The compliance record for one finalized invoice. The invoice document itself is **never modified**;
this is a sidecar joined by `invoiceUid`.

| Field | Type | Notes |
|---|---|---|
| `uid` | String (PK) | `OwnableBaseDomain`; prefix e.g. `EIN` |
| `ownerId` | String | `@TenantId` — workspace isolation |
| `invoiceUid` | String | FK→`invoice.uid`; **unique per `(ownerId, invoiceUid)`** |
| `invoiceNumber` | String | snapshot for display/audit |
| `irn` | String? | 64-char IRP hash; **unique when non-null**; null until generated |
| `ackNo` | String? | IRP acknowledgement number |
| `ackDate` | Instant? | IRP acknowledgement timestamp (basis for the 24h cancel window) |
| `signedInvoice` | String? (`TEXT`/`LONGTEXT`) | IRP-signed invoice JWS — legal artifact; **detail-only**, never in list DTOs |
| `signedQrCode` | String? (`TEXT`) | signed QR payload string (rendered to a QR bitmap client-side) |
| `irnStatus` | enum `IrnStatus` | `PENDING` / `GENERATED` / `FAILED` / `CANCELLED` |
| `failureReason` | String? | human-readable reason when `FAILED` (FR-010) |
| `gspProvider` | String | provider key used (e.g. `NIC_DIRECT`, `MASTERINDIA`) |
| `irpRequestPayload` | String? (`TEXT`) | last request sent to IRP — audit (FR-026); detail-only |
| `irpResponsePayload` | String? (`TEXT`) | last response from IRP — audit (FR-026); detail-only |
| `cancelReason` | enum `CancelReason`? | NIC reason code on cancellation |
| `cancelRemarks` | String? | free-text cancel remarks |
| `cancelledAt` | Instant? | when cancelled |

**Relationships**: 1:1 logical link to `invoice` (by `invoiceUid`, cross-module — joined by uid, not a
JPA FK to another module's table). 1:N to `EwayBill` (an invoice may have an e-way bill, occasionally
re-generated over time). `@NamedEntityGraph` bundles the active `EwayBill` for detail reads.

**Retention**: retained for as long as the parent invoice is retained (clarification — match invoice
retention policy). No independent purge schedule.

**Uniqueness / idempotency** (FR-008):
- Unique constraint `(owner_id, invoice_uid)` — at most one e-invoice doc per invoice.
- Unique constraint on `irn` (where non-null) — no duplicate IRN row.

### State machine — `IrnStatus`

```
            enqueue on InvoiceFinalizedEvent
  (none) ───────────────────────────────► PENDING
                                             │
              worker success / NIC 3029      │  validation/permanent reject
            ┌────────────────────────────────┼───────────────────────────┐
            ▼                                 ▼                           ▼
        GENERATED                          (retry within window)        FAILED
            │                                 │ window expires            │ manual generate (admin/owner)
            │ cancel within 24h of ackDate    └──────────► FAILED ────────┘ (re-enqueue → PENDING)
            ▼  (admin/owner + reason code)
        CANCELLED  ── blocks valid-e-invoice reprint (FR-019)
```

- Transient errors (portal down/timeout) keep the doc `PENDING` and retry with exponential backoff
  until the job's `windowExpiresAt` (default `ack/enqueue + 48h`), then → `FAILED` (clarification).
- Permanent/validation rejections (missing GSTIN/HSN, total mismatch beyond tolerance) → `FAILED`
  immediately, no retry.
- `InvoiceCancelledEvent` (invoice leaves INVOICED) → if an IRN exists and is within 24h, the system
  attempts IRN cancellation; the doc moves to `CANCELLED`.

---

## 2. Entity: `EwayBill` (linked to an invoice; off the IRN when present)

The transport-document record. Generated optionally, either bundled with the IRN or standalone later.

| Field | Type | Notes |
|---|---|---|
| `uid` | String (PK) | prefix e.g. `EWB` |
| `ownerId` | String | `@TenantId` |
| `invoiceUid` | String | FK→`invoice.uid` |
| `eInvoiceDocumentUid` | String? | link to the IRN doc when generated off the IRN |
| `ewbNo` | String? | e-way bill number (null until generated) |
| `ewbDate` | Instant? | generation timestamp (basis for the 24h cancel window) |
| `validUpto` | Instant? | validity expiry |
| `transporterId` | String? | transporter GSTIN/ID |
| `transporterName` | String? | |
| `transMode` | enum `TransMode` | `ROAD` / `RAIL` / `AIR` / `SHIP` |
| `vehicleNo` | String? | required for ROAD |
| `vehicleType` | enum `VehicleType` | `REGULAR` / `ODC` |
| `transDistance` | Int? | km |
| `transDocNo` | String? | transport document number |
| `transDocDate` | Instant? | transport document date |
| `ewbStatus` | enum `EwbStatus` | `GENERATED` / `UPDATED` / `CANCELLED` / `EXPIRED` |
| `failureReason` | String? | reason when generation failed |

**State machine — `EwbStatus`**:
```
(none) ──generate──► GENERATED ──update-vehicle (Part-B)──► UPDATED ──┐
                          │                                            │
                          │ cancel within 24h (admin/owner)           │ validity passes
                          ▼                                            ▼
                       CANCELLED                                     EXPIRED
```
- Cancel allowed only within 24h of `ewbDate` and not after in-transit verification (FR-018).
- `update-vehicle` and `extend-validity` are separate transitions; neither mints a new `ewbNo`.

---

## 3. Entity: `EInvoiceJob` (outbound retry queue)

Drives automatic, bounded-window retry. One active job per pending operation.

| Field | Type | Notes |
|---|---|---|
| `uid` | String (PK) | |
| `ownerId` | String | `@TenantId` |
| `invoiceUid` | String | target invoice |
| `jobType` | enum `JobType` | `GENERATE_IRN` / `CANCEL_IRN` (EWB jobs added in Phase 2) |
| `jobStatus` | enum `JobStatus` | `PENDING` / `IN_PROGRESS` / `DONE` / `FAILED` |
| `attemptCount` | Int | incremented each attempt |
| `nextAttemptAt` | Instant | exponential-backoff schedule; worker polls `nextAttemptAt <= now` |
| `windowExpiresAt` | Instant | enqueue + retry window (default 48h); past this → terminal `FAILED` |
| `lastError` | String? | last transient/permanent error captured |
| `requestId` | String | stable idempotency key carried to GSPs that support it |

**Worker** (`EInvoiceQueueWorker`, `@Scheduled`): polls `jobStatus IN (PENDING)` and
`FAILED`-but-`now < windowExpiresAt` with `nextAttemptAt <= now`; runs the get-by-doc idempotency
pre-check, builds + submits INV-01, persists results. Serialized per `invoiceUid`.

---

## 4. Entity: `EInvoiceCredential` (encrypted per-workspace GSP/IRP creds)

| Field | Type | Notes |
|---|---|---|
| `uid` | String (PK) | |
| `ownerId` | String | `@TenantId` — one (active) credential set per workspace |
| `gspProvider` | String | which provider these creds are for |
| `gstin` | String | the GSTIN these creds authenticate |
| `encryptedClientId` | String (`TEXT`) | AES-encrypted (KMS-backed key, env-provided) |
| `encryptedClientSecret` | String (`TEXT`) | AES-encrypted |
| `encryptedUsername` | String (`TEXT`) | GSTIN-scoped IRP username, encrypted |
| `encryptedPassword` | String (`TEXT`) | encrypted |
| `sessionToken` | String? (`TEXT`) | cached short-lived NIC session token |
| `sessionTokenExpiry` | Instant? | refresh-on-demand basis |

Never exposed by any API. Secrets are env-provided + encrypted at rest (FR-023, Principle XI).

---

## 5. Per-workspace settings (via the `setting` module)

Declared by `EInvoiceSettingDefinitions` (gated by the installed `einvoice` module), not hardcoded
(FR-001/002, research R11):

| Key | Type | Default | Meaning |
|---|---|---|---|
| `einvoice_enabled` | Boolean | `false` | attempt IRN registration for this workspace |
| `einvoice_provider` | String | (env default) | active GSP provider key |
| `einvoice_retry_window_hours` | Int | `48` | bounded retry window before `FAILED` (clarification) |
| `eway_enabled` | Boolean | `false` | allow e-way bill generation (Phase 2) |
| `eway_value_threshold` | Long | `50000` | consignment value above which EWB is expected (Phase 2) |

---

## 6. INV-01 field mapping (NIC e-invoice schema v1.1)

Built by `Inv01PayloadBuilder` from the live invoice — **no parallel data entry** (FR-006, research R6).
External PascalCase contract isolated with explicit `@JsonProperty` (constitution III exception).

| INV-01 path | Source (invoice model) | Rule |
|---|---|---|
| `Version` | constant `"1.1"` | |
| `TranDtls.SupTyp` | derived | `B2B` (B2C/sub-threshold skipped — FR-003) |
| `TranDtls.RegRev` | default `N` | reverse charge (future: RCM, spec 026) |
| `DocDtls.Typ` | constant `INV` | tax invoice |
| `DocDtls.No` | `invoice.invoiceNumber` | gap-free per series (spec 012) |
| `DocDtls.Dt` | `invoice.invoiceDate` | formatted `dd/MM/yyyy` |
| `SellerDtls.Gstin` | `invoice.sellerGst` | seller GSTIN snapshot |
| `SellerDtls.*` | `invoice.sellerName` / `sellerAddress` | name/address snapshot |
| `SellerDtls.Stcd` | from `invoice.sellerPlaceOfSupply` | state code |
| `BuyerDtls.Gstin` | `invoice.customerGst` | buyer GSTIN |
| `BuyerDtls.*` | `invoice.customerName` / `billingAddress` | |
| `BuyerDtls.Pos` | `invoice.placeOfSupply` → state code | place of supply |
| `ItemList[].HsnCd` | `InvoiceItem.taxCode` → HSN | **must be present** else validation FAIL |
| `ItemList[].Qty` / `Unit` | `InvoiceItem.quantity` / `unitId` | |
| `ItemList[].UnitPrice` / `TotAmt` | `InvoiceItem.sellingPrice` / `basePrice` | |
| `ItemList[].GstRt` | `InvoiceItem.taxInfos[].percentage` | |
| `ItemList[].IgstAmt` **or** `CgstAmt`+`SgstAmt` | `InvoiceItem.taxInfos[].value` | **IGST iff `placeOfSupply != sellerPlaceOfSupply`**, else CGST+SGST split |
| `ValDtls.AssVal` | `invoice.basePrice` | assessable value |
| `ValDtls.IgstVal` / `CgstVal` / `SgstVal` | `invoice.taxInfos` aggregated | per intra/inter-state |
| `ValDtls.RndOffAmt` | computed residue | absorbs rounding so `TotInvVal` foots (research R9) |
| `ValDtls.TotInvVal` | `invoice.totalCost` | must reconcile within NIC ±1 tolerance |

Money is converted from the invoice's legacy `Double` to `BigDecimal` scale 2 (half-up) **once** at
build time. Validation before submit: GSTIN format, HSN present per line, PIN codes, `TotInvVal`
tolerance — failures → `FAILED` with reason, no retry (research R9, FR-006).

---

## 7. Mobile mirror (read-only)

`feature/einvoice` Room (workspace-scoped DB `einvoice`) mirrors `EInvoiceDocument` and `EwayBill` for
**display only** via two pull-only `SyncDelegate`s. No `synced=false` writes, no `markPendingPush`.
`signedQrCode` is stored as a string and rendered to a bitmap offline. `signedInvoice`/audit payloads
are **not** synced to devices (detail-only, server-side ACL).
