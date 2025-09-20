package com.ampairs.tax.service

import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.enums.GeographicalZone
import com.ampairs.tax.domain.model.TaxConfiguration
import com.ampairs.tax.repository.BusinessTypeRepository
import com.ampairs.tax.repository.HsnCodeRepository
import com.ampairs.tax.repository.TaxConfigurationRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class TaxConfigurationService(
    private val taxConfigurationRepository: TaxConfigurationRepository,
    private val hsnCodeRepository: HsnCodeRepository,
    private val businessTypeRepository: BusinessTypeRepository
) {

    fun findEffectiveConfiguration(
        businessType: BusinessType,
        hsnCode: String,
        geographicalZone: GeographicalZone? = null,
        effectiveDate: LocalDate = LocalDate.now()
    ): TaxConfiguration? {
        return taxConfigurationRepository.findEffectiveConfigurationByBusinessTypeAndHsnCode(
            businessType = businessType,
            hsnCode = hsnCode,
            geographicalZone = geographicalZone,
            effectiveDate = effectiveDate
        )
    }

    fun findAllEffectiveConfigurationsByHsnCode(
        hsnCode: String,
        effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxConfiguration> {
        val hsnCodeEntity = hsnCodeRepository.findByHsnCodeAndActiveTrue(hsnCode)
            ?: throw IllegalArgumentException("HSN code $hsnCode not found")

        return taxConfigurationRepository.findAllEffectiveConfigurationsByHsnCode(
            hsnCodeId = hsnCodeEntity.id,
            effectiveDate = effectiveDate
        )
    }

    fun findAllEffectiveConfigurationsByBusinessType(
        businessType: BusinessType,
        effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxConfiguration> {
        val businessTypeEntity = businessTypeRepository.findByBusinessTypeAndActiveTrue(businessType)
            ?: throw IllegalArgumentException("Business type $businessType not found")

        return taxConfigurationRepository.findAllEffectiveConfigurationsByBusinessType(
            businessTypeId = businessTypeEntity.id,
            effectiveDate = effectiveDate
        )
    }

    fun findByGeographicalZone(
        geographicalZone: GeographicalZone,
        effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxConfiguration> {
        return taxConfigurationRepository.findByGeographicalZoneAndActiveOnDate(geographicalZone, effectiveDate)
    }

    fun findReverseChargeApplicableConfigurations(
        effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxConfiguration> {
        return taxConfigurationRepository.findReverseChargeApplicableConfigurations(effectiveDate)
    }

    fun findCompositionSchemeConfigurations(
        effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxConfiguration> {
        return taxConfigurationRepository.findCompositionSchemeConfigurations(effectiveDate)
    }

    fun findDistinctActiveGstRates(effectiveDate: LocalDate = LocalDate.now()): List<java.math.BigDecimal> {
        return taxConfigurationRepository.findDistinctActiveGstRates(effectiveDate)
    }

    fun findByGstRate(
        gstRate: java.math.BigDecimal,
        effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxConfiguration> {
        return taxConfigurationRepository.findByGstRateAndActiveOnDate(gstRate, effectiveDate)
    }

    fun findConfigurationsWithCess(effectiveDate: LocalDate = LocalDate.now()): List<TaxConfiguration> {
        return taxConfigurationRepository.findConfigurationsWithCess(effectiveDate)
    }

    fun findConfigurationsWithFixedCess(effectiveDate: LocalDate = LocalDate.now()): List<TaxConfiguration> {
        return taxConfigurationRepository.findConfigurationsWithFixedCess(effectiveDate)
    }

    fun findConfigurationsExpiringInPeriod(fromDate: LocalDate, toDate: LocalDate): List<TaxConfiguration> {
        return taxConfigurationRepository.findConfigurationsExpiringInPeriod(fromDate, toDate)
    }

    fun findConfigurationsEffectiveInPeriod(fromDate: LocalDate, toDate: LocalDate): List<TaxConfiguration> {
        return taxConfigurationRepository.findConfigurationsEffectiveInPeriod(fromDate, toDate)
    }

    fun findByNotificationReference(notificationReference: String): List<TaxConfiguration> {
        return taxConfigurationRepository.findByNotificationReferenceOrderByEffectiveFromDesc(notificationReference)
    }

    fun searchConfigurations(searchTerm: String?, pageable: Pageable): Page<TaxConfiguration> {
        return taxConfigurationRepository.searchActiveConfigurations(searchTerm, pageable)
    }

    fun findConfigurationsWithFilters(
        businessType: BusinessType? = null,
        hsnCode: String? = null,
        geographicalZone: GeographicalZone? = null,
        effectiveDate: LocalDate = LocalDate.now(),
        pageable: Pageable
    ): Page<TaxConfiguration> {
        return taxConfigurationRepository.findConfigurationsWithFilters(
            businessType = businessType,
            hsnCode = hsnCode,
            geographicalZone = geographicalZone,
            effectiveDate = effectiveDate,
            pageable = pageable
        )
    }

    fun countActiveConfigurations(): Long {
        return taxConfigurationRepository.countByActiveTrue()
    }

    fun findRecentlyAdded(fromDate: LocalDateTime): List<TaxConfiguration> {
        return taxConfigurationRepository.findByActiveTrueAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(fromDate)
    }

    fun findRecentlyUpdated(fromDate: LocalDateTime): List<TaxConfiguration> {
        return taxConfigurationRepository.findRecentlyUpdatedConfigurations(fromDate)
    }

    fun findByLastUpdatedBy(userId: String): List<TaxConfiguration> {
        return taxConfigurationRepository.findByLastUpdatedByAndActiveTrueOrderByUpdatedAtDesc(userId)
    }

    @Transactional
    fun createTaxConfiguration(taxConfiguration: TaxConfiguration): TaxConfiguration {
        validateTaxConfiguration(taxConfiguration)
        return taxConfigurationRepository.save(taxConfiguration)
    }

    @Transactional
    fun updateTaxConfiguration(taxConfiguration: TaxConfiguration): TaxConfiguration {
        validateTaxConfiguration(taxConfiguration)
        return taxConfigurationRepository.save(taxConfiguration)
    }

    @Transactional
    fun deactivateTaxConfiguration(configId: Long) {
        val taxConfiguration = taxConfigurationRepository.findById(configId)
            .orElseThrow { IllegalArgumentException("Tax configuration not found with id: $configId") }

        taxConfiguration.active = false
        taxConfigurationRepository.save(taxConfiguration)
    }

    @Transactional
    fun expireTaxConfiguration(configId: Long, effectiveTo: LocalDate) {
        val taxConfiguration = taxConfigurationRepository.findById(configId)
            .orElseThrow { IllegalArgumentException("Tax configuration not found with id: $configId") }

        taxConfiguration.effectiveTo = effectiveTo
        taxConfigurationRepository.save(taxConfiguration)
    }

    private fun validateTaxConfiguration(taxConfiguration: TaxConfiguration) {
        // Validate business type exists
        if (taxConfiguration.businessTypeId == 0L) {
            throw IllegalArgumentException("Business type ID is required")
        }

        // Validate HSN code exists
        if (taxConfiguration.hsnCodeId == 0L) {
            throw IllegalArgumentException("HSN code ID is required")
        }

        // Validate GST rates
        if (taxConfiguration.totalGstRate < java.math.BigDecimal.ZERO) {
            throw IllegalArgumentException("Total GST rate cannot be negative")
        }

        // Validate date range
        if (taxConfiguration.effectiveTo != null &&
            taxConfiguration.effectiveTo!!.isBefore(taxConfiguration.effectiveFrom)) {
            throw IllegalArgumentException("Effective to date cannot be before effective from date")
        }

        // Validate GST component rates sum up correctly for intra-state transactions
        val cgst = taxConfiguration.cgstRate ?: java.math.BigDecimal.ZERO
        val sgst = taxConfiguration.sgstRate ?: java.math.BigDecimal.ZERO
        val utgst = taxConfiguration.utgstRate ?: java.math.BigDecimal.ZERO
        val igst = taxConfiguration.igstRate ?: java.math.BigDecimal.ZERO

        val intraStateTotal = cgst.add(sgst).add(utgst)

        // For intra-state, CGST + SGST/UTGST should equal total GST rate
        // For inter-state, IGST should equal total GST rate
        if (intraStateTotal > java.math.BigDecimal.ZERO &&
            intraStateTotal.compareTo(taxConfiguration.totalGstRate) != 0) {
            throw IllegalArgumentException("CGST + SGST/UTGST rates should equal total GST rate")
        }

        if (igst > java.math.BigDecimal.ZERO &&
            igst.compareTo(taxConfiguration.totalGstRate) != 0) {
            throw IllegalArgumentException("IGST rate should equal total GST rate")
        }

        // Check for overlapping configurations
        checkForOverlappingConfigurations(taxConfiguration)
    }

    private fun checkForOverlappingConfigurations(taxConfiguration: TaxConfiguration) {
        val existingConfig = taxConfigurationRepository.findEffectiveConfiguration(
            businessTypeId = taxConfiguration.businessTypeId,
            hsnCodeId = taxConfiguration.hsnCodeId,
            geographicalZone = taxConfiguration.geographicalZone,
            effectiveDate = taxConfiguration.effectiveFrom
        )

        if (existingConfig != null && existingConfig.id != taxConfiguration.id) {
            throw IllegalArgumentException(
                "Overlapping tax configuration found for the same business type, HSN code, and geographical zone"
            )
        }
    }

    fun getTaxConfigurationStatistics(): TaxConfigurationStatistics {
        val totalActive = countActiveConfigurations()
        val withCess = findConfigurationsWithCess().size
        val withFixedCess = findConfigurationsWithFixedCess().size
        val reverseCharge = findReverseChargeApplicableConfigurations().size
        val compositionScheme = findCompositionSchemeConfigurations().size
        val distinctGstRates = findDistinctActiveGstRates().size

        return TaxConfigurationStatistics(
            totalActiveConfigurations = totalActive,
            configurationsWithCess = withCess,
            configurationsWithFixedCess = withFixedCess,
            reverseChargeConfigurations = reverseCharge,
            compositionSchemeConfigurations = compositionScheme,
            distinctGstRates = distinctGstRates
        )
    }
}

data class TaxConfigurationStatistics(
    val totalActiveConfigurations: Long,
    val configurationsWithCess: Int,
    val configurationsWithFixedCess: Int,
    val reverseChargeConfigurations: Int,
    val compositionSchemeConfigurations: Int,
    val distinctGstRates: Int
)