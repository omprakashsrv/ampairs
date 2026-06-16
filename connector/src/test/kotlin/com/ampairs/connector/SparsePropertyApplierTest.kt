package com.ampairs.connector

import com.ampairs.core.connector.SparsePropertyApplier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/** Validates presence-based, type-coercing flat reflection apply (the basis of the no-data-loss fix). */
class SparsePropertyApplierTest {

    class Bean {
        var name: String = ""
        var quantity: Int = 0
        var price: Double = 0.0
        var note: String? = "keep"
    }

    @Test
    fun `applies present scalar columns with type coercion`() {
        val b = Bean()
        val applied = SparsePropertyApplier.apply(b, mapOf("name" to "Acme", "quantity" to "5", "price" to 9.5))
        assertEquals(setOf("name", "quantity", "price"), applied.toSet())
        assertEquals("Acme", b.name)
        assertEquals(5, b.quantity)
        assertEquals(9.5, b.price)
    }

    @Test
    fun `omitted columns are preserved and unknown keys ignored`() {
        val b = Bean()
        val applied = SparsePropertyApplier.apply(b, mapOf("name" to "X", "unknownField" to "y"))
        assertEquals(listOf("name"), applied)
        assertEquals("keep", b.note) // omitted from payload -> preserved (no data loss)
        assertEquals(0, b.quantity)  // omitted -> preserved
    }

    @Test
    fun `explicit null clears a nullable field`() {
        val b = Bean()
        val applied = SparsePropertyApplier.apply(b, mapOf("note" to null))
        assertEquals(listOf("note"), applied)
        assertNull(b.note)
    }
}
