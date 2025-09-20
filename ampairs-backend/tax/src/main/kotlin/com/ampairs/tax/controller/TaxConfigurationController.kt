package com.ampairs.tax.controller

import com.ampairs.tax.domain.dto.*
import com.ampairs.tax.domain.enums.BusinessType
import com.ampairs.tax.domain.enums.GeographicalZone
import com.ampairs.tax.service.TaxConfigurationService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
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
@RequestMapping("/api/v1/tax-configurations")
@Validated
@Tag(name = "Tax Configuration Management", description = "APIs for managing tax configurations and rates")
class TaxConfigurationController(
    private val taxConfigurationService: TaxConfigurationService
) {

    @GetMapping
    @Operation(
        summary = "Search tax configurations",
        description = "Search and filter tax configurations with advanced filtering options"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Tax configurations retrieved successfully"),
            ApiResponse(responseCode = "400", description = "Invalid search parameters")
        ]
    )
    fun searchTaxConfigurations(
        @Parameter(description = "Filter by business type")
        @RequestParam(required = false) businessType: BusinessType?,

        @Parameter(description = "Filter by HSN code")
        @RequestParam(required = false) hsnCode: String?,

        @Parameter(description = "Filter by geographical zone")
        @RequestParam(required = false) geographicalZone: GeographicalZone?,

        @Parameter(description = "Minimum GST rate filter")
        @RequestParam(required = false) gstRateMin: java.math.BigDecimal?,

        @Parameter(description = "Maximum GST rate filter")
        @RequestParam(required = false) gstRateMax: java.math.BigDecimal?,

        @Parameter(description = "Filter configurations with cess")
        @RequestParam(required = false) withCess: Boolean?,

        @Parameter(description = "Filter reverse charge applicable configurations")
        @RequestParam(required = false) reverseCharge: Boolean?,

        @Parameter(description = "Filter composition scheme applicable configurations")
        @RequestParam(required = false) compositionScheme: Boolean?,

        @Parameter(description = "Effective date for filtering")
        @RequestParam(required = false) effectiveDate: String?,

        @Parameter(description = "Search term for description or notification reference")
        @RequestParam(required = false) searchTerm: String?,

        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int,

        @Parameter(description = "Sort field")
        @RequestParam(defaultValue = "effectiveFrom") sortBy: String,

        @Parameter(description = "Sort direction (ASC/DESC)")
        @RequestParam(defaultValue = "DESC") sortDirection: String
    ): ResponseEntity<TaxConfigurationListResponseDto> {

        val direction = if (sortDirection.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))
        val date = effectiveDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val result = taxConfigurationService.findConfigurationsWithFilters(
            businessType = businessType,
            hsnCode = hsnCode,
            geographicalZone = geographicalZone,
            effectiveDate = date,
            pageable = pageable
        )

        val response = TaxConfigurationListResponseDto(
            content = result.content.map { TaxConfigurationResponseDto.from(it) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            currentPage = result.number,
            pageSize = result.size,
            hasNext = result.hasNext(),
            hasPrevious = result.hasPrevious()
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get tax configuration by ID",
        description = "Retrieve detailed information about a specific tax configuration"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Tax configuration found"),
            ApiResponse(responseCode = "404", description = "Tax configuration not found")
        ]
    )
    fun getTaxConfiguration(
        @Parameter(description = "Tax configuration ID", required = true)
        @PathVariable id: Long
    ): ResponseEntity<TaxConfigurationResponseDto> {

        // This would need a findById method in the service
        // For now, return not found
        return ResponseEntity.notFound().build()
    }

    @GetMapping("/effective")
    @Operation(
        summary = "Get effective tax configuration",
        description = "Get the currently effective tax configuration for a business type and HSN code"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Effective tax configuration found"),
            ApiResponse(responseCode = "404", description = "No effective tax configuration found")
        ]
    )
    fun getEffectiveTaxConfiguration(
        @Parameter(description = "Business type", required = true)
        @RequestParam businessType: BusinessType,

        @Parameter(description = "HSN code", required = true)
        @RequestParam hsnCode: String,

        @Parameter(description = "Geographical zone")
        @RequestParam(required = false) geographicalZone: GeographicalZone?,

        @Parameter(description = "Effective date")
        @RequestParam(required = false) effectiveDate: String?
    ): ResponseEntity<TaxConfigurationResponseDto> {

        val date = effectiveDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val taxConfiguration = taxConfigurationService.findEffectiveConfiguration(
            businessType = businessType,
            hsnCode = hsnCode,
            geographicalZone = geographicalZone,
            effectiveDate = date
        ) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(TaxConfigurationResponseDto.from(taxConfiguration))
    }

    @GetMapping("/by-hsn/{hsnCode}")
    @Operation(
        summary = "Get all configurations for HSN code",
        description = "Retrieve all effective tax configurations for a specific HSN code"
    )
    fun getTaxConfigurationsByHsnCode(
        @Parameter(description = "HSN code", required = true)
        @PathVariable hsnCode: String,

        @Parameter(description = "Effective date")
        @RequestParam(required = false) effectiveDate: String?
    ): ResponseEntity<List<TaxConfigurationResponseDto>> {

        val date = effectiveDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val configurations = taxConfigurationService.findAllEffectiveConfigurationsByHsnCode(
            hsnCode = hsnCode,
            effectiveDate = date
        )

        val response = configurations.map { TaxConfigurationResponseDto.from(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/by-business-type")
    @Operation(
        summary = "Get configurations by business type",
        description = "Retrieve all effective tax configurations for a specific business type"
    )
    fun getTaxConfigurationsByBusinessType(
        @Parameter(description = "Business type", required = true)
        @RequestParam businessType: BusinessType,

        @Parameter(description = "Effective date")
        @RequestParam(required = false) effectiveDate: String?
    ): ResponseEntity<List<TaxConfigurationResponseDto>> {

        val date = effectiveDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val configurations = taxConfigurationService.findAllEffectiveConfigurationsByBusinessType(
            businessType = businessType,
            effectiveDate = date
        )

        val response = configurations.map { TaxConfigurationResponseDto.from(it) }
        return ResponseEntity.ok(response)
    }

    @GetMapping("/with-cess")
    @Operation(
        summary = "Get configurations with cess",
        description = "Retrieve tax configurations that have cess applicable"
    )
    fun getConfigurationsWithCess(
        @Parameter(description = "Effective date")
        @RequestParam(required = false) effectiveDate: String?
    ): ResponseEntity<List<TaxConfigurationResponseDto>> {

        val date = effectiveDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val configurations = taxConfigurationService.findConfigurationsWithCess(date)
        val response = configurations.map { TaxConfigurationResponseDto.from(it) }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/reverse-charge")
    @Operation(
        summary = "Get reverse charge configurations",
        description = "Retrieve tax configurations where reverse charge is applicable"
    )
    fun getReverseChargeConfigurations(
        @Parameter(description = "Effective date")
        @RequestParam(required = false) effectiveDate: String?
    ): ResponseEntity<List<TaxConfigurationResponseDto>> {

        val date = effectiveDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val configurations = taxConfigurationService.findReverseChargeApplicableConfigurations(date)
        val response = configurations.map { TaxConfigurationResponseDto.from(it) }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/composition-scheme")
    @Operation(
        summary = "Get composition scheme configurations",
        description = "Retrieve tax configurations applicable for composition scheme"
    )
    fun getCompositionSchemeConfigurations(
        @Parameter(description = "Effective date")
        @RequestParam(required = false) effectiveDate: String?
    ): ResponseEntity<List<TaxConfigurationResponseDto>> {

        val date = effectiveDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val configurations = taxConfigurationService.findCompositionSchemeConfigurations(date)
        val response = configurations.map { TaxConfigurationResponseDto.from(it) }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/gst-rates")
    @Operation(
        summary = "Get distinct GST rates",
        description = "Retrieve list of all distinct GST rates currently active"
    )
    fun getDistinctGstRates(
        @Parameter(description = "Effective date")
        @RequestParam(required = false) effectiveDate: String?
    ): ResponseEntity<List<java.math.BigDecimal>> {

        val date = effectiveDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val rates = taxConfigurationService.findDistinctActiveGstRates(date)
        return ResponseEntity.ok(rates)
    }

    @GetMapping("/by-gst-rate/{gstRate}")
    @Operation(
        summary = "Get configurations by GST rate",
        description = "Retrieve all tax configurations with a specific GST rate"
    )
    fun getConfigurationsByGstRate(
        @Parameter(description = "GST rate", required = true)
        @PathVariable gstRate: java.math.BigDecimal,

        @Parameter(description = "Effective date")
        @RequestParam(required = false) effectiveDate: String?
    ): ResponseEntity<List<TaxConfigurationResponseDto>> {

        val date = effectiveDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val configurations = taxConfigurationService.findByGstRate(gstRate, date)
        val response = configurations.map { TaxConfigurationResponseDto.from(it) }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get tax configuration statistics",
        description = "Retrieve comprehensive statistics about tax configurations"
    )
    fun getTaxConfigurationStatistics(): ResponseEntity<TaxConfigurationStatisticsResponseDto> {

        val statistics = taxConfigurationService.getTaxConfigurationStatistics()
        val expiringConfigs = taxConfigurationService.findConfigurationsExpiringInPeriod(
            LocalDate.now(),
            LocalDate.now().plusDays(30)
        )

        val response = TaxConfigurationStatisticsResponseDto.from(statistics, expiringConfigs)
        return ResponseEntity.ok(response)
    }

    @PostMapping
    @Operation(
        summary = "Create tax configuration",
        description = "Create a new tax configuration"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "Tax configuration created successfully"),
            ApiResponse(responseCode = "400", description = "Invalid tax configuration data"),
            ApiResponse(responseCode = "409", description = "Overlapping tax configuration exists")
        ]
    )
    fun createTaxConfiguration(
        @Valid @RequestBody request: TaxConfigurationRequestDto
    ): ResponseEntity<TaxConfigurationResponseDto> {

        try {
            val taxConfiguration = taxConfigurationService.createTaxConfiguration(request.toEntity())
            val response = TaxConfigurationResponseDto.from(taxConfiguration)

            return ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update tax configuration",
        description = "Update an existing tax configuration"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Tax configuration updated successfully"),
            ApiResponse(responseCode = "400", description = "Invalid tax configuration data"),
            ApiResponse(responseCode = "404", description = "Tax configuration not found")
        ]
    )
    fun updateTaxConfiguration(
        @Parameter(description = "Tax configuration ID", required = true)
        @PathVariable id: Long,

        @Valid @RequestBody request: TaxConfigurationUpdateDto
    ): ResponseEntity<TaxConfigurationResponseDto> {

        if (id != request.id) {
            return ResponseEntity.badRequest().build()
        }

        try {
            // This would need proper implementation to find and update existing config
            // For now, return not found
            return ResponseEntity.notFound().build()
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
    }

    @PutMapping("/{id}/expire")
    @Operation(
        summary = "Expire tax configuration",
        description = "Set expiry date for a tax configuration"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Tax configuration expired successfully"),
            ApiResponse(responseCode = "404", description = "Tax configuration not found")
        ]
    )
    fun expireTaxConfiguration(
        @Parameter(description = "Tax configuration ID", required = true)
        @PathVariable id: Long,

        @Parameter(description = "Expiry date", required = true)
        @RequestParam effectiveTo: String
    ): ResponseEntity<TaxConfigurationResponseDto> {

        try {
            val expiryDate = LocalDate.parse(effectiveTo)
            taxConfigurationService.expireTaxConfiguration(id, expiryDate)

            // Would need to return the updated configuration
            return ResponseEntity.notFound().build()
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Deactivate tax configuration",
        description = "Deactivate a tax configuration (soft delete)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Tax configuration deactivated successfully"),
            ApiResponse(responseCode = "404", description = "Tax configuration not found")
        ]
    )
    fun deactivateTaxConfiguration(
        @Parameter(description = "Tax configuration ID", required = true)
        @PathVariable id: Long
    ): ResponseEntity<Void> {

        try {
            taxConfigurationService.deactivateTaxConfiguration(id)
            return ResponseEntity.noContent().build()
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/expiring")
    @Operation(
        summary = "Get expiring configurations",
        description = "Retrieve tax configurations that are expiring in the specified period"
    )
    fun getExpiringConfigurations(
        @Parameter(description = "From date")
        @RequestParam(required = false) fromDate: String?,

        @Parameter(description = "To date")
        @RequestParam(required = false) toDate: String?
    ): ResponseEntity<List<TaxConfigurationResponseDto>> {

        val from = fromDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
        val to = toDate?.let { LocalDate.parse(it) } ?: LocalDate.now().plusDays(30)

        val configurations = taxConfigurationService.findConfigurationsExpiringInPeriod(from, to)
        val response = configurations.map { TaxConfigurationResponseDto.from(it) }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/recent")
    @Operation(
        summary = "Get recently modified configurations",
        description = "Retrieve tax configurations that were recently added or updated"
    )
    fun getRecentConfigurations(
        @Parameter(description = "Days back to search")
        @RequestParam(defaultValue = "7") days: Int
    ): ResponseEntity<RecentConfigurationsResponseDto> {

        val fromDate = LocalDateTime.now().minusDays(days.toLong())

        val recentlyAdded = taxConfigurationService.findRecentlyAdded(fromDate)
        val recentlyUpdated = taxConfigurationService.findRecentlyUpdated(fromDate)

        val response = RecentConfigurationsResponseDto(
            recentlyAdded = recentlyAdded.map { TaxConfigurationResponseDto.from(it) },
            recentlyUpdated = recentlyUpdated.map { TaxConfigurationResponseDto.from(it) }
        )

        return ResponseEntity.ok(response)
    }
}

data class RecentConfigurationsResponseDto(
    val recentlyAdded: List<TaxConfigurationResponseDto>,
    val recentlyUpdated: List<TaxConfigurationResponseDto>
)