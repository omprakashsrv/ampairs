package com.ampairs.communication.domain.model

import com.ampairs.communication.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * Address-level block list. A hard bounce / complaint / unsubscribe suppresses all future sends to
 * `(channel, address)` — hard bounces suppress even transactional (FR-031). Unique
 * `(owner_id, channel, address)`.
 */
@Entity(name = "communication_suppression")
@Table
class CommunicationSuppression : OwnableBaseDomain() {

    @Column(name = "channel", length = 20, nullable = false)
    var channel: String = "EMAIL"

    @Column(name = "address", length = 320, nullable = false)
    var address: String = ""

    @Column(name = "reason", length = 20, nullable = false)
    var reason: String = "UNSUBSCRIBE"

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.SUPPRESSION_PREFIX
}
