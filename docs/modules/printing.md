# printing module

Server-side storage + offline-sync for the mobile app's **print templates**. The backend treats a template's layout as an **opaque blob**: it stores and syncs `template_json` verbatim and never parses or renders it — all rendering happens in the app. Templates are workspace-scoped (`OwnableBaseDomain` + `X-Workspace-ID`).

## REST Endpoints (`/printing/v1/templates`)

Canonical `/sync` contract — see `docs/guides/offline-sync-contract.md`.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/printing/v1/templates/sync` | Incremental pull feed (snake_case params: `last_sync`, `page`, `size`, `sort_by`, `sort_dir`). **Includes inactive (soft-deleted) rows** so clients detect deletions. |
| POST | `/printing/v1/templates/sync` | Bulk upsert keyed by uid; soft-deleted rows (`active = false`) ride along in-band — no per-row DELETE. |

## Key Entities

### PrintTemplate

One template per `(document_type, printer_class)` pair — e.g. THERMAL invoice vs PAGE invoice. The `template_json` column holds the opaque layout consumed by the app's `printing/{core,render,transport}` engine.

## Dependencies

- `:core` only. Spring discovers it via the default `com.ampairs` component scan.
- Previously a sub-package of `workspace`; extracted to its own module.

See `printing/CLAUDE.md` for the full entity/column reference.
