package com.ampairs.customer.data.api

import com.ampairs.customer.domain.Customer

interface CustomerApi {
    suspend fun getCustomers(workspaceId: String, lastSync: Long = 0): List<Customer>
    suspend fun createCustomer(workspaceId: String, customer: Customer): Customer
    suspend fun updateCustomer(workspaceId: String, customer: Customer): Customer
    suspend fun deleteCustomer(workspaceId: String, customerId: String)
    suspend fun getCustomer(workspaceId: String, customerId: String): Customer?
}