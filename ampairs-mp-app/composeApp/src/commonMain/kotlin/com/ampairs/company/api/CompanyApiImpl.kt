package com.ampairs.company.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.company.api.model.CompanyApiModel
import com.ampairs.network.model.Response
import io.ktor.client.engine.HttpClientEngine

const val COMPANY_ENDPOINT = "http://localhost:8080"

class CompanyApiImpl(engine: HttpClientEngine, tokenRepository: TokenRepository) : CompanyApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getCompanies(): Response<List<CompanyApiModel>> {
        return get(
            client,
            COMPANY_ENDPOINT + "/company/v1"
        )
    }

    override suspend fun updateCompany(company: CompanyApiModel): Response<CompanyApiModel> {
        return post(
            client,
            COMPANY_ENDPOINT + "/company/v1",
            company
        )
    }
}