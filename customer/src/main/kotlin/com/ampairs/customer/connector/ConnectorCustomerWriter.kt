package com.ampairs.customer.connector

import com.ampairs.core.connector.AbstractRefIdEntityWriter
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.repository.CustomerRepository
import org.springframework.stereotype.Component

/**
 * Applies connector-mapped columns onto Customer entities (spec 013, Principle IX). Matched by `refId`
 * (e.g. Tally GUID) or `uid`; otherwise a new customer is created. Only present, allowlisted columns
 * are written.
 */
@Component
class ConnectorCustomerWriter(
    private val repository: CustomerRepository,
) : AbstractRefIdEntityWriter<Customer>() {
    override val entityType: String = "customer"
    override fun findByRefId(refId: String): Customer? = repository.findByRefId(refId)
    override fun findByUid(uid: String): Customer? = repository.findByUid(uid)
    override fun newInstance(): Customer = Customer()
    override fun persist(entity: Customer): Customer = repository.save(entity)
}
