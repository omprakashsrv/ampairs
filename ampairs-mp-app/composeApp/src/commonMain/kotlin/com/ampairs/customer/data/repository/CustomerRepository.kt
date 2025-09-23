package com.ampairs.customer.data.repository

import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.customer.data.api.CustomerApi
import com.ampairs.customer.data.db.CustomerDao
import com.ampairs.customer.data.db.toDomain
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.CustomerListItem
import com.ampairs.customer.domain.toListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.ExperimentalTime

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val customerApi: CustomerApi,
    private val appPreferences: AppPreferencesDataStore
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
        // Customer should already have UID assigned by ViewModel
        require(customer.uid.isNotBlank()) { "Customer UID must be set before calling createCustomer" }

        // Offline-first: Save to database first with unsynced status
        val unsyncedEntity = customer.toEntity().copy(synced = false)
        customerDao.insertCustomer(unsyncedEntity)

        // Try to sync to server in background
        try {
            val serverCustomer = customerApi.createCustomer(customer)

            // Validate server response has same UID to prevent duplicates
            if (serverCustomer.uid != customer.uid) {
                // Log warning but keep local UID to avoid duplicates
                println("⚠️ Server returned different UID: ${serverCustomer.uid} vs local: ${customer.uid}")
                val correctedServerCustomer = serverCustomer.copy(uid = customer.uid)
                val syncedEntity = correctedServerCustomer.toEntity().copy(synced = true)
                customerDao.insertCustomer(syncedEntity)
                return Result.success(correctedServerCustomer)
            }

            // Update local record with server data and mark as synced
            val syncedEntity = serverCustomer.toEntity().copy(synced = true)
            customerDao.insertCustomer(syncedEntity) // Use insert with REPLACE strategy

            return Result.success(serverCustomer)
        } catch (e: Exception) {
            // If server sync fails, customer is already saved locally as unsynced
            // It will be synced later via syncCustomers()
            return Result.success(customer)
        }
    }

    suspend fun updateCustomer(customer: Customer): Result<Customer> {
        // Offline-first: Update database first with unsynced status
        val unsyncedEntity = customer.toEntity().copy(synced = false)
        customerDao.insertCustomer(unsyncedEntity) // Use insert with REPLACE strategy

        // Try to sync to server in background
        try {
            val serverCustomer = customerApi.updateCustomer(customer)

            // Update local record with server data and mark as synced
            val syncedEntity = serverCustomer.toEntity().copy(synced = true)
            customerDao.insertCustomer(syncedEntity) // Use insert with REPLACE strategy

            return Result.success(serverCustomer)
        } catch (e: Exception) {
            // If server sync fails, customer is already updated locally as unsynced
            // It will be synced later via syncCustomers()
            return Result.success(customer)
        }
    }

    suspend fun deleteCustomer(customerId: String): Result<Unit> {
        // Offline-first: Mark as deleted locally first
        val customer = customerDao.getCustomerById(customerId)
        if (customer != null) {
            val deletedEntity = customer.copy(active = false, synced = false)
            customerDao.insertCustomer(deletedEntity) // Use insert with REPLACE strategy
        }

        // Try to delete on server in background
        try {
            customerApi.deleteCustomer(customerId)
            // If server delete succeeds, remove from local database completely
            customerDao.deleteCustomer(customerId)
        } catch (e: Exception) {
            // If server delete fails, customer remains marked as deleted locally
            // It will be synced later via syncCustomers()
        }

        return Result.success(Unit)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun syncCustomers(): Result<Int> {
        return try {
            // FIRST: Sync unsynced local customers to server (prevents data loss)
            val unsyncedCustomers = customerDao.getUnsyncedCustomers()
            var syncedCount = 0
            for (entity in unsyncedCustomers) {
                val customer = entity.toDomain()
                try {
                    if (!entity.active) {
                        // Handle deleted customers
                        customerApi.deleteCustomer(customer.uid)
                        // Remove from local database completely after successful server delete
                        customerDao.deleteCustomer(customer.uid)
                        syncedCount++
                    } else {
                        // Handle created/updated customers
                        val serverCustomer = try {
                            customerApi.updateCustomer(customer)
                        } catch (updateError: Exception) {
                            // If update fails, assume customer doesn't exist on server - create it
                            customerApi.createCustomer(customer)
                        }

                        // Update local record with server response and mark as synced
                        val syncedEntity = serverCustomer.toEntity().copy(synced = true)
                        customerDao.insertCustomer(syncedEntity) // Use insert with REPLACE strategy
                        syncedCount++
                    }
                } catch (syncError: Exception) {
                    // Continue with other customers if one fails
                    // Failed customer remains unsynced for next attempt
                    continue
                }
            }

            // SECOND: Pull updates from server in batches (after local changes are synced)
            val batchSyncResult = syncCustomersFromServerInBatches()
            val serverSyncedCount = batchSyncResult.getOrElse { 0 }

            Result.success(syncedCount + serverSyncedCount)
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

    private suspend fun getLastSyncTime(): String {
        return appPreferences.getCustomerLastSyncTime().first()
    }

    /**
     * Extract the maximum updatedAt timestamp from server customers.
     * Uses string comparison since ISO 8601 timestamps are naturally sortable.
     */
    private fun getMaxUpdatedAtFromServerCustomers(serverCustomers: List<Customer>): String {
        return serverCustomers.mapNotNull { customer ->
            customer.updatedAt?.takeIf { it.isNotBlank() }
        }.maxOrNull() ?: ""
    }

    /**
     * Sync customers from server in batches to handle large datasets (10K+ customers).
     * Returns the total number of customers synced.
     */
    private suspend fun syncCustomersFromServerInBatches(batchSize: Int = 100): Result<Int> {
        return try {
            val lastSync = getLastSyncTime()
            var totalSynced = 0
            var currentPage = 0
            var maxServerTime = ""

            do {
                // Fetch one page of customers
                val pageResponse = customerApi.getCustomers(
                    lastSync,
                    currentPage,
                    batchSize,
                    "updatedAt",
                    "ASC"
                )

                // Process this batch
                val batchCustomers = pageResponse.content
                if (batchCustomers.isNotEmpty()) {
                    // Insert/update customers from server and mark as synced
                    // Only insert customers that don't conflict with existing local UIDs
                    val entities = batchCustomers.mapNotNull { serverCustomer ->
                        val existingCustomer = customerDao.getCustomerById(serverCustomer.uid)
                        if (existingCustomer != null && !existingCustomer.synced) {
                            // Skip server customer if we have unsynced local version with same UID
                            println("⚠️ Skipping server customer ${serverCustomer.uid} - conflicts with unsynced local version")
                            null
                        } else {
                            serverCustomer.toEntity().copy(synced = true)
                        }
                    }
                    customerDao.insertCustomers(entities)

                    // Track the latest timestamp from this batch
                    val batchMaxTime = getMaxUpdatedAtFromServerCustomers(batchCustomers)
                    if (batchMaxTime > maxServerTime) {
                        maxServerTime = batchMaxTime
                    }

                    totalSynced += entities.size
                    println("📦 Synced batch ${currentPage + 1}: ${entities.size} customers (page ${currentPage + 1}/${pageResponse.totalPages})")
                }

                currentPage++
            } while (pageResponse.hasNext && totalSynced < 10000) // Safety limit to prevent infinite loops

            // Update last sync time using the latest timestamp from all batches
            if (maxServerTime.isNotBlank()) {
                appPreferences.setCustomerLastSyncTime(maxServerTime)
            }

            println("✅ Batch sync completed: $totalSynced customers synced in $currentPage batches")
            Result.success(totalSynced)
        } catch (e: Exception) {
            println("❌ Batch sync failed: ${e.message}")
            Result.failure(e)
        }
    }

}