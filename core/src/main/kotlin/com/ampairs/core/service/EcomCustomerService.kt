package com.ampairs.core.service

/**
 * Cross-module bridge: resolves the workspace CRM `Customer` (distributor account) a storefront buyer
 * is allowed to order for.
 *
 * A storefront buyer must be **pre-linked** to a distributor by the workspace owner — either an
 * explicit `CustomerContact`, or the owner having created a CRM customer with the buyer's phone.
 * Buyers are **never auto-created**: an unlinked buyer cannot order and is told to contact the owner.
 *
 * Follows the same pattern as [OrderEcomService] / [EcomStorefrontLookupService]: the interface lives in
 * `core`, the implementation in the owning module, so callers depend only on `core`. Requires an active
 * tenant context; all lookups/links are within the current workspace.
 */
interface EcomCustomerService {

    /**
     * Resolve the CRM distributor account this storefront buyer may order for, WITHOUT creating one.
     * Returns null when the login is not linked to any account (→ the order must be blocked).
     *
     * Resolution order:
     *  1. [requestedCustomerId] — only if the login is actually linked to it;
     *  2. the login's default (else first) linked account;
     *  3. a CRM customer the owner already created with the buyer's [phone] — auto-linked as a contact;
     *  4. else null (not linked).
     */
    fun resolveLinkedCustomerId(
        ecomUserId: String,
        phone: String?,
        name: String?,
        email: String?,
        requestedCustomerId: String? = null,
    ): String?

    /**
     * The CRM accounts a storefront login can order for (its [CustomerContact] links), for the
     * checkout "ordering for" picker. Empty when the login is not linked to any account. Requires
     * tenant context.
     */
    fun listAccountsForUser(ecomUserId: String): List<EcomCustomerAccount>
}

/** A CRM account a storefront buyer may order on behalf of. */
data class EcomCustomerAccount(
    val customerId: String,
    val name: String,
    val isDefault: Boolean,
    val role: String,
)
