package com.ampairs.customer.domain.service

import com.ampairs.core.domain.model.Address
import com.ampairs.core.service.EcomCustomerService
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.repository.CustomerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * `customer`-module implementation of the [EcomCustomerService] bridge. Find-or-creates a workspace
 * CRM Customer for a storefront buyer and returns its uid. See the interface for the resolution order.
 *
 * Runs inside the caller's tenant context (set by the ecom-order ingestion listener), so the
 * @TenantId-filtered lookups and the persisted Customer are all scoped to the buyer's workspace.
 */
@Service
class EcomCustomerServiceImpl(
    private val customerRepository: CustomerRepository,
    private val customerService: CustomerService,
) : EcomCustomerService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun linkOrCreateEcomCustomer(
        ecomUserId: String,
        name: String,
        phone: String?,
        email: String?,
        billingAddress: Address?,
        shippingAddress: Address?,
    ): String {
        // 1. Already linked to this storefront buyer.
        customerRepository.findFirstByEcomUserId(ecomUserId)?.let { return it.uid }

        // 2. Same phone already in the CRM — adopt it and back-fill the ecom link so future
        //    orders resolve directly (and the merchant doesn't get a duplicate contact).
        if (!phone.isNullOrBlank()) {
            customerRepository.findFirstByPhone(phone)?.let { existing ->
                if (existing.ecomUserId.isNullOrBlank()) {
                    existing.ecomUserId = ecomUserId
                    customerService.updateCustomer(existing)
                }
                return existing.uid
            }
        }

        // 3. New CRM customer for this buyer. (No `apply {}` — the function params share names with
        //    Customer's own properties, which would shadow them inside an apply receiver.)
        val customer = Customer()
        customer.name = name.ifBlank { phone ?: email ?: "Online customer" }
        customer.phone = phone.orEmpty()
        customer.email = email.orEmpty()
        customer.ecomUserId = ecomUserId
        // customerType / customerGroup are required; the merchant can recategorise later.
        customer.customerType = ONLINE_CATEGORY
        customer.customerGroup = ONLINE_CATEGORY
        billingAddress?.let { addr ->
            customer.billingAddress = addr
            // Mirror the delivery address into the flat CRM columns for list/search display.
            addr.street?.let { customer.street = it }
            addr.street2?.let { customer.street2 = it }
            addr.address?.let { customer.address = it }
            addr.city?.let { customer.city = it }
            addr.state?.let { customer.state = it }
            addr.pincode?.let { customer.pincode = it }
            addr.country?.let { customer.country = it }
        }
        shippingAddress?.let { customer.shippingAddress = it }

        val saved = customerService.createCustomer(customer)
        log.info("Created CRM customer {} for ecom buyer {}", saved.uid, ecomUserId)
        return saved.uid
    }

    private companion object {
        /** Default type/group for storefront-originated customers (string codes; no FK). */
        const val ONLINE_CATEGORY = "ONLINE"
    }
}
