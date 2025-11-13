package com.ampairs.core.appupdate.service

import com.ampairs.core.appupdate.domain.*
import com.ampairs.core.appupdate.repository.AppVersionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class AppUpdateService(
    private val appVersionRepository: AppVersionRepository,
    private val s3FileStreamService: S3FileStreamService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Check if an update is available for the given platform and version.
     * This is a PUBLIC endpoint - no authentication required.
     */
    fun checkForUpdates(
        platform: String,
        currentVersion: String,
        currentVersionCode: Int
    ): UpdateCheckResponse {
        logger.info("Checking for updates: platform=$platform, currentVersion=$currentVersion, versionCode=$currentVersionCode")

        val platformType = try {
            PlatformType.valueOf(platform.uppercase())
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid platform type: $platform")
            return UpdateCheckResponse(
                updateAvailable = false,
                message = "Invalid platform type: $platform"
            )
        }

        // Find latest version for platform
        val latestVersion = appVersionRepository.findLatestByPlatformAndActive(platformType)

        if (latestVersion == null) {
            logger.info("No active version found for platform: $platform")
            return UpdateCheckResponse(
                updateAvailable = false,
                message = "No updates available for this platform"
            )
        }

        // Compare version codes
        if (latestVersion.versionCode <= currentVersionCode) {
            logger.info("Current version is up to date")
            return UpdateCheckResponse(
                updateAvailable = false,
                message = "You are running the latest version"
            )
        }

        // Determine if update is mandatory
        val isMandatory = latestVersion.isMandatory || isVersionBelowMinSupported(
            currentVersion,
            latestVersion.minSupportedVersion
        )

        logger.info("Update available: ${latestVersion.version} (mandatory: $isMandatory)")

        // Build update info (no S3 URL exposed - client will call download endpoint)
        val updateInfo = UpdateInfoDTO(
            uid = latestVersion.uid,
            version = latestVersion.version,
            versionCode = latestVersion.versionCode,
            releaseDate = latestVersion.releaseDate ?: latestVersion.createdAt!!,
            isMandatory = isMandatory,
            fileSizeMb = latestVersion.fileSizeMb ?: BigDecimal.ZERO,
            filename = latestVersion.filename,
            platform = latestVersion.platform.name,
            releaseNotes = latestVersion.releaseNotes,
            minSupportedVersion = latestVersion.minSupportedVersion,
            checksum = latestVersion.checksum
        )

        return UpdateCheckResponse(
            updateAvailable = true,
            updateInfo = updateInfo,
            message = if (isMandatory) "Critical update required" else "New version available"
        )
    }

    /**
     * Create a new app version (Admin only).
     *
     * IMPORTANT: Validates that the S3 file exists before creating the database entry.
     * This prevents creating version records that point to non-existent files.
     */
    @Transactional
    fun createAppVersion(request: CreateAppVersionRequest, createdBy: String?): AppVersion {
        logger.info("Creating app version: ${request.version} for ${request.platform}")

        // Check if version already exists
        if (appVersionRepository.existsByVersionAndPlatform(request.version, request.platform)) {
            throw IllegalArgumentException("Version ${request.version} already exists for platform ${request.platform}")
        }

        // Validate that S3 file exists before creating database entry
        if (!s3FileStreamService.fileExists(request.s3Key)) {
            logger.error("S3 file not found: ${request.s3Key}")
            throw IllegalArgumentException(
                "S3 file not found: ${request.s3Key}. " +
                "Please upload the file to S3 before creating the version entry."
            )
        }

        logger.info("S3 file validated: ${request.s3Key}")

        val entity = AppVersion().apply {
            version = request.version
            versionCode = request.versionCode
            platform = request.platform
            isMandatory = request.isMandatory
            s3Key = request.s3Key
            filename = request.filename
            fileSizeMb = request.fileSizeMb
            releaseNotes = request.releaseNotes
            minSupportedVersion = request.minSupportedVersion
            checksum = request.checksum
            releaseDate = request.releaseDate
            this.createdBy = createdBy
        }

        return appVersionRepository.save(entity)
    }

    /**
     * Get all versions (Admin only).
     */
    fun getAllVersions(): List<AppVersion> {
        return appVersionRepository.findAllByOrderByReleaseDateDesc()
    }

    /**
     * Get version by UID (Admin only).
     */
    fun getVersionByUid(uid: String): AppVersion {
        return appVersionRepository.findByUid(uid)
            ?: throw NoSuchElementException("App version not found: $uid")
    }

    /**
     * Get version for download.
     * PUBLIC endpoint - used by download streaming controller.
     */
    fun getVersionForDownload(uid: String): AppVersion {
        val version = appVersionRepository.findByUid(uid)
            ?: throw NoSuchElementException("App version not found: $uid")

        if (!version.isActive) {
            throw IllegalStateException("This version is no longer active")
        }

        logger.info("Download requested for: ${version.version} (${version.platform}), s3_key=${version.s3Key}")
        return version
    }

    /**
     * Activate/Deactivate a version (Admin only).
     */
    @Transactional
    fun toggleVersionActive(uid: String, isActive: Boolean): AppVersion {
        val entity = getVersionByUid(uid)
        entity.isActive = isActive
        return appVersionRepository.save(entity)
    }

    /**
     * Update app version (Admin only).
     *
     * IMPORTANT: Validates that the S3 file exists before updating.
     */
    @Transactional
    fun updateAppVersion(uid: String, request: CreateAppVersionRequest, updatedBy: String?): AppVersion {
        val entity = getVersionByUid(uid)

        // Validate that S3 file exists if s3Key is being changed
        if (entity.s3Key != request.s3Key && !s3FileStreamService.fileExists(request.s3Key)) {
            logger.error("S3 file not found: ${request.s3Key}")
            throw IllegalArgumentException(
                "S3 file not found: ${request.s3Key}. " +
                "Please upload the file to S3 before updating the version entry."
            )
        }

        entity.apply {
            version = request.version
            versionCode = request.versionCode
            platform = request.platform
            isMandatory = request.isMandatory
            s3Key = request.s3Key
            filename = request.filename
            fileSizeMb = request.fileSizeMb
            releaseNotes = request.releaseNotes
            minSupportedVersion = request.minSupportedVersion
            checksum = request.checksum
            releaseDate = request.releaseDate
            this.updatedBy = updatedBy
        }

        return appVersionRepository.save(entity)
    }

    /**
     * Delete a version (Admin only).
     */
    @Transactional
    fun deleteVersion(uid: String) {
        val entity = getVersionByUid(uid)
        appVersionRepository.delete(entity)
        logger.info("Deleted app version: $uid")
    }

    /**
     * Compare versions using semantic versioning.
     * Returns true if currentVersion is below minSupportedVersion.
     */
    private fun isVersionBelowMinSupported(currentVersion: String, minSupportedVersion: String?): Boolean {
        if (minSupportedVersion.isNullOrBlank()) return false

        return try {
            val current = parseVersion(currentVersion)
            val minSupported = parseVersion(minSupportedVersion)
            compareVersions(current, minSupported) < 0
        } catch (e: Exception) {
            logger.warn("Error comparing versions: $currentVersion vs $minSupportedVersion", e)
            false
        }
    }

    private fun parseVersion(version: String): List<Int> {
        return version.split(".")
            .map { it.toIntOrNull() ?: 0 }
    }

    private fun compareVersions(v1: List<Int>, v2: List<Int>): Int {
        val maxLength = maxOf(v1.size, v2.size)
        for (i in 0 until maxLength) {
            val part1 = v1.getOrElse(i) { 0 }
            val part2 = v2.getOrElse(i) { 0 }
            if (part1 != part2) {
                return part1.compareTo(part2)
            }
        }
        return 0
    }
}
