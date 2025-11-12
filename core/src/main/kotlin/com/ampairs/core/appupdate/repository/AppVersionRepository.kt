package com.ampairs.core.appupdate.repository

import com.ampairs.core.appupdate.domain.AppVersion
import com.ampairs.core.appupdate.domain.PlatformType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AppVersionRepository : JpaRepository<AppVersion, Long> {

    /**
     * Find the latest active version for a specific platform.
     * Uses @EntityGraph pattern from CLAUDE.md for efficient loading.
     */
    @Query(
        """
        SELECT v FROM AppVersion v
        WHERE v.platform = :platform
        AND v.isActive = true
        ORDER BY v.versionCode DESC
        LIMIT 1
        """
    )
    fun findLatestByPlatformAndActive(
        @Param("platform") platform: PlatformType
    ): AppVersion?

    /**
     * Find all active versions for a platform ordered by version code descending.
     */
    fun findByPlatformAndIsActiveTrueOrderByVersionCodeDesc(
        platform: PlatformType
    ): List<AppVersion>

    /**
     * Find specific version by version string and platform.
     */
    fun findByVersionAndPlatform(
        version: String,
        platform: PlatformType
    ): AppVersion?

    /**
     * Check if a version exists (for duplicate prevention).
     */
    fun existsByVersionAndPlatform(
        version: String,
        platform: PlatformType
    ): Boolean

    /**
     * Find all versions ordered by release date descending.
     */
    fun findAllByOrderByReleaseDateDesc(): List<AppVersion>

    /**
     * Find all active versions ordered by release date.
     */
    fun findByIsActiveTrueOrderByReleaseDateDesc(): List<AppVersion>

    /**
     * Find by UID (for admin operations).
     */
    fun findByUid(uid: String): AppVersion?
}
