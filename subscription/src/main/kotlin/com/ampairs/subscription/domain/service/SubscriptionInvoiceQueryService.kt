package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.dto.InvoiceSummaryResponse
import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.domain.repository.SubscriptionInvoiceRepository
import com.ampairs.subscription.exception.SubscriptionException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class SubscriptionInvoiceQueryService(
    private val subscriptionInvoiceRepository: SubscriptionInvoiceRepository
) {

    fun getInvoicesForWorkspace(workspaceId: String, status: InvoiceStatus?, pageable: Pageable): Page<Invoice> {
        return if (status != null) {
            val filtered = subscriptionInvoiceRepository.findByStatus(status)
                .filter { it.workspaceId == workspaceId }
            PageImpl(filtered, pageable, filtered.size.toLong())
        } else {
            subscriptionInvoiceRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, pageable)
        }
    }

    fun getInvoice(uid: String): Invoice =
        subscriptionInvoiceRepository.findByUid(uid)
            ?: throw SubscriptionException.InvoiceNotFound(uid)

    fun getSummary(workspaceId: String): InvoiceSummaryResponse {
        val allInvoices = subscriptionInvoiceRepository.findByWorkspaceId(workspaceId)
        val pendingInvoices = allInvoices.filter {
            it.status in listOf(InvoiceStatus.PENDING, InvoiceStatus.OVERDUE, InvoiceStatus.PARTIALLY_PAID)
        }
        val overdueInvoices = allInvoices.filter { it.status == InvoiceStatus.OVERDUE || it.isOverdue() }
        val totalOutstanding = pendingInvoices.sumOf { it.getRemainingBalance() }
        val nextInvoice = pendingInvoices
            .filter { !it.isOverdue() }
            .minByOrNull { it.dueDate }

        return InvoiceSummaryResponse(
            totalInvoices = allInvoices.size,
            pendingInvoices = pendingInvoices.size,
            overdueInvoices = overdueInvoices.size,
            totalOutstanding = totalOutstanding,
            nextDueDate = nextInvoice?.dueDate,
            nextInvoiceAmount = nextInvoice?.totalAmount
        )
    }
}
