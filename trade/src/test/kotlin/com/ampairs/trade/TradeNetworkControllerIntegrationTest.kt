package com.ampairs.trade

import com.ampairs.AmpairsApplication
import com.ampairs.trade.domain.enums.DesignationStatus
import com.ampairs.trade.domain.enums.LinkStatus
import com.ampairs.trade.domain.enums.PrimaryOrderStatus
import com.ampairs.trade.domain.enums.PublicationStatus
import com.ampairs.trade.domain.dto.NetworkBrandRequest
import com.ampairs.trade.domain.dto.PrimaryOrderConfirmRequest
import com.ampairs.trade.domain.dto.PrimaryOrderPlaceRequest
import com.ampairs.trade.domain.dto.SchemePublishRequest
import com.ampairs.trade.domain.dto.TradeLinkInviteRequest
import com.ampairs.trade.domain.model.NetworkBrand
import com.ampairs.trade.domain.model.PrimaryOrderLink
import com.ampairs.trade.domain.model.SchemePublication
import com.ampairs.trade.domain.model.TradeLink
import com.ampairs.trade.exception.ConsentRequiredException
import com.ampairs.trade.exception.LinkStateException
import com.ampairs.trade.service.NetworkBrandService
import com.ampairs.trade.service.PrimaryOrderService
import com.ampairs.trade.service.SchemePublicationService
import com.ampairs.trade.service.TradeLinkService
import com.ampairs.workspace.service.WorkspaceMemberService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper

/**
 * Drives the trade network controller through the real security chain + JSON, the four services
 * mocked. Covers the link/designation/publish/primary-order happy paths plus the consent (403) and
 * link-state (409) error mappings via TradeExceptionHandler.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
class TradeNetworkControllerIntegrationTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext
    @Autowired private lateinit var objectMapper: ObjectMapper
    private lateinit var mockMvc: MockMvc

    @field:MockitoBean private lateinit var tradeLinkService: TradeLinkService
    @field:MockitoBean private lateinit var networkBrandService: NetworkBrandService
    @field:MockitoBean private lateinit var schemePublicationService: SchemePublicationService
    @field:MockitoBean private lateinit var primaryOrderService: PrimaryOrderService
    @field:MockitoBean private lateinit var workspaceMemberService: WorkspaceMemberService

    @BeforeEach
    fun setUp() {
        whenever(workspaceMemberService.isWorkspaceMember(any())).thenReturn(true)
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    private fun link(status: LinkStatus) = TradeLink().apply {
        uid = "TLK-1"; brandWorkspaceId = "BRAND"; distributorWorkspaceId = "DIST"; this.status = status
    }

    @Test
    @DisplayName("POST /trade/v1/links invites a link")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `invite link`() {
        whenever(tradeLinkService.invite(any(), any(), anyOrNull())).thenReturn(link(LinkStatus.INVITED))
        mockMvc.perform(
            post("/trade/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(TradeLinkInviteRequest(brandWorkspaceId = "BRAND", distributorWorkspaceId = "DIST"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("INVITED"))
    }

    @Test
    @DisplayName("POST accept moves the link to ACCEPTED")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `accept link`() {
        whenever(tradeLinkService.accept(any(), anyOrNull())).thenReturn(link(LinkStatus.ACCEPTED))
        mockMvc.perform(post("/trade/v1/links/TLK-1/accept"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
    }

    @Test
    @DisplayName("POST revoke on a non-accepted link → 409")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `revoke conflict`() {
        whenever(tradeLinkService.revoke("TLK-1")).thenThrow(LinkStateException("not accepted"))
        mockMvc.perform(post("/trade/v1/links/TLK-1/revoke"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    @DisplayName("POST /network-brands designates a brand and GET lists them")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `designate and list brands`() {
        val nb = NetworkBrand().apply { uid = "NBR-1"; brandWorkspaceId = "BRAND"; status = DesignationStatus.ACTIVE }
        whenever(networkBrandService.designate("TLK-1", "DBR-1")).thenReturn(nb)
        mockMvc.perform(
            post("/trade/v1/network-brands")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(NetworkBrandRequest(linkUid = "TLK-1", distributorProductBrandUid = "DBR-1"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.uid").value("NBR-1"))

        whenever(networkBrandService.list("TLK-1")).thenReturn(listOf(nb))
        mockMvc.perform(get("/trade/v1/network-brands").param("link_uid", "TLK-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].uid").value("NBR-1"))
    }

    @Test
    @DisplayName("POST publish on a non-accepted link → 403")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `publish forbidden`() {
        whenever(schemePublicationService.publish("TLK-1", "OFFER-1")).thenThrow(ConsentRequiredException("no consent"))
        mockMvc.perform(
            post("/trade/v1/links/TLK-1/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SchemePublishRequest(linkUid = "TLK-1", schemeRef = "OFFER-1"))),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    @DisplayName("POST publish on an accepted link succeeds and GET lists schemes")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `publish and list schemes`() {
        val sp = SchemePublication().apply { uid = "SPB-1"; linkUid = "TLK-1"; schemeRef = "OFFER-1"; status = PublicationStatus.PUBLISHED }
        whenever(schemePublicationService.publish("TLK-1", "OFFER-1")).thenReturn(sp)
        mockMvc.perform(
            post("/trade/v1/links/TLK-1/schemes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(SchemePublishRequest(linkUid = "TLK-1", schemeRef = "OFFER-1"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.scheme_ref").value("OFFER-1"))

        whenever(schemePublicationService.listPublished("TLK-1")).thenReturn(listOf(sp))
        mockMvc.perform(get("/trade/v1/schemes").param("link_uid", "TLK-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].uid").value("SPB-1"))
    }

    @Test
    @DisplayName("primary-order place then confirm")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `place and confirm primary order`() {
        val placed = PrimaryOrderLink().apply { uid = "POL-1"; brandWorkspaceId = "BRAND"; distributorWorkspaceId = "DIST"; status = PrimaryOrderStatus.PLACED }
        whenever(primaryOrderService.place("BRAND", "DIST", "ORD-1")).thenReturn(placed)
        mockMvc.perform(
            post("/trade/v1/primary-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(PrimaryOrderPlaceRequest(brandWorkspaceId = "BRAND", distributorWorkspaceId = "DIST", brandOrderUid = "ORD-1"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("PLACED"))

        val confirmed = PrimaryOrderLink().apply { uid = "POL-1"; status = PrimaryOrderStatus.CONFIRMED; distributorOrderUid = "DORD-1" }
        whenever(primaryOrderService.confirm("POL-1", "DORD-1")).thenReturn(confirmed)
        mockMvc.perform(
            post("/trade/v1/primary-orders/POL-1/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(PrimaryOrderConfirmRequest(distributorOrderUid = "DORD-1"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
    }
}
