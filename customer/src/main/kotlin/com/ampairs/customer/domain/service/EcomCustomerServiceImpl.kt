package com.ampairs.customer.domain.service

import com.ampairs.core.service.EcomContactSummary
import com.ampairs.core.service.EcomCustomerAccount
import com.ampairs.core.service.EcomCustomerService
import com.ampairs.core.service.EcomLinkCandidate
import com.ampairs.core.service.UserService
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.CustomerContact
import com.ampairs.customer.exception.CustomerNotFoundException
import com.ampairs.customer.exception.EcomLinkInvalidException
import com.ampairs.customer.exception.InvalidCustomerDataException
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
 * Also owns the owner-facing side of the same `CustomerContact` data — [listContactsForCustomer] and
 * [linkContactByPhone] are consumed directly by `CustomerController` in this module (no core-bridge
 * indirection needed, same module), while [listAllContacts]/[setContactActive] are part of
 * [EcomCustomerService] because the `ecom` module's owner-facing "ecom users" screen needs them too.
 *
 * Runs inside the caller's tenant context, so the @TenantId-filtered lookups and any link written are
 * scoped to the buyer's workspace.
 */
@Service
class EcomCustomerServiceImpl(
    private val customerRepository: CustomerRepository,
    private val customerContactRepository: CustomerContactRepository,
    private val userService: UserService,
) : EcomCustomerService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    override fun resolveLinkedCustomerId(
        ecomUserId: String,
        requestedCustomerId: String?,
    ): String? {
        // 1. Explicit chosen account — only if the login is actually linked to it (and not restricted).
        if (!requestedCustomerId.isNullOrBlank()) {
            return customerContactRepository
                .findFirstByCustomerIdAndEcomUserIdAndStatus(requestedCustomerId, ecomUserId, ACTIVE)
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
        customerContactRepository.findFirstByCustomerIdAndEcomUserIdAndStatus(customerId, ecomUserId, ACTIVE)?.let { existing ->
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

    /** Every contact (active or restricted) linked to [customerId] — the customer detail screen's list. */
    @Transactional(readOnly = true)
    fun listContactsForCustomer(customerId: String): List<EcomContactSummary> {
        val customerName = customerRepository.findByUid(customerId)?.name ?: ""
        return customerContactRepository.findByCustomerId(customerId).map { it.toSummary(customerName) }
    }

    /**
     * Owner-initiated: link the app account registered with [phone] to [customerId], bypassing the
     * buyer's own confirmation flow (the owner is asserting the match directly). Idempotent if
     * already linked.
     */
    @Transactional
    fun linkContactByPhone(customerId: String, phone: String, name: String?, role: String, isDefault: Boolean): EcomContactSummary {
        val customer = customerRepository.findByUid(customerId)
            ?: throw CustomerNotFoundException("Customer not found: $customerId")
        val user = userService.getUserByPhone(phone)
            ?: throw InvalidCustomerDataException("No app account found with phone $phone")
        customerContactRepository.findFirstByCustomerIdAndEcomUserIdAndStatus(customerId, user.uid, ACTIVE)?.let {
            return it.toSummary(customer.name)
        }
        val contact = CustomerContact()
        contact.customerId = customer.uid
        contact.ecomUserId = user.uid
        contact.name = name?.takeIf { it.isNotBlank() } ?: user.getDisplayName()
        contact.phone = phone
        contact.email = user.email
        contact.role = role.ifBlank { "OWNER" }
        contact.isDefault = isDefault
        contact.status = ACTIVE
        val saved = customerContactRepository.save(contact)
        log.info("Owner linked customer {} to ecom buyer {}", customer.uid, user.uid)
        return saved.toSummary(customer.name)
    }

    @Transactional(readOnly = true)
    override fun listAllContacts(): List<EcomContactSummary> {
        val contacts = customerContactRepository.findAll().toList()
        if (contacts.isEmpty()) return emptyList()
        val customerNames = customerRepository.findByUidIn(contacts.map { it.customerId }.distinct())
            .associateBy({ it.uid }, { it.name })
        return contacts.map { it.toSummary(customerNames[it.customerId] ?: it.name) }
    }

    @Transactional
    override fun setContactActive(contactUid: String, active: Boolean): EcomContactSummary {
        val contact = customerContactRepository.findByUid(contactUid)
            ?: throw EcomLinkInvalidException("Contact not found: $contactUid")
        contact.status = if (active) ACTIVE else INACTIVE
        val saved = customerContactRepository.save(contact)
        val customerName = customerRepository.findByUid(saved.customerId)?.name ?: saved.name
        log.info("{} contact {} (customer {})", if (active) "Re-enabled" else "Restricted", contactUid, saved.customerId)
        return saved.toSummary(customerName)
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
        contact.status = ACTIVE
        customerContactRepository.save(contact)
    }

    private fun CustomerContact.toSummary(customerName: String) = EcomContactSummary(
        contactUid = uid,
        customerId = customerId,
        customerName = customerName,
        name = name,
        phone = phone,
        role = role,
        isDefault = isDefault,
        active = status == ACTIVE,
    )

    private companion object {
        const val ACTIVE = "ACTIVE"
        const val INACTIVE = "INACTIVE"
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
