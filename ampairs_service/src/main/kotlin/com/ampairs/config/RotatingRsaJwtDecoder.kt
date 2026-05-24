package com.ampairs.config

import com.ampairs.auth.service.RsaKeyManager
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.ConcurrentHashMap

/**
 * JWT decoder that supports RSA key rotation.
 *
 * The default NimbusJwtDecoder bean is created once at startup with the key that exists
 * at that moment. After a key rotation, tokens signed with the new key fail to verify
 * because the decoder still uses the old public key.
 *
 * This decoder extracts the `kid` (key ID) from each token's header and looks up the
 * matching public key via RsaKeyManager, which keeps a history of all valid keys.
 * NimbusJwtDecoder instances are cached by keyId to avoid re-creation on every request.
 */
class RotatingRsaJwtDecoder(private val rsaKeyManager: RsaKeyManager) : JwtDecoder {

    private val decoderCache = ConcurrentHashMap<String, JwtDecoder>()

    override fun decode(token: String): Jwt {
        val kid = extractKid(token)

        val publicKey: RSAPublicKey = if (kid != null) {
            rsaKeyManager.getPublicKey(kid)
                ?: throw JwtException("No public key found for key ID: $kid")
        } else {
            rsaKeyManager.getCurrentKeyPair().publicKey
        }

        val cacheKey = kid ?: "current"
        val decoder = decoderCache.computeIfAbsent(cacheKey) {
            NimbusJwtDecoder.withPublicKey(publicKey).build()
        }

        return decoder.decode(token)
    }

    private fun extractKid(token: String): String? = try {
        val headerB64 = token.substringBefore(".")
        val headerJson = String(java.util.Base64.getUrlDecoder().decode(headerB64))
        val kidMatch = """"kid"\s*:\s*"([^"]+)"""".toRegex().find(headerJson)
        kidMatch?.groupValues?.get(1)
    } catch (_: Exception) {
        null
    }
}
