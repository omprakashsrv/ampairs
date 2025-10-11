package com.ampairs.business.data.repository

import com.ampairs.business.data.api.BusinessApi
import com.ampairs.business.data.db.BusinessDao
import com.ampairs.business.data.db.BusinessEntity
import com.ampairs.business.data.db.toDomain
import com.ampairs.business.data.db.toEntity
import com.ampairs.business.domain.Business
import com.ampairs.business.domain.toPayload
import com.ampairs.business.util.BusinessConstants
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.workspace.context.WorkspaceContextManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class BusinessRepository(
    private val businessDao: BusinessDao,
    private val businessApi: BusinessApi,
    private val workspaceContextManager: WorkspaceContextManager
) {

    fun observeBusiness(): Flow<Business?> = businessDao.observeBusiness().map { it?.toDomain() }

    suspend fun getCachedBusiness(): Business? = businessDao.getBusiness()?.toDomain()

    suspend fun saveLocal(business: Business, markSynced: Boolean) {
        val workspaceId = business.workspaceId ?: workspaceContextManager.getCurrentWorkspaceId()
        val existing = businessDao.getBusiness()

        var entity = business
            .ensureId(existing)
            .toEntity(markSynced = markSynced, workspaceId = workspaceId)

        entity = entity.copy(
            seqId = entity.seqId ?: existing?.seqId,
            workspaceId = entity.workspaceId ?: existing?.workspaceId,
            localCreatedAt = existing?.localCreatedAt ?: entity.localCreatedAt
        )

        businessDao.upsertBusiness(entity)
        if (markSynced) {
            val now = Clock.System.now().toEpochMilliseconds()
            businessDao.updateSyncStatus(entity.uid, true, now)
        }
    }

    suspend fun clearLocal() {
        businessDao.clearAll()
    }

    suspend fun fetchFromRemote(): Result<Business> {
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId()
            ?: return Result.failure(IllegalStateException("Workspace not selected"))

        val result = businessApi.getBusiness()
        result.onSuccess { remote ->
            saveLocal(remote.copy(workspaceId = workspaceId), markSynced = true)
        }
        return result
    }

    suspend fun upsertBusiness(business: Business): Result<Business> {
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId()
        val existing = businessDao.getBusiness()
        val ensuredBusiness = business.ensureId(existing)

        // Offline-first: persist immediately with synced=false
        saveLocal(ensuredBusiness.copy(workspaceId = workspaceId), markSynced = false)

        if (workspaceId.isNullOrEmpty()) {
            // No workspace context yet; rely on later sync
            return Result.success(ensuredBusiness)
        }

        val payload = ensuredBusiness.toPayload()

        val apiResult = if (existing == null || ensuredBusiness.id.startsWith(BusinessConstants.LOCAL_ID_PREFIX)) {
            businessApi.createBusiness(payload)
        } else {
            businessApi.updateBusiness(payload)
        }

        return apiResult.fold(
            onSuccess = { remote ->
                saveLocal(remote.copy(workspaceId = workspaceId), markSynced = true)
                Result.success(remote)
            },
            onFailure = {
                // Keep local unsynced entity; background sync will retry
                Result.success(ensuredBusiness)
            }
        )
    }

    suspend fun syncPending(): Result<Boolean> {
        val pending = businessDao.getPendingBusiness() ?: return Result.success(false)
        val workspaceId = workspaceContextManager.getCurrentWorkspaceId() ?: return Result.success(false)

        val domain = pending.toDomain().copy(workspaceId = workspaceId)
        val payload = domain.toPayload()
        val apiResult = if (pending.uid.startsWith(BusinessConstants.LOCAL_ID_PREFIX)) {
            businessApi.createBusiness(payload)
        } else {
            businessApi.updateBusiness(payload)
        }

        return apiResult.map { remote ->
            saveLocal(remote.copy(workspaceId = workspaceId), markSynced = true)
            true
        }
    }

    private fun Business.ensureId(existing: BusinessEntity?): Business {
        if (id.isNotBlank()) {
            return this
        }
        if (existing != null) {
            return copy(id = existing.uid)
        }
        val generatedId = UidGenerator.generateUid(BusinessConstants.UID_PREFIX)
        return copy(id = generatedId)
    }
}
