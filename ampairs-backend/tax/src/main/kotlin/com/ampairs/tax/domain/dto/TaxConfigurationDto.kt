package com.ampairs.tax.domain.dto

import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.enums.GeographicalZone
import com.ampairs.tax.domain.model.TaxConfiguration
import com.ampairs.tax.service.TaxConfigurationStatistics
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TaxConfigurationRequestDto(
    @field:NotNull(message = "Business type ID is required")
    val businessTypeId: Long,

    @field:NotNull(message = "HSN code ID is required")
    val hsnCodeId: Long,

    val geographicalZone: GeographicalZone? = null,

    @field:NotNull(message = "Total GST rate is required")
    @field:DecimalMin(value = "0.0", message = "Total GST rate cannot be negative")
    @field:DecimalMax(value = "100.0", message = "Total GST rate cannot exceed 100%")
    @field:Digits(integer = 2, fraction = 4, message = "Invalid GST rate format")
    val totalGstRate: BigDecimal,

    @field:DecimalMin(value = "0.0", message = "CGST rate cannot be negative")
    @field:DecimalMax(value = "50.0", message = "CGST rate cannot exceed 50%")
    @field:Digits(integer = 2, fraction = 4, message = "Invalid CGST rate format")
    val cgstRate: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "SGST rate cannot be negative")
    @field:DecimalMax(value = "50.0", message = "SGST rate cannot exceed 50%")
    @field:Digits(integer = 2, fraction = 4, message = "Invalid SGST rate format")
    val sgstRate: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "IGST rate cannot be negative")
    @field:DecimalMax(value = "100.0", message = "IGST rate cannot exceed 100%")
    @field:Digits(integer = 2, fraction = 4, message = "Invalid IGST rate format")
    val igstRate: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "UTGST rate cannot be negative")
    @field:DecimalMax(value = "50.0", message = "UTGST rate cannot exceed 50%")
    @field:Digits(integer = 2, fraction = 4, message = "Invalid UTGST rate format")
    val utgstRate: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Cess rate cannot be negative")
    @field:DecimalMax(value = "500.0", message = "Cess rate cannot exceed 500%")
    @field:Digits(integer = 3, fraction = 4, message = "Invalid cess rate format")
    val cessRate: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Cess amount cannot be negative")
    @field:Digits(integer = 8, fraction = 4, message = "Invalid cess amount format")
    val cessAmountPerUnit: BigDecimal? = null,

    @field:NotNull(message = "Effective from date is required")
    val effectiveFrom: LocalDate,

    val effectiveTo: LocalDate? = null,

    val isReverseChargeApplicable: Boolean = false,

    val isCompositionSchemeApplicable: Boolean = true,

    @field:DecimalMin(value = "0.0", message = "Composition rate cannot be negative")
    @field:DecimalMax(value = "10.0", message = "Composition rate cannot exceed 10%")
    @field:Digits(integer = 2, fraction = 4, message = "Invalid composition rate format")
    val compositionRate: BigDecimal? = null,

    val specialConditions: Map<String, Any> = emptyMap(),

    val exemptionCriteria: Map<String, Any> = emptyMap(),

    val thresholdLimits: Map<String, Any> = emptyMap(),

    @field:Size(max = 1000, message = "Description is too long")
    val description: String? = null,

    @field:Size(max = 255, message = "Notification reference is too long")
    val notificationReference: String? = null,

    val isActive: Boolean = true
) {
    fun toEntity(): TaxConfiguration {
        return TaxConfiguration().apply {
            businessTypeId = this@TaxConfigurationRequestDto.businessTypeId
            hsnCodeId = this@TaxConfigurationRequestDto.hsnCodeId
            geographicalZone = this@TaxConfigurationRequestDto.geographicalZone
            totalGstRate = this@TaxConfigurationRequestDto.totalGstRate
            cgstRate = this@TaxConfigurationRequestDto.cgstRate
            sgstRate = this@TaxConfigurationRequestDto.sgstRate
            igstRate = this@TaxConfigurationRequestDto.igstRate
            utgstRate = this@TaxConfigurationRequestDto.utgstRate
            cessRate = this@TaxConfigurationRequestDto.cessRate
            cessAmountPerUnit = this@TaxConfigurationRequestDto.cessAmountPerUnit
            effectiveFrom = this@TaxConfigurationRequestDto.effectiveFrom
            effectiveTo = this@TaxConfigurationRequestDto.effectiveTo
            isReverseChargeApplicable = this@TaxConfigurationRequestDto.isReverseChargeApplicable
            isCompositionSchemeApplicable = this@TaxConfigurationRequestDto.isCompositionSchemeApplicable
            compositionRate = this@TaxConfigurationRequestDto.compositionRate
            specialConditions = this@TaxConfigurationRequestDto.specialConditions
            exemptionCriteria = this@TaxConfigurationRequestDto.exemptionCriteria
            thresholdLimits = this@TaxConfigurationRequestDto.thresholdLimits
            description = this@TaxConfigurationRequestDto.description
            notificationReference = this@TaxConfigurationRequestDto.notificationReference
            active = this@TaxConfigurationRequestDto.isActive
        }
    }
}

data class TaxConfigurationResponseDto(
    val id: Long,
    val uid: String,
    val businessTypeId: Long,
    val businessType: BusinessType?,
    val businessTypeDisplayName: String?,
    val hsnCodeId: Long,
    val hsnCode: String?,
    val hsnDescription: String?,
    val geographicalZone: GeographicalZone?,
    val totalGstRate: BigDecimal,
    val cgstRate: BigDecimal?,
    val sgstRate: BigDecimal?,
    val igstRate: BigDecimal?,
    val utgstRate: BigDecimal?,
    val cessRate: BigDecimal?,
    val cessAmountPerUnit: BigDecimal?,
    val effectiveFrom: LocalDate,
    val effectiveTo: LocalDate?,
    val isActive: Boolean,
    val isReverseChargeApplicable: Boolean,
    val isCompositionSchemeApplicable: Boolean,
    val compositionRate: BigDecimal?,
    val specialConditions: Map<String, Any>,
    val exemptionCriteria: Map<String, Any>,
    val thresholdLimits: Map<String, Any>,
    val description: String?,
    val notificationReference: String?,
    val lastUpdatedBy: String?,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val isValidForToday: Boolean,
    val daysUntilExpiry: Long?
) {
    companion object {
        fun from(taxConfiguration: TaxConfiguration): TaxConfigurationResponseDto {
            val today = LocalDate.now()
            val daysUntilExpiry = taxConfiguration.effectiveTo?.let {
                java.time.temporal.ChronoUnit.DAYS.between(today, it)
            }

            return TaxConfigurationResponseDto(
                id = taxConfiguration.id,
                uid = taxConfiguration.uid,
                businessTypeId = taxConfiguration.businessTypeId,
                businessType = taxConfiguration.businessTypeEntity?.businessType,
                businessTypeDisplayName = taxConfiguration.businessTypeEntity?.displayName,
                hsnCodeId = taxConfiguration.hsnCodeId,
                hsnCode = taxConfiguration.hsnCode?.hsnCode,
                hsnDescription = taxConfiguration.hsnCode?.hsnDescription,
                geographicalZone = taxConfiguration.geographicalZone,
                totalGstRate = taxConfiguration.totalGstRate,
                cgstRate = taxConfiguration.cgstRate,
                sgstRate = taxConfiguration.sgstRate,
                igstRate = taxConfiguration.igstRate,
                utgstRate = taxConfiguration.utgstRate,
                cessRate = taxConfiguration.cessRate,
                cessAmountPerUnit = taxConfiguration.cessAmountPerUnit,
                effectiveFrom = taxConfiguration.effectiveFrom,
                effectiveTo = taxConfiguration.effectiveTo,
                isActive = taxConfiguration.active,
                isReverseChargeApplicable = taxConfiguration.isReverseChargeApplicable,
                isCompositionSchemeApplicable = taxConfiguration.isCompositionSchemeApplicable,
                compositionRate = taxConfiguration.compositionRate,
                specialConditions = taxConfiguration.specialConditions,
                exemptionCriteria = taxConfiguration.exemptionCriteria,
                thresholdLimits = taxConfiguration.thresholdLimits,
                description = taxConfiguration.description,
                notificationReference = taxConfiguration.notificationReference,
                lastUpdatedBy = taxConfiguration.lastUpdatedBy,
                createdAt = taxConfiguration.createdAt,
                updatedAt = taxConfiguration.updatedAt,
                isValidForToday = taxConfiguration.isValidForDate(),
                daysUntilExpiry = daysUntilExpiry
            )
        }
    }
}

data class TaxConfigurationUpdateDto(
    @field:NotNull(message = "ID is required")
    val id: Long,

    @field:NotNull(message = "Total GST rate is required")
    @field:DecimalMin(value = "0.0", message = "Total GST rate cannot be negative")
    @field:DecimalMax(value = "100.0", message = "Total GST rate cannot exceed 100%")
    @field:Digits(integer = 2, fraction = 4, message = "Invalid GST rate format")
    val totalGstRate: BigDecimal,

    @field:DecimalMin(value = "0.0", message = "CGST rate cannot be negative")
    @field:DecimalMax(value = "50.0", message = "CGST rate cannot exceed 50%")
    @field:Digits(integer = 2, fraction = 4, message = "Invalid CGST rate format")
    val cgstRate: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "SGST rate cannot be negative")
    @field:DecimalMax(value = "50.0", message = "SGST rate cannot exceed 50%")
    @field:Digits(integer = 2, fraction = 4, message = "Invalid SGST rate format")
    val sgstRate: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "IGST rate cannot be negative")
    @field:DecimalMax(value = "100.0", message = "IGST rate cannot exceed 100%")
    @field:Digits(integer = 2, fraction = 4, message = "Invalid IGST rate format")
    val igstRate: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "UTGST rate cannot be negative")
    @field:DecimalMax(value = "50.0", message = "UTGST rate cannot exceed 50%")
    @field:Digits(integer = 2, fraction = 4, message = "Invalid UTGST rate format")
    val utgstRate: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Cess rate cannot be negative")
    @field:DecimalMax(value = "500.0", message = "Cess rate cannot exceed 500%")
    @field:Digits(integer = 3, fraction = 4, message = "Invalid cess rate format")
    val cessRate: BigDecimal? = null,

    @field:DecimalMin(value = "0.0", message = "Cess amount cannot be negative")
    @field:Digits(integer = 8, fraction = 4, message = "Invalid cess amount format")
    val cessAmountPerUnit: BigDecimal? = null,

    val effectiveTo: LocalDate? = null,

    val isReverseChargeApplicable: Boolean = false,

    val isCompositionSchemeApplicable: Boolean = true,

    @field:DecimalMin(value = "0.0", message = "Composition rate cannot be negative")
    @field:DecimalMax(value = "10.0", message = "Composition rate cannot exceed 10%")
    @field:Digits(integer = 2, fraction = 4, message = "Invalid composition rate format")
    val compositionRate: BigDecimal? = null,

    val specialConditions: Map<String, Any> = emptyMap(),

    val exemptionCriteria: Map<String, Any> = emptyMap(),

    val thresholdLimits: Map<String, Any> = emptyMap(),

    @field:Size(max = 1000, message = "Description is too long")
    val description: String? = null,

    @field:Size(max = 255, message = "Notification reference is too long")
    val notificationReference: String? = null,

    val isActive: Boolean = true
)

data class TaxConfigurationSearchRequestDto(
    val businessType: BusinessType? = null,
    val hsnCode: String? = null,
    val geographicalZone: GeographicalZone? = null,
    val gstRateMin: BigDecimal? = null,
    val gstRateMax: BigDecimal? = null,
    val withCess: Boolean? = null,
    val reverseCharge: Boolean? = null,
    val compositionScheme: Boolean? = null,
    val effectiveDate: LocalDate = LocalDate.now(),
    val searchTerm: String? = null,
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "effectiveFrom",
    val sortDirection: String = "DESC"
)

data class TaxConfigurationListResponseDto(
    val content: List<TaxConfigurationResponseDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

data class TaxConfigurationStatisticsResponseDto(
    val totalActiveConfigurations: Long,
    val configurationsWithCess: Int,
    val configurationsWithFixedCess: Int,
    val reverseChargeConfigurations: Int,
    val compositionSchemeConfigurations: Int,
    val distinctGstRates: Int,
    val gstRateDistribution: Map<BigDecimal, Int>,
    val businessTypeDistribution: Map<BusinessType, Int>,
    val expiringConfigurations: List<TaxConfigurationResponseDto>
) {
    companion object {
        fun from(
            statistics: TaxConfigurationStatistics,
            expiringConfigurations: List<TaxConfiguration> = emptyList()
        ): TaxConfigurationStatisticsResponseDto {
            return TaxConfigurationStatisticsResponseDto(
                totalActiveConfigurations = statistics.totalActiveConfigurations,
                configurationsWithCess = statistics.configurationsWithCess,
                configurationsWithFixedCess = statistics.configurationsWithFixedCess,
                reverseChargeConfigurations = statistics.reverseChargeConfigurations,
                compositionSchemeConfigurations = statistics.compositionSchemeConfigurations,
                distinctGstRates = statistics.distinctGstRates,
                gstRateDistribution = emptyMap(), // Can be populated with additional query
                businessTypeDistribution = emptyMap(), // Can be populated with additional query
                expiringConfigurations = expiringConfigurations.map { TaxConfigurationResponseDto.from(it) }
            )
        }
    }
}