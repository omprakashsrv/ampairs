package com.ampairs.tax.repository

import com.ampairs.tax.domain.model.TaxComponent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface TaxComponentRepository : JpaRepository<TaxComponent, Long> {

    @Query("SELECT t FROM TaxComponent t WHERE t.uid = :uid")
    fun findByUid(uid: String): TaxComponent?

    @Query("SELECT t FROM TaxComponent t WHERE t.componentTypeId = :componentTypeId")
    fun findByComponentTypeId(componentTypeId: String): TaxComponent?

    @Query("SELECT t FROM TaxComponent t WHERE t.isActive = true ORDER BY t.componentName ASC")
    fun findAllActive(pageable: Pageable): Page<TaxComponent>

    @Query("SELECT t FROM TaxComponent t WHERE t.updatedAt > :modifiedAfter ORDER BY t.updatedAt ASC")
    fun findByUpdatedAtAfter(modifiedAfter: Instant, pageable: Pageable): Page<TaxComponent>

    @Query("SELECT t FROM TaxComponent t WHERE t.taxType = :taxType AND t.isActive = true ORDER BY t.componentName ASC")
    fun findByTaxType(taxType: String, pageable: Pageable): Page<TaxComponent>

    @Query("SELECT t FROM TaxComponent t WHERE t.jurisdiction = :jurisdiction AND t.isActive = true ORDER BY t.componentName ASC")
    fun findByJurisdiction(jurisdiction: String, pageable: Pageable): Page<TaxComponent>
}
