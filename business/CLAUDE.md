# business module

One business profile per workspace — legal details, address, hours, branding (logo + gallery).

## Key entities
- `Business` — name, businessType (RETAIL/WHOLESALE/SERVICE/MANUFACTURING/ECOMMERCE), address, phone, email, taxId (GSTIN), timezone, currency, openingHours, operatingDays (JSON)
- `BusinessImage` — storageUrl, thumbnailUrl, imageType (LOGO/GALLERY/BANNER), isPrimary, displayOrder

## Base path
`/business/v1/businesses/**`

## Migrations
`V1.0.12`, `V1.0.14` (custom attributes), `V1.0.25` (logo + images)

## Full docs
`docs/modules/business.md`
