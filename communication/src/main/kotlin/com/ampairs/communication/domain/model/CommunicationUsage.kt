package com.ampairs.communication.domain.model

import com.ampairs.communication.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * Append-only billing ledger — exactly one row per message that reaches SENT/DELIVERED (unique on
 * communication_log_uid). `billingMode` decides who pays: PLATFORM (bill the client) vs CLIENT_OWN
 * (the client's own provider cost).
 */
@Entity(name = "communication_usage")
@Table(indexes = [Index(name = "idx_usage_owner", columnList = "owner_id")])
class CommunicationUsage : OwnableBaseDomain() {

    @Column(name = "communication_log_uid", length = 200, nullable = false)
    var communicationLogUid: String = ""

    @Column(name = "channel", length = 20, nullable = false)
    var channel: String = "EMAIL"

    @Column(name = "credential_uid", length = 200)
    var credentialUid: String? = null

    @Column(name = "provider_account_ref", length = 200)
    var providerAccountRef: String? = null

    @Column(name = "billing_mode", length = 20, nullable = false)
    var billingMode: String = "PLATFORM"

    @Column(name = "provider_message_id", length = 255)
    var providerMessageId: String? = null

    @Column(name = "cost_units", nullable = false)
    var costUnits: Int = 1

    @Column(name = "cost_category", length = 40)
    var costCategory: String? = null

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant = Instant.now()

    override fun obtainSeqIdPrefix(): String = Constants.USAGE_PREFIX
}
