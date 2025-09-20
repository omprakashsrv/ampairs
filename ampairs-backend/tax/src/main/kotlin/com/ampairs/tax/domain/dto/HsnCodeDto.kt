package com.ampairs.tax.domain.dto

import com.ampairs.tax.domain.model.HsnCode
import com.ampairs.tax.service.HsnCodeStatistics
import jakarta.validation.constraints.*
import java.time.LocalDateTime

data class HsnCodeRequestDto(
    @field:NotBlank(message = "HSN code is required")
    @field:Pattern(regexp = "\\d{4,8}", message = "HSN code should be 4-8 digits")
    val hsnCode: String,

    @field:NotBlank(message = "HSN description is required")
    @field:Size(max = 1000, message = "Description is too long")
    val hsnDescription: String,

    @field:Size(max = 2, message = "Chapter should be 2 digits")
    @field:Pattern(regexp = "\\d{2}", message = "Chapter should be numeric")
    val hsnChapter: String? = null,

    @field:Size(max = 4, message = "Heading should be 4 digits")
    @field:Pattern(regexp = "\\d{4}", message = "Heading should be numeric")
    val hsnHeading: String? = null,

    val parentHsnId: Long? = null,

    @field:Min(value = 1, message = "Level must be at least 1")
    @field:Max(value = 3, message = "Level cannot exceed 3")
    val level: Int = 1,

    @field:Size(max = 50, message = "Unit of measurement too long")
    val unitOfMeasurement: String? = null,

    val exemptionAvailable: Boolean = false,

    val businessCategoryRules: Map<String, Any> = emptyMap(),

    val attributes: Map<String, Any> = emptyMap(),

    val effectiveFrom: LocalDateTime? = null,

    val effectiveTo: LocalDateTime? = null,

    val isActive: Boolean = true
) {
    fun toEntity(): HsnCode {
        return HsnCode().apply {
            hsnCode = this@HsnCodeRequestDto.hsnCode
            hsnDescription = this@HsnCodeRequestDto.hsnDescription
            hsnChapter = this@HsnCodeRequestDto.hsnChapter
            hsnHeading = this@HsnCodeRequestDto.hsnHeading
            parentHsnId = this@HsnCodeRequestDto.parentHsnId
            level = this@HsnCodeRequestDto.level
            unitOfMeasurement = this@HsnCodeRequestDto.unitOfMeasurement
            exemptionAvailable = this@HsnCodeRequestDto.exemptionAvailable
            businessCategoryRules = this@HsnCodeRequestDto.businessCategoryRules
            attributes = this@HsnCodeRequestDto.attributes
            effectiveFrom = this@HsnCodeRequestDto.effectiveFrom
            effectiveTo = this@HsnCodeRequestDto.effectiveTo
            active = this@HsnCodeRequestDto.isActive
        }
    }
}

data class HsnCodeResponseDto(
    val id: Long,
    val uid: String,
    val hsnCode: String,
    val hsnDescription: String,
    val hsnChapter: String?,
    val hsnHeading: String?,
    val parentHsnId: Long?,
    val parentHsnCode: String?,
    val level: Int,
    val unitOfMeasurement: String?,
    val exemptionAvailable: Boolean,
    val businessCategoryRules: Map<String, Any>,
    val attributes: Map<String, Any>,
    val effectiveFrom: LocalDateTime?,
    val effectiveTo: LocalDateTime?,
    val isActive: Boolean,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
    val hasValidTaxRates: Boolean,
    val fullPath: String,
    val childCount: Int
) {
    companion object {
        fun from(hsnCode: HsnCode): HsnCodeResponseDto {
            return HsnCodeResponseDto(
                id = hsnCode.id,
                uid = hsnCode.uid,
                hsnCode = hsnCode.hsnCode,
                hsnDescription = hsnCode.hsnDescription,
                hsnChapter = hsnCode.hsnChapter,
                hsnHeading = hsnCode.hsnHeading,
                parentHsnId = hsnCode.parentHsnId,
                parentHsnCode = hsnCode.parentHsn?.hsnCode,
                level = hsnCode.level,
                unitOfMeasurement = hsnCode.unitOfMeasurement,
                exemptionAvailable = hsnCode.exemptionAvailable,
                businessCategoryRules = hsnCode.businessCategoryRules,
                attributes = hsnCode.attributes,
                effectiveFrom = hsnCode.effectiveFrom,
                effectiveTo = hsnCode.effectiveTo,
                isActive = hsnCode.active,
                createdAt = hsnCode.createdAt,
                updatedAt = hsnCode.updatedAt,
                hasValidTaxRates = hsnCode.hasValidTaxRates(),
                fullPath = hsnCode.getFullPath(),
                childCount = hsnCode.childHsnCodes.size
            )
        }
    }
}

data class HsnCodeUpdateDto(
    @field:NotNull(message = "ID is required")
    val id: Long,

    @field:NotBlank(message = "HSN description is required")
    @field:Size(max = 1000, message = "Description is too long")
    val hsnDescription: String,

    @field:Size(max = 50, message = "Unit of measurement too long")
    val unitOfMeasurement: String? = null,

    val exemptionAvailable: Boolean = false,

    val businessCategoryRules: Map<String, Any> = emptyMap(),

    val attributes: Map<String, Any> = emptyMap(),

    val effectiveFrom: LocalDateTime? = null,

    val effectiveTo: LocalDateTime? = null,

    val isActive: Boolean = true
)

data class HsnCodeSearchRequestDto(
    val searchTerm: String? = null,
    val chapter: String? = null,
    val heading: String? = null,
    val level: Int? = null,
    val exemptionAvailable: Boolean? = null,
    val businessType: String? = null,
    val isActive: Boolean = true,
    val page: Int = 0,
    val size: Int = 20,
    val sortBy: String = "hsnCode",
    val sortDirection: String = "ASC"
)

data class HsnCodeListResponseDto(
    val content: List<HsnCodeResponseDto>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

data class HsnCodeHierarchyResponseDto(
    val hsnCode: HsnCodeResponseDto,
    val children: List<HsnCodeHierarchyResponseDto>
) {
    companion object {
        fun from(hsnCode: HsnCode): HsnCodeHierarchyResponseDto {
            return HsnCodeHierarchyResponseDto(
                hsnCode = HsnCodeResponseDto.from(hsnCode),
                children = hsnCode.childHsnCodes.map { from(it) }
            )
        }
    }
}

data class HsnCodeStatisticsResponseDto(
    val totalActiveHsnCodes: Long,
    val totalChapters: Int,
    val totalHeadings: Int,
    val hsnCodesWithExemption: Int,
    val distributionByLevel: Map<Int, Long>,
    val recentlyAdded: List<HsnCodeResponseDto>
) {
    companion object {
        fun from(statistics: HsnCodeStatistics, recentlyAdded: List<HsnCode> = emptyList()): HsnCodeStatisticsResponseDto {
            return HsnCodeStatisticsResponseDto(
                totalActiveHsnCodes = statistics.totalActiveHsnCodes,
                totalChapters = statistics.totalChapters,
                totalHeadings = statistics.totalHeadings,
                hsnCodesWithExemption = statistics.hsnCodesWithExemption,
                distributionByLevel = emptyMap(), // Can be populated with additional query
                recentlyAdded = recentlyAdded.map { HsnCodeResponseDto.from(it) }
            )
        }
    }
}

data class ChapterResponseDto(
    val chapter: String,
    val description: String,
    val hsnCodeCount: Int
)

data class HeadingResponseDto(
    val heading: String,
    val chapter: String,
    val description: String,
    val hsnCodeCount: Int
)