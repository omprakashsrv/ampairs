package com.ampairs.analytics.service

import com.ampairs.analytics.repository.DemandForecastRepository
import com.ampairs.invoice.domain.dto.FinalizedInvoiceProjection
import com.ampairs.invoice.domain.dto.InvoiceLineProjection
import com.ampairs.invoice.service.InvoiceAnalyticsQueryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.time.ZoneOffset

/**
 * Unit tests for the forecast orchestration (feature 022, T052 — lifts the forecast-path coverage;
 * the Holt-Winters/MA maths itself is covered by DemandForecastingTest). Mocks the invoice projection
 * source, the forecast repository, the timezone provider and the event publisher.
 */
class ForecastServiceTest {

    private val invoiceQuery = mock<InvoiceAnalyticsQueryService>()
    private val repo = mock<DemandForecastRepository>()
    private val tz = mock<BusinessTimeZoneProvider> { on { currentZone() } doReturn ZoneOffset.UTC }
    private val events = mock<ApplicationEventPublisher>()
    private val service = ForecastService(invoiceQuery, repo, tz, events)

    private fun invoice(vararg lines: InvoiceLineProjection) = FinalizedInvoiceProjection(
        invoiceDateEpochMillis = Instant.now().toEpochMilli(),
        gross = 100.0, net = 90.0, tax = 10.0, customerId = "C1", intraState = true,
        taxLines = emptyList(), lines = lines.toList(),
    )

    @Test
    fun `recompute forecasts each product sold and publishes an update`() {
        whenever(invoiceQuery.finalizedBetween(any(), any())).thenReturn(
            listOf(
                invoice(InvoiceLineProjection("PRD1", 5.0, 100.0)),
                invoice(InvoiceLineProjection("PRD1", 3.0, 60.0), InvoiceLineProjection("PRD2", 2.0, 40.0)),
            ),
        )

        val count = service.recompute(lookbackDays = 30, horizonDays = 7)

        assertEquals(2, count) // PRD1 + PRD2
        verify(repo, org.mockito.kotlin.times(2)).save(any())
        verify(events).publishEvent(any<ApplicationEvent>())
    }

    @Test
    fun `blank product ids are skipped`() {
        whenever(invoiceQuery.finalizedBetween(any(), any())).thenReturn(
            listOf(invoice(InvoiceLineProjection("", 5.0, 100.0))),
        )
        assertEquals(0, service.recompute())
        verify(repo, never()).save(any())
        verify(events, never()).publishEvent(any<ApplicationEvent>())
    }

    @Test
    fun `no sales forecasts nothing`() {
        whenever(invoiceQuery.finalizedBetween(any(), any())).thenReturn(emptyList())
        assertEquals(0, service.recompute())
        verify(events, never()).publishEvent(any<ApplicationEvent>())
    }
}
