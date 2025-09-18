package com.ampairs.customer.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.put
import com.ampairs.common.delete
import com.ampairs.common.model.Response
import com.ampairs.customer.domain.Customer
import io.ktor.client.engine.HttpClientEngine

class CustomerApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : CustomerApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getCustomers(workspaceId: String, lastSync: Long): List<Customer> {
        val response: Response<List<Customer>> = get(
            client,
            "/workspace/$workspaceId/customer/v1",
            mapOf("last_sync" to lastSync)
        )
        return response.data ?: emptyList()
    }

    override suspend fun createCustomer(workspaceId: String, customer: Customer): Customer {
        val response: Response<Customer> = post(
            client,
            "/workspace/$workspaceId/customer/v1",
            customer.copy(workspaceId = workspaceId)
        )
        return response.data ?: throw Exception("Failed to create customer")
    }

    override suspend fun updateCustomer(workspaceId: String, customer: Customer): Customer {
        val response: Response<Customer> = put(
            client,
            "/workspace/$workspaceId/customer/v1/${customer.id}",
            customer.copy(workspaceId = workspaceId)
        )
        return response.data ?: throw Exception("Failed to update customer")
    }

    override suspend fun deleteCustomer(workspaceId: String, customerId: String) {
        delete<Response<Unit>>(
            client,
            "/workspace/$workspaceId/customer/v1/$customerId"
        )
    }

    override suspend fun getCustomer(workspaceId: String, customerId: String): Customer? {
        return try {
            val response: Response<Customer> = get(
                client,
                "/workspace/$workspaceId/customer/v1/$customerId"
            )
            response.data
        } catch (e: Exception) {
            null
        }
    }
}