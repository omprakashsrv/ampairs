package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.CustomerContact
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository

@Repository
interface CustomerContactRepository :
    CrudRepository<CustomerContact, Long>,
    PagingAndSortingRepository<CustomerContact, Long> {

    /** All active contacts for a storefront login in the current workspace (@TenantId-filtered). */
    fun findByEcomUserIdAndStatus(ecomUserId: String, status: String): List<CustomerContact>

    /** A contact already linking this login to this account, if any (@TenantId-filtered). */
    fun findFirstByCustomerIdAndEcomUserId(customerId: String, ecomUserId: String): CustomerContact?

    fun findByUid(uid: String): CustomerContact?
}
