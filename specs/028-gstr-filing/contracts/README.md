# GSTR Filing — API Contracts (Phase 1 / Phase 2)

Phase 1 design artifacts for the `gstr` bounded context. These are Markdown contract docs (not
OpenAPI), the source of truth for the controllers in `gstr/src/main/kotlin/com/ampairs/gstr/controller/`.

Base path for the module: **`/gstr/v1/**`**.

## Contract files

| File | Scope | Phase | Endpoints |
|---|---|---|---|
| [`gstr-sync.md`](./gstr-sync.md) | Pull-only `/sync` feed (mobile status mirror) + `GstinRegistration` CRUD | P1 | 7 |
| [`gstr-prepare.md`](./gstr-prepare.md) | Prepare / readiness gate / GSTR-1 / GSTR-3B / CMP-08 retrieval | P1 | 6 |
| [`gstr-export.md`](./gstr-export.md) | Portal-compatible JSON / Excel export (GSTN offline-utility shape) | P1 | 1 |
| [`gstr-filing.md`](./gstr-filing.md) | Electronic filing (EVC/OTP), ARN status, 2A/2B pull, reconciliation, purchase import | **P2** | 6 |

**Total endpoints across the four contracts: 20.**

## Common response envelope

Every endpoint returns the standard `ApiResponse<T>` from `com.ampairs.core.domain.dto.ApiResponse`
(rule 04-api-response). Paginated endpoints wrap `PageResponse<T>`:

```jsonc
{
  "success": true,
  "data":    { /* T, or PageResponse<T>, or null on error */ },
  "error":   null,            // populated by GlobalExceptionHandler on failure
  "timestamp": "2026-06-28T10:15:30Z",
  "path":    "/gstr/v1/returns/...",
  "traceId": "b1c2d3e4-..."
}
```

`PageResponse<T>` shape (snake_case): `{ content: [T], page, size, total_elements, total_pages, has_next }`.

Business exceptions (`GstrException`, `PeriodLockedException`, `ReturnNotReadyException`,
`GstnFilingException`) bubble to the module exception handler — **no try/catch in controllers**
(rule 05). Errors are surfaced in the `error` field, never as a `success:false` body with a 200.

## Headers (all endpoints)

| Header | Required | Purpose |
|---|---|---|
| `X-Workspace-ID` | yes | Multi-tenant context; `SessionUserFilter` sets `@TenantId ownerId`. Missing → 400. |
| `Authorization: Bearer <jwt>` | yes | Auth/session; carries the member's role for RBAC. |
| `Content-Type: application/json` | on POST | Except `purchase-register/import` (multipart) and `export` (download). |

All `gstr` entities extend `OwnableBaseDomain`; the `gstin` path/column adds a second isolation axis
**under** the tenant, so one workspace/registration never sees another's returns (FR-030).

## JSON conventions

- **Internal request/response DTOs** use the global Jackson `SNAKE_CASE` strategy — do **not** add
  `@JsonProperty` for standard camelCase fields (rule 03). Query params are snake_case
  (`last_sync`, `page`, `size`, `sort_by`, `sort_dir`).
- **External GSTN exception (deliberate):** the GSTN portal return JSON and the 2A/2B payloads use
  GSTN's *own* field names (`b2b`, `inv`, `itms`, `txval`, `iamt`/`camt`/`samt`/`csamt`, `pos`,
  `rt`, `hsn_sc`, …). These are produced/consumed only by isolated `*PortalBuilder` DTOs carrying
  explicit `@JsonProperty`, and never leak into the internal API surface. This is an external-contract
  exception, documented inline in each builder, mirroring spec 015 INV-01. See `gstr-export.md`.

## Auth / RBAC

| Action | Allowed roles | Source |
|---|---|---|
| Prepare / readiness / retrieve / export | any workspace member (workspace RBAC) | broader by design |
| Read sync feed / status | any workspace member | mobile status surface (R11) |
| `GstinRegistration` CRUD | **OWNER / ADMIN** | registration is a legal identity |
| **Electronic FILE** (`/file`, `/file/confirm`) | **OWNER / ADMIN only** | spec clarification, FR-031 |
| **Electronic-filing credential setup** | **OWNER / ADMIN only** | FR-027, FR-031, Principle XI |
| `2b/pull`, reconciliation, purchase import | any workspace member | analytical, non-legal |

A non-owner/admin member who attempts a file or credential action is denied (HTTP 403 →
`error.code = FORBIDDEN`) even though they may prepare, review and export the same return (US5 #5).

## Phase tags

- **Phase 1 (export-first):** `gstr-sync.md`, `gstr-prepare.md`, `gstr-export.md`. Real compliance
  value with no GSP onboarding — prepare GSTR-1/3B, gate on readiness, export a portal-ready file.
- **Phase 2 (GSP API filing + ITC):** `gstr-filing.md`. Electronic filing via the `GstnFilingProvider`
  port, EVC/OTP auth, ARN tracking, 2A/2B pull, books⟷2B reconciliation, purchase-register import.

## Period & path conventions used throughout

- `{gstin}` — 15-char GSTIN of a registered `GstinRegistration` under the workspace.
- `{type}` — return type: `gstr1` | `gstr3b` | `cmp08` (P3: `gstr9` | `gstr9c`).
- `{period}` — `MMYYYY` for monthly filers (e.g. `062026`), `Q{n}YYYY` for QRMP quarterly filers
  (e.g. `Q12026`), `YYYY` for annual. A quarterly filer files **one GSTR-1 per quarter** — there is
  **no monthly IFF endpoint** (out of scope, spec clarification / FR-020).
- A `FILED` period is **source-locked and immutable**: re-prepare is refused, and a late back-dated
  invoice dated in that period is **not blocked** — it finalizes and is attributed to the **next open**
  period's GSTR-1 (R8 / FR-025). Period attribution is handled on the invoice finalize path, not by a
  `gstr` endpoint.
