# tax module

GST tax config, HSN/SAC code catalog, tax rule engine. Two-layer model: global master + workspace subscriptions.

## Two layers
- **Master** (global, seeded): `MasterTaxCode` (HSN/SAC), `MasterTaxComponent` (CGST/SGST/IGST/CESS), `MasterTaxRule`
- **Workspace** (per-tenant): `TaxConfiguration`, `TaxCode` (subscribed codes), `TaxRule` (overrides), `TaxComponent`

## Key fields
- `TaxCode.code` — HSN or SAC code
- `TaxRule.componentComposition` — JSON array: `[{component: "CGST", rate: 9.0}, {component: "SGST", rate: 9.0}]`
- `TaxConfiguration.taxStrategy` — `INDIA_GST`, `USA_SALES_TAX`, etc.

## Base path
`/tax/v1/**`

## Migrations
`V1.0.9` (v1 tables), `V1.0.38` (v2 tables), `V1.0.39` (drop v1)

## Full docs
`docs/modules/tax.md`
