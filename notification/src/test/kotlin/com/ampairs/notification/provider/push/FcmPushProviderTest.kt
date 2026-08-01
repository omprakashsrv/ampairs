package com.ampairs.notification.provider.push

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider

class FcmPushProviderTest {

    private val messaging: FirebaseMessaging = mock()
    private val messagingProvider: ObjectProvider<FirebaseMessaging> = mock()
    private val provider = FcmPushProvider(messagingProvider)

    @Test
    fun `sendToTopic broadcasts and reports the message id when FCM is available`() {
        whenever(messagingProvider.ifAvailable).thenReturn(messaging)
        whenever(messaging.send(any<Message>())).thenReturn("projects/x/messages/42")

        val result = provider.sendToTopic("announcements", "Title", "Body", mapOf("deep_link" to "a://b"))

        assertTrue(result.success)
        assertEquals("projects/x/messages/42", result.messageId)
        assertEquals("FCM", result.providerName)
    }

    @Test
    fun `sendToTopic fails gracefully when FCM is not configured`() {
        whenever(messagingProvider.ifAvailable).thenReturn(null)

        val result = provider.sendToTopic("announcements", "Title", "Body")

        assertFalse(result.success)
        assertFalse(provider.isAvailable())
    }
}
