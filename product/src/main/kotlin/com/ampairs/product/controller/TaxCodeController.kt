package com.ampairs.product.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.product.domain.enums.TaxSpec
import com.ampairs.product.domain.enums.TaxType
import com.ampairs.product.domain.model.TaxCode
import com.ampairs.product.repository.TaxCodeRepository
import com.ampairs.product.service.TaxCalculationService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

/**
 * REST controller for tax code management and tax calculations
 */
@RestController
@RequestMapping("/tax/v1")
class TaxCodeController(
    private val taxCodeRepository: TaxCodeRepository,
    private val taxCalculationService: TaxCalculationService
) {

    @GetMapping("/codes")
    fun getTaxCodes(
        @RequestParam("search", required = false) search: String?,
        @RequestParam("type", required = false) typeStr: String?,
        @RequestParam("category", required = false) category: String?,
        @RequestParam("rate", required = false) rate: Double?,
        @RequestParam("active_only", defaultValue = "true") activeOnly: Boolean,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "50") size: Int
    ): ApiResponse<Map<String, Any>> {
        
        val pageable = PageRequest.of(page, size, Sort.by("code"))
        
        val taxCodesPage = when {
            !search.isNullOrBlank() -> taxCodeRepository.searchTaxCodes(search, pageable)
            rate != null -> {
                val codes = taxCodeRepository.findByGstRate(rate)
                org.springframework.data.domain.PageImpl(codes, pageable, codes.size.toLong())
            }
            !category.isNullOrBlank() -> {
                val codes = taxCodeRepository.findByCategory(category)
                org.springframework.data.domain.PageImpl(codes, pageable, codes.size.toLong())
            }
            !typeStr.isNullOrBlank() -> {
                val type = try { TaxType.valueOf(typeStr.uppercase()) } catch (e: Exception) { null }
                val codes = type?.let { taxCodeRepository.findActiveByTypeOrderByRate(it) } ?: emptyList()
                org.springframework.data.domain.PageImpl(codes, pageable, codes.size.toLong())
            }
            else -> taxCodeRepository.findAll(pageable)
        }
        
        val response = mapOf(
            "tax_codes" to taxCodesPage.content.map { it.toResponse() },
            "pagination" to mapOf(
                "page" to page,
                "size" to size,
                "total_pages" to taxCodesPage.totalPages,
                "total_elements" to taxCodesPage.totalElements,
                "has_next" to taxCodesPage.hasNext(),
                "has_previous" to taxCodesPage.hasPrevious()
            )
        )
        
        return ApiResponse.success(response)
    }

    @GetMapping("/codes/{code}")
    fun getTaxCode(@PathVariable code: String): ApiResponse<TaxCodeResponse> {
        val taxCode = taxCodeRepository.findByCodeAndIsActive(code, true)
            .orElseThrow { IllegalArgumentException("Tax code not found: $code") }
        
        return ApiResponse.success(taxCode.toResponse())
    }

    @PostMapping("/calculate")
    fun calculateTax(@RequestBody request: TaxCalculationRequest): ApiResponse<TaxCalculationResponse> {
        
        val result = taxCalculationService.calculateTax(
            taxCode = request.taxCode,
            baseAmount = request.baseAmount,
            buyerStateCode = request.buyerStateCode,
            sellerStateCode = request.sellerStateCode,
            buyerGstin = request.buyerGstin,
            sellerGstin = request.sellerGstin,
            businessType = request.businessType,
            transactionDate = request.transactionDate ?: LocalDateTime.now()
        )
        
        val response = TaxCalculationResponse(
            taxCode = result.taxCode,
            taxSpec = result.taxSpec.name,
            baseAmount = result.baseAmount,
            taxComponents = result.taxComponents.map { component ->
                TaxComponentResponse(
                    name = component.name,
                    type = component.componentType?.name ?: "UNKNOWN",
                    percentage = component.percentage,
                    amount = component.calculatedAmount ?: 0.0
                )
            },
            totalTaxAmount = result.totalTaxAmount,
            totalAmountIncludingTax = result.totalAmountIncludingTax,
            isReverseCharge = result.isReverseCharge
        )
        
        return ApiResponse.success(response)
    }

    @PostMapping("/validate-gstin")
    fun validateGstin(@RequestBody request: Map<String, String>): ApiResponse<Map<String, Any>> {
        val gstin = request["gstin"] ?: return ApiResponse.error("GSTIN is required", "VALIDATION_ERROR")
        
        val result = taxCalculationService.validateGstinAndExtractState(gstin)
        
        val response: Map<String, Any> = mapOf(
            "gstin" to gstin,
            "is_valid" to result.isValid,
            "state_code" to (result.stateCode ?: ""),
            "state_name" to (result.stateCode?.let { TaxCalculationService.STATE_CODES[it] } ?: ""),
            "message" to result.message
        )
        
        return ApiResponse.success(response)
    }

    @GetMapping("/rates")
    fun getTaxRates(
        @RequestParam("min_rate", required = false) minRate: Double?,
        @RequestParam("max_rate", required = false) maxRate: Double?
    ): ApiResponse<List<TaxRateResponse>> {
        
        val taxCodes = when {
            minRate != null && maxRate != null -> taxCodeRepository.findByGstRateRange(minRate, maxRate)
            minRate != null -> taxCodeRepository.findByGstRateRange(minRate, 100.0)
            maxRate != null -> taxCodeRepository.findByGstRateRange(0.0, maxRate)
            else -> taxCodeRepository.findByIsActive(true)
        }
        
        val rates = taxCodes.map { 
            TaxRateResponse(
                code = it.code,
                description = it.description,
                gstRate = it.gstRate,
                cessRate = it.cessRate,
                totalRate = it.getTotalTaxRate(),
                category = it.category
            )
        }.distinctBy { it.gstRate }.sortedBy { it.gstRate }
        
        return ApiResponse.success(rates)
    }

    @GetMapping("/categories")
    fun getTaxCategories(): ApiResponse<List<String>> {
        val categories = taxCalculationService.getAllTaxCategories()
        return ApiResponse.success(categories)
    }

    @PostMapping("/composition/calculate")
    fun calculateCompositionTax(@RequestBody request: CompositionTaxRequest): ApiResponse<CompositionTaxResponse> {
        
        val result = taxCalculationService.calculateCompositionTax(
            taxCode = request.taxCode,
            turnover = request.turnover,
            businessType = request.businessType
        )
        
        val response = CompositionTaxResponse(
            taxCode = result.taxCode,
            businessType = result.businessType,
            turnover = result.turnover,
            compositionRate = result.compositionRate,
            taxAmount = result.taxAmount
        )
        
        return ApiResponse.success(response)
    }

    @GetMapping("/business-types/{businessType}")
    fun getTaxCodesForBusinessType(@PathVariable businessType: String): ApiResponse<List<TaxCodeResponse>> {
        val taxCodes = taxCalculationService.getTaxCodesForBusinessType(businessType)
        return ApiResponse.success(taxCodes.map { it.toResponse() })
    }
}

/**
 * Extension function to convert TaxCode to response DTO
 */
fun TaxCode.toResponse(): TaxCodeResponse {
    return TaxCodeResponse(
        id = this.uid,
        code = this.code,
        type = this.type.name,
        description = this.description,
        gstRate = this.gstRate,
        cessRate = this.cessRate,
        totalRate = this.getTotalTaxRate(),
        category = this.category,
        isReverseCharge = this.isReverseCharge,
        isCompositionApplicable = this.isCompositionApplicable,
        businessTypeRates = this.businessTypeRates,
        isActive = this.active,
        validFrom = this.validFrom,
        validTo = this.validTo
    )
}

/**
 * DTOs for tax API
 */
data class TaxCalculationRequest(
    val taxCode: String,
    val baseAmount: Double,
    val buyerStateCode: String,
    val sellerStateCode: String,
    val buyerGstin: String? = null,
    val sellerGstin: String? = null,
    val businessType: String? = null,
    val transactionDate: LocalDateTime? = null
)

data class TaxCalculationResponse(
    val taxCode: String,
    val taxSpec: String,
    val baseAmount: Double,
    val taxComponents: List<TaxComponentResponse>,
    val totalTaxAmount: Double,
    val totalAmountIncludingTax: Double,
    val isReverseCharge: Boolean
)

data class TaxComponentResponse(
    val name: String,
    val type: String,
    val percentage: Double,
    val amount: Double
)

data class TaxCodeResponse(
    val id: String,
    val code: String,
    val type: String,
    val description: String,
    val gstRate: Double,
    val cessRate: Double,
    val totalRate: Double,
    val category: String?,
    val isReverseCharge: Boolean,
    val isCompositionApplicable: Boolean,
    val businessTypeRates: Map<String, Double>,
    val isActive: Boolean,
    val validFrom: LocalDateTime?,
    val validTo: LocalDateTime?
)

data class TaxRateResponse(
    val code: String,
    val description: String,
    val gstRate: Double,
    val cessRate: Double,
    val totalRate: Double,
    val category: String?
)

data class CompositionTaxRequest(
    val taxCode: String,
    val turnover: Double,
    val businessType: String
)

data class CompositionTaxResponse(
    val taxCode: String,
    val businessType: String,
    val turnover: Double,
    val compositionRate: Double,
    val taxAmount: Double
)