package com.ampairs.payment.service

import com.ampairs.payment.domain.enums.AdjustmentType
import com.ampairs.payment.domain.enums.Direction
import com.ampairs.payment.domain.enums.EntryType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Pure-logic checks for adjustment direction mapping (US5, FR-015) and aging classification
 * (US4, FR-017/018, SC-007).
 */
class AdjustmentAndAgingLogicTest {

    @Test
    fun `adjustment types map to the correct ledger direction`() {
        // Reduce receivable (CR)
        assertEquals(Direction.CR, AdjustmentType.SALES_RETURN.direction())
        assertEquals(Direction.CR, AdjustmentType.CREDIT_NOTE.direction())
        assertEquals(Direction.CR, AdjustmentType.PURCHASE_BILL.direction())
        assertEquals(Direction.CR, AdjustmentType.WRITE_OFF.direction())
        assertEquals(Direction.CR, AdjustmentType.DISCOUNT_ALLOWED.direction())
        // Increase receivable (DR)
        assertEquals(Direction.DR, AdjustmentType.PURCHASE_RETURN.direction())
        assertEquals(Direction.DR, AdjustmentType.DEBIT_NOTE.direction())
    }

    @Test
    fun `entry type natural directions match the sign convention`() {
        assertEquals(Direction.DR, EntryType.SALES_INVOICE.naturalDirection())
        assertEquals(Direction.CR, EntryType.PAYMENT_IN.naturalDirection())
        assertEquals(Direction.DR, EntryType.PAYMENT_OUT.naturalDirection())
        assertEquals(Direction.CR, EntryType.SALES_RETURN.naturalDirection())
    }

    @Test
    fun `aging buckets classify days overdue into the configured buckets`() {
        val buckets = AgingBuckets.parse("30,60,90")
        assertEquals(listOf("0-30", "31-60", "61-90", "90+"), buckets.labels())
        assertEquals("0-30", buckets.classify(0))
        assertEquals("0-30", buckets.classify(30))
        assertEquals("31-60", buckets.classify(31))
        assertEquals("31-60", buckets.classify(60))
        assertEquals("61-90", buckets.classify(90))
        assertEquals("90+", buckets.classify(91))
        assertEquals("90+", buckets.classify(400))
    }

    @Test
    fun `blank or invalid aging config falls back to the default`() {
        assertEquals(AgingBuckets.DEFAULT.labels(), AgingBuckets.parse(null).labels())
        assertEquals(AgingBuckets.DEFAULT.labels(), AgingBuckets.parse("garbage").labels())
    }
}
