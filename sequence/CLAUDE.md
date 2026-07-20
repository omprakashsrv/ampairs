# sequence module

Central document-number sequences: per-workspace definitions (prefix/pattern/counter per document type) and allocations (numbers or blocks reserved by clients — offline-safe numbering for orders, invoices, purchases).

## Key entities
- `SequenceDefinition` — document type, prefix, pattern, padding, next counter, reset policy
- `SequenceAllocation` — number/block allocated to a device/client + usage status

## Base paths
- `/sequence/v1/definitions/**` — CRUD, `GET /{uid}/preview`, `GET`/`POST /sync` (canonical contract)
- `/sequence/v1/allocations/**` — `POST /next` (allocate), `POST /report` (reconcile client usage), `GET` list

## Rules
- Allocation must be race-safe — a number is handed out at most once per definition
- Follow the canonical `/sync` contract for definitions (`docs/guides/offline-sync-contract.md`)
- Tenant context set by `SessionUserFilter` from `X-Workspace-ID` — never in services
- Depends on `:core` only

## Migrations
`sequence/src/main/resources/db/migration/{postgresql,mysql}/` — write BOTH vendors

## Full docs
`docs/modules/sequence.md`
