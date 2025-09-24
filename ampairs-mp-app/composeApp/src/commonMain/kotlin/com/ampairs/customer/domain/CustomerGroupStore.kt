package com.ampairs.customer.domain

import com.ampairs.customer.data.api.CustomerGroupApi
import com.ampairs.customer.data.db.CustomerGroupDao
import com.ampairs.customer.data.db.toCustomerGroup
import com.ampairs.customer.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import com.ampairs.customer.util.CustomerLogger

class CustomerGroupStore(
    private val customerGroupApi: CustomerGroupApi,
    private val customerGroupDao: CustomerGroupDao
) {
    val customerGroupStore: Store<CustomerGroupKey, List<CustomerGroup>> = StoreBuilder
        .from(
            fetcher = Fetcher.ofFlow { key ->
                kotlinx.coroutines.flow.flow {
                    try {
                        val response = if (key.searchQuery.isNotBlank()) {
                            customerGroupApi.searchCustomerGroups(key.searchQuery, key.page, key.size)
                        } else {
                            customerGroupApi.getCustomerGroups(key.page, key.size)
                        }

                        if (response.success && response.data != null) {
                            emit(response.data.content)
                        } else {
                            throw Exception(response.error?.message ?: "Failed to fetch customer groups")
                        }
                    } catch (e: Exception) {
                        CustomerLogger.e("CustomerGroupStore", "Error fetching customer groups", e)
                        throw e
                    }
                }
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: CustomerGroupKey ->
                    if (key.searchQuery.isNotBlank()) {
                        customerGroupDao.searchCustomerGroups(key.searchQuery).map { entities ->
                            entities.map { it.toCustomerGroup() }
                        }
                    } else {
                        customerGroupDao.getAllCustomerGroups().map { entities ->
                            entities.map { it.toCustomerGroup() }
                        }
                    }
                },
                writer = { _: CustomerGroupKey, customerGroups: List<CustomerGroup> ->
                    customerGroupDao.insertCustomerGroups(customerGroups.map { it.toEntity().copy(synced = true) })
                }
            )
        )
        .build()

    /**
     * Get all customer groups for selection/autocomplete
     */
    fun getCustomerGroupsFlow(): Flow<List<CustomerGroup>> {
        return customerGroupDao.getAllCustomerGroups().map { entities ->
            entities.map { it.toCustomerGroup() }
        }
    }

    /**
     * Get customer group by name for form validation
     */
    suspend fun getCustomerGroupByName(name: String): CustomerGroup? {
        return customerGroupDao.getCustomerGroupByName(name)?.toCustomerGroup()
    }
}