package com.ampairs.tax.repository

import com.ampairs.tax.domain.model.HsnCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface HsnCodeRepository : JpaRepository<HsnCode, Long> {

    fun findByHsnCodeAndActiveTrue(hsnCode: String): HsnCode?

    @Query("""
        SELECT h FROM HsnCode h
        WHERE h.hsnCode = :hsnCode
        AND h.active = true
        AND (:date IS NULL OR (h.effectiveFrom IS NULL OR h.effectiveFrom <= :date))
        AND (:date IS NULL OR (h.effectiveTo IS NULL OR h.effectiveTo >= :date))
    """)
    fun findByHsnCodeAndValidForDate(
        @Param("hsnCode") hsnCode: String,
        @Param("date") date: LocalDateTime = LocalDateTime.now()
    ): HsnCode?

    fun findByHsnChapterAndActiveTrueOrderByHsnCode(hsnChapter: String): List<HsnCode>

    fun findByHsnHeadingAndActiveTrueOrderByHsnCode(hsnHeading: String): List<HsnCode>

    fun findByParentHsnIdAndActiveTrueOrderByHsnCode(parentHsnId: Long): List<HsnCode>

    @EntityGraph("HsnCode.withTaxRates")
    @Query("""
        SELECT h FROM HsnCode h
        WHERE h.hsnCode = :hsnCode
        AND h.active = true
        AND EXISTS (
            SELECT tr FROM h.taxRates tr
            WHERE tr.active = true
            AND (:date IS NULL OR (tr.effectiveFrom <= :date))
            AND (:date IS NULL OR (tr.effectiveTo IS NULL OR tr.effectiveTo >= :date))
        )
    """)
    fun findByHsnCodeWithActiveTaxRates(
        @Param("hsnCode") hsnCode: String,
        @Param("date") date: LocalDateTime = LocalDateTime.now()
    ): HsnCode?

    @Query("""
        SELECT h FROM HsnCode h
        WHERE h.active = true
        AND (:searchTerm IS NULL OR (
            LOWER(h.hsnCode) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR
            LOWER(h.hsnDescription) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
        ))
        ORDER BY h.hsnCode
    """)
    fun searchActiveHsnCodes(@Param("searchTerm") searchTerm: String?, pageable: Pageable): Page<HsnCode>

    fun findByActiveTrueAndLevelOrderByHsnCode(level: Int): List<HsnCode>

    @Query("""
        SELECT DISTINCT h.hsnChapter FROM HsnCode h
        WHERE h.active = true
        AND h.hsnChapter IS NOT NULL
        ORDER BY h.hsnChapter
    """)
    fun findDistinctActiveChapters(): List<String>

    @Query("""
        SELECT DISTINCT h.hsnHeading FROM HsnCode h
        WHERE h.active = true
        AND h.hsnHeading IS NOT NULL
        AND (:chapter IS NULL OR h.hsnChapter = :chapter)
        ORDER BY h.hsnHeading
    """)
    fun findDistinctActiveHeadings(@Param("chapter") chapter: String?): List<String>

    fun findByActiveTrueAndExemptionAvailableTrueOrderByHsnCode(): List<HsnCode>

    @Query("""
        WITH RECURSIVE hsn_hierarchy AS (
            SELECT h.id, h.hsn_code, h.hsn_description, h.parent_hsn_id, 1 as depth
            FROM hsn_codes h
            WHERE h.id = :hsnId
            UNION ALL
            SELECT p.id, p.hsn_code, p.hsn_description, p.parent_hsn_id, hh.depth + 1
            FROM hsn_codes p
            INNER JOIN hsn_hierarchy hh ON p.id = hh.parent_hsn_id
        )
        SELECT h.* FROM hsn_codes h
        INNER JOIN hsn_hierarchy hh ON h.id = hh.id
        ORDER BY hh.depth DESC
    """, nativeQuery = true)
    fun findHsnCodeHierarchy(@Param("hsnId") hsnId: Long): List<HsnCode>

    @Query("""
        SELECT * FROM hsn_codes h
        WHERE h.active = true
        AND JSON_EXTRACT(h.business_category_rules, '$.applicableBusinessTypes') LIKE :businessType
        ORDER BY h.hsn_code
    """, nativeQuery = true)
    fun findByBusinessTypeApplicability(@Param("businessType") businessType: String): List<HsnCode>

    fun countByActiveTrue(): Long

    fun findByActiveTrueAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(fromDate: LocalDateTime): List<HsnCode>
}