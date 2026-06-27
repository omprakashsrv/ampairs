package com.ampairs.communication.domain.model

import com.ampairs.communication.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * One logical send. Fans out into per-recipient/per-channel [CommunicationLog] rows. `dedupKey`
 * (event id, or `schedule_uid:occurrence_key`) gives idempotency via the unique (owner_id, dedup_key).
 */
@Entity(name = "communication_request")
@Table(indexes = [Index(name = "idx_request_owner", columnList = "owner_id")])
class CommunicationRequest : OwnableBaseDomain() {

    @Column(name = "template_uid", length = 200)
    var templateUid: String? = null

    @Column(name = "trigger_type", length = 20, nullable = false)
    var triggerType: String = "MANUAL"

    @Column(name = "source_ref", length = 200)
    var sourceRef: String? = null

    @Column(name = "channels", length = 120, nullable = false)
    var channels: String = ""

    @Column(name = "audience_type", length = 20, nullable = false)
    var audienceType: String = "SINGLE"

    @Column(name = "audience_ref", length = 200)
    var audienceRef: String? = null

    @Column(name = "variables_json", columnDefinition = "TEXT")
    var variablesJson: String? = null

    @Column(name = "dedup_key", length = 255)
    var dedupKey: String? = null

    @Column(name = "status", length = 20, nullable = false)
    var status: String = "QUEUED"

    override fun obtainSeqIdPrefix(): String = Constants.REQUEST_PREFIX
}
