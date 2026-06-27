package com.ampairs.notification.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.notification.domain.dto.NotificationLogRequest
import com.ampairs.notification.domain.dto.NotificationLogResponse
import com.ampairs.notification.domain.dto.applyRequest
import com.ampairs.notification.domain.dto.asResponse
import com.ampairs.notification.model.NotificationLog
import com.ampairs.notification.repository.NotificationLogRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Read/sync surface for the in-app notification center. Rows are authored by [PushDispatchService];
 * the app pulls them through the canonical `/sync` feed and pushes back read/dismiss flags.
 */
@Service
class NotificationLogService(
    private val repository: NotificationLogRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {
    private val logger = LoggerFactory.getLogger(NotificationLogService::class.java)

    @Transactional(readOnly = true)
    fun getFeedAfterSync(lastSync: String?, userId: String, pageable: Pageable): Page<NotificationLogResponse> {
        val effectiveUserId = userId.ifBlank { "" }
        val page: Page<NotificationLog> = if (lastSync.isNullOrBlank()) {
            repository.findAllForUser(effectiveUserId, pageable)
        } else {
            try {
                val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
                repository.findForUserAfter(Instant.parse(decoded), effectiveUserId, pageable)
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', falling back to full feed", lastSync, e)
                repository.findAllForUser(effectiveUserId, pageable)
            }
        }
        return page.map { it.asResponse() }
    }

    @Transactional
    fun bulkUpsert(requests: List<NotificationLogRequest>): List<NotificationLogResponse> =
        requests.map { request ->
            val existing = request.id.takeIf { it.isNotBlank() }?.let { repository.findByUid(it) }
            if (existing != null) {
                existing.applyRequest(request)
                repository.save(existing)
                    .also { entityChangePublisher.updated("notification_log", it.uid) }
                    .asResponse()
            } else {
                // App rarely creates notifications, but honor the symmetric UID-keyed contract.
                val created = NotificationLog().apply {
                    uid = request.id
                    type = request.type
                    title = request.title
                    body = request.body
                    dataPayload = request.dataPayload
                    read = request.read
                    active = request.active
                }
                repository.save(created)
                    .also { entityChangePublisher.created("notification_log", it.uid) }
                    .asResponse()
            }
        }

    @Transactional
    fun markRead(uid: String) {
        val log = repository.findByUid(uid) ?: return
        if (!log.read) {
            log.read = true
            repository.save(log).also { entityChangePublisher.updated("notification_log", it.uid) }
        }
    }

    @Transactional
    fun markAllRead(userId: String) {
        // Pull a generous page of the user's unread feed and flip them. Sufficient for typical volumes.
        val page = repository.findAllForUser(
            userId.ifBlank { "" },
            org.springframework.data.domain.PageRequest.of(0, 500),
        )
        page.content.filter { it.read.not() && it.active }.forEach { log ->
            log.read = true
            repository.save(log).also { entityChangePublisher.updated("notification_log", it.uid) }
        }
    }
}
