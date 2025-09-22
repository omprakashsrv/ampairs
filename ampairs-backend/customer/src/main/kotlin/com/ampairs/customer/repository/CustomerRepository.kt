package com.ampairs.customer.repository

import com.ampairs.customer.domain.model.Customer
import com.ampairs.customer.domain.model.CustomerType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CustomerRepository : CrudRepository<Customer, Long>, PagingAndSortingRepository<Customer, Long> {
    fun findByRefId(refId: String?): Customer?
    fun findByUid(uid: String): Customer?
    fun findByGstNumber(gstNumber: String): Optional<Customer>
    fun findByPhone(phone: String): Optional<Customer>
    fun findByEmail(email: String): Optional<Customer>
    fun findByCustomerType(customerType: CustomerType): List<Customer>
    fun findByStatus(status: String): List<Customer>

    @Query("SELECT c FROM customer c WHERE c.name ILIKE %:searchTerm% OR c.phone ILIKE %:searchTerm% OR c.email ILIKE %:searchTerm%")
    fun searchCustomers(searchTerm: String, pageable: Pageable): Page<Customer>

    @Query("SELECT c FROM customer c WHERE c.customerType = :customerType AND c.status = 'ACTIVE'")
    fun findActiveCustomersByType(customerType: CustomerType, pageable: Pageable): Page<Customer>

    @Query("SELECT c FROM customer c WHERE c.creditLimit > 0 AND c.status = 'ACTIVE'")
    fun findCustomersWithCredit(pageable: Pageable): Page<Customer>

    @Query("SELECT c FROM customer c WHERE c.outstandingAmount > 0 AND c.status = 'ACTIVE'")
    fun findCustomersWithOutstanding(pageable: Pageable): Page<Customer>

    @Query("SELECT c FROM customer c WHERE c.city = :city AND c.status = 'ACTIVE'")
    fun findActiveCustomersByCity(city: String, pageable: Pageable): Page<Customer>

    @Query("SELECT c FROM customer c WHERE c.state = :state AND c.status = 'ACTIVE'")
    fun findActiveCustomersByState(state: String, pageable: Pageable): Page<Customer>
}