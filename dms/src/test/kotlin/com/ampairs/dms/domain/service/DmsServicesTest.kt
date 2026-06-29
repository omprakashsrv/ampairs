package com.ampairs.dms.domain.service

import com.ampairs.dms.domain.AttributedRow
import com.ampairs.dms.domain.model.SalesTarget
import com.ampairs.dms.domain.model.SecondarySalesSnapshot
import com.ampairs.dms.repository.DistributorStockSnapshotRepository
import com.ampairs.dms.repository.SalesTargetRepository
import com.ampairs.dms.repository.SecondarySalesSnapshotRepository
import com.ampairs.trade.domain.enums.DataCategory
import com.ampairs.trade.domain.model.TradeLink
import com.ampairs.trade.service.CrossTenantReadGuard
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class DmsServicesTest {

    private val secondaryRepo: SecondarySalesSnapshotRepository = mock()
    private val stockRepo: DistributorStockSnapshotRepository = mock()
    private val targetRepo: SalesTargetRepository = mock()
    private val guard: CrossTenantReadGuard = mock()

    private val snapshotService = SnapshotService(secondaryRepo, stockRepo, guard)
    private val targetService = TargetService(targetRepo, guard)

    @Test
    fun `recompute replaces a distributor's rows wholesale`() {
        val rows = listOf(
            AttributedRow("BRAND", "2026-06", "560001", "BPROD-1", "SKU-1", 10.0, BigDecimal("1000")),
            AttributedRow("BRAND", "2026-06", "560001", null, null, 5.0, BigDecimal("500")),
        )
        val saved = snapshotService.recomputeSecondarySales("DIST", rows)
        assertEquals(2, saved)
        verify(secondaryRepo).deleteByDistributorWorkspaceId("DIST")
        verify(secondaryRepo).saveAll(any<List<SecondarySalesSnapshot>>())
    }

    @Test
    fun `secondary-sales read passes the consent gate then returns rows`() {
        whenever(guard.requireActiveLink(eq("BRAND"), eq("DIST"), eq(DataCategory.SECONDARY_SALES))).thenReturn(mock<TradeLink>())
        whenever(secondaryRepo.findByAttributedBrandWorkspaceIdAndDistributorWorkspaceId("BRAND", "DIST"))
            .thenReturn(listOf(SecondarySalesSnapshot().apply { brandProductUid = "BPROD-1"; quantity = 3.0 }))
        val rows = snapshotService.readSecondarySales("BRAND", "DIST")
        assertEquals(1, rows.size)
        assertEquals("BPROD-1", rows.first().brandProductUid)
        verify(guard).requireActiveLink("BRAND", "DIST", DataCategory.SECONDARY_SALES)
    }

    @Test
    fun `targets read is consent-gated only when a distributor is given`() {
        whenever(targetRepo.findByBrandWorkspaceId("BRAND")).thenReturn(listOf(SalesTarget().apply { brandWorkspaceId = "BRAND" }))
        // no distributor → no gate
        assertEquals(1, targetService.readTargets("BRAND", null).size)
        // with distributor → gate invoked
        whenever(guard.requireActiveLink(eq("BRAND"), eq("DIST"), eq(DataCategory.TARGETS))).thenReturn(mock<TradeLink>())
        assertEquals(1, targetService.readTargets("BRAND", "DIST").size)
        verify(guard).requireActiveLink("BRAND", "DIST", DataCategory.TARGETS)
    }
}
