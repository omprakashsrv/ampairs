package com.ampairs.communication.repository

import com.ampairs.communication.domain.model.Campaign
import com.ampairs.communication.domain.model.CommunicationPreference
import com.ampairs.communication.domain.model.CommunicationSuppression
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.time.Instant

interface CampaignRepository : CrudRepository<Campaign, Long> {
    fun findByUid(uid: String?): Campaign?

    /** SCHEDULED campaigns whose scheduled time has arrived — cross-workspace (sweeper, nativeQuery). */
    @Query(
        value = "SELECT * FROM campaign WHERE active = true AND status = 'SCHEDULED' " +
            "AND scheduled_at IS NOT NULL AND scheduled_at <= :now",
        nativeQuery = true,
    )
    fun findDueScheduled(@Param("now") now: Instant): List<Campaign>

    @Query("SELECT c FROM campaign c WHERE c.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<Campaign>

    @Query("SELECT c FROM campaign c")
    fun findAllForSync(pageable: Pageable): Page<Campaign>
}

interface CommunicationPreferenceRepository : CrudRepository<CommunicationPreference, Long> {
    fun findByUid(uid: String?): CommunicationPreference?
    fun findByCustomerUidAndChannelAndCategory(
        customerUid: String, channel: String, category: String,
    ): CommunicationPreference?

    @Query("SELECT p FROM communication_preference p WHERE p.updatedAt >= :lastSync")
    fun findByUpdatedAtAfter(@Param("lastSync") lastSync: Instant, pageable: Pageable): Page<CommunicationPreference>

    @Query("SELECT p FROM communication_preference p")
    fun findAllForSync(pageable: Pageable): Page<CommunicationPreference>
}

interface CommunicationSuppressionRepository : CrudRepository<CommunicationSuppression, Long> {
    fun findByChannelAndAddressAndActiveTrue(channel: String, address: String): CommunicationSuppression?
}
