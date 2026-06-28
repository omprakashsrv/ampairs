package com.ampairs.notification.credential

import com.ampairs.notification.provider.NotificationChannel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WorkspaceChannelCredentialResolverTest {

    private val repo: WorkspaceChannelCredentialRepository = mock()
    private val crypto = CredentialCryptoService("k")
    private val resolver = WorkspaceChannelCredentialResolver(repo, crypto)

    @Test
    fun `client-owned credential resolves to CLIENT_OWN with decrypted secret`() {
        val cred = WorkspaceChannelCredential().apply {
            uid = "WCC1"; channel = NotificationChannel.WHATSAPP; provider = "META_CLOUD"
            senderRef = "phone-id-1"; secretCiphertext = crypto.encrypt("tok"); status = "VALID"
        }
        whenever(repo.findFirstByChannelAndActiveTrueOrderByUpdatedAtDesc(NotificationChannel.WHATSAPP)).thenReturn(cred)

        val resolved = resolver.resolve(NotificationChannel.WHATSAPP)
        assertEquals("CLIENT_OWN", resolved.billingMode)
        assertEquals("WCC1", resolved.credentialUid)
        assertEquals("phone-id-1", resolved.providerAccountRef)
        assertEquals("tok", resolved.secret)
    }

    @Test
    fun `WhatsApp with no credential throws NoCredentialException (no platform fallback)`() {
        whenever(repo.findFirstByChannelAndActiveTrueOrderByUpdatedAtDesc(NotificationChannel.WHATSAPP)).thenReturn(null)
        assertThrows(NoCredentialException::class.java) { resolver.resolve(NotificationChannel.WHATSAPP) }
    }

    @Test
    fun `email with no credential falls back to PLATFORM`() {
        whenever(repo.findFirstByChannelAndActiveTrueOrderByUpdatedAtDesc(NotificationChannel.EMAIL)).thenReturn(null)
        val resolved = resolver.resolve(NotificationChannel.EMAIL)
        assertEquals("PLATFORM", resolved.billingMode)
        assertNull(resolved.credentialUid)
    }

    @Test
    fun `invalid credential is ignored - email falls back to PLATFORM`() {
        val cred = WorkspaceChannelCredential().apply {
            uid = "WCC2"; channel = NotificationChannel.EMAIL; status = "INVALID"
        }
        whenever(repo.findFirstByChannelAndActiveTrueOrderByUpdatedAtDesc(NotificationChannel.EMAIL)).thenReturn(cred)
        assertEquals("PLATFORM", resolver.resolve(NotificationChannel.EMAIL).billingMode)
    }
}
