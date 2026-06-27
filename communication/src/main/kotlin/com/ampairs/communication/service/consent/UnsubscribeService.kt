package com.ampairs.communication.service.consent

import com.ampairs.core.multitenancy.TenantContextHolder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Tokenized unsubscribe. The token encodes (workspace, customer, channel) and is HMAC-signed so the
 * public endpoint can resolve the tenant and process the opt-out without a session/header. Processing
 * flips the customer's promotional preference for the channel to opted-out (FR-030).
 */
@Service
class UnsubscribeService(
    private val preferenceService: PreferenceService,
    @Value("\${communication.unsubscribe.secret:\${communication.credentials.encryption-key:ampairs-unsub}}")
    private val secret: String,
) {
    private val logger = LoggerFactory.getLogger(UnsubscribeService::class.java)
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    /** Build a signed unsubscribe token for an email/link footer. */
    fun generateToken(workspaceId: String, customerUid: String, channel: String): String {
        val payload = "$workspaceId|$customerUid|$channel"
        val b64 = encoder.encodeToString(payload.toByteArray(Charsets.UTF_8))
        return "$b64.${sign(b64)}"
    }

    /** Verify + process an unsubscribe token. Returns true if applied. */
    fun process(token: String): Boolean {
        val parts = token.split(".")
        if (parts.size != 2 || sign(parts[0]) != parts[1]) {
            logger.warn("Rejected unsubscribe token (bad signature)")
            return false
        }
        val payload = String(decoder.decode(parts[0]), Charsets.UTF_8).split("|")
        if (payload.size != 3) return false
        val (workspaceId, customerUid, channel) = payload

        val prior = TenantContextHolder.getCurrentTenant()
        TenantContextHolder.setCurrentTenant(workspaceId)
        return try {
            preferenceService.optOut(customerUid, channel, source = "UNSUBSCRIBE_LINK")
            true
        } finally {
            if (prior != null) TenantContextHolder.setCurrentTenant(prior) else TenantContextHolder.clearTenantContext()
        }
    }

    private fun sign(data: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return encoder.encodeToString(mac.doFinal(data.toByteArray(Charsets.UTF_8)))
    }
}
