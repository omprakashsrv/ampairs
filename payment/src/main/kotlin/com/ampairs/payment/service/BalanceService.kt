package com.ampairs.payment.service

import com.ampairs.customer.domain.service.CustomerService
import com.ampairs.payment.config.Constants
import com.ampairs.payment.domain.dto.RecomputeResponse
import com.ampairs.payment.domain.enums.Direction
import com.ampairs.payment.domain.enums.EntryType
import com.ampairs.payment.domain.enums.SourceType
import com.ampairs.payment.domain.model.LedgerEntry
import com.ampairs.payment.domain.model.PartyBalance
import com.ampairs.payment.repository.LedgerEntryRepository
import com.ampairs.payment.repository.PartyBalanceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant

/**
 * The ledger engine — sole owner of the foot-to-zero invariant (FR-002/022, SC-002/006).
 *
 * `closing = openingSigned + Σ active LedgerEntry.signedAmount`. The cached
 * `PartyBalance.cachedClosingBalance` is always fully recomputable; every posting recomputes
 * the affected party.
 */
@Service
@Transactional(readOnly = true)
class BalanceService(
    private val ledgerEntryRepository: LedgerEntryRepository,
    private val partyBalanceRepository: PartyBalanceRepository,
    private val customerService: CustomerService,
    private val entityChangePublisher: com.ampairs.core.sync.EntityChangePublisher,
) {
    private val logger = LoggerFactory.getLogger(BalanceService::class.java)

    companion object {
        private val SCALE = Constants.MONEY_SCALE
    }

    private fun BigDecimal.norm(): BigDecimal = this.setScale(SCALE, RoundingMode.HALF_UP)

    /** Deterministic uid for a ledger entry derived from a source document. */
    fun deterministicUid(sourceUid: String): String = "${Constants.LEDGER_SOURCE_PREFIX}$sourceUid"

    /**
     * Posts (creates or updates) a ledger entry with the deterministic uid `LDG_<sourceUid>`,
     * then recomputes the party balance. Used by voucher/adjustment/invoice posting.
     */
    @Transactional
    fun postSourceEntry(
        partyUid: String,
        entryType: EntryType,
        amount: BigDecimal,
        sourceType: SourceType,
        sourceUid: String,
        voucherNo: String?,
        entryDate: Instant,
        narration: String? = null,
        direction: Direction = entryType.naturalDirection(),
    ): LedgerEntry {
        val uid = deterministicUid(sourceUid)
        val existing = ledgerEntryRepository.findByUid(uid)
        val entry = existing ?: LedgerEntry().also { it.uid = uid }
        entry.partyUid = partyUid
        entry.entryDate = entryDate
        entry.entryType = entryType
        entry.direction = direction
        entry.amount = amount.abs().norm()
        entry.sourceType = sourceType
        entry.sourceUid = sourceUid
        entry.voucherNo = voucherNo
        entry.narration = narration
        entry.active = true
        val saved = ledgerEntryRepository.save(entry)
        if (existing == null) entityChangePublisher.created("ledger_entry", saved.uid)
        else entityChangePublisher.updated("ledger_entry", saved.uid)
        recompute(partyUid)
        return saved
    }

    /**
     * Reverses a previously posted source entry by soft-deleting it (audit retained) and
     * recomputing. Used when a document is cancelled/soft-deleted.
     */
    @Transactional
    fun reverseSourceEntry(sourceUid: String): Boolean {
        val entry = ledgerEntryRepository.findByUid(deterministicUid(sourceUid)) ?: return false
        if (!entry.active) return false
        entry.active = false
        entry.reversed = true
        ledgerEntryRepository.save(entry)
        entityChangePublisher.updated("ledger_entry", entry.uid)
        recompute(entry.partyUid)
        return true
    }

    /**
     * Posts a contra reversal entry (keeps the original active, adds an offsetting row).
     * Used by a cheque bounce — restores the dues without erasing history (FR-021).
     * Returns the contra entry, or null when the original is absent/inactive.
     */
    @Transactional
    fun postContraReversal(sourceUid: String, narration: String?): LedgerEntry? {
        val original = ledgerEntryRepository.findByUid(deterministicUid(sourceUid)) ?: return null
        if (!original.active) return null
        val contraUid = "${Constants.LEDGER_SOURCE_PREFIX}REV_$sourceUid"
        val existing = ledgerEntryRepository.findByUid(contraUid)
        val contra = existing ?: LedgerEntry().also { it.uid = contraUid }
        contra.partyUid = original.partyUid
        contra.entryDate = Instant.now()
        contra.entryType = original.entryType
        // opposite direction of the original to cancel its effect
        contra.direction = if (original.direction == Direction.DR) Direction.CR else Direction.DR
        contra.amount = original.amount
        contra.sourceType = original.sourceType
        contra.sourceUid = sourceUid
        contra.voucherNo = original.voucherNo
        contra.narration = narration ?: "Reversal of ${original.voucherNo ?: original.uid}"
        contra.reversalOf = original.uid
        contra.active = true
        val saved = ledgerEntryRepository.save(contra)
        original.reversed = true
        ledgerEntryRepository.save(original)
        entityChangePublisher.updated("ledger_entry", saved.uid)
        recompute(original.partyUid)
        return saved
    }

    /**
     * Recomputes and caches the signed closing balance for a party:
     * `openingSigned + Σ active signed entries`. Creates the [PartyBalance] row if absent.
     * Mirrors the result to `Customer.outstandingAmount` (R12 / T056).
     */
    @Transactional
    fun recompute(partyUid: String): BigDecimal {
        val balance = partyBalanceRepository.findByPartyUid(partyUid)
            ?: PartyBalance().also {
                it.partyUid = partyUid
                it.openingBalance = BigDecimal.ZERO
                it.openingDirection = Direction.DR
                it.openingAsOf = Instant.now()
            }
        val signedSum = ledgerEntryRepository.signedSumForParty(partyUid)
        val closing = balance.openingSigned().add(signedSum).norm()
        balance.cachedClosingBalance = closing
        balance.lastComputedAt = Instant.now()
        partyBalanceRepository.save(balance)
        entityChangePublisher.updated("party_balance", balance.uid)
        mirrorToCustomer(partyUid, closing)
        return closing
    }

    /**
     * Tie-out check (FR-022 / SC-002): `openingSigned + ΣDr − ΣCr == recomputed`.
     * Recomputes first, then reports the breakdown.
     */
    @Transactional
    fun recomputeWithTieOut(partyUid: String): RecomputeResponse {
        val before = partyBalanceRepository.findByPartyUid(partyUid)?.cachedClosingBalance ?: BigDecimal.ZERO
        val recomputed = recompute(partyUid)
        val balance = partyBalanceRepository.findByPartyUid(partyUid)
        val openingSigned = (balance?.openingSigned() ?: BigDecimal.ZERO).norm()
        val totalDebit = ledgerEntryRepository.totalDebitForParty(partyUid).norm()
        val totalCredit = ledgerEntryRepository.totalCreditForParty(partyUid).norm()
        val expected = openingSigned.add(totalDebit).subtract(totalCredit).norm()
        return RecomputeResponse(
            partyUid = partyUid,
            cachedBefore = before.norm(),
            recomputed = recomputed,
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            openingSigned = openingSigned,
            tieOutOk = expected.compareTo(recomputed) == 0,
        )
    }

    private fun mirrorToCustomer(partyUid: String, closing: BigDecimal) {
        try {
            val customer = customerService.getCustomerByUid(partyUid) ?: return
            customer.outstandingAmount = closing.toDouble()
            customerService.updateCustomer(customer)
        } catch (e: Exception) {
            // Mirror is best-effort (R12) — the PartyBalance remains authoritative.
            logger.warn("Failed to mirror balance to customer {}: {}", partyUid, e.message)
        }
    }
}
