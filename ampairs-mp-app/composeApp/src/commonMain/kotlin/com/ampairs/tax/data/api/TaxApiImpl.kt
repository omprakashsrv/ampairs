package com.ampairs.tax.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.put
import com.ampairs.common.delete
import com.ampairs.common.model.Response
import com.ampairs.tax.domain.HsnCode
import com.ampairs.tax.domain.TaxCalculationRequest
import com.ampairs.tax.domain.TaxCalculationResult
import com.ampairs.tax.domain.TaxRate
import io.ktor.client.engine.HttpClientEngine

const val TAX_ENDPOINT = "http://localhost:8080"

class TaxApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : TaxApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getHsnCodes(
        workspaceId: String,
        page: Int,
        size: Int,
        search: String?,
        category: String?
    ): Result<List<HsnCode>> {
        return try {
            val params = mutableMapOf<String, Any>(
                "page" to page,
                "size" to size
            )
            search?.let { params["search"] = it }
            category?.let { params["category"] = it }

            val response: Response<List<HsnCode>> = get(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/hsn",
                params
            )
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHsnCode(workspaceId: String, hsnCodeId: String): Result<HsnCode> {
        return try {
            val response: Response<HsnCode> = get(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/hsn/$hsnCodeId"
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("HSN Code not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createHsnCode(workspaceId: String, hsnCode: HsnCode): Result<HsnCode> {
        return try {
            val response: Response<HsnCode> = post(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/hsn",
                hsnCode
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to create HSN Code"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateHsnCode(
        workspaceId: String,
        hsnCodeId: String,
        hsnCode: HsnCode
    ): Result<HsnCode> {
        return try {
            val response: Response<HsnCode> = put(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/hsn/$hsnCodeId",
                hsnCode
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to update HSN Code"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHsnCode(workspaceId: String, hsnCodeId: String): Result<Unit> {
        return try {
            delete<Unit>(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/hsn/$hsnCodeId"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchHsnCodes(workspaceId: String, query: String): Result<List<HsnCode>> {
        return try {
            val params = mapOf("query" to query)
            val response: Response<List<HsnCode>> = get(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/hsn/search",
                params
            )
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTaxRates(
        workspaceId: String,
        hsnCode: String?,
        businessType: String?,
        effectiveDate: Long?
    ): Result<List<TaxRate>> {
        return try {
            val params = mutableMapOf<String, Any>()
            hsnCode?.let { params["hsn_code"] = it }
            businessType?.let { params["business_type"] = it }
            effectiveDate?.let { params["effective_date"] = it }

            val response: Response<List<TaxRate>> = get(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/rate",
                params
            )
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEffectiveTaxRate(
        workspaceId: String,
        hsnCode: String,
        businessType: String,
        effectiveDate: Long
    ): Result<TaxRate> {
        return try {
            val params = mapOf(
                "hsn_code" to hsnCode,
                "business_type" to businessType,
                "effective_date" to effectiveDate
            )
            val response: Response<TaxRate> = get(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/rate/effective",
                params
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Effective tax rate not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTaxRate(workspaceId: String, taxRateId: String): Result<TaxRate> {
        return try {
            val response: Response<TaxRate> = get(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/rate/$taxRateId"
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Tax Rate not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createTaxRate(workspaceId: String, taxRate: TaxRate): Result<TaxRate> {
        return try {
            val response: Response<TaxRate> = post(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/rate",
                taxRate
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to create Tax Rate"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTaxRate(
        workspaceId: String,
        taxRateId: String,
        taxRate: TaxRate
    ): Result<TaxRate> {
        return try {
            val response: Response<TaxRate> = put(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/rate/$taxRateId",
                taxRate
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to update Tax Rate"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTaxRate(workspaceId: String, taxRateId: String): Result<Unit> {
        return try {
            delete<Unit>(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/rate/$taxRateId"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calculateTax(
        workspaceId: String,
        request: TaxCalculationRequest
    ): Result<TaxCalculationResult> {
        return try {
            val response: Response<TaxCalculationResult> = post(
                client,
                "$TAX_ENDPOINT/workspace/$workspaceId/tax-code/v1/calculate",
                request
            )
            response.data?.let { Result.success(it) }
                ?: Result.failure(Exception("Failed to calculate tax"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}