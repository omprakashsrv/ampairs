package com.ampairs.communication.service.usage

import com.ampairs.communication.domain.model.CommunicationUsage
import com.ampairs.communication.repository.CommunicationUsageRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

class UsageReportServiceTest {

    private val repo: CommunicationUsageRepository = mock()
    private val service = UsageReportService(repo)

    private fun usage(channel: String, billing: String, cred: String?, units: Int = 1) =
        CommunicationUsage().apply {
            this.channel = channel; billingMode = billing; credentialUid = cred; costUnits = units
            communicationLogUid = "CLOG-x"
        }

    @Test
    fun `aggregates by channel x credential x billing mode and reconciles totals`() {
        whenever(repo.findByOccurredAtBetween(any(), any())).thenReturn(
            listOf(
                usage("EMAIL", "PLATFORM", null),
                usage("EMAIL", "PLATFORM", null),
                usage("WHATSAPP", "CLIENT_OWN", "WCC1", units = 1),
                usage("SMS", "PLATFORM", null, units = 2),
            )
        )

        val report = service.report(Instant.EPOCH, Instant.now())

        // Three buckets: EMAIL/PLATFORM, WHATSAPP/CLIENT_OWN, SMS/PLATFORM
        assertEquals(3, report.rows.size)
        val email = report.rows.first { it.channel == "EMAIL" }
        assertEquals(2, email.messageCount)
        // Totals reconcile with the ledger
        assertEquals(4, report.totalMessages)
        assertEquals(5, report.totalCostUnits) // 1+1+1+2
        assertEquals(3, report.platformBillableMessages) // 2 email + 1 sms
    }

    @Test
    fun `empty period yields zero totals`() {
        whenever(repo.findByOccurredAtBetween(any(), any())).thenReturn(emptyList())
        val report = service.report(Instant.EPOCH, Instant.now())
        assertEquals(0, report.totalMessages)
        assertEquals(0, report.rows.size)
    }
}
