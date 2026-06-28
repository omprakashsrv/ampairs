package com.ampairs.analytics.service

import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.enums.TaxKind
import com.ampairs.analytics.domain.model.KpiDailySummary
import com.ampairs.analytics.repository.KpiDailySummaryRepository
import com.ampairs.invoice.domain.dto.FinalizedInvoiceProjection
import com.ampairs.invoice.domain.dto.InvoiceLineProjection
import com.ampairs.invoice.domain.dto.TaxLineProjection
import com.ampairs.invoice.service.InvoiceAnalyticsQueryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Unit tests for the invoice-driven KPI reconcile. No DB — the summary repository, timezone provider,
 * and invoice query port are mocked. Testcontainers DB tests (T015/T016/T016a) are deferred to CI
 * (no Docker in the build sandbox).
 */
class KpiRollupServiceTest {

    private val repo = mock<KpiDailySummaryRepository>()
    private val invoiceQuery = mock<InvoiceAnalyticsQueryService>()

    private fun serviceInZone(zone: ZoneId): KpiRollupService {
        val tz = mock<BusinessTimeZoneProvider> { on { currentZone() } doReturn zone }
        return KpiRollupService(repo, tz, invoiceQuery)
    }

    @Test
    fun `buckets a late-night sale into the business day, not UTC (R7)`() {
        val service = serviceInZone(ZoneId.of("Asia/Kolkata"))
        val epoch = Instant.parse("2026-06-30T18:30:00Z").toEpochMilli()
        assertEquals(LocalDate.of(2026, 7, 1), service.businessDateOf(epoch))
    }

    @Test
    fun `same instant buckets to June in UTC`() {
        val service = serviceInZone(ZoneOffset.UTC)
        val epoch = Instant.parse("2026-06-30T18:30:00Z").toEpochMilli()
        assertEquals(LocalDate.of(2026, 6, 30), service.businessDateOf(epoch))
    }

    @Test
    fun `reconcile rebuilds SALES, TOP_CUSTOMER and GST buckets from finalized invoices`() {
        val service = serviceInZone(ZoneOffset.UTC)
        val day = LocalDate.of(2026, 6, 15)
        val epoch = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val invoice = FinalizedInvoiceProjection(
            invoiceDateEpochMillis = epoch,
            gross = 1180.0, net = 1000.0, tax = 180.0,
            customerId = "CUST1", intraState = true,
            taxLines = listOf(TaxLineProjection(rate = 18.0, taxValue = 180.0)),
            lines = listOf(InvoiceLineProjection(productId = "PRD1", qty = 4.0, gross = 1180.0)),
        )
        whenever(invoiceQuery.finalizedBetween(any(), any())).thenReturn(listOf(invoice))
        whenever(repo.saveAll(any<Iterable<KpiDailySummary>>()))
            .thenAnswer { @Suppress("UNCHECKED_CAST") (it.arguments[0] as Iterable<KpiDailySummary>).toList() }

        val written = service.reconcile(day, day)

        // old buckets cleared first (idempotent rebuild)
        verify(repo).deleteByMetricGroupInAndBusinessDateBetween(any(), eq(day), eq(day))

        val captor = argumentCaptor<Iterable<KpiDailySummary>>()
        verify(repo).saveAll(captor.capture())
        val saved = captor.firstValue.toList()
        assertEquals(4, written)

        val sales = saved.single { it.metricGroup == MetricGroup.SALES }
        assertEquals(0, BigDecimal.valueOf(1180.0).compareTo(sales.grossAmount))
        assertEquals(0, BigDecimal.valueOf(1000.0).compareTo(sales.netAmount))
        assertEquals(0, BigDecimal.valueOf(180.0).compareTo(sales.taxAmount))
        assertEquals(1, sales.docCount)

        val cust = saved.single { it.metricGroup == MetricGroup.TOP_CUSTOMER }
        assertEquals("CUST1", cust.dimCustomerId)
        assertEquals(0, BigDecimal.valueOf(1180.0).compareTo(cust.grossAmount))

        val prod = saved.single { it.metricGroup == MetricGroup.TOP_PRODUCT }
        assertEquals("PRD1", prod.dimProductId)
        assertEquals(0, BigDecimal.valueOf(1180.0).compareTo(prod.grossAmount))
        assertEquals(0, BigDecimal.valueOf(4.0).compareTo(prod.qty))

        val gst = saved.single { it.metricGroup == MetricGroup.GST_SUMMARY }
        assertEquals(TaxKind.INTRA, gst.taxKind)
        assertNotNull(gst.taxRate)
        assertEquals(0, BigDecimal.valueOf(18.0).compareTo(gst.taxRate))
        assertEquals(0, BigDecimal.valueOf(180.0).compareTo(gst.taxAmount))
        // taxable implied by rate: 180 / 0.18 = 1000
        assertEquals(0, BigDecimal.valueOf(1000.0).compareTo(gst.grossAmount))
    }
}
