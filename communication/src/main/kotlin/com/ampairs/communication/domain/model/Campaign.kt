package com.ampairs.communication.domain.model

import com.ampairs.communication.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * A promotional campaign: bulk send to an audience on one channel, gated by consent + quiet hours +
 * throttle. Lifecycle: DRAFT → SCHEDULED → RUNNING → (PAUSED ↔ RUNNING) → DONE. Rollup counts are
 * derived from the per-recipient logs (request.source_ref = campaign.uid).
 */
@Entity(name = "campaign")
@Table(indexes = [Index(name = "idx_campaign_owner", columnList = "owner_id")])
class Campaign : OwnableBaseDomain() {

    @Column(name = "name", length = 200, nullable = false)
    var name: String = ""

    @Column(name = "template_uid", length = 200, nullable = false)
    var templateUid: String = ""

    @Column(name = "channel", length = 20, nullable = false)
    var channel: String = "EMAIL"

    @Column(name = "audience_type", length = 20, nullable = false)
    var audienceType: String = "SEGMENT"

    @Column(name = "audience_ref", length = 200)
    var audienceRef: String? = null

    @Column(name = "variables_json", columnDefinition = "TEXT")
    var variablesJson: String? = null

    @Column(name = "status", length = 20, nullable = false)
    var status: String = "DRAFT"

    @Column(name = "scheduled_at")
    var scheduledAt: Instant? = null

    @Column(name = "throttle_per_minute")
    var throttlePerMinute: Int? = null

    @Column(name = "started_at")
    var startedAt: Instant? = null

    @Column(name = "completed_at")
    var completedAt: Instant? = null

    @Column(name = "targeted_count", nullable = false)
    var targetedCount: Int = 0

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.CAMPAIGN_PREFIX
}
