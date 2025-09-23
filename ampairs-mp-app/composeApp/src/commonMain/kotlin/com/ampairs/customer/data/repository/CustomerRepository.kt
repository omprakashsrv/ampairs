package com.ampairs.customer.data.repository

import com.ampairs.customer.data.api.CustomerApi
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.customer.data.db.toDomain
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.customer.domain.toListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val customerApi: CustomerApi
) {

    fun observeCustomers(): Flow<List<CustomerListItem>> {
        return customerDao.getAllCustomers()
            .map { entities -> entities.map { it.toDomain().toListItem() } }
    }

    fun observeCustomer(customerId: String): Flow<Customer?> {
        return customerDao.observeCustomerById(customerId)
            .map { it?.toDomain() }
    }

    fun searchCustomers(query: String): Flow<List<CustomerListItem>> {
        return customerDao.searchCustomers(query)
            .map { entities -> entities.map { it.toDomain().toListItem() } }
    }

    suspend fun getCustomer(customerId: String): Customer? {
        return customerDao.getCustomerById(customerId)?.toDomain()
    }

    suspend fun createCustomer(customer: Customer): Result<Customer> {
        // Offline-first: Save to database first with unsynced status
        customerDao.insertCustomer(customer.toEntity())

        // Try to sync to server in background
        try {
            val serverCustomer = customerApi.createCustomer(customer)

            // Update local record with server data and mark as synced
            val syncedEntity = serverCustomer.toEntity().copy(synced = true)
            customerDao.updateCustomer(syncedEntity)

            return Result.success(serverCustomer)
        } catch (e: Exception) {
            // If server sync fails, customer is already saved locally as unsynced
            // It will be synced later via syncCustomers()
            return Result.success(customer)
        }
    }

    suspend fun updateCustomer(customer: Customer): Result<Customer> {
        // Offline-first: Update database first with unsynced status
        customerDao.updateCustomer(customer.toEntity())

        // Try to sync to server in background
        try {
            val serverCustomer = customerApi.updateCustomer(customer)

            // Update local record with server data and mark as synced
            val syncedEntity = serverCustomer.toEntity().copy(synced = true)
            customerDao.updateCustomer(syncedEntity)

            return Result.success(serverCustomer)
        } catch (e: Exception) {
            // If server sync fails, customer is already updated locally as unsynced
            // It will be synced later via syncCustomers()
            return Result.success(customer)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        return try {
            // Delete on server first
            customerApi.deleteCustomer(customerId)

            // Delete from local database
            customerDao.deleteCustomer(customerId)

            Result.success(Unit)
        } catch (e: Exception) {
            // Mark as deleted locally if server fails
            val customer = customerDao.getCustomerById(customerId)
            if (customer != null) {
                customerDao.updateCustomer(customer.copy(active = false))
            }
            Result.success(Unit)
        }
    }

    suspend fun syncCustomers(): Result<Int> {
        return try {
            val lastSync = getLastSyncTime()
            val serverCustomers = customerApi.getCustomers(lastSync)

            // Insert/update customers from server and mark as synced
            val entities = serverCustomers.map { it.toEntity().copy(synced = true) }
            customerDao.insertCustomers(entities)

            // Sync unsynced local customers to server
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()
            for (entity in unsyncedCustomers) {
                val customer = entity.toDomain()
                try {
                    if (entity.id.startsWith("local_")) {
                        // Legacy local IDs - create new customer on server
                        val serverCustomer = customerApi.createCustomer(customer)
                        customerDao.deleteCustomer(entity.id) // Remove local temp
                        customerDao.insertCustomer(serverCustomer.toEntity().copy(synced = true))
                    } else {
                        // Proper UIDs - update existing customer on server
                        val serverCustomer = customerApi.updateCustomer(customer)
                        // Update with server response and mark as synced
                        customerDao.updateCustomer(serverCustomer.toEntity().copy(synced = true))
                    }
                } catch (syncError: Exception) {
                    // Continue with other customers if one fails
                    // Failed customer remains unsynced for next attempt
                    continue
                }
            }

            Result.success(serverCustomers.size + unsyncedCustomers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerCount(): Int {
        return customerDao.getCustomerCount()
    }

    suspend fun getUniqueCities(): List<String> {
        return customerDao.getUniqueCities()
    }

    suspend fun getUniquePincodes(): List<String> {
        return customerDao.getUniquePincodes()
    }

    private fun getLastSyncTime(): Long {
        // Implementation to get last sync time from preferences or metadata
        return 0L // Placeholder
    }

}