# API Contracts — 029 Ecom Buyer Account (invoices, statement, order↔invoice)

All endpoints:
- Base path prefix: `/api` (global) + `/v1/ecom/account`.
- Auth: `@PreAuthorize("isAuthenticated()")`, JWT principal; **no** `X-Workspace-ID` (exempted in `SessionUserFilter`).
- Tenant: resolved from `storefront_slug` → `storefront.ownerId`, set via `TenantContextHolder` in try/finally at the controller.
- Party: resolved via `EcomCustomerService.resolveLinkedCustomerId(userId, customer_id?)` → `partyUid`.
- Envelope: `ApiResponse<T>` always; lists use `PageResponse<T>`; JSON is global SNAKE_CASE.
- Errors: unlinked buyer → **403** `NOT_LINKED`; wrong-party / draft / missing document → **404**. Bubble to `GlobalExceptionHandler` (no controller try/catch for business errors).

Common query params: `storefront_slug` (required), `customer_id` (optional; honored only if the login
is genuinely linked to it, else the login's default account).

---

## 1. `GET /v1/ecom/account/invoices` — buyer invoice list (Phase 1a)

Paginated, finalized-only, newest first.

**Query**: `storefront_slug`, `customer_id?`, `page?` (default 0), `size?` (default 20)

**200** → `ApiResponse<PageResponse<BuyerInvoiceSummary>>`
```json
{
  "success": true,
  "data": {
    "content": [
      { "invoice_uid": "INV20260815ab12", "invoice_number": "INV-00042",
        "invoice_date": "2026-08-15T10:00:00Z", "status": "Unpaid",
        "total": 9207.50, "order_ref": "ECO-00007" }
    ],
    "page_number": 0, "page_size": 20, "total_elements": 1, "total_pages": 1,
    "first": true, "last": true, "has_next": false, "has_previous": false, "empty": false
  },
  "error": null
}
```
- `order_ref` is the buyer-facing `EcomOrder.ecomOrderRef`/`orderNumber` (controller-resolved from `orderRefId`), `null` for non-ecom invoices.
- `status` is the buyer-facing payment state — `"Unpaid"` while the invoice carries an outstanding balance (its uid is in `/outstanding`'s `open_bills[].bill_uid`), else `"Paid"`. Composed by the ecom controller from the party ledger, not the `invoice` module (OQ-6).

**403** `NOT_LINKED` — login not linked to any account.

---

## 2. `GET /v1/ecom/account/invoices/{invoiceUid}` — invoice detail (Phase 1a)

**Query**: `storefront_slug`, `customer_id?`

**200** → `ApiResponse<BuyerInvoiceDetail>`
```json
{ "success": true, "data": {
    "invoice_uid": "INV20260815ab12", "invoice_number": "INV-00042",
    "invoice_date": "2026-08-15T10:00:00Z", "status": "Unpaid", "order_ref": "ECO-00007",
    "lines": [ { "description": "Widget A", "quantity": 10, "unit_price": 900.00, "line_total": 9000.00 } ],
    "subtotal": 9000.00, "tax_total": 207.50, "total": 9207.50
  }, "error": null }
```

**404** — invoice not found, belongs to another party (`invoice.customerId != partyUid`), or is a draft.
(404 not 403 — do not confirm the invoice exists in another account.)

---

## 3. `GET /v1/ecom/account/orders/{ecomOrderRef}/invoices` — invoices for an order (Phase 1a)

Resolves the order via `getCustomerOrder(partyUid, ecomOrderRef)` (existing ownership re-check), then
returns invoices where `orderRefId == order.managementOrderRef`.

**Query**: `storefront_slug`, `customer_id?`

**200** → `ApiResponse<List<BuyerInvoiceSummary>>` — `[]` if none yet or `managementOrderRef` still null.

**404** — order not found or not owned by the resolved party.

---

## 4. `GET /v1/ecom/account/orders/{ecomOrderRef}` — order detail (EXISTING, extended, Phase 1a)

The existing response gains an `invoices` array (same `BuyerInvoiceSummary` shape) so US3 needs one
round-trip.

**200** → `ApiResponse<EcomOrderResponse>` with added:
```json
{ "invoices": [ { "invoice_uid": "INV20260815ab12", "invoice_number": "INV-00042",
                  "invoice_date": "2026-08-15T10:00:00Z", "status": "Unpaid",
                  "total": 9207.50, "order_ref": "ECO-00007" } ] }
```
Empty array when no invoice has been raised for the order.

---

## 5. `GET /v1/ecom/account/outstanding` — balance + open bills + aging (Phase 1b)

**Query**: `storefront_slug`, `customer_id?`

**200** → `ApiResponse<BuyerOutstandingResponse>`
```json
{ "success": true, "data": {
    "current_balance": 15420.00, "balance_direction": "DR",
    "open_bills": [ { "bill_uid": "INV20260815ab12", "bill_no": "INV-00042", "bill_date": "2026-08-15T10:00:00Z",
                      "total": 9207.50, "outstanding": 9207.50,
                      "due_date": "2026-09-14T10:00:00Z", "days_overdue": 0, "aging_bucket": "0-30" } ],
    "aging": [ { "label": "0-30", "amount": 9207.50 }, { "label": "31-60", "amount": 6212.50 } ]
  }, "error": null }
```

**403** `NOT_LINKED`.

---

## 6. `GET /v1/ecom/account/statement` — running statement (Phase 1b)

**Query**: `storefront_slug`, `customer_id?`, `from?` (ISO-8601, default = account opening), `to?` (default = now)

**200** → `ApiResponse<BuyerStatementResponse>`
```json
{ "success": true, "data": {
    "from": null, "to": "2026-08-20T00:00:00Z",
    "opening_balance": 0.00, "opening_direction": "DR",
    "lines": [
      { "date": "2026-08-15T10:00:00Z", "kind": "INVOICE", "reference": "INV-00042",
        "narration": null, "debit": 9207.50, "credit": 0.00, "running_balance": 9207.50 },
      { "date": "2026-08-18T09:00:00Z", "kind": "PAYMENT", "reference": "RCP-00011",
        "narration": "UPI", "debit": 0.00, "credit": 5000.00, "running_balance": 4207.50 }
    ],
    "closing_balance": 4207.50, "closing_direction": "DR"
  }, "error": null }
```
Last line's `running_balance` == `closing_balance` (party-ledger invariant, unchanged from spec 013).

**403** `NOT_LINKED`.

---

## 7. `GET /v1/ecom/account/invoices/{invoiceUid}/pdf` — invoice PDF (Phase 2, deferred)

Out of scope for Phase 1; documented for continuity. Same auth/tenant/party gating; streams the
buyer-facing PDF for an invoice owned by the resolved party (404 otherwise).

---

## Access-control matrix (applies to endpoints 1–6)

| Scenario | Result |
|---|---|
| Linked buyer, own party | 200 with own data |
| Unlinked buyer | 403 `NOT_LINKED` |
| `customer_id` for an account the login is **not** linked to | Resolves to the login's default (never the requested), or 403 if none |
| Multi-account buyer, valid `customer_id` | 200 for that account |
| Same buyer, two storefronts/workspaces | Each returns only that workspace's data (tenant isolation) |
| Restricted contact (`active = false`) | Treated as unlinked → 403 |
| Invoice/order of another party addressed directly | 404 |
