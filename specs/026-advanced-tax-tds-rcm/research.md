# Phase 0 Research — Advanced Indian Tax (TDS / TCS / RCM / Composition / ITC)

All Technical Context unknowns are resolved below. Each item: **Decision · Rationale · Alternatives
considered**. These supersede the inline assumptions in `spec.md` with concrete technical choices.

The existing `tax` module is a **master/workspace two-layer GST engine**: `MasterTaxCode`/`TaxCode`
(HSN/SAC subscriptions), `MasterTaxRule`/`TaxRule` (scenario `componentComposition` keyed
`INTRA_STATE`/`INTER_STATE`), `MasterTaxComponent`/`TaxComponent` (CGST/SGST/IGST/CESS rates), and
`TaxConfiguration` (GSTIN, `taxStrategy = INDIA_GST`). Tax is calculated **on-device** by a pluggable
strategy (`IndiaGSTStrategy`: place-of-supply → CGST+SGST vs IGST), money in `Double`. The `tax`
module is intentionally **off** the `/sync` contract (subscribe/unsubscribe). Invoices store tax as a
JSON `taxInfos: List<TaxInfo>` per line + `placeOfSupply`/`sellerPlaceOfSupply`; finalize publishes
`InvoiceFinalizedEvent`, consumed by the `payment` module which posts a `SALES_INVOICE` `LedgerEntry`
(receivable-positive, `BigDecimal(19,4)`).

The central insight driving this research: **GST, TDS/TCS, RCM, and composition scheme are not the
same kind of thing and must not be jammed into one mechanism.** GST is an additive tax *component* on
the line. TDS is an income-tax *withholding* (a deduction at settlement, not a line tax). TCS is a
*collection* added at settlement. RCM *flips who is liable* (it changes ledger posting, not the rate).
Composition scheme *suppresses* the tax line entirely (bill of supply). ITC reversal is a *credit*
event. Each gets the right home below, and the offline-deterministic calculator is preserved.

---

## R1. Module placement — extend `tax`, post through `payment`/`invoice`

- **Decision**: **Extend the existing `tax` module** (do not create a new module) for tax *definitions*
  and *calculation* (TDS/TCS sections, RCM flag, composition config, ITC-reversal rules). TDS/TCS
  *postings* live in the `payment` module's ledger (new `EntryType`s); composition/RCM *document
  shape* lives in the `invoice` module (a `documentType` + `rcmApplicable` flag). No new bounded
  context.
- **Rationale**: These are all *advanced GST/withholding* concerns — squarely the `tax` module's
  bounded context (it already owns GSTIN, components, rules, place-of-supply). Splitting them out
  would fragment the tax domain and force cross-module chatter for a single calc. The *effects*
  (ledger entries, invoice shape) correctly belong to the modules that own those aggregates, reached
  via the existing event/public-service seams (`InvoiceFinalizedEvent` → payment) — respecting module
  boundaries.
- **Alternatives considered**: A new `withholding` module (rejected — TDS/TCS are inseparable from the
  tax calc context and the invoice/payment flow; a new module just adds boundaries to cross). Put
  everything in `invoice` (rejected — tax definitions and the calculator are reusable across
  invoice/order/payment and already centralized in `tax`).

## R2. TDS — where it sits (withholding at settlement, NOT a GST component)

- **Decision**: TDS is modeled as a **withholding deduction at invoice/payment time**, not a
  `TaxComponent`. A new `TdsSection` definition (`tax` module: section code e.g. `194Q`/`194C`/`194J`,
  rate, threshold, applicable party type) drives a **withholding amount computed on the taxable base**
  (typically pre-GST value), which posts a dedicated **`LedgerEntry(type = TDS_WITHHELD,
  direction = CR)`** in the `payment` ledger, reducing the net receivable. The gross invoice value and
  the GST are unchanged; TDS only changes what cash is expected.
- **Rationale**: TDS is income-tax withheld by the *buyer* — it is **not** part of the GST tax stack
  and must never enter `componentComposition` (doing so would corrupt the CGST/SGST/IGST sum and the
  GST audit). It is fundamentally a *ledger* event: gross dues stay, but the buyer pays
  `gross − TDS` and remits TDS to the government. Posting it as a CR ledger entry against the party
  (mirroring spec 013's discount/write-off vouchers) is the natural fit and keeps the party balance
  footing.
- **Alternatives considered**: A negative `TaxComponent` (rejected — pollutes the GST breakup; TDS is
  not GST and is computed on a different base). A field on `Invoice` only (rejected — TDS is often
  applied at *payment* time on the actual amount paid, not just at invoice; the ledger is the one
  place both timings reconcile). Reduce the invoice total by TDS (rejected — the invoice legally shows
  gross + GST; TDS is a payment-side deduction, not an invoice reduction).

## R3. TCS — collection added at settlement (mirror of TDS, opposite sign)

- **Decision**: TCS (e.g. `206C(1H)` on sale of goods above threshold) is a **collection added** to
  the amount receivable: a `TcsSection` definition (rate, threshold, computed on sale value incl. GST
  per the section) drives an additional **`LedgerEntry(type = TCS_COLLECTED, direction = DR)`** that
  *increases* the receivable, plus a tracked liability to remit. Like TDS, it is **not** a GST
  component and is recorded as a separate ledger movement and a distinct invoice line/annotation.
- **Rationale**: TCS is the seller collecting extra from the buyer to remit — the opposite direction
  of TDS but the same architectural shape (a settlement-time withholding/collection, ledger-posted,
  outside the GST stack). Symmetry with TDS keeps the model small and the audit clear.
- **Alternatives considered**: Fold TCS into a GST CESS-like component (rejected — different legal
  basis, different return form, different remittance; CESS is GST, TCS is income-tax collection).

## R4. RCM — the liability flip (buyer liable; affects ledger + GST treatment, not the rate)

- **Decision**: RCM is a **boolean flag on the transaction** (`rcmApplicable` on the invoice/line),
  driven by tax-code/notification rules. When RCM applies: the supplier's invoice shows **no output
  GST collected** (it's a bill noting "tax payable under RCM by recipient"), and on the **buyer's**
  books the GST is **self-assessed** — posted as a *liability* and an offsetting *ITC* entry rather
  than collected from the supplier. The place-of-supply CGST/SGST-vs-IGST decision is **still
  computed** (RCM doesn't change *which* tax, only *who pays it*).
- **Rationale**: RCM inverts liability, not the calculation. The same `IndiaGSTStrategy`
  place-of-supply logic produces the component split; what changes is the *posting* — supplier
  collects nothing, recipient self-pays. Modeling it as a flag that gates the *posting/document*
  (not the *rate*) keeps the offline-deterministic calculator untouched and reuses the existing
  intra/inter-state engine verbatim. (The app's `TransactionContext.isReverseCharge` already exists as
  a hook — wire it through.)
- **Alternatives considered**: A separate RCM tax rule set (rejected — duplicates the rate logic; the
  rate is identical, only liability differs). Compute RCM as negative supplier GST (rejected —
  misstates the supplier's output tax and the GST return).

## R5. Composition scheme — bill of supply, no tax line, flat turnover tax

- **Decision**: A composition-scheme workspace is a **`TaxConfiguration` mode** (`compositionScheme =
  true` + composition rate, e.g. 1%/5%). Its invoices are a distinct **`documentType =
  BILL_OF_SUPPLY`**: **no per-line GST components, no `taxInfos`, no CGST/SGST/IGST split, and a
  mandatory "composition taxable person, not eligible to collect tax" declaration**. The composition
  tax (a flat % of turnover) is **not** charged to the customer — it's the seller's own liability,
  tracked separately (periodic, not per-invoice), so it does **not** post a per-line tax.
- **Rationale**: A composition dealer legally **cannot** issue a tax invoice or collect GST — issuing
  one with a CGST/SGST line would be wrong and a compliance risk. The document shape genuinely differs
  (bill of supply), so a `documentType` enum that suppresses the tax stack is the correct lever. The
  flat turnover tax is a periodic self-liability, modeled as an adjustment/period computation, not a
  line item — keeping per-invoice calc clean.
- **Alternatives considered**: Render a tax invoice with 0% components (rejected — legally a bill of
  supply is required; 0% lines still imply a tax invoice). Charge composition % per line as a
  component (rejected — composition tax is on aggregate turnover and not collected from the buyer;
  per-line is both wrong and not customer-facing).

## R6. ITC / Input Tax Credit reversal — a credit ledger, not a sales-tax change

- **Decision**: ITC and ITC **reversal** (Rule 42/43, exempt supplies, non-payment-in-180-days, blocked
  credits) are modeled as a **separate input-credit ledger** (new `ItcEntry` / reversal entries in the
  `tax` module's reporting layer, or as typed `payment` ledger entries), **decoupled from output-tax
  calculation on sales**. ITC tracks credits *available* against purchases and their reversals; it
  never alters the GST charged on an outgoing invoice.
- **Rationale**: ITC is a *purchase-side credit* concern; output GST on a sale is unrelated. Conflating
  them would let an ITC reversal silently change a customer's invoice — wrong. A dedicated credit
  ledger (mirroring spec 013's signed-entry pattern) keeps reversals auditable and the sales tax calc
  pure. Full ITC requires first-class purchase bills (today represented as `payment` adjustment
  vouchers), so **Phase-gate**: model the ITC ledger now, deepen when purchase billing lands.
- **Alternatives considered**: Net ITC against output tax in the calculator (rejected — that's a
  *return-filing* aggregation, not a per-transaction calc; mixing them breaks determinism and audit).
  Skip ITC entirely (rejected — it's core to "ITC reversal rules" in the brief; scaffold it now).

## R7. Nil / exempt / zero-rated / non-GST — a per-line GST treatment enum

- **Decision**: Add a **`gstTreatment` enum** on the line (and derivable from the tax code):
  `TAXABLE`, `NIL_RATED` (0% by schedule), `EXEMPT` (exempt supply, no ITC), `ZERO_RATED`
  (exports/SEZ — 0% **with** ITC, optionally under LUT or with IGST-paid-and-refunded),
  `NON_GST` (outside GST, e.g. petrol/alcohol). Each suppresses or shapes the component stack
  deterministically: NIL/EXEMPT/NON_GST → no tax components; ZERO_RATED → IGST at 0% (with the ITC
  flag set so R6 treats it correctly).
- **Rationale**: These four are legally distinct (the difference between exempt and zero-rated is ITC
  eligibility, which matters for R6) and each changes the line's component output. A single enum that
  the calculator reads keeps the behavior deterministic and the GST audit (R8) explicit about *why* a
  line carried no tax. Critically, "0% tax" is ambiguous without this — nil vs exempt vs zero-rated
  look identical on the amount but differ on ITC and return reporting.
- **Alternatives considered**: Just use a 0% tax code (rejected — loses the exempt-vs-zero-rated ITC
  distinction and the reporting category). Per-treatment tax codes only (rejected — the treatment is
  orthogonal to HSN and better as an explicit field).

## R8. Per-transaction tax audit snapshot — immutable, captured at finalize

- **Decision**: At invoice finalize, persist an **immutable tax audit snapshot** capturing the exact
  inputs and outputs of the calculation: the resolved `TaxRule`/`componentComposition` version,
  source & destination state, place-of-supply decision (`is_intra_state`), each component
  (name/rate/taxable amount/tax amount), `gstTreatment`, `rcmApplicable`, any TDS/TCS section + amount,
  and the rate-effective date. Stored as a JSON snapshot column on the invoice (and mirrored
  on-device), **never recomputed** after finalize.
- **Rationale**: Tax rules change over time; a finalized invoice must forever reflect the rates and
  rules *as of issue* (legal + audit requirement, and matches how spec 013 keeps voucher history
  immutable). A reproducible snapshot also lets support/audit explain "why was this IGST and not
  CGST+SGST" months later, and is the source for GST return reconciliation. The app's existing
  `TaxCalculationResult.metadata` already captures `is_intra_state`/`source_state` — formalize that
  into a persisted snapshot.
- **Alternatives considered**: Recompute tax from current rules on demand (rejected — backdated rate
  changes would silently alter historical invoices; non-auditable). Store only the final amounts
  (rejected — can't reconstruct *why*, fails audit and return reconciliation).

## R9. Offline-deterministic calculation must be preserved (the hard constraint)

- **Decision**: All advanced-tax logic that affects a **document amount** (RCM treatment, composition
  suppression, nil/exempt/zero-rated, TDS/TCS amount) runs in the **on-device pluggable strategy**
  exactly like the current `IndiaGSTStrategy`, fed by **synced definitions** (TDS/TCS sections,
  composition config, gstTreatment per code) pulled into Room. The calculator stays a **pure
  function** of (taxable base, code, source/dest state, treatment, section) → deterministic result on
  every platform. Server and client share the *definitions*; the *formula* is identical everywhere.
- **Rationale**: The app is offline-first; an invoice created on a plane must compute the same TDS and
  the same CGST/SGST as the backend would. The existing design already achieves this for GST by
  syncing rules/components and running the strategy locally — advanced tax must extend that, not add a
  server round-trip to the calc path. Determinism is the regression guard (a backend recompute must
  match the device byte-for-byte on amounts).
- **Alternatives considered**: Compute advanced tax server-side at finalize (rejected — breaks offline
  invoicing; the device couldn't show the net receivable). Ship a second calculator implementation
  (rejected — two implementations drift; one strategy, synced inputs).

## R10. Money precision — `BigDecimal(19,4)` backend, exact minor-units / `Double`-bridge on device

- **Decision**: All advanced-tax **amounts** are exact: backend `BigDecimal` scale 4 / `DECIMAL(19,4)`
  (consistent with `payment`); on device, withholding/collection amounts that **post to the ledger**
  use the `payment` module's **`Long` minor-units** money type, while the **GST line calc** continues
  in `Double` as today but **rounds half-up to 2 dp** at the line boundary with an explicit round-off
  so a voucher foots. Flag the existing invoice `Double` totals as a known precision risk to converge
  on minor-units in a follow-up.
- **Rationale**: TDS/TCS feed the party ledger, which is already exact (`BigDecimal`/minor-units, spec
  013) — they must use the same exact money to keep the ledger footing to zero. The GST line calc is
  already `Double` and changing it wholesale is out of scope; bounding the error with half-up rounding
  + round-off at the line preserves correctness without a disruptive refactor. The audit snapshot (R8)
  records the rounded, persisted amounts.
- **Alternatives considered**: Compute TDS in `Double` (rejected — it posts to an exact ledger;
  precision drift would un-foot the party balance). Refactor all invoice money to minor-units now
  (rejected — large blast radius across invoice/order; tracked as a separate follow-up).

## R11. Sync model for the new definitions — canonical `/sync`, unlike base `tax`

- **Decision**: New advanced-tax **definitions** that are workspace-authored and client-synced —
  `TdsSection`/`TcsSection` subscriptions/config, composition config, `gstTreatment` overrides — ride
  the **canonical `/sync` contract** (`GET/POST /tax/v1/{resource}/sync`), with their own
  `SyncDelegate`s on device. The base GST *subscription* model (codes/rules/components) stays off-sync
  as today.
- **Rationale**: These definitions are exactly client-authored, UID-keyed, soft-deletable rows — the
  shape `/sync` is built for — and the device must hold them to calculate offline (R9). They're new,
  so there's no reason to inherit the legacy subscribe/unsubscribe off-sync model; using the canonical
  contract gives push/pull/delete-in-band for free.
- **Alternatives considered**: Extend the subscribe model to TDS (rejected — TDS sections aren't a
  global master to subscribe to in the same way; canonical `/sync` of workspace config rows is
  simpler). Server-only definitions fetched per-calc (rejected — breaks offline determinism, R9).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Module placement | Extend `tax`; effects via `payment` ledger + `invoice` document shape (R1) |
| TDS placement | Withholding at settlement → `LedgerEntry(TDS_WITHHELD, CR)`, not a GST component (R2) |
| TCS placement | Collection at settlement → `LedgerEntry(TCS_COLLECTED, DR)`, mirror of TDS (R3) |
| RCM | Boolean liability flip; gates posting/document, not the rate; reuse PoS calc (R4) |
| Composition scheme | `TaxConfiguration` mode + `documentType = BILL_OF_SUPPLY`, no tax line (R5) |
| ITC / reversal | Separate input-credit ledger, decoupled from output-tax calc; phase-gated (R6) |
| Nil/exempt/zero-rated | `gstTreatment` enum per line, deterministically shapes components (R7) |
| Tax audit snapshot | Immutable JSON snapshot at finalize; never recomputed (R8) |
| Offline-deterministic calc | All amount-affecting logic in the on-device strategy, synced inputs (R9) |
| Money precision | `BigDecimal(19,4)`/minor-units for ledger; half-up + round-off on GST line (R10) |
| Sync model | New definitions on canonical `/sync`; base GST stays off-sync (R11) |
