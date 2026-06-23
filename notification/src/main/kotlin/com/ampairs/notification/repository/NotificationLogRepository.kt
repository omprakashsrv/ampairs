package com.ampairs.notification.repository

import com.ampairs.notification.model.NotificationLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface NotificationLogRepository : CrudRepository<NotificationLog, Long> {

    fun findByUid(uid: String?): NotificationLog?

    /**
     * Incremental sync feed for a user: rows updated at/after lastSync that are either workspace-wide
     * (target_user_id IS NULL) or addressed to this user. INCLUDES inactive (dismissed) rows so
     * dismissals propagate. `@TenantId` auto-filters by current workspace.
     */
    @Query(
        """
        SELECT n FROM notification_log n
        WHERE n.updatedAt >= :lastSync
        AND (n.targetUserId IS NULL OR n.targetUserId = :userId)
        """
    )
    fun findForUserAfter(
        @Param("lastSync") lastSync: Instant,
        @Param("userId") userId: String,
        pageable: Pageable,
    ): Page<NotificationLog>

    /**
     * Full feed for a user (no checkpoint), INCLUDING inactive rows. `@TenantId` auto-filters by workspace.
     */
    @Query(
        """
        SELECT n FROM notification_log n
        WHERE (n.targetUserId IS NULL OR n.targetUserId = :userId)
        """
    )
    fun findAllForUser(@Param("userId") userId: String, pageable: Pageable): Page<NotificationLog>
}
