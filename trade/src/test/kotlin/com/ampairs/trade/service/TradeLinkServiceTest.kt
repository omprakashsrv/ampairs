package com.ampairs.trade.service

import com.ampairs.trade.domain.enums.LinkStatus
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
import org.mockito.kotlin.whenever

class TradeLinkServiceTest {

    private val linkRepo: TradeLinkRepository = mock()
    private val networkRepo: TradeNetworkRepository = mock()
    private val service = TradeLinkService(linkRepo, networkRepo)

    private fun link(status: LinkStatus, uid: String = "TLK-1") = TradeLink().apply {
        this.uid = uid; brandWorkspaceId = "BRAND"; distributorWorkspaceId = "DIST"; this.status = status
    }

    private fun stubSaveAndNetwork() {
        whenever(linkRepo.save(any<TradeLink>())).thenAnswer { it.arguments[0] as TradeLink }
        whenever(networkRepo.findByBrandWorkspaceId(any())).thenReturn(null)
        whenever(networkRepo.save(any<TradeNetwork>())).thenAnswer { it.arguments[0] as TradeNetwork }
    }

    @Test
    fun `invite creates an INVITED link`() {
        stubSaveAndNetwork()
        whenever(linkRepo.existsByBrandWorkspaceIdAndDistributorWorkspaceIdAndStatusNot(any(), any(), any())).thenReturn(false)
        val link = service.invite("BRAND", "DIST", null)
        assertEquals(LinkStatus.INVITED, link.status)
    }

    @Test
    fun `invite rejects a second non-revoked link for the same pair`() {
        whenever(linkRepo.existsByBrandWorkspaceIdAndDistributorWorkspaceIdAndStatusNot(eq("BRAND"), eq("DIST"), eq(LinkStatus.REVOKED))).thenReturn(true)
        assertThrows<LinkStateException> { service.invite("BRAND", "DIST", null) }
    }

    @Test
    fun `a workspace cannot link to itself`() {
        assertThrows<TradeException> { service.invite("W", "W", null) }
    }

    @Test
    fun `accept moves INVITED to ACCEPTED`() {
        whenever(linkRepo.findByUid("TLK-1")).thenReturn(link(LinkStatus.INVITED))
        whenever(linkRepo.save(any<TradeLink>())).thenAnswer { it.arguments[0] as TradeLink }
        assertEquals(LinkStatus.ACCEPTED, service.accept("TLK-1", null).status)
    }

    @Test
    fun `accepting a non-invited link is illegal`() {
        whenever(linkRepo.findByUid("TLK-1")).thenReturn(link(LinkStatus.REVOKED))
        assertThrows<LinkStateException> { service.accept("TLK-1", null) }
    }

    @Test
    fun `revoke moves ACCEPTED to REVOKED, but cannot revoke an INVITED link`() {
        whenever(linkRepo.findByUid("TLK-1")).thenReturn(link(LinkStatus.ACCEPTED))
        whenever(linkRepo.save(any<TradeLink>())).thenAnswer { it.arguments[0] as TradeLink }
        assertEquals(LinkStatus.REVOKED, service.revoke("TLK-1").status)

        whenever(linkRepo.findByUid("TLK-2")).thenReturn(link(LinkStatus.INVITED, "TLK-2"))
        assertThrows<LinkStateException> { service.revoke("TLK-2") }
    }
}
