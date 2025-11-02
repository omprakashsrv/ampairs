package com.ampairs.tax.domain.dto

import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.enums.GeographicalZone
import com.ampairs.tax.domain.enums.TaxComponentType
import com.ampairs.tax.domain.model.TaxRate
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

data class TaxRateRequestDto(
    val uid: String? = null, // Optional UID for UPSERT operations

    @field:NotNull(message = "HSN code ID is required")
    val hsnCodeId: Long,

    @field:NotNull(message = "Tax component type is required")
    val taxComponentType: TaxComponentType,

    @field:NotNull(message = "Rate percentage is required")
    @field:DecimalMin(value = "0.0", message = "Rate percentage cannot be negative")
    @field:DecimalMax(value = "100.0", message = "Rate percentage cannot exceed 100%")
    @field:Digits(integer = 3, fraction = 4, message = "Invalid rate percentage format")
    val ratePercentage: BigDecimal,

    @field:DecimalMin(value = "0.0", message = "Fixed amount per unit cannot be negative")
    @field:Digits(integer = 8, fraction = 4, message = "Invalid fixed amount format")
    val fixedAmountPerUnit: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Minimum amount cannot be negative")
    @field:Digits(integer = 8, fraction = 4, message = "Invalid minimum amount format")
    val minimumAmount: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Maximum amount cannot be negative")
    @field:Digits(integer = 8, fraction = 4, message = "Invalid maximum amount format")
    val maximumAmount: BigDecimal? = null,

    @field:NotNull(message = "Business type is required")
    val businessType: BusinessType,

    val geographicalZone: GeographicalZone? = null,

    @field:NotNull(message = "Effective from date is required")
    val effectiveFrom: LocalDate,

    val effectiveTo: LocalDate? = null,

    @field:Min(value = 1, message = "Version number must be at least 1")
    val versionNumber: Int = 1,

    @field:Size(max = 100, message = "Notification number is too long")
    val notificationNumber: String? = null,

    val notificationDate: LocalDate? = null,

    val conditions: Map<String, Any> = emptyMap(),

    val exemptionRules: Map<String, Any> = emptyMap(),

    val isReverseChargeApplicable: Boolean = false,

    val isCompositionSchemeApplicable: Boolean = true,

    @field:Size(max = 1000, message = "Description is too long")
    val description: String? = null,

    @field:Size(max = 255, message = "Source reference is too long")
    val sourceReference: String? = null,

    val isActive: Boolean = true
) {
    fun toEntity(): TaxRate {
        return TaxRate().apply {
            uid?.let { this.uid = it }
            hsnCodeId = this@TaxRateRequestDto.hsnCodeId
            taxComponentType = this@TaxRateRequestDto.taxComponentType
            ratePercentage = this@TaxRateRequestDto.ratePercentage
            fixedAmountPerUnit = this@TaxRateRequestDto.fixedAmountPerUnit
            minimumAmount = this@TaxRateRequestDto.minimumAmount
            maximumAmount = this@TaxRateRequestDto.maximumAmount
            businessType = this@TaxRateRequestDto.businessType
            geographicalZone = this@TaxRateRequestDto.geographicalZone
            effectiveFrom = this@TaxRateRequestDto.effectiveFrom
            effectiveTo = this@TaxRateRequestDto.effectiveTo
            versionNumber = this@TaxRateRequestDto.versionNumber
            notificationNumber = this@TaxRateRequestDto.notificationNumber
            notificationDate = this@TaxRateRequestDto.notificationDate
            conditions = this@TaxRateRequestDto.conditions
            exemptionRules = this@TaxRateRequestDto.exemptionRules
            isReverseChargeApplicable = this@TaxRateRequestDto.isReverseChargeApplicable
            isCompositionSchemeApplicable = this@TaxRateRequestDto.isCompositionSchemeApplicable
            description = this@TaxRateRequestDto.description
            sourceReference = this@TaxRateRequestDto.sourceReference
        }
    }
}

data class TaxRateResponseDto(
    val id: Long,
    val uid: String,
    val hsnCodeId: Long,
    val hsnCode: String?,
    val hsnDescription: String?,
    val taxComponentType: TaxComponentType,
    val ratePercentage: BigDecimal,
    val fixedAmountPerUnit: BigDecimal?,
    val minimumAmount: BigDecimal?,
    val maximumAmount: BigDecimal?,
    val businessType: BusinessType,
    val businessTypeDisplayName: String,
    val geographicalZone: GeographicalZone?,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val versionNumber: Int,
    val notificationNumber: String?,
    val notificationDate: LocalDate?,
    val conditions: Map<String, Any>,
    val exemptionRules: Map<String, Any>,
    val isReverseChargeApplicable: Boolean,
    val isCompositionSchemeApplicable: Boolean,
    val description: String?,
    val sourceReference: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val lastUpdatedBy: String?,
    val isValidForToday: Boolean,
    val daysUntilExpiry: Long?
) {
    companion object {
        fun from(taxRate: TaxRate): TaxRateResponseDto {
            val today = LocalDate.now()
            val daysUntilExpiry = taxRate.effectiveTo?.let {
                java.time.temporal.ChronoUnit.DAYS.between(today, it)
            }

            return TaxRateResponseDto(
                id = taxRate.id,
                uid = taxRate.uid,
                hsnCodeId = taxRate.hsnCodeId,
                hsnCode = taxRate.hsnCode?.hsnCode,
                hsnDescription = taxRate.hsnCode?.hsnDescription,
                taxComponentType = taxRate.taxComponentType,
                ratePercentage = taxRate.ratePercentage,
                fixedAmountPerUnit = taxRate.fixedAmountPerUnit,
                minimumAmount = taxRate.minimumAmount,
                maximumAmount = taxRate.maximumAmount,
                businessType = taxRate.businessType,
                businessTypeDisplayName = taxRate.businessType.displayName,
                geographicalZone = taxRate.geographicalZone,
                effectiveFrom = taxRate.effectiveFrom,
                effectiveTo = taxRate.effectiveTo,
                versionNumber = taxRate.versionNumber,
                notificationNumber = taxRate.notificationNumber,
                notificationDate = taxRate.notificationDate,
                conditions = taxRate.conditions,
                exemptionRules = taxRate.exemptionRules,
                isReverseChargeApplicable = taxRate.isReverseChargeApplicable,
                isCompositionSchemeApplicable = taxRate.isCompositionSchemeApplicable,
                description = taxRate.description,
                sourceReference = taxRate.sourceReference,
                createdAt = taxRate.createdAt,
                updatedAt = taxRate.updatedAt,
                lastUpdatedBy = taxRate.ownerId,
                isValidForToday = taxRate.isValidForDate(),
                daysUntilExpiry = daysUntilExpiry
            )
        }
    }
}

data class TaxRateUpdateDto(
    @field:NotBlank(message = "UID is required")
    val uid: String,

    @field:NotNull(message = "Rate percentage is required")
    @field:DecimalMin(value = "0.0", message = "Rate percentage cannot be negative")
    @field:DecimalMax(value = "100.0", message = "Rate percentage cannot exceed 100%")
    @field:Digits(integer = 3, fraction = 4, message = "Invalid rate percentage format")
    val ratePercentage: BigDecimal,

    @field:DecimalMin(value = "0.0", message = "Fixed amount per unit cannot be negative")
    @field:Digits(integer = 8, fraction = 4, message = "Invalid fixed amount format")
    val fixedAmountPerUnit: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Minimum amount cannot be negative")
    @field:Digits(integer = 8, fraction = 4, message = "Invalid minimum amount format")
    val minimumAmount: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Maximum amount cannot be negative")
    @field:Digits(integer = 8, fraction = 4, message = "Invalid maximum amount format")
    val maximumAmount: BigDecimal? = null,

    val effectiveTo: LocalDate? = null,

    @field:Min(value = 1, message = "Version number must be at least 1")
    val versionNumber: Int = 1,

    @field:Size(max = 100, message = "Notification number is too long")
    val notificationNumber: String? = null,

    val notificationDate: LocalDate? = null,

    val conditions: Map<String, Any> = emptyMap(),

    val exemptionRules: Map<String, Any> = emptyMap(),

    val isReverseChargeApplicable: Boolean = false,

    val isCompositionSchemeApplicable: Boolean = true,

    @field:Size(max = 1000, message = "Description is too long")
    val description: String? = null,

    @field:Size(max = 255, message = "Source reference is too long")
    val sourceReference: String? = null,

) {
    fun toEntity(): TaxRate {
        return TaxRate().apply {
            uid = this@TaxRateUpdateDto.uid
            ratePercentage = this@TaxRateUpdateDto.ratePercentage
            fixedAmountPerUnit = this@TaxRateUpdateDto.fixedAmountPerUnit
            minimumAmount = this@TaxRateUpdateDto.minimumAmount
            maximumAmount = this@TaxRateUpdateDto.maximumAmount
            effectiveTo = this@TaxRateUpdateDto.effectiveTo
            versionNumber = this@TaxRateUpdateDto.versionNumber
            notificationNumber = this@TaxRateUpdateDto.notificationNumber
            notificationDate = this@TaxRateUpdateDto.notificationDate
            conditions = this@TaxRateUpdateDto.conditions
            exemptionRules = this@TaxRateUpdateDto.exemptionRules
            isReverseChargeApplicable = this@TaxRateUpdateDto.isReverseChargeApplicable
            isCompositionSchemeApplicable = this@TaxRateUpdateDto.isCompositionSchemeApplicable
            description = this@TaxRateUpdateDto.description
            sourceReference = this@TaxRateUpdateDto.sourceReference
        }
    }

    companion object {
        fun from(taxRate: TaxRate): TaxRateUpdateDto {
            return TaxRateUpdateDto(
                uid = taxRate.uid,
                ratePercentage = taxRate.ratePercentage,
                fixedAmountPerUnit = taxRate.fixedAmountPerUnit,
                minimumAmount = taxRate.minimumAmount,
                maximumAmount = taxRate.maximumAmount,
                effectiveTo = taxRate.effectiveTo,
                versionNumber = taxRate.versionNumber,
                notificationNumber = taxRate.notificationNumber,
                notificationDate = taxRate.notificationDate,
                conditions = taxRate.conditions,
                exemptionRules = taxRate.exemptionRules,
                isReverseChargeApplicable = taxRate.isReverseChargeApplicable,
                isCompositionSchemeApplicable = taxRate.isCompositionSchemeApplicable,
                description = taxRate.description,
                sourceReference = taxRate.sourceReference,
            )
        }
    }
}

data class TaxRateSearchRequestDto(
    val hsnCode: String? = null,
    val businessType: BusinessType? = null,
    val componentType: TaxComponentType? = null,
    val geographicalZone: GeographicalZone? = null,
    val isActive: Boolean = true,
    val effectiveDate: LocalDate = LocalDate.now(),
    val searchTerm: String? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "effectiveFrom",
    val sortDirection: String = "DESC"
)

data class TaxRateListResponseDto(
    val content: List<TaxRateResponseDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

data class TaxRateStatisticsResponseDto(
    val totalActiveTaxRates: Long,
    val ratesByComponentType: Map<TaxComponentType, Int>,
    val ratesByBusinessType: Map<BusinessType, Int>,
    val averageGstRate: BigDecimal,
    val highestRate: BigDecimal,
    val lowestRate: BigDecimal,
    val ratesWithCess: Int,
    val reverseChargeRates: Int,
    val recentlyAdded: List<TaxRateResponseDto>
)