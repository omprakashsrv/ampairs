package com.ampairs.customer.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.put
import com.ampairs.common.delete
import com.ampairs.customer.domain.CustomerGroup
import com.ampairs.customer.domain.MasterCustomerGroup
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import io.ktor.client.engine.HttpClientEngine

class CustomerGroupApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : CustomerGroupApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getCustomerGroups(
        page: Int,
        size: Int,
        lastSyncTime: String?,
        sortBy: String,
        sortDirection: String
    ): ApiResponse<PageResponse<CustomerGroup>> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
            "sortBy" to sortBy,
            "sortDirection" to sortDirection
        )

        lastSyncTime?.let { params["lastSyncTime"] = it }

        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/groups", params)
        return client.get<ApiResponse<PageResponse<CustomerGroup>>>(url)
    }

    override suspend fun getMasterCustomerGroups(): ApiResponse<List<MasterCustomerGroup>> {
        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/groups/available")
        return client.get<ApiResponse<List<MasterCustomerGroup>>>(url)
    }

    override suspend fun getCustomerGroupById(id: String): ApiResponse<CustomerGroup> {
        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/groups/$id")
        return client.get<ApiResponse<CustomerGroup>>(url)
    }

    override suspend fun createCustomerGroup(customerGroup: CustomerGroup): ApiResponse<CustomerGroup> {
        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/groups")
        return client.post<CustomerGroup, ApiResponse<CustomerGroup>>(url, customerGroup)
    }

    override suspend fun updateCustomerGroup(id: String, customerGroup: CustomerGroup): ApiResponse<CustomerGroup> {
        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/groups/$id")
        return client.put<CustomerGroup, ApiResponse<CustomerGroup>>(url, customerGroup)
    }

    override suspend fun deleteCustomerGroup(id: String): ApiResponse<Unit> {
        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/groups/$id")
        return client.delete<ApiResponse<Unit>>(url)
    }

    override suspend fun searchCustomerGroups(
        query: String,
        page: Int,
        size: Int
    ): ApiResponse<PageResponse<CustomerGroup>> {
        val params = mapOf(
            "query" to query,
            "page" to page.toString(),
            "size" to size.toString()
        )
        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/groups/search", params)
        return client.get<ApiResponse<PageResponse<CustomerGroup>>>(url)
    }
}