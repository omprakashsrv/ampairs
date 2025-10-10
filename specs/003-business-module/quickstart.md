# Quickstart: Business Module

**Feature**: 003-business-module
**Purpose**: Quick validation guide for business module functionality

## Prerequisites

- Ampairs backend running locally (port 8080)
- Valid JWT token for authentication
- At least one workspace created
- HTTP client (curl, Postman, or HTTPie)

## Environment Setup

```bash
# Set environment variables
export API_BASE_URL="http://localhost:8080/api/v1"
export JWT_TOKEN="your-jwt-token-here"
export WORKSPACE_ID="your-workspace-uid"
```

## Test Scenario 1: Create Business Profile

### Step 1.1: Create a new business profile

**Request**:
```bash
curl -X POST "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-Workspace-ID: ${WORKSPACE_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Acme Corporation",
    "business_type": "RETAIL",
    "description": "Leading electronics retail business",
    "owner_name": "John Doe",
    "address_line1": "123 Main Street",
    "address_line2": "Suite 100",
    "city": "Mumbai",
    "state": "Maharashtra",
    "postal_code": "400001",
    "country": "India",
    "phone": "+91-22-12345678",
    "email": "info@acme.com",
    "website": "https://www.acme.com",
    "tax_id": "27AABCU9603R1ZM",
    "registration_number": "U12345MH2020PTC123456",
    "timezone": "Asia/Kolkata",
    "currency": "INR",
    "language": "en",
    "opening_hours": "09:00",
    "closing_hours": "18:00",
    "operating_days": ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
  }'
```

**Expected Response** (201 Created):
```json
{
  "success": true,
  "data": {
    "uid": "bus_abc123def456",
    "seq_id": "BUS-0001",
    "name": "Acme Corporation",
    "business_type": "RETAIL",
    "description": "Leading electronics retail business",
    "owner_name": "John Doe",
    "address_line1": "123 Main Street",
    "address_line2": "Suite 100",
    "city": "Mumbai",
    "state": "Maharashtra",
    "postal_code": "400001",
    "country": "India",
    "latitude": null,
    "longitude": null,
    "phone": "+91-22-12345678",
    "email": "info@acme.com",
    "website": "https://www.acme.com",
    "tax_id": "27AABCU9603R1ZM",
    "registration_number": "U12345MH2020PTC123456",
    "timezone": "Asia/Kolkata",
    "currency": "INR",
    "language": "en",
    "date_format": "DD-MM-YYYY",
    "time_format": "12H",
    "opening_hours": "09:00",
    "closing_hours": "18:00",
    "operating_days": ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"],
    "active": true,
    "created_at": "2025-10-10T10:00:00Z",
    "updated_at": "2025-10-10T10:00:00Z"
  },
  "timestamp": "2025-10-10T10:00:00Z",
  "path": "/api/v1/business"
}
```

**Validation**:
- ✅ Status code is 201
- ✅ Response has success: true
- ✅ data.uid starts with "bus_"
- ✅ data.seq_id starts with "BUS-"
- ✅ data.name matches request
- ✅ data.business_type is "RETAIL"
- ✅ Timestamps are ISO 8601 format

### Step 1.2: Verify business creation failed for duplicate

**Request**:
```bash
# Try to create business again for same workspace
curl -X POST "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-Workspace-ID: ${WORKSPACE_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Another Business",
    "business_type": "WHOLESALE"
  }'
```

**Expected Response** (409 Conflict):
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "BUSINESS_ALREADY_EXISTS",
    "message": "Business profile already exists for this workspace",
    "details": null
  },
  "timestamp": "2025-10-10T10:01:00Z",
  "path": "/api/v1/business",
  "trace_id": "xyz-789"
}
```

**Validation**:
- ✅ Status code is 409
- ✅ success is false
- ✅ error.code is "BUSINESS_ALREADY_EXISTS"

## Test Scenario 2: Retrieve Business Profile

### Step 2.1: Get business profile

**Request**:
```bash
curl -X GET "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-Workspace-ID: ${WORKSPACE_ID}"
```

**Expected Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "uid": "bus_abc123def456",
    "seq_id": "BUS-0001",
    "name": "Acme Corporation",
    "business_type": "RETAIL",
    // ... full business details
  },
  "timestamp": "2025-10-10T10:02:00Z",
  "path": "/api/v1/business"
}
```

**Validation**:
- ✅ Status code is 200
- ✅ success is true
- ✅ data contains all business fields
- ✅ Response time < 50ms (performance check)

### Step 2.2: Verify tenant isolation

**Request**:
```bash
# Try to get business with different workspace ID
curl -X GET "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-Workspace-ID: different-workspace-id"
```

**Expected Response** (404 Not Found):
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "BUSINESS_NOT_FOUND",
    "message": "Business profile not found for workspace",
    "details": null
  },
  "timestamp": "2025-10-10T10:03:00Z",
  "path": "/api/v1/business"
}
```

**Validation**:
- ✅ Status code is 404
- ✅ Cannot access other workspace's business (multi-tenancy works)

## Test Scenario 3: Update Business Profile

### Step 3.1: Update business contact information

**Request**:
```bash
curl -X PUT "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-Workspace-ID: ${WORKSPACE_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "phone": "+91-22-87654321",
    "email": "contact@acme.com",
    "website": "https://www.acmecorp.com",
    "opening_hours": "10:00",
    "closing_hours": "19:00"
  }'
```

**Expected Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "uid": "bus_abc123def456",
    "seq_id": "BUS-0001",
    "name": "Acme Corporation",
    "phone": "+91-22-87654321",
    "email": "contact@acme.com",
    "website": "https://www.acmecorp.com",
    "opening_hours": "10:00",
    "closing_hours": "19:00",
    "updated_at": "2025-10-10T10:05:00Z",  // Updated timestamp
    // ... other fields unchanged
  },
  "timestamp": "2025-10-10T10:05:00Z",
  "path": "/api/v1/business"
}
```

**Validation**:
- ✅ Status code is 200
- ✅ Updated fields reflect new values
- ✅ updated_at timestamp is newer than created_at
- ✅ Other fields remain unchanged

### Step 3.2: Validate business hours consistency

**Request**:
```bash
# Try to set closing hours before opening hours (should fail)
curl -X PUT "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-Workspace-ID: ${WORKSPACE_ID}" \
  -H "Content-Type: application/json" \
  -d '{
    "opening_hours": "18:00",
    "closing_hours": "09:00"
  }'
```

**Expected Response** (400 Bad Request):
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid business hours",
    "details": [
      {
        "field": "closing_hours",
        "message": "Closing hours must be after opening hours"
      }
    ]
  },
  "timestamp": "2025-10-10T10:06:00Z",
  "path": "/api/v1/business"
}
```

**Validation**:
- ✅ Status code is 400
- ✅ Validation error returned
- ✅ Business hours logic enforced

## Test Scenario 4: Validation Testing

### Step 4.1: Test required fields

**Request**:
```bash
curl -X POST "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-Workspace-ID: new-workspace-id" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Missing required fields"
  }'
```

**Expected Response** (400 Bad Request):
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": [
      {
        "field": "name",
        "message": "Business name is required"
      },
      {
        "field": "business_type",
        "message": "Business type is required"
      }
    ]
  },
  "timestamp": "2025-10-10T10:07:00Z",
  "path": "/api/v1/business"
}
```

**Validation**:
- ✅ Status code is 400
- ✅ All missing required fields reported

### Step 4.2: Test email format validation

**Request**:
```bash
curl -X POST "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-Workspace-ID: new-workspace-id-2" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Business",
    "business_type": "SERVICE",
    "email": "invalid-email-format"
  }'
```

**Expected Response** (400 Bad Request):
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": [
      {
        "field": "email",
        "message": "Invalid email format"
      }
    ]
  },
  "timestamp": "2025-10-10T10:08:00Z",
  "path": "/api/v1/business"
}
```

**Validation**:
- ✅ Status code is 400
- ✅ Email format validation works

### Step 4.3: Test phone format validation

**Request**:
```bash
curl -X POST "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-Workspace-ID: new-workspace-id-3" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Business",
    "business_type": "SERVICE",
    "phone": "invalid-phone"
  }'
```

**Expected Response** (400 Bad Request):
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": [
      {
        "field": "phone",
        "message": "Invalid phone number"
      }
    ]
  },
  "timestamp": "2025-10-10T10:09:00Z",
  "path": "/api/v1/business"
}
```

**Validation**:
- ✅ Status code is 400
- ✅ Phone format validation works

## Test Scenario 5: Authorization Testing

### Step 5.1: Test without authentication

**Request**:
```bash
curl -X GET "${API_BASE_URL}/business" \
  -H "X-Workspace-ID: ${WORKSPACE_ID}"
  # No Authorization header
```

**Expected Response** (401 Unauthorized):
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required"
  },
  "timestamp": "2025-10-10T10:10:00Z"
}
```

**Validation**:
- ✅ Status code is 401
- ✅ Authentication enforced

### Step 5.2: Test without workspace header

**Request**:
```bash
curl -X GET "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}"
  # No X-Workspace-ID header
```

**Expected Response** (400 Bad Request):
```json
{
  "success": false,
  "error": {
    "code": "MISSING_WORKSPACE_HEADER",
    "message": "X-Workspace-ID header is required"
  },
  "timestamp": "2025-10-10T10:11:00Z"
}
```

**Validation**:
- ✅ Status code is 400
- ✅ Workspace context required

## Test Scenario 6: Data Migration Verification

### Step 6.1: Verify migrated business data

**For existing workspaces with business data in workspace table:**

**Request**:
```bash
curl -X GET "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-Workspace-ID: ${EXISTING_WORKSPACE_ID}"
```

**Expected Response** (200 OK):
```json
{
  "success": true,
  "data": {
    "uid": "bus_migrated123",
    "name": "Existing Business Name",
    // ... fields migrated from workspace table
  },
  "timestamp": "2025-10-10T10:12:00Z",
  "path": "/api/v1/business"
}
```

**Validation**:
- ✅ Business data exists for pre-migration workspaces
- ✅ All fields correctly migrated
- ✅ Data integrity maintained

## Database Verification

### Verify business table

```sql
-- Check business record exists
SELECT uid, seq_id, name, business_type, workspace_id, active
FROM businesses
WHERE workspace_id = 'your-workspace-uid';

-- Verify unique constraint on workspace_id
SELECT workspace_id, COUNT(*)
FROM businesses
GROUP BY workspace_id
HAVING COUNT(*) > 1;
-- Should return 0 rows

-- Check indexes exist
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'businesses';
-- Should show idx_business_workspace, idx_business_type, etc.

-- Verify foreign key constraint
SELECT conname, conrelid::regclass, confrelid::regclass
FROM pg_constraint
WHERE conname = 'fk_business_workspace';
-- Should show businesses -> workspaces relationship
```

## Performance Verification

### Response time check

```bash
# Measure GET request time
time curl -X GET "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-Workspace-ID: ${WORKSPACE_ID}" \
  -w "\nTime total: %{time_total}s\n"
```

**Expected**:
- ✅ Response time < 50ms for p95
- ✅ Database query time < 10ms

## Success Criteria Checklist

- [ ] ✅ Business profile can be created (POST)
- [ ] ✅ Business profile can be retrieved (GET)
- [ ] ✅ Business profile can be updated (PUT)
- [ ] ✅ Duplicate business creation prevented (409 Conflict)
- [ ] ✅ Tenant isolation works (no cross-workspace access)
- [ ] ✅ Required field validation works
- [ ] ✅ Email format validation works
- [ ] ✅ Phone format validation works
- [ ] ✅ Business hours validation works
- [ ] ✅ Authentication required (401 without JWT)
- [ ] ✅ Workspace header required (400 without X-Workspace-ID)
- [ ] ✅ Migrated data accessible
- [ ] ✅ Database constraints enforced
- [ ] ✅ Performance goals met (<50ms)
- [ ] ✅ ApiResponse wrapper used consistently
- [ ] ✅ JSON snake_case naming convention followed

## Cleanup (Optional)

```bash
# Delete test business (if DELETE endpoint implemented)
curl -X DELETE "${API_BASE_URL}/business" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "X-Workspace-ID: ${WORKSPACE_ID}"
```

## Troubleshooting

### Issue: 404 on all requests
**Solution**: Verify business module is loaded in Spring Boot application.properties

### Issue: 500 Internal Server Error
**Solution**: Check application logs for exceptions, verify database connection

### Issue: Validation errors not returned
**Solution**: Ensure @Valid annotation on controller parameters

### Issue: Cross-tenant data leak
**Solution**: Verify @TenantId annotation on Business.workspaceId field

---

**Status**: Quickstart ready for validation testing
