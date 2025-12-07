# Tax Module V2 API Paths - Updated for Multi-Tenancy

## Overview

This document provides the **updated API paths** for the Tax Module V2 implementation. The paths have been modified to align with Spring's multi-tenancy architecture using `@TenantId`, which eliminates the need for explicit `workspaceId` in the URL paths.

## Key Changes from Original Guide

### ❌ OLD Pattern (from guide):
```
/api/v1/workspaces/{workspaceId}/tax/configuration
/api/v1/workspaces/{workspaceId}/tax/codes
```

### ✅ NEW Pattern (implemented):
```
/api/v1/tax/configuration
/api/v1/tax/code
```

**Why?**
- Spring multi-tenancy via `@TenantId` annotation on `ownerId` in `OwnableBaseDomain`
- Tenant context is automatically applied at JPA level
- Cleaner API contracts without redundant workspace parameters
- No need for `X-Workspace-ID` header validation in controllers

---

## 1. Tax Configuration APIs

### 1.1 Get Workspace Tax Configuration

**Endpoint**: `GET /api/v1/tax/configuration`

**Description**: Retrieve workspace-level tax configuration (automatically scoped to current tenant).

**Request Headers**:
```
Authorization: Bearer {jwt_token}
```

**Response**: Same as guide

**Controller**: `TaxConfigurationController.kt`

---

### 1.2 Update Workspace Tax Configuration

**Endpoint**: `PUT /api/v1/tax/configuration`

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

**Response**: Same as guide

**Controller**: `TaxConfigurationController.kt`

---

## 2. Master Tax Code APIs

### 2.1 Search Master Tax Codes

**Endpoint**: `GET /api/v1/tax/master-code/search`

**Description**: Search global tax code registry (no tenant scoping needed - master data).

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

### 2.2 Get Popular Tax Codes

**Endpoint**: `GET /api/v1/tax/master-code/popular`

**Query Parameters**:
- `countryCode` (required)
- `industry` (optional)
- `limit` (default: 20)

**Example**:
```
GET /api/v1/tax/master-code/popular?countryCode=IN&industry=RETAIL_GROCERY&limit=20
```

**Controller**: `MasterTaxCodeController.kt`

---

## 3. Workspace Tax Code APIs (Subscriptions)

### 3.1 Subscribe to Tax Code

**Endpoint**: `POST /api/v1/tax/code/subscribe`

**Description**: Subscribe workspace to a master tax code (automatically scoped to current tenant).

**Request Body**:
```json
{
  "masterTaxCodeId": "HSN_12345678",
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
    "masterTaxCodeId": "HSN_12345678",
    "code": "12345678",
    "codeType": "HSN_CODE",
    "description": "Oil seeds...",
    "shortDescription": "Oil seeds",
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

**Controller**: `TaxCodeController.kt`

---

### 3.2 Get Workspace Tax Codes (Incremental Sync)

**Endpoint**: `GET /api/v1/tax/code`

**Description**: Get all workspace subscribed tax codes (automatically scoped to current tenant).

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

### 3.3 Get Favorite Tax Codes

**Endpoint**: `GET /api/v1/tax/code/favorites`

**Description**: Get favorite tax codes sorted by usage (automatically scoped to current tenant).

**Query Parameters**:
- `page` (default: 0)
- `size` (default: 100)

**Example**:
```
GET /api/v1/tax/code/favorites?page=0&size=20
```

**Controller**: `TaxCodeController.kt`

---

### 3.4 Unsubscribe from Tax Code

**Endpoint**: `DELETE /api/v1/tax/code/{taxCodeId}`

**Description**: Unsubscribe workspace from a tax code (soft delete, automatically scoped to current tenant).

**Response**:
```json
{
  "success": true,
  "data": null
}
```

**Controller**: `TaxCodeController.kt`

---

### 3.5 Update Tax Code Configuration

**Endpoint**: `PATCH /api/v1/tax/code/{taxCodeId}`

**Description**: Update workspace-specific tax code settings (automatically scoped to current tenant).

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

### 3.6 Increment Usage Count

**Endpoint**: `POST /api/v1/tax/code/{taxCodeId}/usage`

**Description**: Increment usage count when tax code is used (automatically scoped to current tenant).

**Request Body**:
```json
{
  "timestamp": 1733270400000
}
```

**Controller**: `TaxCodeController.kt`

---

## 4. Tax Rule APIs

### 4.1 Get Tax Rules

**Endpoint**: `GET /api/v1/tax/rule`

**Description**: Get tax rules for workspace (automatically scoped to current tenant).

**Query Parameters**:
- `modifiedAfter` (optional): Timestamp for incremental sync
- `taxCode` (optional): Filter by specific tax code
- `page` (default: 0)
- `size` (default: 1000)

**Example**:
```
GET /api/v1/tax/rule?modifiedAfter=1733000000000&page=0&size=1000
```

**Controller**: `TaxRuleController.kt`

---

### 4.2 Get Tax Rules by Tax Code

**Endpoint**: `GET /api/v1/tax/rule/tax-code/{taxCodeId}`

**Description**: Get all tax rules for a specific tax code (automatically scoped to current tenant).

**Example**:
```
GET /api/v1/tax/rule/tax-code/TCD_001
```

**Controller**: `TaxRuleController.kt`

---

## 5. Tax Component APIs

### 5.1 Get Tax Components

**Endpoint**: `GET /api/v1/tax/component`

**Description**: Get workspace tax components (CGST, SGST, IGST, etc.) - automatically scoped to current tenant.

**Query Parameters**:
- `modifiedAfter` (optional): Timestamp for incremental sync
- `taxType` (optional): Filter by GST, VAT, SALES_TAX
- `jurisdiction` (optional): Filter by jurisdiction
- `page` (default: 0)
- `size` (default: 1000)

**Example**:
```
GET /api/v1/tax/component?taxType=GST&jurisdiction=MH&page=0&size=100
```

**Controller**: `TaxComponentController.kt`

---

## Implementation Summary

### Created Files:

**Controllers** (5):
- `TaxConfigurationController.kt` - Tax configuration management
- `MasterTaxCodeController.kt` - Master tax code search
- `TaxCodeController.kt` - Workspace tax code subscriptions
- `TaxRuleController.kt` - Tax rules management
- `TaxComponentController.kt` - Tax components management

**Services** (4):
- `TaxConfigurationServiceV2.kt` - Configuration service
- `MasterTaxCodeService.kt` - Master code search service (already existed)
- `TaxCodeService.kt` - Subscription service
- `TaxComponentService.kt` - Component service
- `TaxRuleService.kt` - Rule service (already existed)

**Repositories** (2 new):
- `TaxCodeRepository.kt` - TaxCode repository with incremental sync
- `TaxComponentRepository.kt` - TaxComponent repository

**DTOs** (Updated):
- Added `customName` field to `WorkspaceTaxCodeDto`
- Added `UpdateTaxConfigurationRequest`
- Added `UpdateTaxCodeRequest`
- Type aliases: `TaxCodeDto`, `TaxComponentDto`

### Key Architectural Decisions:

1. **Multi-Tenancy via @TenantId**
   - Removed explicit `workspaceId` from all URL paths
   - Tenant context automatically applied at JPA level
   - No need for manual tenant validation in controllers

2. **Simplified API Paths**
   - `/api/v1/tax/*` instead of `/api/v1/workspaces/{workspaceId}/tax/*`
   - Cleaner, more RESTful design
   - Reduced complexity in API consumers

3. **Automatic UID Generation**
   - Entity UIDs generated automatically via `@PrePersist` hook in `BaseDomain`
   - No manual UID generator injection needed in services

4. **Incremental Sync Support**
   - All workspace-scoped endpoints support `modifiedAfter` parameter
   - Enables offline-first mobile architecture
   - Efficient data synchronization

---

## Migration Notes for Mobile App

### Required Changes:

1. **Remove Workspace ID from Paths and Use Singular Resource Names**
   ```kotlin
   // OLD
   val url = "/api/v1/workspaces/$workspaceId/tax/codes"

   // NEW (singular naming)
   val url = "/api/v1/tax/code"
   ```

2. **Remove X-Workspace-ID Header**
   - No longer needed as tenant context is in JWT token
   - Backend handles tenant scoping automatically

3. **Update DTO Field Names**
   - `WorkspaceTaxCodeDto` now includes `customName` field
   - All DTOs use `Long` timestamps (epoch milliseconds)

4. **Sync Endpoints**
   - All incremental sync endpoints work the same
   - Use `modifiedAfter` parameter as before

---

## Testing Checklist

- [ ] Tax configuration GET/PUT endpoints
- [ ] Master tax code search and popular codes
- [ ] Tax code subscription and unsubscription
- [ ] Tax code favorites and usage tracking
- [ ] Tax rules retrieval with filters
- [ ] Tax components retrieval with filters
- [ ] Incremental sync for all endpoints
- [ ] Multi-tenant isolation (tenant A cannot access tenant B data)

---

**Last Updated**: 2025-01-09
**Version**: 2.0
**Build Status**: ✅ Successful
