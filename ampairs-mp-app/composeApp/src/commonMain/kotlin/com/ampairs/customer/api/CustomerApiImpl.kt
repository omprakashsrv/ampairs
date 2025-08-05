package com.ampairs.customer.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.customer.api.model.CustomerApiModel
import com.ampairs.network.model.Response
import io.ktor.client.engine.HttpClientEngine

const val CUSTOMER_ENDPOINT = "http://localhost:8080"

class CustomerApiImpl(engine: HttpClientEngine, tokenRepository: TokenRepository) : CustomerApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getCustomers(lastUpdated: Long): Response<List<CustomerApiModel>> {
        return get(
            client,
            "$CUSTOMER_ENDPOINT/customer/v1",
            buildMap {
                put("last_updated", lastUpdated)
            }
        )
    }

    override suspend fun updateCustomers(customers: List<CustomerApiModel>): Response<List<CustomerApiModel>> {
        return post(
            client,
            "$CUSTOMER_ENDPOINT/customer/v1/customers",
            customers
        )
    }

}