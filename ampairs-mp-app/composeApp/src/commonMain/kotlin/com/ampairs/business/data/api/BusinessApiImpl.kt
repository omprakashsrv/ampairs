package com.ampairs.business.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.business.domain.Business
import com.ampairs.business.domain.BusinessPayload
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.common.put
import io.ktor.client.engine.HttpClientEngine

class BusinessApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : BusinessApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getBusiness(): Result<Business> {
        return try {
            val response: Response<Business> = get(
                client,
                ApiUrlBuilder.businessUrl()
            )
            handleResponse(response, "Failed to fetch business profile")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createBusiness(payload: BusinessPayload): Result<Business> {
        return try {
            val response: Response<Business> = post(
                client,
                ApiUrlBuilder.businessUrl(),
                payload
            )
            handleResponse(response, "Failed to create business profile")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBusiness(payload: BusinessPayload): Result<Business> {
        return try {
            val response: Response<Business> = put(
                client,
                ApiUrlBuilder.businessUrl(),
                payload
            )
            handleResponse(response, "Failed to update business profile")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Handle API response with proper error checking.
     * Checks both response.error and response.data to ensure valid response.
     */
    private fun handleResponse(response: Response<Business>, errorMessage: String): Result<Business> {
        val data = response.data
        val error = response.error

        return when {
            data != null && error == null -> Result.success(data)
            error != null -> Result.failure(Exception(error.message))
            else -> Result.failure(IllegalStateException(errorMessage))
        }
    }
}
