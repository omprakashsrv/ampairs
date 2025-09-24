package com.ampairs.customer.data.repository

import com.ampairs.common.util.UidGenerator
import com.ampairs.customer.data.api.CustomerGroupApi
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.customer.data.db.toCustomerGroup
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.customer.domain.CustomerGroupKey
import com.ampairs.customer.domain.CustomerGroupStore
import com.ampairs.customer.domain.MasterCustomerGroup
import com.ampairs.customer.domain.toCustomerGroup
import com.ampairs.customer.util.CustomerConstants
import com.ampairs.customer.util.CustomerLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

class CustomerGroupRepository(
    private val customerGroupApi: CustomerGroupApi,
    private val customerGroupDao: CustomerGroupDao,
    private val customerGroupStore: CustomerGroupStore
) {

    /**
     * Get customer groups with Store5 integration for offline-first
     */
    fun getCustomerGroupsFlow(
        page: Int = 0,
        size: Int = 100,
        forceRefresh: Boolean = false
    ): Flow<StoreReadResponse<List<CustomerGroup>>> {
        val key = CustomerGroupKey(page = page, size = size)
        return customerGroupStore.customerGroupStore.stream(
            StoreReadRequest.cached(key, refresh = forceRefresh)
        )
    }

    /**
     * Search customer groups
     */
    fun searchCustomerGroups(
        query: String,
        page: Int = 0,
        size: Int = 100
    ): Flow<List<CustomerGroup>> {
        return if (query.isBlank()) {
            customerGroupDao.getAllCustomerGroups().map { entities ->
                entities.map { it.toCustomerGroup() }
            }
        } else {
            customerGroupDao.searchCustomerGroups(query).map { entities ->
                entities.map { it.toCustomerGroup() }
            }
        }
    }

    /**
     * Get all customer groups for dropdown/autocomplete
     */
    fun getAllCustomerGroupsFlow(): Flow<List<CustomerGroup>> {
        return customerGroupStore.getCustomerGroupsFlow()
    }

    /**
     * Create a new customer group
     */
    suspend fun createCustomerGroup(customerGroup: CustomerGroup): Result<CustomerGroup> {
        return try {
            // Generate UID if not provided
            val uid = if (customerGroup.id.isBlank()) {
                UidGenerator.generateUid(CustomerConstants.UID_PREFIX)
            } else {
                customerGroup.id
            }

            val customerGroupWithUid = customerGroup.copy(id = uid)

            // 1. Save locally first with unsynced status
            val unsyncedEntity = customerGroupWithUid.toEntity().copy(synced = false)
            customerGroupDao.insertCustomerGroup(unsyncedEntity)

            // 2. Try to sync with server
            try {
                val response = customerGroupApi.createCustomerGroup(customerGroupWithUid)
                if (response.success && response.data != null) {
                    // Server success - correct UID if needed and mark as synced
                    val serverCustomerGroup = response.data
                    val finalCustomerGroup = if (serverCustomerGroup.id != uid) {
                        serverCustomerGroup.copy(id = uid) // Keep local UID consistent
                    } else {
                        serverCustomerGroup
                    }
                    customerGroupDao.insertCustomerGroup(finalCustomerGroup.toEntity().copy(synced = true))
                    Result.success(finalCustomerGroup)
                } else {
                    // Server failed but data is saved locally
                    Result.success(customerGroupWithUid)
                }
            } catch (e: Exception) {
                CustomerLogger.w("CustomerGroupRepository", "Server sync failed, using local data", e)
                Result.success(customerGroupWithUid)
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupRepository", "Failed to create customer group", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing customer group
     */
    suspend fun updateCustomerGroup(customerGroup: CustomerGroup): Result<CustomerGroup> {
        return try {
            // 1. Save locally first with unsynced status
            val unsyncedEntity = customerGroup.toEntity().copy(synced = false)
            customerGroupDao.updateCustomerGroup(unsyncedEntity)

            // 2. Try to sync with server
            try {
                val response = customerGroupApi.updateCustomerGroup(customerGroup.id, customerGroup)
                if (response.success && response.data != null) {
                    customerGroupDao.insertCustomerGroup(response.data.toEntity().copy(synced = true))
                    Result.success(response.data)
                } else {
                    Result.success(customerGroup)
                }
            } catch (e: Exception) {
                CustomerLogger.w("CustomerGroupRepository", "Server update failed, using local data", e)
                Result.success(customerGroup)
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupRepository", "Failed to update customer group", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a customer group
     */
    suspend fun deleteCustomerGroup(id: String): Result<Unit> {
        return try {
            // 1. Mark as inactive locally first
            customerGroupDao.deleteCustomerGroup(id)

            // 2. Try to delete from server
            try {
                customerGroupApi.deleteCustomerGroup(id)
            } catch (e: Exception) {
                CustomerLogger.w("CustomerGroupRepository", "Server delete failed, marked inactive locally", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupRepository", "Failed to delete customer group", e)
            Result.failure(e)
        }
    }

    /**
     * Import customer groups from master list
     */
    suspend fun importCustomerGroup(masterCustomerGroup: MasterCustomerGroup): Result<CustomerGroup> {
        return createCustomerGroup(masterCustomerGroup.toCustomerGroup())
    }

    /**
     * Bulk import customer groups
     */
    suspend fun bulkImportCustomerGroups(masterCustomerGroups: List<MasterCustomerGroup>): Result<List<CustomerGroup>> {
        return try {
            val results = masterCustomerGroups.map { masterGroup ->
                importCustomerGroup(masterGroup)
            }

            val successfulImports = results.mapNotNull { result ->
                result.getOrNull()
            }

            Result.success(successfulImports)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupRepository", "Failed to bulk import customer groups", e)
            Result.failure(e)
        }
    }

    /**
     * Get customer group by name
     */
    suspend fun getCustomerGroupByName(name: String): CustomerGroup? {
        return customerGroupStore.getCustomerGroupByName(name)
    }

    /**
     * Get available customer groups for import
     */
    suspend fun getAvailableCustomerGroupsForImport(): Result<List<MasterCustomerGroup>> {
        return try {
            val response = customerGroupApi.getMasterCustomerGroups()
            if (response.success && response.data != null) {
                // Filter out already imported customer groups
                val existingGroups = customerGroupDao.getAllCustomerGroups().first().map { it.name }
                val availableGroups = response.data.filter { it.name !in existingGroups }
                Result.success(availableGroups)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to load available customer groups"))
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerGroupRepository", "Failed to load available customer groups", e)
            Result.failure(e)
        }
    }
}