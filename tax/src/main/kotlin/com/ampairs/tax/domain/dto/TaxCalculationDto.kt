package com.ampairs.tax.domain.dto

import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.enums.TaxComponentType
import com.ampairs.tax.domain.enums.TransactionType
import com.ampairs.tax.domain.model.TaxCalculationResult
import com.ampairs.tax.domain.model.TaxComponent
import com.ampairs.tax.service.BulkTaxCalculationResult
import com.ampairs.tax.service.TaxCalculationRequest
import jakarta.validation.constraints.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class TaxCalculationRequestDto(
    @field:NotBlank(message = "HSN code is required")
    @field:Pattern(regexp = "\\d{4,8}", message = "HSN code should be 4-8 digits")
    val hsnCode: String,

    @field:NotNull(message = "Base amount is required")
    @field:DecimalMin(value = "0.01", message = "Base amount must be greater than zero")
    @field:Digits(integer = 12, fraction = 4, message = "Base amount format is invalid")
    val baseAmount: BigDecimal,

    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int = 1,

    @field:NotNull(message = "Business type is required")
    val businessType: BusinessType = BusinessType.B2B,

    @field:Size(max = 2, message = "State code should be 2 characters")
    val sourceStateCode: String? = null,

    @field:Size(max = 2, message = "State code should be 2 characters")
    val destinationStateCode: String? = null,

    val effectiveDate: LocalDate = LocalDate.now()
) {
    fun toServiceRequest(): TaxCalculationRequest {
        return TaxCalculationRequest(
            hsnCode = hsnCode,
            baseAmount = baseAmount,
            quantity = quantity
        )
    }
}

data class BulkTaxCalculationRequestDto(
    @field:NotEmpty(message = "Items list cannot be empty")
    @field:Size(max = 100, message = "Maximum 100 items allowed per request")
    @field:jakarta.validation.Valid
    val items: List<TaxCalculationItemDto>,

    @field:NotNull(message = "Business type is required")
    val businessType: BusinessType = BusinessType.B2B,

    @field:Size(max = 2, message = "State code should be 2 characters")
    val sourceStateCode: String? = null,

    @field:Size(max = 2, message = "State code should be 2 characters")
    val destinationStateCode: String? = null,

    val effectiveDate: LocalDate = LocalDate.now()
)

data class TaxCalculationItemDto(
    @field:NotBlank(message = "HSN code is required")
    @field:Pattern(regexp = "\\d{4,8}", message = "HSN code should be 4-8 digits")
    val hsnCode: String,

    @field:NotNull(message = "Base amount is required")
    @field:DecimalMin(value = "0.01", message = "Base amount must be greater than zero")
    @field:Digits(integer = 12, fraction = 4, message = "Base amount format is invalid")
    val baseAmount: BigDecimal,

    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int = 1,

    @field:Size(max = 255, message = "Item description too long")
    val description: String? = null
) {
    fun toServiceRequest(): TaxCalculationRequest {
        return TaxCalculationRequest(
            hsnCode = hsnCode,
            baseAmount = baseAmount,
            quantity = quantity
        )
    }
}

data class TaxCalculationResponseDto(
    val baseAmount: BigDecimal,
    val totalTaxAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val hsnCode: String,
    val transactionType: TransactionType,
    val taxComponents: List<TaxComponentDto>,
    val calculationDate: LocalDateTime,
    val isReverseChargeApplicable: Boolean,
    val exemptionApplied: String?,
    val calculationNotes: List<String>,
    val effectiveRate: BigDecimal
) {
    companion object {
        fun from(result: TaxCalculationResult): TaxCalculationResponseDto {
            return TaxCalculationResponseDto(
                baseAmount = result.baseAmount,
                totalTaxAmount = result.totalTaxAmount,
                totalAmount = result.totalAmount,
                hsnCode = result.hsnCode,
                transactionType = result.transactionType,
                taxComponents = result.taxComponents.map { TaxComponentDto.from(it) },
                calculationDate = result.calculationDate,
                isReverseChargeApplicable = result.isReverseChargeApplicable,
                exemptionApplied = result.exemptionApplied,
                calculationNotes = result.calculationNotes,
                effectiveRate = result.getEffectiveRate()
            )
        }
    }
}

data class TaxComponentDto(
    val componentType: TaxComponentType,
    val name: String,
    val rate: BigDecimal,
    val amount: BigDecimal,
    val baseAmount: BigDecimal,
    val isFixed: Boolean,
    val description: String?,
    val effectiveRate: BigDecimal
) {
    companion object {
        fun from(component: TaxComponent): TaxComponentDto {
            return TaxComponentDto(
                componentType = component.componentType,
                name = component.name,
                rate = component.rate,
                amount = component.amount,
                baseAmount = component.baseAmount,
                isFixed = component.isFixed,
                description = component.description,
                effectiveRate = component.getEffectiveRate()
            )
        }
    }
}

data class BulkTaxCalculationResponseDto(
    val items: List<TaxCalculationResponseDto>,
    val totalBaseAmount: BigDecimal,
    val totalTaxAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val calculationDate: LocalDateTime,
    val summary: TaxSummaryDto
) {
    companion object {
        fun from(result: BulkTaxCalculationResult): BulkTaxCalculationResponseDto {
            val items = result.items.map { TaxCalculationResponseDto.from(it) }
            val summary = TaxSummaryDto.from(result.items)

            return BulkTaxCalculationResponseDto(
                items = items,
                totalBaseAmount = result.totalBaseAmount,
                totalTaxAmount = result.totalTaxAmount,
                totalAmount = result.totalAmount,
                calculationDate = result.calculationDate,
                summary = summary
            )
        }
    }
}

data class TaxSummaryDto(
    val totalCgstAmount: BigDecimal,
    val totalSgstAmount: BigDecimal,
    val totalIgstAmount: BigDecimal,
    val totalUtgstAmount: BigDecimal,
    val totalCessAmount: BigDecimal,
    val totalGstAmount: BigDecimal,
    val averageEffectiveRate: BigDecimal,
    val itemsCount: Int,
    val uniqueHsnCodes: Set<String>,
    val transactionTypes: Set<TransactionType>
) {
    companion object {
        fun from(results: List<TaxCalculationResult>): TaxSummaryDto {
            val totalCgst = results.sumOf { it.getCgstAmount() }
            val totalSgst = results.sumOf { it.getSgstAmount() }
            val totalIgst = results.sumOf { it.getIgstAmount() }
            val totalUtgst = results.sumOf { it.getUtgstAmount() }
            val totalCess = results.sumOf { it.getCessAmount() }
            val totalGst = results.sumOf { it.getTotalGstAmount() }
            val totalBase = results.sumOf { it.baseAmount }

            val averageRate = if (totalBase > BigDecimal.ZERO) {
                results.sumOf { it.totalTaxAmount }
                    .divide(totalBase, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal(100))
            } else {
                BigDecimal.ZERO
            }

            return TaxSummaryDto(
                totalCgstAmount = totalCgst,
                totalSgstAmount = totalSgst,
                totalIgstAmount = totalIgst,
                totalUtgstAmount = totalUtgst,
                totalCessAmount = totalCess,
                totalGstAmount = totalGst,
                averageEffectiveRate = averageRate,
                itemsCount = results.size,
                uniqueHsnCodes = results.map { it.hsnCode }.toSet(),
                transactionTypes = results.map { it.transactionType }.toSet()
            )
        }
    }
}