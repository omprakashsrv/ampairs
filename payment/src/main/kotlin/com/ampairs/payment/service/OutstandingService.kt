package com.ampairs.payment.service

import com.ampairs.customer.domain.service.CustomerService
import com.ampairs.invoice.service.InvoiceService
import com.ampairs.payment.config.Constants
import com.ampairs.payment.domain.dto.OpenBillResponse
import com.ampairs.payment.domain.enums.AllocationTarget
import com.ampairs.payment.repository.PaymentAllocationRepository
import com.ampairs.setting.service.SettingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant

/**
 * Open-bills / outstanding (US1/US4, FR-016/017). For each finalized invoice of the party:
 * `outstanding = total − Σ active allocations`; `dueDate = invoiceDate + customer.creditDays`;
 * aging bucket from days overdue.
 */
@Service
@Transactional(readOnly = true)
class OutstandingService(
    private val allocationRepository: PaymentAllocationRepository,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService,
    private val settingService: SettingService,
) {

    fun openBills(partyUid: String, asOf: Instant = Instant.now()): List<OpenBillResponse> {
        val creditDays = customerService.getCustomerByUid(partyUid)?.creditDays ?: 0
        val buckets = AgingBuckets.parse(settingService.getString("payment", "aging_buckets"))

        // Discover the bills this party has been billed for via the SALES_INVOICE ledger entries'
        // source uids; resolve each invoice for its total/date/number.
        val billUids = invoiceService.getInvoices(null)
            .filter { it.customerId == partyUid }
            .map { it.uid }
            .distinct()

        val result = mutableListOf<OpenBillResponse>()
        for (billUid in billUids) {
            val invoice = invoiceService.getInvoice(billUid) ?: continue
            val total = BigDecimal.valueOf(invoice.totalCost)
            val allocated = allocationRepository.allocatedTotalForTarget(AllocationTarget.INVOICE, billUid)
            val outstanding = total.subtract(allocated).max(BigDecimal.ZERO)
            if (outstanding.signum() <= 0) continue
            val dueDate = invoice.invoiceDate.plus(Duration.ofDays(creditDays.toLong()))
            val daysOverdue = Duration.between(dueDate, asOf).toDays().coerceAtLeast(0)
            result.add(
                OpenBillResponse(
                    billUid = billUid,
                    billNo = invoice.invoiceNumber,
                    billDate = invoice.invoiceDate,
                    total = total,
                    allocated = allocated,
                    outstanding = outstanding,
                    dueDate = dueDate,
                    daysOverdue = daysOverdue,
                    agingBucket = buckets.classify(daysOverdue),
                ),
            )
        }
        return result.sortedBy { it.billDate }
    }

    /** Module code constant exposed for reuse. */
    fun moduleCode(): String = Constants.MODULE_CODE
}
