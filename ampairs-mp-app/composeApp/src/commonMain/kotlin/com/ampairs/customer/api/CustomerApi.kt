package com.ampairs.customer.api

import com.ampairs.customer.api.model.CustomerApiModel
import com.ampairs.common.model.Response

interface CustomerApi {
    suspend fun getCustomers(lastUpdated: Long): Response<List<CustomerApiModel>>

    suspend fun updateCustomers(customers: List<CustomerApiModel>): Response<List<CustomerApiModel>>

}