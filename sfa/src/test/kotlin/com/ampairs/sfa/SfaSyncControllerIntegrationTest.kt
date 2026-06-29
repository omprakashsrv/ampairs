package com.ampairs.sfa

import com.ampairs.AmpairsApplication
import com.ampairs.sfa.domain.dto.VisitRequest
import com.ampairs.sfa.domain.enums.GeoFenceStatus
import com.ampairs.sfa.domain.enums.VisitOutcome
import com.ampairs.sfa.domain.model.Visit
import com.ampairs.sfa.domain.service.AttendanceService
import com.ampairs.sfa.domain.service.BeatService
import com.ampairs.sfa.domain.service.FieldOrderService
import com.ampairs.sfa.domain.service.JourneyPlanService
import com.ampairs.sfa.domain.service.VisitService
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
 * Exercises the SFA `/sync` controller end-to-end through the real security chain + JSON
 * (de)serialization, with the services mocked (mirrors CustomerControllerIntegrationTest).
 * Verifies the canonical contract shape: POST upsert echoes the rows, GET sync returns a page.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
class SfaSyncControllerIntegrationTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext
    @Autowired private lateinit var objectMapper: ObjectMapper
    private lateinit var mockMvc: MockMvc

    @field:MockitoBean private lateinit var beatService: BeatService
    @field:MockitoBean private lateinit var journeyPlanService: JourneyPlanService
    @field:MockitoBean private lateinit var visitService: VisitService
    @field:MockitoBean private lateinit var attendanceService: AttendanceService
    @field:MockitoBean private lateinit var fieldOrderService: FieldOrderService
    @field:MockitoBean private lateinit var workspaceMemberService: WorkspaceMemberService

    @BeforeEach
    fun setUp() {
        whenever(workspaceMemberService.isWorkspaceMember(any())).thenReturn(true)
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    private fun visit(uid: String) = Visit().apply {
        this.uid = uid; customerUid = "CUS-1"; repMemberUid = "REP-1"; adHoc = true
        outcome = VisitOutcome.PRODUCTIVE; geoFenceStatus = GeoFenceStatus.NO_LOCATION
    }

    @Test
    @DisplayName("POST /sfa/v1/visits/sync echoes the upserted visits")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `push visits`() {
        whenever(visitService.bulkUpsertVisits(any())).thenReturn(listOf(visit("VIS-1"), visit("VIS-2")))
        val body = listOf(
            VisitRequest(uid = "VIS-1", customerUid = "CUS-1", repMemberUid = "REP-1", adHoc = true),
            VisitRequest(uid = "VIS-2", customerUid = "CUS-1", repMemberUid = "REP-1", adHoc = true),
        )
        mockMvc.perform(
            post("/sfa/v1/visits/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].uid").value("VIS-1"))
            .andExpect(jsonPath("$.data[1].uid").value("VIS-2"))
    }

    @Test
    @DisplayName("GET /sfa/v1/visits/sync returns an ApiResponse page")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `pull visits`() {
        whenever(visitService.getVisitsAfterSync(anyOrNull(), any()))
            .thenReturn(org.springframework.data.domain.PageImpl(listOf(visit("VIS-9"))))
        mockMvc.perform(get("/sfa/v1/visits/sync"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content").isArray)
    }
}
