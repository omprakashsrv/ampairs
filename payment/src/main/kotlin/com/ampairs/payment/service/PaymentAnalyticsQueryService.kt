package com.ampairs.payment.service

import com.ampairs.payment.domain.dto.AgingSlice
import com.ampairs.payment.domain.dto.CollectionsAgingProjection
import com.ampairs.payment.domain.enums.PaymentDirection
import com.ampairs.payment.repository.PaymentVoucherRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

/**
 * Public read-only port exposing collections/aging aggregates to the `analytics` module without
 * leaking payment internals (Principle II/IX). Aging is inherently point-in-time, so analytics reads
 * it live here rather than materializing it into the KPI summary.
 */
@Service
class PaymentAnalyticsQueryService(
    private val voucherRepository: PaymentVoucherRepository,
    private val agingService: AgingService,
) {

    /** Total money received (active RECEIVED vouchers) in [fromInclusive, toExclusive). */
    @Transactional(readOnly = true)
    fun collectedBetween(fromInclusive: Instant, toExclusive: Instant): BigDecimal =
        voucherRepository.sumActiveByDirectionInWindow(PaymentDirection.RECEIVED, fromInclusive, toExclusive)

    /** Open receivable + aging-bucket breakdown as of [asOf]. */
    @Transactional(readOnly = true)
    fun collectionsAging(asOf: Instant): CollectionsAgingProjection {
        val summary = agingService.summary(asOf)
        return CollectionsAgingProjection(
            totalOutstanding = summary.totalReceivable,
            buckets = summary.buckets.map { AgingSlice(it.label, it.amount) },
        )
    }
}
