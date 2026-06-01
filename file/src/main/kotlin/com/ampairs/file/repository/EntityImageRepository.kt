package com.ampairs.file.repository

import com.ampairs.file.domain.model.EntityImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface EntityImageRepository : JpaRepository<EntityImage, Long> {

    @Query("SELECT ei FROM entity_image ei WHERE ei.entityType = :type AND ei.entityUid = :uid AND ei.active = true ORDER BY ei.displayOrder ASC, ei.createdAt ASC")
    fun findActiveByEntity(@Param("type") entityType: String, @Param("uid") entityUid: String): List<EntityImage>

    @Query("SELECT ei FROM entity_image ei WHERE ei.uid = :imageUid AND ei.entityType = :type AND ei.entityUid = :uid")
    fun findByUidAndEntity(
        @Param("imageUid") imageUid: String,
        @Param("type") entityType: String,
        @Param("uid") entityUid: String
    ): EntityImage?

    @Query("SELECT ei FROM entity_image ei WHERE ei.entityType = :type AND ei.entityUid = :uid AND ei.isPrimary = true AND ei.active = true")
    fun findPrimaryByEntity(@Param("type") entityType: String, @Param("uid") entityUid: String): EntityImage?

    @Query("SELECT ei FROM entity_image ei WHERE ei.checksum = :checksum AND ei.entityType = :type AND ei.entityUid = :uid AND ei.active = true")
    fun findByChecksum(
        @Param("checksum") checksum: String,
        @Param("type") entityType: String,
        @Param("uid") entityUid: String
    ): EntityImage?

    @Modifying
    @Transactional
    @Query("UPDATE entity_image ei SET ei.isPrimary = false WHERE ei.entityType = :type AND ei.entityUid = :uid")
    fun clearPrimaryStatus(@Param("type") entityType: String, @Param("uid") entityUid: String)

    @Modifying
    @Transactional
    @Query("UPDATE entity_image ei SET ei.active = false WHERE ei.uid = :imageUid")
    fun softDelete(@Param("imageUid") imageUid: String)

    @Modifying
    @Transactional
    @Query("UPDATE entity_image ei SET ei.displayOrder = :order WHERE ei.uid = :imageUid")
    fun updateDisplayOrder(@Param("imageUid") imageUid: String, @Param("order") displayOrder: Int)

    @Query("SELECT COALESCE(MAX(ei.displayOrder), -1) + 1 FROM entity_image ei WHERE ei.entityType = :type AND ei.entityUid = :uid AND ei.active = true")
    fun getNextDisplayOrder(@Param("type") entityType: String, @Param("uid") entityUid: String): Int

    @Modifying
    @Transactional
    @Query("UPDATE entity_image ei SET ei.active = false WHERE ei.entityType = :type AND ei.entityUid = :uid")
    fun softDeleteAllByEntity(@Param("type") entityType: String, @Param("uid") entityUid: String)
}
