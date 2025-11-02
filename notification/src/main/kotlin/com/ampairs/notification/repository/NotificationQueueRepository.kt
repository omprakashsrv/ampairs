package com.ampairs.notification.repository

import com.ampairs.notification.model.NotificationQueue
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.provider.NotificationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * Repository for Notification Queue operations
 */
@Repository
interface NotificationQueueRepository : JpaRepository<NotificationQueue, String> {

    /**
     * Find pending notifications ready to be sent
     */
    @Query(
        """
        SELECT n FROM NotificationQueue n 
        WHERE n.status IN (:statuses) 
        AND n.scheduledAt <= :currentTime 
        ORDER BY n.createdAt ASC
    """
    )
    fun findPendingNotifications(
        @Param("statuses") statuses: List<NotificationStatus> = listOf(
            NotificationStatus.PENDING,
            NotificationStatus.RETRYING
        ),
        @Param("currentTime") currentTime: LocalDateTime = LocalDateTime.now(),
    ): List<NotificationQueue>

    /**
     * Find pending notifications by channel
     */
    @Query(
        """
        SELECT n FROM NotificationQueue n 
        WHERE n.status IN (:statuses) 
        AND n.channel = :channel 
        AND n.scheduledAt <= :currentTime 
        ORDER BY n.createdAt ASC
    """
    )
    fun findPendingNotificationsByChannel(
        @Param("channel") channel: NotificationChannel,
        @Param("statuses") statuses: List<NotificationStatus> = listOf(
            NotificationStatus.PENDING,
            NotificationStatus.RETRYING
        ),
        @Param("currentTime") currentTime: LocalDateTime = LocalDateTime.now(),
    ): List<NotificationQueue>

    /**
     * Find notifications that failed and can be retried
     */
    @Query(
        """
        SELECT n FROM NotificationQueue n 
        WHERE n.status = :status 
        AND n.retryCount < n.maxRetries 
        AND n.scheduledAt <= :currentTime 
        ORDER BY n.createdAt ASC
    """
    )
    fun findFailedNotificationsForRetry(
        @Param("status") status: NotificationStatus = NotificationStatus.FAILED,
        @Param("currentTime") currentTime: LocalDateTime = LocalDateTime.now(),
    ): List<NotificationQueue>

    /**
     * Count notifications by status
     */
    fun countByStatusIn(statuses: List<NotificationStatus>): Long

    /**
     * Count notifications by status and channel
     */
    fun countByStatusInAndChannel(statuses: List<NotificationStatus>, channel: NotificationChannel): Long

    /**
     * Find notifications by recipient and status
     */
    fun findByRecipientAndStatusOrderByCreatedAtDesc(
        recipient: String,
        status: NotificationStatus,
    ): List<NotificationQueue>

    /**
     * Find notifications by channel and status
     */
    fun findByChannelAndStatusOrderByCreatedAtDesc(
        channel: NotificationChannel,
        status: NotificationStatus,
    ): List<NotificationQueue>

    /**
     * Delete old notification records (for cleanup)
     */
    @Query("DELETE FROM NotificationQueue n WHERE n.createdAt < :cutoffDate")
    fun deleteOldRecords(@Param("cutoffDate") cutoffDate: LocalDateTime)

    /**
     * Find notifications older than specified days for cleanup
     */
    @Query(
        """
        SELECT n FROM NotificationQueue n 
        WHERE n.createdAt < :cutoffDate 
        AND n.status IN ('SENT', 'EXHAUSTED')
    """
    )
    fun findOldCompletedNotifications(@Param("cutoffDate") cutoffDate: LocalDateTime): List<NotificationQueue>
}

/**
 * Legacy SMS Queue Repository alias for backward compatibility
 */
typealias SmsQueueRepository = NotificationQueueRepository