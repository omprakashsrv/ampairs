package com.ampairs.customer.domain.service

import com.ampairs.core.domain.model.Address
import com.ampairs.core.service.EcomCustomerAccount
import com.ampairs.core.service.EcomCustomerService
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.CustomerContact
import com.ampairs.customer.repository.CustomerContactRepository
import com.ampairs.customer.repository.CustomerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * `customer`-module implementation of the [EcomCustomerService] bridge. Find-or-creates a workspace
 * CRM Customer for a storefront buyer and returns its uid, backed by the [CustomerContact] link so
 * many logins can share one account and one login can belong to many accounts.
 *
 * Runs inside the caller's tenant context (set by the ecom-order ingestion listener), so the
 * @TenantId-filtered lookups and persisted rows are all scoped to the buyer's workspace.
 */
@Service
class EcomCustomerServiceImpl(
    private val customerRepository: CustomerRepository,
    private val customerContactRepository: CustomerContactRepository,
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
        requestedCustomerId: String?,
    ): String {
        // 0. Buyer explicitly chose an account → attribute the order to it, linking this login if the
        //    contact doesn't exist yet (e.g. an account the merchant created and shared).
        if (!requestedCustomerId.isNullOrBlank()) {
            if (customerContactRepository.findFirstByCustomerIdAndEcomUserId(requestedCustomerId, ecomUserId) == null) {
                linkContact(requestedCustomerId, ecomUserId, name, phone, email, isDefault = false)
            }
            return requestedCustomerId
        }

        // 1. This login already maps to one or more accounts → use the default (else the first).
        val contacts = customerContactRepository.findByEcomUserIdAndStatus(ecomUserId, ACTIVE)
        if (contacts.isNotEmpty()) {
            return (contacts.firstOrNull { it.isDefault } ?: contacts.first()).customerId
        }

        // 2. A CRM customer with this phone already exists → attach this login to it as a contact
        //    (so the merchant doesn't get a duplicate account).
        if (!phone.isNullOrBlank()) {
            customerRepository.findFirstByPhone(phone)?.let { existing ->
                linkContact(existing.uid, ecomUserId, name, phone, email)
                return existing.uid
            }
        }

        // 3. New CRM account + its first contact (the creating login, default).
        val customer = createCustomer(name, phone, email, billingAddress, shippingAddress)
        linkContact(customer.uid, ecomUserId, name, phone, email)
        log.info("Created CRM customer {} for ecom buyer {}", customer.uid, ecomUserId)
        return customer.uid
    }

    @Transactional(readOnly = true)
    override fun listAccountsForUser(ecomUserId: String): List<EcomCustomerAccount> {
        return customerContactRepository.findByEcomUserIdAndStatus(ecomUserId, ACTIVE).map { contact ->
            // Label the picker with the CRM account name; fall back to the contact's own name.
            val accountName = customerRepository.findByUid(contact.customerId)?.name ?: contact.name
            EcomCustomerAccount(
                customerId = contact.customerId,
                name = accountName,
                isDefault = contact.isDefault,
                role = contact.role,
            )
        }
    }

    private fun linkContact(
        customerId: String,
        ecomUserId: String,
        name: String,
        phone: String?,
        email: String?,
        isDefault: Boolean = true,
    ) {
        val contact = CustomerContact()
        contact.customerId = customerId
        contact.ecomUserId = ecomUserId
        contact.name = name.ifBlank { phone ?: email ?: "Online customer" }
        contact.phone = phone
        contact.email = email
        contact.role = "OWNER"
        contact.isDefault = isDefault
        customerContactRepository.save(contact)
    }

    private fun createCustomer(name: String, phone: String?, email: String?, billing: Address?, shipping: Address?): Customer {
        val customer = Customer()
        customer.name = name.ifBlank { phone ?: email ?: "Online customer" }
        customer.phone = phone.orEmpty()
        customer.email = email.orEmpty()
        // customerType / customerGroup are required; the merchant can recategorise later.
        customer.customerType = ONLINE_CATEGORY
        customer.customerGroup = ONLINE_CATEGORY
        billing?.let { addr ->
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
        shipping?.let { customer.shippingAddress = it }
        return customerService.createCustomer(customer)
    }

    private companion object {
        const val ACTIVE = "ACTIVE"
        /** Default type/group for storefront-originated customers (string codes; no FK). */
        const val ONLINE_CATEGORY = "ONLINE"
    }
}
