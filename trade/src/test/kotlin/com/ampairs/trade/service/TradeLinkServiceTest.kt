package com.ampairs.trade.service

import com.ampairs.trade.domain.enums.LinkStatus
import com.ampairs.trade.domain.model.ConsentScope
import com.ampairs.trade.domain.model.TradeLink
import com.ampairs.trade.domain.model.TradeNetwork
import com.ampairs.trade.exception.LinkStateException
import com.ampairs.trade.exception.TradeException
import com.ampairs.trade.repository.TradeLinkRepository
import com.ampairs.trade.repository.TradeNetworkRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TradeLinkServiceTest {

    private val linkRepo: TradeLinkRepository = mock()
    private val networkRepo: TradeNetworkRepository = mock()
    private val service = TradeLinkService(linkRepo, networkRepo)

    private fun link(status: LinkStatus) = TradeLink().apply {
        uid = "TLK-1"; brandWorkspaceId = "BRAND"; distributorWorkspaceId = "DIST"; this.status = status
    }

    // ───────── invite ─────────
    @Test
    fun `invite creates an INVITED link and ensures the network exists`() {
        whenever(linkRepo.existsByBrandWorkspaceIdAndDistributorWorkspaceIdAndStatusNot(any(), any(), any()))
            .thenReturn(false)
        whenever(networkRepo.findByBrandWorkspaceId("BRAND")).thenReturn(null)
        whenever(linkRepo.save(any<TradeLink>())).thenAnswer { it.arguments[0] as TradeLink }

        val created = service.invite("BRAND", "DIST", ConsentScope())
        assertEquals(LinkStatus.INVITED, created.status)
        verify(networkRepo).save(any<TradeNetwork>()) // created since none existed
    }

    @Test
    fun `invite does not recreate an existing network`() {
        whenever(linkRepo.existsByBrandWorkspaceIdAndDistributorWorkspaceIdAndStatusNot(any(), any(), any()))
            .thenReturn(false)
        whenever(networkRepo.findByBrandWorkspaceId("BRAND")).thenReturn(TradeNetwork().apply { brandWorkspaceId = "BRAND" })
        whenever(linkRepo.save(any<TradeLink>())).thenAnswer { it.arguments[0] as TradeLink }

        service.invite("BRAND", "DIST", null)
        verify(networkRepo, never()).save(any<TradeNetwork>())
    }

    @Test
    fun `invite rejects blank ids, self-link, and a duplicate non-revoked link`() {
        assertThrows<TradeException> { service.invite("", "DIST", null) }
        assertThrows<TradeException> { service.invite("BRAND", "BRAND", null) }
        whenever(linkRepo.existsByBrandWorkspaceIdAndDistributorWorkspaceIdAndStatusNot(any(), any(), any()))
            .thenReturn(true)
        assertThrows<LinkStateException> { service.invite("BRAND", "DIST", null) }
    }

    // ───────── accept / decline / revoke ─────────
    @Test
    fun `accept moves INVITED to ACCEPTED and may tighten scope`() {
        whenever(linkRepo.findByUid("TLK-1")).thenReturn(link(LinkStatus.INVITED))
        whenever(linkRepo.save(any<TradeLink>())).thenAnswer { it.arguments[0] as TradeLink }
        val tightened = ConsentScope()
        val accepted = service.accept("TLK-1", tightened)
        assertEquals(LinkStatus.ACCEPTED, accepted.status)
        assertEquals(tightened, accepted.consentScope)
    }

    @Test
    fun `accept on a non-invited link is rejected`() {
        whenever(linkRepo.findByUid("TLK-1")).thenReturn(link(LinkStatus.ACCEPTED))
        assertThrows<LinkStateException> { service.accept("TLK-1", null) }
    }

    @Test
    fun `decline moves INVITED to DECLINED, else rejects`() {
        whenever(linkRepo.findByUid("TLK-1")).thenReturn(link(LinkStatus.INVITED))
        whenever(linkRepo.save(any<TradeLink>())).thenAnswer { it.arguments[0] as TradeLink }
        assertEquals(LinkStatus.DECLINED, service.decline("TLK-1").status)

        whenever(linkRepo.findByUid("TLK-1")).thenReturn(link(LinkStatus.ACCEPTED))
        assertThrows<LinkStateException> { service.decline("TLK-1") }
    }

    @Test
    fun `revoke moves ACCEPTED to REVOKED, else rejects`() {
        whenever(linkRepo.findByUid("TLK-1")).thenReturn(link(LinkStatus.ACCEPTED))
        whenever(linkRepo.save(any<TradeLink>())).thenAnswer { it.arguments[0] as TradeLink }
        assertEquals(LinkStatus.REVOKED, service.revoke("TLK-1").status)

        whenever(linkRepo.findByUid("TLK-1")).thenReturn(link(LinkStatus.INVITED))
        assertThrows<LinkStateException> { service.revoke("TLK-1") }
    }

    @Test
    fun `require throws when the link is missing`() {
        whenever(linkRepo.findByUid("nope")).thenReturn(null)
        assertThrows<TradeException> { service.require("nope") }
    }

    // ───────── reads ─────────
    @Test
    fun `findActiveLink and acceptedLinksForDistributor filter correctly`() {
        whenever(linkRepo.findByBrandWorkspaceIdAndDistributorWorkspaceIdAndStatus("BRAND", "DIST", LinkStatus.ACCEPTED))
            .thenReturn(link(LinkStatus.ACCEPTED))
        assertEquals("TLK-1", service.findActiveLink("BRAND", "DIST")?.uid)

        whenever(linkRepo.findByDistributorWorkspaceId("DIST"))
            .thenReturn(listOf(link(LinkStatus.ACCEPTED), link(LinkStatus.INVITED), link(LinkStatus.REVOKED)))
        assertEquals(1, service.acceptedLinksForDistributor("DIST").size)
    }
}
