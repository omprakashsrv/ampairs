package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.Customer
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CustomerRepository : CrudRepository<Customer, String> {
    fun findByRefId(refId: String?): Customer?

    @Query("SELECT cu FROM customer cu WHERE cu.id = :id")
    override fun findById(id: String): Optional<Customer>

}