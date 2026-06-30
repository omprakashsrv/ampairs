package com.ampairs.sfa.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.sfa.domain.model.Beat
import com.ampairs.sfa.domain.model.BeatOutlet
import com.ampairs.sfa.domain.model.FieldOrder
import com.ampairs.sfa.domain.model.JourneyPlan
import com.ampairs.sfa.domain.model.Leave
import com.ampairs.sfa.domain.model.PlannedVisit
import com.ampairs.sfa.domain.model.Visit
import com.ampairs.sfa.domain.model.VisitSurveyResponse
import com.ampairs.sfa.repository.BeatOutletRepository
import com.ampairs.sfa.repository.BeatRepository
import com.ampairs.sfa.repository.FieldOrderRepository
import com.ampairs.sfa.repository.JourneyPlanRepository
import com.ampairs.sfa.repository.LeaveRepository
import com.ampairs.sfa.repository.PlannedVisitRepository
import com.ampairs.sfa.repository.VisitRepository
import com.ampairs.sfa.repository.VisitSurveyResponseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

/**
 * Fast unit cover for the SFA CRUD/`/sync` services (create + update branches, soft-delete,
 * adherence delegation). Repos + the change publisher are mocked — no Spring context.
 */
class SfaCrudServicesTest {

    private val publisher: EntityChangePublisher = mock()

    // ───────── Beat ─────────
    @Test
    fun `beat upsert inserts new then updates existing, and lists outlets`() {
        val beatRepo: BeatRepository = mock()
        val outletRepo: BeatOutletRepository = mock()
        val service = BeatService(beatRepo, outletRepo, publisher)
        whenever(beatRepo.save(any<Beat>())).thenAnswer { it.arguments[0] as Beat }

        // create branch (blank uid → no lookup)
        service.bulkUpsertBeats(listOf(Beat().apply { name = "North" }))
        verify(publisher).created(eq("beat"), any())

        // update branch
        whenever(beatRepo.findByUid("BEAT-1")).thenReturn(Beat().apply { uid = "BEAT-1" })
        service.bulkUpsertBeats(listOf(Beat().apply { uid = "BEAT-1"; name = "North-v2" }))
        verify(publisher).updated(eq("beat"), eq("BEAT-1"))

        // outlets upsert + ordered read
        whenever(outletRepo.save(any<BeatOutlet>())).thenAnswer { it.arguments[0] as BeatOutlet }
        service.bulkUpsertBeatOutlets(listOf(BeatOutlet().apply { beatUid = "BEAT-1" }))
        verify(publisher).created(eq("beat_outlet"), any())
        whenever(outletRepo.findByBeatUidAndActiveTrueOrderByVisitSequenceAsc("BEAT-1"))
            .thenReturn(listOf(BeatOutlet().apply { uid = "BO-1" }))
        assertEquals(1, service.outletsForBeat("BEAT-1").size)
    }

    // ───────── JourneyPlan + PlannedVisit + adherence ─────────
    @Test
    fun `journey plan and planned visit upsert plus adherence delegation`() {
        val jpRepo: JourneyPlanRepository = mock()
        val pvRepo: PlannedVisitRepository = mock()
        val visitRepo: VisitRepository = mock()
        val service = JourneyPlanService(jpRepo, pvRepo, visitRepo, publisher)
        whenever(jpRepo.save(any<JourneyPlan>())).thenAnswer { it.arguments[0] as JourneyPlan }
        whenever(pvRepo.save(any<PlannedVisit>())).thenAnswer { it.arguments[0] as PlannedVisit }

        service.bulkUpsertJourneyPlans(listOf(JourneyPlan().apply { repMemberUid = "REP-1" }))
        verify(publisher).created(eq("journey_plan"), any())
        whenever(jpRepo.findByUid("JP-1")).thenReturn(JourneyPlan().apply { uid = "JP-1" })
        service.bulkUpsertJourneyPlans(listOf(JourneyPlan().apply { uid = "JP-1" }))
        verify(publisher).updated(eq("journey_plan"), eq("JP-1"))

        service.bulkUpsertPlannedVisits(listOf(PlannedVisit().apply { repMemberUid = "REP-1" }))
        verify(publisher).created(eq("planned_visit"), any())
        whenever(pvRepo.findByUid("PV-1")).thenReturn(PlannedVisit().apply { uid = "PV-1" })
        service.bulkUpsertPlannedVisits(listOf(PlannedVisit().apply { uid = "PV-1" }))
        verify(publisher).updated(eq("planned_visit"), eq("PV-1"))

        val from = Instant.parse("2026-06-01T00:00:00Z")
        val to = Instant.parse("2026-06-30T00:00:00Z")
        whenever(pvRepo.findByRepMemberUidAndPlannedDateBetween("REP-1", from, to)).thenReturn(emptyList())
        whenever(visitRepo.findByRepMemberUidAndVisitedAtBetween("REP-1", from, to)).thenReturn(emptyList<Visit>())
        assertNotNull(service.adherence("REP-1", from, to))
    }

    // ───────── FieldOrder ─────────
    @Test
    fun `field order upsert inserts then updates`() {
        val repo: FieldOrderRepository = mock()
        val service = FieldOrderService(repo, publisher)
        whenever(repo.save(any<FieldOrder>())).thenAnswer { it.arguments[0] as FieldOrder }

        service.bulkUpsertFieldOrders(listOf(FieldOrder().apply { repMemberUid = "REP-1" }))
        verify(publisher).created(eq("field_order"), any())
        whenever(repo.findByUid("FO-1")).thenReturn(FieldOrder().apply { uid = "FO-1" })
        service.bulkUpsertFieldOrders(listOf(FieldOrder().apply { uid = "FO-1" }))
        verify(publisher).updated(eq("field_order"), eq("FO-1"))
    }

    // ───────── Leave (CRUD + soft-delete) ─────────
    @Test
    fun `leave create, soft-delete, and missing-delete`() {
        val repo: LeaveRepository = mock()
        val service = LeaveService(repo, publisher)
        whenever(repo.save(any<Leave>())).thenAnswer { it.arguments[0] as Leave }

        service.create(Leave().apply { uid = "LV-1"; repMemberUid = "REP-1" })
        verify(publisher).created(eq("leave"), eq("LV-1"))

        val existing = Leave().apply { uid = "LV-1"; active = true }
        whenever(repo.findByUid("LV-1")).thenReturn(existing)
        assertTrue(service.delete("LV-1"))
        assertFalse(existing.active)
        verify(publisher).updated(eq("leave"), eq("LV-1"))

        whenever(repo.findByUid("LV-missing")).thenReturn(null)
        assertFalse(service.delete("LV-missing"))
    }

    @Test
    fun `leave bulk upsert inserts then updates`() {
        val repo: LeaveRepository = mock()
        val service = LeaveService(repo, publisher)
        whenever(repo.save(any<Leave>())).thenAnswer { it.arguments[0] as Leave }

        service.bulkUpsertLeaves(listOf(Leave().apply { repMemberUid = "REP-1" }))
        verify(publisher).created(eq("leave"), any())
        whenever(repo.findByUid("LV-2")).thenReturn(Leave().apply { uid = "LV-2" })
        service.bulkUpsertLeaves(listOf(Leave().apply { uid = "LV-2" }))
        verify(publisher).updated(eq("leave"), eq("LV-2"))
    }

    // ───────── VisitSurveyResponse ─────────
    @Test
    fun `survey response upsert inserts then updates`() {
        val repo: VisitSurveyResponseRepository = mock()
        val service = VisitSurveyResponseService(repo, publisher)
        whenever(repo.save(any<VisitSurveyResponse>())).thenAnswer { it.arguments[0] as VisitSurveyResponse }

        service.bulkUpsert(listOf(VisitSurveyResponse().apply { visitUid = "V-1" }))
        verify(publisher).created(eq("visit_survey_response"), any())
        verify(publisher, never()).updated(eq("visit_survey_response"), any())
        whenever(repo.findByUid("VSR-1")).thenReturn(VisitSurveyResponse().apply { uid = "VSR-1" })
        service.bulkUpsert(listOf(VisitSurveyResponse().apply { uid = "VSR-1" }))
        verify(publisher).updated(eq("visit_survey_response"), eq("VSR-1"))
    }
}
