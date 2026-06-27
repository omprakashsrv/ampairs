package com.ampairs.communication.domain.model

import com.ampairs.communication.config.Constants
import com.ampairs.core.domain.model.OwnableBaseDomain
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * Durable per-recipient, per-channel delivery record. `status` is monotonic (never regresses).
 * Carries the credential/provider-account/billing-mode attribution used by the usage ledger.
 */
@Entity(name = "communication_log")
@Table(
    indexes = [
        Index(name = "idx_log_owner", columnList = "owner_id"),
        Index(name = "idx_log_request", columnList = "request_uid"),
        Index(name = "idx_log_notification", columnList = "notification_uid"),
        Index(name = "idx_log_customer_channel", columnList = "customer_uid,channel"),
    ]
)
class CommunicationLog : OwnableBaseDomain() {

    @Column(name = "request_uid", length = 200, nullable = false)
    var requestUid: String = ""

    @Column(name = "customer_uid", length = 200)
    var customerUid: String? = null

    @Column(name = "channel", length = 20, nullable = false)
    var channel: String = "EMAIL"

    @Column(name = "recipient_address", length = 320, nullable = false)
    var recipientAddress: String = ""

    @Column(name = "category", length = 20, nullable = false)
    var category: String = "TRANSACTIONAL"

    @Column(name = "status", length = 20, nullable = false)
    var status: String = "QUEUED"

    @Column(name = "skip_reason", length = 30)
    var skipReason: String? = null

    @Column(name = "notification_uid", length = 200)
    var notificationUid: String? = null

    @Column(name = "provider_message_id", length = 255)
    var providerMessageId: String? = null

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null

    @Column(name = "occurrence_key", length = 64)
    var occurrenceKey: String? = null

    @Column(name = "credential_uid", length = 200)
    var credentialUid: String? = null

    @Column(name = "provider_account_ref", length = 200)
    var providerAccountRef: String? = null

    @Column(name = "billing_mode", length = 20)
    var billingMode: String? = null

    @Column(name = "sent_at")
    var sentAt: Instant? = null

    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null

    override fun obtainSeqIdPrefix(): String = Constants.LOG_PREFIX
}
