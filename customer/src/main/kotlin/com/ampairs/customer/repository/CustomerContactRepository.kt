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

    /**
     * An [status]-matching contact already linking this login to this account, if any
     * (@TenantId-filtered). Status-scoped so a restricted (INACTIVE) contact is never treated as a
     * valid link by checkout / confirmLink's idempotency check.
     */
    fun findFirstByCustomerIdAndEcomUserIdAndStatus(customerId: String, ecomUserId: String, status: String): CustomerContact?

    /**
     * Every contact linked to this CRM customer, active or restricted (@TenantId-filtered) — the
     * customer detail screen's "linked accounts" list; restricted ones must still show up so the
     * owner can re-enable them.
     */
    fun findByCustomerId(customerId: String): List<CustomerContact>

    fun findByUid(uid: String): CustomerContact?
}
