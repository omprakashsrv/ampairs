# Contract — Online Commands & Failed-Document List

These are explicit, **online-only** actions (never offline-authored). All mutating commands require the
caller to be a **workspace admin/owner** (clarification 2026-06-28) and the workspace to have
e-invoicing enabled.

---

## POST `/einvoice/v1/documents/{invoiceUid}/generate`

Manually trigger (or retry) IRN registration for a finalized invoice. Idempotent — if an IRN already
exists, returns it; if the IRP reports a duplicate (NIC error 3029), the existing IRN is stored and
returned as success (FR-008).

- **Auth**: admin/owner.
- **Body**: none.
- **Response** `201` `ApiResponse<EInvoiceDocumentResponse>` (status `GENERATED`, or `PENDING` if
  queued, or `FAILED` with `failure_reason`).
- **Errors**: `403` not admin/owner or e-invoicing disabled; `404` invoice not found or not finalized;
  `422` IRP validation rejection (reason surfaced).

---

## POST `/einvoice/v1/documents/{invoiceUid}/cancel`

Cancel a generated e-invoice within 24h of `ack_date`.

- **Auth**: admin/owner.
- **Body**:
```json
{ "cancel_reason": "DUPLICATE", "cancel_remarks": "duplicate of INV/2026/00041" }
```
  `cancel_reason` ∈ NIC reason codes (`DUPLICATE`, `DATA_ENTRY_ERROR`, `ORDER_CANCELLED`, `OTHER`).
- **Response** `200` `ApiResponse<EInvoiceDocumentResponse>` (status `CANCELLED`).
- **Errors**: `409` window closed (>24h — message points to credit-note remedy, FR-017); `404` no IRN;
  `403` not admin/owner.

---

## GET `/einvoice/v1/documents/failed`

Aggregate list of invoices whose e-invoice (or e-way) registration is `FAILED`, each with its reason —
so stuck documents are never silently missed (clarification 2026-06-28, FR-011a).

- **Auth**: any workspace member.
- **Query**: `page`, `size`, `sort_by` (default `updatedAt`), `sort_dir`.
- **Response** `200` `ApiResponse<PageResponse<EInvoiceDocumentResponse>>` filtered to
  `irn_status = FAILED` (and, Phase 2, e-way `FAILED`).

---

## E-Way Bill commands (Phase 2)

### POST `/einvoice/v1/eway-bills`
Generate an e-way bill, off the IRN when one exists (so document detail is not re-keyed).
- **Auth**: admin/owner. **Body**:
```json
{
  "invoice_uid": "INV20261028...",
  "trans_mode": "ROAD",
  "transporter_id": "29ABCDE1234F1Z5",
  "transporter_name": "ABC Logistics",
  "vehicle_no": "KA01AB1234",
  "vehicle_type": "REGULAR",
  "trans_distance": 320,
  "trans_doc_no": "LR-558",
  "trans_doc_date": "2026-10-28"
}
```
- **Response** `201` `ApiResponse<EwayBillResponse>`.
- **Errors**: `400` missing transport fields for the chosen mode; `403`/`404` as above.

### POST `/einvoice/v1/eway-bills/{uid}/cancel`
Cancel within 24h of `ewb_date`, not after in-transit verification. Body: `{ "reason": "..." }`.
`409` when the window is closed.

### POST `/einvoice/v1/eway-bills/{uid}/update-vehicle`
Part-B vehicle update — does **not** mint a new `ewb_no`. Body: `{ "vehicle_no": "...", "reason": "..." }`.

### POST `/einvoice/v1/eway-bills/{uid}/extend-validity`
Extend validity where the regulation permits. Body: transport/remaining-distance fields.

All four are **admin/owner only**.

---

## Event-driven (no HTTP caller)

| Trigger | Effect |
|---|---|
| `InvoiceFinalizedEvent` (invoice → INVOICED) | upsert `EInvoiceDocument(PENDING)` + enqueue `GENERATE_IRN` job (only if `einvoice_enabled` and the invoice is eligible — B2B, not sub-threshold) |
| `InvoiceCancelledEvent` (invoice leaves INVOICED) | if an IRN exists within 24h, enqueue `CANCEL_IRN`; otherwise flag for credit-note follow-up |
