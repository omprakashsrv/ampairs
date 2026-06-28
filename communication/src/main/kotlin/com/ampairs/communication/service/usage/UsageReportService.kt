package com.ampairs.communication.service.usage

import com.ampairs.communication.domain.dto.UsageReportResponse
import com.ampairs.communication.domain.dto.UsageRow
import com.ampairs.communication.repository.CommunicationUsageRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Aggregates the append-only usage ledger into a billing report for a period, grouped by
 * channel × credential × billing mode (× cost category). Totals reconcile with the SENT/DELIVERED
 * messages in the ledger (SC-010). `PLATFORM` rows are billable to the client.
 */
@Service
class UsageReportService(
    private val usageRepository: CommunicationUsageRepository,
) {
    @Transactional(readOnly = true)
    fun report(from: Instant, to: Instant): UsageReportResponse {
        val rows = usageRepository.findByOccurredAtBetween(from, to)
        val grouped = rows.groupBy {
            GroupKey(it.channel, it.credentialUid, it.providerAccountRef, it.billingMode, it.costCategory)
        }.map { (key, items) ->
            UsageRow(
                channel = key.channel,
                credentialUid = key.credentialUid,
                providerAccountRef = key.providerAccountRef,
                billingMode = key.billingMode,
                costCategory = key.costCategory,
                messageCount = items.size.toLong(),
                costUnits = items.sumOf { it.costUnits.toLong() },
            )
        }.sortedWith(compareBy({ it.channel }, { it.billingMode }))

        return UsageReportResponse(
            from = from.toString(),
            to = to.toString(),
            rows = grouped,
            totalMessages = rows.size.toLong(),
            totalCostUnits = rows.sumOf { it.costUnits.toLong() },
            platformBillableMessages = rows.count { it.billingMode == "PLATFORM" }.toLong(),
        )
    }

    private data class GroupKey(
        val channel: String,
        val credentialUid: String?,
        val providerAccountRef: String?,
        val billingMode: String,
        val costCategory: String?,
    )
}
