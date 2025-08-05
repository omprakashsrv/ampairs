package com.ampairs.company.db

import androidx.paging.PagingSource
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.flower_core.dbBoundResource
import com.ampairs.common.flower_core.networkResource
import com.ampairs.company.api.CompanyApi
import com.ampairs.company.api.model.CompanyApiModel
import com.ampairs.company.db.dao.CompanyDao
import com.ampairs.company.db.entity.CompanyEntity
import com.ampairs.company.domain.Company
import com.ampairs.company.domain.asApiModel
import com.ampairs.company.domain.asDatabaseModel
import com.ampairs.network.model.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class CompanyRepository(
    val companyApi: CompanyApi,
    val companyDao: CompanyDao,
) {

    suspend fun getCompanyApi(): Response<List<CompanyApiModel>> {
        return companyApi.getCompanies()
    }

    suspend fun getCompany(id: String): CompanyEntity? {
        return companyDao.selectById(id)
    }

    suspend fun saveCompanies(companyApiModel: List<CompanyApiModel>) {
        companyApiModel.asDatabaseModel().forEach { company ->
            companyDao.insert(company)
        }
    }

    fun getCompanyResource(): Flow<Resource<List<CompanyEntity>>> {
        return dbBoundResource(
            shouldMakeNetworkRequest = { true },
            makeNetworkRequest = {
                flow {
                    emit(getCompanyApi())
                }
            },
            fetchFromLocal = {
                flow {
                    emit(companyDao.selectAll())
                }
            },
            onNetworkRequestFailed = { message: String, code: Int ->

            },
            processNetworkResponse = {
                saveCompanies(it)
            }).flowOn(Dispatchers.IO)
    }

    fun getCompanyPaging(): PagingSource<Int, CompanyEntity> {
        return companyDao.companiesPaging()
    }

    fun updateCompany(company: Company): Flow<Resource<CompanyApiModel>> {
        return networkResource(
            shouldMakeNetworkRequest = { true },
            makeNetworkRequest = {
                flow {
                    emit(companyApi.updateCompany(company.asApiModel()))
                }
            },
            processNetworkResponse = {
                companyDao.insert(it.asDatabaseModel())
            }).flowOn(Dispatchers.IO)
    }


}