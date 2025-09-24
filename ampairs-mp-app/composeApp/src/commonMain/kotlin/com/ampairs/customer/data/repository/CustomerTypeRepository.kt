package com.ampairs.customer.data.repository

import com.ampairs.common.util.UidGenerator
import com.ampairs.customer.data.api.CustomerTypeApi
import com.ampairs.customer.data.db.CustomerTypeDao
import com.ampairs.customer.data.db.toCustomerType
import com.ampairs.customer.data.db.toEntity
import com.ampairs.customer.domain.CustomerType
import com.ampairs.customer.domain.CustomerTypeKey
import com.ampairs.customer.domain.CustomerTypeStore
import com.ampairs.customer.domain.MasterCustomerType
import com.ampairs.customer.domain.toCustomerType
import com.ampairs.customer.util.CustomerConstants
import com.ampairs.customer.util.CustomerLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

class CustomerTypeRepository(
    private val customerTypeApi: CustomerTypeApi,
    private val customerTypeDao: CustomerTypeDao,
    private val customerTypeStore: CustomerTypeStore
) {

    /**
     * Get customer types with Store5 integration for offline-first
     */
    fun getCustomerTypesFlow(
        page: Int = 0,
        size: Int = 100,
        forceRefresh: Boolean = false
    ): Flow<StoreReadResponse<List<CustomerType>>> {
        val key = CustomerTypeKey(page = page, size = size)
        return customerTypeStore.customerTypeStore.stream(
            StoreReadRequest.cached(key, refresh = forceRefresh)
        )
    }

    /**
     * Search customer types
     */
    fun searchCustomerTypes(
        query: String,
        page: Int = 0,
        size: Int = 100
    ): Flow<List<CustomerType>> {
        return if (query.isBlank()) {
            customerTypeDao.getAllCustomerTypes().map { entities ->
                entities.map { it.toCustomerType() }
            }
        } else {
            customerTypeDao.searchCustomerTypes(query).map { entities ->
                entities.map { it.toCustomerType() }
            }
        }
    }

    /**
     * Get all customer types for dropdown/autocomplete
     */
    fun getAllCustomerTypesFlow(): Flow<List<CustomerType>> {
        return customerTypeStore.getCustomerTypesFlow()
    }

    /**
     * Create a new customer type
     */
    suspend fun createCustomerType(customerType: CustomerType): Result<CustomerType> {
        return try {
            // Generate UID if not provided
            val uid = if (customerType.id.isBlank()) {
                UidGenerator.generateUid(CustomerConstants.UID_PREFIX)
            } else {
                customerType.id
            }

            val customerTypeWithUid = customerType.copy(id = uid)

            // 1. Save locally first with unsynced status
            val unsyncedEntity = customerTypeWithUid.toEntity().copy(synced = false)
            customerTypeDao.insertCustomerType(unsyncedEntity)

            // 2. Try to sync with server
            try {
                val response = customerTypeApi.createCustomerType(customerTypeWithUid)
                if (response.success && response.data != null) {
                    // Server success - correct UID if needed and mark as synced
                    val serverCustomerType = response.data
                    val finalCustomerType = if (serverCustomerType.id != uid) {
                        serverCustomerType.copy(id = uid) // Keep local UID consistent
                    } else {
                        serverCustomerType
                    }
                    customerTypeDao.insertCustomerType(finalCustomerType.toEntity().copy(synced = true))
                    Result.success(finalCustomerType)
                } else {
                    // Server failed but data is saved locally
                    Result.success(customerTypeWithUid)
                }
            } catch (e: Exception) {
                CustomerLogger.w("CustomerTypeRepository", "Server sync failed, using local data", e)
                Result.success(customerTypeWithUid)
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to create customer type", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing customer type
     */
    suspend fun updateCustomerType(customerType: CustomerType): Result<CustomerType> {
        return try {
            // 1. Save locally first with unsynced status
            val unsyncedEntity = customerType.toEntity().copy(synced = false)
            customerTypeDao.updateCustomerType(unsyncedEntity)

            // 2. Try to sync with server
            try {
                val response = customerTypeApi.updateCustomerType(customerType.id, customerType)
                if (response.success && response.data != null) {
                    customerTypeDao.insertCustomerType(response.data.toEntity().copy(synced = true))
                    Result.success(response.data)
                } else {
                    Result.success(customerType)
                }
            } catch (e: Exception) {
                CustomerLogger.w("CustomerTypeRepository", "Server update failed, using local data", e)
                Result.success(customerType)
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to update customer type", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a customer type
     */
    suspend fun deleteCustomerType(id: String): Result<Unit> {
        return try {
            // 1. Mark as inactive locally first
            customerTypeDao.deleteCustomerType(id)

            // 2. Try to delete from server
            try {
                customerTypeApi.deleteCustomerType(id)
            } catch (e: Exception) {
                CustomerLogger.w("CustomerTypeRepository", "Server delete failed, marked inactive locally", e)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to delete customer type", e)
            Result.failure(e)
        }
    }

    /**
     * Import customer types from master list
     */
    suspend fun importCustomerType(masterCustomerType: MasterCustomerType): Result<CustomerType> {
        return createCustomerType(masterCustomerType.toCustomerType())
    }

    /**
     * Bulk import customer types
     */
    suspend fun bulkImportCustomerTypes(masterCustomerTypes: List<MasterCustomerType>): Result<List<CustomerType>> {
        return try {
            val results = masterCustomerTypes.map { masterType ->
                importCustomerType(masterType)
            }

            val successfulImports = results.mapNotNull { result ->
                result.getOrNull()
            }

            Result.success(successfulImports)
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to bulk import customer types", e)
            Result.failure(e)
        }
    }

    /**
     * Get customer type by name
     */
    suspend fun getCustomerTypeByName(name: String): CustomerType? {
        return customerTypeStore.getCustomerTypeByName(name)
    }

    /**
     * Get available customer types for import
     */
    suspend fun getAvailableCustomerTypesForImport(): Result<List<MasterCustomerType>> {
        return try {
            val response = customerTypeApi.getMasterCustomerTypes()
            if (response.success && response.data != null) {
                // Filter out already imported customer types
                val existingTypes = customerTypeDao.getAllCustomerTypes().first().map { it.name }
                val availableTypes = response.data.filter { it.name !in existingTypes }
                Result.success(availableTypes)
            } else {
                Result.failure(Exception(response.error?.message ?: "Failed to load available customer types"))
            }
        } catch (e: Exception) {
            CustomerLogger.e("CustomerTypeRepository", "Failed to load available customer types", e)
            Result.failure(e)
        }
    }
}