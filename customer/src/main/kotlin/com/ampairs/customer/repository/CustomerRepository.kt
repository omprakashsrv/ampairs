package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.Customer
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface CustomerRepository : CrudRepository<Customer, Long>, PagingAndSortingRepository<Customer, Long> {
    @EntityGraph("Customer.withImages")
    fun findByRefId(refId: String?): Customer?

    @EntityGraph("Customer.withImages")
    fun findByUid(uid: String): Customer?

    @EntityGraph("Customer.withImages")
    @Query("SELECT c FROM customer c WHERE c.updatedAt >= :lastSync ORDER BY c.updatedAt ASC")
    fun findCustomersUpdatedAfter(lastSync: Instant, pageable: Pageable): Page<Customer>

    /** Sync checkpoint: max updatedAt for the current workspace (null when empty). @TenantId-filtered. */
    @Query("SELECT MAX(c.updatedAt) FROM customer c")
    fun findMaxUpdatedAt(): Instant?

    /** Active members of a customer group (excludes soft-deleted). Used for audience/segment resolution. */
    fun findByCustomerGroupAndStatusNot(customerGroup: String, status: String): List<Customer>
}
