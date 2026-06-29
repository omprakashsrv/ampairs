package com.ampairs.dms.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SnapshotAttributionCalculatorTest {

    private fun sale(
        label: String?, product: String, qty: Double, value: String, pincode: String? = "560001", period: String = "2026-06",
        distributor: String = "DIST-1",
    ) = RawSale(distributor, label, product, qty, BigDecimal(value), pincode, period)

    private val hopA = mapOf("LABEL-BRANDX" to "BRAND-X")
    private val hopB = mapOf("DPROD-1" to BrandSku("BPROD-1", "SKU-1"))

    @Test
    fun `other-brand and untagged sales are excluded`() {
        val rows = SnapshotAttributionCalculator.attribute(
            listOf(
                sale("LABEL-OTHER", "DPROD-9", 5.0, "100"), // other brand → excluded
                sale(null, "DPROD-8", 3.0, "50"),           // untagged → excluded
                sale("LABEL-BRANDX", "DPROD-1", 2.0, "200"),// attributed
            ),
            hopA, hopB,
        )
        assertEquals(1, rows.size)
        assertEquals("BRAND-X", rows.first().brandWorkspaceId)
    }

    @Test
    fun `attributed-but-unmapped sales fall into the unmapped bucket, never dropped`() {
        val rows = SnapshotAttributionCalculator.attribute(
            listOf(sale("LABEL-BRANDX", "DPROD-UNMAPPED", 7.0, "700")),
            hopA, hopB,
        )
        assertEquals(1, rows.size)
        assertEquals(null, rows.first().brandProductUid) // unmapped bucket
        assertEquals(7.0, rows.first().quantity)
    }

    @Test
    fun `Hop-B mapped sales are itemized by brand SKU and aggregate across distributors`() {
        val rows = SnapshotAttributionCalculator.attribute(
            listOf(
                sale("LABEL-BRANDX", "DPROD-1", 4.0, "400", distributor = "DIST-1"),
                sale("LABEL-BRANDX", "DPROD-1", 6.0, "600", distributor = "DIST-2"),
            ),
            hopA, hopB,
        )
        // Same brand SKU + same area + period ⇒ one aggregated row across both distributors.
        assertEquals(1, rows.size)
        assertEquals("BPROD-1", rows.first().brandProductUid)
        assertEquals(10.0, rows.first().quantity)
        assertEquals(0, BigDecimal("1000").compareTo(rows.first().value))
    }

    @Test
    fun `area is keyed off the retailer pincode, UNKNOWN when absent`() {
        val rows = SnapshotAttributionCalculator.attribute(
            listOf(
                sale("LABEL-BRANDX", "DPROD-1", 1.0, "10", pincode = "110001"),
                sale("LABEL-BRANDX", "DPROD-1", 1.0, "10", pincode = null),
            ),
            hopA, hopB,
        )
        val areas = rows.map { it.areaCode }.toSet()
        assertEquals(setOf("110001", "UNKNOWN"), areas)
    }

    @Test
    fun `same input yields identical output (deterministic, recomputable)`() {
        val input = listOf(sale("LABEL-BRANDX", "DPROD-1", 3.0, "300"))
        val a = SnapshotAttributionCalculator.attribute(input, hopA, hopB)
        val b = SnapshotAttributionCalculator.attribute(input, hopA, hopB)
        assertEquals(a, b)
        assertTrue(a.isNotEmpty())
    }
}
