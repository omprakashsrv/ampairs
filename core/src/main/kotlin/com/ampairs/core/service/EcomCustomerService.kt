package com.ampairs.core.service

/**
 * Cross-module bridge: resolves the workspace CRM `Customer` (distributor account) a storefront buyer
 * is allowed to order for.
 *
 * A storefront buyer must be **linked** to a distributor — either an explicit `CustomerContact`, or by
 * confirming a phone-match candidate via [findLinkCandidateByPhone] + [confirmLink]. Buyers are never
 * auto-linked without their own confirmation: an unlinked buyer cannot order and is told to link (or,
 * if no phone match exists, to contact the owner).
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
     *  3. else null (not linked — the caller should offer [findLinkCandidateByPhone]).
     */
    fun resolveLinkedCustomerId(
        ecomUserId: String,
        requestedCustomerId: String? = null,
    ): String?

    /**
     * The CRM accounts a storefront login can order for (its [CustomerContact] links), for the
     * checkout "ordering for" picker. Empty when the login is not linked to any account. Requires
     * tenant context.
     */
    fun listAccountsForUser(ecomUserId: String): List<EcomCustomerAccount>

    /**
     * Read-only lookup: is there a CRM `Customer` in this workspace whose phone matches [phone]?
     * Never creates anything — used to offer the buyer a "link your account?" confirmation instead of
     * silently linking them. Returns null when there's no match (the caller should fall back to
     * "contact the business owner").
     */
    fun findLinkCandidateByPhone(phone: String): EcomLinkCandidate?

    /**
     * Confirms the buyer's link to [customerId], after they've approved a candidate from
     * [findLinkCandidateByPhone]. Re-validates server-side that [customerId]'s phone still equals
     * [phone] — never trusts the client's claim. Idempotent: if the login is already linked to this
     * account, returns the existing link rather than erroring or duplicating.
     *
     * Throws when [customerId] doesn't exist or its phone no longer matches [phone].
     */
    fun confirmLink(
        ecomUserId: String,
        customerId: String,
        name: String?,
        phone: String?,
        email: String?,
    ): EcomCustomerAccount

    /**
     * Every buyer↔CRM-customer link across the current workspace, across ALL customers — feeds the
     * owner's "ecom users" management screen (independent of any single customer's detail page).
     * Requires tenant context (the caller's workspace).
     */
    fun listAllContacts(): List<EcomContactSummary>

    /**
     * Restricts ([active] = false) or re-enables ([active] = true) a contact's access. A restricted
     * contact is treated as unlinked by [resolveLinkedCustomerId]/[confirmLink] — the buyer is told
     * to contact the owner again, exactly as if never linked.
     */
    fun setContactActive(contactUid: String, active: Boolean): EcomContactSummary
}

/** A CRM account a storefront buyer may order on behalf of. */
data class EcomCustomerAccount(
    val customerId: String,
    val name: String,
    val isDefault: Boolean,
    val role: String,
)

/**
 * A CRM account found to match the buyer's phone, offered for confirmation before linking. Carries
 * enough of the CRM record for the buyer to recognize/verify it's really their account (shown in a
 * confirmation sheet client-side) — [phone]/[gstNumber]/[address] mirror the workspace owner's own
 * CRM data entry, not the buyer's.
 */
data class EcomLinkCandidate(
    val customerId: String,
    val name: String,
    val phone: String,
    val gstNumber: String?,
    val address: String?,
)

/** A buyer↔CRM-customer link, for the owner's management views (per-customer or workspace-wide). */
data class EcomContactSummary(
    val contactUid: String,
    val customerId: String,
    val customerName: String,
    val name: String,
    val phone: String?,
    val role: String,
    val isDefault: Boolean,
    val active: Boolean,
)
