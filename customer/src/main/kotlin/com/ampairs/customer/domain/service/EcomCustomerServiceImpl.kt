package com.ampairs.customer.domain.service

import com.ampairs.core.service.EcomCustomerAccount
import com.ampairs.core.service.EcomCustomerService
import com.ampairs.customer.domain.model.CustomerContact
import com.ampairs.customer.repository.CustomerContactRepository
import com.ampairs.customer.repository.CustomerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * `customer`-module implementation of [EcomCustomerService]. Resolves the CRM distributor account a
 * storefront buyer is allowed to order for — never creating one. A buyer must be pre-linked by the
 * workspace owner (an explicit contact, or a CRM customer created with the buyer's phone); otherwise
 * this returns null and checkout blocks the order.
 *
 * Runs inside the caller's tenant context, so the @TenantId-filtered lookups and any link written are
 * scoped to the buyer's workspace.
 */
@Service
class EcomCustomerServiceImpl(
    private val customerRepository: CustomerRepository,
    private val customerContactRepository: CustomerContactRepository,
) : EcomCustomerService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun resolveLinkedCustomerId(
        ecomUserId: String,
        phone: String?,
        name: String?,
        email: String?,
        requestedCustomerId: String?,
    ): String? {
        // 1. Explicit chosen account — only if the login is actually linked to it.
        if (!requestedCustomerId.isNullOrBlank()) {
            return customerContactRepository
                .findFirstByCustomerIdAndEcomUserId(requestedCustomerId, ecomUserId)
                ?.customerId
        }
        // 2. The login's default (else first) linked account.
        val contacts = customerContactRepository.findByEcomUserIdAndStatus(ecomUserId, ACTIVE)
        if (contacts.isNotEmpty()) {
            return (contacts.firstOrNull { it.isDefault } ?: contacts.first()).customerId
        }
        // 3. Owner already created a CRM customer with this phone → auto-link and use it.
        if (!phone.isNullOrBlank()) {
            customerRepository.findFirstByPhone(phone)?.let { existing ->
                linkContact(existing.uid, ecomUserId, name, phone, email)
                log.info("Linked ecom buyer {} to existing customer {} by phone", ecomUserId, existing.uid)
                return existing.uid
            }
        }
        // 4. Not linked to any distributor — the caller must block the order.
        return null
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

    private fun linkContact(customerId: String, ecomUserId: String, name: String?, phone: String?, email: String?) {
        val contact = CustomerContact()
        contact.customerId = customerId
        contact.ecomUserId = ecomUserId
        contact.name = name?.takeIf { it.isNotBlank() } ?: phone ?: email ?: "Online customer"
        contact.phone = phone
        contact.email = email
        contact.role = "OWNER"
        contact.isDefault = true
        customerContactRepository.save(contact)
    }

    private companion object {
        const val ACTIVE = "ACTIVE"
    }
}
