package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.repository.CustomerRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Default [CustomerContactProvider] backed by the customer repository (same module — allowed). */
@Service
class CustomerContactProviderImpl(
    private val customerRepository: CustomerRepository,
) : CustomerContactProvider {

    @Transactional(readOnly = true)
    override fun byUid(uid: String): CustomerContact? =
        customerRepository.findByUid(uid)?.takeIf { !it.status.equals("DELETED", ignoreCase = true) }?.toContact()

    @Transactional(readOnly = true)
    override fun byGroup(customerGroup: String): List<CustomerContact> =
        customerRepository.findByCustomerGroupAndStatusNot(customerGroup, "DELETED").map { it.toContact() }

    private fun Customer.toContact() = CustomerContact(
        uid = uid,
        name = name,
        email = email.takeIf { it.isNotBlank() },
        phone = phone.takeIf { it.isNotBlank() },
        locale = locale,
    )
}
