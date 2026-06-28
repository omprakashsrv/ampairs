package com.ampairs.communication.service.consent

import com.ampairs.communication.domain.dto.PreferenceRequest
import com.ampairs.communication.domain.dto.PreferenceResponse
import com.ampairs.communication.domain.dto.applyRequest
import com.ampairs.communication.domain.dto.asResponse
import com.ampairs.communication.domain.model.CommunicationPreference
import com.ampairs.communication.repository.CommunicationPreferenceRepository
import com.ampairs.core.sync.EntityChangePublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/** CRUD + `/sync` for per-customer communication preferences (consent). */
@Service
class PreferenceService(
    private val repository: CommunicationPreferenceRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {
    private val logger = LoggerFactory.getLogger(PreferenceService::class.java)

    @Transactional(readOnly = true)
    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<PreferenceResponse> {
        val page: Page<CommunicationPreference> = if (lastSync.isNullOrBlank()) {
            repository.findAllForSync(pageable)
        } else {
            try {
                repository.findByUpdatedAtAfter(
                    Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8)), pageable
                )
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', full feed", lastSync, e)
                repository.findAllForSync(pageable)
            }
        }
        return page.map { it.asResponse() }
    }

    @Transactional
    fun bulkUpsert(requests: List<PreferenceRequest>): List<PreferenceResponse> = requests.map { req ->
        val existing = repository.findByUid(req.uid)
            ?: repository.findByCustomerUidAndChannelAndCategory(
                req.customerUid, req.channel.uppercase(), req.category.uppercase()
            )
        repository.save((existing ?: CommunicationPreference()).applyRequest(req))
            .also { entityChangePublisher.updated("communication_preference", it.uid) }
            .asResponse()
    }

    /** Flip a customer's promotional preference for a channel to opted-out (used by unsubscribe). */
    @Transactional
    fun optOut(customerUid: String, channel: String, source: String) {
        val pref = repository.findByCustomerUidAndChannelAndCategory(customerUid, channel.uppercase(), "PROMOTIONAL")
            ?: CommunicationPreference().apply {
                this.customerUid = customerUid; this.channel = channel.uppercase(); category = "PROMOTIONAL"
            }
        pref.optedIn = false
        pref.source = source
        repository.save(pref)
    }
}
