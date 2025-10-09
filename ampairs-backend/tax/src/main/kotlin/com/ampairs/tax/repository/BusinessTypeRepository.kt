package com.ampairs.tax.repository

import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.model.BusinessTypeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BusinessTypeRepository : JpaRepository<BusinessTypeEntity, Long> {

    fun findByBusinessTypeAndActiveTrue(businessType: BusinessType): BusinessTypeEntity?

    fun findByActiveTrueOrderByDisplayName(): List<BusinessTypeEntity>

    @Query("""
        SELECT bt FROM BusinessTypeEntity bt
        WHERE bt.active = true
        AND (:searchTerm IS NULL OR (
            LOWER(bt.displayName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
            LOWER(bt.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ))
        ORDER BY bt.displayName
    """)
    fun searchActiveBusinessTypes(@Param("searchTerm") searchTerm: String?): List<BusinessTypeEntity>

    fun findByCompositionSchemeRateIsNotNullAndActiveTrueOrderByDisplayName(): List<BusinessTypeEntity>

    fun findByTurnoverThresholdIsNotNullAndActiveTrueOrderByTurnoverThreshold(): List<BusinessTypeEntity>

    fun countByActiveTrue(): Long
}