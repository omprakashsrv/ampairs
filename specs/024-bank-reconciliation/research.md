# Phase 0 Research — Bank Reconciliation

All Technical Context unknowns are resolved below. Each item: **Decision · Rationale · Alternatives
considered**. These supersede the inline assumptions in `spec.md` with concrete technical choices.
The feature imports bank statements (CSV / MT940 / account-aggregator) and auto-matches statement lines
against the spec-013 payment ledger's receipts and payments, so a business owner can see which bank
credits/debits correspond to which `PaymentVoucher` and resolve the rest.

---

## R1. Module boundary — new `banking` module vs extend `payment`

- **Decision**: A **new backend bounded context `banking`** owns bank accounts, imported statement
  lines, the matching engine and match records. It reads the payment ledger through `payment`'s public
  services (receipts/payments by date/amount/UTR) and, on a confirmed match, **annotates** the
  `PaymentVoucher` (marks it bank-reconciled / records the UTR + statement-line link) via a public
  `payment` interface — it does **not** create or mutate ledger entries. A bounce/reversal discovered in
  the statement is handed back to `payment` as a `bounce`/reversal command (the existing
  `POST /vouchers/{uid}/bounce`).
- **Rationale**: Statement ingestion + fuzzy matching is a distinct concern from the subsidiary ledger
  (Principle IX). Keeping `payment` the sole ledger author preserves the spec-013 foot-to-zero invariant;
  `banking` is a *reconciliation overlay* that links external bank lines to existing vouchers. This also
  isolates the messy, format-specific import parsers from the clean ledger context.
- **Alternatives considered**: Add statement tables + matching to `payment` (rejected — couples bank-file
  parsing to the ledger; bloats it). A `treasury` mega-module also doing payouts/settlement (rejected —
  premature; settlement tracking already lives with `collection` feature 016 via the UTR).

## R2. Statement-line entity & multi-account

- **Decision**: A `BankAccount` (per workspace: bank name, masked account no, IFSC, accountType,
  openingBalance, currency) and a `BankStatementLine` (bankAccountUid, valueDate, txnDate, amountMinor,
  drCr, narration/description, refNo/`utr`/chequeNo parsed out, runningBalance, sourceImportUid,
  `matchStatus`). Lines are **immutable once imported**; matching state lives in a separate `BankMatch`
  record, never by mutating the line's facts. Multi-account is first-class — every line and match is
  scoped to a `bankAccountUid`.
- **Rationale**: A statement line is an external fact (what the bank says); reconciliation is our
  interpretation of it — separating them keeps re-import idempotent and lets a line be re-matched without
  rewriting bank data. Most SMBs run several accounts (current + UPI settlement + OD); scoping everything
  to an account is non-negotiable for correct balances.
- **Alternatives considered**: One flat table mixing line + match state (rejected — re-matching mutates
  bank facts; messy audit). Single implicit account (rejected — wrong for multi-account businesses; the
  brief calls out multi-account).

## R3. Import formats & a parser abstraction

- **Decision**: A `StatementParser` port with implementations: `CsvStatementParser` (column-mapping
  driven — banks differ wildly, so a per-bank/per-workspace `ColumnMapping` profile maps headers →
  canonical fields), `Mt940Parser` (SWIFT MT940 `:61:`/`:86:` tag parsing), and an
  `AccountAggregatorParser` (the RBI AA / OnMoney-AA JSON schema). Each yields a canonical
  `List<ParsedLine>`; a `StatementImport` record tracks the file, account, row counts, and dedupe result.
- **Rationale**: Bank CSVs have no standard layout (date formats, debit/credit columns vs signed amount,
  ₹ formatting), MT940 is a fixed SWIFT grammar, and account-aggregator is structured JSON — three
  genuinely different parsing problems behind one canonical output. The column-mapping profile makes CSV
  import reusable per bank without code. This mirrors the provider-port pattern used in features
  015/016/017.
- **Alternatives considered**: CSV-only (rejected — MT940 and AA are explicitly in scope). A single
  hardcoded CSV layout (rejected — every bank's export differs; mapping profiles are essential). Parse on
  the client (rejected — heavy, format-specific, and the ledger lives server-side).

## R4. Import idempotency & de-duplication

- **Decision**: Re-importing an overlapping statement must **not** create duplicate lines. Each line gets
  a **deterministic fingerprint** uid `BSL_<hash(bankAccountUid, valueDate, amountMinor, drCr,
  refNo|narration)>`; import is an upsert keyed on it (unique constraint). The `StatementImport` reports
  inserted/skipped counts. Where the bank provides a `runningBalance`, the importer validates line
  continuity (each line's balance = previous + signed amount) and flags gaps.
- **Rationale**: Users routinely re-download an overlapping date range; without a stable fingerprint
  every overlap doubles the lines and wrecks matching. Hashing the bank's own immutable facts is the only
  reliable key (no bank txn id in CSV). Running-balance continuity catches a truncated/garbled file early.
  Same deterministic-uid idempotency discipline as features 013/015/016.
- **Alternatives considered**: Row-index keying (rejected — shifts on overlap). Trust an internal txn id
  (rejected — CSV rarely has one). No dedupe (rejected — guarantees corruption on re-import).

## R5. Matching engine — heuristics, fuzzy, confidence

- **Decision**: A multi-pass `MatchingEngine` scoring statement-line ↔ ledger-voucher candidates:
  **Pass 1 (exact ref)** — `utr`/refNo/chequeNo on the line equals the voucher's `referenceNumber`
  (the UTR populated by feature 016 collections is the strongest signal) → `confidence = EXACT`.
  **Pass 2 (amount + date window)** — equal `amountMinor`, same dr/cr direction, `valueDate` within a
  ±N-day window of `voucherDate` → high confidence; ambiguity (multiple candidates) lowers it.
  **Pass 3 (fuzzy)** — amount within tolerance and narration token-overlap with the party name → MEDIUM.
  A line above the auto-match threshold (default EXACT/HIGH, single candidate) is **auto-matched**;
  everything else becomes a **suggestion** for manual review. One-to-many (a bank credit covering several
  receipts) and many-to-one are supported via a `BankMatch` that links one or more line uids to one or
  more voucher uids.
- **Rationale**: Reconciliation accuracy comes from layering cheap exact signals before expensive fuzzy
  ones; the UTR join (from UPI rails, feature 016) makes the common case exact. A confidence score lets
  the system auto-clear the obvious and escalate only the ambiguous — the difference between a tool that
  saves time and one that creates review noise. Supporting N:M matches handles batched settlements (a
  single UPI payout settling many receipts).
- **Alternatives considered**: Exact-only matching (rejected — misses date-shifted/rounded lines). Pure
  fuzzy on amount (rejected — false positives on common round amounts). Auto-match everything (rejected —
  wrong matches silently corrupt the reconciliation; manual gate on low confidence is essential).

## R6. Where the match lives & money representation

- **Decision**: A `BankMatch` (statementLineUids, voucherUids, amountMinor, confidence, matchType
  AUTO/MANUAL, status `SUGGESTED → CONFIRMED → REJECTED`, matchedBy, matchedAt) is the join. Confirming a
  match annotates the linked `PaymentVoucher`(s) as bank-reconciled (via `payment`'s public interface),
  not by editing the ledger. Money is **`Long` paise** in `banking` (banks quote rupees/paise; mobile
  minor-unit convention); a tolerance compare (±`match_amount_tolerance_paise`) handles rounding. No
  currency math posts to the ledger from here.
- **Rationale**: Keeping the match as a separate confirmable record (with audit of who matched what)
  means a wrong auto-match can be rejected without touching bank facts or the ledger. Paise is exact and
  matches the PSP/settlement UTR amounts from feature 016. Annotating rather than posting keeps the
  ledger's foot-to-zero invariant intact.
- **Alternatives considered**: Write a new "bank receipt" ledger entry on match (rejected — the receipt
  already exists from the manual/UPI flow; double-posting). Store match flags on the line (rejected —
  loses N:M and audit).

## R7. Unmatched handling & the reconciliation report

- **Decision**: Three unmatched buckets surfaced in a reconciliation view per account+period:
  (a) **bank lines with no ledger voucher** (e.g. bank charges, interest, a cash deposit not yet
  recorded) — actionable as "create a voucher/adjustment from this line" (hands to `payment` as a
  receipt/`AdjustmentVoucher`); (b) **ledger vouchers with no bank line** (uncleared cheques, in-transit)
  — informational; (c) **suggested matches awaiting confirmation**. The report ties out:
  `opening bank balance + Σ matched + Σ unmatched-bank = statement closing balance`.
- **Rationale**: The value of reconciliation is the *exceptions* — what's on the bank but not the books
  and vice-versa. Letting the user spawn a voucher straight from an orphan bank line closes the loop
  (bank charges/interest are the classic case). A tie-out invariant is the regression guard, analogous to
  spec 013's ledger foot-to-zero.
- **Alternatives considered**: Only show matches (rejected — hides the exceptions that matter). Auto-create
  vouchers for every orphan (rejected — bank charges vs genuine unrecorded receipts need human judgement).

## R8. Reversal / bounce reconciliation

- **Decision**: A statement **debit reversing an earlier credit** (cheque return / NACH bounce — often
  flagged in narration, e.g. `RET`/`I/W RETURN`/`CHQ RETURN`) is matched to the original receipt's
  voucher and triggers `payment`'s existing **bounce** flow (`POST /vouchers/{uid}/bounce`), which posts
  a reversal `LedgerEntry` restoring the receivable (spec 013 R7) — `banking` does not reverse anything
  itself. The bank's return-charge debit line is reconciled as a bank-charge adjustment.
- **Rationale**: Bounces are a real reconciliation case and the ledger already models them
  (`clearanceStatus` → BOUNCED + reversal). Routing a detected return through the existing bounce command
  keeps a single reversal implementation and preserves audit. Detecting it from narration patterns is the
  pragmatic SMB approach (no structured return code in CSV).
- **Alternatives considered**: Reverse in `banking` (rejected — duplicates the bounce logic, risks ledger
  drift). Ignore bounces (rejected — they're in the brief and materially affect dues).

## R9. Offline behaviour & where reconciliation runs

- **Decision**: Statement **import and the matching engine run backend-side** — file parsing, the
  ledger-wide candidate search and N:M scoring are server work and depend on the full ledger. The mobile
  app surfaces reconciliation as: configure `BankAccount`s (offline-editable synced config), **upload** a
  statement file (online command, like `file` multipart), and **review/confirm/reject** suggested matches
  and view the reconciliation report (pull-only state + confirm/reject online actions). Imported lines,
  matches and the report are **pull-only** synced entities.
- **Rationale**: Matching needs the authoritative ledger and isn't meaningful on a partial offline copy;
  file upload is inherently online (mirrors how the app's `file` images upload via multipart, not central
  sync). Bank accounts, however, are plain config that fits the offline-first sync model. This is the same
  honest split used in features 015–017 (server-authored state pulled; config synced).
- **Alternatives considered**: Match on-device (rejected — needs the whole ledger + heavy fuzzy scoring;
  partial offline data gives wrong matches). Make import a synced push (rejected — binary files/large
  statements don't ride a JSON `List<T>` sync body, exactly why `file` is off the contract).

## R10. Settings & enablement

- **Decision**: Reuse the `setting` module via a `BankingSettingDefinitions` provider gated by an
  installed `banking` module: `banking_enabled`, `auto_match_min_confidence` (default HIGH),
  `match_date_window_days` (default 3), `match_amount_tolerance_paise` (default 0), `default_csv_profile`,
  `bounce_narration_patterns`.
- **Rationale**: Matches spec 013/016/017; tunable thresholds let conservative workspaces require manual
  confirmation while others auto-clear aggressively, without a release.
- **Alternatives considered**: Hardcode thresholds/windows (rejected — risk appetite and bank formats
  differ per workspace).

---

## Resolved unknowns summary

| Unknown (Technical Context) | Resolution |
|---|---|
| Module placement | New `banking` overlay context; annotates `payment`, never posts (R1) |
| Statement-line / multi-account | `BankAccount` + immutable `BankStatementLine`; match state separate (R2) |
| Import formats | `StatementParser` port: CSV (mapping profiles) / MT940 / AA JSON (R3) |
| Import idempotency | Deterministic fingerprint uid + upsert; running-balance continuity (R4) |
| Matching engine | Multi-pass exact→amount/date→fuzzy with confidence; N:M (R5) |
| Match record / money | `BankMatch` join, confirmable; `Long` paise + tolerance; annotate not post (R6) |
| Unmatched handling | Three buckets + spawn-voucher-from-orphan + tie-out invariant (R7) |
| Reversal / bounce | Route to `payment`'s existing bounce flow; detect from narration (R8) |
| Offline / where it runs | Backend import+match; mobile = synced accounts + upload + pull-only review (R9) |
| Settings | `StoreSetting` + `BankingSettingDefinitions` (R10) |
