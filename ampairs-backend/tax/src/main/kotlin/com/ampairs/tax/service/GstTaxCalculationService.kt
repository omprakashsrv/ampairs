package com.ampairs.tax.service

import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.enums.GeographicalZone
import com.ampairs.tax.domain.enums.TaxComponentType
import com.ampairs.tax.domain.enums.TransactionType
import com.ampairs.tax.domain.model.TaxCalculationResult
import com.ampairs.tax.domain.model.TaxComponent
import com.ampairs.tax.domain.model.TaxConfiguration
import com.ampairs.tax.repository.BusinessTypeRepository
import com.ampairs.tax.repository.HsnCodeRepository
import com.ampairs.tax.repository.TaxConfigurationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class GstTaxCalculationService(
    private val taxConfigurationRepository: TaxConfigurationRepository,
    private val hsnCodeRepository: HsnCodeRepository,
    private val businessTypeRepository: BusinessTypeRepository
) {

    fun calculateTax(
        hsnCode: String,
        baseAmount: BigDecimal,
        quantity: Int = 1,
        businessType: BusinessType = BusinessType.B2B,
        sourceStateCode: String?,
        destinationStateCode: String?,
        effectiveDate: LocalDate = LocalDate.now()
    ): TaxCalculationResult {

        // Validate inputs
        if (baseAmount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Base amount must be greater than zero")
        }

        if (quantity <= 0) {
            throw IllegalArgumentException("Quantity must be greater than zero")
        }

        // Find HSN code
        val hsnCodeEntity = hsnCodeRepository.findByHsnCodeAndValidForDate(hsnCode, effectiveDate.atStartOfDay())
            ?: throw IllegalArgumentException("HSN code $hsnCode not found or not valid for date $effectiveDate")

        // Find business type entity
        val businessTypeEntity = businessTypeRepository.findByBusinessTypeAndActiveTrue(businessType)
            ?: throw IllegalArgumentException("Business type $businessType not found")

        // Determine transaction type
        val transactionType = determineTransactionType(sourceStateCode, destinationStateCode, businessType)

        // Determine geographical zone
        val geographicalZone = determineGeographicalZone(sourceStateCode, destinationStateCode)

        // Find effective tax configuration
        val taxConfiguration = taxConfigurationRepository.findEffectiveConfigurationByBusinessTypeAndHsnCode(
            businessType = businessType,
            hsnCode = hsnCode,
            geographicalZone = geographicalZone,
            effectiveDate = effectiveDate
        ) ?: throw IllegalArgumentException("No tax configuration found for HSN code $hsnCode and business type $businessType")

        // Calculate tax components
        val taxComponents = calculateTaxComponents(
            taxConfiguration = taxConfiguration,
            baseAmount = baseAmount,
            quantity = quantity,
            transactionType = transactionType,
            businessType = businessType
        )

        // Calculate totals
        val totalTaxAmount = taxComponents.sumOf { it.amount }
        val totalAmount = baseAmount.add(totalTaxAmount)

        // Check for exemptions
        val exemptionApplied = checkExemptions(taxConfiguration, baseAmount, quantity)

        // Generate calculation notes
        val calculationNotes = generateCalculationNotes(taxConfiguration, transactionType, exemptionApplied)

        return TaxCalculationResult(
            baseAmount = baseAmount,
            totalTaxAmount = totalTaxAmount,
            totalAmount = totalAmount,
            hsnCode = hsnCode,
            transactionType = transactionType,
            taxComponents = taxComponents,
            calculationDate = LocalDateTime.now(),
            isReverseChargeApplicable = taxConfiguration.isReverseChargeApplicable,
            exemptionApplied = exemptionApplied,
            calculationNotes = calculationNotes
        )
    }

    fun calculateBulkTax(
        items: List<TaxCalculationRequest>,
        businessType: BusinessType = BusinessType.B2B,
        sourceStateCode: String?,
        destinationStateCode: String?,
        effectiveDate: LocalDate = LocalDate.now()
    ): BulkTaxCalculationResult {

        val results = items.map { item ->
            try {
                calculateTax(
                    hsnCode = item.hsnCode,
                    baseAmount = item.baseAmount,
                    quantity = item.quantity,
                    businessType = businessType,
                    sourceStateCode = sourceStateCode,
                    destinationStateCode = destinationStateCode,
                    effectiveDate = effectiveDate
                )
            } catch (e: Exception) {
                // Create error result
                TaxCalculationResult(
                    baseAmount = item.baseAmount,
                    totalTaxAmount = BigDecimal.ZERO,
                    totalAmount = item.baseAmount,
                    hsnCode = item.hsnCode,
                    transactionType = TransactionType.INTER_STATE,
                    taxComponents = emptyList(),
                    calculationNotes = listOf("Error: ${e.message}")
                )
            }
        }

        val totalBaseAmount = results.sumOf { it.baseAmount }
        val totalTaxAmount = results.sumOf { it.totalTaxAmount }
        val totalAmount = results.sumOf { it.totalAmount }

        return BulkTaxCalculationResult(
            items = results,
            totalBaseAmount = totalBaseAmount,
            totalTaxAmount = totalTaxAmount,
            totalAmount = totalAmount,
            calculationDate = LocalDateTime.now()
        )
    }

    private fun calculateTaxComponents(
        taxConfiguration: TaxConfiguration,
        baseAmount: BigDecimal,
        quantity: Int,
        transactionType: TransactionType,
        businessType: BusinessType
    ): List<TaxComponent> {

        val components = mutableListOf<TaxComponent>()

        // Handle composition scheme
        if (businessType == BusinessType.COMPOSITION && taxConfiguration.compositionRate != null) {
            return listOf(
                TaxComponent(
                    componentType = TaxComponentType.IGST,
                    name = "GST (Composition)",
                    rate = taxConfiguration.compositionRate!!,
                    amount = calculatePercentageAmount(baseAmount, taxConfiguration.compositionRate!!),
                    baseAmount = baseAmount,
                    description = "Composition scheme rate"
                )
            )
        }

        // Calculate GST components based on transaction type
        when (transactionType) {
            TransactionType.INTRA_STATE -> {
                // CGST + SGST
                taxConfiguration.cgstRate?.let { cgstRate ->
                    components.add(
                        TaxComponent(
                            componentType = TaxComponentType.CGST,
                            name = "CGST",
                            rate = cgstRate,
                            amount = calculatePercentageAmount(baseAmount, cgstRate),
                            baseAmount = baseAmount,
                            description = "Central Goods and Services Tax"
                        )
                    )
                }

                taxConfiguration.sgstRate?.let { sgstRate ->
                    components.add(
                        TaxComponent(
                            componentType = TaxComponentType.SGST,
                            name = "SGST",
                            rate = sgstRate,
                            amount = calculatePercentageAmount(baseAmount, sgstRate),
                            baseAmount = baseAmount,
                            description = "State Goods and Services Tax"
                        )
                    )
                }
            }

            TransactionType.UNION_TERRITORY -> {
                // CGST + UTGST
                taxConfiguration.cgstRate?.let { cgstRate ->
                    components.add(
                        TaxComponent(
                            componentType = TaxComponentType.CGST,
                            name = "CGST",
                            rate = cgstRate,
                            amount = calculatePercentageAmount(baseAmount, cgstRate),
                            baseAmount = baseAmount,
                            description = "Central Goods and Services Tax"
                        )
                    )
                }

                taxConfiguration.utgstRate?.let { utgstRate ->
                    components.add(
                        TaxComponent(
                            componentType = TaxComponentType.UTGST,
                            name = "UTGST",
                            rate = utgstRate,
                            amount = calculatePercentageAmount(baseAmount, utgstRate),
                            baseAmount = baseAmount,
                            description = "Union Territory Goods and Services Tax"
                        )
                    )
                }
            }

            TransactionType.INTER_STATE -> {
                // IGST
                taxConfiguration.igstRate?.let { igstRate ->
                    components.add(
                        TaxComponent(
                            componentType = TaxComponentType.IGST,
                            name = "IGST",
                            rate = igstRate,
                            amount = calculatePercentageAmount(baseAmount, igstRate),
                            baseAmount = baseAmount,
                            description = "Integrated Goods and Services Tax"
                        )
                    )
                }
            }

            TransactionType.EXPORT -> {
                // Zero rated - no GST components
            }

            else -> {
                // Default to IGST
                components.add(
                    TaxComponent(
                        componentType = TaxComponentType.IGST,
                        name = "IGST",
                        rate = taxConfiguration.totalGstRate,
                        amount = calculatePercentageAmount(baseAmount, taxConfiguration.totalGstRate),
                        baseAmount = baseAmount,
                        description = "Integrated Goods and Services Tax"
                    )
                )
            }
        }

        // Add cess if applicable
        val effectiveCessRate = taxConfiguration.getEffectiveCessRate()
        if (effectiveCessRate > BigDecimal.ZERO) {
            components.add(
                TaxComponent(
                    componentType = TaxComponentType.CESS,
                    name = "Cess",
                    rate = effectiveCessRate,
                    amount = calculatePercentageAmount(baseAmount, effectiveCessRate),
                    baseAmount = baseAmount,
                    description = "Additional cess"
                )
            )
        }

        // Add fixed cess per unit if applicable
        val effectiveCessAmountPerUnit = taxConfiguration.getEffectiveCessAmountPerUnit()
        if (effectiveCessAmountPerUnit > BigDecimal.ZERO) {
            components.add(
                TaxComponent(
                    componentType = TaxComponentType.CESS,
                    name = "Cess (Per Unit)",
                    rate = BigDecimal.ZERO,
                    amount = effectiveCessAmountPerUnit.multiply(BigDecimal(quantity)),
                    baseAmount = baseAmount,
                    isFixed = true,
                    description = "Fixed cess per unit"
                )
            )
        }

        return components
    }

    private fun determineTransactionType(
        sourceStateCode: String?,
        destinationStateCode: String?,
        businessType: BusinessType
    ): TransactionType {

        if (businessType == BusinessType.EXPORT) {
            return TransactionType.EXPORT
        }

        if (sourceStateCode == null || destinationStateCode == null) {
            return TransactionType.INTER_STATE
        }

        if (sourceStateCode.uppercase() == destinationStateCode.uppercase()) {
            return if (GeographicalZone.isUnionTerritory(destinationStateCode)) {
                TransactionType.UNION_TERRITORY
            } else {
                TransactionType.INTRA_STATE
            }
        }

        return TransactionType.INTER_STATE
    }

    private fun determineGeographicalZone(
        sourceStateCode: String?,
        destinationStateCode: String?
    ): GeographicalZone? {

        val stateCode = destinationStateCode ?: sourceStateCode ?: return null
        return GeographicalZone.getZoneByStateCode(stateCode)
    }

    private fun calculatePercentageAmount(baseAmount: BigDecimal, rate: BigDecimal): BigDecimal {
        return baseAmount.multiply(rate)
            .divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
    }

    private fun checkExemptions(
        taxConfiguration: TaxConfiguration,
        baseAmount: BigDecimal,
        quantity: Int
    ): String? {

        // Check threshold-based exemptions
        taxConfiguration.getThresholdLimit("EXEMPTION_THRESHOLD")?.let { threshold ->
            if (baseAmount <= threshold) {
                return "Amount below exemption threshold"
            }
        }

        // Check quantity-based exemptions
        taxConfiguration.getThresholdLimit("QUANTITY_THRESHOLD")?.let { threshold ->
            if (BigDecimal(quantity) <= threshold) {
                return "Quantity below exemption threshold"
            }
        }

        // Check other exemption criteria
        if (taxConfiguration.isExemptionApplicable("SMALL_BUSINESS", baseAmount)) {
            return "Small business exemption"
        }

        if (taxConfiguration.isExemptionApplicable("ESSENTIAL_GOODS")) {
            return "Essential goods exemption"
        }

        return null
    }

    private fun generateCalculationNotes(
        taxConfiguration: TaxConfiguration,
        transactionType: TransactionType,
        exemptionApplied: String?
    ): List<String> {

        val notes = mutableListOf<String>()

        notes.add("Transaction type: ${transactionType.displayName}")

        if (taxConfiguration.isReverseChargeApplicable) {
            notes.add("Reverse charge applicable - Tax to be paid by recipient")
        }

        if (taxConfiguration.isCompositionSchemeApplicable) {
            notes.add("Eligible for composition scheme")
        }

        exemptionApplied?.let {
            notes.add("Exemption applied: $it")
        }

        taxConfiguration.description?.let {
            notes.add("Note: $it")
        }

        return notes
    }
}

data class TaxCalculationRequest(
    val hsnCode: String,
    val baseAmount: BigDecimal,
    val quantity: Int = 1
)

data class BulkTaxCalculationResult(
    val items: List<TaxCalculationResult>,
    val totalBaseAmount: BigDecimal,
    val totalTaxAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val calculationDate: LocalDateTime
)