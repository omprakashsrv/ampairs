package com.ampairs.payment.service

import com.ampairs.payment.domain.dto.PartyBalanceRequest
import com.ampairs.payment.domain.dto.PartyBalanceResponse
import com.ampairs.payment.domain.dto.asResponse
import com.ampairs.payment.domain.model.PartyBalance
import com.ampairs.payment.exception.InvalidPaymentDataException
import com.ampairs.payment.repository.PartyBalanceRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * `/sync` service for party balances (contracts/payment-sync.md §4). Push carries opening-balance
 * edits only; `cachedClosingBalance` is server-computed and authoritative on pull (US3 / FR-003).
 */
@Service
@Transactional(readOnly = true)
class PartyBalanceService(
    private val partyBalanceRepository: PartyBalanceRepository,
    private val balanceService: BalanceService,
) {

    @Transactional
    fun bulkUpsert(requests: List<PartyBalanceRequest>): List<PartyBalanceResponse> {
        val results = mutableListOf<PartyBalanceResponse>()
        for (request in requests) {
            if (request.openingBalance < BigDecimal.ZERO) {
                throw InvalidPaymentDataException("opening_balance must be >= 0")
            }
            // One balance row per (owner, party): match on partyUid first, then uid.
            val existing = partyBalanceRepository.findByPartyUid(request.partyUid)
                ?: request.uid.takeIf { it.isNotBlank() }?.let { partyBalanceRepository.findByUid(it) }
            val balance = existing ?: PartyBalance().also {
                if (request.uid.isNotBlank()) it.uid = request.uid
                it.partyUid = request.partyUid
            }
            balance.openingBalance = request.openingBalance
            balance.openingDirection = request.openingDirection
            balance.openingAsOf = request.openingAsOf ?: balance.openingAsOf ?: Instant.now()
            balance.active = request.active
            partyBalanceRepository.save(balance)
            // Fold the new opening into the cached closing.
            balanceService.recompute(request.partyUid)
            val refreshed = partyBalanceRepository.findByPartyUid(request.partyUid) ?: balance
            results.add(refreshed.asResponse())
        }
        return results
    }

    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<PartyBalance> {
        val cursor = SyncSupport.parseLastSync(lastSync)
        return if (cursor == null) partyBalanceRepository.findAllByOrderByUpdatedAtAsc(pageable)
        else partyBalanceRepository.findByUpdatedAtGreaterThanEqual(cursor, pageable)
    }
}
