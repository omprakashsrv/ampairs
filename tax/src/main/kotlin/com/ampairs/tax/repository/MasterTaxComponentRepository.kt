package com.ampairs.tax.repository

import com.ampairs.tax.domain.model.MasterTaxComponent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MasterTaxComponentRepository : JpaRepository<MasterTaxComponent, Long> {

    fun findByUid(uid: String): MasterTaxComponent?

    @Query(
        """
        SELECT m FROM MasterTaxComponent m
        WHERE m.isActive = true
        AND (:componentTypeId IS NULL OR m.componentTypeId = :componentTypeId)
        AND (:jurisdiction IS NULL OR m.jurisdiction = :jurisdiction)
        ORDER BY m.ratePercentage ASC
    """
    )
    fun searchComponents(
        @Param("componentTypeId") componentTypeId: String?,
        @Param("jurisdiction") jurisdiction: String?,
        pageable: Pageable
    ): Page<MasterTaxComponent>

    fun findByIsActiveTrue(pageable: Pageable): Page<MasterTaxComponent>

    fun findByComponentTypeIdAndRatePercentage(
        componentTypeId: String,
        ratePercentage: Double
    ): MasterTaxComponent?
}
