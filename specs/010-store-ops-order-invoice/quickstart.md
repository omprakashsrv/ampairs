# Quickstart / Acceptance Walkthrough: Store Ops Order & Invoice

**Spec**: `specs/010-store-ops-order-invoice/spec.md` · **Plan**: `plan.md`
Manual end-to-end checks mapping to the success criteria. Run on **Android, iOS, and Desktop**.

## Setup
1. Workspace with: a seller business profile (GSTIN → seller state), the `INDIA_GST` tax config, at
   least one HSN tax code @ 18% subscribed, a product priced ₹100/PCS with a `1 BOX = 12 PCS` conversion,
   and a buyer customer with a GSTIN.
2. Business settings: `defaultPriceMode=TAX_EXCLUSIVE`, `defaultOverallDiscountMode=PRE_TAX_APPORTIONED`,
   `invoiceSeriesPrefix` set for this device.

## A — Order with intra-state GST (SC-001, SC-003)
1. New order → pick the buyer (same state as seller).
2. Add the product, qty 2 (base unit PCS) → line shows **CGST ₹9 + SGST ₹9**, taxable ₹200, line ₹218.
3. Save **with networking off** → order appears in the list, `synced=false`.
4. Turn networking on → order pushes, gets an `orderNumber`, `synced=true`; pushing again creates no duplicate.

## B — Inter-state GST (SC-003)
1. New order → buyer in a **different** state → same line shows **IGST ₹18** (not CGST/SGST). (Confirms the inverted-`TaxSpec` fix.)

## C — Unit conversion on a line (SC-008)
1. Add the product, switch the line unit to **BOX**, qty 5 → unit price ₹120/BOX, taxable ₹600,
   `baseQuantity = 60` PCS stored.
2. Switch the unit to **PCS** qty 60 → identical taxable ₹600.
3. A unit with `decimalPlaces=0` rejects fractional qty.

## D — Discounts (SC-009)
1. Two lines (₹500 @18%, ₹500 @18%). Apply a **10% line discount** to line 1 → its taxable ₹450.
2. Apply a **flat ₹200 overall discount** (PRE_TAX_APPORTIONED) → discount apportions across lines by
   taxable, each line's GST recomputes, grand total reconciles to taxable + tax.
3. Switch business setting to `POST_TAX_REDUCTION` → GST computed on full lines, ₹200 subtracted from the
   grand total; per-HSN tax unchanged.

## E — Tax inclusive vs exclusive (SC-010)
1. Same line in `TAX_EXCLUSIVE` → GST added on top.
2. Toggle the document to `TAX_INCLUSIVE` → GST is extracted from the price; components + taxable sum
   back to the inclusive line amount.

## F — Independent invoice + client numbering (SC-002, SC-011)
1. New invoice (no order), walk-in customer, add lines → GST computes as above.
2. Save **offline** → invoice has a complete number `"{prefix}/{FY}/{seq}"` and is printable immediately.
3. On a **second device** (same workspace, different series prefix) create an invoice offline → numbers
   never collide. Push both → backend accepts both (unique series+number); a forced duplicate is rejected.

## G — Line price override (C3)
1. Edit a line's unit price → entered value is used, GST recomputes on it, `productPrice` keeps the original.

## H — Order → Invoice conversion (SC-004)
1. From a saved order tap **Create Invoice** → invoice built from order lines/tax/discounts/units,
   `orderRefId`↔`invoiceRefId` cross-link, invoice gets its series number.
2. Tap **Create Invoice** again → opens the existing invoice (no duplicate).
3. Do it offline → both push with links intact.

## I — Inline product create (SC-005)
1. In the picker choose **New product** → name, ₹ price, HSN code, unit → product created
   (`PENDING_PUSH`) and added as a line.
2. Push → product syncs **before** the order/invoice that references it.

## J — Regression & build (SC-006, SC-007)
1. Existing order list/search/paging and saved-order display unchanged.
2. No repository makes a network call (delegates own all order/invoice API traffic).
3. `./gradlew androidApp:compileDebugKotlinAndroid && shared:compileKotlinIosSimulatorArm64 && desktopApp:compileKotlin` all green.
4. Backend: `./gradlew :ampairs_service:flywayInfo && buildAll` and order/invoice module tests pass.
