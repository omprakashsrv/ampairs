package com.ampairs.cb_maintenance.service

import com.ampairs.cb_maintenance.domain.dto.AssetCategoryAliasRequest
import com.ampairs.cb_maintenance.domain.dto.AssetCategoryAliasResponse
import com.ampairs.cb_maintenance.domain.dto.applyRequest
import com.ampairs.cb_maintenance.domain.dto.asAssetCategoryAliasResponse
import com.ampairs.cb_maintenance.domain.model.AssetCategoryAlias
import com.ampairs.cb_maintenance.repository.AssetCategoryAliasRepository
import com.ampairs.core.sync.EntityChangePublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class AssetCategoryAliasService(
    private val repository: AssetCategoryAliasRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {
    private val logger = LoggerFactory.getLogger(AssetCategoryAliasService::class.java)

    /** Resolve a messy source name to its canonical asset category (falls back to the input). */
    @Transactional(readOnly = true)
    fun canonicalize(name: String): String {
        if (name.isBlank()) return name
        return repository.findByAliasLowerAndActiveTrue(name.trim().lowercase())?.canonical ?: name.trim()
    }

    @Transactional(readOnly = true)
    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<AssetCategoryAliasResponse> {
        val page: Page<AssetCategoryAlias> = if (lastSync.isNullOrBlank()) {
            repository.findAllForSync(pageable)
        } else {
            try {
                repository.findByUpdatedAtAfter(
                    Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8)), pageable,
                )
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', full feed", lastSync, e)
                repository.findAllForSync(pageable)
            }
        }
        return page.map { it.asAssetCategoryAliasResponse() }
    }

    @Transactional
    fun bulkUpsert(requests: List<AssetCategoryAliasRequest>): List<AssetCategoryAliasResponse> =
        requests.map { request ->
            val existing = request.uid?.takeIf { it.isNotBlank() }?.let { repository.findByUid(it) }
            val entity = (existing ?: AssetCategoryAlias()).applyRequest(request)
            repository.save(entity)
                .also { entityChangePublisher.updated("cb_asset_alias", it.uid) }
                .asAssetCategoryAliasResponse()
        }
}
