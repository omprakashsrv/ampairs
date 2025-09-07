package com.ampairs.product.service

import com.ampairs.product.domain.enums.TaxSpec
import com.ampairs.product.domain.model.TaxCode
import com.ampairs.product.domain.model.TaxComponent
import com.ampairs.product.domain.model.TaxComponentType
import com.ampairs.product.domain.model.TaxInfoModel
import com.ampairs.product.repository.TaxCodeRepository
import com.ampairs.product.repository.TaxComponentRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.math.round

/**
 * Service for calculating GST and other taxes according to Indian taxation rules
 */
@Service
class TaxCalculationService(
    private val taxCodeRepository: TaxCodeRepository,
    private val taxComponentRepository: TaxComponentRepository
) {

    /**
     * Calculate complete tax breakdown for a transaction
     */
    fun calculateTax(
        taxCode: String,
        baseAmount: Double,
        buyerStateCode: String,
        sellerStateCode: String,
        buyerGstin: String? = null,
        sellerGstin: String? = null,
        businessType: String? = null,
        transactionDate: LocalDateTime = LocalDateTime.now()
    ): TaxCalculationResult {
        
        val taxCodeEntity = taxCodeRepository.findByCodeAndIsActive(taxCode, true)
            .orElseThrow { IllegalArgumentException("Tax code not found or inactive: $taxCode") }

        if (!taxCodeEntity.isValidForDate(transactionDate)) {
            throw IllegalArgumentException("Tax code not valid for date: $transactionDate")
        }

        val taxSpec = determineTaxSpec(buyerStateCode, sellerStateCode, buyerGstin, sellerGstin)
        val effectiveRate = businessType?.let { taxCodeEntity.getRateForBusinessType(it) } ?: taxCodeEntity.gstRate
        
        val components = calculateTaxComponents(taxCodeEntity, baseAmount, taxSpec, buyerStateCode, sellerStateCode)
        val totalTaxAmount = components.sumOf { it.calculatedAmount ?: 0.0 }

        return TaxCalculationResult(
            taxCode = taxCode,
            taxSpec = taxSpec,
            baseAmount = baseAmount,
            taxComponents = components,
            totalTaxAmount = round(totalTaxAmount * 100) / 100,
            totalAmountIncludingTax = round((baseAmount + totalTaxAmount) * 100) / 100,
            isReverseCharge = taxCodeEntity.isReverseCharge,
            calculatedAt = transactionDate
        )
    }

    /**
     * Calculate tax components based on transaction type
     */
    private fun calculateTaxComponents(
        taxCode: TaxCode,
        baseAmount: Double,
        taxSpec: TaxSpec,
        buyerStateCode: String,
        sellerStateCode: String
    ): List<TaxInfoModel> {
        
        return taxCode.calculateTaxComponents(baseAmount, taxSpec, buyerStateCode, sellerStateCode)
    }

    /**
     * Determine tax specification based on state codes and GSTIN
     */
    private fun determineTaxSpec(
        buyerStateCode: String,
        sellerStateCode: String,
        buyerGstin: String?,
        sellerGstin: String?
    ): TaxSpec {
        
        // Extract state codes from GSTIN if not provided directly
        val buyerState = buyerGstin?.substring(0, 2) ?: buyerStateCode
        val sellerState = sellerGstin?.substring(0, 2) ?: sellerStateCode

        return when {
            buyerState == sellerState -> TaxSpec.INTRA
            buyerState != sellerState -> TaxSpec.INTER
            else -> TaxSpec.INTER // Default to interstate
        }
    }

    /**
     * Validate GSTIN format and extract state code
     */
    fun validateGstinAndExtractState(gstin: String): GstinValidationResult {
        val gstinRegex = Regex("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")
        
        if (!gstin.matches(gstinRegex)) {
            return GstinValidationResult(false, null, "Invalid GSTIN format")
        }

        val stateCode = gstin.substring(0, 2)
        val stateCodeInt = stateCode.toIntOrNull()
        
        if (stateCodeInt == null || stateCodeInt !in 1..37) {
            return GstinValidationResult(false, null, "Invalid state code in GSTIN")
        }

        return GstinValidationResult(true, stateCode, "Valid GSTIN")
    }

    /**
     * Get applicable tax codes for a business type
     */
    fun getTaxCodesForBusinessType(businessType: String): List<TaxCode> {
        return taxCodeRepository.findByBusinessType(businessType)
    }

    /**
     * Get tax codes by category
     */
    fun getTaxCodesByCategory(category: String): List<TaxCode> {
        return taxCodeRepository.findByCategory(category)
    }

    /**
     * Get tax codes by GST rate range
     */
    fun getTaxCodesByRateRange(minRate: Double, maxRate: Double): List<TaxCode> {
        return taxCodeRepository.findByGstRateRange(minRate, maxRate)
    }

    /**
     * Get all tax categories
     */
    fun getAllTaxCategories(): List<String> {
        return taxCodeRepository.findDistinctCategories()
    }

    /**
     * Calculate composition scheme tax
     */
    fun calculateCompositionTax(
        taxCode: String,
        turnover: Double,
        businessType: String
    ): CompositionTaxResult {
        
        val taxCodeEntity = taxCodeRepository.findByCodeAndIsActive(taxCode, true)
            .orElseThrow { IllegalArgumentException("Tax code not found: $taxCode") }

        if (!taxCodeEntity.isCompositionApplicable) {
            throw IllegalArgumentException("Tax code not applicable for composition scheme: $taxCode")
        }

        val compositionRate = when (businessType.uppercase()) {
            "TRADER" -> 1.0
            "MANUFACTURER" -> 2.0
            "RESTAURANT" -> 5.0
            else -> taxCodeEntity.businessTypeRates["COMPOSITION"] ?: 1.0
        }

        val taxAmount = turnover * compositionRate / 100

        return CompositionTaxResult(
            taxCode = taxCode,
            businessType = businessType,
            turnover = turnover,
            compositionRate = compositionRate,
            taxAmount = round(taxAmount * 100) / 100
        )
    }

    companion object {
        /**
         * Indian state codes for GST
         */
        val STATE_CODES = mapOf(
            "01" to "Jammu and Kashmir", "02" to "Himachal Pradesh", "03" to "Punjab",
            "04" to "Chandigarh", "05" to "Uttarakhand", "06" to "Haryana",
            "07" to "Delhi", "08" to "Rajasthan", "09" to "Uttar Pradesh",
            "10" to "Bihar", "11" to "Sikkim", "12" to "Arunachal Pradesh",
            "13" to "Nagaland", "14" to "Manipur", "15" to "Mizoram",
            "16" to "Tripura", "17" to "Meghalaya", "18" to "Assam",
            "19" to "West Bengal", "20" to "Jharkhand", "21" to "Odisha",
            "22" to "Chhattisgarh", "23" to "Madhya Pradesh", "24" to "Gujarat",
            "25" to "Daman and Diu", "26" to "Dadra and Nagar Haveli", "27" to "Maharashtra",
            "28" to "Andhra Pradesh", "29" to "Karnataka", "30" to "Goa",
            "31" to "Lakshadweep", "32" to "Kerala", "33" to "Tamil Nadu",
            "34" to "Puducherry", "35" to "Andaman and Nicobar Islands", "36" to "Telangana",
            "37" to "Andhra Pradesh"
        )
    }
}

/**
 * Result of tax calculation
 */
data class TaxCalculationResult(
    val taxCode: String,
    val taxSpec: TaxSpec,
    val baseAmount: Double,
    val taxComponents: List<TaxInfoModel>,
    val totalTaxAmount: Double,
    val totalAmountIncludingTax: Double,
    val isReverseCharge: Boolean,
    val calculatedAt: LocalDateTime
)

/**
 * Result of GSTIN validation
 */
data class GstinValidationResult(
    val isValid: Boolean,
    val stateCode: String?,
    val message: String
)

/**
 * Result of composition scheme tax calculation
 */
data class CompositionTaxResult(
    val taxCode: String,
    val businessType: String,
    val turnover: Double,
    val compositionRate: Double,
    val taxAmount: Double
)