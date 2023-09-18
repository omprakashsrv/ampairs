package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.State
import com.ampairs.customer.domain.model.asDatabaseModel
import com.ampairs.customer.repository.CustomerPagingRepository
import com.ampairs.customer.repository.CustomerRepository
import com.ampairs.customer.repository.StateRepository
import com.ampairs.tally.model.TallyMessage
import com.ampairs.tally.model.TallyXML
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomerService @Autowired constructor(
    val customerRepository: CustomerRepository,
    val customerPagingRepository: CustomerPagingRepository,
    val stateRepository: StateRepository,
) {

    @Transactional
    fun updateCustomer(customer: Customer): Customer {
        return customerRepository.save(customer)
    }

    @Transactional
    fun updateCustomers(customers: List<Customer>): List<Customer> {
        customers.forEach { customer ->
            val existingCustomer = customerRepository.findByRefId(customer.refId)
            customer.seqId = existingCustomer?.seqId
            customer.id = existingCustomer?.id ?: ""
            customerRepository.save(customer)
        }
        return customers
    }

    fun getCustomers(lastUpdate: Long?): List<Customer> {
        val customers =
            customerPagingRepository.findAllByLastUpdatedGreaterThanEqual(
                lastUpdate ?: 0, PageRequest.of(0, 1000, Sort.by("lastUpdated").ascending())
            )
        return customers
    }

    fun updateMasters(tallyMessage: TallyMessage?) {
        if (tallyMessage?.ledger?.isBillWiseOn?.equals("Yes") == true) {
            tallyMessage.ledger?.asDatabaseModel()?.let {
                val existingCustomer = customerRepository.findByRefId(it.refId)
                it.seqId = existingCustomer?.seqId
                it.id = existingCustomer?.id ?: ""
                customerRepository.save(it)
            }
        }
    }

    @Transactional
    fun updateTallyXml(tallyXML: TallyXML?) {
        for (tallyMessage in tallyXML?.body?.importData?.requestData?.tallyMessage.orEmpty()) {
            updateMasters(tallyMessage)
        }
    }

    fun getStates(): List<State> {
        return stateRepository.findAll().toMutableList()
    }
}
