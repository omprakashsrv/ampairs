package com.ampairs.communication.service.consent

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.eq

class UnsubscribeServiceTest {

    private val preferenceService: PreferenceService = mock()
    private val service = UnsubscribeService(preferenceService, "test-secret")

    @Test
    fun `a generated token round-trips and applies the opt-out`() {
        val token = service.generateToken("WS1", "CUS1", "EMAIL")
        assertTrue(service.process(token))
        verify(preferenceService).optOut(eq("CUS1"), eq("EMAIL"), eq("UNSUBSCRIBE_LINK"))
    }

    @Test
    fun `a tampered token is rejected`() {
        val token = service.generateToken("WS1", "CUS1", "EMAIL")
        val tampered = token.dropLast(3) + "xyz"
        assertFalse(service.process(tampered))
    }

    @Test
    fun `a malformed token is rejected`() {
        assertFalse(service.process("not-a-valid-token"))
    }
}
