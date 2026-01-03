package com.ampairs.tax.repository

import com.ampairs.tax.domain.model.MasterTaxRule
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MasterTaxRuleRepository : JpaRepository<MasterTaxRule, Long> {

    fun findByUid(uid: String): MasterTaxRule?

    fun findByMasterTaxCodeId(masterTaxCodeId: String): List<MasterTaxRule>

    @Query(
        """
        SELECT m FROM MasterTaxRule m
        WHERE m.countryCode = :countryCode
        AND m.isActive = true
        AND (:taxCodeType IS NULL OR m.taxCodeType = :taxCodeType)
        ORDER BY m.taxCode ASC
    """
    )
    fun searchRules(
        @Param("countryCode") countryCode: String,
        @Param("taxCodeType") taxCodeType: String?,
        pageable: Pageable
    ): Page<MasterTaxRule>

    fun findByCountryCodeAndIsActiveTrue(countryCode: String, pageable: Pageable): Page<MasterTaxRule>

    fun findByIsActiveTrue(pageable: Pageable): Page<MasterTaxRule>
}
