# Tax Module V2 - Implementation Documentation

## Overview

The Tax Module V2 is a comprehensive multi-tenant tax management system built on Spring Boot with multi-tenancy via `@TenantId`. The module supports:

- **Global tax code registry** (master_tax_codes) - System-managed HSN/SAC codes
- **Master templates** (master_tax_component, master_tax_rule) - Reusable tax configurations
- **Workspace subscriptions** - Tenant-specific tax code subscriptions
- **Auto-component creation** - Automatic workspace tax_component and tax_rule creation on subscription
- **Incremental sync** - Offline-first mobile architecture support
- **Multi-jurisdiction** - Support for INTRA_STATE, INTER_STATE, and cross-border scenarios

---

## Database Schema

### Migration Files
- **PostgreSQL**: `V1.0.38__create_tax_module_v2_tables.sql`
- **MySQL**: `V1.0.38__create_tax_module_v2_tables.sql`

### Tables

#### 1. master_tax_codes (Global)
Global tax code registry maintained by system administrators.

**Key Fields**:
- `uid` - Unique identifier (e.g., "MTC_HSN_1001")
- `country_code` - ISO country code (IN, US, UK)
- `code_type` - HSN_CODE, SAC_CODE, TAX_CATEGORY
- `code` - Actual tax code (e.g., "1001", "998314")
- `description` - Full description
- `default_tax_rate` - Default GST/tax rate percentage
- `category` - Product/service category

**Sample Data**: 9 India GST codes (HSN/SAC) pre-populated

**Unique Constraint**: `(country_code, code_type, code)`

---

#### 2. master_tax_component (Global)
Global tax component templates (CGST, SGST, IGST rate definitions).

**Key Fields**:
- `uid` - Unique identifier (e.g., "MCOMP_CGST_9")
- `component_type_id` - Component type (TYPE_CGST, TYPE_SGST, TYPE_IGST)
- `component_name` - Short name (CGST, SGST, IGST)
- `component_display_name` - Display name (e.g., "CGST 9%")
- `tax_type` - GST, VAT, SALES_TAX
- `jurisdiction` - INDIA, MAHARASHTRA, etc.
- `jurisdiction_level` - COUNTRY, STATE, COUNTY
- `rate_percentage` - Tax rate (e.g., 9.0, 14.0)

**Sample Data**: 18 components for standard GST rates (0%, 0.25%, 3%, 5%, 12%, 18%, 28%)
- CGST variants: 6 records (0, 0.125, 1.5, 2.5, 6, 9, 14)
- SGST variants: 6 records (0, 0.125, 1.5, 2.5, 6, 9, 14)
- IGST variants: 6 records (0, 0.25, 3, 5, 12, 18, 28)

**Unique Constraint**: `(component_type_id, rate_percentage)`

---

#### 3. master_tax_rule (Global)
Global tax rule templates with component composition mapping.

**Key Fields**:
- `uid` - Unique identifier (e.g., "MTR_HSN_1001")
- `country_code` - ISO country code
- `master_tax_code_id` - Foreign key to master_tax_codes
- `tax_code` - Tax code value
- `tax_code_type` - HSN_CODE, SAC_CODE
- `tax_rate` - Total tax rate
- `jurisdiction` - INDIA
- `jurisdiction_level` - COUNTRY, STATE
- `component_composition` - JSONB structure:
  ```json
  {
    "INTRA_STATE": {
      "scenario": "INTRA_STATE",
      "components": [
        {"id": "MCOMP_CGST_9", "name": "CGST", "rate": 9.0, "order": 1},
        {"id": "MCOMP_SGST_9", "name": "SGST", "rate": 9.0, "order": 2}
      ],
      "totalRate": 18.0
    },
    "INTER_STATE": {
      "scenario": "INTER_STATE",
      "components": [
        {"id": "MCOMP_IGST_18", "name": "IGST", "rate": 18.0, "order": 1}
      ],
      "totalRate": 18.0
    }
  }
  ```

**Sample Data**: 9 rules mapping to sample master_tax_codes

**Unique Constraint**: `master_tax_code_id`

---

#### 4. tax_configurations (Workspace-scoped)
Workspace-level tax settings (multi-tenant via `owner_id`).

**Key Fields**:
- `owner_id` - Workspace/tenant ID (multi-tenancy key)
- `country_code` - Primary country for workspace
- `tax_strategy` - INDIA_GST, USA_SALES_TAX, etc.
- `default_tax_code_system` - HSN_CODE, SAC_CODE
- `tax_jurisdictions` - Array of jurisdiction codes
- `auto_subscribe_new_codes` - Boolean flag
- `metadata` - JSONB for country-specific settings

**Unique Constraint**: `owner_id` (one config per workspace)

---

#### 5. tax_codes (Workspace-scoped)
Workspace tax code subscriptions (multi-tenant via `owner_id`).

**Key Fields**:
- `owner_id` - Workspace/tenant ID (multi-tenancy key)
- `master_tax_code_id` - Foreign key to master_tax_codes
- `code`, `code_type`, `description` - Cached from master for offline access
- `custom_name` - Workspace-specific custom name
- `custom_tax_rule_id` - Optional custom rule override
- `usage_count` - Incremented on usage
- `last_used_at` - Timestamp of last usage
- `is_favorite` - Favorite flag
- `notes` - Workspace notes
- `sync_status` - SYNCED, PENDING_SYNC

**Unique Constraint**: `(owner_id, master_tax_code_id)`

---

#### 6. tax_rules (Workspace-scoped)
Tax calculation rules with component composition (multi-tenant via `owner_id`).

**Key Fields**:
- `owner_id` - Workspace/tenant ID (multi-tenancy key)
- `tax_code_id` - Foreign key to tax_codes
- `country_code` - Country code
- `tax_code`, `tax_code_type` - Tax code details
- `jurisdiction`, `jurisdiction_level` - Location details
- `component_composition` - JSONB structure (same format as master_tax_rule)

**Unique Constraint**: `(owner_id, tax_code_id, jurisdiction, scenario_type, effective_date)`

**Auto-creation**: Created automatically when workspace subscribes to a tax code via master_tax_rule template

---

#### 7. tax_components (Workspace-scoped)
Workspace tax components (CGST, SGST, IGST instances) - multi-tenant via `owner_id`.

**Key Fields**:
- `owner_id` - Workspace/tenant ID (multi-tenancy key)
- `component_type_id` - Component type
- `component_name` - Short name
- `component_display_name` - Display name
- `tax_type` - GST, VAT, etc.
- `jurisdiction`, `jurisdiction_level` - Location
- `rate_percentage` - Tax rate
- `is_compound` - Compound tax flag
- `calculation_method` - PERCENTAGE, FLAT_AMOUNT

**Unique Constraint**: `(owner_id, component_type_id)`

**Auto-creation**: Created automatically when workspace subscribes to a tax code, copied from master_tax_component templates

---

## Entity Models

### Global Entities (No Tenant Scoping)

1. **MasterTaxCode** (extends BaseDomain)
   - Global tax code registry
   - Full-text search support
   - Sample data: 9 India GST codes

2. **MasterTaxComponent** (extends BaseDomain)
   - Global component templates
   - Sample data: 18 GST components

3. **MasterTaxRule** (extends BaseDomain)
   - Global rule templates with component_composition
   - Sample data: 9 rules

### Workspace Entities (Multi-Tenant via @TenantId)

4. **TaxConfiguration** (extends OwnableBaseDomain)
   - Workspace tax settings
   - One per workspace

5. **TaxCode** (extends OwnableBaseDomain)
   - Workspace subscriptions to master codes
   - Tracks usage and favorites

6. **TaxRule** (extends OwnableBaseDomain)
   - Workspace tax calculation rules
   - Scenario-based (INTRA_STATE, INTER_STATE)

7. **TaxComponent** (extends OwnableBaseDomain)
   - Workspace tax components
   - Referenced by tax_rule.component_composition

### Domain Classes

8. **ComponentComposition** (data class)
   - Represents a scenario's component breakdown
   - Fields: scenario, components, totalRate

9. **ComponentReference** (data class)
   - Represents a single component in composition
   - Fields: id, name, rate, order

---

## API Endpoints

### Base Path: `/api/v1/tax`

All workspace-scoped endpoints automatically filtered by `@TenantId` - no explicit `workspaceId` needed.

---

### 1. Tax Configuration APIs

#### 1.1 Get Workspace Tax Configuration
```
GET /api/v1/tax/configuration
```

**Description**: Retrieve workspace tax configuration (auto-scoped to current tenant).

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "WTC_001",
    "countryCode": "IN",
    "taxStrategy": "INDIA_GST",
    "defaultTaxCodeSystem": "HSN_CODE",
    "taxJurisdictions": ["MH", "GJ", "DL"],
    "industry": "RETAIL_GROCERY",
    "autoSubscribeNewCodes": true,
    "syncedAt": 1733270400000,
    "metadata": {}
  }
}
```

**Controller**: `TaxConfigurationController.kt`

---

#### 1.2 Update Workspace Tax Configuration
```
PUT /api/v1/tax/configuration
```

**Request Body**:
```json
{
  "countryCode": "IN",
  "taxStrategy": "INDIA_GST",
  "defaultTaxCodeSystem": "HSN_CODE",
  "taxJurisdictions": ["MH", "GJ", "DL"],
  "industry": "RETAIL_GROCERY"
}
```

**Controller**: `TaxConfigurationController.kt`

---

### 2. Master Tax Code APIs

#### 2.1 Search Master Tax Codes
```
GET /api/v1/tax/master-code/search?query={term}&countryCode={code}&codeType={type}
```

**Query Parameters**:
- `query` (required): Search term
- `countryCode` (required): ISO country code
- `codeType` (optional): HSN_CODE, SAC_CODE, TAX_CATEGORY
- `category` (optional): Filter by category
- `page` (default: 0)
- `size` (default: 50, max: 100)

**Example**:
```
GET /api/v1/tax/master-code/search?query=oil&countryCode=IN&codeType=HSN_CODE&page=0&size=20
```

**Controller**: `MasterTaxCodeController.kt`

---

#### 2.2 Get Popular Tax Codes
```
GET /api/v1/tax/master-code/popular?countryCode={code}
```

**Query Parameters**:
- `countryCode` (required)
- `industry` (optional)
- `limit` (default: 20)

**Controller**: `MasterTaxCodeController.kt`

---

#### 2.3 Get Master Tax Code by ID
```
GET /api/v1/tax/master-code/{id}
```

**Controller**: `MasterTaxCodeController.kt`

---

### 3. Master Tax Component APIs

#### 3.1 Get All Master Tax Components
```
GET /api/v1/tax/master-component?page={page}&size={size}
```

**Controller**: `MasterTaxComponentController.kt`

---

#### 3.2 Search Master Tax Components
```
GET /api/v1/tax/master-component/search?componentTypeId={typeId}&jurisdiction={jurisdiction}
```

**Query Parameters**:
- `componentTypeId` (optional): TYPE_CGST, TYPE_SGST, TYPE_IGST
- `jurisdiction` (optional): INDIA, MAHARASHTRA, etc.
- `page` (default: 0)
- `size` (default: 50, max: 100)

**Controller**: `MasterTaxComponentController.kt`

---

#### 3.3 Get Master Tax Component by ID
```
GET /api/v1/tax/master-component/{id}
```

**Controller**: `MasterTaxComponentController.kt`

---

#### 3.4 Get Master Tax Components by Type
```
GET /api/v1/tax/master-component/by-type/{componentTypeId}
```

**Controller**: `MasterTaxComponentController.kt`

---

### 4. Master Tax Rule APIs

#### 4.1 Get All Master Tax Rules
```
GET /api/v1/tax/master-rule?page={page}&size={size}
```

**Controller**: `MasterTaxRuleController.kt`

---

#### 4.2 Search Master Tax Rules
```
GET /api/v1/tax/master-rule/search?countryCode={code}&taxCodeType={type}
```

**Query Parameters**:
- `countryCode` (required): IN, US, UK
- `taxCodeType` (optional): HSN_CODE, SAC_CODE
- `page` (default: 0)
- `size` (default: 50, max: 100)

**Controller**: `MasterTaxRuleController.kt`

---

#### 4.3 Get Master Tax Rule by ID
```
GET /api/v1/tax/master-rule/{id}
```

**Controller**: `MasterTaxRuleController.kt`

---

#### 4.4 Get Master Tax Rules by Master Tax Code
```
GET /api/v1/tax/master-rule/by-master-code/{masterTaxCodeId}
```

**Controller**: `MasterTaxRuleController.kt`

---

### 5. Workspace Tax Code APIs (Subscriptions)

#### 5.1 Subscribe to Tax Code
```
POST /api/v1/tax/code/subscribe
```

**Description**: Subscribe workspace to a master tax code. Automatically creates workspace tax_component and tax_rule records using master templates.

**Request Body**:
```json
{
  "masterTaxCodeId": "MTC_HSN_1001",
  "customName": "Cooking Oil Products",
  "isFavorite": false,
  "notes": "Used for cooking oil products"
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "TCD_001",
    "masterTaxCodeId": "MTC_HSN_1001",
    "code": "1001",
    "codeType": "HSN_CODE",
    "description": "Live animals; animal products",
    "shortDescription": "Live animals",
    "customName": "Cooking Oil Products",
    "usageCount": 0,
    "lastUsedAt": null,
    "isFavorite": false,
    "notes": "Used for cooking oil products",
    "isActive": true,
    "addedAt": 1733270400000,
    "updatedAt": 1733270400000,
    "syncStatus": "SYNCED"
  }
}
```

**Auto-Creation Logic**:
1. Looks up `master_tax_rule` by `master_tax_code_id`
2. Extracts component IDs from `component_composition` JSON
3. For each component ID, fetches `master_tax_component` and creates workspace `tax_component`
4. Creates workspace `tax_rule` with proper component references
5. Fallback: If no master template exists, generates standard GST composition

**Controller**: `TaxCodeController.kt`
**Service**: `TaxCodeService.kt:31-102` (subscribe method)

---

#### 5.2 Get Workspace Tax Codes (Incremental Sync)
```
GET /api/v1/tax/code?modifiedAfter={timestamp}
```

**Query Parameters**:
- `modifiedAfter` (optional): Timestamp for incremental sync
- `page` (default: 0)
- `size` (default: 1000)

**Example**:
```
GET /api/v1/tax/code?modifiedAfter=1733000000000&page=0&size=1000
```

**Controller**: `TaxCodeController.kt`

---

#### 5.3 Get Favorite Tax Codes
```
GET /api/v1/tax/code/favorites?page={page}&size={size}
```

**Controller**: `TaxCodeController.kt`

---

#### 5.4 Get Tax Code by ID
```
GET /api/v1/tax/code/{taxCodeId}
```

**Controller**: `TaxCodeController.kt`

---

#### 5.5 Unsubscribe from Tax Code
```
DELETE /api/v1/tax/code/{taxCodeId}
```

**Description**: Soft delete - sets `is_active = false`.

**Controller**: `TaxCodeController.kt`

---

#### 5.6 Update Tax Code Configuration
```
PATCH /api/v1/tax/code/{taxCodeId}
```

**Request Body**:
```json
{
  "isFavorite": true,
  "notes": "Updated notes",
  "customName": "Premium Cooking Oil"
}
```

**Controller**: `TaxCodeController.kt`

---

#### 5.7 Increment Usage Count
```
POST /api/v1/tax/code/{taxCodeId}/usage
```

**Request Body**:
```json
{
  "timestamp": 1733270400000
}
```

**Controller**: `TaxCodeController.kt`

---

#### 5.8 Set Favorite Status
```
POST /api/v1/tax/code/{taxCodeId}/favorite
```

**Request Body**:
```json
{
  "isFavorite": true
}
```

**Controller**: `TaxCodeController.kt`

---

#### 5.9 Bulk Subscribe Tax Codes
```
POST /api/v1/tax/code/bulk-subscribe
```

**Request Body**:
```json
{
  "masterTaxCodeIds": ["MTC_HSN_1001", "MTC_HSN_8517", "MTC_HSN_3004"]
}
```

**Response**:
```json
{
  "success": true,
  "data": {
    "successCount": 3,
    "failureCount": 0,
    "subscribedCodes": [...],
    "errors": []
  }
}
```

**Controller**: `TaxCodeController.kt`

---

### 6. Tax Rule APIs

#### 6.1 Get Tax Rules
```
GET /api/v1/tax/rule?modifiedAfter={timestamp}
```

**Query Parameters**:
- `modifiedAfter` (optional): Timestamp for incremental sync
- `taxCode` (optional): Filter by specific tax code
- `page` (default: 0)
- `size` (default: 1000)

**Controller**: `TaxRuleController.kt`

---

#### 6.2 Get Tax Rules by Tax Code
```
GET /api/v1/tax/rule/tax-code/{taxCodeId}
```

**Controller**: `TaxRuleController.kt`

---

#### 6.3 Get Tax Rule by ID
```
GET /api/v1/tax/rule/{id}
```

**Controller**: `TaxRuleController.kt`

---

#### 6.4 Update Tax Rule
```
PATCH /api/v1/tax/rule/{id}
```

**Request Body**:
```json
{
  "jurisdiction": "MAHARASHTRA",
  "jurisdictionLevel": "STATE",
  "componentComposition": {...},
  "isActive": true
}
```

**Controller**: `TaxRuleController.kt`

---

### 7. Tax Component APIs

#### 7.1 Get Tax Components
```
GET /api/v1/tax/component?modifiedAfter={timestamp}
```

**Query Parameters**:
- `modifiedAfter` (optional): Timestamp for incremental sync
- `taxType` (optional): GST, VAT, SALES_TAX
- `jurisdiction` (optional): Filter by jurisdiction
- `page` (default: 0)
- `size` (default: 1000)

**Example**:
```
GET /api/v1/tax/component?taxType=GST&jurisdiction=MH&page=0&size=100
```

**Controller**: `TaxComponentController.kt`

---

#### 7.2 Get Tax Component by ID
```
GET /api/v1/tax/component/{id}
```

**Controller**: `TaxComponentController.kt`

---

#### 7.3 Search Tax Components
```
GET /api/v1/tax/component/search?taxType={type}&jurisdiction={jurisdiction}
```

**Controller**: `TaxComponentController.kt`

---

## Subscription Flow Architecture

### When a workspace subscribes to a tax code:

1. **User Request**: `POST /api/v1/tax/code/subscribe`
   ```json
   {
     "masterTaxCodeId": "MTC_HSN_1001",
     "customName": "Live Animals"
   }
   ```

2. **TaxCodeService.subscribe()** (`TaxCodeService.kt:31`):
   - Fetches `master_tax_code` by UID
   - Checks if already subscribed (idempotent)
   - Creates workspace `tax_code` record
   - Calls `createDefaultTaxRule()` if `defaultTaxRate` exists

3. **createDefaultTaxRule()** (`TaxCodeService.kt:111`):
   - Looks up `master_tax_rule` by `master_tax_code_id`
   - If template found: calls `createRuleFromMasterTemplate()`
   - If not found: calls `createRuleWithGeneratedComposition()` (fallback)

4. **createRuleFromMasterTemplate()** (`TaxCodeService.kt:129`):
   - Extracts component IDs from `master_tax_rule.component_composition`
   - For each component ID:
     - Fetches `master_tax_component` by UID
     - Creates workspace `tax_component` (if doesn't exist)
   - Creates workspace `tax_rule` with component_composition

5. **Result**:
   - ✅ Workspace `tax_code` created
   - ✅ Workspace `tax_component` records created (CGST, SGST, IGST as needed)
   - ✅ Workspace `tax_rule` created with proper references

---

## Implementation Details

### Controllers (7)
1. **TaxConfigurationController.kt** - Tax configuration management
2. **MasterTaxCodeController.kt** - Master tax code search
3. **MasterTaxComponentController.kt** - Master component CRUD
4. **MasterTaxRuleController.kt** - Master rule CRUD
5. **TaxCodeController.kt** - Workspace subscriptions
6. **TaxRuleController.kt** - Tax rules management
7. **TaxComponentController.kt** - Tax components management

### Services (7)
1. **TaxConfigurationService.kt** - Configuration service
2. **MasterTaxCodeService.kt** - Master code search
3. **MasterTaxComponentService.kt** - Master component service
4. **MasterTaxRuleService.kt** - Master rule service
5. **TaxCodeService.kt** - Subscription service with auto-component creation
6. **TaxRuleService.kt** - Rule service
7. **TaxComponentService.kt** - Component service
8. **GstRuleTemplateService.kt** - GST composition generator (fallback)

### Repositories (7)
1. **TaxConfigurationRepository.kt**
2. **MasterTaxCodeRepository.kt**
3. **MasterTaxComponentRepository.kt**
4. **MasterTaxRuleRepository.kt**
5. **TaxCodeRepository.kt** - With incremental sync support
6. **TaxRuleRepository.kt**
7. **TaxComponentRepository.kt**

### DTOs
- **TaxConfigurationDto** / **TaxConfigurationRequest** / **UpdateTaxConfigurationRequest**
- **MasterTaxCodeDto**
- **MasterTaxComponentDto**
- **MasterTaxRuleDto**
- **TaxCodeDto** (alias for WorkspaceTaxCodeDto)
- **SubscribeTaxCodeRequest** / **UpdateTaxCodeRequest** / **BulkSubscribeTaxCodesRequest**
- **TaxRuleDto** / **UpdateTaxRuleRequest**
- **TaxComponentDto** (alias for WorkspaceTaxComponentDto)
- **ComponentCompositionDto** / **ComponentReferenceDto**

---

## Key Architectural Decisions

### 1. Multi-Tenancy via @TenantId
- Removed explicit `workspaceId` from all URL paths
- Tenant context automatically applied at JPA level via `@TenantId` on `ownerId`
- No need for manual tenant validation in controllers
- Simplified API paths: `/api/v1/tax/*` instead of `/api/v1/workspaces/{workspaceId}/tax/*`

### 2. Master/Workspace Pattern
- **Master tables** (master_tax_code, master_tax_component, master_tax_rule) - Global templates
- **Workspace tables** (tax_code, tax_component, tax_rule) - Tenant-specific instances
- Workspaces subscribe to master templates and create local copies

### 3. Auto-Component Creation
- When workspace subscribes to tax code, both `tax_rule` AND `tax_component` records auto-created
- Uses master templates as source of truth
- Fallback to generated composition if master template missing

### 4. Incremental Sync Support
- All workspace-scoped endpoints support `modifiedAfter` parameter
- Enables offline-first mobile architecture
- Efficient data synchronization via timestamp filtering

### 5. Automatic UID Generation
- Entity UIDs generated via `@PrePersist` hook in `BaseDomain`
- No manual UID generator injection needed

### 6. Instant Timestamps
- All timestamps use `java.time.Instant` (UTC)
- Serializes to ISO-8601 with Z suffix
- Avoids timezone-related bugs

---

## Build Status

### Latest Build
```
> Task :tax:build

BUILD SUCCESSFUL in 1s
7 actionable tasks: 3 executed, 4 up-to-date
```

### Latest Commit
```
6d1cc2b feat(tax): auto-create workspace tax_component records on tax code subscription
```

---

## File Locations

### Migrations
- `/tax/src/main/resources/db/migration/postgresql/V1.0.38__create_tax_module_v2_tables.sql`
- `/tax/src/main/resources/db/migration/mysql/V1.0.38__create_tax_module_v2_tables.sql`

### Entities
- `/tax/src/main/kotlin/com/ampairs/tax/domain/model/MasterTaxCode.kt`
- `/tax/src/main/kotlin/com/ampairs/tax/domain/model/MasterTaxComponent.kt`
- `/tax/src/main/kotlin/com/ampairs/tax/domain/model/MasterTaxRule.kt`
- `/tax/src/main/kotlin/com/ampairs/tax/domain/model/TaxConfiguration.kt`
- `/tax/src/main/kotlin/com/ampairs/tax/domain/model/TaxCode.kt`
- `/tax/src/main/kotlin/com/ampairs/tax/domain/model/TaxRule.kt`
- `/tax/src/main/kotlin/com/ampairs/tax/domain/model/TaxComponent.kt`

### Controllers
- `/tax/src/main/kotlin/com/ampairs/tax/controller/*.kt`

### Services
- `/tax/src/main/kotlin/com/ampairs/tax/service/*.kt`

### Repositories
- `/tax/src/main/kotlin/com/ampairs/tax/repository/*.kt`

### DTOs
- `/tax/src/main/kotlin/com/ampairs/tax/domain/dto/TaxModuleDtos.kt`

---

## Configuration

### Database Connection
Connection strings must include:
```
?serverTimezone=UTC
```

### JPA/Hibernate
- Using `java.time.Instant` for all timestamps
- Auto-mapped to TIMESTAMP columns
- Multi-tenancy via @TenantId on ownerId

### Jackson Serialization
- Global snake_case configuration active
- No @JsonProperty annotations needed
- Instant serializes to ISO-8601 with Z suffix

---

**Last Updated**: 2025-12-14
**Version**: 2.1
**Build Status**: ✅ Successful
