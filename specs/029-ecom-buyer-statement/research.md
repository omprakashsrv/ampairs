# Phase 0 Research — 029 Ecom Buyer Account (invoices, statement, order↔invoice)

All `NEEDS CLARIFICATION` from Technical Context resolved below. Each entry: Decision · Rationale ·
Alternatives considered.

## R1 — How buyers reach workspace documents without membership

**Decision.** Reuse the existing order-access mechanism verbatim: buyer endpoints live under
`/v1/ecom/account/**`, `@PreAuthorize("isAuthenticated()")`, with **no** `X-Workspace-ID` (exempted
in `SessionUserFilter`). The `storefront_slug` query param resolves the tenant via
`storefrontService.getPublishedStorefrontBySlug(slug).ownerId`, set with `TenantContextHolder` in a
`try/finally`. The buyer's login is resolved to a linked CRM customer with
`EcomCustomerService.resolveLinkedCustomerId(userId, requestedCustomerId)`.

**Rationale.** `GET /v1/ecom/account/orders` already does exactly this; parity keeps one security
model, one exemption, one link-resolution path. No new auth surface to review.

**Alternatives considered.**
- *Give the buyer a scoped workspace role / `X-Workspace-ID`* — rejected: buyers aren't members;
  would require RBAC holes and membership rows.
- *Expose `/invoice/v1/**` and `/payment/v1/**` to buyers with a filter* — rejected: those are
  owner/management surfaces, tenant-guarded for members; punching a customer filter into them widens
  the blast radius far beyond ecom.

## R2 — Invoice vs order data-scope semantics (why statement is account-level)

**Decision.** Invoice list and party statement are **account-level**: the buyer sees the linked CRM
customer's full position in that workspace (including counter/offline invoices), not only
ecom-originated documents. Ship the full-account view; an "ecom-only" filter is out of scope.

**Rationale.** The `payment` party ledger has no per-channel dimension — it is the single number the
owner would read to that customer. An account statement that hides some of the customer's own bills
would mislead. Adding a channel filter means a new derivation + a different surface, unjustified now.

**Alternatives considered.** *Filter to invoices whose `orderRefId` maps to an `EcomOrder`* — rejected
for the statement (breaks running-balance math); noted as a possible future invoice-list filter (OQ-7).

## R3 — Order↔invoice link mechanics

**Decision.** Read the existing chain, no schema change:
`EcomOrder.managementOrderRef` (set on ingest) `== workspace Order.uid == Invoice.orderRefId` (set when
the invoice is raised from the order). Order→invoices: `Invoice.orderRefId == ecomOrder.managementOrderRef`.
Invoice→order: map `Invoice.orderRefId` back via `EcomOrderRepository.findByManagementOrderRef(...)`
to the buyer-facing `ecomOrderRef`. Reverse mapping happens in the **ecom** controller (it owns the
`EcomOrder` side); `InvoiceEcomService` returns the raw `orderRefId` and ecom swaps it.

**Rationale.** The columns already exist and are indexed enough for point lookups. Keeping the reverse
map in ecom avoids a new `ecom→invoice` behavioral coupling and keeps `InvoiceEcomService` ignorant of
ecom concepts.

**Alternatives considered.**
- *Store the ecom ref on the invoice* — rejected: duplicates state, needs a migration + backfill +
  write-path change in a module that shouldn't know about storefronts.
- *Resolve the reverse link inside `invoice`* — rejected: would make `invoice` depend on `ecom`.

## R4 — `amountDue` sourcing for the invoice list

**Decision.** Keep the invoice **list** total-only (`amountDue` omitted or `= total`); per-bill
outstanding/dues come from the `outstanding` endpoint (payment-backed `OutstandingService.openBills`,
which already computes `total − Σ active allocations` + due date + aging). Revisit only if UX demands
dues inline (OQ-5).

**Rationale.** `OutstandingService` already owns the allocation math in `payment`. Duplicating an
allocation read inside `invoice` adds an `invoice→payment` edge and a second source of truth for the
same number. The statement/outstanding endpoints are the buyer's "what do I owe" surface.

**Alternatives considered.** *Compute `amountDue` in `InvoiceEcomServiceImpl` via a payment allocation
read* — rejected for now (new cross-module edge, duplicate math). Left as an open question.

## R5 — Buyer-facing enum vocabulary (invoice status, ledger kind, direction)

**Decision.** Never serialize raw enums. Map `InvoiceStatus` → a buyer-facing `status` string; ledger
`EntryType` → a small `kind` string (`INVOICE`/`PAYMENT`/`ADJUSTMENT`); `Direction` → `"DR"`/`"CR"`.
Drafts are excluded from every buyer view (finalized-only, matching `OutstandingService`).

**Rationale.** DTO isolation (Principle II): buyers get a stable, minimal contract and never see
internal enum churn. Finalized-only prevents leaking work-in-progress documents.

**Alternatives considered.** *Pass enums through* — rejected (contract leak, exposes internal states).
Exact buyer vocabulary (collapse part-paid, etc.) deferred to OQ-6.

## R6 — Cross-module wiring shape

**Decision.** Two new interfaces in `core.service`, impls in the owning modules, buyer DTOs beside the
interfaces in `core` (precedent: `EcomCustomerAccount`, `OrderEcomService`):
`InvoiceEcomService` (impl in `invoice`) and `PartyLedgerEcomService` (impl in `payment`). Both require
an active tenant context set by the ecom controller — same contract as existing ecom-facing services.

**Rationale.** Matches the established pattern exactly; dependency edges already exist
(`invoice→core`, `payment→core`, `ecom→core`), so no new module graph edges are introduced.

**Alternatives considered.** *Have ecom call a single aggregating façade* — deferred; two focused
interfaces map cleanly to the two owning modules and their existing services.

## R7 — Pagination & bounds

**Decision.** Invoice list is paginated via `PageResponse` (`page`/`size`, newest-first). Statement is
windowed by `from`/`to` (default `from`=account opening, `to`=now). Cap the statement window later if
ledger scans grow (OQ-1); not gating for first cut since scans are per-party.

**Rationale.** Aligns with Principle V pagination rules and the existing `getCustomerOrders` paging.
Party-scoped scans are naturally bounded by one customer's document volume.

**Alternatives considered.** *Unbounded list* — rejected (Principle V requires pagination metadata).

## R8 — Migrations

**Decision.** None. Read-only + joins over existing `invoice`/`payment`/`ecom` tables. Record in
`NO_MIGRATION_NEEDED.md`.

**Rationale.** No new columns/tables; the order↔invoice link already exists in the schema.
