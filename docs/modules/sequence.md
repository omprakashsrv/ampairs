# sequence module

Central document-number sequences: per-workspace **definitions** (prefix/pattern/counter per document type) and **allocations** (numbers handed out to clients). Solves offline-safe, gap-aware numbering for orders, invoices, purchases, etc.

## REST Endpoints

### Definitions (`/sequence/v1/definitions`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/sequence/v1/definitions` | List definitions |
| POST | `/sequence/v1/definitions` | Create definition |
| GET | `/sequence/v1/definitions/{uid}` | Get definition |
| PUT | `/sequence/v1/definitions/{uid}` | Update definition |
| GET | `/sequence/v1/definitions/{uid}/preview` | Preview the next formatted number |
| GET | `/sequence/v1/definitions/sync` | Incremental pull feed (canonical `/sync` contract) |
| POST | `/sequence/v1/definitions/sync` | UID-keyed bulk upsert |

### Allocations (`/sequence/v1/allocations`)

| Method | Path | Description |
|--------|------|-------------|
| POST | `/sequence/v1/allocations/next` | Allocate the next number for a document type |
| POST | `/sequence/v1/allocations/report` | Report client-side usage of allocated numbers |
| GET | `/sequence/v1/allocations` | List allocations |

## Key Entities

### SequenceDefinition

Per-workspace, per-document-type numbering rule: prefix, pattern, padding, next counter, reset policy.

### SequenceAllocation

A number (or block) allocated to a device/client, with usage status — lets offline clients reserve numbers ahead of time and reconcile later.

## Dependencies

- `:core` only (standard module rules apply — no cross-module imports).
