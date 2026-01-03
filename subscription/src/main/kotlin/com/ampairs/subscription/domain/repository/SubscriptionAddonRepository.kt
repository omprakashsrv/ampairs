package com.ampairs.subscription.domain.repository

import com.ampairs.subscription.domain.model.AddonModuleCode
import com.ampairs.subscription.domain.model.SubscriptionAddon
import com.ampairs.subscription.domain.model.SubscriptionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
interface SubscriptionAddonRepository : JpaRepository<SubscriptionAddon, Long> {

    fun findByUid(uid: String): SubscriptionAddon?

    fun findBySubscriptionId(subscriptionId: String): List<SubscriptionAddon>

    fun findByWorkspaceId(workspaceId: String): List<SubscriptionAddon>

    @Query("""
        SELECT a FROM SubscriptionAddon a
        WHERE a.subscriptionId = :subscriptionId
        AND a.status = 'ACTIVE'
    """)
    fun findActiveBySubscriptionId(subscriptionId: String): List<SubscriptionAddon>

    @Query("""
        SELECT a FROM SubscriptionAddon a
        WHERE a.workspaceId = :workspaceId
        AND a.status = 'ACTIVE'
    """)
    fun findActiveByWorkspaceId(workspaceId: String): List<SubscriptionAddon>

    fun findBySubscriptionIdAndAddonCode(
        subscriptionId: String,
        addonCode: AddonModuleCode
    ): SubscriptionAddon?

    fun findByWorkspaceIdAndAddonCode(
        workspaceId: String,
        addonCode: AddonModuleCode
    ): SubscriptionAddon?

    @Query("""
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM SubscriptionAddon a
        WHERE a.workspaceId = :workspaceId
        AND a.addonCode = :addonCode
        AND a.status = 'ACTIVE'
    """)
    fun hasActiveAddon(
        @Param("workspaceId") workspaceId: String,
        @Param("addonCode") addonCode: AddonModuleCode
    ): Boolean

    /**
     * Find addons expiring soon
     */
    @Query("""
        SELECT a FROM SubscriptionAddon a
        WHERE a.status = 'ACTIVE'
        AND a.expiresAt BETWEEN :from AND :to
    """)
    fun findExpiringSoon(
        @Param("from") from: Instant,
        @Param("to") to: Instant
    ): List<SubscriptionAddon>

    /**
     * Find expired addons
     */
    @Query("""
        SELECT a FROM SubscriptionAddon a
        WHERE a.status = 'ACTIVE'
        AND a.expiresAt < :now
    """)
    fun findExpired(@Param("now") now: Instant): List<SubscriptionAddon>

    @Modifying
    @Transactional
    @Query("UPDATE SubscriptionAddon a SET a.status = :status, a.updatedAt = :now WHERE a.uid = :uid")
    fun updateStatus(
        @Param("uid") uid: String,
        @Param("status") status: SubscriptionStatus,
        @Param("now") now: Instant
    )

    /**
     * Cancel all addons for a subscription
     */
    @Modifying
    @Transactional
    @Query("""
        UPDATE SubscriptionAddon a
        SET a.status = 'CANCELLED', a.cancelledAt = :now, a.updatedAt = :now
        WHERE a.subscriptionId = :subscriptionId AND a.status = 'ACTIVE'
    """)
    fun cancelAllBySubscriptionId(
        @Param("subscriptionId") subscriptionId: String,
        @Param("now") now: Instant
    )
}
