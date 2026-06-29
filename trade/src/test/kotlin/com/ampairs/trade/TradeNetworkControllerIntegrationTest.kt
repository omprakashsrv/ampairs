package com.ampairs.trade

import com.ampairs.AmpairsApplication
import com.ampairs.trade.domain.dto.PrimaryOrderPlaceRequest
import com.ampairs.trade.domain.dto.TradeLinkInviteRequest
import com.ampairs.trade.domain.enums.LinkStatus
import com.ampairs.trade.domain.model.TradeLink
import com.ampairs.trade.exception.ConsentRequiredException
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper

/**
 * Drives the trade network controller through the real security chain + JSON, services mocked.
 * Verifies the link-invite happy path and that a ConsentRequiredException maps to HTTP 403 via
 * TradeExceptionHandler — the consent edge's core contract.
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

    @Test
    @DisplayName("POST /trade/v1/links creates an INVITED link")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `invite link`() {
        val link = TradeLink().apply { uid = "TLK-1"; brandWorkspaceId = "BRAND"; distributorWorkspaceId = "DIST"; status = LinkStatus.INVITED }
        whenever(tradeLinkService.invite(any(), any(), anyOrNull())).thenReturn(link)
        mockMvc.perform(
            post("/trade/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(TradeLinkInviteRequest(brandWorkspaceId = "BRAND", distributorWorkspaceId = "DIST"))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.uid").value("TLK-1"))
            .andExpect(jsonPath("$.data.status").value("INVITED"))
    }

    @Test
    @DisplayName("POST /trade/v1/primary-orders without a link → 403")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `primary order without consent is forbidden`() {
        whenever(primaryOrderService.place(any(), any(), any())).thenThrow(ConsentRequiredException("no link"))
        mockMvc.perform(
            post("/trade/v1/primary-orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(PrimaryOrderPlaceRequest(brandWorkspaceId = "BRAND", distributorWorkspaceId = "DIST", brandOrderUid = "ORD-1"))),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
    }
}
