package com.ampairs.product.repository

import com.ampairs.product.domain.enums.TaxType
import com.ampairs.product.domain.model.TaxCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import java.time.LocalDateTime
import java.util.*

interface TaxCodeRepository : CrudRepository<TaxCode, Long>, PagingAndSortingRepository<TaxCode, Long> {
    fun findByUid(uid: String?): TaxCode?
    fun findByRefId(refId: String?): TaxCode?
    fun findByCode(code: String?): TaxCode?
    fun findByCodeAndActive(code: String, active: Boolean): Optional<TaxCode>
    fun findByType(type: TaxType): List<TaxCode>
    fun findByActive(active: Boolean): List<TaxCode>
    fun findByCategory(category: String): List<TaxCode>

    @Query("SELECT tc FROM tax_code tc WHERE tc.code ILIKE %:searchTerm% OR tc.description ILIKE %:searchTerm%")
    fun searchTaxCodes(searchTerm: String, pageable: Pageable): Page<TaxCode>

    @Query("SELECT tc FROM tax_code tc WHERE tc.gstRate = :rate AND tc.active = true")
    fun findByGstRate(rate: Double): List<TaxCode>

    @Query("SELECT tc FROM tax_code tc WHERE tc.gstRate BETWEEN :minRate AND :maxRate AND tc.active = true")
    fun findByGstRateRange(minRate: Double, maxRate: Double): List<TaxCode>

    @Query("SELECT tc FROM tax_code tc WHERE tc.active = true AND (tc.validFrom IS NULL OR tc.validFrom <= :date) AND (tc.validTo IS NULL OR tc.validTo > :date)")
    fun findActiveForDate(date: LocalDateTime): List<TaxCode>

    @Query("SELECT tc FROM tax_code tc WHERE tc.type = :type AND tc.active = true ORDER BY tc.gstRate")
    fun findActiveByTypeOrderByRate(type: TaxType): List<TaxCode>

    @Query("SELECT tc FROM tax_code tc WHERE tc.isCompositionApplicable = true AND tc.active = true")
    fun findCompositionApplicable(): List<TaxCode>

    @Query("SELECT tc FROM tax_code tc WHERE tc.isReverseCharge = true AND tc.active = true")
    fun findReverseChargeApplicable(): List<TaxCode>

    @Query("SELECT DISTINCT tc.category FROM tax_code tc WHERE tc.category IS NOT NULL AND tc.active = true")
    fun findDistinctCategories(): List<String>

    @Query("SELECT tc FROM tax_code tc WHERE JSON_EXTRACT(tc.businessTypeRates, '$.\":businessType\"') IS NOT NULL AND tc.active = true")
    fun findByBusinessType(businessType: String): List<TaxCode>

    @Query("SELECT COUNT(tc) FROM tax_code tc WHERE tc.type = :type AND tc.active = true")
    fun countActiveByType(type: TaxType): Long
}