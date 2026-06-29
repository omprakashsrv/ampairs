# claim module

Trade-scheme **claims & settlement** — the brand→distributor reimbursement lifecycle that `pricing`/spec-015 explicitly deferred (feature 021, US6). Scheme *definition* lives in `pricing` (referenced by `schemeRef`); this owns only the claim→settlement lifecycle. Depends on `core` (cross-module accrual/ledger deferred).

## REST Endpoints (`/claim/v1`)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/claim/v1/claims` | Accrue a claim (DRAFT) |
| POST | `/claim/v1/claims/{uid}/submit` | Distributor: DRAFT → SUBMITTED |
| POST | `/claim/v1/claims/{uid}/approve` | Brand: SUBMITTED → APPROVED |
| POST | `/claim/v1/claims/{uid}/reject` | Brand: SUBMITTED → REJECTED (records reason) |
| POST | `/claim/v1/claims/{uid}/settle` | Brand: APPROVED → SETTLED (records reference) |

## Key entities
`SchemeClaim` (`schemeRef` = pricing offer uid, `computedAmount`, `status`, `rejectionReason`), `ClaimSettlement` (reconcilable reference, settled amount).

## Key patterns
- `ClaimService` lifecycle: DRAFT → SUBMITTED → APPROVED | REJECTED → SETTLED, with `ClaimStateException` (409) on any illegal transition. Reject records a reason and never settles; settle records a `ClaimSettlement`. Negative amount rejected; zero allowed.
- `computed_amount` is identical for brand and distributor (computed from the same shared data).

## Migrations
`V1.0.120` (scheme_claims, claim_settlements). PostgreSQL + MySQL.

## Deferred follow-ups
Accrual FROM qualifying `fundingBrandId`-tagged `SecondarySalesSnapshot` rows (dms); optional `payment`-ledger post on settle.
