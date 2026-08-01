package com.ampairs.payment.service

import com.ampairs.sequence.service.SequenceCounterService
import com.ampairs.sequence.service.SequenceFormatter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Assigns gap-free, human-readable voucher numbers per series via the `sequence` module
 * (FR-024 / R10). Series: RCP (receipts), PAY (payments-out), CRN/DBN (notes), ADJ (adjustments).
 *
 * A client-supplied voucher number is honored as-is (offline numbering); only a blank number
 * is filled from the server counter.
 */
@Service
class VoucherNumberService(
    private val sequenceCounterService: SequenceCounterService,
) {
    private val logger = LoggerFactory.getLogger(VoucherNumberService::class.java)

    /**
     * Returns [supplied] if non-blank, otherwise the next number for [series]
     * (entity type = `payment_<series-lowercased>`). Falls back gracefully on any
     * sequence error so a save is never blocked by numbering.
     */
    fun resolve(series: String, supplied: String?): String {
        if (!supplied.isNullOrBlank()) return supplied
        return try {
            val entityType = "payment_${series.lowercase()}"
            val definition = sequenceCounterService.resolveOrProvision(entityType, null)
            val range = sequenceCounterService.advance(definition.uid, 1)
            SequenceFormatter.format(
                prefix = series,
                suffix = definition.suffix,
                paddingLength = definition.paddingLength,
                value = range.first,
            )
        } catch (e: Exception) {
            logger.warn("Voucher numbering failed for series {}: {}", series, e.message)
            ""
        }
    }
}
