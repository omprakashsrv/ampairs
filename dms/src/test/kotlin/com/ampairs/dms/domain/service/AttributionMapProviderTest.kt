package com.ampairs.dms.domain.service

import com.ampairs.trade.domain.enums.DesignationStatus
import com.ampairs.trade.domain.enums.MappingStatus
import com.ampairs.trade.domain.enums.MatchSource
import com.ampairs.trade.domain.model.NetworkBrand
import com.ampairs.trade.domain.model.NetworkProduct
import com.ampairs.trade.domain.model.TradeLink
import com.ampairs.trade.service.NetworkBrandService
import com.ampairs.trade.service.NetworkProductService
import com.ampairs.trade.service.TradeLinkService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AttributionMapProviderTest {

    private val tradeLinkService: TradeLinkService = mock()
    private val networkBrandService: NetworkBrandService = mock()
    private val networkProductService: NetworkProductService = mock()
    private val provider = AttributionMapProvider(tradeLinkService, networkBrandService, networkProductService)

    private fun link(uid: String) = TradeLink().apply { this.uid = uid; distributorWorkspaceId = "DIST" }

    @Test
    fun `hopA collects only ACTIVE designations across the distributor's accepted links`() {
        whenever(tradeLinkService.acceptedLinksForDistributor("DIST")).thenReturn(listOf(link("TLK-1")))
        whenever(networkBrandService.list("TLK-1")).thenReturn(
            listOf(
                NetworkBrand().apply { distributorProductBrandUid = "LABEL-A"; brandWorkspaceId = "BRAND-A"; status = DesignationStatus.ACTIVE },
                NetworkBrand().apply { distributorProductBrandUid = "LABEL-B"; brandWorkspaceId = "BRAND-B"; status = DesignationStatus.REMOVED },
            ),
        )
        val map = provider.hopA("DIST")
        assertEquals(mapOf("LABEL-A" to "BRAND-A"), map)
    }

    @Test
    fun `hopB collects only CONFIRMED mappings with a brand product`() {
        whenever(tradeLinkService.acceptedLinksForDistributor("DIST")).thenReturn(listOf(link("TLK-1")))
        whenever(networkProductService.list("TLK-1")).thenReturn(
            listOf(
                NetworkProduct().apply { distributorProductUid = "DPROD-1"; brandProductUid = "BPROD-1"; brandSkuCode = "SKU-1"; status = MappingStatus.CONFIRMED; matchSource = MatchSource.AUTO_BARCODE },
                NetworkProduct().apply { distributorProductUid = "DPROD-2"; brandProductUid = "BPROD-2"; status = MappingStatus.SUGGESTED },
                NetworkProduct().apply { distributorProductUid = "DPROD-3"; brandProductUid = null; status = MappingStatus.CONFIRMED },
            ),
        )
        val map = provider.hopB("DIST")
        assertEquals(1, map.size)
        assertEquals("BPROD-1", map["DPROD-1"]?.brandProductUid)
        assertEquals("SKU-1", map["DPROD-1"]?.brandSkuCode)
    }
}
