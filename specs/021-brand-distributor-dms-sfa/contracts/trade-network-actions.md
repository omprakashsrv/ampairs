# Contract — Trade network actions (links, primary orders, schemes, claims)

Non-sync, request/response actions. `ApiResponse<T>` envelope. Tenant per actor as noted.

## Links (the consent edge)

### Invite — brand creates a pending link
```
POST /trade/v1/links                                 # tenant: brand (ADMIN/OWNER)
body: { "distributor_workspace_id": "WSP...", "network_uid": "TNW...|null",
        "consent_scope": { "share_secondary_sales": true, "share_stock": true,
                           "retailer_visibility": "CODED", "share_targets": true } }
→ ApiResponse<TradeLinkResponse>   # status = INVITED
```
`retailer_visibility` defaults to `CODED` if omitted (clarification R13.1). `IDENTIFIED` exposes
outlet name/area only; full retailer contact PII is never shareable.

### Accept / Decline — distributor responds
```
POST /trade/v1/links/{uid}/accept                    # tenant: distributor (ADMIN/OWNER)
body: { "consent_scope": { ... }? }                  # distributor may tighten scope on accept
→ ApiResponse<TradeLinkResponse>   # status = ACCEPTED

POST /trade/v1/links/{uid}/decline                   # → status = DECLINED
```

### Revoke — distributor (or brand) ends an active link
```
POST /trade/v1/links/{uid}/revoke                    # tenant: distributor
→ ApiResponse<TradeLinkResponse>   # status = REVOKED; all further brand reads denied immediately (SC-009)
```

### TradeLinkResponse
```json
{ "uid": "TLN...", "network_uid": "TNW...", "brand_workspace_id": "WSP...",
  "distributor_workspace_id": "WSP...", "status": "INVITED|ACCEPTED|DECLINED|REVOKED",
  "consent_scope": { "share_secondary_sales": true, "share_stock": true,
                     "retailer_visibility": "CODED|IDENTIFIED", "share_targets": true },
  "invited_at": "...", "responded_at": "...|null", "revoked_at": "...|null" }
```
Illegal transitions (e.g. accept a REVOKED link) → `LinkStateException` (409). Reads with no ACCEPTED link →
`ConsentRequiredException` (403).

## Primary orders (brand → distributor handshake — R13.5)

```
POST /trade/v1/primary-orders                        # tenant: brand
body: { "link_uid": "TLN...", "brand_order_uid": "ORD..." }   # order already created in the BRAND tenant
→ ApiResponse<PrimaryOrderLinkResponse>   # status = PLACED ; requires ACCEPTED link

POST /trade/v1/primary-orders/{uid}/confirm          # tenant: distributor
→ ApiResponse<PrimaryOrderLinkResponse>   # status = CONFIRMED ; creates a normal order in the DISTRIBUTOR tenant
                                          #   (via OrderService) and sets distributor_order_uid

POST /trade/v1/primary-orders/{uid}/reject           # tenant: distributor → status = REJECTED
```
```json
// PrimaryOrderLinkResponse
{ "uid": "POL...", "link_uid": "TLN...", "brand_order_uid": "ORD...",
  "status": "PLACED|CONFIRMED|REJECTED", "distributor_order_uid": "ORD...|null", "responded_at": "...|null" }
```
No endpoint writes directly into the distributor's order tables; only `confirm` does, through the
distributor's own `OrderService` (its pricing/validation apply).

## Product linking — two-level (sub-spec product-brand-attribution)

Brand and distributor are separate workspaces with separate catalogs. Linking is **two-level**:
**Hop A** (`NetworkBrand`) attributes via the distributor's existing in-catalog brand label; **Hop B**
(`NetworkProduct`, optional) reconciles to the brand's specific SKU.

### Hop A — brand attribution (`NetworkBrand`) — required

```
GET  /trade/v1/network-brands?link_uid={TLN...}         # tenant: distributor (or brand, read-only) — designations for the link
→ ApiResponse<PageResponse<NetworkBrandRow>>

POST /trade/v1/network-brands                           # tenant: distributor — designate one of its ProductBrand labels for the linked brand
body: { "link_uid": "TLN...", "distributor_product_brand_uid": "PBR..." }
→ ApiResponse<NetworkBrandRow>   # status = ACTIVE ; requires ACTIVE link

DELETE /trade/v1/network-brands/{uid}                   # tenant: distributor — remove a designation (status = REMOVED)
```
```json
// NetworkBrandRow
{ "uid": "NBR...", "link_uid": "TLN...", "distributor_product_brand_uid": "PBR...",
  "brand_workspace_id": "WSP...", "status": "ACTIVE|REMOVED" }
```
Multiple labels MAY be designated for one brand (aliases); a label MAY be designated for at most one brand per
active link. The **brand** can `GET` this read-only; only the **distributor** can create/remove. All distributor
products under a designated label attribute to the brand (point-in-time, as of sale).

### Hop B — SKU mapping (`NetworkProduct`) — optional refinement

The brand publishes its catalog down the active link; the distributor reconciles its products to brand SKUs
(auto-suggested by **barcode/SKU**, manual confirm/override). A product attributed by Hop A but not Hop B-mapped
is still **counted** in the brand's totals (aggregated "unmapped" bucket) — never excluded.

```
GET  /trade/v1/brand-catalog?link_uid={TLN...}          # tenant: distributor — read the brand's published catalog (consent-gated, ACTIVE link)
→ ApiResponse<PageResponse<BrandProductRow>>            # { brand_product_uid, brand_sku_code, name, barcode? }

GET  /trade/v1/network-products?link_uid={TLN...}&status=SUGGESTED|CONFIRMED   # tenant: distributor
→ ApiResponse<PageResponse<NetworkProductRow>>          # incl. auto-suggestions (match_source=AUTO_BARCODE|AUTO_SKU); only products under a designated label are candidates

POST /trade/v1/network-products                         # tenant: distributor — create/confirm a SKU mapping
body: { "link_uid": "TLN...", "distributor_product_uid": "PRD...",
        "brand_product_uid": "PRD...", "brand_sku_code": "BRX-12", "match_source": "MANUAL|AUTO_BARCODE|AUTO_SKU" }
→ ApiResponse<NetworkProductRow>   # status = CONFIRMED
```
```json
// NetworkProductRow
{ "uid": "NPR...", "link_uid": "TLN...", "distributor_product_uid": "PRD...",
  "brand_product_uid": "PRD...", "brand_sku_code": "BRX-12",
  "match_source": "AUTO_BARCODE|AUTO_SKU|MANUAL", "status": "SUGGESTED|CONFIRMED", "active": true }
```
At most one CONFIRMED mapping per `(link, distributor_product_uid)`. Auto-suggestions come from the brand-catalog
matcher (shared barcode/SKU — **no HSN**, which is a tax attribute); the distributor confirms/overrides them.

## Schemes (Phase 3 — brand)

```
POST /trade/v1/schemes                # create
POST /trade/v1/schemes/{uid}/publish  # publish down in-scope links
GET  /trade/v1/schemes                # distributor lists schemes published to it (consent-gated)
```

## Claims (Phase 3)

```
GET  /trade/v1/claims                  # both tiers, scoped
POST /trade/v1/claims/{uid}/submit     # distributor: DRAFT → SUBMITTED
POST /trade/v1/claims/{uid}/approve    # brand: SUBMITTED → APPROVED
POST /trade/v1/claims/{uid}/reject     # brand: SUBMITTED → REJECTED  (body: { "reason": "..." })
POST /trade/v1/claims/{uid}/settle     # brand: APPROVED → SETTLED   (body: { "reference": "...", "amount": "..." })
```
`computed_amount` is derived from the same SecondarySalesSnapshot both sides read, so brand and distributor
see an identical figure (SC-008). Illegal transitions → `ClaimStateException` (409).
