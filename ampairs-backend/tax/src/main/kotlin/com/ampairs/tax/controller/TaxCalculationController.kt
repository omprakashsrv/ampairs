package com.ampairs.tax.controller

import com.ampairs.tax.domain.dto.*
import com.ampairs.tax.service.GstTaxCalculationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tax")
@Validated
@Tag(name = "Tax Calculation", description = "GST and tax calculation APIs for Indian retail businesses")
class TaxCalculationController(
    private val gstTaxCalculationService: GstTaxCalculationService
) {

    @PostMapping("/calculate")
    @Operation(
        summary = "Calculate tax for a product",
        description = "Calculate GST, CESS and total tax amount for a product based on HSN code, business type, and transaction details"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Tax calculation successful"),
            ApiResponse(responseCode = "400", description = "Invalid input parameters"),
            ApiResponse(responseCode = "404", description = "HSN code or tax configuration not found"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun calculateTax(
        @Valid @RequestBody request: TaxCalculationRequestDto
    ): ResponseEntity<TaxCalculationResponseDto> {

        val result = gstTaxCalculationService.calculateTax(
            hsnCode = request.hsnCode,
            baseAmount = request.baseAmount,
            quantity = request.quantity,
            businessType = request.businessType,
            sourceStateCode = request.sourceStateCode,
            destinationStateCode = request.destinationStateCode,
            effectiveDate = request.effectiveDate
        )

        return ResponseEntity.ok(TaxCalculationResponseDto.from(result))
    }

    @PostMapping("/calculate/bulk")
    @Operation(
        summary = "Calculate tax for multiple products",
        description = "Calculate GST and tax amounts for multiple products in a single request"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Bulk tax calculation successful"),
            ApiResponse(responseCode = "400", description = "Invalid input parameters"),
            ApiResponse(responseCode = "422", description = "Some items failed calculation"),
            ApiResponse(responseCode = "500", description = "Internal server error")
        ]
    )
    fun calculateBulkTax(
        @Valid @RequestBody request: BulkTaxCalculationRequestDto
    ): ResponseEntity<BulkTaxCalculationResponseDto> {

        val serviceRequests = request.items.map { it.toServiceRequest() }

        val result = gstTaxCalculationService.calculateBulkTax(
            items = serviceRequests,
            businessType = request.businessType,
            sourceStateCode = request.sourceStateCode,
            destinationStateCode = request.destinationStateCode,
            effectiveDate = request.effectiveDate
        )

        return ResponseEntity.ok(BulkTaxCalculationResponseDto.from(result))
    }

    @GetMapping("/calculate")
    @Operation(
        summary = "Calculate tax via GET request",
        description = "Calculate tax for a product using GET request with query parameters"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Tax calculation successful"),
            ApiResponse(responseCode = "400", description = "Invalid query parameters"),
            ApiResponse(responseCode = "404", description = "HSN code or tax configuration not found")
        ]
    )
    fun calculateTaxGet(
        @Parameter(description = "HSN code (4-8 digits)", required = true)
        @RequestParam hsnCode: String,

        @Parameter(description = "Base amount for tax calculation", required = true)
        @RequestParam baseAmount: java.math.BigDecimal,

        @Parameter(description = "Quantity of items", required = false)
        @RequestParam(defaultValue = "1") quantity: Int,

        @Parameter(description = "Business type", required = false)
        @RequestParam(defaultValue = "B2B") businessType: String,

        @Parameter(description = "Source state code (2 characters)", required = false)
        @RequestParam(required = false) sourceStateCode: String?,

        @Parameter(description = "Destination state code (2 characters)", required = false)
        @RequestParam(required = false) destinationStateCode: String?
    ): ResponseEntity<TaxCalculationResponseDto> {

        val businessTypeEnum = try {
            com.ampairs.tax.domain.enums.BusinessType.valueOf(businessType.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid business type: $businessType")
        }

        val result = gstTaxCalculationService.calculateTax(
            hsnCode = hsnCode,
            baseAmount = baseAmount,
            quantity = quantity,
            businessType = businessTypeEnum,
            sourceStateCode = sourceStateCode,
            destinationStateCode = destinationStateCode
        )

        return ResponseEntity.ok(TaxCalculationResponseDto.from(result))
    }

    @GetMapping("/rates/{hsnCode}")
    @Operation(
        summary = "Get current tax rates for HSN code",
        description = "Get current applicable tax rates for a specific HSN code and business type"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Tax rates retrieved successfully"),
            ApiResponse(responseCode = "404", description = "HSN code not found"),
            ApiResponse(responseCode = "400", description = "Invalid parameters")
        ]
    )
    fun getCurrentTaxRates(
        @Parameter(description = "HSN code", required = true)
        @PathVariable hsnCode: String,

        @Parameter(description = "Business type", required = false)
        @RequestParam(defaultValue = "B2B") businessType: String,

        @Parameter(description = "State code for geographical filtering", required = false)
        @RequestParam(required = false) stateCode: String?
    ): ResponseEntity<CurrentTaxRatesResponseDto> {

        val businessTypeEnum = try {
            com.ampairs.tax.domain.enums.BusinessType.valueOf(businessType.uppercase())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid business type: $businessType")
        }

        // This would need to be implemented in the service
        // For now, we'll calculate tax for a nominal amount to get the rates
        val result = gstTaxCalculationService.calculateTax(
            hsnCode = hsnCode,
            baseAmount = java.math.BigDecimal(100), // Nominal amount for rate calculation
            quantity = 1,
            businessType = businessTypeEnum,
            sourceStateCode = stateCode,
            destinationStateCode = stateCode
        )

        val currentRates = CurrentTaxRatesResponseDto(
            hsnCode = hsnCode,
            businessType = businessTypeEnum,
            totalGstRate = result.getGstComponents().sumOf { it.rate },
            cgstRate = result.getCgstAmount().divide(java.math.BigDecimal(100), 4, java.math.BigDecimal.ROUND_HALF_UP).multiply(java.math.BigDecimal(100)),
            sgstRate = result.getSgstAmount().divide(java.math.BigDecimal(100), 4, java.math.BigDecimal.ROUND_HALF_UP).multiply(java.math.BigDecimal(100)),
            igstRate = result.getIgstAmount().divide(java.math.BigDecimal(100), 4, java.math.BigDecimal.ROUND_HALF_UP).multiply(java.math.BigDecimal(100)),
            utgstRate = result.getUtgstAmount().divide(java.math.BigDecimal(100), 4, java.math.BigDecimal.ROUND_HALF_UP).multiply(java.math.BigDecimal(100)),
            cessRate = result.getCessAmount().divide(java.math.BigDecimal(100), 4, java.math.BigDecimal.ROUND_HALF_UP).multiply(java.math.BigDecimal(100)),
            isReverseChargeApplicable = result.isReverseChargeApplicable,
            applicableComponents = result.taxComponents.map { it.componentType }.toSet().toList()
        )

        return ResponseEntity.ok(currentRates)
    }

    @GetMapping("/business-types")
    @Operation(
        summary = "Get available business types",
        description = "Get list of all available business types for tax calculation"
    )
    fun getBusinessTypes(): ResponseEntity<List<BusinessTypeInfoDto>> {
        val businessTypes = com.ampairs.tax.domain.enums.BusinessType.values().map {
            BusinessTypeInfoDto(
                code = it.name,
                displayName = it.displayName,
                description = it.description
            )
        }

        return ResponseEntity.ok(businessTypes)
    }

    @GetMapping("/transaction-types")
    @Operation(
        summary = "Get available transaction types",
        description = "Get list of all available transaction types"
    )
    fun getTransactionTypes(): ResponseEntity<List<TransactionTypeInfoDto>> {
        val transactionTypes = com.ampairs.tax.domain.enums.TransactionType.values().map {
            TransactionTypeInfoDto(
                code = it.name,
                displayName = it.displayName,
                description = it.description
            )
        }

        return ResponseEntity.ok(transactionTypes)
    }
}

data class CurrentTaxRatesResponseDto(
    val hsnCode: String,
    val businessType: com.ampairs.tax.domain.enums.BusinessType,
    val totalGstRate: java.math.BigDecimal,
    val cgstRate: java.math.BigDecimal,
    val sgstRate: java.math.BigDecimal,
    val igstRate: java.math.BigDecimal,
    val utgstRate: java.math.BigDecimal,
    val cessRate: java.math.BigDecimal,
    val isReverseChargeApplicable: Boolean,
    val applicableComponents: List<com.ampairs.tax.domain.enums.TaxComponentType>
)

data class BusinessTypeInfoDto(
    val code: String,
    val displayName: String,
    val description: String
)

data class TransactionTypeInfoDto(
    val code: String,
    val displayName: String,
    val description: String
)