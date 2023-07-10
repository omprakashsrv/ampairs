package com.ampairs.customer.domain.service

import com.ampairs.core.user.model.Customer
import com.ampairs.core.user.repository.CompanyRepository
import com.ampairs.customer.domain.model.asDatabaseModel
import com.ampairs.customer.repository.CustomerPagingRepository
import com.ampairs.customer.repository.CustomerRepository
import com.ampairs.tally.model.TallyMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomerService @Autowired constructor(
    val customerRepository: CustomerRepository,
    val customerPagingRepository: CustomerPagingRepository,
    private val companyRepository: CompanyRepository
) {

    @Transactional
    fun updateCustomer(ownerId: String, customer: Customer): Customer {
        return customerRepository.save(customer)
    }

    fun getCustomers(companyId: String, lastUpdate: Long?): List<Customer> {
        val customers =
            customerPagingRepository.findAllByOwnerIdAndLastUpdatedGreaterThanEqual(
                companyId, lastUpdate ?: 0, Sort.by("lastUpdated").ascending()
            )
        return customers
    }

    @Transactional
    fun updateMasters(tallyMessage: TallyMessage?) {
        tallyMessage?.ledger?.asDatabaseModel()?.let {
            customerRepository.save(it)
        }
    }
}
