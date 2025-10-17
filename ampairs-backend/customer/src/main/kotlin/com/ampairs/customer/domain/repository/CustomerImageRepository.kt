package com.ampairs.customer.domain.repository

import com.ampairs.customer.domain.model.CustomerImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

/**
 * Repository for CustomerImage entity with tenant-aware operations
 */
@Repository
interface CustomerImageRepository : JpaRepository<CustomerImage, Long> {

    /**
     * Find all images for a specific customer
     */
    @Query("SELECT ci FROM customer_image ci WHERE ci.customerUid = :customerUid AND ci.active = true ORDER BY ci.displayOrder ASC, ci.createdAt ASC")
    fun findByCustomerUidAndActiveTrue(@Param("customerUid") customerUid: String): List<CustomerImage>

    /**
     * Find all images for a specific customer including inactive ones
     */
    @Query("SELECT ci FROM customer_image ci WHERE ci.customerUid = :customerUid ORDER BY ci.displayOrder ASC, ci.createdAt ASC")
    fun findByCustomerUid(@Param("customerUid") customerUid: String): List<CustomerImage>

    /**
     * Find primary image for a customer
     */
    @Query("SELECT ci FROM customer_image ci WHERE ci.customerUid = :customerUid AND ci.isPrimary = true AND ci.active = true")
    fun findPrimaryByCustomerUid(@Param("customerUid") customerUid: String): CustomerImage?

    /**
     * Find image by UID for a specific customer
     */
    @Query("SELECT ci FROM customer_image ci WHERE ci.uid = :imageUid AND ci.customerUid = :customerUid")
    fun findByUidAndCustomerUid(
        @Param("imageUid") imageUid: String,
        @Param("customerUid") customerUid: String
    ): CustomerImage?

    /**
     * Find image by storage path
     */
    @Query("SELECT ci FROM customer_image ci WHERE ci.storagePath = :storagePath AND ci.active = true")
    fun findByStoragePath(@Param("storagePath") storagePath: String): CustomerImage?

    /**
     * Count active images for a customer
     */
    @Query("SELECT COUNT(ci) FROM customer_image ci WHERE ci.customerUid = :customerUid AND ci.active = true")
    fun countActiveByCustomerUid(@Param("customerUid") customerUid: String): Long

    /**
     * Find all images by workspace slug (for workspace-level operations)
     */
    @Query("SELECT ci FROM customer_image ci WHERE ci.workspaceSlug = :workspaceSlug AND ci.active = true ORDER BY ci.createdAt DESC")
    fun findByWorkspaceSlugAndActiveTrue(@Param("workspaceSlug") workspaceSlug: String): List<CustomerImage>

    /**
     * Update primary image status - set all images for customer as non-primary
     */
    @Modifying
    @Transactional
    @Query("UPDATE customer_image ci SET ci.isPrimary = false WHERE ci.customerUid = :customerUid")
    fun clearPrimaryStatus(@Param("customerUid") customerUid: String)

    /**
     * Set image as primary
     */
    @Modifying
    @Transactional
    @Query("UPDATE customer_image ci SET ci.isPrimary = true WHERE ci.uid = :imageUid")
    fun setPrimary(@Param("imageUid") imageUid: String)

    /**
     * Soft delete image (set active = false)
     */
    @Modifying
    @Transactional
    @Query("UPDATE customer_image ci SET ci.active = false WHERE ci.uid = :imageUid")
    fun softDelete(@Param("imageUid") imageUid: String)

    /**
     * Soft delete all images for a customer
     */
    @Modifying
    @Transactional
    @Query("UPDATE customer_image ci SET ci.active = false WHERE ci.customerUid = :customerUid")
    fun softDeleteByCustomerUid(@Param("customerUid") customerUid: String)

    /**
     * Update display order for images
     */
    @Modifying
    @Transactional
    @Query("UPDATE customer_image ci SET ci.displayOrder = :displayOrder WHERE ci.uid = :imageUid")
    fun updateDisplayOrder(@Param("imageUid") imageUid: String, @Param("displayOrder") displayOrder: Int)

    /**
     * Get next display order for customer images
     */
    @Query("SELECT COALESCE(MAX(ci.displayOrder), -1) + 1 FROM customer_image ci WHERE ci.customerUid = :customerUid AND ci.active = true")
    fun getNextDisplayOrder(@Param("customerUid") customerUid: String): Int

    /**
     * Find all active images across all workspaces
     */
    @Query("SELECT ci FROM customer_image ci WHERE ci.active = true ORDER BY ci.createdAt DESC")
    fun findByActiveTrue(): List<CustomerImage>

    /**
     * Count all active images
     */
    @Query("SELECT COUNT(ci) FROM customer_image ci WHERE ci.active = true")
    fun countByActiveTrue(): Long

    /**
     * Find images uploaded after a specific date
     */
    @Query("SELECT ci FROM customer_image ci WHERE ci.active = true AND ci.uploadedAt > :uploadedAt ORDER BY ci.uploadedAt DESC")
    fun findByActiveTrueAndUploadedAtAfter(@Param("uploadedAt") uploadedAt: java.time.LocalDateTime): List<CustomerImage>
}

/**
 * Data class for storage statistics
 */
data class StorageStats(
    val totalImages: Long,
    val totalSize: Long,
    val primaryImages: Long
)