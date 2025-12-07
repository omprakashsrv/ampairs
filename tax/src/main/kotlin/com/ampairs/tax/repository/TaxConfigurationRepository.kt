package com.ampairs.tax.repository

import com.ampairs.tax.domain.model.TaxConfiguration
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface TaxConfigurationRepository : JpaRepository<TaxConfiguration, Long> {

    @Query("SELECT t FROM TaxConfiguration t WHERE t.uid = :uid")
    fun findByUid(uid: String): TaxConfiguration?

    @Query("SELECT t FROM TaxConfiguration t WHERE t.ownerId = :ownerId")
    fun findByOwnerId(ownerId: String): TaxConfiguration?

    @Query("SELECT t FROM TaxConfiguration t WHERE t.countryCode = :countryCode")
    fun findByCountryCode(countryCode: String, pageable: Pageable): Page<TaxConfiguration>

    @Query("SELECT t FROM TaxConfiguration t WHERE t.taxStrategy = :taxStrategy")
    fun findByTaxStrategy(taxStrategy: String, pageable: Pageable): Page<TaxConfiguration>

    @Query("SELECT t FROM TaxConfiguration t WHERE t.updatedAt > :modifiedAfter ORDER BY t.updatedAt ASC")
    fun findByUpdatedAtAfter(modifiedAfter: Instant, pageable: Pageable): Page<TaxConfiguration>

    @Query("SELECT t FROM TaxConfiguration t ORDER BY t.createdAt DESC")
    fun findAllOrderByCreatedAtDesc(pageable: Pageable): Page<TaxConfiguration>
}
