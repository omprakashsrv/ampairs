package com.ampairs.customer.domain.service

import com.ampairs.core.domain.model.Company
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.repository.CompanyRepository
import com.ampairs.customer.repository.CustomerPagingRepository
import com.ampairs.customer.repository.CustomerRepository
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
    fun updateCustomer(ownerId: String, company: Company): Company {
        val company = companyRepository.save(company)
        val customer = Customer()
        customer.companyId = company.id
        customer.ownerId = ownerId
        customerRepository.save(customer)
        return company
    }

    fun getCustomers(companyId: String, lastUpdate: Long?): List<Company> {
        val customers =
            customerPagingRepository.findAllByOwnerIdAndLastUpdatedGreaterThanEqual(
                companyId, lastUpdate ?: 0, Sort.by("lastUpdated").ascending()
            )
        val allCustomer: MutableList<Company> = mutableListOf()
        for (customer in customers) {
            allCustomer.add(customer.company)
        }
        return allCustomer
    }
}
