package com.ampairs.tax.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.tax.domain.dto.*
import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.enums.TaxComponentType
import com.ampairs.tax.service.TaxRateService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/tax-rates")
@Validated
@Tag(name = "Tax Rate Management", description = "APIs for managing tax rates")
class TaxRateController(
    private val taxRateService: TaxRateService
) {

    @GetMapping
    @Operation(
        summary = "Search tax rates",
        description = "Search and filter tax rates with pagination support"
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Tax rates retrieved successfully"),
            SwaggerApiResponse(responseCode = "400", description = "Invalid search parameters")
        ]
    )
    fun searchTaxRates(
        @Parameter(description = "Filter by HSN code")
        @RequestParam(required = false) hsnCode: String?,

        @Parameter(description = "Filter by business type")
        @RequestParam(required = false) businessType: BusinessType?,

        @Parameter(description = "Filter by tax component type")
        @RequestParam(required = false) componentType: TaxComponentType?,

        @Parameter(description = "Filter by active status")
        @RequestParam(defaultValue = "true") isActive: Boolean,

        @Parameter(description = "Effective date for filtering")
        @RequestParam(required = false) effectiveDate: String?,

        @Parameter(description = "Search term for description")
        @RequestParam(required = false) searchTerm: String?,

        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int,

        @Parameter(description = "Sort field")
        @RequestParam(defaultValue = "effectiveFrom") sortBy: String,

        @Parameter(description = "Sort direction (ASC/DESC)")
        @RequestParam(defaultValue = "DESC") sortDirection: String
    ): ResponseEntity<ApiResponse<TaxRateListResponseDto>> {

        val direction = if (sortDirection.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))
        val date = effectiveDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val result = taxRateService.searchTaxRates(
            hsnCode = hsnCode,
            businessType = businessType,
            componentType = componentType,
            isActive = isActive,
            effectiveDate = date,
            searchTerm = searchTerm,
            pageable = pageable
        )

        val response = TaxRateListResponseDto(
            content = result.content.map { TaxRateResponseDto.from(it) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            currentPage = result.number,
            pageSize = result.size,
            hasNext = result.hasNext(),
            hasPrevious = result.hasPrevious()
        )

        return ResponseEntity.ok(
            ApiResponse.success(data = response)
        )
    }

    @GetMapping("/{uid}")
    @Operation(
        summary = "Get tax rate by UID",
        description = "Retrieve detailed information about a specific tax rate"
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Tax rate found"),
            SwaggerApiResponse(responseCode = "404", description = "Tax rate not found")
        ]
    )
    fun getTaxRate(
        @Parameter(description = "Tax rate UID", required = true)
        @PathVariable uid: String
    ): ResponseEntity<ApiResponse<TaxRateResponseDto>> {

        val taxRate = taxRateService.findByUid(uid)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error("NOT_FOUND", "Tax rate not found with UID: $uid")
            )

        return ResponseEntity.ok(
            ApiResponse.success(data = TaxRateResponseDto.from(taxRate))
        )
    }

    @GetMapping("/by-hsn/{hsnCode}")
    @Operation(
        summary = "Get tax rates by HSN code",
        description = "Retrieve all tax rates for a specific HSN code and business type"
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Tax rates found"),
            SwaggerApiResponse(responseCode = "404", description = "HSN code not found")
        ]
    )
    fun getTaxRatesByHsnCode(
        @Parameter(description = "HSN code", required = true)
        @PathVariable hsnCode: String,

        @Parameter(description = "Business type")
        @RequestParam(required = false) businessType: BusinessType?,

        @Parameter(description = "Effective date")
        @RequestParam(required = false) effectiveDate: String?
    ): ResponseEntity<ApiResponse<List<TaxRateResponseDto>>> {

        val date = effectiveDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val taxRates = taxRateService.findByHsnCodeAndBusinessType(
            hsnCode = hsnCode,
            businessType = businessType,
            effectiveDate = date
        )

        val response = taxRates.map { TaxRateResponseDto.from(it) }

        return ResponseEntity.ok(
            ApiResponse.success(data = response)
        )
    }

    @GetMapping("/effective")
    @Operation(
        summary = "Get effective tax rate",
        description = "Get the currently effective tax rate for a specific HSN code and business type"
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Effective tax rate found"),
            SwaggerApiResponse(responseCode = "404", description = "No effective tax rate found")
        ]
    )
    fun getEffectiveTaxRate(
        @Parameter(description = "HSN code", required = true)
        @RequestParam hsnCode: String,

        @Parameter(description = "Business type", required = true)
        @RequestParam businessType: BusinessType,

        @Parameter(description = "Tax component type")
        @RequestParam(required = false) componentType: TaxComponentType?,

        @Parameter(description = "Effective date")
        @RequestParam(required = false) effectiveDate: String?
    ): ResponseEntity<ApiResponse<TaxRateResponseDto>> {

        val date = effectiveDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val taxRate = taxRateService.findEffectiveTaxRate(
            hsnCode = hsnCode,
            businessType = businessType,
            componentType = componentType,
            effectiveDate = date
        ) ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
            ApiResponse.error("NOT_FOUND", "No effective tax rate found for HSN: $hsnCode, Business Type: $businessType")
        )

        return ResponseEntity.ok(
            ApiResponse.success(data = TaxRateResponseDto.from(taxRate))
        )
    }

    @PostMapping
    @Operation(
        summary = "Create or update tax rate",
        description = "Create a new tax rate or update an existing one (UPSERT operation)"
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Tax rate updated successfully"),
            SwaggerApiResponse(responseCode = "201", description = "Tax rate created successfully"),
            SwaggerApiResponse(responseCode = "400", description = "Invalid tax rate data")
        ]
    )
    fun createOrUpdateTaxRate(
        @Valid @RequestBody request: TaxRateRequestDto
    ): ResponseEntity<ApiResponse<TaxRateResponseDto>> {

        // Check if tax rate already exists by UID (if provided) or by unique combination
        val existingTaxRate = request.uid?.let { uid ->
            taxRateService.findByUid(uid)
        }

        val result = if (existingTaxRate != null) {
            // Update existing tax rate
            val updateDto = TaxRateUpdateDto(
                uid = existingTaxRate.uid,
                ratePercentage = request.ratePercentage,
                fixedAmountPerUnit = request.fixedAmountPerUnit,
                minimumAmount = request.minimumAmount,
                maximumAmount = request.maximumAmount,
                effectiveTo = request.effectiveTo,
                versionNumber = request.versionNumber,
                notificationNumber = request.notificationNumber,
                notificationDate = request.notificationDate,
                conditions = request.conditions,
                exemptionRules = request.exemptionRules,
                isReverseChargeApplicable = request.isReverseChargeApplicable,
                isCompositionSchemeApplicable = request.isCompositionSchemeApplicable,
                description = request.description,
                sourceReference = request.sourceReference,
            )
            val updated = taxRateService.updateTaxRateByUid(existingTaxRate.uid, updateDto)
            ResponseEntity.ok(ApiResponse.success(data = TaxRateResponseDto.from(updated)))
        } else {
            // Create new tax rate
            val created = taxRateService.createTaxRate(request.toEntity())
            ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(data = TaxRateResponseDto.from(created))
            )
        }

        return result
    }


    @DeleteMapping("/{uid}")
    @Operation(
        summary = "Deactivate tax rate",
        description = "Deactivate a tax rate (soft delete)"
    )
    @ApiResponses(
        value = [
            SwaggerApiResponse(responseCode = "200", description = "Tax rate deactivated successfully"),
            SwaggerApiResponse(responseCode = "404", description = "Tax rate not found")
        ]
    )
    fun deactivateTaxRate(
        @Parameter(description = "Tax rate UID", required = true)
        @PathVariable uid: String
    ): ResponseEntity<ApiResponse<Unit>> {

        taxRateService.deactivateTaxRateByUid(uid)
        return ResponseEntity.ok(
            ApiResponse.success(data = Unit)
        )
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get tax rate statistics",
        description = "Retrieve comprehensive statistics about tax rates"
    )
    fun getTaxRateStatistics(): ResponseEntity<ApiResponse<TaxRateStatisticsResponseDto>> {

        val statistics = taxRateService.getTaxRateStatistics()
        val recentRates = taxRateService.findRecentlyAdded(LocalDate.now().minusDays(30))

        val response = TaxRateStatisticsResponseDto(
            totalActiveTaxRates = statistics.totalActiveTaxRates,
            ratesByComponentType = statistics.ratesByComponentType,
            ratesByBusinessType = statistics.ratesByBusinessType,
            averageGstRate = statistics.averageGstRate,
            highestRate = statistics.highestRate,
            lowestRate = statistics.lowestRate,
            ratesWithCess = statistics.ratesWithCess,
            reverseChargeRates = statistics.reverseChargeRates,
            recentlyAdded = recentRates.map { TaxRateResponseDto.from(it) }
        )

        return ResponseEntity.ok(
            ApiResponse.success(data = response)
        )
    }

    @GetMapping("/components")
    @Operation(
        summary = "Get available tax component types",
        description = "Get list of all available tax component types"
    )
    fun getTaxComponentTypes(): ResponseEntity<ApiResponse<List<TaxComponentTypeInfoDto>>> {
        val componentTypes = TaxComponentType.values().map {
            TaxComponentTypeInfoDto(
                code = it.name,
                displayName = it.displayName,
                description = it.description
            )
        }

        return ResponseEntity.ok(
            ApiResponse.success(data = componentTypes)
        )
    }
}

data class TaxComponentTypeInfoDto(
    val code: String,
    val displayName: String,
    val description: String
)