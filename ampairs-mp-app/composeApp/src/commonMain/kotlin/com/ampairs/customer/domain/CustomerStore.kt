package com.ampairs.customer.domain

import com.ampairs.customer.data.repository.CustomerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

data class CustomerKey(val customerId: String)
data class CustomerListKey(val searchQuery: String = "")

@OptIn(ExperimentalStoreApi::class)
class CustomerStore(private val repository: CustomerRepository) {

    val customerListStore: Store<CustomerListKey, List<CustomerListItem>> = StoreBuilder
        .from(
            fetcher = Fetcher.of { key: CustomerListKey ->
                // Only read from local database - sync is handled separately
                if (key.searchQuery.isBlank()) {
                    repository.observeCustomers().first()
                } else {
                    repository.searchCustomers(key.searchQuery).first()
                }
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: CustomerListKey ->
                    if (key.searchQuery.isBlank()) {
                        repository.observeCustomers()
                    } else {
                        repository.searchCustomers(key.searchQuery)
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
                repository.getCustomer(key.customerId)
                    ?: throw Exception("Customer not found: ${key.customerId}")
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: CustomerKey ->
                    repository.observeCustomer(key.customerId)
                        .map { it ?: throw Exception("Customer not found: ${key.customerId}") }
                },
                writer = { _: CustomerKey, _: Customer ->
                    // Writing is handled through repository methods
                }
            )
        ).build()

    suspend fun createCustomer(customer: Customer): Result<Customer> {
        val result = repository.createCustomer(customer)
        if (result.isSuccess) {
            // Invalidate store to refresh data
            customerListStore.clear()
        }
        return result
    }

    suspend fun updateCustomer(customer: Customer): Result<Customer> {
        val result = repository.updateCustomer(customer)
        if (result.isSuccess) {
            // Invalidate stores to refresh data
            customerListStore.clear()
            customerStore.clear()
        }
        return result
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        val result = repository.deleteCustomer(customerId)
        if (result.isSuccess) {
            // Invalidate stores to refresh data
            customerListStore.clear()
            customerStore.clear()
        }
        return result
    }

    suspend fun syncCustomers(): Result<Int> {
        val result = repository.syncCustomers()
        if (result.isSuccess) {
            // Invalidate store to refresh data
            customerListStore.clear()
        }
        return result
    }

    suspend fun getUniqueCities(): List<String> {
        return repository.getUniqueCities()
    }

    suspend fun getUniquePincodes(): List<String> {
        return repository.getUniquePincodes()
    }

}