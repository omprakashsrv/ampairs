package com.ampairs.dms.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RawSaleAssemblerTest {

    private fun line(product: String, customer: String) =
        SaleLine("DIST-1", product, customer, 2.0, BigDecimal("200"), "2026-06")

    private val labels = mapOf("DPROD-1" to "LABEL-BRANDX") // DPROD-9 intentionally absent (untagged)
    private val pincodes = mapOf("CUS-1" to "560001")

    @Test
    fun `assemble resolves brand label and pincode per line`() {
        val raw = RawSaleAssembler.assemble(
            listOf(line("DPROD-1", "CUS-1")),
            brandLabelOf = { labels[it] },
            pincodeOf = { pincodes[it] },
        )
        assertEquals(1, raw.size)
        assertEquals("LABEL-BRANDX", raw.first().productBrandLabelUid)
        assertEquals("DPROD-1", raw.first().distributorProductUid)
        assertEquals("560001", raw.first().retailerPincode)
        assertEquals("2026-06", raw.first().periodKey)
    }

    @Test
    fun `unresolved label and pincode pass through as null (excluded or UNKNOWN downstream)`() {
        val raw = RawSaleAssembler.assemble(
            listOf(line("DPROD-9", "CUS-9")),
            brandLabelOf = { labels[it] },
            pincodeOf = { pincodes[it] },
        )
        assertNull(raw.first().productBrandLabelUid) // calculator will exclude (untagged)
        assertNull(raw.first().retailerPincode)      // calculator will bucket as UNKNOWN area
    }

    @Test
    fun `the assembled feed flows through the attribution calculator end to end`() {
        val raw = RawSaleAssembler.assemble(
            listOf(line("DPROD-1", "CUS-1"), line("DPROD-9", "CUS-1")), // one tagged, one untagged
            brandLabelOf = { labels[it] },
            pincodeOf = { pincodes[it] },
        )
        val rows = SnapshotAttributionCalculator.attribute(
            raw,
            hopA = mapOf("LABEL-BRANDX" to "BRAND-X"),
            hopB = mapOf("DPROD-1" to BrandSku("BPROD-1", "SKU-1")),
        )
        assertEquals(1, rows.size) // only the tagged line is attributed
        assertEquals("BRAND-X", rows.first().brandWorkspaceId)
        assertEquals("BPROD-1", rows.first().brandProductUid)
        assertEquals("560001", rows.first().areaCode)
    }
}
