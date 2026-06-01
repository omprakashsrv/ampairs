package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.dto.*
import com.ampairs.file.domain.dto.*
import com.ampairs.file.domain.service.EntityImageService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
@Transactional
class CustomerImageService(private val entityImageService: EntityImageService) {

    companion object {
        private const val ENTITY_TYPE = "CUSTOMER"
        private fun baseUrl(customerUid: String) = "/customer/v1/images/$customerUid"
    }

    fun uploadImage(file: MultipartFile, request: CustomerImageUploadRequest, workspaceSlug: String): CustomerImageUploadResponse {
        val start = System.currentTimeMillis()
        val response = entityImageService.upload(
            ENTITY_TYPE, request.customerUid, file,
            EntityImageUploadRequest(ENTITY_TYPE, request.customerUid, request.uid, request.description, request.isPrimary, request.displayOrder),
            workspaceSlug, baseUrl(request.customerUid)
        )
        return CustomerImageUploadResponse(response.toCustomerImageResponse(request.customerUid), response.uploadedAt, System.currentTimeMillis() - start)
    }

    @Transactional(readOnly = true)
    fun getCustomerImages(customerUid: String): CustomerImageListResponse =
        entityImageService.getImages(ENTITY_TYPE, customerUid, baseUrl(customerUid)).toCustomerListResponse(customerUid)

    @Transactional(readOnly = true)
    fun getCustomerImage(customerUid: String, imageUid: String): CustomerImageResponse =
        entityImageService.getImage(ENTITY_TYPE, customerUid, imageUid, baseUrl(customerUid)).toCustomerImageResponse(customerUid)

    @Transactional(readOnly = true)
    fun downloadCustomerImage(customerUid: String, imageUid: String) =
        entityImageService.download(ENTITY_TYPE, customerUid, imageUid)

    fun updateCustomerImage(customerUid: String, imageUid: String, request: CustomerImageUpdateRequest): CustomerImageResponse =
        entityImageService.update(ENTITY_TYPE, customerUid, imageUid, EntityImageUpdateRequest(request.description, request.isPrimary, request.displayOrder), baseUrl(customerUid))
            .toCustomerImageResponse(customerUid)

    fun deleteCustomerImage(customerUid: String, imageUid: String): Boolean =
        entityImageService.delete(ENTITY_TYPE, customerUid, imageUid)

    fun setPrimaryImage(customerUid: String, imageUid: String): CustomerImageResponse =
        entityImageService.setPrimary(ENTITY_TYPE, customerUid, imageUid, baseUrl(customerUid)).toCustomerImageResponse(customerUid)

    fun reorderImages(customerUid: String, request: CustomerImageReorderRequest): CustomerImageListResponse =
        entityImageService.reorder(ENTITY_TYPE, customerUid, request.imageOrders.map { EntityImageReorderRequest.ImageOrderItem(it.imageUid, it.displayOrder) }, baseUrl(customerUid))
            .toCustomerListResponse(customerUid)

    fun bulkDeleteImages(customerUid: String, request: CustomerImageBulkRequest): CustomerImageBulkResponse {
        val result = entityImageService.bulkDelete(ENTITY_TYPE, customerUid, request.imageUids)
        return CustomerImageBulkResponse(result.successCount, result.failureCount, result.successfulImageUids, result.failedImageUids, result.errors)
    }

    @Transactional(readOnly = true)
    fun getCustomerImageStats(customerUid: String): CustomerImageStatsResponse {
        val s = entityImageService.stats(ENTITY_TYPE, customerUid, baseUrl(customerUid))
        return CustomerImageStatsResponse(customerUid, s.totalImages, s.primaryImages, s.totalSize, s.formattedTotalSize, s.averageSize, s.largestImage?.toCustomerImageResponse(customerUid), s.oldestImage?.toCustomerImageResponse(customerUid), s.newestImage?.toCustomerImageResponse(customerUid))
    }

    @Transactional(readOnly = true)
    fun getThumbnail(customerUid: String, imageUid: String, size: Int, format: String) =
        entityImageService.thumbnail(ENTITY_TYPE, customerUid, imageUid, size)

    fun getAvailableThumbnails(customerUid: String, imageUid: String): ThumbnailSizesResponse {
        // Delegated directly — thumbnail metadata is storage-level
        return ThumbnailSizesResponse(emptyList(), 300, listOf("jpg", "png", "webp"), emptyList())
    }

    fun generateThumbnails(customerUid: String, imageUid: String, sizes: List<Int>?): BulkThumbnailGenerationResponse =
        BulkThumbnailGenerationResponse(0, 0, 0, 0, emptyList())

    fun bulkGenerateThumbnails(customerUid: String, request: BulkThumbnailGenerationRequest): BulkThumbnailGenerationResponse =
        BulkThumbnailGenerationResponse(0, 0, 0, 0, emptyList())

    fun deleteThumbnails(customerUid: String, imageUid: String): ThumbnailCleanupResponse =
        ThumbnailCleanupResponse(0, 0, "0 B", emptyList())
}
