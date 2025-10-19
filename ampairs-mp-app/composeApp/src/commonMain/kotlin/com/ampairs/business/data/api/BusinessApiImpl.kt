package com.ampairs.business.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.business.domain.*
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.common.put
import io.ktor.client.engine.HttpClientEngine

/**
 * Implementation of BusinessApi using Ktor HTTP client.
 * Handles all business management API calls.
 */
class BusinessApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : BusinessApi {

    private val client = httpClient(engine, tokenRepository)

    // ==================== Creation ====================

    override suspend fun createBusinessProfile(request: BusinessCreateRequest): Result<BusinessProfile> {
        return try {
            val response: Response<BusinessProfile> = post(
                client,
                ApiUrlBuilder.businessUrl(),
                request
            )
            handleResponse(response, "Failed to create business profile")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkBusinessExists(): Result<Boolean> {
        return try {
            val response: Response<Map<String, Boolean>> = get(
                client,
                ApiUrlBuilder.businessUrl("exists")
            )
            val data = response.data
            val error = response.error

            when {
                data != null && error == null -> Result.success(data["exists"] ?: false)
                error != null -> Result.failure(Exception(error.message))
                else -> Result.failure(IllegalStateException("Failed to check business existence"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Overview ====================

    override suspend fun getBusinessOverview(): Result<BusinessOverview> {
        return try {
            val response: Response<BusinessOverview> = get(
                client,
                ApiUrlBuilder.businessUrl("overview")
            )
            handleResponse(response, "Failed to fetch business overview")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Profile & Registration ====================

    override suspend fun getBusinessProfile(): Result<BusinessProfile> {
        return try {
            val response: Response<BusinessProfile> = get(
                client,
                ApiUrlBuilder.businessUrl("profile")
            )
            handleResponse(response, "Failed to fetch business profile")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBusinessProfile(request: BusinessProfileUpdateRequest): Result<BusinessProfile> {
        return try {
            val response: Response<BusinessProfile> = put(
                client,
                ApiUrlBuilder.businessUrl("profile"),
                request
            )
            handleResponse(response, "Failed to update business profile")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Operations ====================

    override suspend fun getBusinessOperations(): Result<BusinessOperations> {
        return try {
            val response: Response<BusinessOperations> = get(
                client,
                ApiUrlBuilder.businessUrl("operations")
            )
            handleResponse(response, "Failed to fetch business operations")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateBusinessOperations(request: BusinessOperationsUpdateRequest): Result<BusinessOperations> {
        return try {
            val response: Response<BusinessOperations> = put(
                client,
                ApiUrlBuilder.businessUrl("operations"),
                request
            )
            handleResponse(response, "Failed to update business operations")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Tax Configuration ====================

    override suspend fun getTaxConfiguration(): Result<TaxConfiguration> {
        return try {
            val response: Response<TaxConfiguration> = get(
                client,
                ApiUrlBuilder.businessUrl("tax-config")
            )
            handleResponse(response, "Failed to fetch tax configuration")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTaxConfiguration(request: TaxConfigurationUpdateRequest): Result<TaxConfiguration> {
        return try {
            val response: Response<TaxConfiguration> = put(
                client,
                ApiUrlBuilder.businessUrl("tax-config"),
                request
            )
            handleResponse(response, "Failed to update tax configuration")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Legacy Methods (Backward Compatibility) ====================

    override suspend fun getBusiness(): Result<Business> {
        return try {
            val response: Response<Business> = get(
                client,
                ApiUrlBuilder.businessUrl()
            )
            handleResponse(response, "Failed to fetch business")
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
            handleResponse(response, "Failed to update business")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Response Handlers ====================

    private fun <T> handleResponse(response: Response<T>, errorMessage: String): Result<T> {
        val data = response.data
        val error = response.error

        return when {
            data != null && error == null -> Result.success(data)
            error != null -> Result.failure(Exception(error.message))
            else -> Result.failure(IllegalStateException(errorMessage))
        }
    }
}
