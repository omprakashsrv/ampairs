package com.ampairs.sfa

import com.ampairs.AmpairsApplication
import com.ampairs.sfa.domain.dto.AttendanceSummaryResponse
import com.ampairs.sfa.domain.dto.LeaveRequest
import com.ampairs.sfa.domain.dto.VisitProductivityResponse
import com.ampairs.sfa.domain.model.Leave
import com.ampairs.sfa.domain.service.LeaveService
import com.ampairs.sfa.domain.service.ReportingService
import com.ampairs.sfa.domain.service.VisitSurveyResponseService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * Drives the SFA reporting/leave controller through the real security chain + JSON, services mocked.
 * Covers leave create/list/delete and the two server-computed reporting reads.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
class SfaReportingControllerIntegrationTest {

    @Autowired private lateinit var webApplicationContext: WebApplicationContext
    @Autowired private lateinit var objectMapper: ObjectMapper
    private lateinit var mockMvc: MockMvc

    @field:MockitoBean private lateinit var leaveService: LeaveService
    @field:MockitoBean private lateinit var reportingService: ReportingService
    @field:MockitoBean private lateinit var visitSurveyResponseService: VisitSurveyResponseService
    @field:MockitoBean private lateinit var workspaceMemberService: WorkspaceMemberService

    @BeforeEach
    fun setUp() {
        whenever(workspaceMemberService.isWorkspaceMember(any())).thenReturn(true)
        mockMvc = MockMvcBuilders
            .webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .build()
    }

    private fun leave(uid: String) = Leave().apply {
        this.uid = uid; repMemberUid = "REP-1"; leaveDate = Instant.parse("2026-06-10T00:00:00Z"); active = true
    }

    @Test
    @DisplayName("POST /sfa/v1/leaves creates a leave")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `create leave`() {
        whenever(leaveService.create(any())).thenReturn(leave("LV-1"))
        mockMvc.perform(
            post("/sfa/v1/leaves")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    LeaveRequest(repMemberUid = "REP-1", leaveDate = Instant.parse("2026-06-10T00:00:00Z")),
                )),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.uid").value("LV-1"))
    }

    @Test
    @DisplayName("GET /sfa/v1/leaves lists a rep's leaves in a window")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `list leaves`() {
        whenever(leaveService.list(any(), any(), any())).thenReturn(listOf(leave("LV-1")))
        mockMvc.perform(
            get("/sfa/v1/leaves")
                .param("rep_member_uid", "REP-1")
                .param("from", "2026-06-01T00:00:00Z")
                .param("to", "2026-06-30T00:00:00Z"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].uid").value("LV-1"))
    }

    @Test
    @DisplayName("DELETE /sfa/v1/leaves/{uid} soft-deletes")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `delete leave`() {
        whenever(leaveService.delete("LV-1")).thenReturn(true)
        mockMvc.perform(delete("/sfa/v1/leaves/LV-1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data").value(true))
    }

    @Test
    @DisplayName("GET /sfa/v1/attendance/summary returns the computed summary")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `attendance summary`() {
        whenever(reportingService.attendanceSummary(any(), any(), any()))
            .thenReturn(AttendanceSummaryResponse("REP-1", daysPresent = 20, totalWorkingHours = 160.0, openDays = 1, leaveDays = 2))
        mockMvc.perform(
            get("/sfa/v1/attendance/summary")
                .param("rep_member_uid", "REP-1")
                .param("from", "2026-06-01T00:00:00Z")
                .param("to", "2026-06-30T00:00:00Z"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.days_present").value(20))
            .andExpect(jsonPath("$.data.leave_days").value(2))
    }

    @Test
    @DisplayName("GET /sfa/v1/visits/productivity returns the computed productivity")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun `visit productivity`() {
        whenever(reportingService.visitProductivity(any(), any(), any()))
            .thenReturn(VisitProductivityResponse("REP-1", totalVisits = 10, productiveVisits = 7, productivePercent = 70.0, uniqueOutlets = 5, adHocVisits = 2))
        mockMvc.perform(
            get("/sfa/v1/visits/productivity")
                .param("rep_member_uid", "REP-1")
                .param("from", "2026-06-01T00:00:00Z")
                .param("to", "2026-06-30T00:00:00Z"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.total_visits").value(10))
            .andExpect(jsonPath("$.data.productive_percent").value(70.0))
    }
}
