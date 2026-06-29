# Phase 1 Data Model — Brand → Distributor DMS + Sales Force Automation

Backend module `trade` (`com.ampairs.trade`). All tenant-scoped entities extend `OwnableBaseDomain`
(inherits `id: Long`, `uid: String`, `createdAt/updatedAt: Instant`, `ownerId: String` `@TenantId`,
`refId: String?`). Money is `BigDecimal` → `DECIMAL(19,4)`; timestamps `Instant` → `TIMESTAMPTZ`/`TIMESTAMP`;
geo is `Double` lat/lng. Mobile (`feature/trade`) mirrors the SFA-authored subset in Room with `Long` minor
units and `synced`/`active` flags per the offline contract.

Legend: **PK** uid (prefixed nanoid). **Tenant** = which workspace owns the row. `→` = reference by uid.

---

## 1. Network & consent (foundation — owned by the BRAND tenant, except where noted)

### TradeNetwork  *(tenant: brand)*
A brand's network container.
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `TNW` |
| name | String | brand-chosen network name; `@field:NotBlank` |
| brandWorkspaceId | String | = ownerId (the brand workspace) |
| active | Boolean | soft-delete |

### TradeLink  *(tenant: brand owns the row; distributor consents)*  — **the sole cross-tenant trust edge**
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `TLN` |
| network → | String | TradeNetwork.uid |
| brandWorkspaceId | String | inviting brand |
| distributorWorkspaceId | String | invited distributor |
| status | LinkStatus | see state machine |
| consentScope | ConsentScope | embeddable (below); set by brand on invite, confirmable by distributor on accept |
| invitedAt / respondedAt / revokedAt | Instant? | lifecycle stamps |
Uniqueness: at most one **non-revoked** link per `(brandWorkspaceId, distributorWorkspaceId)`.

### ConsentScope  *(embeddable on TradeLink)* — clarification R13.1
| Field | Type | Default | Notes |
|---|---|---|---|
| shareSecondarySales | Boolean | true | brand may read secondary-sales snapshots |
| shareStock | Boolean | true | brand may read distributor-stock snapshots |
| retailerVisibility | RetailerVisibility | **CODED** | CODED = coded/aggregated outlets only; IDENTIFIED = name/area exposed. **Full contact PII never shared in either case.** |
| shareTargets | Boolean | true | brand may read distributor/rep targets |

### NetworkRetailer  *(tenant: distributor)* — projection of a distributor customer into the network
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `NRT` |
| link → | String | TradeLink.uid |
| customerUid → | String | distributor's `customer` (the outlet) — never leaves the distributor tenant raw |
| outletCode | String | stable, brand-visible code (always) |
| displayName / area | String? | populated/projected to the brand **only** when scope.retailerVisibility = IDENTIFIED |
| active | Boolean | |

### NetworkBrand  *(tenant: distributor)* — brand attribution / Hop A (sub-spec product-brand-attribution)
The **primary, required** product-linking edge: designates one of the distributor's existing in-catalog brand
labels (`product_brand`) as corresponding to the linked brand workspace. Decides *whose* product a distributor
product is — all products whose `brandId` is a designated label attribute to that brand (unmapped-by-Hop-B
included, bucketed "unmapped").
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `NBR` |
| link → | String | TradeLink.uid |
| distributorProductBrandUid → | String | the distributor's existing `ProductBrand` (`product_brand`) label |
| brandWorkspaceId | String | the linked brand's workspace (from the link) |
| status | DesignationStatus | ACTIVE / REMOVED |
Multiple labels MAY be designated for one brand (aliases); a label MAY be designated for **at most one** brand
per active link. Brand sees this **read-only**; the distributor controls it.

### NetworkProduct  *(tenant: distributor)* — SKU identity / Hop B (optional refinement; clarification R13.6)
Optional finer mapping of a distributor product to the brand's **specific** SKU; refines attributed figures to
SKU grain. Only distributor products under a `NetworkBrand`-designated label are candidates; absence never
drops a sale (it falls into the aggregated "unmapped" bucket).
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `NPR` |
| link → | String | TradeLink.uid |
| distributorProductUid → | String | distributor's `product` (the SKU the distributor carries; never leaves the distributor tenant raw) |
| brandProductUid → | String | brand's `product` uid (from the catalog the brand published down the link) |
| brandSkuCode | String | brand's stable SKU code — the brand-facing product dimension |
| matchSource | MatchSource | AUTO_BARCODE / AUTO_SKU / MANUAL (barcode = GTIN/EAN; **no HSN** — HSN is a tax attribute, not on `Product`) |
| status | MappingStatus | SUGGESTED (auto-proposed) / CONFIRMED (distributor accepted) — only CONFIRMED itemizes by brand SKU |
| active | Boolean | |
Uniqueness: at most one CONFIRMED mapping per `(link, distributorProductUid)`. The brand's published catalog
is read by the distributor over the link (consented brand→distributor direction) to populate `brandProductUid`/
`brandSkuCode`.

---

## 2. SFA field entities (tenant: DISTRIBUTOR; SFA-authored offline, ride `/sync`)

### Beat  *(distributor)*
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `BET` |
| name | String | route name; `@field:NotBlank` |
| active | Boolean | |
`@NamedEntityGraph("Beat.outlets")` → beatOutlets.

### BeatOutlet  *(distributor)* — membership of a customer in a beat
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `BTO` |
| beat → | String | Beat.uid |
| customerUid → | String | distributor customer (the outlet) |
| sequence | Int | visit order within the beat |
| visitDays | Set<DayOfWeek> | scheduled days (stored as CSV / smallint mask) |
| active | Boolean | |

### JourneyPlan (PJP)  *(distributor)* — a rep's recurring weekly beat calendar
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `PJP` |
| repMemberUid → | String | distributor `workspace` member with FIELD_REP role |
| beat → | String | Beat.uid |
| weekday | DayOfWeek | which day this rep works this beat |
| effectiveFrom / effectiveTo | Instant? | validity window |
| active | Boolean | |

### PlannedVisit  *(distributor)* — expected stop for a day (derivable from PJP; materialised for adherence)
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `PVS` |
| journeyPlan → | String | JourneyPlan.uid |
| beatOutlet → | String | BeatOutlet.uid |
| plannedDate | LocalDate (Instant day) | the expected visit date |
| status | PlannedVisitStatus | PENDING / VISITED / MISSED |

### Visit  *(distributor; offline-authored)* — actual stop — clarifications R13.3, R13.4
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `VST`; client-generated (offline idempotency) |
| repMemberUid → | String | the rep |
| customerUid → | String | the outlet visited |
| plannedVisit → | String? | null when `adHoc = true` |
| adHoc | Boolean | true = unplanned stop (R13.4) |
| outcome | VisitOutcome | PRODUCTIVE / UNPRODUCTIVE / NO_ORDER |
| checkInAt / checkOutAt | Instant | captured on-device at author time |
| lat / lng | Double? | null when location unavailable |
| geoFenceStatus | GeoFenceStatus | IN_RADIUS / OUT_OF_RADIUS / NO_LOCATION (R13.3) — **never blocks** |
| distanceMeters | Double? | distance to the outlet's known location, when computable |
| fieldOrder → | String? | order taken at the counter |
| notes | String? | |
| synced / active | Boolean | offline-sync flags (mobile + server) |

### Attendance  *(distributor; offline-authored)*
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `ATT`; client-generated |
| repMemberUid → | String | the rep |
| type | AttendanceType | CHECK_IN / CHECK_OUT |
| at | Instant | on-device time |
| lat / lng | Double? | on-device location |
| synced / active | Boolean | |

### FieldOrder  *(distributor; offline-authored)* — counter order reference
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `FOR`; client-generated |
| repMemberUid → | String | the rep |
| customerUid → | String | the outlet |
| visit → | String? | the visit it was taken on |
| orderUid → | String | the resulting distributor `order` (created via `OrderService`) |
| totalMinor | Long (mobile) / BigDecimal (server) | order value |
| synced / active | Boolean | |
The FieldOrder is a thin trade-side reference; the real order lives in the `order` module (so distributor
pricing/validation/sync apply). Tagged `SECONDARY` for snapshot rollup.

---

## 3. Brand DMS aggregates & targets (Phase 2)

### SalesTarget  *(tenant: setter — brand for primary, distributor for secondary/rep)*
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `STG` |
| tier | TradeTier | BRAND_TO_DISTRIBUTOR (primary) / DISTRIBUTOR (secondary) / REP |
| subjectUid | String | distributorWorkspaceId, repMemberUid, or beatUid per tier |
| periodStart / periodEnd | Instant | target window |
| grain | TargetGrain | product / category / area |
| grainKey | String? | sku/category/area code |
| targetValue | BigDecimal | amount or qty |
| targetQty | BigDecimal? | optional separate qty target |
Achievement is **derived** (not stored) from primary orders (brand tenant) or `SecondarySalesSnapshot`
(distributor tenant) over the same period × grain.

### SecondarySalesSnapshot  *(tenant: distributor; published; versioned, recomputable)* — R11, R13.2
Deterministic aggregate, keyed by `(distributorWorkspaceId, grain, periodKey, version)`; **recomputed**, never
incrementally mutated.
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `SSS` |
| distributorWorkspaceId | String | source tenant |
| grain | SnapshotGrain | SKU×PERIOD, SKU×PERIOD×OUTLET, SKU×PERIOD×AREA |
| periodKey | String | e.g. `2026-06` (month) or `2026-W26` |
| skuUid | String | the **distributor's** source product uid (from the distributor's order/invoice docs) |
| attributedBrandWorkspaceId | String? | the brand this row is attributed to **as of sale time** (point-in-time) via Hop A (`NetworkBrand`); null = not attributed to any brand (other-brand/untagged) → excluded from brand reads |
| brandProductUid / brandSkuCode | String? | set only where a CONFIRMED Hop B (`NetworkProduct`) mapping exists; attributed rows with null here are **counted** in the brand's aggregated "unmapped" bucket, never dropped (FR-018b) |
| outletCode / areaCode | String? | per grain; outlet only when scope IDENTIFIED-eligible (still coded) |
| qty | BigDecimal | summed secondary qty |
| value | BigDecimal | summed secondary value |
| version | Long | bumped on each recompute |
| asOf | Instant | recompute time |
Read by the brand only through `CrossTenantReadGuard` (active link + scope.shareSecondarySales).

### DistributorStockSnapshot  *(tenant: distributor; published; versioned)* — R9, R13.2
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `DSS` |
| distributorWorkspaceId | String | source tenant |
| skuUid | String | the **distributor's** source product uid (from the distributor's inventory) |
| attributedBrandWorkspaceId | String? | brand attributed via Hop A (`NetworkBrand`); null = other-brand/untagged → excluded |
| brandProductUid / brandSkuCode | String? | set only where a CONFIRMED Hop B mapping exists; attributed stock with null here is counted in the aggregated "unmapped" bucket, never dropped (FR-018b) |
| warehouseCode / areaCode | String? | grain |
| quantityOnHand | BigDecimal | from distributor `inventory` |
| version | Long | bump on recompute |
| asOf | Instant | |

### PrimaryOrderLink  *(tenant: brand owns; distributor confirms)* — R13.5 handshake
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `POL` |
| link → | String | TradeLink.uid (must be ACTIVE) |
| brandOrderUid → | String | the order created in the BRAND tenant |
| status | PrimaryOrderStatus | PLACED / CONFIRMED / REJECTED |
| distributorOrderUid → | String? | set on confirm — the order created in the DISTRIBUTOR tenant |
| respondedAt | Instant? | |

---

## 4. Schemes & claims (Phase 3)

### TradeScheme  *(tenant: brand)*
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `TSC` |
| name | String | `@field:NotBlank` |
| type | SchemeType | SLAB / VALUE / QTY / FREE_GOODS / DISPLAY |
| eligibility | (embedded) | SKU/category set × geography × period |
| fundingSource | String | |
| periodStart / periodEnd | Instant | |
| published | Boolean | published down in-scope links |

### SchemeClaim  *(tenant: distributor)*
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `SCL` |
| scheme → | String | TradeScheme.uid |
| link → | String | TradeLink.uid |
| computedAmount | BigDecimal | from qualifying SecondarySalesSnapshot — identical figure both sides |
| status | ClaimStatus | DRAFT / SUBMITTED / APPROVED / REJECTED / SETTLED |

### ClaimSettlement  *(tenant: brand)*
| Field | Type | Notes |
|---|---|---|
| uid | String | PK, prefix `CST` |
| claim → | String | SchemeClaim.uid |
| settledAmount | BigDecimal | |
| reference | String | reconcilable reference (optional spec-013 ledger ref) |
| rejectionReason | String? | when the claim is rejected |
| settledAt | Instant? | |

---

## 5. Roles

`WorkspaceRole` (in `workspace` module) gains **FIELD_REP** at `level = 30` (between GUEST=20 and MEMBER=40):
`FIELD_REP("Field Representative", 30, "Field sales rep limited to assigned beats")`. A brand user's access
to distributor data is NOT via membership — it is mediated solely by the `TradeLink` (R10).

---

## 6. State machines

**LinkStatus**: `INVITED → ACCEPTED → REVOKED`; `INVITED → DECLINED`. Data flows only while `ACCEPTED`.
Revoke is terminal for that link (a new invite makes a new link).

**PrimaryOrderStatus**: `PLACED → CONFIRMED` (creates distributor order) | `PLACED → REJECTED`.

**ClaimStatus**: `DRAFT → SUBMITTED → APPROVED → SETTLED` | `SUBMITTED → REJECTED`. Distributor owns
DRAFT→SUBMITTED; brand owns APPROVED/REJECTED/SETTLED.

**PlannedVisitStatus**: `PENDING → VISITED` (a Visit references it) | `PENDING → MISSED` (day passed, no Visit).

**Visit.geoFenceStatus** is a captured attribute, not a lifecycle: `IN_RADIUS | OUT_OF_RADIUS | NO_LOCATION`
— informational only; never gates creation.

---

## 7. Validation & integrity rules

- A Visit, FieldOrder, Attendance MUST carry a client-generated uid (offline idempotency, UID-keyed upsert).
- A Visit with `adHoc = false` MUST reference a `plannedVisit`; with `adHoc = true` it MUST NOT.
- Check-in is never rejected for missing/with out-of-radius location (R13.3) — `geoFenceStatus` records it.
- A brand read of any snapshot MUST pass `CrossTenantReadGuard`: an `ACCEPTED` `TradeLink` whose
  `consentScope` permits that data category; retailer dimension projected per `retailerVisibility`.
- `PrimaryOrderLink` creation/confirm requires an `ACCEPTED` link; confirm is the only path that writes the
  distributor-tenant order (via `OrderService.bulkUpsertOrders`/create).
- Snapshots are recomputed wholesale per `(distributor, grain, periodKey)`; `version` bumps; a backdated/
  cancelled invoice triggers a rebuild that supersedes the prior version (no double count).
- Money: `BigDecimal`/`DECIMAL(19,4)` server, `Long` minor units mobile; `computedAmount` and `settledAmount`
  for the same claim+sales MUST match across tenants.
- Brand-facing product figures (secondary sales, stock, targets, scheme eligibility) follow the **two-level
  model**: **Hop A** attributes each distributor product to a brand iff its `brandId` is under a label
  designated by an ACTIVE `NetworkBrand` for that link (captured **as of sale time** — point-in-time);
  other-brand/untagged products are EXCLUDED (no leakage, FR-018a). **Hop B** (`NetworkProduct`, optional)
  itemizes attributed figures by the brand SKU where a CONFIRMED mapping exists; attributed-but-unmapped sales
  are **counted** in a single aggregated "unmapped" bucket, never dropped (FR-018b). Enums:
  `DesignationStatus ∈ {ACTIVE, REMOVED}`; `MatchSource ∈ {AUTO_BARCODE, AUTO_SKU, MANUAL}` (no HSN);
  `MappingStatus ∈ {SUGGESTED, CONFIRMED}`.
