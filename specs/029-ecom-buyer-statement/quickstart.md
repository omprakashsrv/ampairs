# Quickstart / Validation Guide — 029 Ecom Buyer Account

Validates the buyer can read their invoices, follow order↔invoice links, and see their money position
from a storefront — link-gated, tenant-scoped, no workspace membership. References:
[contracts/buyer-account-api.md](./contracts/buyer-account-api.md) · [data-model.md](./data-model.md).

## Prerequisites

- Backend running with Docker deps up (Postgres via `docker-compose.yml`).
  ```bash
  ./gradlew :ampairs_service:bootRun          # or SPRING_PROFILES_ACTIVE=test for E2E
  ```
- Seed/fixtures in one workspace:
  - a published storefront with a known `storefront_slug`;
  - a CRM `customer` (the `partyUid`) whose phone matches a buyer login, linked via the ecom
    account-link flow (`POST /v1/ecom/account/link`);
  - at least one **finalized** invoice for that customer, one raised from an ecom order
    (`invoice.orderRefId == ecomOrder.managementOrderRef`) and one not;
  - at least one payment/receipt so the ledger has an invoice line + a payment line.
- A buyer JWT for the linked login (obtained through the normal storefront auth flow).

## Build & test

```bash
# Fast unit slices for the new mapping logic
./gradlew :invoice:test :payment:test :ecom:test

# Full gate (Testcontainers) before merge
./gradlew testAll
```

## Scenario 1 — Invoice list (US1)

```bash
curl -s "$BASE/api/v1/ecom/account/invoices?storefront_slug=$SLUG&page=0&size=20" \
  -H "Authorization: Bearer $BUYER_JWT" | jq '.data.content[0]'
```
**Expect**: 200; newest-first list of the linked customer's **finalized** invoices; no drafts; each
item carries `order_ref` (buyer-facing) or `null`.

## Scenario 2 — Invoice detail + reverse link (US2, US4)

```bash
curl -s "$BASE/api/v1/ecom/account/invoices/$INVOICE_UID?storefront_slug=$SLUG" \
  -H "Authorization: Bearer $BUYER_JWT" | jq '{status,order_ref,total,lines}'
```
**Expect**: 200 with line items + totals; `order_ref` set for the ecom-originated invoice, `null` for
the non-ecom one. Requesting an invoice of **another** party → **404**.

## Scenario 3 — Order → invoices (US3)

```bash
# Direct link endpoint
curl -s "$BASE/api/v1/ecom/account/orders/$ECOM_ORDER_REF/invoices?storefront_slug=$SLUG" \
  -H "Authorization: Bearer $BUYER_JWT" | jq
# Embedded in order detail (single round-trip)
curl -s "$BASE/api/v1/ecom/account/orders/$ECOM_ORDER_REF?storefront_slug=$SLUG" \
  -H "Authorization: Bearer $BUYER_JWT" | jq '.data.invoices'
```
**Expect**: both return the invoice(s) for that order. Order with none yet, or `management_order_ref`
still null → `[]` (not an error). Multiple invoices for one order → all listed.

## Scenario 4 — Outstanding + aging (US5)

```bash
curl -s "$BASE/api/v1/ecom/account/outstanding?storefront_slug=$SLUG" \
  -H "Authorization: Bearer $BUYER_JWT" | jq '{current_balance,balance_direction,open_bills,aging}'
```
**Expect**: 200; current signed balance, per-bill outstanding with due dates + aging bucket, and the
aging summary. Numbers match what the owner sees for that customer in `payment`.

## Scenario 5 — Statement (US6)

```bash
curl -s "$BASE/api/v1/ecom/account/statement?storefront_slug=$SLUG&from=2026-08-01T00:00:00Z" \
  -H "Authorization: Bearer $BUYER_JWT" | jq '{opening_balance,closing_balance,lines}'
```
**Expect**: 200; interleaved invoice/payment lines with a running balance; the last line's
`running_balance` equals `closing_balance`.

## Scenario 6 — Access control (US7, US8)

```bash
# Unlinked buyer
curl -s -o /dev/null -w '%{http_code}\n' \
  "$BASE/api/v1/ecom/account/statement?storefront_slug=$SLUG" -H "Authorization: Bearer $UNLINKED_JWT"
# → 403 with error code NOT_LINKED

# Multi-account buyer picks an account they ARE linked to
curl -s "$BASE/api/v1/ecom/account/invoices?storefront_slug=$SLUG&customer_id=$OTHER_LINKED_CUS" \
  -H "Authorization: Bearer $BUYER_JWT" | jq '.data.total_elements'

# customer_id the buyer is NOT linked to → resolves to their default, never the requested account
```
**Expect**: unlinked → 403 `NOT_LINKED`; a `customer_id` the login isn't linked to never returns that
account's data. Restricted contact (`active=false`) is treated as unlinked.

## Scenario 7 — Tenant isolation

Repeat Scenario 1 with the **same buyer** against a second storefront/workspace they're also linked in.
**Expect**: each call returns only that workspace's invoices/ledger — never a merge across workspaces.

## Done / pass criteria

- [ ] Endpoints 1–6 return the shapes in the contract, `ApiResponse`-wrapped, snake_case.
- [ ] Drafts never appear; wrong-party documents → 404; unlinked → 403 `NOT_LINKED`.
- [ ] Order↔invoice link resolves both directions; empty/multi cases handled.
- [ ] Statement last-line running balance == closing balance.
- [ ] Tenant isolation holds across two workspaces for one buyer.
- [ ] `./gradlew testAll` green.
