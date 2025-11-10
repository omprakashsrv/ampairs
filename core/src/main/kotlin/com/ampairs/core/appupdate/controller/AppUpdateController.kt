package com.ampairs.core.appupdate.controller

import com.ampairs.core.appupdate.domain.*
import com.ampairs.core.appupdate.service.AppUpdateService
import com.ampairs.core.appupdate.service.S3FileStreamService
import com.ampairs.core.domain.dto.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

/**
 * Controller for desktop app update management.
 *
 * IMPORTANT API Design:
 * - `/check` endpoint is PUBLIC (no authentication) - desktop apps need to check before login
 * - Admin endpoints require ADMIN role
 * - All responses use ApiResponse<T> wrapper (see CLAUDE.md)
 * - DTOs use global snake_case configuration (no @JsonProperty needed)
 *
 * API Documentation:
 * - Tag: App Updates
 * - Description: Desktop app update management (macOS, Windows, Linux)
 */
@RestController
@RequestMapping("/api/v1/app-updates")
class AppUpdateController(
    private val appUpdateService: AppUpdateService,
    private val s3FileStreamService: S3FileStreamService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Check for available updates.
     *
     * PUBLIC ENDPOINT - No authentication required.
     * Desktop apps need to check for updates before user login.
     *
     * @param platform Platform type (MACOS, WINDOWS, LINUX)
     * @param currentVersion Current version string (e.g., "1.0.0.9")
     * @param versionCode Current version code (e.g., 9)
     * @return Update check response with update info if available
     */
    @GetMapping("/check")
    fun checkForUpdates(
        @RequestParam platform: String,
        @RequestParam currentVersion: String,
        @RequestParam versionCode: Int
    ): ApiResponse<UpdateCheckResponse> {
        logger.info("Update check request: platform=$platform, version=$currentVersion, code=$versionCode")

        val result = appUpdateService.checkForUpdates(platform, currentVersion, versionCode)

        return ApiResponse.success(result)
    }

    /**
     * Stream app update file from S3.
     *
     * PUBLIC ENDPOINT - No authentication required.
     * Streams the binary file directly from S3 through the backend.
     *
     * Rate Limited: 1 request per 10 seconds per IP (configured in nginx).
     *
     * @param uid Version UID from update check response
     * @return Binary file stream with proper headers
     */
    @GetMapping("/download/{uid}")
    fun downloadFile(@PathVariable uid: String): ResponseEntity<ByteArray> {
        logger.info("Download request: uid=$uid")

        // Get version metadata and validate
        val version = appUpdateService.getVersionForDownload(uid)

        // Stream file from S3
        val s3Object = s3FileStreamService.streamFile(version.s3Key)

        // Build response with proper headers
        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType(s3FileStreamService.getContentType(version.filename))
            setContentDispositionFormData("attachment", version.filename)
            contentLength = s3Object.asByteArray().size.toLong()
            // Add checksum header if available
            version.checksum?.let { set("X-Checksum-SHA256", it) }
        }

        logger.info("Streaming file: ${version.filename} (${headers.contentLength} bytes)")

        return ResponseEntity.ok()
            .headers(headers)
            .body(s3Object.asByteArray())
    }

    /**
     * Get all app versions (Admin only).
     *
     * @return List of all app versions across all platforms
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun getAllVersions(): ApiResponse<List<AppVersionResponse>> {
        val versions = appUpdateService.getAllVersions()
        return ApiResponse.success(versions.asAppVersionResponses())
    }

    /**
     * Create new app version (Admin or API Key with APP_UPDATES scope).
     *
     * @param request Version creation request
     * @return Created version response
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('API_KEY:APP_UPDATES')")
    fun createVersion(
        @RequestBody request: CreateAppVersionRequest
        // TODO: Get current user from SecurityContext
        // @AuthenticationPrincipal user: UserPrincipal
    ): ApiResponse<AppVersionResponse> {
        val version = appUpdateService.createAppVersion(request, createdBy = "admin")
        return ApiResponse.success(version.asAppVersionResponse())
    }

    /**
     * Get version by UID (Admin only).
     *
     * @param uid Version UID
     * @return Version details
     */
    @GetMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getVersionByUid(@PathVariable uid: String): ApiResponse<AppVersionResponse> {
        val version = appUpdateService.getVersionByUid(uid)
        return ApiResponse.success(version.asAppVersionResponse())
    }

    /**
     * Update app version (Admin only).
     *
     * @param uid Version UID
     * @param request Update request
     * @return Updated version response
     */
    @PutMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    fun updateVersion(
        @PathVariable uid: String,
        @RequestBody request: CreateAppVersionRequest
    ): ApiResponse<AppVersionResponse> {
        val version = appUpdateService.updateAppVersion(uid, request, updatedBy = "admin")
        return ApiResponse.success(version.asAppVersionResponse())
    }

    /**
     * Activate/Deactivate version (Admin only).
     *
     * @param uid Version UID
     * @param isActive Active status
     * @return Updated version response
     */
    @PatchMapping("/{uid}/active")
    @PreAuthorize("hasRole('ADMIN')")
    fun toggleActive(
        @PathVariable uid: String,
        @RequestParam isActive: Boolean
    ): ApiResponse<AppVersionResponse> {
        val version = appUpdateService.toggleVersionActive(uid, isActive)
        return ApiResponse.success(version.asAppVersionResponse())
    }

    /**
     * Delete version (Admin only).
     *
     * @param uid Version UID
     * @return Success message
     */
    @DeleteMapping("/{uid}")
    @PreAuthorize("hasRole('ADMIN')")
    fun deleteVersion(@PathVariable uid: String): ApiResponse<Map<String, String>> {
        appUpdateService.deleteVersion(uid)
        return ApiResponse.success(mapOf("message" to "Version deleted successfully"))
    }
}
