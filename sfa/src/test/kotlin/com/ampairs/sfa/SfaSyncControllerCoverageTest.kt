package com.ampairs.sfa

import com.ampairs.AmpairsApplication
import com.ampairs.sfa.domain.dto.AdherenceSummary
import com.ampairs.sfa.domain.dto.AttendanceRequest
import com.ampairs.sfa.domain.dto.BeatOutletRequest
import com.ampairs.sfa.domain.dto.BeatRequest
import com.ampairs.sfa.domain.dto.FieldOrderRequest
import com.ampairs.sfa.domain.dto.JourneyPlanRequest
import com.ampairs.sfa.domain.dto.PlannedVisitRequest
import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.domain.model.Beat
import com.ampairs.sfa.domain.model.BeatOutlet
import com.ampairs.sfa.domain.model.FieldOrder
import com.ampairs.sfa.domain.model.JourneyPlan
import com.ampairs.sfa.domain.model.PlannedVisit
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
import org.springframework.data.domain.PageImpl
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
 * Coverage for the remaining SFA `/sync` controller resources (beats, beat-outlets, journey-plans,
 * planned-visits, attendance, field-orders) + the adherence read-model. Visits are covered in
 * SfaSyncControllerIntegrationTest. Services mocked; real security chain + JSON.
 */
@SpringBootTest(classes = [AmpairsApplication::class])
@ActiveProfiles("test")
class SfaSyncControllerCoverageTest {

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

    private fun postJson(url: String, body: Any) =
        post(url).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body))

    @Test
    @DisplayName("beats push + pull")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun beats() {
        whenever(beatService.bulkUpsertBeats(any())).thenReturn(listOf(Beat().apply { uid = "BEAT-1"; name = "North" }))
        mockMvc.perform(postJson("/sfa/v1/beats/sync", listOf(BeatRequest(name = "North"))))
            .andExpect(status().isOk).andExpect(jsonPath("$.data[0].uid").value("BEAT-1"))
        whenever(beatService.getBeatsAfterSync(anyOrNull(), any())).thenReturn(PageImpl(listOf(Beat().apply { uid = "BEAT-9"; name = "S" })))
        mockMvc.perform(get("/sfa/v1/beats/sync")).andExpect(status().isOk).andExpect(jsonPath("$.data.content").isArray)
    }

    @Test
    @DisplayName("beat-outlets push + pull")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun beatOutlets() {
        whenever(beatService.bulkUpsertBeatOutlets(any())).thenReturn(listOf(BeatOutlet().apply { uid = "BO-1"; beatUid = "BEAT-1"; customerUid = "CUS-1" }))
        mockMvc.perform(postJson("/sfa/v1/beat-outlets/sync", listOf(BeatOutletRequest(beatUid = "BEAT-1", customerUid = "CUS-1"))))
            .andExpect(status().isOk).andExpect(jsonPath("$.data[0].uid").value("BO-1"))
        whenever(beatService.getBeatOutletsAfterSync(anyOrNull(), any())).thenReturn(PageImpl(listOf(BeatOutlet().apply { uid = "BO-9" })))
        mockMvc.perform(get("/sfa/v1/beat-outlets/sync")).andExpect(status().isOk)
    }

    @Test
    @DisplayName("journey-plans + planned-visits push + pull")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun journeyPlansAndPlannedVisits() {
        whenever(journeyPlanService.bulkUpsertJourneyPlans(any())).thenReturn(listOf(JourneyPlan().apply { uid = "JP-1"; repMemberUid = "REP-1" }))
        mockMvc.perform(postJson("/sfa/v1/journey-plans/sync", listOf(JourneyPlanRequest(repMemberUid = "REP-1", beatUid = "BEAT-1"))))
            .andExpect(status().isOk).andExpect(jsonPath("$.data[0].uid").value("JP-1"))
        whenever(journeyPlanService.getJourneyPlansAfterSync(anyOrNull(), any())).thenReturn(PageImpl(listOf(JourneyPlan().apply { uid = "JP-9" })))
        mockMvc.perform(get("/sfa/v1/journey-plans/sync")).andExpect(status().isOk)

        whenever(journeyPlanService.bulkUpsertPlannedVisits(any())).thenReturn(listOf(PlannedVisit().apply { uid = "PV-1"; customerUid = "CUS-1" }))
        mockMvc.perform(postJson("/sfa/v1/planned-visits/sync", listOf(PlannedVisitRequest(customerUid = "CUS-1", repMemberUid = "REP-1"))))
            .andExpect(status().isOk).andExpect(jsonPath("$.data[0].uid").value("PV-1"))
        whenever(journeyPlanService.getPlannedVisitsAfterSync(anyOrNull(), any())).thenReturn(PageImpl(listOf(PlannedVisit().apply { uid = "PV-9" })))
        mockMvc.perform(get("/sfa/v1/planned-visits/sync")).andExpect(status().isOk)
    }

    @Test
    @DisplayName("attendance push + pull")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun attendance() {
        whenever(attendanceService.bulkUpsertAttendance(any())).thenReturn(listOf(Attendance().apply { uid = "ATT-1"; repMemberUid = "REP-1" }))
        mockMvc.perform(postJson("/sfa/v1/attendance/sync", listOf(AttendanceRequest(repMemberUid = "REP-1"))))
            .andExpect(status().isOk).andExpect(jsonPath("$.data[0].uid").value("ATT-1"))
        whenever(attendanceService.getAttendanceAfterSync(anyOrNull(), any())).thenReturn(PageImpl(listOf(Attendance().apply { uid = "ATT-9" })))
        mockMvc.perform(get("/sfa/v1/attendance/sync")).andExpect(status().isOk)
    }

    @Test
    @DisplayName("field-orders push + pull")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun fieldOrders() {
        whenever(fieldOrderService.bulkUpsertFieldOrders(any())).thenReturn(listOf(FieldOrder().apply { uid = "FO-1"; customerUid = "CUS-1" }))
        mockMvc.perform(postJson("/sfa/v1/field-orders/sync", listOf(FieldOrderRequest(customerUid = "CUS-1", repMemberUid = "REP-1"))))
            .andExpect(status().isOk).andExpect(jsonPath("$.data[0].uid").value("FO-1"))
        whenever(fieldOrderService.getFieldOrdersAfterSync(anyOrNull(), any())).thenReturn(PageImpl(listOf(FieldOrder().apply { uid = "FO-9" })))
        mockMvc.perform(get("/sfa/v1/field-orders/sync")).andExpect(status().isOk)
    }

    @Test
    @DisplayName("GET /sfa/v1/adherence returns the read-model")
    @WithMockUser(username = "tester", roles = ["USER"])
    fun adherence() {
        whenever(journeyPlanService.adherence(any(), any(), any()))
            .thenReturn(AdherenceSummary("REP-1", plannedCount = 10, visitedCount = 8, missedCount = 2, pendingCount = 0, adHocCount = 1, adherencePercent = 80.0))
        mockMvc.perform(
            get("/sfa/v1/adherence")
                .param("rep_member_uid", "REP-1")
                .param("period_from", "2026-06-01T00:00:00Z")
                .param("period_to", "2026-06-30T00:00:00Z"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.adherence_percent").value(80.0))
    }
}
