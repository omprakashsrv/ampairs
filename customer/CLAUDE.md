# customer module

CRM — customers, groups, types, images. GST-aware addressing, GPS location, credit management.

## Key entities
- `Customer` — name, phone, gstNumber, panNumber, creditLimit, creditDays, outstandingAmount, billingAddress, shippingAddress, location (GPS Point), attributes (JSON)
- `CustomerGroup` — groupCode, priorityLevel, defaultDiscountPercentage
- `CustomerType` — typeCode, defaultCreditLimit, defaultCreditDays, allowCreditFacility
- `CustomerImage` — storageUrl, thumbnailUrl, isPrimary, displayOrder

## Base path
`/customer/v1/**`

## Migrations
`V1.0.6`, `V1.0.20` (performance indexes)

## Full docs
`docs/modules/customer.md`
