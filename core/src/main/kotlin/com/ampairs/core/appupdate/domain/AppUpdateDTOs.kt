package com.ampairs.core.appupdate.domain

import java.math.BigDecimal
import java.time.Instant

/**
 * Response DTO for update check endpoint.
 *
 * IMPORTANT: No @JsonProperty annotations needed - global snake_case config handles naming.
 * See CLAUDE.md for details.
 */
data class UpdateCheckResponse(
    val updateAvailable: Boolean,
    val updateInfo: UpdateInfoDTO? = null,
    val message: String? = null
)

/**
 * Detailed update information.
 *
 * NOTE: No download_url is exposed. Clients must use the uid
 * to call GET /api/v1/app-updates/download/{uid} to stream the file.
 */
data class UpdateInfoDTO(
    val uid: String,
    val version: String,
    val versionCode: Int,
    val releaseDate: Instant,
    val isMandatory: Boolean,
    val fileSizeMb: BigDecimal,
    val filename: String,
    val platform: String,
    val releaseNotes: String? = null,
    val minSupportedVersion: String? = null,
    val checksum: String? = null
)

/**
 * Request DTO for creating/updating app version.
 *
 * Admin uploads file to S3 first, then provides s3_key and filename.
 */
data class CreateAppVersionRequest(
    val version: String,
    val versionCode: Int,
    val platform: PlatformType,
    val isMandatory: Boolean = false,
    val s3Key: String,
    val filename: String,
    val fileSizeMb: BigDecimal? = null,
    val releaseNotes: String? = null,
    val minSupportedVersion: String? = null,
    val checksum: String? = null,
    val releaseDate: Instant? = null
)

/**
 * Response DTO for admin listing.
 */
data class AppVersionResponse(
    val uid: String,
    val version: String,
    val versionCode: Int,
    val platform: String,
    val isMandatory: Boolean,
    val isActive: Boolean,
    val s3Key: String,
    val filename: String,
    val fileSizeMb: BigDecimal?,
    val releaseDate: Instant?,
    val releaseNotes: String?,
    val checksum: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

/**
 * Extension function to convert entity to response DTO.
 * Follows DTO pattern from CLAUDE.md - never expose entities directly.
 */
fun AppVersion.asAppVersionResponse(): AppVersionResponse = AppVersionResponse(
    uid = this.uid,
    version = this.version,
    versionCode = this.versionCode,
    platform = this.platform.name,
    isMandatory = this.isMandatory,
    isActive = this.isActive,
    s3Key = this.s3Key,
    filename = this.filename,
    fileSizeMb = this.fileSizeMb,
    releaseDate = this.releaseDate,
    releaseNotes = this.releaseNotes,
    checksum = this.checksum,
    createdAt = this.createdAt!!,
    updatedAt = this.updatedAt!!
)

fun List<AppVersion>.asAppVersionResponses(): List<AppVersionResponse> = map { it.asAppVersionResponse() }
