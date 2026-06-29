package com.ampairs.dms

import com.ampairs.AmpairsApplication
import com.ampairs.dms.domain.dto.SecondarySalesRow
import com.ampairs.dms.domain.service.SnapshotService
import com.ampairs.dms.domain.service.TargetService
import com.ampairs.trade.exception.ConsentRequiredException
import com.ampairs.workspace.service.WorkspaceMemberService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.math.BigDecimal

/**
 * Verifies the brand DMS read path: a consented read returns rows, and a read without consent maps to
 * HTTP 403 (the trade-module ConsentRequiredException handler, on the classpath via the dms→trade dep).
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
class SnapshotControllerIntegrationTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext
    private lateinit var mockMvc: MockMvc

    @field:MockitoBean private lateinit var snapshotService: SnapshotService
    @field:MockitoBean private lateinit var targetService: TargetService
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
    @DisplayName("GET secondary-sales with consent returns rows")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `consented read returns rows`() {
        whenever(snapshotService.readSecondarySales("BRAND", "DIST"))
            .thenReturn(listOf(SecondarySalesRow("2026-06", "560001", "BPROD-1", "SKU-1", 10.0, BigDecimal("1000"), 1)))
        mockMvc.perform(get("/dms/v1/snapshots/secondary-sales").param("brand_workspace_id", "BRAND").param("distributor_workspace_id", "DIST"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].brand_product_uid").value("BPROD-1"))
    }

    @Test
    @DisplayName("GET secondary-sales without consent → 403")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `unconsented read is forbidden`() {
        whenever(snapshotService.readSecondarySales("BRAND", "DIST")).thenThrow(ConsentRequiredException("no link"))
        mockMvc.perform(get("/dms/v1/snapshots/secondary-sales").param("brand_workspace_id", "BRAND").param("distributor_workspace_id", "DIST"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.success").value(false))
    }
}
