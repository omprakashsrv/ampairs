package com.ampairs.customer.data.api

import com.ampairs.customer.domain.CustomerType
import com.ampairs.customer.domain.MasterCustomerType
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse

interface CustomerTypeApi {

    suspend fun getCustomerTypes(
        page: Int = 0,
        size: Int = 100,
        lastSyncTime: String? = null,
        sortBy: String = "updatedAt",
        sortDirection: String = "ASC"
    ): ApiResponse<PageResponse<CustomerType>>

    suspend fun getMasterCustomerTypes(): ApiResponse<List<MasterCustomerType>>

    suspend fun getCustomerTypeById(id: String): ApiResponse<CustomerType>

    suspend fun createCustomerType(customerType: CustomerType): ApiResponse<CustomerType>

    suspend fun updateCustomerType(id: String, customerType: CustomerType): ApiResponse<CustomerType>

    suspend fun deleteCustomerType(id: String): ApiResponse<Unit>

    suspend fun searchCustomerTypes(
        query: String,
        page: Int = 0,
        size: Int = 100
    ): ApiResponse<PageResponse<CustomerType>>
}