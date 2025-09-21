package com.ampairs.tax.service

import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.enums.GeographicalZone
import com.ampairs.tax.domain.enums.TaxComponentType
import com.ampairs.tax.domain.model.TaxRate
import com.ampairs.tax.repository.TaxRateRepository
import com.ampairs.tax.repository.HsnCodeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional
class TaxRateService(
    private val taxRateRepository: TaxRateRepository,
    private val hsnCodeRepository: HsnCodeRepository
) {

    fun searchTaxRates(
        hsnCode: String? = null,
        businessType: BusinessType? = null,
        componentType: TaxComponentType? = null,
        isActive: Boolean = true,
        effectiveDate: LocalDate = LocalDate.now(),
        searchTerm: String? = null,
        pageable: Pageable
    ): Page<TaxRate> {

        // Get all tax rates and filter in memory for now
        // In production, this should be optimized with a custom query
        val allRates = taxRateRepository.findAll()

        val filteredRates = allRates.filter { rate ->
            var matches = true

            if (isActive && !rate.active) matches = false
            if (!rate.isValidForDate(effectiveDate.atStartOfDay())) matches = false
            if (businessType != null && rate.businessType != businessType) matches = false
            if (componentType != null && rate.taxComponentType != componentType) matches = false

            if (hsnCode != null) {
                val hsnEntity = hsnCodeRepository.findByHsnCodeAndActiveTrue(hsnCode)
                if (hsnEntity == null || rate.hsnCodeId != hsnEntity.id) matches = false
            }

            if (searchTerm != null && !searchTerm.isBlank()) {
                val searchLower = searchTerm.lowercase()
                val matchesDescription = rate.description?.lowercase()?.contains(searchLower) == true
                val matchesNotification = rate.notificationNumber?.lowercase()?.contains(searchLower) == true
                val matchesSource = rate.sourceReference?.lowercase()?.contains(searchLower) == true

                if (!matchesDescription && !matchesNotification && !matchesSource) {
                    matches = false
                }
            }

            matches
        }

        // Simple pagination implementation
        val start = (pageable.pageNumber * pageable.pageSize).coerceAtMost(filteredRates.size)
        val end = ((pageable.pageNumber + 1) * pageable.pageSize).coerceAtMost(filteredRates.size)
        val pageContent = if (start <= end) filteredRates.subList(start, end) else emptyList()

        return PageImpl(pageContent, pageable, filteredRates.size.toLong())
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): TaxRate? {
        return taxRateRepository.findById(id).orElse(null)
    }

    @Transactional(readOnly = true)
    fun findByUid(uid: String): TaxRate? {
        return taxRateRepository.findByUidAndActiveTrue(uid)
    }

    @Transactional(readOnly = true)
    fun findByHsnCodeAndBusinessType(
        hsnCode: String,
        businessType: BusinessType? = null,
        effectiveDate: LocalDate = LocalDate.now()
    ): List<TaxRate> {
        return if (businessType != null) {
            taxRateRepository.findEffectiveTaxRatesByHsnCode(hsnCode, businessType, null, effectiveDate)
        } else {
            // Get all business types for this HSN code
            val rates = mutableListOf<TaxRate>()
            BusinessType.values().forEach { bt ->
                rates.addAll(taxRateRepository.findEffectiveTaxRatesByHsnCode(hsnCode, bt, null, effectiveDate))
            }
            rates.distinctBy { "${it.hsnCodeId}-${it.businessType}-${it.taxComponentType}" }
        }
    }

    @Transactional(readOnly = true)
    fun findEffectiveTaxRate(
        hsnCode: String,
        businessType: BusinessType,
        componentType: TaxComponentType? = null,
        effectiveDate: LocalDate = LocalDate.now()
    ): TaxRate? {
        val hsnEntity = hsnCodeRepository.findByHsnCodeAndActiveTrue(hsnCode) ?: return null

        return if (componentType != null) {
            taxRateRepository.findEffectiveTaxRate(hsnEntity.id, businessType, componentType, null, effectiveDate)
        } else {
            // Return the first effective rate found (prefer IGST, then CGST+SGST)
            val preferredOrder = listOf(TaxComponentType.IGST, TaxComponentType.CGST, TaxComponentType.SGST)
            preferredOrder.firstNotNullOfOrNull { component ->
                taxRateRepository.findEffectiveTaxRate(hsnEntity.id, businessType, component, null, effectiveDate)
            }
        }
    }

    fun createTaxRate(taxRate: TaxRate): TaxRate {
        // Validate HSN code exists
        val hsnCode = hsnCodeRepository.findById(taxRate.hsnCodeId).orElse(null)
            ?: throw IllegalArgumentException("HSN code not found with ID: ${taxRate.hsnCodeId}")

        // Check for overlapping tax rates
        val existingRate = taxRateRepository.findEffectiveTaxRate(
            taxRate.hsnCodeId,
            taxRate.businessType,
            taxRate.taxComponentType,
            taxRate.geographicalZone,
            taxRate.effectiveFrom
        )

        if (existingRate != null) {
            throw IllegalArgumentException(
                "Overlapping tax rate already exists for HSN: ${hsnCode.hsnCode}, " +
                "Business Type: ${taxRate.businessType}, Component: ${taxRate.taxComponentType}"
            )
        }

        return taxRateRepository.save(taxRate)
    }

    fun updateTaxRate(taxRate: TaxRate): TaxRate {
        val existingRate = taxRateRepository.findById(taxRate.id).orElse(null)
            ?: throw NoSuchElementException("Tax rate not found with ID: ${taxRate.id}")

        // Update only allowed fields
        existingRate.apply {
            ratePercentage = taxRate.ratePercentage
            fixedAmountPerUnit = taxRate.fixedAmountPerUnit
            minimumAmount = taxRate.minimumAmount
            maximumAmount = taxRate.maximumAmount
            effectiveTo = taxRate.effectiveTo
            versionNumber = taxRate.versionNumber
            notificationNumber = taxRate.notificationNumber
            notificationDate = taxRate.notificationDate
            conditions = taxRate.conditions
            exemptionRules = taxRate.exemptionRules
            isReverseChargeApplicable = taxRate.isReverseChargeApplicable
            isCompositionSchemeApplicable = taxRate.isCompositionSchemeApplicable
            description = taxRate.description
            sourceReference = taxRate.sourceReference
            active = taxRate.active
        }

        return taxRateRepository.save(existingRate)
    }

    fun deactivateTaxRate(id: Long) {
        val taxRate = taxRateRepository.findById(id).orElse(null)
            ?: throw IllegalArgumentException("Tax rate not found with ID: $id")

        taxRate.active = false
        taxRateRepository.save(taxRate)
    }

    fun updateTaxRateByUid(uid: String, updateDto: com.ampairs.tax.domain.dto.TaxRateUpdateDto): TaxRate {
        val existingRate = findByUid(uid)
            ?: throw NoSuchElementException("Tax rate not found with UID: $uid")

        // Update only allowed fields
        existingRate.apply {
            ratePercentage = updateDto.ratePercentage
            fixedAmountPerUnit = updateDto.fixedAmountPerUnit
            minimumAmount = updateDto.minimumAmount
            maximumAmount = updateDto.maximumAmount
            effectiveTo = updateDto.effectiveTo
            versionNumber = updateDto.versionNumber
            notificationNumber = updateDto.notificationNumber
            notificationDate = updateDto.notificationDate
            conditions = updateDto.conditions
            exemptionRules = updateDto.exemptionRules
            isReverseChargeApplicable = updateDto.isReverseChargeApplicable
            isCompositionSchemeApplicable = updateDto.isCompositionSchemeApplicable
            description = updateDto.description
            sourceReference = updateDto.sourceReference
            active = updateDto.isActive
        }

        return taxRateRepository.save(existingRate)
    }

    fun deactivateTaxRateByUid(uid: String) {
        val taxRate = findByUid(uid)
            ?: throw IllegalArgumentException("Tax rate not found with UID: $uid")

        taxRate.active = false
        taxRateRepository.save(taxRate)
    }

    @Transactional(readOnly = true)
    fun getTaxRateStatistics(): TaxRateStatistics {
        val activeRates = taxRateRepository.findAll().filter { it.active }

        val ratesByComponentType = activeRates.groupBy { it.taxComponentType }
            .mapValues { it.value.size }

        val ratesByBusinessType = activeRates.groupBy { it.businessType }
            .mapValues { it.value.size }

        val nonZeroRates = activeRates.filter { it.ratePercentage > BigDecimal.ZERO }
        val averageRate = if (nonZeroRates.isNotEmpty()) {
            nonZeroRates.map { it.ratePercentage }.reduce { acc, rate -> acc.add(rate) }
                .divide(BigDecimal(nonZeroRates.size), 4, BigDecimal.ROUND_HALF_UP)
        } else BigDecimal.ZERO

        return TaxRateStatistics(
            totalActiveTaxRates = activeRates.size.toLong(),
            ratesByComponentType = ratesByComponentType,
            ratesByBusinessType = ratesByBusinessType,
            averageGstRate = averageRate,
            highestRate = nonZeroRates.maxOfOrNull { it.ratePercentage } ?: BigDecimal.ZERO,
            lowestRate = nonZeroRates.minOfOrNull { it.ratePercentage } ?: BigDecimal.ZERO,
            ratesWithCess = activeRates.count { it.fixedAmountPerUnit != null },
            reverseChargeRates = activeRates.count { it.isReverseChargeApplicable }
        )
    }

    @Transactional(readOnly = true)
    fun findRecentlyAdded(fromDate: LocalDate): List<TaxRate> {
        return taxRateRepository.findByActiveTrueAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            fromDate.atStartOfDay()
        )
    }
}

data class TaxRateStatistics(
    val totalActiveTaxRates: Long,
    val ratesByComponentType: Map<TaxComponentType, Int>,
    val ratesByBusinessType: Map<BusinessType, Int>,
    val averageGstRate: BigDecimal,
    val highestRate: BigDecimal,
    val lowestRate: BigDecimal,
    val ratesWithCess: Int,
    val reverseChargeRates: Int
)