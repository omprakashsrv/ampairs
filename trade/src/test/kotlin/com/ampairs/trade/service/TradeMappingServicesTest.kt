package com.ampairs.trade.service

import com.ampairs.trade.domain.enums.LinkStatus
import com.ampairs.trade.domain.enums.MappingStatus
import com.ampairs.trade.domain.enums.MatchSource
import com.ampairs.trade.domain.enums.PrimaryOrderStatus
import com.ampairs.trade.domain.model.NetworkBrand
import com.ampairs.trade.domain.model.NetworkProduct
import com.ampairs.trade.domain.model.PrimaryOrderLink
import com.ampairs.trade.domain.model.SchemePublication
import com.ampairs.trade.domain.model.TradeLink
import com.ampairs.trade.exception.ConsentRequiredException
import com.ampairs.trade.exception.LinkStateException
import com.ampairs.trade.repository.NetworkBrandRepository
import com.ampairs.trade.repository.NetworkProductRepository
import com.ampairs.trade.repository.PrimaryOrderLinkRepository
import com.ampairs.trade.repository.SchemePublicationRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class TradeMappingServicesTest {

    private val tradeLinkService: TradeLinkService = mock()

    private fun link(status: LinkStatus) = TradeLink().apply {
        uid = "TLK-1"; brandWorkspaceId = "BRAND"; distributorWorkspaceId = "DIST"; this.status = status
    }

    // ───────── NetworkBrand (Hop A) ─────────
    @Test
    fun `designate requires an ACCEPTED link and is idempotent`() {
        val repo: NetworkBrandRepository = mock()
        val service = NetworkBrandService(repo, tradeLinkService)
        whenever(tradeLinkService.require("TLK-1")).thenReturn(link(LinkStatus.ACCEPTED))
        whenever(repo.findByLinkUidAndDistributorProductBrandUidAndStatus(any(), any(), any())).thenReturn(null)
        whenever(repo.save(any<NetworkBrand>())).thenAnswer { it.arguments[0] as NetworkBrand }
        val nb = service.designate("TLK-1", "LABEL-1")
        assertEquals("BRAND", nb.brandWorkspaceId)

        // idempotent: an existing ACTIVE designation is returned, not duplicated
        val existing = NetworkBrand().apply { uid = "NBR-1" }
        whenever(repo.findByLinkUidAndDistributorProductBrandUidAndStatus(any(), any(), any())).thenReturn(existing)
        assertSame(existing, service.designate("TLK-1", "LABEL-1"))
    }

    @Test
    fun `designate on a non-accepted link is rejected`() {
        val repo: NetworkBrandRepository = mock()
        val service = NetworkBrandService(repo, tradeLinkService)
        whenever(tradeLinkService.require("TLK-1")).thenReturn(link(LinkStatus.INVITED))
        assertThrows<LinkStateException> { service.designate("TLK-1", "LABEL-1") }
    }

    // ───────── NetworkProduct (Hop B) ─────────
    @Test
    fun `upsert mapping requires ACCEPTED link then confirm flips to CONFIRMED`() {
        val repo: NetworkProductRepository = mock()
        val service = NetworkProductService(repo, tradeLinkService)
        whenever(tradeLinkService.require("TLK-1")).thenReturn(link(LinkStatus.ACCEPTED))
        whenever(repo.save(any<NetworkProduct>())).thenAnswer { it.arguments[0] as NetworkProduct }
        val np = service.upsertMapping("TLK-1", "DPROD-1", "BPROD-1", "SKU-1", MatchSource.AUTO_BARCODE, MappingStatus.SUGGESTED)
        assertEquals(MappingStatus.SUGGESTED, np.status)

        whenever(repo.findByUid("NPR-1")).thenReturn(NetworkProduct().apply { uid = "NPR-1"; status = MappingStatus.SUGGESTED })
        assertEquals(MappingStatus.CONFIRMED, service.confirm("NPR-1").status)
    }

    // ───────── SchemePublication ─────────
    @Test
    fun `publish requires an ACCEPTED link`() {
        val repo: SchemePublicationRepository = mock()
        val service = SchemePublicationService(repo, tradeLinkService)
        whenever(tradeLinkService.require("TLK-1")).thenReturn(link(LinkStatus.REVOKED))
        assertThrows<ConsentRequiredException> { service.publish("TLK-1", "OFFER-1") }

        whenever(tradeLinkService.require("TLK-1")).thenReturn(link(LinkStatus.ACCEPTED))
        whenever(repo.save(any<SchemePublication>())).thenAnswer { it.arguments[0] as SchemePublication }
        assertEquals("OFFER-1", service.publish("TLK-1", "OFFER-1").schemeRef)
    }

    // ───────── PrimaryOrder handshake ─────────
    @Test
    fun `place requires an active link then confirm and reject enforce the state machine`() {
        val repo: PrimaryOrderLinkRepository = mock()
        val service = PrimaryOrderService(repo, tradeLinkService)

        whenever(tradeLinkService.findActiveLink("BRAND", "DIST")).thenReturn(null)
        assertThrows<ConsentRequiredException> { service.place("BRAND", "DIST", "ORD-1") }

        whenever(tradeLinkService.findActiveLink("BRAND", "DIST")).thenReturn(link(LinkStatus.ACCEPTED))
        whenever(repo.save(any<PrimaryOrderLink>())).thenAnswer { it.arguments[0] as PrimaryOrderLink }
        assertEquals(PrimaryOrderStatus.PLACED, service.place("BRAND", "DIST", "ORD-1").status)

        whenever(repo.findByUid("POL-1")).thenReturn(PrimaryOrderLink().apply { uid = "POL-1"; status = PrimaryOrderStatus.PLACED })
        val confirmed = service.confirm("POL-1", "DORD-1")
        assertEquals(PrimaryOrderStatus.CONFIRMED, confirmed.status)
        assertEquals("DORD-1", confirmed.distributorOrderUid)

        whenever(repo.findByUid("POL-2")).thenReturn(PrimaryOrderLink().apply { uid = "POL-2"; status = PrimaryOrderStatus.CONFIRMED })
        assertThrows<LinkStateException> { service.reject("POL-2") } // only PLACED can be rejected
    }
}
