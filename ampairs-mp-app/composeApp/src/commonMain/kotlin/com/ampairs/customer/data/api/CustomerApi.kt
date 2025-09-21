package com.ampairs.customer.data.api

import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.State

interface CustomerApi {
    suspend fun getCustomers(lastSync: Long = 0): List<Customer>
    suspend fun createCustomer(customer: Customer): Customer
    suspend fun updateCustomer(customer: Customer): Customer
    suspend fun deleteCustomer(customerId: String)
    suspend fun getCustomer(customerId: String): Customer?
    suspend fun getStates(lastSync: Long = 0): List<State>
}