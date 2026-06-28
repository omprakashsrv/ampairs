package com.ampairs.analytics.service

import com.ampairs.analytics.domain.enums.MetricGroup
import com.ampairs.analytics.domain.enums.Period
import com.ampairs.analytics.domain.enums.TaxKind
import com.ampairs.analytics.domain.model.KpiDailySummary
import com.ampairs.analytics.repository.KpiDailySummaryRepository
import com.ampairs.business.service.BusinessService
import com.ampairs.inventory.service.InventoryAnalyticsQueryService
import com.ampairs.payment.domain.dto.AgingSlice
import com.ampairs.payment.domain.dto.CollectionsAgingProjection
import com.ampairs.payment.service.PaymentAnalyticsQueryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Unit tests for the dashboard read logic (GST split, top-N ordering, trend bucketing) over a mocked
 * summary repository. Currency resolution is forced down its UTC/"INR" fallback (the business service
 * throws) to avoid constructing a JPA entity.
 */
class DashboardReadServiceTest {

    private val repo = mock<KpiDailySummaryRepository>()
    private val business = mock<BusinessService> { on { getBusinessProfile() } doThrow RuntimeException("no profile") }
    private val payment = mock<PaymentAnalyticsQueryService>()
    private val inventory = mock<InventoryAnalyticsQueryService>()
    private val tz = mock<BusinessTimeZoneProvider> { on { currentZone() } doReturn ZoneOffset.UTC }
    private val service = DashboardReadService(repo, business, payment, inventory, tz)

    private fun row(
        group: MetricGroup, date: LocalDate, gross: Double = 0.0, net: Double = 0.0, tax: Double = 0.0,
        count: Int = 0, customerId: String = "", rate: Double? = null, kind: TaxKind? = null,
    ) = KpiDailySummary().apply {
        metricGroup = group
        businessDate = date
        grossAmount = BigDecimal.valueOf(gross)
        netAmount = BigDecimal.valueOf(net)
        taxAmount = BigDecimal.valueOf(tax)
        docCount = count
        dimCustomerId = customerId
        rate?.let { taxRate = BigDecimal.valueOf(it) }
        taxKind = kind
    }

    @Test
    fun `gst summary splits intra into CGST+SGST and inter into IGST`() {
        val from = LocalDate.of(2026, 6, 1); val to = LocalDate.of(2026, 6, 30)
        whenever(repo.findByMetricGroupAndBusinessDateBetween(eq(MetricGroup.GST_SUMMARY), any(), any()))
            .thenReturn(
                listOf(
                    row(MetricGroup.GST_SUMMARY, LocalDate.of(2026, 6, 10), gross = 1000.0, tax = 180.0, rate = 18.0, kind = TaxKind.INTRA),
                    row(MetricGroup.GST_SUMMARY, LocalDate.of(2026, 6, 12), gross = 1000.0, tax = 50.0, rate = 5.0, kind = TaxKind.INTER),
                ),
            )

        val r = service.gstSummary(from, to)

        assertEquals(0, BigDecimal.valueOf(230.0).compareTo(r.totalTax))
        assertEquals(0, BigDecimal.valueOf(90.0).compareTo(r.intraState.cgst))
        assertEquals(0, BigDecimal.valueOf(90.0).compareTo(r.intraState.sgst))
        assertEquals(0, BigDecimal.valueOf(50.0).compareTo(r.interState.igst))
        assertEquals(2, r.byRate.size)
        // sorted ascending by rate → 5% first
        assertEquals(0, BigDecimal.valueOf(5.0).compareTo(r.byRate.first().taxRate))
        assertEquals("INR", r.currencyCode)
    }

    @Test
    fun `top customers are ranked by gross descending`() {
        val from = LocalDate.of(2026, 6, 1); val to = LocalDate.of(2026, 6, 30)
        whenever(repo.findByMetricGroupAndBusinessDateBetween(eq(MetricGroup.TOP_CUSTOMER), any(), any()))
            .thenReturn(
                listOf(
                    row(MetricGroup.TOP_CUSTOMER, from, gross = 600.0, count = 1, customerId = "CUST1"),
                    row(MetricGroup.TOP_CUSTOMER, to, gross = 400.0, count = 1, customerId = "CUST1"),
                    row(MetricGroup.TOP_CUSTOMER, from, gross = 2000.0, count = 1, customerId = "CUST2"),
                ),
            )

        val top = service.top("customer", from, to, 5)

        assertEquals(2, top.size)
        assertEquals("CUST2", top[0].id); assertEquals(1, top[0].rank)
        assertEquals("CUST1", top[1].id); assertEquals(2, top[1].rank)
        assertEquals(0, BigDecimal.valueOf(1000.0).compareTo(top[1].grossAmount)) // 600 + 400
    }

    @Test
    fun `collections kpis read collected and outstanding live from payment`() {
        val from = LocalDate.of(2026, 6, 1); val to = LocalDate.of(2026, 6, 30)
        whenever(payment.collectedBetween(any(), any())).thenReturn(BigDecimal.valueOf(12000.0))
        whenever(payment.collectionsAging(any()))
            .thenReturn(CollectionsAgingProjection(BigDecimal.valueOf(45500.0), emptyList()))

        val r = service.kpis(MetricGroup.COLLECTIONS, from, to, Period.MONTH)

        val collected = r.values.single { it.metricId == "collections.collected" }
        val outstanding = r.values.single { it.metricId == "collections.outstanding" }
        assertEquals(0, BigDecimal.valueOf(12000.0).compareTo(collected.value))
        assertEquals(0, BigDecimal.valueOf(45500.0).compareTo(outstanding.value))
    }

    @Test
    fun `inventory kpis read stock value, low-stock count and turns live from product`() {
        whenever(inventory.totalStockValue()).thenReturn(BigDecimal.valueOf(250000.0))
        whenever(inventory.lowStockCount()).thenReturn(7L)
        whenever(inventory.inventoryTurns(any(), any())).thenReturn(BigDecimal.valueOf(1.250))

        val r = service.kpis(MetricGroup.INVENTORY, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), Period.MONTH)

        assertEquals(0, BigDecimal.valueOf(250000.0).compareTo(r.values.single { it.metricId == "inventory.stock_value" }.value))
        assertEquals(0, BigDecimal.valueOf(7).compareTo(r.values.single { it.metricId == "inventory.low_stock_count" }.value))
        assertEquals(0, BigDecimal.valueOf(1.250).compareTo(r.values.single { it.metricId == "inventory.turns" }.value))
    }

    @Test
    fun `aging maps payment buckets and total outstanding`() {
        whenever(payment.collectionsAging(any())).thenReturn(
            CollectionsAgingProjection(
                totalOutstanding = BigDecimal.valueOf(45500.0),
                buckets = listOf(AgingSlice("0-30", BigDecimal.valueOf(18000.0)), AgingSlice("90+", BigDecimal.valueOf(2000.0))),
            ),
        )

        val r = service.aging(LocalDate.of(2026, 6, 27))

        assertEquals(LocalDate.of(2026, 6, 27), r.asOfDate)
        assertEquals(0, BigDecimal.valueOf(45500.0).compareTo(r.totalOutstanding))
        assertEquals(2, r.buckets.size)
        assertEquals("0-30", r.buckets[0].bucket)
        assertEquals(0, BigDecimal.valueOf(18000.0).compareTo(r.buckets[0].amount))
    }

    @Test
    fun `trend buckets SALES gross by month`() {
        whenever(repo.findByMetricGroupAndBusinessDateBetween(eq(MetricGroup.SALES), any(), any()))
            .thenReturn(
                listOf(
                    row(MetricGroup.SALES, LocalDate.of(2026, 5, 10), gross = 100.0),
                    row(MetricGroup.SALES, LocalDate.of(2026, 5, 20), gross = 50.0),
                    row(MetricGroup.SALES, LocalDate.of(2026, 6, 5), gross = 200.0),
                ),
            )

        val trend = service.trend("sales.gross", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 30), Period.MONTH)

        assertEquals(2, trend.size)
        assertEquals(LocalDate.of(2026, 5, 1), trend[0].bucketStart)
        assertEquals(0, BigDecimal.valueOf(150.0).compareTo(trend[0].value))
        assertEquals(0, BigDecimal.valueOf(200.0).compareTo(trend[1].value))
    }
}
