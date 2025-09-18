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

    fun observeCustomers(workspaceId: String): Flow<List<CustomerListItem>> {
        return customerDao.getAllCustomers(workspaceId)
            .map { entities -> entities.map { it.toDomain().toListItem() } }
    }

    fun observeCustomer(workspaceId: String, customerId: String): Flow<Customer?> {
        return customerDao.observeCustomerById(workspaceId, customerId)
            .map { it?.toDomain() }
    }

    fun searchCustomers(workspaceId: String, query: String): Flow<List<CustomerListItem>> {
        return customerDao.searchCustomers(workspaceId, query)
            .map { entities -> entities.map { it.toDomain().toListItem() } }
    }

    suspend fun getCustomer(workspaceId: String, customerId: String): Customer? {
        return customerDao.getCustomerById(workspaceId, customerId)?.toDomain()
    }

    suspend fun createCustomer(workspaceId: String, customer: Customer): Result<Customer> {
        return try {
            // Create on server first
            val serverCustomer = customerApi.createCustomer(workspaceId, customer)

            // Save to local database
            customerDao.insertCustomer(serverCustomer.toEntity())

            Result.success(serverCustomer)
        } catch (e: Exception) {
            // Save locally if server fails (offline mode)
            val localCustomer = customer.copy(
                id = generateLocalId(),
                workspaceId = workspaceId
            )
            customerDao.insertCustomer(localCustomer.toEntity())
            Result.success(localCustomer)
        }
    }

    suspend fun updateCustomer(workspaceId: String, customer: Customer): Result<Customer> {
        return try {
            // Update on server first
            val serverCustomer = customerApi.updateCustomer(workspaceId, customer)

            // Update local database
            customerDao.updateCustomer(serverCustomer.toEntity())

            Result.success(serverCustomer)
        } catch (e: Exception) {
            // Update locally if server fails (offline mode)
            customerDao.updateCustomer(customer.toEntity())
            Result.success(customer)
        }
    }

    suspend fun deleteCustomer(workspaceId: String, customerId: String): Result<Unit> {
        return try {
            // Delete on server first
            customerApi.deleteCustomer(workspaceId, customerId)

            // Delete from local database
            customerDao.deleteCustomer(workspaceId, customerId)

            Result.success(Unit)
        } catch (e: Exception) {
            // Mark as deleted locally if server fails
            val customer = customerDao.getCustomerById(workspaceId, customerId)
            if (customer != null) {
                customerDao.updateCustomer(customer.copy(active = false))
            }
            Result.success(Unit)
        }
    }

    suspend fun syncCustomers(workspaceId: String): Result<Int> {
        return try {
            val lastSync = getLastSyncTime(workspaceId)
            val serverCustomers = customerApi.getCustomers(workspaceId, lastSync)

            // Insert/update customers from server
            val entities = serverCustomers.map { it.toEntity() }
            customerDao.insertCustomers(entities)

            // Sync unsynced local customers to server
            val unsyncedCustomers = customerDao.getUnsyncedCustomers(workspaceId)
            for (entity in unsyncedCustomers) {
                try {
                    val customer = entity.toDomain()
                    if (entity.id.startsWith("local_")) {
                        // Create new customer on server
                        val serverCustomer = customerApi.createCustomer(workspaceId, customer)
                        customerDao.deleteCustomer(workspaceId, entity.id) // Remove local temp
                        customerDao.insertCustomer(serverCustomer.toEntity())
                    } else {
                        // Update existing customer on server
                        customerApi.updateCustomer(workspaceId, customer)
                        customerDao.markAsSynced(workspaceId, entity.id)
                    }
                } catch (e: Exception) {
                    // Skip failed syncs, will retry later
                }
            }

            Result.success(serverCustomers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerCount(workspaceId: String): Int {
        return customerDao.getCustomerCount(workspaceId)
    }

    private suspend fun getLastSyncTime(workspaceId: String): Long {
        // Implementation to get last sync time from preferences or metadata
        return 0L // Placeholder
    }

    @OptIn(ExperimentalTime::class)
    private fun generateLocalId(): String {
        return "local_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
}