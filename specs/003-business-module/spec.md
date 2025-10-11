# Feature Specification: Business Module

**Feature ID**: 003
**Branch**: `003-business-module`
**Date**: 2025-10-10
**Status**: Planning

## Overview

Extract business-specific configuration and details from the Workspace entity into a dedicated Business module. The Workspace entity has grown too large with mixed concerns - it currently contains both tenant management data AND business profile/configuration data. This refactoring will separate concerns and make business configuration more maintainable and extensible.

## Problem Statement

Currently, the `Workspace` entity contains:
- Multi-tenant workspace management fields (name, slug, subscription, members)
- Business profile fields (address, phone, email, website)
- Tax/regulatory fields (taxId, registrationNumber)
- Operating configuration (timezone, currency, business hours)
- Theme/UI settings (which are already in WorkspaceSettings)

**Issues with current design**:
1. **Bloated entity**: 40+ fields mixing different concerns
2. **Hard to extend**: Adding new business configurations requires modifying core Workspace entity
3. **Poor separation of concerns**: Workspace should manage tenancy, not business details
4. **Difficult testing**: Changes to business logic affect workspace tests
5. **Future scalability**: Multi-location businesses would need complex workarounds

## Goals

1. **Separation of Concerns**: Move business-specific data out of Workspace
2. **Maintainability**: Create a focused Business entity for profile and configuration
3. **Extensibility**: Enable easy addition of new business attributes without affecting core tenancy
4. **API Clarity**: Clear endpoints for business management vs workspace management
5. **Multi-location Support**: Foundation for future multi-location business support

## User Stories

### US-1: View Business Profile
**As a** workspace owner
**I want to** view my business profile details
**So that** I can verify my business information is correct

**Acceptance Criteria**:
- Display business name, type, and description
- Show complete address details (line1, line2, city, state, postal code, country)
- Display contact information (phone, email, website)
- Show tax/regulatory details (GST/tax ID, registration number)
- Display location coordinates if available

### US-2: Update Business Profile
**As a** workspace owner or admin
**I want to** update my business profile
**So that** I can keep my business information current

**Acceptance Criteria**:
- Edit all business profile fields
- Validate required fields (name, address, tax ID if applicable)
- Track modification history (who/when)
- Return updated business profile

### US-3: Configure Business Operations
**As a** workspace owner
**I want to** configure my business operating parameters
**So that** the system reflects my business rules

**Acceptance Criteria**:
- Set opening hours (start/end times)
- Configure operating days of the week
- Set timezone for business operations
- Configure currency and locale preferences
- Set date/time format preferences

### US-4: Manage Tax Configuration
**As a** business owner
**I want to** configure tax and regulatory details
**So that** I can comply with local regulations

**Acceptance Criteria**:
- Store GST/VAT/tax identification numbers
- Store business registration number
- Configure tax settings by region
- Link to tax codes and rates

### US-5: API Integration for Business Data
**As a** developer
**I want to** access business data via clean APIs
**So that** I can integrate business information into frontend/mobile apps

**Acceptance Criteria**:
- GET /api/v1/business - Retrieve current business profile
- PUT /api/v1/business - Update business profile
- Use standard ApiResponse wrapper
- Include proper error handling and validation

## Functional Requirements

### FR-1: Business Entity
Create a new `Business` entity with:
- **Profile Fields**: name, type (enum), description, owner name
- **Address Fields**: addressLine1, addressLine2, city, state, postalCode, country
- **Location**: latitude, longitude (for future location-based features)
- **Contact Fields**: phone, email, website
- **Tax/Regulatory**: taxId, registrationNumber, taxSettings (JSON)
- **Operating Config**: timezone, currency, language
- **Hours**: openingHours, closingHours, operatingDays (JSON array)
- **Formatting**: dateFormat, timeFormat
- **Metadata**: workspaceId (foreign key), createdBy, updatedBy, timestamps

### FR-2: Data Migration
- Migrate existing workspace business fields to Business table
- One-to-one relationship: Workspace → Business
- Maintain data integrity during migration
- Preserve audit trail

### FR-3: API Endpoints
Create REST API at `/api/v1/business`:
- `GET /api/v1/business` - Get current workspace's business profile
- `PUT /api/v1/business` - Update business profile
- `POST /api/v1/business` - Create business profile (during workspace setup)
- All responses wrapped in `ApiResponse<T>`

### FR-4: Repository Layer
- `BusinessRepository` with @TenantId support for automatic workspace filtering
- Standard CRUD methods
- Custom queries for business lookup by workspace

### FR-5: Service Layer
- `BusinessService` with validation logic
- Business rule enforcement (required fields, format validation)
- Modification tracking
- Integration with workspace context

### FR-6: Controller Layer
- `BusinessController` with workspace-aware endpoints
- DTO pattern: separate request/response DTOs
- Input validation using Jakarta validation annotations
- Proper HTTP status codes

## Non-Functional Requirements

### NFR-1: Performance
- Business profile queries < 50ms p95
- Support 10,000+ businesses per database
- Efficient indexing on workspaceId and active status

### NFR-2: Multi-Tenancy
- Automatic workspace filtering using @TenantId annotation
- Tenant context from X-Workspace-ID header
- No cross-tenant data leakage

### NFR-3: Data Validation
- Required field validation (name, workspaceId)
- Email format validation
- Phone number format validation
- URL format validation for website
- Tax ID format validation based on country

### NFR-4: Backward Compatibility
- Existing Workspace API continues to work during migration
- Gradual deprecation of workspace business fields
- Support both old and new endpoints during transition period

### NFR-5: Security
- Only workspace owners/admins can modify business profile
- Workspace members with VIEW permission can read
- Audit trail for all modifications

## Technical Constraints

1. **Technology Stack**:
   - Backend: Spring Boot + Kotlin
   - Database: PostgreSQL with JPA/Hibernate
   - Architecture: Multi-module Gradle project

2. **Existing Patterns**:
   - All entities extend `OwnableBaseDomain`
   - DTOs for all API inputs/outputs (no entity exposure)
   - `ApiResponse<T>` wrapper for all endpoints
   - @TenantId for automatic workspace filtering
   - @EntityGraph for efficient relationship loading

3. **Database Constraints**:
   - Must maintain referential integrity
   - Add foreign key: business.workspace_id → workspaces.uid
   - Create indexes for performance

4. **Module Structure**:
   - Create new module: `ampairs-backend/business`
   - Follow existing module pattern (model, dto, repository, service, controller)

## Success Criteria

1. **Functional Success**:
   - Business profile can be created, read, and updated via API
   - All existing workspace business data migrated successfully
   - Frontend can retrieve business profile independently

2. **Code Quality**:
   - All code follows project conventions (CLAUDE.md)
   - No @JsonProperty annotations (global snake_case config)
   - Proper DTO pattern implementation
   - @EntityGraph usage for efficient queries

3. **Testing**:
   - Unit tests for service layer
   - Integration tests for repository layer
   - API tests for controller endpoints
   - Migration script tested with sample data

4. **Documentation**:
   - API documentation updated
   - Migration guide for developers
   - Database schema documented

## Out of Scope

1. Multi-location support (future feature)
2. Business categories/industries taxonomy
3. Business logo upload (separate feature)
4. Business hours scheduling system
5. Holiday calendar management
6. Business verification/KYC process

## Dependencies

1. **Existing Modules**:
   - `core`: For BaseDomain, ApiResponse, Constants
   - `workspace`: For workspace context and relationships

2. **Database**:
   - PostgreSQL with JSON support
   - Flyway for migrations

3. **Frontend/Mobile**:
   - Will need to consume new business APIs
   - Separate ticket for UI implementation

## Migration Strategy

### Phase 1: Create New Module
1. Create `business` module with entity, DTOs, repository, service, controller
2. Deploy with endpoints but don't enforce usage yet

### Phase 2: Data Migration
1. Create Flyway migration script to:
   - Create `businesses` table
   - Copy data from `workspaces` to `businesses`
   - Add foreign key relationship
   - Create indexes

### Phase 3: Update Workspace Entity
1. Mark workspace business fields as @Deprecated
2. Add @OneToOne relationship to Business
3. Maintain backward compatibility in Workspace APIs

### Phase 4: Frontend Integration
1. Update frontend to use new business endpoints
2. Display business profile separately from workspace settings

### Phase 5: Cleanup (Future)
1. Remove deprecated fields from Workspace entity
2. Drop old columns from database
3. Remove old API endpoints

## Risk Analysis

| Risk | Impact | Mitigation |
|------|--------|------------|
| Data loss during migration | HIGH | Thorough testing, backup before migration, rollback plan |
| Breaking existing APIs | MEDIUM | Maintain backward compatibility, gradual deprecation |
| Performance degradation | LOW | Proper indexing, @EntityGraph optimization |
| Frontend integration delays | MEDIUM | Parallel development, clear API contracts |

## Timeline Estimate

- **Planning & Design**: 1 day (current phase)
- **Backend Implementation**: 2-3 days
  - Module setup: 4 hours
  - Entity & DTOs: 4 hours
  - Repository & Service: 4 hours
  - Controller & Tests: 4 hours
  - Migration script: 4 hours
- **Testing & QA**: 1 day
- **Documentation**: 4 hours
- **Total**: 4-5 days

## References

- **Project Guidelines**: `/CLAUDE.md`
- **Existing Workspace Entity**: `ampairs-backend/workspace/src/main/kotlin/com/ampairs/workspace/model/Workspace.kt`
- **Existing Settings Entity**: `ampairs-backend/workspace/src/main/kotlin/com/ampairs/workspace/model/WorkspaceSettings.kt`
- **Architecture Pattern**: Customer, Product, Order, Invoice modules

## Questions & Clarifications

1. Should business profile be mandatory for workspace creation?
2. Should we support multiple businesses per workspace (future)?
3. What's the priority for mobile app integration?
4. Should business hours support per-day configuration or simple open/close?
5. Do we need business type categories/taxonomy or free text?

---
**Status**: Ready for /plan command execution
