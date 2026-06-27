package com.ampairs.customer.domain.service

/**
 * A customer's contact details for messaging. Public, read-only projection exposed so other modules
 * (e.g. communication) can resolve audiences without touching customer repositories directly
 * (module-boundary rule: cross-module access via public service interfaces only).
 */
data class CustomerContact(
    val uid: String,
    val name: String,
    val email: String?,
    val phone: String?,
    val locale: String?,
)

/** Public contact lookup for the customer module. */
interface CustomerContactProvider {
    /** Contact for one customer uid, or null if not found. */
    fun byUid(uid: String): CustomerContact?

    /** Active contacts in a customer group (segment audience). */
    fun byGroup(customerGroup: String): List<CustomerContact>
}
