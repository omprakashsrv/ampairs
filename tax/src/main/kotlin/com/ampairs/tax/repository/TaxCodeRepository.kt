package com.ampairs.tax.repository

import com.ampairs.tax.domain.model.TaxCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface TaxCodeRepository : JpaRepository<TaxCode, Long> {

    @Query("SELECT t FROM TaxCode t WHERE t.uid = :uid")
    fun findByUid(uid: String): TaxCode?

    @Query("SELECT t FROM TaxCode t WHERE t.masterTaxCodeId = :masterTaxCodeId")
    fun findByMasterTaxCodeId(masterTaxCodeId: String): TaxCode?

    @Query("SELECT t FROM TaxCode t WHERE t.isActive = true ORDER BY t.updatedAt ASC")
    fun findAllActive(pageable: Pageable): Page<TaxCode>

    @Query("SELECT t FROM TaxCode t WHERE t.updatedAt > :modifiedAfter ORDER BY t.updatedAt ASC")
    fun findByUpdatedAtAfter(modifiedAfter: Instant, pageable: Pageable): Page<TaxCode>

    @Query("SELECT t FROM TaxCode t WHERE t.isFavorite = true AND t.isActive = true ORDER BY t.usageCount DESC, t.code ASC")
    fun findFavorites(pageable: Pageable): Page<TaxCode>

    @Query("SELECT t FROM TaxCode t WHERE t.isActive = true ORDER BY t.usageCount DESC, t.lastUsedAt DESC")
    fun findMostUsed(pageable: Pageable): Page<TaxCode>

    @Query("SELECT t FROM TaxCode t WHERE t.code LIKE CONCAT(:codePrefix, '%') AND t.isActive = true ORDER BY t.code ASC")
    fun findByCodeStartingWith(codePrefix: String, pageable: Pageable): Page<TaxCode>
}
