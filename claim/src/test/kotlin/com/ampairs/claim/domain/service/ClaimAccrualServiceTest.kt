package com.ampairs.claim.domain.service

import com.ampairs.claim.domain.enums.ClaimStatus
import com.ampairs.claim.domain.model.SchemeClaim
import com.ampairs.claim.exception.ClaimException
import com.ampairs.claim.repository.ClaimSettlementRepository
import com.ampairs.claim.repository.SchemeClaimRepository
import com.ampairs.dms.domain.service.SnapshotService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class ClaimAccrualServiceTest {

    private val snapshotService: SnapshotService = mock()
    private val claimRepository: SchemeClaimRepository = mock()
    private val settlementRepository: ClaimSettlementRepository = mock()
    private val claimService = ClaimService(claimRepository, settlementRepository)
    private val service = ClaimAccrualService(snapshotService, claimService)

    init {
        whenever(claimRepository.save(any<SchemeClaim>())).thenAnswer { it.arguments[0] as SchemeClaim }
    }

    @Test
    fun `accrue computes amount as qualifying value times rate and opens a DRAFT claim`() {
        whenever(snapshotService.qualifyingSecondaryValue(eq("BRAND"), eq("DIST"), eq("2026-06")))
            .thenReturn(BigDecimal("10000"))

        val claim = service.accrueFromSecondarySales("OFFER-1", "BRAND", "DIST", "2026-06", BigDecimal("2.5"))

        // 10000 × 2.5 / 100 = 250.0000
        assertEquals(0, BigDecimal("250.0000").compareTo(claim.computedAmount))
        assertEquals(ClaimStatus.DRAFT, claim.status)
        assertEquals("OFFER-1", claim.schemeRef)
        assertEquals("2026-06", claim.periodKey)
    }

    @Test
    fun `zero qualifying value yields a zero-amount claim`() {
        whenever(snapshotService.qualifyingSecondaryValue(eq("BRAND"), eq("DIST"), anyOrNull())).thenReturn(BigDecimal.ZERO)
        val claim = service.accrueFromSecondarySales("OFFER-1", "BRAND", "DIST", null, BigDecimal("5"))
        assertEquals(0, BigDecimal.ZERO.compareTo(claim.computedAmount))
    }

    @Test
    fun `a negative rate is rejected before any read`() {
        assertThrows<ClaimException> {
            service.accrueFromSecondarySales("OFFER-1", "BRAND", "DIST", "2026-06", BigDecimal("-1"))
        }
    }
}
