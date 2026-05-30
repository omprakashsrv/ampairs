package com.ampairs.ecom.repository

import com.ampairs.ecom.domain.model.CustomerAddress
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CustomerAddressRepository : CrudRepository<CustomerAddress, Long> {
    fun findByCustomerId(customerId: String): List<CustomerAddress>
    fun findByCustomerIdAndUid(customerId: String, uid: String): CustomerAddress?
    fun findByCustomerIdAndIsDefaultTrue(customerId: String): CustomerAddress?
    fun countByCustomerId(customerId: String): Long
}
