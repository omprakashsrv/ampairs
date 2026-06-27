package com.ampairs.communication.service.audience

import com.ampairs.communication.port.CustomerAudiencePort
import com.ampairs.communication.port.Recipient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Audience adapter for SINGLE/LIST audiences supplied explicitly by the caller.
 *
 * SEGMENT (customer-group) and SINGLE-by-customer-uid resolution require a public read interface on
 * the customer module (contact + group membership). That is intentionally NOT wired here yet —
 * resolving it via the customer repository would violate module boundaries (Principle IX). Until
 * the customer module exposes a `CustomerContactProvider`, segment resolution returns empty and logs
 * a warning. Tracked as a follow-up to T020 / US3 (T046).
 */
@Component
class ExplicitAudienceAdapter : CustomerAudiencePort {

    private val logger = LoggerFactory.getLogger(ExplicitAudienceAdapter::class.java)

    override fun resolve(audienceType: String, audienceRef: String?, explicit: List<Recipient>): List<Recipient> {
        return when (audienceType.uppercase()) {
            "SINGLE", "LIST" -> explicit
            "SEGMENT" -> {
                logger.warn(
                    "SEGMENT audience '{}' not yet resolvable — pending a customer-module contact provider; returning no recipients",
                    audienceRef
                )
                emptyList()
            }
            else -> explicit
        }
    }
}
