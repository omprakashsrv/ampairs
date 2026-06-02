package com.ampairs.file.domain.model

import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity(name = "entity_image")
@Table(
    name = "entity_image",
    indexes = [
        Index(name = "idx_entity_image_entity", columnList = "entity_type, entity_uid"),
        Index(name = "idx_entity_image_workspace", columnList = "owner_id"),
        Index(name = "idx_entity_image_primary", columnList = "entity_type, entity_uid, is_primary"),
        Index(name = "idx_entity_image_checksum", columnList = "checksum")
    ]
)
class EntityImage : OwnableBaseDomain() {

    @Column(name = "entity_type", nullable = false, length = 50)
    var entityType: String = ""

    @Column(name = "entity_uid", nullable = false, length = 200)
    var entityUid: String = ""

    @Column(name = "original_filename", length = 500)
    var originalFilename: String = ""

    @Column(name = "file_extension", length = 20)
    var fileExtension: String = ""

    @Column(name = "content_type", length = 100)
    var contentType: String = ""

    @Column(name = "file_size", nullable = false)
    var fileSize: Long = 0

    @Column(name = "storage_path", length = 1000)
    var storagePath: String = ""

    @Column(name = "storage_url", length = 1000)
    var storageUrl: String? = null

    // SHA-256 hex of the processed bytes — used for deduplication within entity scope
    @Column(name = "checksum", length = 64)
    var checksum: String = ""

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    var metadata: EntityImageMetadata = EntityImageMetadata()

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    @Column(name = "description", length = 500)
    var description: String? = null

    @Column(name = "uploaded_at", nullable = false)
    var uploadedAt: Instant = Instant.now()

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = "EIM"

    fun storagePath(): String =
        "$ownerId/$entityType/$entityUid/${this.uid}.$fileExtension"
}

data class EntityImageMetadata(
    val etag: String? = null,
    val lastModified: Instant? = null,
    val width: Int? = null,
    val height: Int? = null,
    val thumbnailsGenerated: Boolean = false,
    val exifStripped: Boolean = true
)
