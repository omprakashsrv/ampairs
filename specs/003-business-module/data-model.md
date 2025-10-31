# Data Model: Business Module

**Feature**: 003-business-module
**Date**: 2025-10-10

## Entity: Business

### Purpose
Stores business profile and configuration data for a workspace. Separates business concerns from tenant management (Workspace entity).

### Table: `businesses`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| **uid** | VARCHAR(36) | PRIMARY KEY | Unique business identifier (inherited from BaseDomain) |
| **seq_id** | VARCHAR(50) | UNIQUE, NOT NULL | Sequential ID with prefix "BUS" |
| **workspace_id** | VARCHAR(36) | UNIQUE, NOT NULL, FK | Reference to workspace (tenant discriminator) |
| **name** | VARCHAR(255) | NOT NULL | Business/company name |
| **business_type** | VARCHAR(50) | NOT NULL | Type of business (RETAIL, WHOLESALE, etc.) |
| **description** | TEXT | NULL | Business description |
| **owner_name** | VARCHAR(255) | NULL | Business owner's name |
| **address_line1** | VARCHAR(255) | NULL | Primary address line |
| **address_line2** | VARCHAR(255) | NULL | Secondary address line |
| **city** | VARCHAR(100) | NULL | City name |
| **state** | VARCHAR(100) | NULL | State/province |
| **postal_code** | VARCHAR(20) | NULL | Postal/ZIP code |
| **country** | VARCHAR(100) | NULL | Country name |
| **latitude** | DECIMAL(10, 7) | NULL | GPS latitude (future: location services) |
| **longitude** | DECIMAL(10, 7) | NULL | GPS longitude (future: location services) |
| **phone** | VARCHAR(20) | NULL | Primary phone number |
| **email** | VARCHAR(255) | NULL | Primary email address |
| **website** | VARCHAR(500) | NULL | Business website URL |
| **tax_id** | VARCHAR(50) | NULL | Tax identification number (GST/VAT) |
| **registration_number** | VARCHAR(100) | NULL | Business registration number |
| **tax_settings** | JSON | NULL | Tax-related settings by region |
| **timezone** | VARCHAR(50) | NOT NULL DEFAULT 'UTC' | Business timezone (IANA format) |
| **currency** | VARCHAR(3) | NOT NULL DEFAULT 'INR' | Currency code (ISO 4217) |
| **language** | VARCHAR(10) | NOT NULL DEFAULT 'en' | Language code (ISO 639-1) |
| **date_format** | VARCHAR(20) | NOT NULL DEFAULT 'DD-MM-YYYY' | Date display format |
| **time_format** | VARCHAR(10) | NOT NULL DEFAULT '12H' | Time display format (12H/24H) |
| **opening_hours** | VARCHAR(5) | NULL | Opening time (HH:MM format) |
| **closing_hours** | VARCHAR(5) | NULL | Closing time (HH:MM format) |
| **operating_days** | JSON | NOT NULL DEFAULT '[]' | Array of operating days |
| **active** | BOOLEAN | NOT NULL DEFAULT true | Whether business is active |
| **created_at** | TIMESTAMP | NOT NULL | Creation timestamp |
| **updated_at** | TIMESTAMP | NOT NULL | Last update timestamp |
| **created_by** | VARCHAR(36) | NULL | User ID who created |
| **updated_by** | VARCHAR(36) | NULL | User ID who last updated |

### Indexes

```sql
CREATE UNIQUE INDEX idx_business_workspace ON businesses(workspace_id);
CREATE INDEX idx_business_type ON businesses(business_type);
CREATE INDEX idx_business_active ON businesses(active) WHERE active = true;
CREATE INDEX idx_business_country ON businesses(country);
CREATE INDEX idx_business_created_at ON businesses(created_at);
```

### Foreign Keys

```sql
ALTER TABLE businesses
    ADD CONSTRAINT fk_business_workspace
    FOREIGN KEY (workspace_id)
    REFERENCES workspaces(uid)
    ON DELETE CASCADE;
```

## Enum: BusinessType

Values representing different types of business operations.

| Value | Description |
|-------|-------------|
| RETAIL | Retail business (B2C) |
| WHOLESALE | Wholesale business (B2B) |
| MANUFACTURING | Manufacturing/production |
| SERVICE | Service-based business |
| RESTAURANT | Restaurant/food service |
| ECOMMERCE | Online e-commerce |
| HEALTHCARE | Healthcare services |
| EDUCATION | Educational institution |
| REAL_ESTATE | Real estate |
| LOGISTICS | Logistics/transport |
| OTHER | Other business type |

## Relationships

### Business → Workspace (Many-to-One)
- **Relationship**: `Business.workspaceId` → `Workspace.uid`
- **Cardinality**: N:1 (many businesses can belong to one workspace in future, currently 1:1)
- **Cascade**: ON DELETE CASCADE
- **Fetch**: LAZY (only load workspace when explicitly needed)

### Future Relationships

#### Business → BusinessLocation (One-to-Many)
*Future implementation for multi-location support*
- One business can have multiple physical locations
- Each location inherits business defaults but can override settings

#### Business → TaxConfiguration (One-to-Many)
*Future implementation for advanced tax management*
- Different tax configurations per region/state
- Complex tax rules for interstate/international commerce

## Validation Rules

### Field-Level Validation

| Field | Rules |
|-------|-------|
| name | NOT NULL, 2-255 characters |
| businessType | NOT NULL, must be valid enum value |
| email | Valid email format if provided |
| phone | Valid phone format (+[country][number]) if provided |
| website | Valid URL format if provided |
| currency | 3-letter ISO 4217 code |
| timezone | Valid IANA timezone identifier |
| latitude | -90 to 90 |
| longitude | -180 to 180 |
| opening_hours | HH:MM format (00:00-23:59) |
| closing_hours | HH:MM format (00:00-23:59) |
| operating_days | JSON array of day names |

### Business Rules

1. **Unique workspace**: One business profile per workspace (enforced by unique constraint on workspace_id)
2. **Required fields**: name, businessType, workspaceId, timezone, currency
3. **Email uniqueness**: Optional, but should be unique per workspace if provided
4. **Hours validation**: If opening_hours provided, closing_hours must also be provided
5. **Operating days**: Must be valid day names (Monday-Sunday)

## JSON Fields

### tax_settings (JSON)

Example structure:
```json
{
  "default_tax_code": "GST-18",
  "interstate_tax": "IGST",
  "intrastate_tax": "SGST+CGST",
  "exemption_certificate": "EXM123456",
  "tax_registration_date": "2020-01-15"
}
```

### operating_days (JSON)

Example structure:
```json
["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
```

## State Transitions

### active (Boolean)

```
┌─────────┐
│  true   │ ← Initial state (default)
│ (Active)│
└────┬────┘
     │
     │ deactivate()
     ▼
┌─────────┐
│  false  │
│(Inactive)│
└────┬────┘
     │
     │ activate()
     ▼
┌─────────┐
│  true   │
│ (Active)│
└─────────┘
```

**Business Rules**:
- Only active businesses appear in default queries
- Inactive businesses retained for audit/history
- Reactivation possible by workspace owner/admin

## Migration Impact

### Data Migration from Workspace

Fields migrated from `workspaces` table:

| Workspace Field | Business Field | Transformation |
|----------------|----------------|----------------|
| name | name | Direct copy |
| workspace_type | business_type | Enum mapping |
| address_line1 | address_line1 | Direct copy |
| address_line2 | address_line2 | Direct copy |
| city | city | Direct copy |
| state | state | Direct copy |
| postal_code | postal_code | Direct copy |
| country | country | Direct copy |
| phone | phone | Direct copy |
| email | email | Direct copy |
| website | website | Direct copy |
| tax_id | tax_id | Direct copy |
| registration_number | registration_number | Direct copy |
| timezone | timezone | Direct copy |
| currency | currency | Direct copy |
| language | language | Direct copy |
| date_format | date_format | Direct copy |
| time_format | time_format | Direct copy |
| business_hours_start | opening_hours | Direct copy |
| business_hours_end | closing_hours | Direct copy |
| working_days | operating_days | JSON parse/transform |

### Fields Remaining in Workspace

These fields stay in `workspaces` (tenant management concerns):
- slug (URL identifier)
- subscription_plan
- max_users
- storage_limit_gb
- storage_used_gb
- status (workspace status vs business active)
- avatar_url (workspace avatar vs business logo)
- settings (workspace-level settings vs business config)
- features (feature flags)

## API Data Transfer Objects

### BusinessResponse

```kotlin
data class BusinessResponse(
    val uid: String,
    val seqId: String,
    val name: String,
    val businessType: String,
    val description: String?,
    val ownerName: String?,
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String?,
    val latitude: Double?,
    val longitude: Double?,
    val phone: String?,
    val email: String?,
    val website: String?,
    val taxId: String?,
    val registrationNumber: String?,
    val timezone: String,
    val currency: String,
    val language: String,
    val dateFormat: String,
    val timeFormat: String,
    val openingHours: String?,
    val closingHours: String?,
    val operatingDays: List<String>,
    val active: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)
```

### BusinessCreateRequest

```kotlin
data class BusinessCreateRequest(
    @field:NotBlank(message = "Business name is required")
    @field:Size(min = 2, max = 255)
    val name: String,

    @field:NotNull(message = "Business type is required")
    val businessType: BusinessType,

    @field:Size(max = 1000)
    val description: String? = null,

    @field:Size(max = 255)
    val ownerName: String? = null,

    // Address fields
    @field:Size(max = 255)
    val addressLine1: String? = null,

    @field:Size(max = 255)
    val addressLine2: String? = null,

    @field:Size(max = 100)
    val city: String? = null,

    @field:Size(max = 100)
    val state: String? = null,

    @field:Size(max = 20)
    val postalCode: String? = null,

    @field:Size(max = 100)
    val country: String? = null,

    // Contact fields
    @field:Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    val phone: String? = null,

    @field:Email(message = "Invalid email format")
    @field:Size(max = 255)
    val email: String? = null,

    @field:Size(max = 500)
    val website: String? = null,

    // Tax fields
    @field:Size(max = 50)
    val taxId: String? = null,

    @field:Size(max = 100)
    val registrationNumber: String? = null,

    // Configuration
    @field:Size(min = 3, max = 3, message = "Currency must be 3-letter ISO code")
    val currency: String = "INR",

    @field:Size(max = 50)
    val timezone: String = "UTC",

    @field:Size(max = 10)
    val language: String = "en",

    val openingHours: String? = null,
    val closingHours: String? = null,
    val operatingDays: List<String> = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
)
```

### BusinessUpdateRequest

```kotlin
data class BusinessUpdateRequest(
    val name: String? = null,
    val businessType: BusinessType? = null,
    val description: String? = null,
    val ownerName: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val taxId: String? = null,
    val registrationNumber: String? = null,
    val timezone: String? = null,
    val currency: String? = null,
    val language: String? = null,
    val dateFormat: String? = null,
    val timeFormat: String? = null,
    val openingHours: String? = null,
    val closingHours: String? = null,
    val operatingDays: List<String>? = null,
    val active: Boolean? = null
)
```

## Database Diagram

```
┌─────────────────────────────────┐
│         workspaces              │
├─────────────────────────────────┤
│ uid (PK)                        │◄──┐
│ name                            │   │
│ slug (UNIQUE)                   │   │
│ subscription_plan               │   │
│ max_users                       │   │
│ status                          │   │
│ ...                             │   │
└─────────────────────────────────┘   │
                                      │ 1:1
                                      │
┌─────────────────────────────────┐   │
│         businesses              │   │
├─────────────────────────────────┤   │
│ uid (PK)                        │   │
│ workspace_id (FK, UNIQUE)       │───┘
│ name                            │
│ business_type                   │
│ address_line1                   │
│ city, state, country            │
│ phone, email, website           │
│ tax_id, registration_number     │
│ timezone, currency, language    │
│ opening_hours, closing_hours    │
│ operating_days (JSON)           │
│ tax_settings (JSON)             │
│ active                          │
│ created_at, updated_at          │
│ created_by, updated_by          │
└─────────────────────────────────┘
```

## Performance Considerations

1. **Primary Query Pattern**: Lookup by workspace_id (unique index)
2. **Expected Query Frequency**: Medium (typically once per page load)
3. **Data Volume**: 1 record per workspace (~10K-100K records)
4. **Read:Write Ratio**: High (95:5 - profiles updated infrequently)
5. **Caching Strategy**: Consider Redis caching for frequently accessed businesses

## Security Considerations

1. **Multi-tenancy**: @TenantId annotation ensures automatic workspace filtering
2. **Authorization**: Only workspace owners/admins can modify
3. **Data Privacy**: Business details visible only to workspace members
4. **Audit Trail**: created_by, updated_by, created_at, updated_at for accountability
5. **Sensitive Data**: Tax IDs, registration numbers - ensure GDPR/data protection compliance

---

**Status**: Data model complete and ready for contract generation
