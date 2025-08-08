package com.ampairs.product.db

import androidx.paging.PagingSource
import com.ampairs.common.flower_core.Resource
import com.ampairs.common.flower_core.dbBoundResource
import com.ampairs.common.flower_core.networkResource
import com.ampairs.product.api.ProductApi
import com.ampairs.product.api.model.TaxCodeApiModel
import com.ampairs.product.api.model.TaxInfoApiModel
import com.ampairs.product.db.entity.TaxCodeEntity
import com.ampairs.product.db.entity.TaxInfoEntity
import com.ampairs.product.domain.TaxCode
import com.ampairs.product.domain.TaxInfo
import com.ampairs.product.domain.asDatabaseModel
import com.ampairs.product.domain.asTaxCodeApiModel
import com.ampairs.product.domain.asTaxInfoApiModel
import com.ampairs.product.domain.asTaxInfoDomainModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class TaxRepository(
    private val taxDao: TaxDaoAdapter,
    private val productApi: ProductApi,
) {

    suspend fun getTaxInfos(): List<TaxInfoEntity> {
        return taxDao.getTaxInfos()
    }

    suspend fun getTaxCodes(): List<TaxCodeEntity> {
        return taxDao.getTaxCodes()
    }

    suspend fun getTaxInfo(id: String): TaxInfoEntity? {
        return taxDao.getTaxInfo(id)
    }

    suspend fun getTaxCode(id: String): TaxCodeEntity? {
        return taxDao.getTaxCodeById(id)
    }

    fun getTaxCodeResource(): Flow<Resource<List<TaxCodeApiModel>>> {
        return networkResource(
            shouldMakeNetworkRequest = { true },
            makeNetworkRequest = {
                flow {
                    emit(productApi.getTaxCodes())
                }
            },
            processNetworkResponse = {
                taxDao.updateTaxCodes(it.asDatabaseModel())
            }).flowOn(Dispatchers.IO)
    }

    fun getTaxInfoResource(): Flow<Resource<List<TaxInfo>>> {
        return dbBoundResource(
            fetchFromLocal = {
                flow {
                    emit(taxDao.getTaxInfos().asTaxInfoDomainModel())
                }
            },
            shouldMakeNetworkRequest = { true },
            makeNetworkRequest = {
                flow {
                    emit(productApi.getTaxInfos())
                }
            },
            processNetworkResponse = {
                taxDao.insertTaxInfos(it.asDatabaseModel())
            },
            onNetworkRequestFailed = { message: String, code: Int ->

            }).flowOn(Dispatchers.IO)
    }

    fun getTaxCodePaging(searchText: String): PagingSource<Int, TaxCodeEntity> {
        return taxDao.getTaxCodePaging(searchText)
    }

    fun getTaxInfoPaging(searchText: String): PagingSource<Int, TaxInfoEntity> {
        return taxDao.getTaxInfoPaging(searchText)
    }

    suspend fun updateTaxInfo(taxInfoToUpdate: TaxInfo) {
        taxDao.updateTaxInfo(taxInfoToUpdate.asDatabaseModel())
        val taxInfos = taxDao.getUnSyncedTaxInfos()
        val updatedTaxInfos = updateTaxInfos(taxInfos.asTaxInfoApiModel())
        updatedTaxInfos?.map {
            it
        }?.asDatabaseModel()?.let {
            taxDao.insertTaxInfos(it)
        }
    }

    suspend fun updateTaxCode(taxCodeToUpdate: TaxCode) {
        taxDao.updateTaxCode(taxCodeToUpdate.asDatabaseModel())
        val taxCodes = taxDao.getUnSyncedTaxCodes()
        val updatedTaxInfos = updateTaxCodes(taxCodes.asTaxCodeApiModel())
        updatedTaxInfos?.map {
            it
        }?.asDatabaseModel()?.let {
            taxDao.updateTaxCodes(it)
        }
    }

    suspend fun updateTaxInfos(taxInfos: List<TaxInfoApiModel>): List<TaxInfoApiModel>? {
        return productApi.updateTaxInfos(taxInfos).data
    }

    suspend fun updateTaxCodes(taxCodes: List<TaxCodeApiModel>): List<TaxCodeApiModel>? {
        return productApi.updateTaxCodes(taxCodes).data
    }

}