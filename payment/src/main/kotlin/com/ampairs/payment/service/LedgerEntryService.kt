package com.ampairs.payment.service

import com.ampairs.payment.domain.dto.LedgerEntryRequest
import com.ampairs.payment.domain.dto.LedgerEntryResponse
import com.ampairs.payment.domain.dto.asResponse
import com.ampairs.payment.domain.dto.toEntity
import com.ampairs.payment.domain.model.LedgerEntry
import com.ampairs.payment.repository.LedgerEntryRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * `/sync` service for ledger entries (contracts/payment-sync.md §3). UID-keyed bulk upsert;
 * recomputes the affected `PartyBalance` after each push so the cache + foot-to-zero invariant
 * hold even when a freshly-created offline entry arrives.
 */
@Service
@Transactional(readOnly = true)
class LedgerEntryService(
    private val ledgerEntryRepository: LedgerEntryRepository,
    private val balanceService: BalanceService,
) {

    @Transactional
    fun bulkUpsert(requests: List<LedgerEntryRequest>): List<LedgerEntryResponse> {
        val results = mutableListOf<LedgerEntryResponse>()
        val affectedParties = linkedSetOf<String>()
        for (request in requests) {
            val incoming = request.toEntity()
            // amount stored ≥ 0; direction defaults applied in the converter.
            val existing = if (incoming.uid.isNotBlank()) ledgerEntryRepository.findByUid(incoming.uid) else null
            val toSave: LedgerEntry = if (existing != null) {
                existing.partyUid = incoming.partyUid
                existing.entryDate = incoming.entryDate
                existing.entryType = incoming.entryType
                existing.direction = incoming.direction
                existing.amount = incoming.amount
                existing.sourceType = incoming.sourceType
                existing.sourceUid = incoming.sourceUid
                existing.voucherNo = incoming.voucherNo
                existing.narration = incoming.narration
                existing.reversalOf = incoming.reversalOf
                existing.reversed = incoming.reversed
                existing.active = incoming.active
                existing
            } else {
                incoming
            }
            val saved = ledgerEntryRepository.save(toSave)
            affectedParties.add(saved.partyUid)
            results.add(saved.asResponse())
        }
        // Recompute each affected party once (order-independent pure sum).
        affectedParties.forEach { balanceService.recompute(it) }
        return results
    }

    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<LedgerEntry> {
        val cursor = SyncSupport.parseLastSync(lastSync)
        return if (cursor == null) ledgerEntryRepository.findAllByOrderByUpdatedAtAsc(pageable)
        else ledgerEntryRepository.findByUpdatedAtGreaterThanEqual(cursor, pageable)
    }
}
