# Quickstart — Brand → Distributor DMS + SFA

End-to-end walkthrough of the core loops. Assumes the `trade` backend module is deployed, the mobile
`feature/trade` SFA module is installed, and two workspaces exist: a **brand** (B) and a **distributor** (D),
both already using Ampairs. All requests carry `X-Workspace-ID` + JWT.

## 0. Roles & setup (one-time)
- In D, an ADMIN assigns a member the **FIELD_REP** role (level 30) and creates a `Beat` with ordered
  outlets (existing D customers) and a `JourneyPlan` (PJP) putting the rep on that beat for given weekdays.

## 1. Link the brand and distributor (consent edge)
```
# Brand invites (default coded outlets)
POST /trade/v1/links            X-Workspace-ID: B
  { "distributor_workspace_id": "D", "consent_scope": { "retailer_visibility": "CODED" } }
  → status INVITED

# Distributor accepts (optionally tightens scope)
POST /trade/v1/links/{uid}/accept   X-Workspace-ID: D   → status ACCEPTED
```
Before acceptance, any brand snapshot read returns 403 `ConsentRequiredException`. After acceptance, the
brand can read only what the scope allows.

## 1b. Designate the brand label, then (optionally) map SKUs
The distributor carries many brands and already tags products with its own in-catalog brand labels
(`ProductBrand`). To attribute sales to Brand B, the distributor designates which label = Brand B:
```
# Hop A (required): designate a ProductBrand label for the linked brand
POST /trade/v1/network-brands   X-Workspace-ID: D
  { "link_uid": "TLN...", "distributor_product_brand_uid": "PBR-nestle" }   → status ACTIVE
# Brand B can see the designation read-only:
GET  /trade/v1/network-brands?link_uid=TLN...   X-Workspace-ID: B
```
Now every D product under that label attributes to Brand B (point-in-time, as of each sale); other brands'
products never reach Brand B. Sales with no SKU mapping still count, under an aggregated "unmapped" total.
```
# Hop B (optional): reconcile specific products to Brand B's SKUs for SKU-grain itemization
GET  /trade/v1/brand-catalog?link_uid=TLN...    X-Workspace-ID: D   # read Brand B's published catalog
POST /trade/v1/network-products X-Workspace-ID: D
  { "link_uid": "TLN...", "distributor_product_uid": "PRD...", "brand_product_uid": "PRD...",
    "brand_sku_code": "BRX-12", "match_source": "AUTO_BARCODE" }    → status CONFIRMED
```

## 2. Rep runs the beat — OFFLINE (the SFA core loop)
On the rep's device (airplane mode):
1. Open **Today's Beat** → planned outlets in sequence (from PJP/PlannedVisit, pulled earlier).
2. **Check in** at outlet #1 → `Attendance(CHECK_IN)` + the visit capture location/time on-device.
   Out-of-radius or no GPS? The visit is still saved, flagged `OUT_OF_RADIUS`/`NO_LOCATION` (never blocked).
3. **Take an order** at the counter → a distributor `order` is authored locally (its `FieldOrder` ref +
   `Visit.outcome = PRODUCTIVE`).
4. Found a new shop not on any beat? **Add outlet** offline (creates a D `customer`), then visit it with
   `ad_hoc = true`.
5. **Check out** at end of day → `Attendance(CHECK_OUT)`.

Everything persists locally; nothing blocks on the network (SC-001/003).

## 3. Sync when connectivity returns
The offline engine pushes via the canonical `/sync` (UID-keyed, idempotent):
```
POST /trade/v1/visits/sync          body: [VisitRequest...]      → upserted
POST /trade/v1/field-orders/sync    body: [FieldOrderRequest...] → upserted
POST /trade/v1/attendance/sync      body: [AttendanceRequest...] → upserted
# new outlet rode the existing customer /sync first; its uid is referenced by the visit/order
```
Re-running a push with the same uids upserts once (SC-002). Counter orders now appear in D's normal order
list and are tagged `SECONDARY`.

## 4. Secondary-sales rolls up to the brand (≤ ~5 min)
D's `InvoiceFinalizedEvent`/order events fire → the `trade` listener tags `SECONDARY` → `SnapshotService`
enqueues a rebuild, **coalesced to ≤ once per ~5 min per distributor**. The brand reads:
```
GET /trade/v1/snapshots/secondary-sales?distributor_workspace_id=all-linked&grain=SKU_PERIOD&period_from=2026-06&period_to=2026-06
  X-Workspace-ID: B  → aggregated qty/value by SKU × month across all ACCEPTED links
```
Only Brand B's products appear (attributed via the Hop A designation); SKU-mapped ones itemize by brand SKU,
the rest fall into an aggregated "unmapped" total. With `grain=SKU_PERIOD_AREA`, `area_code` is the retailer's
**pincode**, so the same area aggregates across distributors with no mapping. Outlets appear as `outlet_code`
only (scope CODED). A backdated/cancelled D invoice bumps the snapshot `version` within the same window —
totals self-correct, no double count (SC-005).

## 5. Distributor stock & replenishment (brand)
```
GET /trade/v1/snapshots/distributor-stock?distributor_workspace_id=D   X-Workspace-ID: B
  → quantity_on_hand + days_of_stock + out_of_stock per SKU (requires scope.share_stock)
```

## 6. Primary order (brand → distributor handshake)
```
# Brand creates the order in ITS tenant, then registers it over the link
POST /trade/v1/primary-orders   X-Workspace-ID: B   { "link_uid": "TLN...", "brand_order_uid": "ORD-B..." }
  → status PLACED
# Distributor confirms → a normal order is created in D's tenant (its pricing/validation apply)
POST /trade/v1/primary-orders/{uid}/confirm   X-Workspace-ID: D
  → status CONFIRMED, distributor_order_uid = ORD-D...
```

## 7. Revoke (kill switch)
```
POST /trade/v1/links/{uid}/revoke   X-Workspace-ID: D
```
The brand's subsequent snapshot reads return 403 immediately (SC-009). Data the rep already captured stays
owned by D.

## Validation commands
```
# Backend
./gradlew :trade:test            # consent-gate, snapshot-recompute determinism, claim lifecycle, PII projection
./gradlew :ampairs_service:flywayInfo   # confirm V1.0.117 is free before migrating
./gradlew ciBuild
# Mobile (sibling repo ampairs-app)
./gradlew :feature:trade:check
./gradlew shared:compileKotlinIosSimulatorArm64 androidApp:compileDebugKotlinAndroid desktopApp:compileKotlin
```
