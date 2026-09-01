# Contract — Pull-only Sync Feeds

Server-authored entities. **Pull only** — there is no `POST .../sync` push half (see
[README](./README.md)). The mobile `EInvoiceDocumentSyncDelegate` / `EwayBillSyncDelegate` mirror these
for read-only display. Query params are `SNAKE_CASE`, matching the canonical pull feed.

---

## GET `/einvoice/v1/documents/sync`

Incremental pull of e-invoice documents (includes cancelled rows so cancellation propagates).

**Query params**

| Param | Type | Notes |
|---|---|---|
| `last_sync` | string (ISO-8601) | sent only when non-blank |
| `page` | int | default 0 |
| `size` | int | default 100, max 500 |
| `sort_by` | string | default `updatedAt` |
| `sort_dir` | string | default `ASC` |

**Response** `ApiResponse<PageResponse<EInvoiceDocumentResponse>>`

`EInvoiceDocumentResponse` (list-safe — **no** signed payload / audit blobs):
```json
{
  "uid": "EIN20261028...",
  "invoice_uid": "INV20261028...",
  "invoice_number": "INV/2026/00042",
  "irn": "a1b2c3...64hex",
  "ack_no": "112410000123",
  "ack_date": "2026-10-28T10:15:00Z",
  "signed_qr_code": "<signed-qr-string>",
  "irn_status": "GENERATED",
  "failure_reason": null,
  "gsp_provider": "NIC_DIRECT",
  "cancel_reason": null,
  "cancelled_at": null,
  "updated_at": "2026-10-28T10:15:01Z",
  "active": true
}
```
> `signed_invoice`, `irp_request_payload`, `irp_response_payload` are **never** in this feed — they are
> detail-only and access-controlled (FR-024). `signed_qr_code` IS included so the client can render the
> QR offline.

---

## GET `/einvoice/v1/eway-bills/sync` (Phase 2)

Same param shape. **Response** `ApiResponse<PageResponse<EwayBillResponse>>`:
```json
{
  "uid": "EWB20261028...",
  "invoice_uid": "INV20261028...",
  "e_invoice_document_uid": "EIN20261028...",
  "ewb_no": "381012345678",
  "ewb_date": "2026-10-28T11:00:00Z",
  "valid_upto": "2026-10-30T23:59:59Z",
  "transporter_name": "ABC Logistics",
  "trans_mode": "ROAD",
  "vehicle_no": "KA01AB1234",
  "vehicle_type": "REGULAR",
  "trans_distance": 320,
  "ewb_status": "GENERATED",
  "updated_at": "2026-10-28T11:00:01Z",
  "active": true
}
```

---

## GET `/einvoice/v1/documents/{invoiceUid}` (detail)

Single-document detail — the **only** place signed/audit payloads are exposed (access-controlled).
**Response** `ApiResponse<EInvoiceDocumentDetailResponse>` = the list shape **plus** `signed_invoice`,
`irp_request_payload`, `irp_response_payload`, and the linked e-way bill(s).
