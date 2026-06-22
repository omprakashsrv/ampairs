package com.ampairs.file.domain.service

import com.ampairs.core.exception.NotFoundException
import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.file.config.StorageProperties
import com.ampairs.file.domain.dto.*
import com.ampairs.file.domain.model.EntityImage
import com.ampairs.file.domain.model.EntityImageMetadata
import com.ampairs.file.repository.EntityImageRepository
import com.ampairs.file.service.FileValidationService
import com.ampairs.file.service.ImageResizingService
import com.ampairs.file.service.ThumbnailCacheService
import com.ampairs.file.storage.ObjectStorageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream
import java.time.Instant

@Service
@Transactional
class EntityImageService(
    private val entityImageRepository: EntityImageRepository,
    private val objectStorageService: ObjectStorageService,
    private val thumbnailCacheService: ThumbnailCacheService,
    private val imageResizingService: ImageResizingService,
    private val fileValidationService: FileValidationService,
    private val storageProperties: StorageProperties,
    private val entityChangePublisher: EntityChangePublisher,
) {
    private val logger = LoggerFactory.getLogger(EntityImageService::class.java)

    companion object {
        /**
         * Entity types stored as raw non-image documents (HTML print templates, etc.).
         * These skip the image pipeline: no decode/resize/EXIF-strip, no thumbnails, raw bytes only.
         */
        private val DOCUMENT_ENTITY_TYPES = setOf("PRINT_TEMPLATE")
        private val DOCUMENT_ALLOWED_EXTENSIONS = setOf("html", "htm")
    }

    /** Maps the stored (uppercased) entityType to the mobile SyncEntity image code, e.g. "customer_image". */
    private fun imageCode(entityType: String) = "${entityType.lowercase()}_image"

    fun upload(
        entityType: String,
        entityUid: String,
        file: MultipartFile,
        request: EntityImageUploadRequest,
        baseUrl: String,
    ): EntityImageResponse {
        if (entityType.uppercase() in DOCUMENT_ENTITY_TYPES) {
            return uploadDocument(entityType, entityUid, file, request, baseUrl)
        }
        val start = System.currentTimeMillis()

        // 1. Validate — magic bytes, content type, dangerous content
        val validation = fileValidationService.validateFile(
            file,
            allowedTypes = setOf("jpg", "jpeg", "png", "gif", "webp"),
        )
        if (!validation.isValid) {
            throw IllegalArgumentException("Invalid image file: ${validation.errors.joinToString()}")
        }

        val rawBytes = file.bytes
        val checksum = validation.checksum
            ?: throw IllegalArgumentException("Checksum unavailable")

        // 2. Deduplication — same content already uploaded for this entity?
        entityImageRepository.findByChecksum(checksum, entityType, entityUid)?.let { existing ->
            logger.info("Dedup hit: entity={}/{}, existing={}", entityType, entityUid, existing.uid)
            return existing.asEntityImageResponse(baseUrl)
        }

        // 3. Process: auto-resize if oversized + always re-encode to strip EXIF
        val ext = resolveExtension(file.originalFilename ?: "image", file.contentType)
        val processed = imageResizingService.processForStorage(rawBytes, ext)

        // 4. Build entity record
        val image = EntityImage().apply {
            request.uid?.let { uid = it }
            this.entityType = entityType
            this.entityUid = entityUid
            originalFilename = file.originalFilename ?: "image.$ext"
            fileExtension = ext
            contentType = file.contentType ?: "image/jpeg"
            fileSize = processed.bytes.size.toLong()
            this.checksum = checksum
            isPrimary = request.isPrimary
            displayOrder = request.displayOrder ?: 0
            description = request.description
            uploadedAt = Instant.now()
            active = true
        }
        image.storagePath = image.storagePath()

        // 5. Upload to object storage
        val result = objectStorageService.uploadFile(
            processed.bytes,
            storageProperties.defaultBucket,
            image.storagePath,
            image.contentType,
            mapOf(
                "entity-type" to entityType,
                "entity-uid" to entityUid,
                "workspace" to image.ownerId,
                "original-filename" to image.originalFilename,
                "checksum" to checksum
            )
        )

        image.storageUrl = result.url
        image.metadata = EntityImageMetadata(
            etag = result.etag,
            lastModified = result.lastModified,
            width = processed.width,
            height = processed.height,
            exifStripped = true
        )

        // 6. Primary image logic
        if (request.isPrimary) {
            entityImageRepository.clearPrimaryStatus(entityType, entityUid)
            image.isPrimary = true
            image.displayOrder = 0
        } else if (request.displayOrder == null) {
            image.displayOrder = entityImageRepository.getNextDisplayOrder(entityType, entityUid)
        }

        val saved = entityImageRepository.save(image)

        // 7. Pre-generate thumbnails if configured
        if (storageProperties.image.thumbnails.autoGenerate) {
            thumbnailCacheService.preGenerateThumbnails(storageProperties.defaultBucket, saved.storagePath)
        }

        logger.info(
            "Image uploaded: entity={}/{}, uid={}, size={}b, dims={}x{}, time={}ms",
            entityType, entityUid, saved.uid, processed.bytes.size,
            processed.width, processed.height, System.currentTimeMillis() - start
        )

        entityChangePublisher.created(imageCode(entityType), entityUid)
        return saved.asEntityImageResponse(baseUrl)
    }

    /**
     * Upload a raw non-image document (e.g. an HTML print template). Skips the image pipeline:
     * the bytes are validated for extension/size/dangerous-signature only, stored verbatim, and no
     * thumbnails are generated. Shares the entity-scoped storage + sync model with images so the
     * mobile file module can list/pull/reconcile templates exactly like images.
     */
    private fun uploadDocument(
        entityType: String,
        entityUid: String,
        file: MultipartFile,
        request: EntityImageUploadRequest,
        baseUrl: String,
    ): EntityImageResponse {
        val start = System.currentTimeMillis()

        // 1. Validate — extension/size/dangerous-signature; skip the HTML-flagging text scan for
        //    these trusted, server-authored markup documents.
        val validation = fileValidationService.validateFile(
            file,
            allowedTypes = DOCUMENT_ALLOWED_EXTENSIONS,
            scanMaliciousText = false,
        )
        if (!validation.isValid) {
            throw IllegalArgumentException("Invalid document file: ${validation.errors.joinToString()}")
        }

        val rawBytes = file.bytes
        val checksum = validation.checksum
            ?: throw IllegalArgumentException("Checksum unavailable")

        // 2. Deduplication — same content already uploaded for this entity?
        entityImageRepository.findByChecksum(checksum, entityType, entityUid)?.let { existing ->
            logger.info("Dedup hit (document): entity={}/{}, existing={}", entityType, entityUid, existing.uid)
            return existing.asEntityImageResponse(baseUrl)
        }

        val ext = resolveExtension(file.originalFilename ?: "template.html", file.contentType)

        // 3. Build entity record — store raw bytes, no image dimensions/EXIF processing
        val document = EntityImage().apply {
            request.uid?.let { uid = it }
            this.entityType = entityType
            this.entityUid = entityUid
            originalFilename = file.originalFilename ?: "template.$ext"
            fileExtension = ext
            contentType = file.contentType ?: "text/html"
            fileSize = rawBytes.size.toLong()
            this.checksum = checksum
            isPrimary = request.isPrimary
            displayOrder = request.displayOrder ?: 0
            description = request.description
            uploadedAt = Instant.now()
            active = true
        }
        document.storagePath = document.storagePath()

        // 4. Upload raw bytes to object storage
        val result = objectStorageService.uploadFile(
            rawBytes,
            storageProperties.defaultBucket,
            document.storagePath,
            document.contentType,
            mapOf(
                "entity-type" to entityType,
                "entity-uid" to entityUid,
                "workspace" to document.ownerId,
                "original-filename" to document.originalFilename,
                "checksum" to checksum
            )
        )

        document.storageUrl = result.url
        document.metadata = EntityImageMetadata(
            etag = result.etag,
            lastModified = result.lastModified,
            exifStripped = false
        )

        // 5. Primary logic (mirrors the image path)
        if (request.isPrimary) {
            entityImageRepository.clearPrimaryStatus(entityType, entityUid)
            document.isPrimary = true
            document.displayOrder = 0
        } else if (request.displayOrder == null) {
            document.displayOrder = entityImageRepository.getNextDisplayOrder(entityType, entityUid)
        }

        val saved = entityImageRepository.save(document)

        logger.info(
            "Document uploaded: entity={}/{}, uid={}, size={}b, type={}, time={}ms",
            entityType, entityUid, saved.uid, rawBytes.size, document.contentType,
            System.currentTimeMillis() - start
        )

        entityChangePublisher.created(imageCode(entityType), entityUid)
        return saved.asEntityImageResponse(baseUrl)
    }

    @Transactional(readOnly = true)
    fun getImages(entityType: String, entityUid: String, baseUrl: String): EntityImageListResponse =
        entityImageRepository.findActiveByEntity(entityType, entityUid)
            .asEntityImageListResponse(baseUrl)

    @Transactional(readOnly = true)
    fun getImage(entityType: String, entityUid: String, imageUid: String, baseUrl: String): EntityImageResponse =
        requireActive(entityType, entityUid, imageUid).asEntityImageResponse(baseUrl)

    @Transactional(readOnly = true)
    fun download(entityType: String, entityUid: String, imageUid: String): Pair<EntityImage, InputStream> {
        val image = requireActive(entityType, entityUid, imageUid)
        val stream = objectStorageService.downloadFile(storageProperties.defaultBucket, image.storagePath)
        return Pair(image, stream)
    }

    @Transactional(readOnly = true)
    fun thumbnail(
        entityType: String,
        entityUid: String,
        imageUid: String,
        size: Int,
    ): Pair<EntityImage, InputStream> {
        val image = requireActive(entityType, entityUid, imageUid)
        val thumbnailSize = ImageResizingService.ThumbnailSize.fromPixels(size)
            ?: ImageResizingService.ThumbnailSize.fromPixels(storageProperties.image.thumbnails.defaultSize)
            ?: ImageResizingService.ThumbnailSize.MEDIUM
        val (stream, _) = thumbnailCacheService.getThumbnail(
            storageProperties.defaultBucket, image.storagePath, thumbnailSize
        )
        return Pair(image, stream)
    }

    @Transactional(readOnly = true)
    fun downloadByUid(imageUid: String): Pair<EntityImage, InputStream> {
        val image = entityImageRepository.findByUid(imageUid)
            ?: throw NotFoundException("Image not found: $imageUid")
        if (!image.active) throw NotFoundException("Image is not active: $imageUid")
        val stream = objectStorageService.downloadFile(storageProperties.defaultBucket, image.storagePath)
        return Pair(image, stream)
    }

    @Transactional(readOnly = true)
    fun thumbnailByUid(imageUid: String, size: String): Pair<EntityImage, InputStream> {
        val image = entityImageRepository.findByUid(imageUid)
            ?: throw NotFoundException("Image not found: $imageUid")
        if (!image.active) throw NotFoundException("Image is not active: $imageUid")
        val thumbnailSize = ImageResizingService.ThumbnailSize.fromAlias(size)
            ?: ImageResizingService.ThumbnailSize.fromPixels(size.toIntOrNull() ?: 0)
            ?: ImageResizingService.ThumbnailSize.fromPixels(storageProperties.image.thumbnails.defaultSize)
            ?: ImageResizingService.ThumbnailSize.MEDIUM
        val (stream, _) = thumbnailCacheService.getThumbnail(
            storageProperties.defaultBucket, image.storagePath, thumbnailSize
        )
        return Pair(image, stream)
    }

    fun update(
        entityType: String,
        entityUid: String,
        imageUid: String,
        request: EntityImageUpdateRequest,
        baseUrl: String,
    ): EntityImageResponse {
        val image = requireActive(entityType, entityUid, imageUid)
        request.description?.let { image.description = it }
        request.displayOrder?.let { image.displayOrder = it }
        request.isPrimary?.let { isPrimary ->
            if (isPrimary && !image.isPrimary) {
                entityImageRepository.clearPrimaryStatus(entityType, entityUid)
                image.isPrimary = true
                image.displayOrder = 0
            } else if (!isPrimary) {
                image.isPrimary = false
            }
        }
        val saved = entityImageRepository.save(image)
        entityChangePublisher.updated(imageCode(entityType), entityUid)
        return saved.asEntityImageResponse(baseUrl)
    }

    fun setPrimary(entityType: String, entityUid: String, imageUid: String, baseUrl: String): EntityImageResponse {
        val image = requireActive(entityType, entityUid, imageUid)
        entityImageRepository.clearPrimaryStatus(entityType, entityUid)
        image.isPrimary = true
        image.displayOrder = 0
        val saved = entityImageRepository.save(image)
        entityChangePublisher.updated(imageCode(entityType), entityUid)
        return saved.asEntityImageResponse(baseUrl)
    }

    fun delete(entityType: String, entityUid: String, imageUid: String): Boolean {
        val image = entityImageRepository.findByUidAndEntity(imageUid, entityType, entityUid)
            ?: throw NotFoundException("Image not found: $imageUid")
        return try {
            entityImageRepository.softDelete(imageUid)
            objectStorageService.deleteObject(storageProperties.defaultBucket, image.storagePath)
            thumbnailCacheService.deleteThumbnails(storageProperties.defaultBucket, image.storagePath)
            entityChangePublisher.deleted(imageCode(entityType), entityUid)
            true
        } catch (e: Exception) {
            logger.error("Failed to delete image: entity={}/{}, uid={}, error={}", entityType, entityUid, imageUid, e.message)
            false
        }
    }

    fun deleteByUid(imageUid: String) {
        val image = entityImageRepository.findByUid(imageUid)
            ?: throw NotFoundException("Image not found: $imageUid")
        if (!image.active) return
        entityImageRepository.softDelete(imageUid)
        entityChangePublisher.deleted(imageCode(image.entityType), image.entityUid)
        try {
            objectStorageService.deleteObject(storageProperties.defaultBucket, image.storagePath)
            thumbnailCacheService.deleteThumbnails(storageProperties.defaultBucket, image.storagePath)
        } catch (e: Exception) {
            logger.error("Storage cleanup failed for image: uid={}, error={}", imageUid, e.message)
        }
    }

    fun setPrimaryByUid(imageUid: String, baseUrl: String): EntityImageResponse {
        val image = entityImageRepository.findByUid(imageUid)
            ?: throw NotFoundException("Image not found: $imageUid")
        if (!image.active) throw NotFoundException("Image is not active: $imageUid")
        entityImageRepository.clearPrimaryStatus(image.entityType, image.entityUid)
        image.isPrimary = true
        image.displayOrder = 0
        return entityImageRepository.save(image).asEntityImageResponse(baseUrl)
    }

    fun bulkDelete(entityType: String, entityUid: String, imageUids: List<String>): EntityImageBulkDeleteResponse {
        val successful = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val errors = mutableListOf<String>()
        imageUids.forEach { uid ->
            try {
                if (delete(entityType, entityUid, uid)) successful.add(uid)
                else { failed.add(uid); errors.add("Failed to delete: $uid") }
            } catch (e: Exception) {
                failed.add(uid); errors.add("Error deleting $uid: ${e.message}")
            }
        }
        return EntityImageBulkDeleteResponse(successful.size, failed.size, successful, failed, errors)
    }

    fun reorder(entityType: String, entityUid: String, orders: List<EntityImageReorderRequest.ImageOrderItem>, baseUrl: String): EntityImageListResponse {
        orders.forEach { item ->
            entityImageRepository.findByUidAndEntity(item.imageUid, entityType, entityUid)
                ?: throw NotFoundException("Image not found: ${item.imageUid}")
            entityImageRepository.updateDisplayOrder(item.imageUid, item.displayOrder)
        }
        return getImages(entityType, entityUid, baseUrl)
    }

    @Transactional(readOnly = true)
    fun stats(entityType: String, entityUid: String, baseUrl: String): EntityImageStatsResponse {
        val images = entityImageRepository.findActiveByEntity(entityType, entityUid)
        if (images.isEmpty()) return EntityImageStatsResponse(
            entityType, entityUid, 0, 0, 0, "0 B", 0, null, null, null
        )
        val totalSize = images.sumOf { it.fileSize }
        return EntityImageStatsResponse(
            entityType = entityType,
            entityUid = entityUid,
            totalImages = images.size,
            primaryImages = images.count { it.isPrimary },
            totalSize = totalSize,
            formattedTotalSize = formatEntityFileSize(totalSize),
            averageSize = totalSize / images.size,
            largestImage = images.maxByOrNull { it.fileSize }?.asEntityImageResponse(baseUrl),
            oldestImage = images.minByOrNull { it.uploadedAt }?.asEntityImageResponse(baseUrl),
            newestImage = images.maxByOrNull { it.uploadedAt }?.asEntityImageResponse(baseUrl)
        )
    }

    private fun requireActive(entityType: String, entityUid: String, imageUid: String): EntityImage {
        val image = entityImageRepository.findByUidAndEntity(imageUid, entityType, entityUid)
            ?: throw NotFoundException("Image not found: $imageUid")
        if (!image.active) throw NotFoundException("Image is not active: $imageUid")
        return image
    }

    private fun resolveExtension(filename: String, contentType: String?): String {
        val idx = filename.lastIndexOf('.')
        if (idx > 0 && idx < filename.length - 1) return filename.substring(idx + 1).lowercase()
        return when (contentType?.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }
}
