package com.ampairs.tax.service

import com.ampairs.tax.domain.model.HsnCode
import com.ampairs.tax.repository.HsnCodeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class HsnCodeService(
    private val hsnCodeRepository: HsnCodeRepository
) {

    fun findByHsnCode(hsnCode: String): HsnCode? {
        return hsnCodeRepository.findByHsnCodeAndActiveTrue(hsnCode)
    }

    fun findByHsnCodeAndValidForDate(hsnCode: String, date: LocalDateTime = LocalDateTime.now()): HsnCode? {
        return hsnCodeRepository.findByHsnCodeAndValidForDate(hsnCode, date)
    }

    fun findByChapter(chapter: String): List<HsnCode> {
        return hsnCodeRepository.findByHsnChapterAndActiveTrueOrderByHsnCode(chapter)
    }

    fun findByHeading(heading: String): List<HsnCode> {
        return hsnCodeRepository.findByHsnHeadingAndActiveTrueOrderByHsnCode(heading)
    }

    fun findByParentHsnId(parentId: Long): List<HsnCode> {
        return hsnCodeRepository.findByParentHsnIdAndActiveTrueOrderByHsnCode(parentId)
    }

    fun findWithActiveTaxRates(hsnCode: String, date: LocalDateTime = LocalDateTime.now()): HsnCode? {
        return hsnCodeRepository.findByHsnCodeWithActiveTaxRates(hsnCode, date)
    }

    fun searchHsnCodes(searchTerm: String?, pageable: Pageable): Page<HsnCode> {
        return hsnCodeRepository.searchActiveHsnCodes(searchTerm, pageable)
    }

    fun findByLevel(level: Int): List<HsnCode> {
        return hsnCodeRepository.findByActiveTrueAndLevelOrderByHsnCode(level)
    }

    fun findDistinctChapters(): List<String> {
        return hsnCodeRepository.findDistinctActiveChapters()
    }

    fun findDistinctHeadings(chapter: String? = null): List<String> {
        return hsnCodeRepository.findDistinctActiveHeadings(chapter)
    }

    fun findHsnCodesWithExemption(): List<HsnCode> {
        return hsnCodeRepository.findByActiveTrueAndExemptionAvailableTrueOrderByHsnCode()
    }

    fun findHsnCodeHierarchy(hsnId: Long): List<HsnCode> {
        return hsnCodeRepository.findHsnCodeHierarchy(hsnId)
    }

    fun findByBusinessTypeApplicability(businessType: String): List<HsnCode> {
        return hsnCodeRepository.findByBusinessTypeApplicability("%$businessType%")
    }

    fun countActiveHsnCodes(): Long {
        return hsnCodeRepository.countByActiveTrue()
    }

    fun findRecentlyAdded(fromDate: LocalDateTime): List<HsnCode> {
        return hsnCodeRepository.findByActiveTrueAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(fromDate)
    }

    @Transactional
    fun createHsnCode(hsnCode: HsnCode): HsnCode {
        validateHsnCode(hsnCode)
        return hsnCodeRepository.save(hsnCode)
    }

    @Transactional
    fun updateHsnCode(hsnCode: HsnCode): HsnCode {
        validateHsnCode(hsnCode)
        return hsnCodeRepository.save(hsnCode)
    }

    @Transactional
    fun deactivateHsnCode(hsnId: Long) {
        val hsnCode = hsnCodeRepository.findById(hsnId)
            .orElseThrow { IllegalArgumentException("HSN code not found with id: $hsnId") }

        hsnCode.active = false
        hsnCodeRepository.save(hsnCode)
    }

    private fun validateHsnCode(hsnCode: HsnCode) {
        if (hsnCode.hsnCode.isBlank()) {
            throw IllegalArgumentException("HSN code cannot be blank")
        }

        if (hsnCode.hsnDescription.isBlank()) {
            throw IllegalArgumentException("HSN description cannot be blank")
        }

        // Check for duplicate HSN code
        val existing = hsnCodeRepository.findByHsnCodeAndActiveTrue(hsnCode.hsnCode)
        if (existing != null && existing.id != hsnCode.id) {
            throw IllegalArgumentException("HSN code ${hsnCode.hsnCode} already exists")
        }

        // Validate HSN code format (should be numeric and appropriate length)
        if (!hsnCode.hsnCode.matches(Regex("\\d{4,8}"))) {
            throw IllegalArgumentException("HSN code should be 4-8 digits")
        }

        // Validate level based on HSN code length
        val expectedLevel = when (hsnCode.hsnCode.length) {
            4 -> 1  // Chapter level
            6 -> 2  // Heading level
            8 -> 3  // Sub-heading level
            else -> throw IllegalArgumentException("Invalid HSN code length")
        }

        if (hsnCode.level != expectedLevel) {
            hsnCode.level = expectedLevel
        }

        // Set chapter and heading based on HSN code
        if (hsnCode.hsnCode.length >= 4) {
            hsnCode.hsnChapter = hsnCode.hsnCode.substring(0, 2)
            hsnCode.hsnHeading = hsnCode.hsnCode.substring(0, 4)
        }
    }

    fun validateHsnCodeExists(hsnCode: String): HsnCode {
        return findByHsnCode(hsnCode)
            ?: throw IllegalArgumentException("HSN code $hsnCode not found")
    }

    fun getHsnCodeStatistics(): HsnCodeStatistics {
        val totalActive = countActiveHsnCodes()
        val chapters = findDistinctChapters().size
        val headings = findDistinctHeadings().size
        val withExemption = findHsnCodesWithExemption().size

        return HsnCodeStatistics(
            totalActiveHsnCodes = totalActive,
            totalChapters = chapters,
            totalHeadings = headings,
            hsnCodesWithExemption = withExemption
        )
    }
}

data class HsnCodeStatistics(
    val totalActiveHsnCodes: Long,
    val totalChapters: Int,
    val totalHeadings: Int,
    val hsnCodesWithExemption: Int
)