package com.ampairs.communication.service.audience

import com.ampairs.communication.port.CustomerAudiencePort
import com.ampairs.communication.port.Recipient
import com.ampairs.customer.domain.service.CustomerContact
import com.ampairs.customer.domain.service.CustomerContactProvider
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Component

/**
 * Resolves audiences for the send engine.
 * - SINGLE: `audienceRef` = a customer uid → resolve full contact (email/phone/locale) via the
 *   customer module's public [CustomerContactProvider].
 * - LIST: caller-supplied explicit recipients (manual send).
 * - SEGMENT: `audienceRef` = a customer group → all active members.
 *
 * `CustomerContactProvider` is injected via [ObjectProvider] so communication stays unit-testable
 * without the customer module on the classpath; when absent, customer-keyed resolution returns empty.
 */
@Component
class CustomerAudienceAdapter(
    private val contactProvider: ObjectProvider<CustomerContactProvider>,
) : CustomerAudiencePort {

    private val logger = LoggerFactory.getLogger(CustomerAudienceAdapter::class.java)

    override fun resolve(audienceType: String, audienceRef: String?, explicit: List<Recipient>): List<Recipient> {
        return when (audienceType.uppercase()) {
            "LIST" -> explicit
            "SINGLE" -> when {
                explicit.isNotEmpty() -> explicit
                audienceRef.isNullOrBlank() -> emptyList()
                else -> resolveByUid(audienceRef)
            }
            "SEGMENT" -> if (audienceRef.isNullOrBlank()) emptyList() else resolveByGroup(audienceRef)
            else -> explicit
        }
    }

    private fun resolveByUid(customerUid: String): List<Recipient> {
        val provider = contactProvider.ifAvailable ?: run { warnNoProvider("SINGLE", customerUid); return emptyList() }
        return provider.byUid(customerUid)?.let { listOf(it.toRecipient()) } ?: emptyList()
    }

    private fun resolveByGroup(group: String): List<Recipient> {
        val provider = contactProvider.ifAvailable ?: run { warnNoProvider("SEGMENT", group); return emptyList() }
        return provider.byGroup(group).map { it.toRecipient() }
    }

    private fun warnNoProvider(type: String, ref: String) =
        logger.warn("No CustomerContactProvider available; cannot resolve {} audience '{}'", type, ref)

    private fun CustomerContact.toRecipient() = Recipient(
        customerUid = uid,
        email = email,
        phone = phone,
        pushToken = null,
        locale = locale,
    )
}
