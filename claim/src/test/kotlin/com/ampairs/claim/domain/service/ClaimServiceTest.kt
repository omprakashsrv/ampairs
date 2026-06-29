package com.ampairs.claim.domain.service

import com.ampairs.claim.domain.enums.ClaimStatus
import com.ampairs.claim.domain.model.ClaimSettlement
import com.ampairs.claim.domain.model.SchemeClaim
import com.ampairs.claim.exception.ClaimException
import com.ampairs.claim.exception.ClaimStateException
import com.ampairs.claim.repository.ClaimSettlementRepository
import com.ampairs.claim.repository.SchemeClaimRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class ClaimServiceTest {

    private val claimRepo: SchemeClaimRepository = mock()
    private val settlementRepo: ClaimSettlementRepository = mock()
    private val service = ClaimService(claimRepo, settlementRepo)

    private fun claim(status: ClaimStatus, uid: String = "SCL-1") = SchemeClaim().apply {
        this.uid = uid; schemeRef = "OFFER-1"; brandWorkspaceId = "BRAND"; distributorWorkspaceId = "DIST"
        computedAmount = BigDecimal("100"); this.status = status
    }

    @Test
    fun `accrue creates a DRAFT claim with zero amount allowed`() {
        whenever(claimRepo.save(any<SchemeClaim>())).thenAnswer { it.arguments[0] as SchemeClaim }
        val c = service.accrue("OFFER-1", "BRAND", "DIST", BigDecimal.ZERO, null, "2026-Q2")
        assertEquals(ClaimStatus.DRAFT, c.status)
        assertEquals(0, BigDecimal.ZERO.compareTo(c.computedAmount))
    }

    @Test
    fun `negative amount is rejected`() {
        assertThrows<ClaimException> { service.accrue("OFFER-1", "BRAND", "DIST", BigDecimal("-1"), null, null) }
    }

    @Test
    fun `happy path DRAFT to SETTLED`() {
        whenever(claimRepo.save(any<SchemeClaim>())).thenAnswer { it.arguments[0] as SchemeClaim }
        whenever(settlementRepo.save(any<ClaimSettlement>())).thenAnswer { it.arguments[0] as ClaimSettlement }

        whenever(claimRepo.findByUid("SCL-1")).thenReturn(claim(ClaimStatus.DRAFT))
        assertEquals(ClaimStatus.SUBMITTED, service.submit("SCL-1").status)

        whenever(claimRepo.findByUid("SCL-1")).thenReturn(claim(ClaimStatus.SUBMITTED))
        assertEquals(ClaimStatus.APPROVED, service.approve("SCL-1").status)

        whenever(claimRepo.findByUid("SCL-1")).thenReturn(claim(ClaimStatus.APPROVED))
        val settlement = service.settle("SCL-1", "REF-123", BigDecimal("100"))
        assertEquals("REF-123", settlement.reference)
    }

    @Test
    fun `illegal transitions throw`() {
        whenever(claimRepo.findByUid("SCL-1")).thenReturn(claim(ClaimStatus.DRAFT))
        assertThrows<ClaimStateException> { service.approve("SCL-1") } // DRAFT cannot be approved
        assertThrows<ClaimStateException> { service.settle("SCL-1", "REF", BigDecimal("1")) } // DRAFT cannot be settled
    }

    @Test
    fun `reject records the reason and does not settle`() {
        whenever(claimRepo.save(any<SchemeClaim>())).thenAnswer { it.arguments[0] as SchemeClaim }
        whenever(claimRepo.findByUid("SCL-1")).thenReturn(claim(ClaimStatus.SUBMITTED))
        val rejected = service.reject("SCL-1", "insufficient proof")
        assertEquals(ClaimStatus.REJECTED, rejected.status)
        assertEquals("insufficient proof", rejected.rejectionReason)
    }
}
