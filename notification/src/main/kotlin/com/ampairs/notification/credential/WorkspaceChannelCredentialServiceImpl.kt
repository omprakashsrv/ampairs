package com.ampairs.notification.credential

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Default credential service. Secrets are AES-GCM encrypted on write via [CredentialCryptoService]
 * and never returned (responses are masked). Decryption happens only on the send path (the resolver).
 */
@Service
class WorkspaceChannelCredentialServiceImpl(
    private val repository: WorkspaceChannelCredentialRepository,
    private val crypto: CredentialCryptoService,
) : WorkspaceChannelCredentialService {

    private val logger = LoggerFactory.getLogger(WorkspaceChannelCredentialServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun list(): List<CredentialResponse> = repository.findAllActive().map { it.asMaskedResponse() }

    @Transactional
    override fun upsert(request: CredentialRequest): CredentialResponse {
        val channel = parseChannel(request.channel)
        val existing = request.uid?.takeIf { it.isNotBlank() }?.let { repository.findByUid(it) }
            ?: repository.findByChannelAndProvider(channel, request.provider)

        val entity = (existing ?: WorkspaceChannelCredential()).apply {
            this.channel = channel
            provider = request.provider.trim()
            senderRef = request.senderRef.trim()
            displayName = request.displayName
            configJson = request.configJson
            allowPlatformFallback = request.allowPlatformFallback
            active = true
            // Only (re)write the secret when a new one is supplied; otherwise keep the stored cipher.
            if (!request.secret.isNullOrBlank()) {
                check(crypto.isConfigured) { "Cannot store credential secret — encryption key not configured" }
                secretCiphertext = crypto.encrypt(request.secret)
                secretLast4 = request.secret.takeLast(4)
                status = "UNVERIFIED"
            }
        }
        return repository.save(entity).asMaskedResponse()
    }

    @Transactional
    override fun delete(uid: String) {
        repository.findByUid(uid)?.let {
            it.active = false
            repository.save(it)
        }
    }

    @Transactional
    override fun validate(uid: String): CredentialResponse {
        val entity = repository.findByUid(uid) ?: throw IllegalArgumentException("No credential '$uid'")
        // Provider-side probe is provider-specific; for now mark VALID if a secret is present.
        // (A real probe — e.g. a WhatsApp token check — is layered per provider.)
        entity.status = if (entity.secretCiphertext.isNullOrBlank()) "INVALID" else "VALID"
        entity.lastValidatedAt = Instant.now()
        return repository.save(entity).asMaskedResponse()
    }
}
