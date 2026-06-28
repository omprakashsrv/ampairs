package com.ampairs.notification.credential

import com.ampairs.notification.provider.NotificationChannel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Outcome of resolving which credential a send should use. `secret` is decrypted, in-memory only. */
data class ResolvedCredential(
    val credentialUid: String?,
    val provider: String?,
    val providerAccountRef: String?,
    val secret: String?,
    val configJson: String?,
    val billingMode: String, // CLIENT_OWN | PLATFORM
)

/** Thrown when a client-owned-sender channel (e.g. WhatsApp) has no valid workspace credential. */
class NoCredentialException(val channel: NotificationChannel) :
    RuntimeException("No workspace credential for client-owned channel $channel")

/**
 * Resolves the credential for a channel in the current tenant context:
 * - a valid workspace credential → CLIENT_OWN (the client's own sender + secret);
 * - none, channel allows platform fallback → PLATFORM (the shared platform config is used);
 * - none, channel is client-owned-only (WhatsApp) → [NoCredentialException] (→ NO_CREDENTIAL skip).
 *
 * Must be called within the target workspace's tenant context (@TenantId filters the lookup).
 */
@Service
class WorkspaceChannelCredentialResolver(
    private val repository: WorkspaceChannelCredentialRepository,
    private val crypto: CredentialCryptoService,
) {
    private val logger = LoggerFactory.getLogger(WorkspaceChannelCredentialResolver::class.java)

    /** Channels that must send from the client's own credential — no platform fallback. */
    private val clientOwnedOnly = setOf(NotificationChannel.WHATSAPP)

    @Transactional(readOnly = true)
    fun resolve(channel: NotificationChannel): ResolvedCredential {
        val credential = repository.findFirstByChannelAndActiveTrueOrderByUpdatedAtDesc(channel)
            ?.takeIf { it.status != "INVALID" && it.status != "EXPIRED" }

        if (credential != null) {
            val secret = credential.secretCiphertext?.let { runCatching { crypto.decrypt(it) }.getOrNull() }
            return ResolvedCredential(
                credentialUid = credential.uid,
                provider = credential.provider,
                providerAccountRef = credential.senderRef,
                secret = secret,
                configJson = credential.configJson,
                billingMode = "CLIENT_OWN",
            )
        }

        if (channel in clientOwnedOnly) {
            logger.warn("No workspace credential for client-owned channel {}; refusing platform fallback", channel)
            throw NoCredentialException(channel)
        }

        // Platform/shared credential — billable to the client.
        return ResolvedCredential(
            credentialUid = null, provider = null, providerAccountRef = null,
            secret = null, configJson = null, billingMode = "PLATFORM",
        )
    }
}
