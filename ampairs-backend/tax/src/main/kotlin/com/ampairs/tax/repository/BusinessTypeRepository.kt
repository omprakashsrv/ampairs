package com.ampairs.tax.repository

import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.model.BusinessTypeEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface BusinessTypeRepository : JpaRepository<BusinessTypeEntity, Long> {

    @Query("SELECT bt FROM BusinessTypeEntity bt WHERE bt.businessType = :businessType AND bt.active = true")
    fun findByBusinessTypeAndActive(@Param("businessType") businessType: BusinessType): BusinessTypeEntity?

    @Query("SELECT bt FROM BusinessTypeEntity bt WHERE bt.active = true ORDER BY bt.displayName")
    fun findAllActive(): List<BusinessTypeEntity>

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

    @Query("""
        SELECT bt FROM BusinessTypeEntity bt
        WHERE bt.compositionSchemeRate IS NOT NULL
        AND bt.active = true
        ORDER BY bt.displayName
    """)
    fun findCompositionSchemeApplicableTypes(): List<BusinessTypeEntity>

    @Query("""
        SELECT bt FROM BusinessTypeEntity bt
        WHERE bt.turnoverThreshold IS NOT NULL
        AND bt.active = true
        ORDER BY bt.turnoverThreshold
    """)
    fun findTypesWithTurnoverThreshold(): List<BusinessTypeEntity>

    @Query("SELECT COUNT(bt) FROM BusinessTypeEntity bt WHERE bt.active = true")
    fun countActiveBusinessTypes(): Long
}