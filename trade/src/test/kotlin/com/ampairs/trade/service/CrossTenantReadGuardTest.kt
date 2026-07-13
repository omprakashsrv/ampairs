package com.ampairs.trade.service

import com.ampairs.trade.domain.enums.DataCategory
import com.ampairs.trade.domain.enums.LinkStatus
import com.ampairs.trade.domain.model.TradeLink
import com.ampairs.trade.exception.ConsentRequiredException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CrossTenantReadGuardTest {

    private val tradeLinkService: TradeLinkService = mock()
    private val guard = CrossTenantReadGuard(tradeLinkService)

    private fun acceptedLink(shareStock: Boolean = true) = TradeLink().apply {
        uid = "TLK-1"; brandWorkspaceId = "BRAND"; distributorWorkspaceId = "DIST"; status = LinkStatus.ACCEPTED
        consentScope.shareStock = shareStock
    }

    @Test
    fun `no active link is denied`() {
        whenever(tradeLinkService.findActiveLink("BRAND", "DIST")).thenReturn(null)
        assertThrows<ConsentRequiredException> {
            guard.requireActiveLink("BRAND", "DIST", DataCategory.SECONDARY_SALES)
        }
    }

    @Test
    fun `active link not permitting the category is denied`() {
        whenever(tradeLinkService.findActiveLink("BRAND", "DIST")).thenReturn(acceptedLink(shareStock = false))
        assertThrows<ConsentRequiredException> {
            guard.requireActiveLink("BRAND", "DIST", DataCategory.STOCK)
        }
    }

    @Test
    fun `active link permitting the category returns the link`() {
        val link = acceptedLink(shareStock = true)
        whenever(tradeLinkService.findActiveLink("BRAND", "DIST")).thenReturn(link)
        assertEquals(link, guard.requireActiveLink("BRAND", "DIST", DataCategory.STOCK))
    }
}
