# Phase 0 Research — UPI Collection & Payment Links

All Technical Context unknowns are resolved below. Each item: **Decision · Rationale · Alternatives
considered**. These supersede the inline assumptions in `spec.md` with concrete technical choices.
The feature upgrades UPI from a *manual payment-mode label* (today it is only a `PaymentMode.UPI` value
on a `PaymentVoucher`) to **real collection rails**: dynamic UPI QR, UPI collect requests, shareable
payment links, and webhook-driven auto-reconciliation that posts a receipt into the existing spec-013
payment ledger.

---

## R1. Module boundary — new `collection` module vs extend `payment`

- **Decision**: A **new backend bounded context `collection`** owns the payment-gateway integration,
  collect-request/link lifecycle, QR generation and inbound webhooks. It does **not** own the money
  ledger — on a confirmed payment it calls the existing `payment` module's public `PaymentVoucherService`
  to post a receipt (`PaymentVoucher` RECEIVED → `LedgerEntry` PAYMENT_IN CR). The two modules talk via
  a public service interface and a domain event (`CollectionSettledEvent`), never repositories.
- **Rationale**: A PSP integration (provider creds, webhooks, settlement reconciliation) is a distinct
  concern from the subsidiary party ledger. Keeping `payment` as the single source of ledger truth
  (spec 013's invariant `opening + ΣDr − ΣCr = closing`) is preserved — `collection` is just one more
  *author* of a receipt voucher, exactly as a manual receipt is today. This honours Principle IX and
  avoids a PSP webhook controller living inside the ledger context.
- **Alternatives considered**: Add gateway code to `payment` (rejected — couples regulated PSP
  integration + webhooks to the ledger; bloats it). A generic `payments-gateway` shared library
  (rejected — premature; this is a bounded context with its own persistence and lifecycle).

## R2. Provider abstraction (Razorpay / Cashfree / PhonePe)

- **Decision**: A `UpiCollectionProvider` port — `createCollectRequest`, `createPaymentLink`,
  `createDynamicQr`, `fetchPaymentStatus`, `verifyWebhookSignature`, `refund` — with per-PSP
  implementations (`RazorpayProvider`, `CashfreeProvider`, `PhonePeProvider`). Provider is resolved
  per-workspace by a `CollectionProviderResolver`; PSP key id/secret + webhook secret come from
  environment + an encrypted per-workspace credential row. Each provider maps its own status vocabulary
  to a canonical internal `CollectionStatus`.
- **Rationale**: Mirrors the proven `notification` provider pattern (MSG91 primary / SNS fallback) and
  the e-invoice GSP abstraction (feature 015). PSPs differ in field names, signature scheme and status
  strings but share the same conceptual flow (request → pending → paid/failed/expired). A port lets a
  workspace pick its PSP and lets us add one without touching reconciliation logic.
- **Alternatives considered**: Hardcode one PSP (rejected — vendor lock-in, no per-workspace choice,
  no fallback). UPI-deeplink-only with no PSP (rejected — a bare `upi://` intent has no server-side
  confirmation; auto-reconciliation requires a PSP webhook).

## R3. Three collection instruments — one entity or three

- **Decision**: One **`CollectionRequest`** aggregate with a `type` ∈ {`COLLECT`, `LINK`,
  `DYNAMIC_QR`} and a shared lifecycle, plus type-specific fields (collect: payer VPA; link: short URL,
  channel; QR: the UPI intent string / image ref). It carries `partyUid`, optional `invoiceUid`,
  `amountMinor`, `expiresAt`, `status`, `providerOrderId`, and the reconciliation link
  (`paymentVoucherUid` once settled).
- **Rationale**: All three are "ask a party to pay X, then confirm" — same status machine
  (`CREATED → PENDING → PAID | FAILED | EXPIRED | CANCELLED`), same webhook reconciliation, same ledger
  posting. A single entity keeps the reconciler uniform; a discriminator handles the small per-type
  differences. The mobile list ("pending collections") is one feed.
- **Alternatives considered**: Three separate entities/tables (rejected — triples the sync/reconcile
  surface for one lifecycle). Model the link as a column on the voucher (rejected — a request precedes
  and may never produce a voucher; conflates ask with receipt).

## R4. Idempotent reconciliation — the crux

- **Decision**: A confirmed payment posts **exactly one** receipt `PaymentVoucher` with a
  **deterministic uid** `RCP_<providerPaymentId>`. Reconciliation runs in a `@Transactional` block
  guarded by: (1) a unique constraint on `CollectionRequest.providerPaymentId` and on the derived
  voucher uid; (2) a check that the request isn't already `PAID`. Both the **webhook** and a
  **fetch-status reconciler** (polling fallback) funnel through the same `reconcile(providerPaymentId)`
  method, so a webhook + a poll for the same payment collapse to one posting. The amount is validated
  against the request amount before posting.
- **Rationale**: PSPs deliver webhooks **at-least-once** (duplicates, retries, out-of-order) and may
  fire before/after a status poll — double-posting a receipt would corrupt the ledger
  (spec 013 SC-006). A deterministic uid + unique constraint + single reconcile path makes posting
  exactly-once by construction, the same discipline spec 013 used for `LDG_<sourceUid>`.
- **Alternatives considered**: Trust the webhook only with no dedupe (rejected — duplicates double-post).
  Post on every webhook and dedupe receipts later (rejected — transient bad balances, manual cleanup).
  Insert-then-detect-conflict without the pre-check (acceptable as the DB backstop, but the explicit
  status check avoids exception-driven flow).

## R5. Webhook signature verification & security

- **Decision**: Every PSP webhook hits `POST /collection/v1/webhooks/{provider}` (a **public,
  unauthenticated** endpoint — PSPs don't carry our JWT). The raw body + the provider's signature header
  are verified via `provider.verifyWebhookSignature(rawBody, headers, workspaceWebhookSecret)` (HMAC-SHA256
  for Razorpay/Cashfree; PhonePe X-VERIFY checksum) **before** any parsing. The workspace is resolved
  from the request's metadata/notes (we stamp `workspace_id` + `request_uid` when creating the order),
  then tenant context is set for the ledger posting. Unverified webhooks are dropped with 400 and logged.
- **Rationale**: Webhooks are an unauthenticated attack surface; signature verification on the **raw**
  body (before JSON parsing, which can re-order/normalize) is mandatory. Because there's no
  `X-Workspace-ID` header, the tenant must be carried in PSP notes and set explicitly in the handler
  (controller-level, per rule 05/06). This mirrors the secrets discipline of feature 015's GSP callbacks.
- **Alternatives considered**: Authenticate webhooks with our JWT (rejected — PSPs can't send it).
  Parse first then verify (rejected — signature is over the raw bytes; parsing breaks it). Single global
  webhook secret (rejected — per-workspace secret limits blast radius).

## R6. Money representation

- **Decision**: Internally **`amountMinor: Long` (paise)** end-to-end in `collection`, matching the PSP
  contract (Razorpay/Cashfree quote amounts in paise) and the mobile minor-unit convention from spec
  013. When posting to `payment`, convert paise → `BigDecimal` scale 4 once at the boundary (the
  `PaymentVoucherService` already takes `BigDecimal`). The receipt amount is validated to equal the
  request amount before posting; partial/over payments are rejected to `FAILED` with a reason (UPI
  collect is exact-amount).
- **Rationale**: Paise is exact and is the PSP's native unit, so no rounding occurs in the gateway path;
  a single conversion at the ledger boundary avoids drift (spec 013 R5). Exact-amount enforcement keeps
  reconciliation unambiguous.
- **Alternatives considered**: `BigDecimal` throughout `collection` (rejected — PSP APIs are integer
  paise; needless conversions). Allow part-payment auto-allocation (rejected for Phase 1 — UPI collect
  is exact; part-payment belongs to the manual receipt/allocation flow).

## R7. Collect-request & link lifecycle / expiry

- **Decision**: States: `CREATED → PENDING (sent to PSP) → PAID | FAILED | EXPIRED | CANCELLED`. A
  collect request has a short TTL (PSP-enforced, ~T+ minutes, mirrored in `expiresAt`); a payment link
  has a configurable expiry (default 7 days). A `@Scheduled` sweeper marks lapsed requests `EXPIRED` and
  closes the PSP order. Cancellation is allowed only from `PENDING`. Re-share regenerates the link/QR
  for an active request rather than creating a new one (avoids duplicate live links for one bill).
- **Rationale**: UPI collect requests genuinely expire (NPCI mandates a short approval window); links
  must expire for security/staleness. A single sweeper keeps state honest even if a terminal webhook is
  missed; the poll reconciler (R4) heals the "paid but webhook lost" case at the same time.
- **Alternatives considered**: Never expire (rejected — stale live links are a fraud/UX risk). New
  request per re-share (rejected — multiple payable links for one invoice, ambiguous reconciliation).

## R8. Link sharing channel & integration with notification

- **Decision**: Sharing a payment link is delegated to the existing `notification` module
  (WhatsApp/SMS/email) via its public `NotificationService` — `collection` produces the short link +
  templated message; `notification` delivers and tracks. The QR (for in-person) is returned to the
  client to render. This same outbound capability is what feature 017 (dunning) reuses to embed the link.
- **Rationale**: Channel delivery is already solved by `notification` (queue, retry, provider
  abstraction); duplicating it in `collection` would violate module boundaries. Returning the link as
  data lets dunning (017) compose it without `collection` knowing about reminder schedules.
- **Alternatives considered**: Build SMS/WhatsApp sending in `collection` (rejected — duplicates
  `notification`). Only return a URL with no built-in send (rejected — the brief asks for shareable
  links; one-tap send is the value).

## R9. Settlement status & T+1 reconciliation

- **Decision**: Beyond payment capture, track **settlement** separately: a `settlementStatus`
  (`UNSETTLED → SETTLED`) and `settledAt`/`utr` on the `CollectionRequest` (or a small
  `Settlement` record), updated from the PSP's settlement webhook/report. The payment receipt posts at
  **capture** time (party's dues clear immediately); settlement (money actually hitting the bank, T+1)
  is informational and is what feature 024 (bank reconciliation) later matches the bank credit against
  by `utr`.
- **Rationale**: A customer's outstanding must clear the instant UPI succeeds, independent of when the
  PSP settles to the merchant bank (T+1). Conflating the two would delay receivable clearance. The
  `utr` is the join key for downstream bank reconciliation.
- **Alternatives considered**: Post the receipt only on settlement (rejected — delays clearing the
  party's balance by a day). Ignore settlement (rejected — feature 024 needs the UTR linkage and the
  merchant needs payout visibility).

## R10. Offline behaviour — what is genuinely possible

- **Decision**: Creating a collect request / link / QR and confirming a payment all **require
  connectivity** (PSP call + webhook) and are therefore **online-only commands**, not offline-authored
  sync entities. The mobile app's `feature/collection` is **pull-only** for the `CollectionRequest`
  feed (so the "pending collections" list and a settled receipt appear after sync) and exposes online
  command actions (create QR, send link). A QR string, once created and pulled, renders offline. The
  resulting receipt voucher arrives in `feature/payment` via its normal pull.
- **Rationale**: There is no offline authority for a real-money collection — the PSP mints the order and
  the bank confirms it. Pretending otherwise (queueing a "payment received" offline) would risk
  posting receipts that never actually happened. Pull-only display + online commands is the honest model
  (consistent with feature 015's IRN).
- **Alternatives considered**: Let the app author a "UPI received" voucher offline (rejected — that is
  the existing *manual* receipt flow; real-rail collection must be PSP-confirmed). Full offline queue of
  collect requests (rejected — they'd expire before reaching the PSP).

## R11. Where the receipt voucher gets posted (mobile vs backend)

- **Decision**: For real-rail collections the **backend posts the receipt** (the webhook lands
  server-side). The mobile app does **not** author the ledger entry for these — it pulls the resulting
  `PaymentVoucher`/`LedgerEntry` through `feature/payment`'s existing delegates. This differs from a
  manual receipt (which the document-authoring client posts per spec 013 R4) precisely because the
  confirmation is server-side.
- **Rationale**: The authority for a PSP-confirmed payment is the verified webhook on the server; the
  client cannot know the payment succeeded until it syncs. Backend posting + client pull keeps the
  ledger correct without a second posting path.
- **Alternatives considered**: Client posts on a push notification "you got paid" (rejected — push is
  best-effort and unverified; the webhook is the truth).

## R12. Settings

- **Decision**: Reuse the `setting` module via a `CollectionSettingDefinitions` provider gated by an
  installed `collection` module: `collection_enabled`, `collection_provider`, `default_link_expiry_days`,
  `collect_request_ttl_minutes`, `merchant_vpa` (for static/intent QR), `auto_send_channel`.
- **Rationale**: Matches how `payment`/`invoice` expose toggles (spec 013 R11); no new settings
  infrastructure.
- **Alternatives considered**: Hardcode provider/expiry (rejected — workspaces use different PSPs and
  policies).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Module placement | New `collection` context; posts receipts into `payment` via service/event (R1) |
| PSP abstraction | `UpiCollectionProvider` port + per-workspace resolver, encrypted creds (R2) |
| Instrument modelling | One `CollectionRequest` aggregate with `type` discriminator (R3) |
| Idempotent reconciliation | Deterministic `RCP_<providerPaymentId>` + unique constraint + single reconcile path (R4) |
| Webhook security | Public endpoint, raw-body signature verify, workspace from PSP notes (R5) |
| Money | `Long` paise internally; convert once to `BigDecimal` at ledger boundary (R6) |
| Lifecycle / expiry | State machine + scheduled expiry sweeper; re-share, not re-create (R7) |
| Link delivery | Delegate to `notification`; return QR/link as data (R8) |
| Settlement | Separate `settlementStatus`/UTR; receipt posts at capture, not settlement (R9) |
| Offline behaviour | Online-only commands; mobile pull-only feed (R10) |
| Receipt posting authority | Backend posts on verified webhook; mobile pulls (R11) |
| Settings | `StoreSetting` + `CollectionSettingDefinitions` (R12) |
