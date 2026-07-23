package com.ampairs.payment.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.payment.repository.AdjustmentVoucherRepository
import com.ampairs.payment.repository.LedgerEntryRepository
import com.ampairs.payment.repository.PartyBalanceRepository
import com.ampairs.payment.repository.PaymentAllocationRepository
import com.ampairs.payment.repository.PaymentVoucherRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the payment module's sync checkpoints (max `updatedAt` per entity) for the current
 * workspace. Without this, the mobile client's connect/reconnect/hourly bootstrap never learns the
 * server has payment data and never pulls it — entries pushed from another device (e.g. a desktop
 * Tally sync) stay invisible on mobile. Queries are `@TenantId`-filtered, so automatically
 * workspace-scoped.
 */
@Component
class PaymentCheckpointContributor(
    private val paymentVoucherRepository: PaymentVoucherRepository,
    private val paymentAllocationRepository: PaymentAllocationRepository,
    private val ledgerEntryRepository: LedgerEntryRepository,
    private val partyBalanceRepository: PartyBalanceRepository,
    private val adjustmentVoucherRepository: AdjustmentVoucherRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> = mapOf(
        "payment_voucher" to paymentVoucherRepository.findMaxUpdatedAt(),
        "payment_allocation" to paymentAllocationRepository.findMaxUpdatedAt(),
        "ledger_entry" to ledgerEntryRepository.findMaxUpdatedAt(),
        "party_balance" to partyBalanceRepository.findMaxUpdatedAt(),
        "adjustment" to adjustmentVoucherRepository.findMaxUpdatedAt(),
    )
}
