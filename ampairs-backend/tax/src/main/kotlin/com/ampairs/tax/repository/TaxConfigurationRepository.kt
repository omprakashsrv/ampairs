package com.ampairs.tax.repository

import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.enums.GeographicalZone
import com.ampairs.tax.domain.model.TaxConfiguration
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface TaxConfigurationRepository : JpaRepository<TaxConfiguration, Long> {

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.businessTypeId = :businessTypeId
        AND tc.hsnCodeId = :hsnCodeId
        AND tc.active = true
        AND tc.effectiveFrom <= :effectiveDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :effectiveDate)
        AND (:geographicalZone IS NULL OR tc.geographicalZone IS NULL OR tc.geographicalZone = :geographicalZone)
        ORDER BY tc.effectiveFrom DESC
    """)
    fun findEffectiveConfiguration(
        @Param("businessTypeId") businessTypeId: Long,
        @Param("hsnCodeId") hsnCodeId: Long,
        @Param("geographicalZone") geographicalZone: GeographicalZone?,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): TaxConfiguration?

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        INNER JOIN tc.businessTypeEntity bt
        INNER JOIN tc.hsnCode h
        WHERE bt.businessType = :businessType
        AND h.hsnCode = :hsnCode
        AND tc.active = true
        AND tc.effectiveFrom <= :effectiveDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :effectiveDate)
        AND (:geographicalZone IS NULL OR tc.geographicalZone IS NULL OR tc.geographicalZone = :geographicalZone)
        ORDER BY tc.effectiveFrom DESC
    """)
    fun findEffectiveConfigurationByBusinessTypeAndHsnCode(
        @Param("businessType") businessType: BusinessType,
        @Param("hsnCode") hsnCode: String,
        @Param("geographicalZone") geographicalZone: GeographicalZone?,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): TaxConfiguration?

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.hsnCodeId = :hsnCodeId
        AND tc.active = true
        AND tc.effectiveFrom <= :effectiveDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :effectiveDate)
        ORDER BY tc.businessTypeId
    """)
    fun findAllEffectiveConfigurationsByHsnCode(
        @Param("hsnCodeId") hsnCodeId: Long,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxConfiguration>

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.businessTypeId = :businessTypeId
        AND tc.active = true
        AND tc.effectiveFrom <= :effectiveDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :effectiveDate)
        ORDER BY tc.hsnCodeId
    """)
    fun findAllEffectiveConfigurationsByBusinessType(
        @Param("businessTypeId") businessTypeId: Long,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxConfiguration>

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.geographicalZone = :geographicalZone
        AND tc.active = true
        AND tc.effectiveFrom <= :effectiveDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :effectiveDate)
        ORDER BY tc.businessTypeId, tc.hsnCodeId
    """)
    fun findByGeographicalZoneAndActiveOnDate(
        @Param("geographicalZone") geographicalZone: GeographicalZone,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxConfiguration>

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.isReverseChargeApplicable = true
        AND tc.active = true
        AND tc.effectiveFrom <= :effectiveDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :effectiveDate)
        ORDER BY tc.businessTypeId, tc.hsnCodeId
    """)
    fun findReverseChargeApplicableConfigurations(@Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()): List<TaxConfiguration>

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.compositionRate IS NOT NULL
        AND tc.isCompositionSchemeApplicable = true
        AND tc.active = true
        AND tc.effectiveFrom <= :effectiveDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :effectiveDate)
        ORDER BY tc.businessTypeId, tc.hsnCodeId
    """)
    fun findCompositionSchemeConfigurations(@Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()): List<TaxConfiguration>

    @Query("""
        SELECT DISTINCT tc.totalGstRate FROM TaxConfiguration tc
        WHERE tc.active = true
        AND tc.effectiveFrom <= :effectiveDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :effectiveDate)
        ORDER BY tc.totalGstRate
    """)
    fun findDistinctActiveGstRates(@Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()): List<java.math.BigDecimal>

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.totalGstRate = :gstRate
        AND tc.active = true
        AND tc.effectiveFrom <= :effectiveDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :effectiveDate)
        ORDER BY tc.businessTypeId, tc.hsnCodeId
    """)
    fun findByGstRateAndActiveOnDate(
        @Param("gstRate") gstRate: java.math.BigDecimal,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxConfiguration>

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.cessRate IS NOT NULL
        AND tc.cessRate > 0
        AND tc.active = true
        AND tc.effectiveFrom <= :effectiveDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :effectiveDate)
        ORDER BY tc.cessRate DESC
    """)
    fun findConfigurationsWithCess(@Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()): List<TaxConfiguration>

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.cessAmountPerUnit IS NOT NULL
        AND tc.cessAmountPerUnit > 0
        AND tc.active = true
        AND tc.effectiveFrom <= :effectiveDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :effectiveDate)
        ORDER BY tc.cessAmountPerUnit DESC
    """)
    fun findConfigurationsWithFixedCess(@Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()): List<TaxConfiguration>

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.effectiveTo IS NOT NULL
        AND tc.effectiveTo BETWEEN :fromDate AND :toDate
        AND tc.active = true
        ORDER BY tc.effectiveTo
    """)
    fun findConfigurationsExpiringInPeriod(
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate
    ): List<TaxConfiguration>

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.effectiveFrom BETWEEN :fromDate AND :toDate
        AND tc.active = true
        ORDER BY tc.effectiveFrom
    """)
    fun findConfigurationsEffectiveInPeriod(
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate
    ): List<TaxConfiguration>

    fun findByNotificationReferenceOrderByEffectiveFromDesc(notificationReference: String): List<TaxConfiguration>

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.active = true
        AND (:searchTerm IS NULL OR (
            LOWER(tc.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
            LOWER(tc.notificationReference) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ))
        ORDER BY tc.businessTypeId, tc.hsnCodeId
    """)
    fun searchActiveConfigurations(@Param("searchTerm") searchTerm: String?, pageable: Pageable): Page<TaxConfiguration>

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        INNER JOIN tc.businessTypeEntity bt
        INNER JOIN tc.hsnCode h
        WHERE tc.active = true
        AND tc.effectiveFrom <= :effectiveDate
        AND (tc.effectiveTo IS NULL OR tc.effectiveTo >= :effectiveDate)
        AND (:businessType IS NULL OR bt.businessType = :businessType)
        AND (:hsnCode IS NULL OR h.hsnCode LIKE CONCAT(:hsnCode, '%'))
        AND (:geographicalZone IS NULL OR tc.geographicalZone IS NULL OR tc.geographicalZone = :geographicalZone)
        ORDER BY bt.businessType, h.hsnCode
    """)
    fun findConfigurationsWithFilters(
        @Param("businessType") businessType: BusinessType?,
        @Param("hsnCode") hsnCode: String?,
        @Param("geographicalZone") geographicalZone: GeographicalZone?,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now(),
        pageable: Pageable
    ): Page<TaxConfiguration>

    fun countByActiveTrue(): Long

    fun findByActiveTrueAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(fromDate: LocalDateTime): List<TaxConfiguration>

    @Query("""
        SELECT tc FROM TaxConfiguration tc
        WHERE tc.active = true
        AND tc.updatedAt >= :fromDate
        AND tc.createdAt < :fromDate
        ORDER BY tc.updatedAt DESC
    """)
    fun findRecentlyUpdatedConfigurations(@Param("fromDate") fromDate: LocalDateTime): List<TaxConfiguration>

    fun findByLastUpdatedByAndActiveTrueOrderByUpdatedAtDesc(lastUpdatedBy: String): List<TaxConfiguration>
}