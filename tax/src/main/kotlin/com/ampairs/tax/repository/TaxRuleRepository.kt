package com.ampairs.tax.repository

import com.ampairs.tax.domain.model.TaxRule
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface TaxRuleRepository : JpaRepository<TaxRule, Long> {

    fun findByUid(uid: String): TaxRule?

    @Query(
        """
        SELECT tr FROM TaxRule tr
        WHERE tr.updatedAt > :modifiedAfter
        AND tr.isActive = true
        ORDER BY tr.updatedAt ASC
    """
    )
    fun findByUpdatedAtAfter(
        @Param("modifiedAfter") modifiedAfter: Instant,
        pageable: Pageable
    ): Page<TaxRule>

    @Query(
        """
        SELECT tr FROM TaxRule tr
        WHERE (:taxCode IS NULL OR tr.taxCode = :taxCode)
        AND tr.isActive = true
        ORDER BY tr.taxCode ASC
    """
    )
    fun findByTaxCode(
        @Param("taxCode") taxCode: String?,
        pageable: Pageable
    ): Page<TaxRule>

    fun findByTaxCodeId(taxCodeId: String): List<TaxRule>

}
