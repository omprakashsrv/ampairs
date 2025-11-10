# Desktop App Update API

This module provides REST APIs for managing desktop application updates for macOS, Windows, and Linux platforms.

## Architecture

### Package Structure
```
com.ampairs.core.appupdate/
├── controller/
│   └── AppUpdateController.kt    # REST endpoints
├── service/
│   └── AppUpdateService.kt       # Business logic
├── repository/
│   └── AppVersionRepository.kt   # Data access
└── domain/
    ├── AppVersion.kt              # Entity
    └── AppUpdateDTOs.kt           # Request/Response DTOs
```

### Database Schema

Table: `app_versions`

Key features:
- Platform-specific versions (MACOS, WINDOWS, LINUX)
- Version comparison via `version_code` (incremental integer)
- Optional mandatory updates
- SHA-256 checksum for file integrity
- S3-hosted binary files
- Semantic versioning support with `min_supported_version`

Migration: `core/src/main/resources/db/migration/mysql/V1.0.3__create_app_versions_table.sql`

## API Endpoints

### Public Endpoint (No Authentication)

#### Check for Updates
```http
GET /api/v1/app-updates/check?platform=MACOS&currentVersion=1.0.0.9&versionCode=9
```

**Response (Update Available):**
```json
{
  "success": true,
  "data": {
    "update_available": true,
    "update_info": {
      "uid": "VER-1234567890",
      "version": "1.0.0.10",
      "version_code": 10,
      "release_date": "2025-01-15T10:00:00Z",
      "is_mandatory": false,
      "file_size_mb": 125.5,
      "filename": "Ampairs-1.0.0.10-macos.dmg",
      "platform": "MACOS",
      "release_notes": "- New features\n- Bug fixes\n- Performance improvements",
      "min_supported_version": "1.0.0.5",
      "checksum": "abc123def456..."
    },
    "message": "New version available"
  },
  "timestamp": "2025-01-15T12:00:00Z"
}
```

**Note:** No `download_url` is exposed. Clients use the `uid` to call the download endpoint.

**Response (No Update):**
```json
{
  "success": true,
  "data": {
    "update_available": false,
    "message": "You are running the latest version"
  },
  "timestamp": "2025-01-15T12:00:00Z"
}
```

#### Download App Update File
```http
GET /api/v1/app-updates/download/{uid}
```

**PUBLIC ENDPOINT - No authentication required.**

Streams the app update binary file directly from S3 through the backend.
No S3 URLs are exposed to clients - complete backend control.

**Rate Limited:** 1 request per 10 seconds per IP address (strict, no burst).

**Response Headers:**
```
Content-Type: application/x-apple-diskimage (or appropriate MIME type)
Content-Disposition: attachment; filename="Ampairs-1.0.0.10-macos.dmg"
Content-Length: 131621376
X-Checksum-SHA256: abc123def456...
```

**Response Body:** Binary file stream

**Error Responses:**
- 404: Version not found
- 400: Version is no longer active
- 429: Rate limit exceeded (too many requests)

**Security Benefits:**
- No S3 URLs exposed to clients
- Complete rate limiting control at backend
- Download analytics and logging
- No risk of URL sharing or abuse
- Can add authentication later if needed

### Admin Endpoints (Require `ADMIN` Role)

#### List All Versions
```http
GET /api/v1/app-updates
Authorization: Bearer <admin_token>
```

#### Get Version by UID
```http
GET /api/v1/app-updates/{uid}
Authorization: Bearer <admin_token>
```

#### Create New Version
```http
POST /api/v1/app-updates
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "version": "1.0.0.10",
  "version_code": 10,
  "platform": "MACOS",
  "is_mandatory": false,
  "s3_key": "updates/macos-1.0.0.10.dmg",
  "filename": "Ampairs-1.0.0.10-macos.dmg",
  "file_size_mb": 125.5,
  "release_notes": "- New features\n- Bug fixes",
  "min_supported_version": "1.0.0.5",
  "checksum": "abc123def456...",
  "release_date": "2025-01-15T10:00:00Z"
}
```

**Note:** Admin must upload file to S3 first, then provide the `s3_key`.

#### Update Version
```http
PUT /api/v1/app-updates/{uid}
Authorization: Bearer <admin_token>
Content-Type: application/json
```

#### Toggle Active Status
```http
PATCH /api/v1/app-updates/{uid}/active?isActive=false
Authorization: Bearer <admin_token>
```

#### Delete Version
```http
DELETE /api/v1/app-updates/{uid}
Authorization: Bearer <admin_token>
```

## Configuration

### Application Properties

**Development (`application.yml`):**
```yaml
ampairs:
  app-updates:
    storage:
      bucket: ampairs-app-updates
      base-url: https://ampairs-app-updates.s3.amazonaws.com
      folder: updates
    max-file-size: 500MB
```

**Production (`application-production.yml`):**
```yaml
ampairs:
  app-updates:
    storage:
      bucket: ${APP_UPDATES_BUCKET:ampairs-app-updates}
      base-url: ${APP_UPDATES_BASE_URL:https://ampairs-app-updates.s3.ap-south-1.amazonaws.com}
      folder: updates
    max-file-size: 500MB
```

### Environment Variables
- `APP_UPDATES_BUCKET` - S3 bucket for app binaries
- `APP_UPDATES_BASE_URL` - Public URL for downloads

## Usage Workflow

### 1. Build Desktop App
```bash
# macOS
./gradlew packageDmg

# Windows
./gradlew packageMsi

# Linux
./gradlew packageDeb
```

### 2. Upload to S3
```bash
aws s3 cp Ampairs-1.0.0.10-macos.dmg \
  s3://ampairs-app-updates/updates/macos-1.0.0.10.dmg \
  --acl public-read
```

### 3. Calculate Checksum
```bash
sha256sum Ampairs-1.0.0.10-macos.dmg
```

### 4. Create Version Entry
```bash
curl -X POST https://api.ampairs.in/api/v1/app-updates \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "version": "1.0.0.10",
    "version_code": 10,
    "platform": "MACOS",
    "is_mandatory": false,
    "s3_key": "updates/macos-1.0.0.10.dmg",
    "filename": "Ampairs-1.0.0.10-macos.dmg",
    "file_size_mb": 125.5,
    "release_notes": "- New features\n- Bug fixes",
    "min_supported_version": "1.0.0.5",
    "checksum": "abc123def456...",
    "release_date": "2025-01-15T10:00:00Z"
  }'
```

### 5. Desktop App Checks for Updates
```kotlin
// Kotlin Multiplatform client code
val platform = when (currentOperatingSystem) {
    OperatingSystem.MacOS -> "MACOS"
    OperatingSystem.Windows -> "WINDOWS"
    OperatingSystem.Linux -> "LINUX"
}

val response = httpClient.get("https://api.ampairs.in/api/v1/app-updates/check") {
    parameter("platform", platform)
    parameter("currentVersion", BuildConfig.VERSION)
    parameter("versionCode", BuildConfig.VERSION_CODE)
}

if (response.data.updateAvailable) {
    val updateInfo = response.data.updateInfo!!

    // Show update dialog to user
    showUpdateDialog(updateInfo)

    // When user confirms download, stream file through backend (rate-limited)
    val downloadUrl = "https://api.ampairs.in/api/v1/app-updates/download/${updateInfo.uid}"
    val downloadedFile = downloadFileFromBackend(downloadUrl, updateInfo.filename)

    // Verify checksum (from update info or response header)
    verifyChecksum(downloadedFile, updateInfo.checksum)

    // Install update
    installUpdate(downloadedFile)
}

// Helper function to download file
fun downloadFileFromBackend(url: String, filename: String): File {
    val response = httpClient.get(url)
    val targetFile = File(downloadDir, filename)

    response.bodyAsChannel().copyTo(targetFile.outputStream())

    // Optional: verify checksum from X-Checksum-SHA256 header
    val checksumHeader = response.headers["X-Checksum-SHA256"]

    return targetFile
}
```

## Version Comparison Logic

The service uses **version code** (integer) for primary comparison:
- Current version code: `9`
- Latest version code: `10`
- Result: Update available ✅

### Mandatory Updates

Updates are marked mandatory if:
1. `is_mandatory` flag is `true` in database, OR
2. Current version is below `min_supported_version` (semantic version comparison)

Example:
- Current: `1.0.0.4`
- Min supported: `1.0.0.5`
- Result: Mandatory update (forced)

## Security Considerations

### Public Endpoint Security
The `/check` endpoint is intentionally **public** (no authentication) because:
- Desktop apps need to check for updates before user login
- No sensitive data is exposed (only version metadata)
- Rate limiting is applied via nginx configuration

### File Integrity
- SHA-256 checksums verify file integrity
- Clients should verify checksum after download
- Prevents corrupted or tampered binaries

### S3 Security & Cost Control
- **Private S3 bucket** - No public-read ACL needed
- Backend streams files using AWS credentials - full access control
- No S3 URLs exposed to clients - prevents bandwidth abuse
- Complete rate limiting at application layer
- Download analytics and logging in application
- Enable S3 bucket logging for audit trail
- Enable versioning for rollback capability

**Billing Protection:**
- Rate limiting prevents download abuse (1 req/10s)
- Backend can implement authentication if needed
- No risk of URL sharing causing unexpected costs
- Full visibility into who downloads what

## Design Patterns (Following CLAUDE.md)

### 1. Instant for Timestamps
```kotlin
// ✅ CORRECT - Uses Instant (UTC)
@Column(name = "release_date")
var releaseDate: Instant? = null

// ❌ WRONG - LocalDateTime causes timezone bugs
var releaseDate: LocalDateTime? = null
```

### 2. Global snake_case Configuration
```kotlin
// ✅ CORRECT - No @JsonProperty needed
data class UpdateCheckResponse(
    val updateAvailable: Boolean,  // Maps to "update_available"
    val updateInfo: UpdateInfoDTO? = null
)

// ❌ WRONG - Redundant annotations
data class UpdateCheckResponse(
    @JsonProperty("update_available")  // ❌ Unnecessary
    val updateAvailable: Boolean
)
```

### 3. DTO Pattern
```kotlin
// ✅ CORRECT - Never expose entities
@GetMapping
fun getAllVersions(): ApiResponse<List<AppVersionResponse>> {
    return ApiResponse.success(service.getAllVersions().asAppVersionResponses())
}

// ❌ WRONG - Exposes internal entity structure
@GetMapping
fun getAllVersions(): ApiResponse<List<AppVersion>> {
    return ApiResponse.success(service.getAllVersions())
}
```

### 4. ApiResponse Wrapper
```kotlin
// ✅ CORRECT - Consistent response format
return ApiResponse.success(data)
return ApiResponse.error("ERROR_CODE", "Error message")

// ❌ WRONG - Direct return
return ResponseEntity.ok(data)
```

## Testing

### Manual Testing with cURL

**Check for updates:**
```bash
curl "http://localhost:8080/api/v1/app-updates/check?platform=MACOS&currentVersion=1.0.0.9&versionCode=9"
```

**Create version (admin):**
```bash
curl -X POST http://localhost:8080/api/v1/app-updates \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d @version.json
```

### Integration Tests
```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class AppUpdateIntegrationTest {
    @Test
    fun `should return update when newer version exists`() {
        // Create test version in DB
        // Call /check endpoint
        // Assert update_available = true
    }
}
```

## Deployment Checklist

- [ ] Create **private** S3 bucket: `ampairs-app-updates`
- [ ] Configure AWS credentials for backend access to S3
- [ ] Enable S3 versioning
- [ ] Enable S3 bucket logging
- [ ] Set environment variables in production (bucket name, region)
- [ ] Run Flyway migration (V1.0.17)
- [ ] Test update check endpoint
- [ ] Upload first app version to S3 (private)
- [ ] Create initial version entries in database with `s3_key`
- [ ] Test download streaming endpoint
- [ ] Verify rate limiting (1 req/10s for downloads)

## Troubleshooting

### Update Not Detected
- Verify `version_code` is incremented
- Check `is_active = true` in database
- Confirm platform matches exactly (case-sensitive)

### Download Fails
- Verify backend has S3 access credentials configured
- Check S3 bucket name and region in configuration
- Verify `s3_key` is correct in database
- Check backend logs for S3 errors
- Verify file exists in S3 bucket
- Verify checksum matches file

### Mandatory Update Not Enforced
- Check `is_mandatory` flag
- Verify `min_supported_version` comparison logic
- Review client-side update logic

## Future Enhancements

- [ ] Add file upload endpoint for admins
- [ ] Implement automatic checksum calculation
- [ ] Add delta updates (patch files)
- [ ] Add update statistics/analytics
- [ ] Add staged rollouts (percentage-based)
- [ ] Add rollback capability
- [ ] Add webhook notifications on new releases
