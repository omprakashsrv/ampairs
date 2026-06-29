package com.ampairs.claim

import com.ampairs.AmpairsApplication
import com.ampairs.claim.domain.dto.ClaimAccrueRequest
import com.ampairs.claim.domain.enums.ClaimStatus
import com.ampairs.claim.domain.model.SchemeClaim
import com.ampairs.claim.domain.service.ClaimService
import com.ampairs.claim.exception.ClaimStateException
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
import java.math.BigDecimal

/**
 * Drives the claim controller through the real security chain + JSON, ClaimService mocked. Verifies
 * the accrue happy path and that a ClaimStateException maps to HTTP 409 via ClaimExceptionHandler.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
class ClaimControllerIntegrationTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext
    @Autowired private lateinit var objectMapper: ObjectMapper
    private lateinit var mockMvc: MockMvc

    @field:MockitoBean private lateinit var claimService: ClaimService
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
    @DisplayName("POST /claim/v1/claims accrues a DRAFT claim")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `accrue claim`() {
        val claim = SchemeClaim().apply {
            uid = "SCL-1"; schemeRef = "OFFER-1"; brandWorkspaceId = "BRAND"; distributorWorkspaceId = "DIST"
            computedAmount = BigDecimal("100"); status = ClaimStatus.DRAFT
        }
        whenever(claimService.accrue(any(), any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(claim)
        mockMvc.perform(
            post("/claim/v1/claims")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ClaimAccrueRequest(schemeRef = "OFFER-1", brandWorkspaceId = "BRAND", distributorWorkspaceId = "DIST", computedAmount = BigDecimal("100")))),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.success").value(true))
            .andExpect(jsonPath("\$.data.status").value("DRAFT"))
    }

    @Test
    @DisplayName("POST approve on a non-submitted claim returns 409")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `illegal transition is conflict`() {
        whenever(claimService.approve("SCL-1")).thenThrow(ClaimStateException("not submitted"))
        mockMvc.perform(post("/claim/v1/claims/SCL-1/approve"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("\$.success").value(false))
    }
}
