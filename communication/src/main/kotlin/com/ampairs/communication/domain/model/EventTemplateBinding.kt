package com.ampairs.communication.domain.model

import com.ampairs.communication.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * Maps a domain event type (e.g. INVOICE_CREATED) to the template + channels the transactional
 * listener fires. One binding per (workspace, event_type).
 */
@Entity(name = "event_template_binding")
@Table(indexes = [Index(name = "idx_binding_owner", columnList = "owner_id")])
class EventTemplateBinding : OwnableBaseDomain() {

    @Column(name = "event_type", length = 80, nullable = false)
    var eventType: String = ""

    @Column(name = "template_uid", length = 200, nullable = false)
    var templateUid: String = ""

    @Column(name = "channels", length = 120, nullable = false)
    var channels: String = ""

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = Constants.BINDING_PREFIX
}
