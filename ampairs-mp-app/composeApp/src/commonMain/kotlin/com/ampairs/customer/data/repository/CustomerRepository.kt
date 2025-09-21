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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

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
        return try {
            // Create on server first
            val serverCustomer = customerApi.createCustomer(customer)

            // Save to local database
            customerDao.insertCustomer(serverCustomer.toEntity())

            Result.success(serverCustomer)
        } catch (e: Exception) {
            // Save locally if server fails (offline mode)
            val localCustomer = customer.copy(
                uid = generateLocalId(),
            )
            customerDao.insertCustomer(localCustomer.toEntity())
            Result.success(localCustomer)
        }
    }

    suspend fun updateCustomer(customer: Customer): Result<Customer> {
        return try {
            // Update on server first
            val serverCustomer = customerApi.updateCustomer(customer)

            // Update local database
            customerDao.updateCustomer(serverCustomer.toEntity())

            Result.success(serverCustomer)
        } catch (e: Exception) {
            // Update locally if server fails (offline mode)
            customerDao.updateCustomer(customer.toEntity())
            Result.success(customer)
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

            // Insert/update customers from server
            val entities = serverCustomers.map { it.toEntity() }
            customerDao.insertCustomers(entities)

            // Sync unsynced local customers to server
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()
            for (entity in unsyncedCustomers) {
                val customer = entity.toDomain()
                if (entity.id.startsWith("local_")) {
                    // Create new customer on server
                    val serverCustomer = customerApi.createCustomer(customer)
                    customerDao.deleteCustomer(entity.id) // Remove local temp
                    customerDao.insertCustomer(serverCustomer.toEntity())
                } else {
                    // Update existing customer on server
                    customerApi.updateCustomer(customer)
                    customerDao.markAsSynced(entity.id)
                }
            }

            Result.success(serverCustomers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerCount(): Int {
        return customerDao.getCustomerCount()
    }

    private suspend fun getLastSyncTime(): Long {
        // Implementation to get last sync time from preferences or metadata
        return 0L // Placeholder
    }

    @OptIn(ExperimentalTime::class)
    private fun generateLocalId(): String {
        return "local_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
}