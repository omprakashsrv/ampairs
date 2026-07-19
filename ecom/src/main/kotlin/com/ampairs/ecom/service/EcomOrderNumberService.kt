package com.ampairs.ecom.service

import com.ampairs.sequence.service.SequenceCounterService
import com.ampairs.sequence.service.SequenceFormatter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Assigns gap-free, human-readable order numbers to ecom orders via the `sequence` module
 * (same engine `payment`'s `VoucherNumberService` uses for voucher numbers). Called once per
 * order at checkout time; the counter advance joins checkout's own transaction, so a rolled-back
 * checkout also rolls back the counter increment — no number is ever issued without a
 * corresponding created order.
 */
@Service
class EcomOrderNumberService(
    private val sequenceCounterService: SequenceCounterService,
) {
    private val logger = LoggerFactory.getLogger(EcomOrderNumberService::class.java)

    /** Next order number (e.g. "ECO-00001"), or "" if numbering fails — never blocks checkout. */
    fun next(): String {
        return try {
            val definition = sequenceCounterService.resolveOrProvision(ENTITY_TYPE, null)
            val range = sequenceCounterService.advance(definition.uid, 1)
            SequenceFormatter.format(
                prefix = PREFIX,
                suffix = definition.suffix,
                paddingLength = PADDING_LENGTH,
                value = range.first,
            )
        } catch (e: Exception) {
            logger.warn("Ecom order numbering failed: {}", e.message)
            ""
        }
    }

    private companion object {
        const val ENTITY_TYPE = "ecom_order"
        const val PREFIX = "ECO"
        const val PADDING_LENGTH = 5
    }
}
