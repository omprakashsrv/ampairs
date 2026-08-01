# Phase 0 Research — B2B Wholesale Network (workspace-to-workspace ordering)

This feature builds a **private B2B ordering graph between Ampairs workspaces**: a retailer workspace
places a purchase order to its distributor/wholesaler workspace that is **also on Ampairs**, modelled on
the Udaan / Jumbotail buyer↔seller relationship (a retailer browses a distributor's price-list, places a
PO, the distributor fulfils it, an invoice and ledger entry follow). The technical crux is **cross-tenant
data flow**: a single business transaction legitimately spans two `ownerId` tenants, which the
multi-tenancy rule (05) normally forbids without `nativeQuery=true` and explicit consent. Each item:
**Decision · Rationale · Alternatives considered**. These supersede inline assumptions in `spec.md`.

---

## R1. New `b2b` bounded context (the connection graph lives outside both tenants)

- **Decision**: A **new backend module `b2b`** (`com.ampairs.b2b`) owns the buyer↔seller **connection**,
  the per-buyer **price-list/catalog share**, and the **cross-tenant purchase order** as a first-class
  network entity (`B2bPurchaseOrder`) that references *both* workspace ids. It does **not** own
  fulfilment, invoicing or ledgering — once accepted, the PO **projects into the seller's existing `order`
  module** (a normal sales `Order`) and, optionally, into the **buyer's `order`/`payment`** modules as a
  purchase. `b2b` is the broker; the two tenants' existing modules do the work.
- **Rationale**: The connection and the in-flight PO are inherently *bi-tenant* — they belong to neither
  workspace alone. Placing them in a dedicated context keeps the cross-tenant access (the part that must
  bypass `@TenantId`) **surgically contained** to one module with audited native queries, rather than
  scattering tenant-crossing logic through `order`/`customer`. Rule 08: new bounded context = new module.
- **Alternatives considered**: Bolt onto `ecom` (rejected — `ecom` is B2C storefront/cart, not a
  workspace-to-workspace graph; no relationship/consent model). Bolt onto `order` (rejected — `order` is
  strictly single-tenant; adding cross-tenant reads there pollutes the cleanest module). Bolt onto
  `customer` (rejected — a connected seller is a *workspace*, not a CRM contact, though a mirror `Customer`
  is created, see R5).

## R2. The connection entity & double opt-in consent (the security gate)

- **Decision**: `B2bConnection` is the consent record linking a **buyer workspace** and a **seller
  workspace** with a state machine: `REQUESTED → PENDING_APPROVAL → ACTIVE → (SUSPENDED | REVOKED)`.
  Either side may initiate (buyer requests to a seller's connect code / seller invites a buyer); the
  **other side must approve**. Only an `ACTIVE` connection authorises *any* cross-tenant read or PO. The
  connection carries `buyerWorkspaceId`, `sellerWorkspaceId`, `status`, `approvedBy`, `priceListId`,
  `creditTerms`, and is stored once (not duplicated per tenant) in the `b2b` module.
- **Rationale**: Cross-tenant visibility without **explicit, mutual, revocable consent** is a data-leak.
  A double opt-in connection is the authorization primitive every cross-tenant query checks first — it is
  the "explicit consent" that rule 05 requires before a `nativeQuery` may read across `ownerId`. Revocation
  must instantly cut access.
- **Alternatives considered**: Single-sided "follow" (rejected — lets a buyer pull a seller's price-list
  without the seller's consent). Implicit connection on first PO (rejected — no consent gate, no approval).
  Per-tenant duplicated connection rows (rejected — two sources of truth, approval-state divergence).

## R3. Cross-tenant access mechanism — native queries scoped by an ACTIVE connection

- **Decision**: Every cross-tenant read/write in `b2b` goes through a thin `B2bAccessGuard` that (a)
  loads the `B2bConnection`, (b) asserts `status == ACTIVE`, (c) asserts the caller's
  `TenantContextHolder.getCurrentTenant()` is one of the two parties and is allowed that direction, then
  (d) performs the access via **`nativeQuery = true`** repositories that explicitly take both workspace ids
  as parameters — **never** relying on `@TenantId` auto-filtering, and **never** mixing `@TenantId` with an
  explicit `workspaceId` param (rule 05). Reads of the *seller's* catalog run as the seller's tenant
  context via `TenantContextHolder.withTenant(sellerWorkspaceId) { ... }` only after the guard passes.
- **Rationale**: This is the literal rule-05 prescription: cross-tenant queries use `nativeQuery=true` to
  bypass auto-filtering, gated by explicit consent. Centralising it in one guard means there is exactly one
  audited place where tenant isolation is deliberately crossed, every call is connection-checked, and the
  blast radius of a bug is contained.
- **Alternatives considered**: Disable `@TenantId` globally for these flows (rejected — catastrophic,
  leaks everything). JPQL with a `workspaceId` param alongside `@TenantId` (rejected — rule 05 forbids
  combining them; the tenant filter would still narrow to one side). A second DB connection per tenant
  (rejected — over-engineered; one guarded native path suffices).

## R4. PO placement — buyer authors, seller's sales order is the projection

- **Decision**: The buyer authors a `B2bPurchaseOrder` (header + lines referencing the **seller's**
  catalog item ids and the **shared price-list** prices). On the buyer side it is a *purchase* document; on
  acceptance by the seller it **projects into the seller's `order` module** as a sales `Order`
  (`orderType="B2B"`, `customerId` = the buyer's mirror `Customer` in the seller's workspace, see R5) via
  a `B2bOrderIngestionService` that mirrors the existing `EcomOrderIngestionService` pattern (idempotent on
  the `b2bPoRef`). Status flows back from the seller's `Order` to the `B2bPurchaseOrder` (and thus to the
  buyer) via `OrderStatusChangedEvent` → a `b2b` listener.
- **Rationale**: A B2B PO accepted by a seller **is** a sales order — it must enter the seller's normal
  fulfilment/invoice/inventory/ledger pipeline, exactly as ecom and ONDC orders do. Reusing the proven
  ingestion pattern (idempotent, event-driven) avoids a parallel order lifecycle. Keeping the
  `B2bPurchaseOrder` as the buyer-facing truth lets the buyer track it without reading the seller's `Order`
  directly.
- **Alternatives considered**: Write directly into the seller's `Order` table from the buyer's request
  (rejected — buyer code mutating seller data with no acceptance step; no seller approval; tenant-crossing
  write outside the guard). One shared order row read by both tenants (rejected — `Order` is single-tenant
  by design; both sides need their own document — purchase vs sales).

## R5. Buyer↔seller identity mirroring (Customer/Supplier shadows)

- **Decision**: When a `B2bConnection` becomes `ACTIVE`, create a **mirror `Customer`** in the seller's
  workspace representing the buyer (name, GSTIN, address, credit terms copied from the connection) and a
  **mirror supplier/party** representation in the buyer's workspace representing the seller (Phase 1: a
  `PartyBalance`/ledger party in the spec-013 `payment` module, since there is no first-class supplier
  module yet). The mirror `Customer.uid` is what the seller's projected `Order.customerId` points to; it
  carries a `b2bConnectionId` back-reference and is flagged so CRM edits don't desync the connection.
- **Rationale**: The seller's existing pipeline (order → invoice → payment ledger, aging, credit limit)
  is **customer-keyed**; a B2B buyer must therefore appear as a `Customer` in the seller's tenant for any
  of it to work. Mirroring at connection-activation (not per-PO) means the relationship, credit terms and
  ledger exist before the first order.
- **Alternatives considered**: Special-case "is this customerId actually a workspace" everywhere
  (rejected — invasive, every consumer of `Order`/`invoice`/`payment` would need to know about B2B).
  No mirror, read buyer's workspace profile live (rejected — couples seller's invoice/ledger to a
  cross-tenant read on every operation; the mirror is a clean local projection).

## R6. Price-list / catalog sharing per buyer

- **Decision**: A `B2bPriceList` (owned by the seller) is a named set of price overrides over the seller's
  `Product`/`EcomListedProduct` catalog (per-item `b2bPrice`, optional MOQ, pack size, tier breaks),
  assignable per `B2bConnection`. The buyer's catalog browse is a **guarded cross-tenant read** (R3) of the
  seller's listed products **with the connection's price-list applied server-side** — the buyer never sees
  the seller's base/MRP cost, only the negotiated `b2bPrice`. Reuses `EcomListedProduct` as the shareable
  surface (same set already curated for the storefront).
- **Rationale**: Wholesale is price-list-driven and confidential — different buyers get different prices,
  and a buyer must not see another buyer's pricing or the seller's margins. Applying the price-list
  server-side inside the guard enforces this. Reusing `EcomListedProduct` keeps one curated catalog.
- **Alternatives considered**: Share the raw `Product` catalog with base prices (rejected — leaks cost/MRP
  and exposes unlisted SKUs). One global B2B price for all buyers (rejected — wholesale is inherently
  per-buyer negotiated). Client-side price application (rejected — the buyer's client would receive the
  seller's confidential base prices).

## R7. Credit terms, limits and the order→invoice→ledger reuse

- **Decision**: Credit terms (`creditDays`, `creditLimit`, payment terms) live on the `B2bConnection` and
  are copied onto the seller-side mirror `Customer` (which already has `creditLimit`/`creditDays`/
  `outstandingAmount`). Acceptance of a PO can be **gated on the buyer's outstanding vs credit limit** via
  the existing spec-013 `payment`/aging services. Once the seller fulfils and finalises an invoice, the
  **existing `InvoiceFinalizedEvent` → `payment` ledger** path posts the receivable against the mirror
  `Customer` automatically — no new ledger code. Collections/dunning (specs 013/017) then apply unchanged.
- **Rationale**: The whole point of reusing the seller's pipeline is that credit, invoicing and ledgering
  already exist and are customer-keyed. By mirroring the buyer as a `Customer`, B2B receivables, aging and
  dunning come "for free" through the existing event paths.
- **Alternatives considered**: A separate B2B receivables ledger (rejected — duplicates spec-013;
  reconciliation nightmare). No credit gating (rejected — wholesale runs on credit; a limit check is core).

## R8. Offline placement (the buyer can draft a PO offline)

- **Decision**: PO **drafting/editing is offline-capable on the buyer side** via the standard
  offline-first pattern: `B2bPurchaseOrder` is a workspace-scoped Room entity on the buyer with
  `synced=false` + `markPendingPush`, and a `B2bPurchaseOrderSyncDelegate` owns the canonical
  `GET/POST /b2b/v1/purchase-orders/sync`. **Submission for the seller's acceptance is an online action**
  (`POST /b2b/v1/purchase-orders/{uid}/submit`) — the cross-tenant projection, consent re-check and credit
  gate require the seller's tenant and cannot happen offline. The *catalog browse + price-list* is an
  online read (live cross-tenant), optionally cached read-only on device.
- **Rationale**: A field salesperson must be able to build a PO with no signal; but the moment it crosses
  into the seller's tenant (acceptance, stock reservation, credit check) it is inherently online. Splitting
  *draft* (offline, synced) from *submit* (online action) matches both the offline-sync architecture and
  the cross-tenant reality.
- **Alternatives considered**: Fully offline including submission (rejected — cross-tenant write + credit
  gate can't be done on-device against another tenant's live state). Fully online PO (rejected — loses the
  field-sales offline value, which is the differentiator vs web-only Udaan).

## R9. Notifications

- **Decision**: Connection requests/approvals, PO submission/acceptance/rejection, fulfilment status and
  invoice events drive notifications to the counterpart workspace via the existing **`notification`
  module** (`NotificationService.queueNotificationWithTenant(...)`, channel SMS/WHATSAPP/PUSH) and the
  in-app feed. Each notification is addressed to the *other* tenant's members and respects that tenant's
  context.
- **Rationale**: The platform already has a tenant-aware notification queue; B2B events are exactly the
  kind of cross-party signal it exists for. WhatsApp/push for "new PO" is the Udaan-style nudge sellers
  expect.
- **Alternatives considered**: Build B2B-specific messaging (rejected — duplicates `notification`).
  Email-only (rejected — Indian SMB B2B runs on WhatsApp/push, spec 023).

## R10. Cross-tenant security & visibility model (consolidated)

- **Decision**: (1) **Authorization**: every cross-tenant operation requires an `ACTIVE` `B2bConnection`
  *and* that the acting member has the right role in *their own* workspace (reuse `WorkspaceMember`
  permissions). (2) **Direction**: a buyer may read the seller's *shared* catalog + their own POs +
  fulfilment status; the seller may read the buyer's *connection-disclosed* profile (GSTIN/address/credit)
  + the POs addressed to them — **nothing else**. (3) **Field minimisation**: cross-tenant DTOs expose only
  consented fields (no cost, no other buyers, no unrelated CRM). (4) **Revocation**: setting the connection
  `REVOKED`/`SUSPENDED` immediately fails the `B2bAccessGuard`; in-flight projected `Order`s remain (they
  are now the seller's own data) but no new cross-tenant access is allowed. (5) **Audit**: every guarded
  cross-tenant access is logged with both tenant ids, connection id and acting user.
- **Rationale**: The feature deliberately punches a hole in tenant isolation; it must do so along a narrow,
  consented, role-checked, minimised, revocable and audited channel — anything less is a multi-tenant SaaS
  liability.
- **Alternatives considered**: Trust the connection alone without per-member role checks (rejected — any
  member of a connected workspace could place POs). Broad profile sharing (rejected — minimise to consented
  fields).

## R11. Idempotency & status reconciliation

- **Decision**: PO submission carries a client `b2bPoRef`; the seller-side projection is idempotent on it
  (re-submit = no-op, like `EcomOrderIngestionService` on `ecomOrderRef`). A `B2bOrderLink` maps
  `B2bPurchaseOrder.uid` ↔ seller `Order.uid` ↔ resulting `invoiceRefId`, so status and invoice changes
  reconcile both directions deterministically. Last-write-wins on status by the seller's `Order` timestamp.
- **Rationale**: Networked submission can be retried; without an idempotency key a flaky link creates
  duplicate sales orders. The link table is the single correlation point.
- **Alternatives considered**: No idempotency key (rejected — duplicates). Status held only on the buyer
  side (rejected — seller's `Order` is the fulfilment truth).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Module placement | New `b2b` bounded context brokering between tenants; reuses `order`/`invoice`/`payment` (R1) |
| Consent model | `B2bConnection` double opt-in state machine; only `ACTIVE` authorises anything (R2) |
| Cross-tenant access | `B2bAccessGuard` + `nativeQuery=true` scoped by connection + `withTenant {}` (R3) |
| PO → sales order | Buyer authors `B2bPurchaseOrder`; projects into seller `order` (`orderType="B2B"`) (R4) |
| Identity mirroring | Buyer ↔ mirror `Customer` in seller; seller ↔ ledger party in buyer (R5) |
| Price-list sharing | Per-buyer `B2bPriceList` over `EcomListedProduct`, applied server-side in the guard (R6) |
| Credit & ledger reuse | Terms on connection → mirror `Customer`; existing `InvoiceFinalizedEvent`→ledger (R7) |
| Offline | Draft/edit offline-synced; **submit** is an online cross-tenant action (R8) |
| Notifications | Existing `notification` module, tenant-aware, SMS/WhatsApp/push (R9) |
| Security/visibility | Consented + role-checked + minimised + revocable + audited cross-tenant channel (R10) |
| Idempotency | `b2bPoRef`-keyed projection + `B2bOrderLink` two-way correlation (R11) |
