package com.ampairs.product.db

import androidx.paging.PagingSource
import com.ampairs.product.db.dao.TaxCodeDao
import com.ampairs.product.db.dao.TaxInfoDao
import com.ampairs.product.db.entity.TaxCodeEntity
import com.ampairs.product.db.entity.TaxInfoEntity
import com.ampairs.product.db.entity.TaxCodeEntity as RoomTaxCodeEntity
import com.ampairs.product.db.entity.TaxInfoEntity as RoomTaxInfoEntity

/**
 * Adapter class that combines TaxCodeDao and TaxInfoDao to maintain compatibility 
 * with existing TaxRepository interface during SQLDelight to Room migration
 */
class TaxDaoAdapter(
    private val taxCodeDao: TaxCodeDao,
    private val taxInfoDao: TaxInfoDao
) {
    
    suspend fun updateTaxCodes(taxCodes: List<RoomTaxCodeEntity>) {
        // Convert SQLDelight entities to Room entities
        val roomEntities = taxCodes.map { sqlEntity ->
            RoomTaxCodeEntity(
                id = sqlEntity.id,
                code = sqlEntity.code,
                type = sqlEntity.type,
                description = sqlEntity.description,
                effective_from = sqlEntity.effective_from,
                tax_info = sqlEntity.tax_info,
                synced = 1
            )
        }
        taxCodeDao.insertAll(roomEntities)
    }

    suspend fun insertTaxInfos(taxInfos: List<RoomTaxInfoEntity>) {
        // Convert SQLDelight entities to Room entities
        val roomEntities = taxInfos.map { sqlEntity ->
            RoomTaxInfoEntity(
                id = sqlEntity.id,
                name = sqlEntity.name,
                formatted_name = sqlEntity.formatted_name,
                tax_spec = sqlEntity.tax_spec,
                percentage = sqlEntity.percentage,
                synced = 1
            )
        }
        taxInfoDao.insertAll(roomEntities)
    }

    suspend fun updateTaxInfo(taxInfo: TaxInfoEntity) {
        val roomEntity = RoomTaxInfoEntity(
            id = taxInfo.id,
            name = taxInfo.name,
            formatted_name = taxInfo.formatted_name,
            tax_spec = taxInfo.tax_spec,
            percentage = taxInfo.percentage,
            synced = 0
        )
        taxInfoDao.insert(roomEntity)
    }

    suspend fun updateTaxCode(taxCode: RoomTaxCodeEntity) {
        val roomEntity = RoomTaxCodeEntity(
            id = taxCode.id,
            code = taxCode.code,
            type = taxCode.type,
            description = taxCode.description,
            effective_from = taxCode.effective_from,
            tax_info = taxCode.tax_info,
            synced = 0
        )
        taxCodeDao.insert(roomEntity)
    }

    suspend fun getTaxCodeById(id: String): RoomTaxCodeEntity? {
        return taxCodeDao.taxCodeById(id)?.let { roomEntity ->
            TaxCodeEntity(
                id = roomEntity.id,
                code = roomEntity.code,
                type = roomEntity.type,
                description = roomEntity.description,
                effective_from = roomEntity.effective_from,
                tax_info = roomEntity.tax_info,
                active = roomEntity.active,
                soft_deleted = roomEntity.soft_deleted,
                synced = roomEntity.synced,
                seq_id = roomEntity.seq_id
            )
        }
    }

    suspend fun getTaxInfos(): List<TaxInfoEntity> {
        return taxInfoDao.taxInfos().map { roomEntity ->
            TaxInfoEntity(
                id = roomEntity.id,
                name = roomEntity.name,
                formatted_name = roomEntity.formatted_name,
                tax_spec = roomEntity.tax_spec,
                percentage = roomEntity.percentage,
                active = roomEntity.active,
                soft_deleted = roomEntity.soft_deleted,
                synced = roomEntity.synced,
                seq_id = roomEntity.seq_id
            )
        }
    }

    suspend fun getTaxCodes(): List<TaxCodeEntity> {
        return taxCodeDao.taxCodes().map { roomEntity ->
            TaxCodeEntity(
                id = roomEntity.id,
                code = roomEntity.code,
                type = roomEntity.type,
                description = roomEntity.description,
                effective_from = roomEntity.effective_from,
                tax_info = roomEntity.tax_info,
                active = roomEntity.active,
                soft_deleted = roomEntity.soft_deleted,
                synced = roomEntity.synced,
                seq_id = roomEntity.seq_id
            )
        }
    }

    suspend fun getTaxInfo(id: String): TaxInfoEntity? {
        return taxInfoDao.taxInfoById(id)?.let { roomEntity ->
            TaxInfoEntity(
                id = roomEntity.id,
                name = roomEntity.name,
                formatted_name = roomEntity.formatted_name,
                tax_spec = roomEntity.tax_spec,
                percentage = roomEntity.percentage,
                active = roomEntity.active,
                soft_deleted = roomEntity.soft_deleted,
                synced = roomEntity.synced,
                seq_id = roomEntity.seq_id
            )
        }
    }

    fun getTaxCodePaging(searchText: String): PagingSource<Int, TaxCodeEntity> {
        // Note: This would need a mapper to convert Room entities to SQLDelight entities
        // For now, we'll need to update the paging source later
        return if (searchText.isNotEmpty()) {
            taxCodeDao.getTaxCodesPagingSource(searchText) as PagingSource<Int, TaxCodeEntity>
        } else {
            taxCodeDao.getAllTaxCodesPagingSource() as PagingSource<Int, TaxCodeEntity>
        }
    }

    fun getTaxInfoPaging(searchText: String): PagingSource<Int, TaxInfoEntity> {
        // Note: This would need a mapper to convert Room entities to SQLDelight entities
        // For now, we'll need to update the paging source later
        return if (searchText.isNotEmpty()) {
            taxInfoDao.getTaxInfosPagingSource(searchText) as PagingSource<Int, TaxInfoEntity>
        } else {
            taxInfoDao.getAllTaxInfosPagingSource() as PagingSource<Int, TaxInfoEntity>
        }
    }

    suspend fun getUnSyncedTaxInfos(): List<TaxInfoEntity> {
        return taxInfoDao.unSyncedTaxInfos().map { roomEntity ->
            TaxInfoEntity(
                id = roomEntity.id,
                name = roomEntity.name,
                formatted_name = roomEntity.formatted_name,
                tax_spec = roomEntity.tax_spec,
                percentage = roomEntity.percentage,
                active = roomEntity.active,
                soft_deleted = roomEntity.soft_deleted,
                synced = roomEntity.synced,
                seq_id = roomEntity.seq_id
            )
        }
    }

    suspend fun getUnSyncedTaxCodes(): List<TaxCodeEntity> {
        return taxCodeDao.unSyncedTaxCodes().map { roomEntity ->
            TaxCodeEntity(
                id = roomEntity.id,
                code = roomEntity.code,
                type = roomEntity.type,
                description = roomEntity.description,
                effective_from = roomEntity.effective_from,
                tax_info = roomEntity.tax_info,
                active = roomEntity.active,
                soft_deleted = roomEntity.soft_deleted,
                synced = roomEntity.synced,
                seq_id = roomEntity.seq_id
            )
        }
    }
}