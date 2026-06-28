package com.ampairs.communication.service.consent

import com.ampairs.communication.domain.enums.Channel
import com.ampairs.communication.domain.enums.SuppressionReason
import com.ampairs.communication.domain.model.CommunicationSuppression
import com.ampairs.communication.repository.CommunicationSuppressionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** Address-level suppression checks/writes (hard bounce, complaint, unsubscribe). */
@Service
class SuppressionService(
    private val repository: CommunicationSuppressionRepository,
) {
    @Transactional(readOnly = true)
    fun isSuppressed(channel: Channel, address: String): Boolean =
        repository.findByChannelAndAddressAndActiveTrue(channel.name, address) != null

    @Transactional
    fun suppress(channel: Channel, address: String, reason: SuppressionReason) {
        if (address.isBlank()) return
        if (repository.findByChannelAndAddressAndActiveTrue(channel.name, address) != null) return
        repository.save(CommunicationSuppression().apply {
            this.channel = channel.name
            this.address = address
            this.reason = reason.name
        })
    }
}
