package com.ampairs.dms.domain.service

import com.ampairs.dms.domain.AttributedRow
import com.ampairs.dms.domain.BrandSku
import com.ampairs.dms.domain.SaleLine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class SecondarySalesRecomputeServiceTest {

    private val attributionMapProvider: AttributionMapProvider = mock()
    private val snapshotService: SnapshotService = mock()
    private val service = SecondarySalesRecomputeService(attributionMapProvider, snapshotService)

    @Test
    fun `recompute composes the pipeline end to end and persists attributed rows`() {
        whenever(attributionMapProvider.hopA("DIST")).thenReturn(mapOf("LABEL-BRANDX" to "BRAND-X"))
        whenever(attributionMapProvider.hopB("DIST")).thenReturn(mapOf("DPROD-1" to BrandSku("BPROD-1", "SKU-1")))
        whenever(snapshotService.recomputeSecondarySales(eq("DIST"), any())).thenAnswer { (it.arguments[1] as List<*>).size }

        val lines = listOf(
            SaleLine("DIST", "DPROD-1", "CUS-1", 4.0, BigDecimal("400"), "2026-06"),  // attributed + SKU-mapped
            SaleLine("DIST", "DPROD-9", "CUS-1", 9.0, BigDecimal("900"), "2026-06"),  // untagged → excluded
        )
        val count = service.recompute(
            "DIST", lines,
            brandLabelOf = { if (it == "DPROD-1") "LABEL-BRANDX" else null },
            pincodeOf = { "560001" },
        )

        val captor = argumentCaptor<List<AttributedRow>>()
        whenever(snapshotService.recomputeSecondarySales(eq("DIST"), captor.capture())).thenReturn(1)
        // re-run to capture the persisted rows
        service.recompute("DIST", lines, brandLabelOf = { if (it == "DPROD-1") "LABEL-BRANDX" else null }, pincodeOf = { "560001" })
        val persisted = captor.firstValue
        assertEquals(1, persisted.size) // only the tagged line survives attribution
        assertEquals("BRAND-X", persisted.first().brandWorkspaceId)
        assertEquals("BPROD-1", persisted.first().brandProductUid)
        assertEquals(1, count)
    }
}
