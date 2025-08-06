package com.ampairs.customer.domain.service

import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.State
import com.ampairs.customer.repository.CustomerPagingRepository
import com.ampairs.customer.repository.CustomerRepository
import com.ampairs.customer.repository.StateRepository
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
            if (customer.uid.isNotEmpty()) {
                val existingCustomer = customerRepository.findByUid(customer.uid)
                customer.refId = existingCustomer?.refId ?: ""
                customer.createdAt = existingCustomer?.createdAt ?: ""
                customer.updatedAt = existingCustomer?.updatedAt ?: ""
            } else if (customer.refId?.isNotEmpty() == true) {
                val existingCustomer = customerRepository.findByRefId(customer.refId)
                customer.uid = existingCustomer?.uid ?: ""
                customer.createdAt = existingCustomer?.createdAt ?: ""
                customer.updatedAt = existingCustomer?.updatedAt ?: ""
            }
            customer.lastUpdated = System.currentTimeMillis()
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

    fun getStates(): List<State> {
        return stateRepository.findAll().toMutableList()
    }
}
