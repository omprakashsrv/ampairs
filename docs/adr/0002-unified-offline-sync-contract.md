# 0002 — One canonical `/sync` contract for all syncable entities

- Status: Accepted
- Date: 2026-06-10 (documenting a pre-existing decision; see `docs/guides/offline-sync-contract.md`)

## Context

The mobile client (Compose Multiplatform) is offline-first: it writes to a local Room DB and
reconciles with the backend in the background, across multiple devices per workspace. Early on, each
entity grew its own bespoke list/bulk endpoints with subtly different semantics (pagination, delete
handling, timestamps), which made the client sync code a pile of per-entity special cases and made
deletions fail to propagate between devices.

## Decision

Every standard syncable resource exposes exactly one pair of endpoints:

```
GET  /{module}/v1/{resource}/sync ?last_sync&page&size&sort_by&sort_dir  → ApiResponse<PageResponse<T>>
POST /{module}/v1/{resource}/sync   body: List<T>                        → ApiResponse<List<T>>
```

Rules that make it work uniformly:

- **snake_case** query params; `last_sync` sent only when non-blank (incremental pull).
- The pull feed **includes soft-deleted rows** so deletes propagate to every device.
- **In-band delete**: the push body carries soft-deleted rows (`active=false`/`status=DELETED`);
  there is no per-row DELETE. On pull, rows the server reports DELETED are hard-deleted locally.
- **UID-keyed upsert**: the client generates UIDs, so push is idempotent by UID.
- **Conflict resolution**: local unsynced edits win over the server during a pull.

The client mirrors this with one `SyncDelegate` per entity behind a `CentralSyncService`
coordinator. Off-contract by design: `tax` (online subscribe model) and `file` (multipart upload).

## Consequences

- **Positive:** one mental model and near-identical delegate/controller code per entity; deletes
  round-trip correctly; incremental pulls are cheap; idempotent retries are safe.
- **Negative / debt:** the per-entity delegates are still copy-paste (a `BaseSyncDelegate<T>`
  abstraction is a known follow-up). Two resources have residual gaps — `form` has no soft-delete
  column (deletes don't round-trip) and order/invoice still have a legacy non-`/sync` list pull to
  converge.
- **Failure semantics matter:** a push that fully fails must report failure, not `Success(0)`,
  or the UI shows false green. This recurred and is encoded as a rule in the client's offline-sync
  skill.
