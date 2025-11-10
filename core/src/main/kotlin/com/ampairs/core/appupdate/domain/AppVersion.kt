package com.ampairs.core.appupdate.domain

import com.ampairs.core.domain.model.BaseDomain
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

/**
 * App version entity for managing desktop app updates (macOS, Windows, Linux).
 *
 * This entity does NOT extend OwnableBaseDomain because app versions are global,
 * not tenant-specific. All users across all workspaces use the same app binaries.
 */
@Entity
@Table(
    name = "app_versions",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_app_versions_version_platform", columnNames = ["version", "platform"])
    ],
    indexes = [
        Index(name = "idx_app_versions_platform_active", columnList = "platform,is_active,version_code"),
        Index(name = "idx_app_versions_active", columnList = "is_active,release_date")
    ]
)
class AppVersion : BaseDomain() {

    @Column(nullable = false, length = 50)
    var version: String = ""

    @Column(name = "version_code", nullable = false)
    var versionCode: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var platform: PlatformType = PlatformType.MACOS

    @Column(name = "is_mandatory", nullable = false)
    var isMandatory: Boolean = false

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @Column(name = "s3_key", nullable = false, length = 500)
    var s3Key: String = ""

    @Column(name = "filename", nullable = false, length = 255)
    var filename: String = ""

    @Column(name = "file_size_mb", precision = 10, scale = 2)
    var fileSizeMb: BigDecimal? = null

    @Column(length = 128)
    var checksum: String? = null

    @Column(name = "release_date")
    var releaseDate: Instant? = null

    @Column(name = "release_notes", columnDefinition = "TEXT")
    var releaseNotes: String? = null

    @Column(name = "min_supported_version", length = 50)
    var minSupportedVersion: String? = null

    @Column(name = "created_by", length = 100)
    var createdBy: String? = null

    @Column(name = "updated_by", length = 100)
    var updatedBy: String? = null

    override fun obtainSeqIdPrefix(): String = "VER"
}

enum class PlatformType {
    MACOS,
    WINDOWS,
    LINUX
}
