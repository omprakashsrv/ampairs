package com.ampairs.customer.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.delete
import com.ampairs.common.model.Response
import com.ampairs.customer.domain.Customer
import com.ampairs.customer.domain.State
import io.ktor.client.engine.HttpClientEngine

const val CUSTOMER_ENDPOINT = "http://localhost:8080"

class CustomerApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : CustomerApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getCustomers(lastSync: Long): List<Customer> {
        val response: Response<List<Customer>> = get(
            client,
            "$CUSTOMER_ENDPOINT/customer/v1",
            mapOf("last_sync" to lastSync)
        )
        return response.data ?: emptyList()
    }

    override suspend fun createCustomer(customer: Customer): Customer {
        val response: Response<Customer> = post(
            client,
            "$CUSTOMER_ENDPOINT/customer/v1",
            customer
        )
        return response.data ?: throw Exception("Failed to create customer")
    }

    override suspend fun updateCustomer(customer: Customer): Customer {
        val response: Response<Customer> = post(
            client,
            "$CUSTOMER_ENDPOINT/customer/v1",
            customer
        )
        return response.data ?: throw Exception("Failed to update customer")
    }

    override suspend fun deleteCustomer(customerId: String) {
        delete<Response<Unit>>(
            client,
            "$CUSTOMER_ENDPOINT/customer/v1/$customerId"
        )
    }

    override suspend fun getCustomer(customerId: String): Customer? {
        return try {
            val response: Response<Customer> = get(
                client,
                "$CUSTOMER_ENDPOINT/customer/v1/$customerId"
            )
            response.data
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getStates(lastSync: Long): List<State> {
        val response: Response<List<State>> = get(
            client,
            "$CUSTOMER_ENDPOINT/customer/v1/states",
            mapOf("last_updated" to lastSync)
        )
        return response.data ?: emptyList()
    }

    override suspend fun createState(state: State): State {
        val response: Response<State> = post(
            client,
            "$CUSTOMER_ENDPOINT/customer/v1/states",
            state
        )
        return response.data ?: throw Exception("Failed to create state")
    }

    override suspend fun updateState(state: State): State {
        val response: Response<State> = post(
            client,
            "$CUSTOMER_ENDPOINT/customer/v1/states",
            state
        )
        return response.data ?: throw Exception("Failed to update state")
    }

    override suspend fun deleteState(stateId: String) {
        delete<Response<Unit>>(
            client,
            "$CUSTOMER_ENDPOINT/customer/v1/states/$stateId"
        )
    }

    override suspend fun getState(stateId: String): State? {
        return try {
            val response: Response<State> = get(
                client,
                "$CUSTOMER_ENDPOINT/customer/v1/states/$stateId"
            )
            response.data
        } catch (_: Exception) {
            null
        }
    }
}