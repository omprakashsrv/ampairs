package com.ampairs.company.api

import com.ampairs.company.api.model.CompanyApiModel
import com.ampairs.network.model.Response

interface CompanyApi {

    suspend fun getCompanies(): Response<List<CompanyApiModel>>
    suspend fun updateCompany(company: CompanyApiModel): Response<CompanyApiModel>


}