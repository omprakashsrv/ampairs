package com.ampairs.sequence

import com.ampairs.sequence.service.SequenceFormatter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SequenceFormatterTest {

    @Test
    fun `next value continues from high-water mark`() {
        assertEquals(1251, SequenceFormatter.nextValue(currentValue = 1250, startValue = 1, incrementStep = 1))
    }

    @Test
    fun `next value uses start value when nothing issued yet`() {
        assertEquals(1001, SequenceFormatter.nextValue(currentValue = 0, startValue = 1001, incrementStep = 1))
    }

    @Test
    fun `raising start value above the counter fast-forwards without reuse`() {
        assertEquals(5000, SequenceFormatter.nextValue(currentValue = 1250, startValue = 5000, incrementStep = 1))
    }

    @Test
    fun `lowering start value below the counter has no effect on issuance`() {
        assertEquals(1255, SequenceFormatter.nextValue(currentValue = 1250, startValue = 1, incrementStep = 5))
    }

    @Test
    fun `format plain number without prefix or padding`() {
        assertEquals("1001", SequenceFormatter.format(prefix = null, suffix = null, paddingLength = 0, value = 1001))
    }

    @Test
    fun `format with prefix`() {
        assertEquals("INV-1001", SequenceFormatter.format(prefix = "INV", suffix = null, paddingLength = 0, value = 1001))
    }

    @Test
    fun `format with prefix and padding`() {
        assertEquals("INV-000007", SequenceFormatter.format(prefix = "INV", suffix = null, paddingLength = 6, value = 7))
    }

    @Test
    fun `format with prefix and suffix`() {
        assertEquals("INV-42-GST", SequenceFormatter.format(prefix = "INV", suffix = "GST", paddingLength = 0, value = 42))
    }

    @Test
    fun `padding does not truncate values longer than the padding length`() {
        assertEquals("INV-1234567", SequenceFormatter.format(prefix = "INV", suffix = null, paddingLength = 4, value = 1234567))
    }

    @Test
    fun `default prefixes for standard entity types`() {
        assertEquals("PRD", SequenceFormatter.defaultPrefix("product"))
        assertEquals("CUS", SequenceFormatter.defaultPrefix("customer"))
        assertEquals("ORD", SequenceFormatter.defaultPrefix("order"))
        assertEquals("INV", SequenceFormatter.defaultPrefix("invoice"))
    }

    @Test
    fun `default prefix derives from unknown entity types`() {
        assertEquals("QUO", SequenceFormatter.defaultPrefix("quotation"))
        assertEquals("SEQ", SequenceFormatter.defaultPrefix("---"))
    }
}
