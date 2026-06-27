package com.ampairs.notification.credential

import com.ampairs.core.domain.model.OwnableBaseDomain
import com.ampairs.notification.provider.NotificationChannel
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * A workspace's own provider credential for a channel — the client's sender identity (e.g. their
 * WhatsApp `phone_number_id` + access token, their email sender/SMTP). The secret is stored as
 * AES-GCM ciphertext and is NEVER returned by the API (write-only / masked). Not synced to the app.
 */
@Entity(name = "workspace_channel_credential")
@Table(indexes = [Index(name = "idx_wcc_owner", columnList = "owner_id")])
class WorkspaceChannelCredential : OwnableBaseDomain() {

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 20, nullable = false)
    var channel: NotificationChannel = NotificationChannel.EMAIL

    /** Provider key, e.g. META_CLOUD, MSG91, SES, SMTP. */
    @Column(name = "provider", length = 40, nullable = false)
    var provider: String = ""

    /** Client sender identity — WhatsApp phone_number_id / from-domain / SMS sender id (non-secret). */
    @Column(name = "sender_ref", length = 200, nullable = false)
    var senderRef: String = ""

    @Column(name = "display_name", length = 200)
    var displayName: String? = null

    /** AES-GCM ciphertext of the token/password/key. NEVER returned by any API. */
    @Column(name = "secret_ciphertext", columnDefinition = "TEXT")
    var secretCiphertext: String? = null

    /** Masked hint for the UI (last 4 chars), non-secret. */
    @Column(name = "secret_last4", length = 8)
    var secretLast4: String? = null

    /** Non-secret extra config (region, api url, etc.). */
    @Column(name = "config_json", columnDefinition = "TEXT")
    var configJson: String? = null

    /** Per-credential policy: may the platform/shared credential be used when this is absent? */
    @Column(name = "allow_platform_fallback", nullable = false)
    var allowPlatformFallback: Boolean = false

    /** UNVERIFIED / VALID / INVALID / EXPIRED. */
    @Column(name = "status", length = 20, nullable = false)
    var status: String = "UNVERIFIED"

    @Column(name = "last_validated_at")
    var lastValidatedAt: Instant? = null

    @Column(name = "active", nullable = false)
    var active: Boolean = true

    override fun obtainSeqIdPrefix(): String = "WCC"
}
