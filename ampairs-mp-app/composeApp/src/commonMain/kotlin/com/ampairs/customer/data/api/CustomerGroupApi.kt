package com.ampairs.customer.data.api

import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.customer.domain.MasterCustomerGroup
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse

interface CustomerGroupApi {

    suspend fun getCustomerGroups(
        page: Int = 0,
        size: Int = 100,
        lastSyncTime: String? = null,
        sortBy: String = "updatedAt",
        sortDirection: String = "ASC"
    ): ApiResponse<PageResponse<CustomerGroup>>

    suspend fun getMasterCustomerGroups(): ApiResponse<List<MasterCustomerGroup>>

    suspend fun getCustomerGroupById(id: String): ApiResponse<CustomerGroup>

    suspend fun createCustomerGroup(customerGroup: CustomerGroup): ApiResponse<CustomerGroup>

    suspend fun updateCustomerGroup(id: String, customerGroup: CustomerGroup): ApiResponse<CustomerGroup>

    suspend fun deleteCustomerGroup(id: String): ApiResponse<Unit>

    suspend fun searchCustomerGroups(
        query: String,
        page: Int = 0,
        size: Int = 100
    ): ApiResponse<PageResponse<CustomerGroup>>
}