package com.ampairs.cb_store.service

import com.ampairs.cb_store.domain.dto.StoreRequest
import com.ampairs.cb_store.domain.dto.StoreResponse
import com.ampairs.cb_store.domain.dto.applyRequest
import com.ampairs.cb_store.domain.dto.asStoreResponse
import com.ampairs.cb_store.domain.model.Store
import com.ampairs.cb_store.exception.StoreNotFoundException
import com.ampairs.cb_store.repository.StoreRepository
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
class StoreServiceImpl(
    private val storeRepository: StoreRepository,
    private val entityChangePublisher: EntityChangePublisher,
) : StoreService {

    private val logger = LoggerFactory.getLogger(StoreServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun findByUid(uid: String): StoreResponse? {
        if (uid.isBlank()) return null
        return storeRepository.findByUid(uid)?.asStoreResponse()
    }

    @Transactional(readOnly = true)
    override fun getByUid(uid: String): StoreResponse =
        findByUid(uid) ?: throw StoreNotFoundException("Store not found for uid: $uid")

    @Transactional(readOnly = true)
    override fun getZonalOfficeId(storeId: String): String =
        (storeRepository.findByUid(storeId)
            ?: throw StoreNotFoundException("Store not found for uid: $storeId")).zonalOfficeId

    @Transactional(readOnly = true)
    override fun findAllActive(): List<StoreResponse> =
        storeRepository.findByActiveTrueOrderByCode().map { it.asStoreResponse() }

    @Transactional(readOnly = true)
    override fun getStoresAfterSync(lastSync: String?, pageable: Pageable): Page<StoreResponse> {
        val page: Page<Store> = if (lastSync.isNullOrBlank()) {
            storeRepository.findAllForSync(pageable)
        } else {
            try {
                val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
                storeRepository.findByUpdatedAtAfter(Instant.parse(decoded), pageable)
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', falling back to full sync feed", lastSync, e)
                storeRepository.findAllForSync(pageable)
            }
        }
        return page.map { it.asStoreResponse() }
    }

    @Transactional
    override fun bulkUpsert(requests: List<StoreRequest>): List<StoreResponse> =
        requests.map { request ->
            val existing = request.uid?.takeIf { it.isNotBlank() }?.let { storeRepository.findByUid(it) }
            if (existing != null) {
                existing.applyRequest(request)
                storeRepository.save(existing)
                    .also { entityChangePublisher.updated("cb_store", it.uid) }
                    .asStoreResponse()
            } else {
                val store = Store().applyRequest(request)
                storeRepository.save(store)
                    .also { entityChangePublisher.created("cb_store", it.uid) }
                    .asStoreResponse()
            }
        }

    @Transactional
    override fun create(request: StoreRequest): StoreResponse {
        val saved = storeRepository.save(Store().applyRequest(request))
        entityChangePublisher.created("cb_store", saved.uid)
        return saved.asStoreResponse()
    }

    @Transactional
    override fun delete(uid: String) {
        val store = storeRepository.findByUid(uid)
            ?: throw StoreNotFoundException("Store not found for uid: $uid")
        store.active = false
        storeRepository.save(store)
        entityChangePublisher.deleted("cb_store", store.uid)
    }
}
