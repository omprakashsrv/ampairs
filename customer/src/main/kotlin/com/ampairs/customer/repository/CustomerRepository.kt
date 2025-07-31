package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.Customer
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CustomerRepository : CrudRepository<Customer, Long> {
    fun findByRefId(refId: String?): Customer?

    fun findBySeqId(seqId: String): Customer?

}