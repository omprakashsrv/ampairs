package com.ampairs.dms.domain

import com.ampairs.invoice.domain.enums.InvoiceStatus
import com.ampairs.invoice.domain.model.Invoice
import java.math.BigDecimal
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Pure mapping of a distributor's finalized invoices to secondary-sales [SaleLine]s. Only INVOICED
 * invoices count (DRAFT/NEW are excluded); each invoice line → one SaleLine keyed to its product +
 * the invoice's customer (retailer). Period is the invoice month (UTC; business-tz refinement later).
 */
object InvoiceSaleLineMapper {

    fun toSaleLines(distributorWorkspaceId: String, invoices: List<Invoice>): List<SaleLine> =
        invoices.asSequence()
            .filter { it.status == InvoiceStatus.INVOICED }
            .flatMap { invoice ->
                val period = YearMonth.from(invoice.invoiceDate.atZone(ZoneOffset.UTC)).toString()
                val customerUid = invoice.customerId ?: ""
                invoice.invoiceItems.asSequence().map { item ->
                    SaleLine(
                        distributorWorkspaceId = distributorWorkspaceId,
                        productUid = item.productId,
                        customerUid = customerUid,
                        quantity = item.quantity,
                        value = BigDecimal.valueOf(item.totalCost),
                        periodKey = period,
                    )
                }
            }
            .toList()
}
