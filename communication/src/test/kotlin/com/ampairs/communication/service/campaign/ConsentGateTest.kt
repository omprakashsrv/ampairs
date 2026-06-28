package com.ampairs.communication.service.campaign

import com.ampairs.communication.domain.enums.Channel
import com.ampairs.communication.domain.model.CommunicationPreference
import com.ampairs.communication.port.Recipient
import com.ampairs.communication.repository.CommunicationPreferenceRepository
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ConsentGateTest {

    private val repo: CommunicationPreferenceRepository = mock()
    private val gate = ConsentGate(repo)

    @Test
    fun `allowed by default when no preference exists`() {
        whenever(repo.findByCustomerUidAndChannelAndCategory(any(), any(), any())).thenReturn(null)
        assertTrue(gate.isAllowed(Recipient(customerUid = "CUS1", email = "a@b.com"), Channel.EMAIL))
    }

    @Test
    fun `excluded when opted out of promotional on the channel`() {
        whenever(repo.findByCustomerUidAndChannelAndCategory("CUS1", "EMAIL", "PROMOTIONAL"))
            .thenReturn(CommunicationPreference().apply { optedIn = false })
        assertFalse(gate.isAllowed(Recipient(customerUid = "CUS1", email = "a@b.com"), Channel.EMAIL))
    }

    @Test
    fun `ad-hoc recipient without a customer uid is allowed`() {
        assertTrue(gate.isAllowed(Recipient(email = "a@b.com"), Channel.EMAIL))
    }
}
