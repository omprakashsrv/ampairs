# Business Module

## Overview
The Business module manages the workspace-level business profile that powers invoicing, taxation, and document branding across Ampairs. It stores legal metadata, contact channels, registered addresses, working hours, and regulatory identifiers for each workspace. The module enforces the invariant that a workspace may own only one business profile, and it guarantees multi-tenant isolation by resolving the workspace ID from the request context.

## Architecture
### Package Structure
```
com.ampairs.business/
├── controller/            # REST endpoints exposed under /api/v1/business
├── exception/             # Domain-specific exceptions and advice
├── model/                 # Business entity, DTOs, enums, and extensions
│   ├── dto/               # Create/update requests, responses, and mappers
│   └── enums/             # Business type declarations
├── repository/            # Spring Data repository interfaces
└── service/               # Business profile orchestration logic
```

## Key Components
- **`BusinessController`** – Exposes CRUD APIs for the current workspace, plus helper endpoints for existence checks, formatted addresses, and business-hours availability.
- **`BusinessService`** – Orchestrates validation, enforces one-profile-per-workspace, resolves the active tenant via `TenantContextHolder`, and surfaces helper operations (hours, address formatting).
- **`BusinessRepository`** – Provides workspace-scoped lookups (`findByOwnerId`, `existsByOwnerId`) and UID retrieval helpers.
- **`Business` entity** – Extends core domain primitives, tracks contact info, tax registration details, logo URLs, working hours, and audit metadata.
- **Exception classes (`BusinessNotFoundException`, `BusinessAlreadyExistsException`, `InvalidBusinessDataException`)** – Map validation and lookup failures into API-friendly responses handled by `BusinessExceptionHandler`.

## Business Rules & Features
- **Single profile per workspace** enforced by repository checks and database uniqueness constraints.
- **Multi-tenancy aware** operations powered by `TenantContextHolder` and security context lookups for auditing the actor.
- **Business-hour validation** ensures closing time is later than opening time and supports per-day schedules via the entity helpers.
- **Address helpers** compose full address strings and provide formatting for downstream modules (invoice headers, tax documents).
- **Optimistic updates** reuse the entity `applyUpdate` extension to perform partial updates without losing unspecified fields.

## API Highlights
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/business` | `GET` | Retrieve the current workspace's business profile. |
| `/api/v1/business` | `POST` | Create the first business profile for a workspace; returns 409 if one already exists. |
| `/api/v1/business` | `PUT` | Update mutable fields on the workspace business profile. |
| `/api/v1/business/exists` | `GET` | Lightweight existence check used by onboarding flows. |
| `/api/v1/business/address` | `GET` | Returns the formatted postal address for display/printing. |
| `/api/v1/business/hours/{day}` | `GET` | Reports whether the business operates on a given weekday. |

All responses are wrapped in the shared `ApiResponse<T>` envelope from the `core` module.

## Integration Points
- Depends on `core` for base entities, API responses, and multi-tenancy helpers.
- Uses `workspace` conventions for workspace IDs (sourced from the `X-Workspace-ID` header) and inherits role checks via Spring Security.
- Supplies business metadata to the `invoice` and `tax` modules for document generation and GST calculations.
- Publishes exceptions that bubble through the shared global handler, so endpoints remain consistent with the rest of the API surface.

## Build & Test
```bash
# From ampairs-backend/
./gradlew :business:build
./gradlew :business:test
```

Run `./gradlew :ampairs_service:bootRun` to exercise the module end-to-end through the aggregated application.

## Database Schema

### Table: `businesses`
The business profile table with multi-tenant isolation:

```sql
CREATE TABLE businesses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uid VARCHAR(200) UNIQUE NOT NULL,
    owner_id VARCHAR(200) NOT NULL,  -- Workspace ID with @TenantId

    -- Profile Information
    name VARCHAR(255) NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    description TEXT,
    owner_name VARCHAR(255),

    -- Address fields
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),

    -- GPS coordinates
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),

    -- Contact information
    phone VARCHAR(20),
    email VARCHAR(255),
    website VARCHAR(500),

    -- Tax/Regulatory
    tax_id VARCHAR(50),
    registration_number VARCHAR(100),
    tax_settings JSON,

    -- Operational configuration
    timezone VARCHAR(50) NOT NULL DEFAULT 'UTC',
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    date_format VARCHAR(20) NOT NULL DEFAULT 'DD-MM-YYYY',
    time_format VARCHAR(10) NOT NULL DEFAULT '12H',

    -- Business hours
    opening_hours VARCHAR(5),
    closing_hours VARCHAR(5),
    operating_days JSON NOT NULL,

    -- Status and audit
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_updated BIGINT NOT NULL,

    CONSTRAINT fk_business_owner FOREIGN KEY (owner_id)
        REFERENCES workspaces(uid) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_business_type ON businesses(business_type);
CREATE INDEX idx_business_active ON businesses(active);
CREATE INDEX idx_business_country ON businesses(country);
CREATE INDEX idx_business_created_at ON businesses(created_at);
```

## Usage Examples

### Creating a Business Profile
```bash
curl -X POST http://localhost:8080/api/v1/business \
  -H "X-Workspace-ID: ws_abc123" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tech Solutions Pvt Ltd",
    "business_type": "RETAIL",
    "email": "contact@techsolutions.com",
    "phone": "+911234567890",
    "timezone": "Asia/Kolkata",
    "currency": "INR",
    "address_line1": "123 Main Street",
    "city": "Mumbai",
    "state": "Maharashtra",
    "postal_code": "400001",
    "country": "India"
  }'
```

### Getting Business Profile
```bash
curl -X GET http://localhost:8080/api/v1/business \
  -H "X-Workspace-ID: ws_abc123"
```

### Updating Business Profile
```bash
curl -X PUT http://localhost:8080/api/v1/business \
  -H "X-Workspace-ID: ws_abc123" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+919876543210",
    "opening_hours": "09:00",
    "closing_hours": "18:00",
    "operating_days": ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"]
  }'
```

## Business Types
Available business types in `BusinessType` enum:
- `RETAIL` - Business to Consumer
- `WHOLESALE` - Business to Business
- `MANUFACTURING` - Manufacturing/Production
- `SERVICES` - Professional Services
- `RESTAURANT` - Food & Beverage
- `ECOMMERCE` - Online Business
- `HOSPITALITY` - Hotels/Lodging
- `HEALTHCARE` - Medical Services
- `EDUCATION` - Educational Institutions
- `LOGISTICS` - Transportation/Delivery
- `CONSTRUCTION` - Building/Infrastructure
- `OTHER` - Other business types

## Multi-Tenancy Design

The Business module uses `OwnableBaseDomain` which provides:
- `ownerId` field with `@TenantId` annotation for automatic workspace-based filtering
- Inherited `createdAt`, `updatedAt`, `lastUpdated` timestamps (all use `Instant` for UTC)
- Automatic tenant context resolution via `TenantContextHolder`

**Key Point**: The `ownerId` field serves as the workspace identifier. All queries are automatically filtered by the current tenant context.

## Timezone Handling

All timestamps use `java.time.Instant` (UTC):
- ✅ No timezone ambiguity
- ✅ Serializes as ISO-8601 with Z suffix: `"2025-10-10T14:30:00Z"`
- ✅ Frontend can convert to local timezone for display
- ✅ The `timezone` field in business profile is for display/business hours, not data storage

## Implementation Status

✅ **Completed**:
- Entity model with all fields
- Repository with multi-tenant queries
- Service layer with business logic
- Controller with REST endpoints
- Exception handling (404, 409, 400)
- DTOs with validation
- Extension functions for mapping
- Database migrations
- Integration with main application
- Basic integration tests
- Documentation

📋 **Pending**:
- Comprehensive unit tests for service layer
- Additional repository integration tests
- Performance testing
- Logo/image upload support
- Business verification workflow
