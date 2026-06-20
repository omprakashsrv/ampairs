package com.ampairs.payment.service

import com.ampairs.customer.domain.service.CustomerService
import com.ampairs.payment.domain.dto.AgingBucketAmount
import com.ampairs.payment.domain.dto.AgingSummaryResponse
import com.ampairs.payment.domain.dto.PartyOverCreditLimit
import com.ampairs.payment.repository.PartyBalanceRepository
import com.ampairs.setting.service.SettingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * Collections summary (US4, FR-017/018/019): total receivable/payable, aging buckets for open
 * receivable bills, and parties whose receivable exceeds their configured credit limit.
 */
@Service
@Transactional(readOnly = true)
class AgingService(
    private val partyBalanceRepository: PartyBalanceRepository,
    private val outstandingService: OutstandingService,
    private val customerService: CustomerService,
    private val settingService: SettingService,
) {

    fun summary(asOf: Instant = Instant.now()): AgingSummaryResponse {
        val buckets = AgingBuckets.parse(settingService.getString("payment", "aging_buckets"))
        val bucketTotals = linkedMapOf<String, BigDecimal>().apply {
            buckets.labels().forEach { this[it] = BigDecimal.ZERO }
        }

        var totalReceivable = BigDecimal.ZERO
        var totalPayable = BigDecimal.ZERO
        val overLimit = mutableListOf<PartyOverCreditLimit>()

        for (balance in partyBalanceRepository.findByActiveTrue()) {
            val closing = balance.cachedClosingBalance
            if (closing.signum() > 0) {
                totalReceivable = totalReceivable.add(closing)
            } else if (closing.signum() < 0) {
                totalPayable = totalPayable.add(closing.abs())
            }

            // Credit-limit warning (FR-019): receivable beyond the customer's limit (> 0 limit only).
            val customer = customerService.getCustomerByUid(balance.partyUid)
            val creditLimit = BigDecimal.valueOf(customer?.creditLimit ?: 0.0)
            if (creditLimit.signum() > 0 && closing > creditLimit) {
                overLimit.add(
                    PartyOverCreditLimit(
                        partyUid = balance.partyUid,
                        balance = closing,
                        creditLimit = creditLimit,
                    ),
                )
            }

            // Bucket the party's open receivable bills by age.
            for (bill in outstandingService.openBills(balance.partyUid, asOf)) {
                bucketTotals[bill.agingBucket] = (bucketTotals[bill.agingBucket] ?: BigDecimal.ZERO)
                    .add(bill.outstanding)
            }
        }

        return AgingSummaryResponse(
            asOf = asOf,
            totalReceivable = totalReceivable,
            totalPayable = totalPayable,
            buckets = bucketTotals.map { (label, amount) -> AgingBucketAmount(label, amount) },
            partiesOverCreditLimit = overLimit,
        )
    }
}
