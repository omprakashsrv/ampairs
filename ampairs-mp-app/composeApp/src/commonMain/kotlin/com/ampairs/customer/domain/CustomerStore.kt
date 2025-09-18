package com.ampairs.customer.domain

import com.ampairs.customer.data.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

data class CustomerKey(val workspaceId: String, val customerId: String? = null)
data class CustomerListKey(val workspaceId: String, val searchQuery: String = "")

class CustomerStore(private val repository: CustomerRepository) {

    val customerListStore: Store<CustomerListKey, List<CustomerListItem>> = StoreBuilder
        .from(
            fetcher = Fetcher.of { key: CustomerListKey ->
                // Force sync from server
                repository.syncCustomers(key.workspaceId)
                emptyList<CustomerListItem>() // Store5 will use SourceOfTruth for actual data
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: CustomerListKey ->
                    if (key.searchQuery.isBlank()) {
                        repository.observeCustomers(key.workspaceId)
                    } else {
                        repository.searchCustomers(key.workspaceId, key.searchQuery)
                    }
                },
                writer = { _: CustomerListKey, _: List<CustomerListItem> ->
                    // Writing is handled through repository methods
                }
            )
        ).build()

    val customerStore: Store<CustomerKey, Customer> = StoreBuilder
        .from(
            fetcher = Fetcher.of { key: CustomerKey ->
                key.customerId?.let { customerId ->
                    repository.getCustomer(key.workspaceId, customerId)
                } ?: throw Exception("Customer ID is required")
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: CustomerKey ->
                    key.customerId?.let { customerId ->
                        repository.observeCustomer(key.workspaceId, customerId)
                            .map { it ?: throw Exception("Customer not found") }
                    } ?: kotlinx.coroutines.flow.flowOf()
                },
                writer = { _: CustomerKey, _: Customer ->
                    // Writing is handled through repository methods
                }
            )
        ).build()

    // Convenience methods for common operations
    fun observeCustomers(workspaceId: String): Flow<List<CustomerListItem>> {
        return repository.observeCustomers(workspaceId)
    }

    fun searchCustomers(workspaceId: String, query: String): Flow<List<CustomerListItem>> {
        return repository.searchCustomers(workspaceId, query)
    }

    fun observeCustomer(workspaceId: String, customerId: String): Flow<Customer?> {
        return repository.observeCustomer(workspaceId, customerId)
    }

    suspend fun createCustomer(workspaceId: String, customer: Customer): Result<Customer> {
        return repository.createCustomer(workspaceId, customer)
    }

    suspend fun updateCustomer(workspaceId: String, customer: Customer): Result<Customer> {
        return repository.updateCustomer(workspaceId, customer)
    }

    suspend fun deleteCustomer(workspaceId: String, customerId: String): Result<Unit> {
        return repository.deleteCustomer(workspaceId, customerId)
    }

    suspend fun syncCustomers(workspaceId: String): Result<Int> {
        return repository.syncCustomers(workspaceId)
    }

    suspend fun getCustomerCount(workspaceId: String): Int {
        return repository.getCustomerCount(workspaceId)
    }
}