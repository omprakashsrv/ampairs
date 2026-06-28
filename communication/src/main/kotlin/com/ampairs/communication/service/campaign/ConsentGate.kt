package com.ampairs.communication.service.campaign

import com.ampairs.communication.domain.enums.Channel
import com.ampairs.communication.domain.enums.MessageCategory
import com.ampairs.communication.port.Recipient
import com.ampairs.communication.repository.CommunicationPreferenceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Promotional consent gate. A recipient is excluded when they have an explicit opt-out preference for
 * the channel + PROMOTIONAL category. Transactional sends never pass through this gate (FR-016).
 */
@Service
class ConsentGate(
    private val preferenceRepository: CommunicationPreferenceRepository,
) {
    /** True if the customer may receive a promotional message on this channel. */
    @Transactional(readOnly = true)
    fun isAllowed(recipient: Recipient, channel: Channel): Boolean {
        val customerUid = recipient.customerUid ?: return true // ad-hoc recipient → no stored preference
        val pref = preferenceRepository.findByCustomerUidAndChannelAndCategory(
            customerUid, channel.name, MessageCategory.PROMOTIONAL.name,
        )
        return pref?.optedIn ?: true // default allowed unless explicitly opted out
    }
}
