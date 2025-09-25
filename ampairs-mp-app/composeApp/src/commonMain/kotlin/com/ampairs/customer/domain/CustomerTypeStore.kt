package com.ampairs.customer.domain

import com.ampairs.customer.data.api.CustomerTypeApi
import com.ampairs.customer.data.db.CustomerTypeDao
import com.ampairs.customer.data.db.toCustomerType
import com.ampairs.customer.data.db.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import com.ampairs.customer.util.CustomerLogger

class CustomerTypeStore(
    private val customerTypeApi: CustomerTypeApi,
    private val customerTypeDao: CustomerTypeDao
) {
    val customerTypeStore: Store<CustomerTypeKey, List<CustomerType>> = StoreBuilder
        .from(
            fetcher = Fetcher.ofFlow { key ->
                kotlinx.coroutines.flow.flow {
                    try {
                        val response = if (key.searchQuery.isNotBlank()) {
                            customerTypeApi.searchCustomerTypes(key.searchQuery, key.page, key.size)
                        } else {
                            customerTypeApi.getCustomerTypes(key.page, key.size)
                        }

                        if (response.data != null && response.error == null) {
                            emit(response.data!!.content)
                        } else {
                            throw Exception(response.error?.message ?: "Failed to fetch customer types")
                        }
                    } catch (e: Exception) {
                        CustomerLogger.e("CustomerTypeStore", "Error fetching customer types", e)
                        throw e
                    }
                }
            },
            sourceOfTruth = SourceOfTruth.of(
                reader = { key: CustomerTypeKey ->
                    if (key.searchQuery.isNotBlank()) {
                        customerTypeDao.searchCustomerTypes(key.searchQuery).map { entities ->
                            entities.map { it.toCustomerType() }
                        }
                    } else {
                        customerTypeDao.getAllCustomerTypes().map { entities ->
                            entities.map { it.toCustomerType() }
                        }
                    }
                },
                writer = { _: CustomerTypeKey, customerTypes: List<CustomerType> ->
                    customerTypeDao.insertCustomerTypes(customerTypes.map { it.toEntity().copy(synced = true) })
                }
            )
        )
        .build()

    /**
     * Get all customer types for selection/autocomplete
     */
    fun getCustomerTypesFlow(): Flow<List<CustomerType>> {
        return customerTypeDao.getAllCustomerTypes().map { entities ->
            entities.map { it.toCustomerType() }
        }
    }

    /**
     * Get customer type by ID for form editing
     */
    suspend fun getCustomerTypeById(id: String): CustomerType? {
        return customerTypeDao.getCustomerTypeById(id)?.toCustomerType()
    }

    /**
     * Get customer type by name for form validation
     */
    suspend fun getCustomerTypeByName(name: String): CustomerType? {
        return customerTypeDao.getCustomerTypeByName(name)?.toCustomerType()
    }
}