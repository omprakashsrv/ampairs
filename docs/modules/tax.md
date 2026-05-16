# tax module

GST tax configuration, HSN/SAC code catalog, tax rule engine, and calculation service used by the order and invoice modules.

## Model

Tax in Ampairs is a two-layer system:

```
Master Layer (global, seeded by system)
  MasterTaxCode   — HSN/SAC codes (e.g. 0901 for coffee)
  MasterTaxComponent — tax components (CGST, SGST, IGST, CESS)
  MasterTaxRule   — default rates per code + jurisdiction

Workspace Layer (per-workspace subscription)
  TaxConfiguration — workspace tax strategy and jurisdiction
  TaxCode         — workspace's subscribed codes (from master)
  TaxRule         — workspace-specific rate overrides
  TaxComponent    — enabled components per workspace
```

## REST Endpoints

### Configuration (`/tax/v1/configuration`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/tax/v1/configuration` | Get workspace tax config |
| POST | `/tax/v1/configuration` | Create config (first-time setup) |
| PUT | `/tax/v1/configuration` | Update config |

### Tax Codes (`/tax/v1/code`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/tax/v1/code` | List subscribed codes with pagination |
| GET | `/tax/v1/code/favorites` | List favorite codes |
| GET | `/tax/v1/code/{taxCodeId}` | Get code by ID |
| POST | `/tax/v1/code/subscribe` | Subscribe to a master code |
| POST | `/tax/v1/code/bulk-subscribe` | Bulk subscribe |
| DELETE | `/tax/v1/code/{taxCodeId}` | Unsubscribe |
| PATCH | `/tax/v1/code/{taxCodeId}` | Update code config |
| POST | `/tax/v1/code/{taxCodeId}/usage` | Increment usage counter |
| POST | `/tax/v1/code/{taxCodeId}/favorite` | Toggle favorite |

### Tax Rules (`/tax/v1/rule`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/tax/v1/rule` | List rules with pagination |
| GET | `/tax/v1/rule/tax-code/{taxCodeId}` | Rules for a code |
| GET | `/tax/v1/rule/{taxRuleId}` | Get rule by ID |
| PUT | `/tax/v1/rule/{taxRuleId}` | Update rule |
| DELETE | `/tax/v1/rule/{taxRuleId}` | Delete rule |

### Master Catalog (read-only, global)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/tax/v1/master-code/search` | Search HSN/SAC codes |
| GET | `/tax/v1/master-code/popular` | Most-used codes |
| GET | `/tax/v1/master-component` | All tax components |
| GET | `/tax/v1/master-component/search` | Search components |
| GET | `/tax/v1/master-component/{id}` | Get component |
| GET | `/tax/v1/master-component/by-type/{typeId}` | By component type |
| GET | `/tax/v1/master-rule` | All master rules |
| GET | `/tax/v1/master-rule/search` | Search rules |
| GET | `/tax/v1/master-rule/{id}` | Get rule |
| GET | `/tax/v1/master-rule/by-master-code/{codeId}` | Rules by code |

## Key Entities

### TaxConfiguration (workspace-level)

```kotlin
class TaxConfiguration : OwnableBaseDomain() {
    val countryCode: String            // "IN"
    val taxStrategy: TaxStrategy       // INDIA_GST, USA_SALES_TAX, etc.
    val defaultTaxCodeSystem: String   // HSN_CODE or SAC_CODE
    val taxJurisdictions: List<String> // state codes for IGST threshold
    val industry: String?
    val autoSubscribeNewCodes: Boolean
}
```

### TaxCode (workspace subscription)

```kotlin
class TaxCode : OwnableBaseDomain() {
    val masterTaxCodeId: String
    val code: String               // HSN or SAC code
    val codeType: String           // HSN, SAC
    val description: String
    val customName: String?
    val customTaxRuleId: String?
    val usageCount: Int            // for sorting frequently-used codes
    val lastUsedAt: Instant?
    val isFavorite: Boolean
    val isActive: Boolean
    val notes: String?
}
```

### TaxRule (workspace overrides)

```kotlin
class TaxRule : OwnableBaseDomain() {
    val taxCodeId: String
    val taxCode: String
    val jurisdiction: String       // state code
    val jurisdictionLevel: String  // STATE, INTERSTATE, UNION_TERRITORY
    val componentComposition: List<TaxComponent>  // JSON
    val isActive: Boolean
}
```

### MasterTaxCode (system-seeded, global)

```kotlin
class MasterTaxCode : BaseDomain() {
    val countryCode: String
    val codeType: String           // HSN or SAC
    val code: String
    val description: String
    val chapter: String?
    val heading: String?
    val subHeading: String?
    val category: String?
    val defaultTaxRate: BigDecimal
    val isActive: Boolean
}
```

## GST Component Composition

A typical CGST + SGST rule for intra-state supply:

```json
{
  "jurisdiction": "KA",
  "jurisdiction_level": "STATE",
  "component_composition": [
    { "component": "CGST", "rate": 9.0 },
    { "component": "SGST", "rate": 9.0 }
  ]
}
```

Inter-state supply:
```json
{
  "jurisdiction_level": "INTERSTATE",
  "component_composition": [
    { "component": "IGST", "rate": 18.0 }
  ]
}
```

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.9__create_tax_module_tables.sql` | Initial v1 tables |
| `V1.0.38__create_tax_module_v2_tables.sql` | v2 — workspace subscription model |
| `V1.0.39__drop_tax_module_v1_tables.sql` | Drop deprecated v1 tables |

## Package Structure

```
com.ampairs.tax
├── config/
├── controller/     — MasterTaxCodeController, MasterTaxComponentController,
│                     MasterTaxRuleController, TaxCodeController,
│                     TaxComponentController, TaxConfigurationController, TaxRuleController
├── domain/
│   ├── dto/        — TaxModuleDtos.kt (all request/response DTOs)
│   └── model/      — MasterTaxCode, MasterTaxComponent, MasterTaxRule,
│                     TaxCode, TaxComponent, TaxConfiguration, TaxRule
├── repository/     — per-entity repositories
└── service/        — TaxCodeService, TaxConfigurationServiceV2, TaxRuleService,
                      MasterTaxCodeService, MasterTaxComponentService, MasterTaxRuleService,
                      GstRuleTemplateService
```
