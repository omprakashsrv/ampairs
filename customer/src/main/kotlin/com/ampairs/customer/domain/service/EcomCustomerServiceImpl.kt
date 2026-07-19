package com.ampairs.customer.domain.service

import com.ampairs.core.service.EcomCustomerAccount
import com.ampairs.core.service.EcomCustomerService
import com.ampairs.core.service.EcomLinkCandidate
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.CustomerContact
import com.ampairs.customer.exception.EcomLinkInvalidException
import com.ampairs.customer.repository.CustomerContactRepository
import com.ampairs.customer.repository.CustomerRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * `customer`-module implementation of [EcomCustomerService]. Resolves the CRM distributor account a
 * storefront buyer is allowed to order for — never creating one on the caller's behalf. A buyer must
 * be pre-linked by the workspace owner (an explicit contact), or link themselves by confirming a
 * phone-match candidate via [findLinkCandidateByPhone] + [confirmLink]; otherwise this returns null
 * and checkout blocks the order.
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

    @Transactional(readOnly = true)
    override fun resolveLinkedCustomerId(
        ecomUserId: String,
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
        // 3. Not linked to any distributor — the caller must block the order and offer
        // findLinkCandidateByPhone() instead of silently linking here.
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

    @Transactional(readOnly = true)
    override fun findLinkCandidateByPhone(phone: String): EcomLinkCandidate? {
        if (phone.isBlank()) return null
        return customerRepository.findFirstByPhone(phone)?.let {
            EcomLinkCandidate(
                customerId = it.uid,
                name = it.name,
                phone = it.phone,
                gstNumber = it.gstNumber,
                address = it.formattedAddress(),
            )
        }
    }

    @Transactional
    override fun confirmLink(
        ecomUserId: String,
        customerId: String,
        name: String?,
        phone: String?,
        email: String?,
    ): EcomCustomerAccount {
        // Idempotent: already linked — return the existing link rather than erroring or duplicating.
        customerContactRepository.findFirstByCustomerIdAndEcomUserId(customerId, ecomUserId)?.let { existing ->
            val accountName = customerRepository.findByUid(customerId)?.name ?: existing.name
            return EcomCustomerAccount(customerId, accountName, existing.isDefault, existing.role)
        }
        // Re-validate server-side — never trust the client's claim that this customerId is a phone
        // match; the candidate could be stale (the CRM record's phone may have since changed).
        val customer = customerRepository.findByUid(customerId)
            ?: throw EcomLinkInvalidException("Distributor account not found")
        if (phone.isNullOrBlank() || customer.phone != phone) {
            throw EcomLinkInvalidException("This account is not linked to your phone number")
        }
        linkContact(customer.uid, ecomUserId, name, phone, email)
        log.info("Confirmed link: ecom buyer {} -> customer {}", ecomUserId, customer.uid)
        return EcomCustomerAccount(customerId = customer.uid, name = customer.name, isDefault = true, role = "OWNER")
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

/** Single-line address for display (candidate confirmation sheet), or null when nothing is on file. */
private fun Customer.formattedAddress(): String? {
    val primaryLine = street.ifBlank { address }
    return listOfNotNull(
        primaryLine.takeIf { it.isNotBlank() },
        street2.takeIf { it.isNotBlank() },
        city.takeIf { it.isNotBlank() },
        state.takeIf { it.isNotBlank() },
        pincode.takeIf { it.isNotBlank() },
    ).joinToString(", ").takeIf { it.isNotBlank() }
}
