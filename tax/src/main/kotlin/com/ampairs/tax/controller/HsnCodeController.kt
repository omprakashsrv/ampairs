package com.ampairs.tax.controller

import com.ampairs.tax.domain.dto.*
import com.ampairs.tax.service.HsnCodeService
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
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/v1/hsn-codes")
@Validated
@Tag(name = "HSN Code Management", description = "APIs for managing HSN (Harmonized System of Nomenclature) codes")
class HsnCodeController(
    private val hsnCodeService: HsnCodeService
) {

    @GetMapping
    @Operation(
        summary = "Search HSN codes",
        description = "Search and filter HSN codes with pagination support"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "HSN codes retrieved successfully"),
            ApiResponse(responseCode = "400", description = "Invalid search parameters")
        ]
    )
    fun searchHsnCodes(
        @Parameter(description = "Search term for HSN code or description")
        @RequestParam(required = false) searchTerm: String?,

        @Parameter(description = "Filter by chapter")
        @RequestParam(required = false) chapter: String?,

        @Parameter(description = "Filter by heading")
        @RequestParam(required = false) heading: String?,

        @Parameter(description = "Filter by level")
        @RequestParam(required = false) level: Int?,

        @Parameter(description = "Filter by exemption availability")
        @RequestParam(required = false) exemptionAvailable: Boolean?,

        @Parameter(description = "Filter by business type applicability")
        @RequestParam(required = false) businessType: String?,

        @Parameter(description = "Filter by active status")
        @RequestParam(defaultValue = "true") isActive: Boolean,

        @Parameter(description = "Page number (0-based)")
        @RequestParam(defaultValue = "0") page: Int,

        @Parameter(description = "Page size")
        @RequestParam(defaultValue = "20") size: Int,

        @Parameter(description = "Sort field")
        @RequestParam(defaultValue = "hsnCode") sortBy: String,

        @Parameter(description = "Sort direction (ASC/DESC)")
        @RequestParam(defaultValue = "ASC") sortDirection: String
    ): ResponseEntity<HsnCodeListResponseDto> {

        val direction = if (sortDirection.uppercase() == "DESC") Sort.Direction.DESC else Sort.Direction.ASC
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))

        val searchRequest = HsnCodeSearchRequestDto(
            searchTerm = searchTerm,
            chapter = chapter,
            heading = heading,
            level = level,
            exemptionAvailable = exemptionAvailable,
            businessType = businessType,
            isActive = isActive,
            page = page,
            size = size,
            sortBy = sortBy,
            sortDirection = sortDirection
        )

        val result = hsnCodeService.searchHsnCodes(searchTerm, pageable)

        val response = HsnCodeListResponseDto(
            content = result.content.map { HsnCodeResponseDto.from(it) },
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            currentPage = result.number,
            pageSize = result.size,
            hasNext = result.hasNext(),
            hasPrevious = result.hasPrevious()
        )

        return ResponseEntity.ok(response)
    }

    @GetMapping("/{uid}")
    @Operation(
        summary = "Get HSN code by UID",
        description = "Retrieve detailed information about a specific HSN code by its UID"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "HSN code found"),
            ApiResponse(responseCode = "404", description = "HSN code not found")
        ]
    )
    fun getHsnCode(
        @Parameter(description = "HSN code UID", required = true)
        @PathVariable uid: String
    ): ResponseEntity<HsnCodeResponseDto> {

        val hsn = hsnCodeService.findByUid(uid)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(HsnCodeResponseDto.from(hsn))
    }

    @GetMapping("/{hsnCode}/with-tax-rates")
    @Operation(
        summary = "Get HSN code with tax rates",
        description = "Retrieve HSN code information along with applicable tax rates"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "HSN code with tax rates found"),
            ApiResponse(responseCode = "404", description = "HSN code not found")
        ]
    )
    fun getHsnCodeWithTaxRates(
        @Parameter(description = "HSN code", required = true)
        @PathVariable hsnCode: String,

        @Parameter(description = "Effective date for tax rates")
        @RequestParam(required = false) effectiveDate: String?
    ): ResponseEntity<HsnCodeResponseDto> {

        val date = effectiveDate?.let { LocalDateTime.parse(it) } ?: LocalDateTime.now()
        val hsn = hsnCodeService.findWithActiveTaxRates(hsnCode, date)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(HsnCodeResponseDto.from(hsn))
    }

    @GetMapping("/chapters")
    @Operation(
        summary = "Get all chapters",
        description = "Retrieve list of all available HSN chapters"
    )
    fun getChapters(): ResponseEntity<List<String>> {
        val chapters = hsnCodeService.findDistinctChapters()
        return ResponseEntity.ok(chapters)
    }

    @GetMapping("/headings")
    @Operation(
        summary = "Get headings by chapter",
        description = "Retrieve list of headings for a specific chapter or all headings"
    )
    fun getHeadings(
        @Parameter(description = "Filter by chapter")
        @RequestParam(required = false) chapter: String?
    ): ResponseEntity<List<String>> {
        val headings = hsnCodeService.findDistinctHeadings(chapter)
        return ResponseEntity.ok(headings)
    }

    @GetMapping("/hierarchy/{hsnId}")
    @Operation(
        summary = "Get HSN code hierarchy",
        description = "Retrieve the complete hierarchy of a HSN code including parents and children"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "Hierarchy retrieved successfully"),
            ApiResponse(responseCode = "404", description = "HSN code not found")
        ]
    )
    fun getHsnCodeHierarchy(
        @Parameter(description = "HSN code ID", required = true)
        @PathVariable hsnId: Long
    ): ResponseEntity<List<HsnCodeResponseDto>> {

        val hierarchy = hsnCodeService.findHsnCodeHierarchy(hsnId)
        val response = hierarchy.map { HsnCodeResponseDto.from(it) }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/by-level/{level}")
    @Operation(
        summary = "Get HSN codes by level",
        description = "Retrieve HSN codes filtered by their hierarchical level"
    )
    fun getHsnCodesByLevel(
        @Parameter(description = "HSN code level (1-3)", required = true)
        @PathVariable level: Int
    ): ResponseEntity<List<HsnCodeResponseDto>> {

        if (level < 1 || level > 3) {
            return ResponseEntity.badRequest().build()
        }

        val hsnCodes = hsnCodeService.findByLevel(level)
        val response = hsnCodes.map { HsnCodeResponseDto.from(it) }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/with-exemption")
    @Operation(
        summary = "Get HSN codes with exemptions",
        description = "Retrieve HSN codes that have exemptions available"
    )
    fun getHsnCodesWithExemption(): ResponseEntity<List<HsnCodeResponseDto>> {
        val hsnCodes = hsnCodeService.findHsnCodesWithExemption()
        val response = hsnCodes.map { HsnCodeResponseDto.from(it) }

        return ResponseEntity.ok(response)
    }

    @GetMapping("/statistics")
    @Operation(
        summary = "Get HSN code statistics",
        description = "Retrieve comprehensive statistics about HSN codes in the system"
    )
    fun getHsnCodeStatistics(): ResponseEntity<HsnCodeStatisticsResponseDto> {
        val statistics = hsnCodeService.getHsnCodeStatistics()
        val recentlyAdded = hsnCodeService.findRecentlyAdded(LocalDateTime.now().minusDays(30))

        val response = HsnCodeStatisticsResponseDto.from(statistics, recentlyAdded)
        return ResponseEntity.ok(response)
    }

    @PostMapping
    @Operation(
        summary = "Create or update HSN code",
        description = "Create a new HSN code or update an existing one (UPSERT operation)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "HSN code updated successfully"),
            ApiResponse(responseCode = "201", description = "HSN code created successfully"),
            ApiResponse(responseCode = "400", description = "Invalid HSN code data")
        ]
    )
    fun createOrUpdateHsnCode(
        @Valid @RequestBody request: HsnCodeRequestDto
    ): ResponseEntity<HsnCodeResponseDto> {
        // Check if HSN code already exists by UID (if provided) or HSN code
        val existingHsn = request.uid?.let { uid ->
            hsnCodeService.findByUid(uid)
        } ?: hsnCodeService.findByHsnCode(request.hsnCode)

        val result = if (existingHsn != null) {
            // Update existing HSN code
            val updateDto = HsnCodeUpdateDto(
                uid = existingHsn.uid,
                hsnDescription = request.hsnDescription,
                unitOfMeasurement = request.unitOfMeasurement,
                exemptionAvailable = request.exemptionAvailable,
                businessCategoryRules = request.businessCategoryRules,
                attributes = request.attributes,
                effectiveFrom = request.effectiveFrom,
                effectiveTo = request.effectiveTo,
                isActive = request.isActive
            )
            val updated = hsnCodeService.updateHsnCodeByUid(existingHsn.uid, updateDto)
            ResponseEntity.ok(HsnCodeResponseDto.from(updated))
        } else {
            // Create new HSN code
            val created = hsnCodeService.createHsnCode(request.toEntity())
            ResponseEntity.status(HttpStatus.CREATED).body(HsnCodeResponseDto.from(created))
        }

        return result
    }


    @DeleteMapping("/{uid}")
    @Operation(
        summary = "Deactivate HSN code",
        description = "Deactivate an HSN code (soft delete)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "HSN code deactivated successfully"),
            ApiResponse(responseCode = "404", description = "HSN code not found")
        ]
    )
    fun deactivateHsnCode(
        @Parameter(description = "HSN code UID", required = true)
        @PathVariable uid: String
    ): ResponseEntity<Void> {
        hsnCodeService.deactivateHsnCodeByUid(uid)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/validate/{hsnCode}")
    @Operation(
        summary = "Validate HSN code",
        description = "Validate if an HSN code exists and is active"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "HSN code is valid"),
            ApiResponse(responseCode = "404", description = "HSN code is invalid or not found")
        ]
    )
    fun validateHsnCode(
        @Parameter(description = "HSN code to validate", required = true)
        @PathVariable hsnCode: String
    ): ResponseEntity<HsnCodeValidationResponseDto> {

        val hsn = hsnCodeService.validateHsnCodeExists(hsnCode)
        val response = HsnCodeValidationResponseDto(
            hsnCode = hsnCode,
            isValid = true,
            description = hsn.hsnDescription,
            hasValidTaxRates = hsn.hasValidTaxRates(),
            message = "HSN code is valid"
        )
        return ResponseEntity.ok(response)
    }
}

data class HsnCodeValidationResponseDto(
    val hsnCode: String,
    val isValid: Boolean,
    val description: String?,
    val hasValidTaxRates: Boolean,
    val message: String
)