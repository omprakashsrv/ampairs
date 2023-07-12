package com.ampairs.customer.domain.service

import com.ampairs.core.user.repository.CompanyRepository
import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.asDatabaseModel
import com.ampairs.customer.repository.CustomerPagingRepository
import com.ampairs.customer.repository.CustomerRepository
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
    private val companyRepository: CompanyRepository
) {

    @Transactional
    fun updateCustomer(ownerId: String, customer: Customer): Customer {
        return customerRepository.save(customer)
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
}
