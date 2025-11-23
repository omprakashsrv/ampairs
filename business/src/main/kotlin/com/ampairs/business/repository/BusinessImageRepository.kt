package com.ampairs.business.repository

import com.ampairs.business.model.BusinessImage
import com.ampairs.business.model.BusinessImageType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

/**
 * Repository for BusinessImage entity operations.
 *
 * **Multi-Tenancy**:
 * - BusinessImage extends OwnableBaseDomain with @TenantId on ownerId
 * - All queries automatically filtered by current tenant context
 */
@Repository
interface BusinessImageRepository : CrudRepository<BusinessImage, Long> {

    /**
     * Find image by UID.
     */
    fun findByUid(uid: String): BusinessImage?

    /**
     * Find all images for a business, ordered by displayOrder.
     */
    fun findByBusinessIdAndActiveOrderByDisplayOrderAsc(businessId: String, active: Boolean): List<BusinessImage>

    /**
     * Find all images for a business with pagination.
     */
    fun findByBusinessIdAndActive(businessId: String, active: Boolean, pageable: Pageable): Page<BusinessImage>

    /**
     * Find images by business and type.
     */
    fun findByBusinessIdAndImageTypeAndActiveOrderByDisplayOrderAsc(
        businessId: String,
        imageType: BusinessImageType,
        active: Boolean
    ): List<BusinessImage>

    /**
     * Find the primary image for a business.
     */
    fun findByBusinessIdAndIsPrimaryTrueAndActiveTrue(businessId: String): BusinessImage?

    /**
     * Count images for a business.
     */
    fun countByBusinessIdAndActive(businessId: String, active: Boolean): Long

    /**
     * Check if image exists by UID.
     */
    fun existsByUid(uid: String): Boolean

    /**
     * Get max display order for a business.
     */
    @Query("SELECT COALESCE(MAX(bi.displayOrder), 0) FROM BusinessImage bi WHERE bi.businessId = :businessId")
    fun getMaxDisplayOrder(businessId: String): Int

    /**
     * Clear primary flag for all images of a business (except the specified one).
     */
    @Modifying
    @Query("UPDATE BusinessImage bi SET bi.isPrimary = false WHERE bi.businessId = :businessId AND bi.uid != :excludeUid")
    fun clearPrimaryFlagExcept(businessId: String, excludeUid: String)

    /**
     * Clear all primary flags for a business.
     */
    @Modifying
    @Query("UPDATE BusinessImage bi SET bi.isPrimary = false WHERE bi.businessId = :businessId")
    fun clearAllPrimaryFlags(businessId: String)

    /**
     * Delete all images for a business.
     */
    @Modifying
    fun deleteByBusinessId(businessId: String)
}
