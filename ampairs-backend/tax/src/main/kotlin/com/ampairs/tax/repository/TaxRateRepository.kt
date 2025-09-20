package com.ampairs.tax.repository

import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.enums.GeographicalZone
import com.ampairs.tax.domain.enums.TaxComponentType
import com.ampairs.tax.domain.model.TaxRate
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
interface TaxRateRepository : JpaRepository<TaxRate, Long> {

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.hsnCodeId = :hsnCodeId
        AND tr.businessType = :businessType
        AND tr.taxComponentType = :componentType
        AND tr.active = true
        AND tr.effectiveFrom <= :effectiveDate
        AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :effectiveDate)
        AND (:geographicalZone IS NULL OR tr.geographicalZone IS NULL OR tr.geographicalZone = :geographicalZone)
        ORDER BY tr.effectiveFrom DESC
    """)
    fun findEffectiveTaxRate(
        @Param("hsnCodeId") hsnCodeId: Long,
        @Param("businessType") businessType: BusinessType,
        @Param("componentType") componentType: TaxComponentType,
        @Param("geographicalZone") geographicalZone: GeographicalZone?,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): TaxRate?

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.hsnCodeId = :hsnCodeId
        AND tr.businessType = :businessType
        AND tr.active = true
        AND tr.effectiveFrom <= :effectiveDate
        AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :effectiveDate)
        AND (:geographicalZone IS NULL OR tr.geographicalZone IS NULL OR tr.geographicalZone = :geographicalZone)
        ORDER BY tr.taxComponentType, tr.effectiveFrom DESC
    """)
    fun findAllEffectiveTaxRates(
        @Param("hsnCodeId") hsnCodeId: Long,
        @Param("businessType") businessType: BusinessType,
        @Param("geographicalZone") geographicalZone: GeographicalZone?,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxRate>

    @Query("""
        SELECT tr FROM TaxRate tr
        INNER JOIN tr.hsnCode h
        WHERE h.hsnCode = :hsnCode
        AND tr.businessType = :businessType
        AND tr.active = true
        AND tr.effectiveFrom <= :effectiveDate
        AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :effectiveDate)
        AND (:geographicalZone IS NULL OR tr.geographicalZone IS NULL OR tr.geographicalZone = :geographicalZone)
        ORDER BY tr.taxComponentType, tr.effectiveFrom DESC
    """)
    fun findEffectiveTaxRatesByHsnCode(
        @Param("hsnCode") hsnCode: String,
        @Param("businessType") businessType: BusinessType,
        @Param("geographicalZone") geographicalZone: GeographicalZone?,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxRate>

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.hsnCodeId = :hsnCodeId
        AND tr.taxComponentType IN :componentTypes
        AND tr.businessType = :businessType
        AND tr.active = true
        AND tr.effectiveFrom <= :effectiveDate
        AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :effectiveDate)
        ORDER BY tr.taxComponentType
    """)
    fun findTaxRatesByComponentTypes(
        @Param("hsnCodeId") hsnCodeId: Long,
        @Param("componentTypes") componentTypes: List<TaxComponentType>,
        @Param("businessType") businessType: BusinessType,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxRate>

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.taxComponentType = :componentType
        AND tr.active = true
        AND tr.effectiveFrom <= :effectiveDate
        AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :effectiveDate)
        ORDER BY tr.ratePercentage DESC
    """)
    fun findByComponentTypeAndActiveOnDate(
        @Param("componentType") componentType: TaxComponentType,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now(),
        pageable: Pageable
    ): Page<TaxRate>

    @Query("""
        SELECT DISTINCT tr.ratePercentage FROM TaxRate tr
        WHERE tr.taxComponentType = :componentType
        AND tr.active = true
        AND tr.effectiveFrom <= :effectiveDate
        AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :effectiveDate)
        ORDER BY tr.ratePercentage
    """)
    fun findDistinctRatesByComponentType(
        @Param("componentType") componentType: TaxComponentType,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): List<java.math.BigDecimal>

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.businessType = :businessType
        AND tr.active = true
        AND tr.effectiveFrom <= :effectiveDate
        AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :effectiveDate)
        ORDER BY tr.hsnCodeId, tr.taxComponentType
    """)
    fun findByBusinessTypeAndActiveOnDate(
        @Param("businessType") businessType: BusinessType,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxRate>

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.geographicalZone = :geographicalZone
        AND tr.active = true
        AND tr.effectiveFrom <= :effectiveDate
        AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :effectiveDate)
        ORDER BY tr.hsnCodeId, tr.taxComponentType
    """)
    fun findByGeographicalZoneAndActiveOnDate(
        @Param("geographicalZone") geographicalZone: GeographicalZone,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxRate>

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.isReverseChargeApplicable = true
        AND tr.active = true
        AND tr.effectiveFrom <= :effectiveDate
        AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :effectiveDate)
        ORDER BY tr.hsnCodeId
    """)
    fun findReverseChargeApplicableRates(@Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()): List<TaxRate>

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.fixedAmountPerUnit IS NOT NULL
        AND tr.active = true
        AND tr.effectiveFrom <= :effectiveDate
        AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :effectiveDate)
        ORDER BY tr.hsnCodeId
    """)
    fun findFixedAmountTaxRates(@Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()): List<TaxRate>

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.effectiveTo IS NOT NULL
        AND tr.effectiveTo BETWEEN :fromDate AND :toDate
        AND tr.active = true
        ORDER BY tr.effectiveTo
    """)
    fun findRatesExpiringInPeriod(
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate
    ): List<TaxRate>

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.effectiveFrom BETWEEN :fromDate AND :toDate
        AND tr.active = true
        ORDER BY tr.effectiveFrom
    """)
    fun findRatesEffectiveInPeriod(
        @Param("fromDate") fromDate: LocalDate,
        @Param("toDate") toDate: LocalDate
    ): List<TaxRate>

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.notificationNumber = :notificationNumber
        ORDER BY tr.effectiveFrom DESC
    """)
    fun findByNotificationNumber(@Param("notificationNumber") notificationNumber: String): List<TaxRate>

    @Query("""
        SELECT tr1 FROM TaxRate tr1
        WHERE tr1.hsnCodeId = :hsnCodeId
        AND tr1.businessType = :businessType
        AND tr1.taxComponentType = :componentType
        AND tr1.isActive = true
        AND tr1.effectiveFrom <= :effectiveDate
        AND (tr1.effectiveTo IS NULL OR tr1.effectiveTo >= :effectiveDate)
        AND tr1.effectiveFrom = (
            SELECT MAX(tr2.effectiveFrom)
            FROM TaxRate tr2
            WHERE tr2.hsnCodeId = tr1.hsnCodeId
            AND tr2.businessType = tr1.businessType
            AND tr2.taxComponentType = tr1.taxComponentType
            AND tr2.isActive = true
            AND tr2.effectiveFrom <= :effectiveDate
            AND (tr2.effectiveTo IS NULL OR tr2.effectiveTo >= :effectiveDate)
        )
    """)
    fun findLatestEffectiveTaxRate(
        @Param("hsnCodeId") hsnCodeId: Long,
        @Param("businessType") businessType: BusinessType,
        @Param("componentType") componentType: TaxComponentType,
        @Param("effectiveDate") effectiveDate: LocalDate = LocalDate.now()
    ): TaxRate?

    @Query("SELECT COUNT(tr) FROM TaxRate tr WHERE tr.active = true")
    fun countActiveTaxRates(): Long

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.active = true
        AND tr.createdAt >= :fromDate
        ORDER BY tr.createdAt DESC
    """)
    fun findRecentlyAddedTaxRates(@Param("fromDate") fromDate: LocalDateTime): List<TaxRate>

    @Query("""
        SELECT tr FROM TaxRate tr
        WHERE tr.active = true
        AND tr.updatedAt >= :fromDate
        AND tr.createdAt < :fromDate
        ORDER BY tr.updatedAt DESC
    """)
    fun findRecentlyUpdatedTaxRates(@Param("fromDate") fromDate: LocalDateTime): List<TaxRate>
}