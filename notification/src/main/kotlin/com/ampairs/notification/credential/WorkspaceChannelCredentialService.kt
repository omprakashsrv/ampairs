package com.ampairs.notification.credential

import com.ampairs.notification.provider.NotificationChannel
import jakarta.validation.constraints.NotBlank

/** Write payload. `secret` is accepted but NEVER returned. */
data class CredentialRequest(
    val uid: String? = null,
    @field:NotBlank val channel: String,
    @field:NotBlank val provider: String,
    @field:NotBlank val senderRef: String,
    val displayName: String? = null,
    val secret: String? = null,
    val configJson: String? = null,
    val allowPlatformFallback: Boolean = false,
)

/** Masked read payload — contains NO secret, only a last-4 hint. */
data class CredentialResponse(
    val uid: String,
    val channel: String,
    val provider: String,
    val senderRef: String,
    val displayName: String?,
    val secretLast4: String?,
    val configJson: String?,
    val allowPlatformFallback: Boolean,
    val status: String,
    val lastValidatedAt: String?,
    val active: Boolean,
)

/** Public CRUD + validate for workspace provider credentials. Secrets are write-only. */
interface WorkspaceChannelCredentialService {
    fun list(): List<CredentialResponse>
    fun upsert(request: CredentialRequest): CredentialResponse
    fun delete(uid: String)
    fun validate(uid: String): CredentialResponse
}

internal fun WorkspaceChannelCredential.asMaskedResponse() = CredentialResponse(
    uid = uid,
    channel = channel.name,
    provider = provider,
    senderRef = senderRef,
    displayName = displayName,
    secretLast4 = secretLast4,
    configJson = configJson,
    allowPlatformFallback = allowPlatformFallback,
    status = status,
    lastValidatedAt = lastValidatedAt?.toString(),
    active = active,
)

internal fun parseChannel(value: String): NotificationChannel =
    runCatching { NotificationChannel.valueOf(value.uppercase()) }
        .getOrElse { throw IllegalArgumentException("Unknown channel '$value'") }
