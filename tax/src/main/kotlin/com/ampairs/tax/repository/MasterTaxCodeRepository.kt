package com.ampairs.tax.repository

import com.ampairs.tax.domain.model.MasterTaxCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MasterTaxCodeRepository : JpaRepository<MasterTaxCode, Long> {

    fun findByUid(uid: String): MasterTaxCode?

    @Query(
        """
        SELECT m FROM MasterTaxCode m
        WHERE m.countryCode = :countryCode
        AND m.isActive = true
        AND (
            LOWER(m.code) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(m.shortDescription) LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND (:codeType IS NULL OR m.codeType = :codeType)
        AND (:category IS NULL OR m.category = :category)
        ORDER BY m.code ASC
    """
    )
    fun searchCodes(
        @Param("query") query: String,
        @Param("countryCode") countryCode: String,
        @Param("codeType") codeType: String?,
        @Param("category") category: String?,
        pageable: Pageable
    ): Page<MasterTaxCode>

    @Query(
        """
        SELECT m FROM MasterTaxCode m
        WHERE m.countryCode = :countryCode
        AND m.isActive = true
        AND (:industry IS NULL OR m.category = :industry)
        ORDER BY m.defaultTaxRate DESC, m.code ASC
    """
    )
    fun findPopularCodes(
        @Param("countryCode") countryCode: String,
        @Param("industry") industry: String?,
        pageable: Pageable
    ): Page<MasterTaxCode>

    fun findByCountryCodeAndCodeTypeAndCode(
        countryCode: String,
        codeType: String,
        code: String
    ): MasterTaxCode?

    fun findByCountryCodeAndIsActiveTrue(countryCode: String, pageable: Pageable): Page<MasterTaxCode>
}
