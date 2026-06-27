package com.ampairs.supplier.repository

import com.ampairs.supplier.domain.model.Supplier
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SupplierRepository : CrudRepository<Supplier, Long>, PagingAndSortingRepository<Supplier, Long> {
    fun findByRefId(refId: String?): Supplier?

    fun findByUid(uid: String): Supplier?

    @Query("SELECT s FROM supplier s WHERE s.updatedAt >= :lastSync ORDER BY s.updatedAt ASC")
    fun findSuppliersUpdatedAfter(lastSync: Instant, pageable: Pageable): Page<Supplier>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(s.updatedAt) FROM supplier s")
    fun findMaxUpdatedAt(): Instant?
}
