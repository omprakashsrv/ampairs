package com.ampairs.communication.domain.model

import com.ampairs.communication.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * One row per workspace. Quiet-hours wall-clock is in the workspace business timezone and may span
 * midnight (start > end). `promotionalFooterHtml` is appended to promotional email at render time.
 */
@Entity(name = "communication_config")
@Table
class CommunicationConfig : OwnableBaseDomain() {

    @Column(name = "quiet_hours_start", length = 5)
    var quietHoursStart: String? = null

    @Column(name = "quiet_hours_end", length = 5)
    var quietHoursEnd: String? = null

    @Column(name = "default_throttle_per_minute", nullable = false)
    var defaultThrottlePerMinute: Int = 60

    @Column(name = "promotional_footer_html", columnDefinition = "TEXT")
    var promotionalFooterHtml: String? = null

    @Column(name = "unsubscribe_base_url", length = 500)
    var unsubscribeBaseUrl: String? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.CONFIG_PREFIX
}
