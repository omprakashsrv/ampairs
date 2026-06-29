package com.ampairs.sfa.domain.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.sfa.domain.enums.GeoFenceStatus
import com.ampairs.sfa.domain.model.Visit
import com.ampairs.sfa.exception.SfaValidationException
import com.ampairs.sfa.repository.PlannedVisitRepository
import com.ampairs.sfa.repository.VisitRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class VisitServiceTest {

    private val visitRepository: VisitRepository = mock()
    private val plannedVisitRepository: PlannedVisitRepository = mock()
    private val publisher: EntityChangePublisher = mock()
    private val service = VisitService(visitRepository, plannedVisitRepository, publisher)

    private fun visit(block: Visit.() -> Unit) = Visit().apply(block)

    @Test
    fun `planned visit without plannedVisitUid is rejected`() {
        val v = visit { adHoc = false; customerUid = "CUS-1"; repMemberUid = "REP-1" }
        assertThrows<SfaValidationException> { service.bulkUpsertVisits(listOf(v)) }
    }

    @Test
    fun `ad-hoc visit referencing a planned visit is rejected`() {
        val v = visit { adHoc = true; plannedVisitUid = "PLV-1"; customerUid = "CUS-1"; repMemberUid = "REP-1" }
        assertThrows<SfaValidationException> { service.bulkUpsertVisits(listOf(v)) }
    }

    @Test
    fun `out-of-radius ad-hoc visit is still saved and flagged`() {
        whenever(visitRepository.save(any<Visit>())).thenAnswer { it.arguments[0] as Visit }
        val v = visit {
            adHoc = true
            customerUid = "CUS-1"
            repMemberUid = "REP-1"
            distanceMeters = 5_000.0 // far outside the default 200m radius
        }
        val saved = service.bulkUpsertVisits(listOf(v))
        assertEquals(1, saved.size)
        assertEquals(GeoFenceStatus.OUT_OF_RADIUS, saved.first().geoFenceStatus)
    }

    @Test
    fun `no-location visit is saved as NO_LOCATION`() {
        whenever(visitRepository.save(any<Visit>())).thenAnswer { it.arguments[0] as Visit }
        val v = visit { adHoc = true; customerUid = "CUS-1"; repMemberUid = "REP-1" } // no distance
        val saved = service.bulkUpsertVisits(listOf(v))
        assertEquals(GeoFenceStatus.NO_LOCATION, saved.first().geoFenceStatus)
    }
}
