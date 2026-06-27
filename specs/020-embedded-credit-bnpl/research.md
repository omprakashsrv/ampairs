# Phase 0 Research — Embedded Working-Capital Credit / BNPL

All Technical Context unknowns are resolved below. Each item: **Decision · Rationale · Alternatives
considered**. These supersede the inline assumptions in `spec.md` with concrete technical choices.

The defining constraint of this feature: **Ampairs is a Lending Service Provider (LSP) / technology
layer, never a lender.** Under RBI's *Digital Lending Guidelines (2022, consolidated 2025)* and the
*Default Loss Guarantee (DLG)* circular, only a Regulated Entity (RE — an NBFC or bank) may underwrite,
disburse, or sit a loan on its books. Every decision below is shaped by keeping Ampairs strictly on the
LSP side of that line. This feature is **online + partner-dependent by construction** — unlike the rest
of the platform, the credit lifecycle cannot run offline.

---

## R1. Regulatory posture — Ampairs is an LSP, not a lender (the hard boundary)

- **Decision**: A new `credit` bounded context acts purely as an **LSP**: it originates applications,
  packages consented borrower data into a credit-signal payload, hands KYC and underwriting to a partner
  **NBFC/bank RE**, and tracks the resulting loan **for display + repayment-reminder purposes only**.
  Ampairs **never** computes an approve/reject decision, **never** sets price/interest, **never** moves
  loan principal, and **never** holds funds. The loan ledger of record lives at the RE; Ampairs stores a
  *mirror* (read model) keyed by the partner's `lender_loan_id`.
- **Rationale**: RBI requires disbursement RE→borrower and repayment borrower→RE to flow **directly**
  between the regulated entity and the borrower's bank account — no pass-through/pooled account of the
  LSP. Putting the decisioning engine or a funds-movement step in Ampairs would make it an unlicensed
  lender and a money-transmission entity. Keeping Ampairs to origination + signals + display is the only
  posture that is lawful without a lending licence.
- **Alternatives considered**: Ampairs-as-NBFC (rejected — needs ₹-crore net-owned-funds, RBI
  registration, and a balance sheet Ampairs does not have); co-lending where Ampairs funds a tranche
  (rejected — same licensing problem); marketplace that merely deep-links to a lender's app (rejected —
  loses the embedded-data advantage that is the whole point; the *signal export* is the moat).

## R2. What Ampairs must NOT store (data-minimisation as a control)

- **Decision**: A hard **prohibited-data list**, enforced in code review and schema: no lending licence /
  NBFC registration details, no full bank-account numbers or card PANs of the borrower (only masked /
  tokenised handles returned by the partner), no credit-bureau (CIBIL/Experian) raw pull report, no
  interest-rate/APR computation inputs, no underwriting score, no KYC document images (Aadhaar/PAN
  scans), no AA-fetched raw bank statement. Ampairs stores: the application, the **consent artefact**, a
  derived **credit-signal snapshot** (aggregates, not raw rows), the partner's loan-id + status mirror,
  and repayment-schedule metadata the partner returns. KYC and bureau data live with the RE / the AA /
  the KYC provider.
- **Rationale**: The *Digital Personal Data Protection Act (DPDP) 2023* makes Ampairs a **Data Fiduciary**
  for borrower data and a **Data Processor** for anything it handles on the RE's behalf. Minimising what
  is stored shrinks the breach surface, the consent scope, and the audit burden, and keeps Ampairs clear
  of "deemed lender" arguments. Storing a KYC image or a bureau report would pull regulated data into a
  non-regulated store.
- **Alternatives considered**: Store everything "for convenience / future ML" (rejected — DPDP purpose-
  limitation + RBI data-localisation make this a liability, not an asset); store encrypted KYC images
  (rejected — still in-scope for RBI/DPDP and offers no business value to an LSP).

## R3. Lender/NBFC partner abstraction (multi-partner from day one)

- **Decision**: A `LenderPartner` **port/adapter** abstraction: a `LenderAdapter` interface
  (`submitApplication`, `fetchOffers`, `initiateKyc`, `confirmDisbursement`, `pullStatus`,
  `pushRepayment`) with one implementation per integrated RE, selected at runtime by a `LenderRouter`
  (eligibility/product/geography rules). Each adapter normalises the partner's proprietary API to a
  canonical internal contract (`CreditOffer`, `LoanStatus`, `RepaymentSchedule`). Partner credentials live
  in env/secret store, never in DB.
- **Rationale**: REs differ wildly (REST, webhook callbacks, OCEN rails) and churn (commercial terms,
  go-lives, pauses). An adapter layer lets Ampairs add/remove lenders without touching the application
  lifecycle, and lets a single merchant be routed to whichever partner approves them. The router also
  supports A/B and fallback (partner-A declines → re-route to partner-B with fresh consent).
- **Alternatives considered**: Hard-wire one NBFC (rejected — single point of failure; concentration
  risk; no fallback on decline); generic webhook-only integration (rejected — too thin to normalise
  offer/status semantics).

## R4. OCEN and the Account Aggregator rail

- **Decision**: Support **OCEN (Open Credit Enablement Network)** roles explicitly — Ampairs plays
  **LSP + (optionally) Loan Agent**; the partner RE is the **Lender**; an **Account Aggregator (AA)**
  (Sahamati framework) is the data rail for the borrower's *bank-statement* signals when the partner
  needs them. Ampairs **initiates an AA consent request** (purpose, FI types, duration) but the
  AA→RE data flow is **direct**: the raw bank statement goes AA→RE, **never through Ampairs**. Ampairs
  receives only a consent-handle and, optionally, RE-returned derived attributes. OCEN's `Loan
  Application Protocol (LAP)` message shapes inform the canonical `CreditOffer`/`LoanStatus` contracts so
  OCEN-native lenders are a thin adapter.
- **Rationale**: AA is the RBI-sanctioned, consent-first way to share financial data; routing raw
  statements through Ampairs would re-introduce regulated data (R2) and make Ampairs a data bottleneck.
  Aligning the canonical contracts to OCEN means OCEN-compliant lenders integrate with minimal adapter
  code, while non-OCEN REST lenders still fit the same `LenderAdapter`.
- **Alternatives considered**: Ampairs screen-scrapes / aggregates bank data itself (rejected — illegal
  post-AA, and a huge data-liability); ignore OCEN and do bespoke per-lender integrations only (rejected —
  loses standardisation and future ONDC-credit interoperability).

## R5. Credit-signal export from the existing ledger (the moat — consent-gated)

- **Decision**: A `CreditSignalService` derives a **`CreditSignalSnapshot`** from data Ampairs **already
  holds** — the spec-013 party ledger (`PartyBalance`, `LedgerEntry`, `PaymentAllocation` aging),
  finalized GST invoices (sales velocity, ticket size, seasonality), and order history. The snapshot is
  **aggregate features only** (e.g. `avg_monthly_sales`, `receivables_aging_dpd_buckets`,
  `gst_filing_consistency`, `sales_trend_slope`, `unique_active_customers`), computed **only after** an
  explicit, purpose-bound borrower consent, versioned and hash-stamped, and exported to the partner via
  the adapter. Raw customer-level rows are never exported.
- **Rationale**: This is the entire commercial thesis — Ampairs can underwrite a thin-file kirana/SME on
  *transaction behaviour* a bureau can't see. Exporting **aggregates** (not raw customer rows) satisfies
  DPDP data-minimisation and protects the merchant's own customers' data, while still giving the RE a
  rich, differentiated signal. Versioning + hashing makes the snapshot an auditable artefact the RE can
  rely on.
- **Alternatives considered**: Export the raw ledger/invoice rows (rejected — leaks the merchant's
  customers' PII; over-broad consent; DPDP violation); let the RE pull from Ampairs DB directly (rejected —
  no DB sharing across the regulatory boundary; must be a consented, controlled, audited export).

## R6. Consent model — DPDP + RBI digital-lending consent

- **Decision**: A first-class, **immutable `CreditConsent`** artefact per application: explicit purpose
  ("share business credit-signals with {partner} for loan assessment"), the exact **data categories** and
  **time window** covered, partner identity, timestamp, IP/device, and a server-minted consent-ref. Consent
  is **granular** (signal-export consent ≠ AA consent ≠ KYC consent are separate artefacts), **revocable**
  (revocation halts further export and is propagated to the partner), and **logged append-only**. No
  feature step proceeds without the matching live consent.
- **Rationale**: Both DPDP (notice + purpose + revocability) and the RBI digital-lending rules (clear,
  auditable borrower consent; Key Fact Statement before acceptance) require a defensible consent trail.
  Making consent immutable + append-only and gating every export/handoff on it turns compliance into a
  structural invariant rather than a policy hope.
- **Alternatives considered**: A single blanket consent at signup (rejected — not purpose-bound, not
  revocable per-purpose, fails DPDP); UI-only checkbox with no stored artefact (rejected — unauditable,
  indefensible in a regulatory review).

## R7. Loan application lifecycle & state machine

- **Decision**: A `CreditApplication` aggregate with an explicit state machine:
  `DRAFT → CONSENTED → SIGNALS_EXPORTED → SUBMITTED → (OFFERED | DECLINED) → KYC_PENDING →
  KYC_COMPLETED → DISBURSEMENT_PENDING → DISBURSED → (CLOSED | DEFAULTED)`; plus `EXPIRED`/`WITHDRAWN`.
  Each transition is event-sourced as `CreditApplicationEvent` rows (append-only), and partner-driven
  transitions arrive via **webhooks** (`SUBMITTED→OFFERED`, `→DISBURSED`, etc.) validated by signature.
  Ampairs owns the *state machine*; the partner owns the *decision* at each gated transition.
- **Rationale**: A loan origination is inherently a long-running, multi-party, partly-asynchronous saga.
  An explicit state machine + append-only event log gives idempotent webhook handling, a clean audit
  trail, and resumability if the partner is slow or a callback is missed (a reconciliation poll fills
  gaps — see R10).
- **Alternatives considered**: Boolean flags on a flat row (rejected — can't express the async,
  partner-gated saga; race-prone); store state only at the partner (rejected — Ampairs needs a local
  read model to drive merchant UX and reminders).

## R8. BNPL-at-checkout vs term loan for the merchant — two products, one rail

- **Decision**: Model two **credit products** over the same lifecycle:
  (a) **Merchant working-capital term loan** — a lump sum disbursed to the merchant's bank account,
  fixed-tenor EMI; underwritten on the R5 signal snapshot.
  (b) **BNPL at checkout (B2B)** — a revolving/transaction-level line that funds a *purchase order* the
  merchant places with **their** supplier (or a consumer's purchase at the merchant's counter, phase 3):
  per-transaction credit drawdown against a partner-approved limit, settled on a billing cycle. A
  `CreditProduct` discriminator + `CreditLine` (approved limit, available limit, utilisation) underpins
  BNPL; term loans skip the line and disburse directly.
- **Rationale**: Both are the same LSP origination flow with different disbursement semantics; sharing the
  application/consent/signal machinery avoids duplication. BNPL needs a *line + drawdown* concept the term
  loan doesn't; isolating that in `CreditLine` keeps the term-loan path simple. The merchant value props
  differ (cash for growth vs deferred payables), so they're surfaced as distinct products.
- **Alternatives considered**: One-size loan only (rejected — misses the high-frequency BNPL use the
  embedded-data story is strongest for); build BNPL as a separate module (rejected — duplicates
  origination/consent; same regulatory rail).

## R9. Disbursement & repayment tracking — mirror, not mover

- **Decision**: Ampairs **never** disburses or collects. The RE disburses **directly** to the merchant's
  verified bank account and collects via its own NACH/UPI-autopay mandate. Ampairs records a
  `Disbursement` mirror (amount, date, `lender_loan_id`) and a `RepaymentSchedule` (instalments, due
  dates, status) **as reported by the partner** via webhook/poll, and surfaces **due-date reminders** and
  a repayment view to the merchant. A repayment *event* (paid/overdue/bounced) is mirrored, not authored —
  Ampairs' copy is display-only and reconciled against the partner as source of truth (R10).
- **Rationale**: Funds-flow through the LSP is the bright regulatory line (R1). Mirroring lets Ampairs give
  the merchant a useful repayment UX (reminders reduce delinquency, improving future signal scores) without
  ever touching money or becoming a collection agent — the RE/its agents own collections.
- **Alternatives considered**: Ampairs collects EMIs and remits to RE (rejected — money-transmission +
  pooled-account violation); no repayment view at all (rejected — loses the stickiness and the
  delinquency-reduction value).

## R10. Idempotency & reconciliation across the partner boundary

- **Decision**: Every outbound partner call carries a deterministic **idempotency key**
  (`{application_uid}:{step}` or `{loan_id}:{event_seq}`); every inbound webhook is **dedup-keyed** on
  `(partner, event_id)` and processed **at-most-once** against the append-only event log; signatures are
  HMAC/ mTLS-verified. A scheduled **reconciliation poll** (`pullStatus`) per active loan repairs missed
  webhooks and is the tie-breaker — **partner state always wins** over a stale local mirror. Disbursement
  confirmation is double-gated: a webhook + a poll must agree before the local mirror flips to `DISBURSED`.
- **Rationale**: Money-adjacent flows must be exactly-correct under retries, duplicate callbacks, and
  network partitions. Idempotency keys + dedup + a reconciler give safety without distributed
  transactions; "partner wins" resolves the inevitable divergence deterministically and keeps Ampairs'
  mirror honest.
- **Alternatives considered**: Trust webhooks alone (rejected — callbacks get lost/duplicated; a missed
  `DISBURSED` would strand a merchant); two-phase commit with the partner (rejected — partners don't offer
  it; over-engineered for a mirror).

## R11. Online-only & graceful degradation on the mobile app

- **Decision**: The `feature/credit` mobile module is **online-only** for every state-changing step
  (consent capture, application submit, KYC handoff, offer accept, disbursement/repayment status). It does
  **not** ride the offline `/sync` engine for the loan lifecycle; instead it calls the backend live and
  shows explicit "requires internet / awaiting lender" states. A **read-only cache** of the latest loan
  status + next-due reminder may be stored locally (Room, `synced=true`, pull-only) so a merchant offline
  can still *see* their balance and due date, but **cannot act**.
- **Rationale**: A loan decision/disbursement cannot be authored offline — there's a regulated partner in
  the loop and money at stake; offline authoring would create unreconcilable phantom state. The rest of
  Ampairs is offline-first, so this exception must be deliberate and clearly signposted in the UX. A
  pull-only status cache preserves the "see your dues anywhere" value without enabling unsafe offline
  actions.
- **Alternatives considered**: Force the whole feature onto the `/sync` contract (rejected — the lifecycle
  is partner-gated and online by nature; faking it offline is dangerous); no local cache at all (rejected —
  merchant can't see dues on a flaky rural connection, hurting reminders).

## R12. Tenant model, scoping & cross-module integration

- **Decision**: All `credit` entities extend `OwnableBaseDomain` and are workspace-scoped (`@TenantId` →
  `X-Workspace-ID`), tenant context set at the controller. The signal snapshot is built via **public
  service interfaces** of `payment` (party-ledger aggregates), `invoice` (finalized-invoice aggregates),
  and `order` — never by reaching into their repositories — and consumes a published
  `LedgerRecomputedEvent`/invoice events where helpful. The borrower is the **workspace owner** (the
  merchant), identified by workspace + KYC at the RE; this is *not* a per-customer credit feature.
- **Rationale**: Matches the constitution's tenant-isolation and module-boundary rules and reuses the
  exact integration style spec-013 established (events + public services). Scoping credit to the merchant
  workspace (not the merchant's customers) keeps the data subject unambiguous for DPDP/RBI.
- **Alternatives considered**: Cross-tenant signal aggregation (rejected — not needed; the merchant is one
  tenant; would require `nativeQuery=true` + extra consent for no benefit here); embed credit fields on
  `Workspace`/`Customer` (rejected — crosses module boundaries; bloats those contexts).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Regulatory role of Ampairs | LSP / technology layer only — never a lender (R1) |
| Prohibited data | No licence/KYC-image/bureau/PAN/raw-statement/score storage (R2) |
| Lender integration shape | `LenderAdapter` port + `LenderRouter`, multi-partner (R3) |
| Bank-data rail | Account Aggregator consent-handle; raw statement AA→RE direct (R4) |
| OCEN | Canonical contracts modelled on OCEN LAP; OCEN lenders = thin adapter (R4) |
| Credit signals | Aggregate `CreditSignalSnapshot` from spec-013 ledger + invoices, consent-gated (R5) |
| Consent | Immutable, granular, revocable `CreditConsent` per purpose (DPDP+RBI) (R6) |
| Application lifecycle | Event-sourced state machine, partner-gated transitions via webhook (R7) |
| Products | Term working-capital loan + B2B BNPL line over one rail (R8) |
| Disbursement/repayment | RE moves funds directly; Ampairs mirrors only (R9) |
| Idempotency/reconciliation | Idempotency keys + dedup webhooks + reconciler; partner wins (R10) |
| Offline posture | Online-only lifecycle; pull-only read cache for status/dues (R11) |
| Tenant & integration | Workspace-scoped; signals via public service interfaces (R12) |
