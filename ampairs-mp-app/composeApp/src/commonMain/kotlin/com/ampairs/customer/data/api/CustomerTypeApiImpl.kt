package com.ampairs.customer.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.put
import com.ampairs.common.delete
import com.ampairs.customer.domain.CustomerType
import com.ampairs.customer.domain.MasterCustomerType
import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import io.ktor.client.engine.HttpClientEngine

class CustomerTypeApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : CustomerTypeApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getCustomerTypes(
        page: Int,
        size: Int,
        lastSyncTime: String?,
        sortBy: String,
        sortDirection: String
    ): ApiResponse<PageResponse<CustomerType>> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "size" to size.toString(),
            "sortBy" to sortBy,
            "sortDirection" to sortDirection
        )

        lastSyncTime?.let { params["lastSyncTime"] = it }

        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/types", params)
        return client.get<ApiResponse<PageResponse<CustomerType>>>(url)
    }

    override suspend fun getMasterCustomerTypes(): ApiResponse<List<MasterCustomerType>> {
        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/types/available")
        return client.get<ApiResponse<List<MasterCustomerType>>>(url)
    }

    override suspend fun getCustomerTypeById(id: String): ApiResponse<CustomerType> {
        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/types/$id")
        return client.get<ApiResponse<CustomerType>>(url)
    }

    override suspend fun createCustomerType(customerType: CustomerType): ApiResponse<CustomerType> {
        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/types")
        return client.post<CustomerType, ApiResponse<CustomerType>>(url, customerType)
    }

    override suspend fun updateCustomerType(id: String, customerType: CustomerType): ApiResponse<CustomerType> {
        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/types/$id")
        return client.put<CustomerType, ApiResponse<CustomerType>>(url, customerType)
    }

    override suspend fun deleteCustomerType(id: String): ApiResponse<Unit> {
        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/types/$id")
        return client.delete<ApiResponse<Unit>>(url)
    }

    override suspend fun searchCustomerTypes(
        query: String,
        page: Int,
        size: Int
    ): ApiResponse<PageResponse<CustomerType>> {
        val params = mapOf(
            "query" to query,
            "page" to page.toString(),
            "size" to size.toString()
        )
        val url = ApiUrlBuilder.buildUrl("customer-management/v1/master/types/search", params)
        return client.get<ApiResponse<PageResponse<CustomerType>>>(url)
    }
}