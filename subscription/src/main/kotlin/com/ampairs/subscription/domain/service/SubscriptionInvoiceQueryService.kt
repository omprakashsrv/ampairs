package com.ampairs.subscription.domain.service

import com.ampairs.subscription.domain.dto.InvoiceResponse
import com.ampairs.subscription.domain.dto.InvoiceSummaryResponse
import com.ampairs.subscription.domain.dto.asInvoiceResponse
import com.ampairs.subscription.domain.model.Invoice
import com.ampairs.subscription.domain.model.InvoiceStatus
import com.ampairs.subscription.domain.repository.SubscriptionInvoiceRepository
import com.ampairs.subscription.exception.SubscriptionException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SubscriptionInvoiceQueryService(
    private val subscriptionInvoiceRepository: SubscriptionInvoiceRepository
) {

    // Map to DTO inside the transaction so lazy lineItems batch-load while the session is open.
    @Transactional(readOnly = true)
    fun getInvoicesForWorkspace(workspaceId: String, status: InvoiceStatus?, pageable: Pageable): Page<InvoiceResponse> {
        val page = if (status != null) {
            subscriptionInvoiceRepository.findByWorkspaceIdAndStatusOrderByCreatedAtDesc(workspaceId, status, pageable)
        } else {
            subscriptionInvoiceRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, pageable)
        }
        return page.map { it.asInvoiceResponse() }
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
