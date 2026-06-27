package com.ampairs.communication.repository

import com.ampairs.communication.domain.model.CommunicationLog
import com.ampairs.communication.domain.model.CommunicationRequest
import com.ampairs.communication.domain.model.CommunicationUsage
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface CommunicationRequestRepository : CrudRepository<CommunicationRequest, Long> {
    fun findByUid(uid: String?): CommunicationRequest?
    fun findByDedupKey(dedupKey: String): CommunicationRequest?
}

interface CommunicationLogRepository : CrudRepository<CommunicationLog, Long> {
    fun findByUid(uid: String?): CommunicationLog?
    fun findByNotificationUid(notificationUid: String): CommunicationLog?
    fun findByRequestUid(requestUid: String): List<CommunicationLog>

    /** Pull-only sync feed for delivery status. @TenantId filters by workspace. */
    @Query("SELECT l FROM communication_log l WHERE l.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<CommunicationLog>

    @Query("SELECT l FROM communication_log l")
    fun findAllForSync(pageable: Pageable): Page<CommunicationLog>
}

interface CommunicationUsageRepository : CrudRepository<CommunicationUsage, Long> {
    fun findByCommunicationLogUid(communicationLogUid: String): CommunicationUsage?

    /** Usage rows in a period for the current workspace (@TenantId-filtered) — billing report source. */
    fun findByOccurredAtBetween(from: Instant, to: Instant): List<CommunicationUsage>
}
