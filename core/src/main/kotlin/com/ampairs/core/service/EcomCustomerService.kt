package com.ampairs.core.service

import com.ampairs.core.domain.model.Address

/**
 * Cross-module bridge: turns an ecom storefront buyer into a workspace CRM `Customer`.
 *
 * The ecom checkout only knows the buyer as an auth `User` (uid + name/phone/email snapshots) — there
 * is no link to the merchant's CRM. This interface (implemented in the `customer` module) find-or-creates
 * a real Customer so ecom orders and their invoices reference an actual CRM record.
 *
 * Follows the same pattern as [OrderEcomService] / [EcomStorefrontLookupService]: the interface lives in
 * `core`, the implementation in the owning module, so callers depend only on `core`.
 *
 * Requires an active tenant context (the caller — e.g. the ecom-order ingestion listener — sets it); the
 * Customer is created/looked up within the current workspace.
 */
interface EcomCustomerService {

    /**
     * Find-or-create the workspace CRM Customer for a storefront buyer and return its uid.
     *
     * Resolution order:
     *  1. an existing Customer already linked to [ecomUserId];
     *  2. else an existing Customer with the same [phone] in this workspace — adopted and back-filled
     *     with [ecomUserId] so future orders resolve directly;
     *  3. else a new Customer.
     */
    fun linkOrCreateEcomCustomer(
        ecomUserId: String,
        name: String,
        phone: String?,
        email: String?,
        billingAddress: Address?,
        shippingAddress: Address?,
    ): String
}
