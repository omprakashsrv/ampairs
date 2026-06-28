# gstr-export — Portal-compatible JSON / Excel export (Phase 1)

Stream a **GSTN-portal-compatible artifact** for a prepared GSTR-1 or GSTR-3B: the offline-utility
**JSON** the GST portal ingests, or an **Excel** workbook of the same (R6/FR-021). This is the
export-first value proposition — the accountant uploads the file to the portal manually, no GSP needed.

**Controller:** `GstrController`. **Services:** `Gstr1PortalBuilder`, `Gstr3bPortalBuilder`,
`GstrExcelExporter`. **RBAC:** available to any workspace member (export is broader than the
owner/admin-only file action).

---

## 1. Export a prepared return

```
GET /gstr/v1/returns/{gstin}/{type}/{period}/export?format=json|xlsx
```

Path params: `{gstin}` (15-char), `{type}` (`gstr1`|`gstr3b`|`cmp08`), `{period}`
(`MMYYYY`|`Q{n}YYYY`).

### Query params

| Param | Type | Default | Notes |
|---|---|---|---|
| `format` | string | `json` | `json` (GSTN offline-utility schema) \| `xlsx` (spreadsheet form) |

### Response

This endpoint **streams the artifact** (it does **not** wrap the artifact body in `ApiResponse` — the
portal/offline-utility expects the bare GSTN JSON or a binary `.xlsx`). Success returns the file with:

| `format` | `Content-Type` | `Content-Disposition` |
|---|---|---|
| `json` | `application/json` | `attachment; filename="GSTR1_27ABCDE1234F1Z5_062026.json"` |
| `xlsx` | `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` | `attachment; filename="GSTR1_27ABCDE1234F1Z5_062026.xlsx"` |

Only **errors** use the `ApiResponse` envelope (bubbled by the exception handler):

| Case | Result |
|---|---|
| Return not prepared | 404 — `error.code = RETURN_NOT_PREPARED` |
| Blocking readiness errors outstanding | 409 — `error.code = NOT_READY_FOR_EXPORT` (advise: fix + re-prepare) |
| `gstin` not registered | 404 — `GSTIN_NOT_FOUND` |
| Unknown `format` | 400 — `BAD_REQUEST` |

---

## GSTN JSON shape (deliberate external-contract exception)

The exported JSON uses **GSTN's own field names** (NOT the global snake_case) so the GST Offline Utility
/ portal accepts it on first upload (SC-005). It is produced **only** by the isolated `Gstr1PortalBuilder`
/ `Gstr3bPortalBuilder` DTOs, each carrying explicit `@JsonProperty` — these field names never appear on
the internal API (which stays snake_case, see `gstr-prepare.md`). This is the same deliberate external
exception spec 015 documented for the e-invoice INV-01 payload (plan.md · Principle III).

GSTN field-name glossary: `gstin`, `fp` (filing period `MMYYYY`), `b2b`/`b2cl`/`b2cs`/`cdnr`/`cdnur`/
`exp`/`nil`/`hsn`/`doc_issue`, `inv` (invoices), `itms` (items), `txval` (taxable value),
`iamt`/`camt`/`samt`/`csamt` (IGST/CGST/SGST/CESS amounts), `pos` (place of supply, 2-digit state),
`rt` (rate %), `hsn_sc` (HSN/SAC), `uqc` (unit qty code), `ct` (customer GSTIN), `idt` (invoice date),
`val` (invoice value), `inum` (invoice number), `sply_ty` (`INTER`/`INTRA`). **All amounts are
rupee-rounded** (HALF_UP at the section-total boundary — R12); totals foot to the rounded header.

### Trimmed GSTR-1 portal JSON example

```jsonc
{
  "gstin": "27ABCDE1234F1Z5",
  "fp": "062026",
  "gt": 1842500,            // gross turnover (rupee-rounded)
  "cur_gt": 1842500,

  "b2b": [                  // invoice-wise, registered buyers
    {
      "ctin": "29PQRS5678K1Z2",
      "inv": [
        {
          "inum": "INV/2026/0102",
          "idt": "18-06-2026",
          "val": 118000,    // invoice value, rupee-rounded
          "pos": "29",      // inter-state ⇒ IGST
          "rchrg": "N",
          "inv_typ": "R",
          "itms": [
            { "num": 1, "itm_det": { "rt": 18, "txval": 100000, "iamt": 18000, "camt": 0, "samt": 0, "csamt": 0 } }
          ]
        }
      ]
    }
  ],

  "b2cl": [                 // unregistered, inter-state, value > threshold (₹1,00,000)
    {
      "pos": "29",
      "inv": [
        { "inum": "INV/2026/0140", "idt": "22-06-2026", "val": 150000,
          "itms": [ { "num": 1, "itm_det": { "rt": 18, "txval": 127119, "iamt": 22881, "camt": 0, "samt": 0, "csamt": 0 } } ] }
      ]
    }
  ],

  "b2cs": [                 // other B2C, summarized by (pos, rate) — NOT invoice-wise
    { "sply_ty": "INTRA", "pos": "27", "typ": "OE", "rt": 18, "txval": 45000, "iamt": 0, "camt": 4050, "samt": 4050, "csamt": 0 },
    { "sply_ty": "INTER", "pos": "24", "typ": "OE", "rt": 12, "txval": 8000,  "iamt": 960,  "camt": 0, "samt": 0, "csamt": 0 }
  ],

  "cdnr": [                 // credit/debit notes, registered
    {
      "ctin": "29PQRS5678K1Z2",
      "nt": [
        { "ntty": "C", "nt_num": "CRN/2026/0007", "nt_dt": "25-06-2026", "val": 11800, "pos": "29", "rchrg": "N",
          "itms": [ { "num": 1, "itm_det": { "rt": 18, "txval": 10000, "iamt": 1800, "camt": 0, "samt": 0, "csamt": 0 } } ] }
      ]
    }
  ],

  "exp": [                  // exports / zero-rated
    { "exp_typ": "WPAY", "inv": [
        { "inum": "EXP/2026/0003", "idt": "20-06-2026", "val": 250000,
          "itms": [ { "txval": 250000, "rt": 0, "iamt": 0, "csamt": 0 } ] } ] }
  ],

  "hsn": {                  // HSN summary, rolled up by (hsn, uqc, rate)
    "data": [
      { "num": 1, "hsn_sc": "8471", "desc": "Computers", "uqc": "NOS", "qty": 12, "rt": 18,
        "txval": 227119, "iamt": 40881, "camt": 4050, "samt": 4050, "csamt": 0 }
    ]
  },

  "doc_issue": {            // document-series summary (issued / cancelled / net)
    "doc_det": [
      { "doc_num": 1, "docs": [
          { "num": 1, "from": "INV/2026/0001", "to": "INV/2026/0412", "totnum": 412, "cancel": 3, "net_issue": 409 } ] }
    ]
  }
}
```

> The GSTR-3B export (`Gstr3bPortalBuilder`) follows the GSTN 3B schema — `sup_details` (3.1 outward),
> `inter_sup` (3.2), `itc_elg` (table 4, marked pending in P1), `intr_ltfee` — same GSTN field names and
> rupee rounding. CMP-08 exports the GSTN CMP-08 schema.

---

## Endpoint count: 1
(single `export` endpoint, `format=json|xlsx`, covering GSTR-1 / GSTR-3B / CMP-08.)
