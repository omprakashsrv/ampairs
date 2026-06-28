package com.ampairs.communication.port

import com.ampairs.communication.domain.enums.Channel

/** A concrete recipient resolved from an audience. */
data class Recipient(
    val customerUid: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val pushToken: String? = null,
    val locale: String? = null,
) {
    /** The address for a channel, or null if the recipient has none (→ SKIPPED:NO_ADDRESS). */
    fun addressFor(channel: Channel): String? = when (channel) {
        Channel.EMAIL -> email?.takeIf { it.isNotBlank() }
        Channel.SMS, Channel.WHATSAPP -> phone?.takeIf { it.isNotBlank() }
        Channel.PUSH -> pushToken?.takeIf { it.isNotBlank() }
    }
}

/**
 * Resolves an audience to concrete recipients at send time (FR-013). Implemented as a port so the
 * communication module never reaches into the customer module's repositories (Principle IX).
 */
interface CustomerAudiencePort {
    /**
     * @param explicit recipients supplied directly by the caller (manual send / LIST audience).
     * Returns the resolved recipient set for the audience.
     */
    fun resolve(audienceType: String, audienceRef: String?, explicit: List<Recipient>): List<Recipient>
}
