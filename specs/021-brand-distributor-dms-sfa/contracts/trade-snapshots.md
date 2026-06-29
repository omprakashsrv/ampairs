# Contract — Consented snapshot reads (brand DMS view)

Read-only, **consent-gated** brand views over distributor-published aggregates. Tenant: the **brand**.
Every call passes `CrossTenantReadGuard`: an `ACCEPTED` `TradeLink` to the target distributor whose
`ConsentScope` permits the data category. Retailer dimension is projected per `retailerVisibility`
(CODED default → `outlet_code` only; IDENTIFIED → `display_name`/`area` added; never full contact PII).
Figures are at most ~5 minutes stale (clarification R13.2 / SC-011).

## Secondary sales

```
GET /trade/v1/snapshots/secondary-sales
    ?distributor_workspace_id={WSP...|all-linked}
    &grain=SKU_PERIOD|SKU_PERIOD_OUTLET|SKU_PERIOD_AREA
    &period_from=2026-04&period_to=2026-06
    &brand_product_uid={PRD...}?  &area_code={...}?     # the BRAND's product uid (not the distributor's)
    &page={int}&size={int}
→ ApiResponse<PageResponse<SecondarySalesRow>>
```
```json
// SecondarySalesRow — product dimension is the BRAND product (resolved via the confirmed NetworkProduct map)
{ "distributor_workspace_id": "WSP...", "brand_product_uid": "PRD...", "brand_sku_code": "BRX-12",
  "period_key": "2026-06", "outlet_code": "OUT-0153|null", "area_code": "BLR-S|null",
  "qty": "120.000", "value": "920710.50", "version": 7, "as_of": "2026-06-28T04:20:00Z" }
```
Only the distributor's products **confirmed-mapped** to a brand SKU appear; unmapped/other-brand products are
excluded (FR-018b). Cross-distributor aggregation sums by `brand_product_uid`, so the same brand SKU coded
differently by each distributor rolls up correctly.
`distributor_workspace_id=all-linked` aggregates across every ACCEPTED link of the calling brand; a
distributor with no active link is excluded (SC-004/SC-005). No active link → `ConsentRequiredException`.

## Distributor stock

```
GET /trade/v1/snapshots/distributor-stock
    ?distributor_workspace_id={WSP...|all-linked}&brand_product_uid={PRD...}?&area_code={...}?
    &page={int}&size={int}
→ ApiResponse<PageResponse<DistributorStockRow>>
```
```json
// DistributorStockRow — product dimension is the BRAND product (via the confirmed NetworkProduct map)
{ "distributor_workspace_id": "WSP...", "brand_product_uid": "PRD...", "brand_sku_code": "BRX-12",
  "warehouse_code": "WH-1|null", "area_code": "BLR-S|null", "quantity_on_hand": "340.000",
  "days_of_stock": 9.4, "out_of_stock": false, "version": 3, "as_of": "..." }
```
Unmapped/other-brand distributor stock is excluded (FR-018b).
`days_of_stock` / `out_of_stock` are derived from stock + the secondary-sales run rate (FR-021). Requires
`scope.share_stock`.

## Targets vs achievement

```
GET /trade/v1/targets
    ?tier=BRAND_TO_DISTRIBUTOR|DISTRIBUTOR|REP
    &subject_uid={...}?&period_from=...&period_to=...
→ ApiResponse<PageResponse<TargetAchievementRow>>
```
```json
// TargetAchievementRow
{ "uid": "STG...", "tier": "DISTRIBUTOR", "subject_uid": "WSP...",
  "period_start": "...", "period_end": "...", "grain": "product", "grain_key": "PRD...",
  "target_value": "1000000.00", "achieved_value": "742300.00", "achievement_pct": 74.23 }
```
`achieved_value` is derived from primary orders (brand tenant) or SecondarySalesSnapshot (distributor
tenant) for the same period × grain — brand and distributor see agreeing figures (SC-007). Brand reads of a
distributor's targets require `scope.share_targets`.

## Notes
- All snapshot reads are pull-only; the brand never POSTs aggregates and never reads distributor live tables.
- Snapshots are versioned; a backdated/cancelled distributor invoice bumps `version` within the ~5-min
  coalescing window (no double counting) — the brand always reads the latest version.
