# API Contracts — GST E-Invoicing (IRN) & E-Way Bill

Backend module: `einvoice`. Base path: `/einvoice/v1`. All endpoints return the standard
`ApiResponse<T>` envelope (constitution V); paginated payloads use `PageResponse<T>`. Every request is
workspace-scoped and requires the `X-Workspace-ID` header (constitution IV); JSON is global
`SNAKE_CASE`.

This feature deliberately sits **off the canonical push `/sync` contract** for its two synced
entities: e-invoice documents and e-way bills are **server-authored**, so only the **pull** half
(`GET .../sync`) exists — there is no client `POST .../sync` upsert. This mirrors how `tax` (subscribe)
and `file` (multipart) sit off the standard contract, and is recorded in `plan.md` Complexity Tracking.

| File | Covers |
|---|---|
| [einvoice-sync.md](./einvoice-sync.md) | Pull-only `/sync` feeds the mobile app mirrors (documents, eway-bills) |
| [einvoice-actions.md](./einvoice-actions.md) | Online commands: generate/cancel IRN, generate/cancel/update/extend EWB, failed-document list |

## Authorization summary

| Operation | Who |
|---|---|
| Automatic IRN registration on `InvoiceFinalizedEvent` | system (no caller) |
| `GET` sync feeds, `GET` failed list, `GET` detail | any workspace member |
| `POST` generate / retry, all cancel / update / extend commands | **workspace admin/owner only** (clarification 2026-06-28) |

## Status codes

`200` OK · `201` created (generate) · `400` validation · `401` unauthenticated · `403` not
admin/owner or e-invoicing disabled for workspace · `404` invoice/doc not found · `409` window closed
(cancel after 24h) or already-generated conflict · `422` IRP/GSP rejection surfaced as a typed error ·
`500` unexpected. Business exceptions bubble to the global handler (constitution VI).
