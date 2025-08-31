package com.ampairs.customer.db

import androidx.paging.PagingSource
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.flower_core.networkResource
import com.ampairs.customer.api.CustomerApi
import com.ampairs.customer.api.model.CustomerApiModel
import com.ampairs.customer.db.dao.CustomerDao
import com.ampairs.customer.db.entity.CustomerEntity
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.asDatabaseModel
import com.ampairs.common.model.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn

class CustomerRepository(
    val customerDao: CustomerDao, val customerApi: CustomerApi,
) {

    fun getCustomerPaging(searchText: String): PagingSource<Int, CustomerEntity> {
        // TODO: Implement paging with Room - for now return empty paging source
        // This would need a proper PagingSource implementation
        throw NotImplementedError("Customer paging not yet implemented with Room")
    }

    suspend fun syncCustomers(onSyncComplete: (Int) -> Unit) {
        // TODO: Implement proper sync with Room
        // This method needs to be restructured to work with Room patterns
        onSyncComplete(0)
    }

    suspend fun getCustomer(customerId: String): CustomerEntity? {
        return customerDao.selectById(customerId)
    }

    suspend fun getDefaultCustomer(companyId: String): CustomerEntity? {
        return customerDao.selectByCompanyId(companyId).firstOrNull()
    }

    suspend fun updateCustomer(customer: Customer) {
        customerDao.update(customer.asDatabaseModel())
        // TODO: Implement proper sync with Room
        // The sync logic needs to be restructured for Room patterns
    }

    suspend fun updateCustomers(customers: List<CustomerApiModel>): Response<List<CustomerApiModel>> {
        return customerApi.updateCustomers(customers)
    }

    fun getCustomerResource(): Flow<Resource<List<CustomerApiModel>>> {
        return networkResource(
            shouldMakeNetworkRequest = { true },
            makeNetworkRequest = {
                val sharedFlow = MutableSharedFlow<Response<List<CustomerApiModel>>>(replay = 5)
                var fetchSize = 1000
                while (fetchSize == 1000) {
                    val lastUpdated = customerDao.getLastUpdated() ?: 0L
                    val customers = customerApi.getCustomers(lastUpdated)
                    fetchSize = customers.data?.size ?: 0
                    sharedFlow.emit(customers)
                }
                sharedFlow
            },
            processNetworkResponse = {
                it.asDatabaseModel().forEach { customer ->
                    customerDao.insert(customer)
                }
            }).flowOn(Dispatchers.IO)
    }

}