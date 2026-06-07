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

    fun findByUid(uid: String): TaxConfiguration?

    fun findByOwnerId(ownerId: String): TaxConfiguration?

    fun findByCountryCode(countryCode: String, pageable: Pageable): Page<TaxConfiguration>

    fun findByTaxStrategy(taxStrategy: String, pageable: Pageable): Page<TaxConfiguration>

    fun findByUpdatedAtAfter(modifiedAfter: Instant, pageable: Pageable): Page<TaxConfiguration>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(t.updatedAt) FROM TaxConfiguration t")
    fun findMaxUpdatedAt(): Instant?
}
