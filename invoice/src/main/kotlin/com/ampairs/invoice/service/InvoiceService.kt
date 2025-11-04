package com.ampairs.invoice.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.event.domain.events.InvoiceCreatedEvent
import com.ampairs.event.domain.events.InvoiceStatusChangedEvent
import com.ampairs.event.domain.events.InvoiceUpdatedEvent
import com.ampairs.invoice.domain.dto.InvoiceResponse
import com.ampairs.invoice.domain.dto.toResponse
import com.ampairs.invoice.domain.model.Invoice
import com.ampairs.invoice.domain.model.InvoiceItem
import com.ampairs.invoice.repository.InvoiceItemRepository
import com.ampairs.invoice.repository.InvoicePagingRepository
import com.ampairs.invoice.repository.InvoiceRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import kotlin.jvm.optionals.getOrDefault

@Service
class InvoiceService @Autowired constructor(
    val invoiceRepository: InvoiceRepository,
    val invoiceItemRepository: InvoiceItemRepository,
    val invoicePagingRepository: InvoicePagingRepository,
    val eventPublisher: ApplicationEventPublisher
) {

    /**
     * Helper methods for event publishing
     */
    private fun getWorkspaceId(): String = TenantContextHolder.getCurrentTenant() ?: ""

    private fun getUserId(): String {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.let { AuthenticationHelper.getCurrentUserId(it) } ?: ""
    }

    private fun getDeviceId(): String = DeviceContextHolder.getCurrentDevice() ?: ""

    @Transactional
    fun updateInvoice(invoice: Invoice, invoiceItems: List<InvoiceItem>): InvoiceResponse {
        val existingInvoice = invoiceRepository.findByUid(invoice.uid)
        val isNewInvoice = existingInvoice == null
        val oldStatus = existingInvoice?.status

        invoice.uid = existingInvoice?.uid ?: invoice.uid
        invoice.invoiceNumber = existingInvoice?.invoiceNumber ?: ""
        if (invoice.invoiceNumber.isEmpty()) {
            val invoiceNumber = invoiceRepository.findMaxInvoiceNumber().getOrDefault("0").toIntOrNull() ?: 0
            invoice.invoiceNumber = (invoiceNumber + 1).toString()
        }
        val updatedInvoice = invoiceRepository.save(invoice)
        invoice.uid = updatedInvoice.uid

        invoiceItems.forEach { invoiceItem ->
            if (invoiceItem.uid.isNotEmpty()) {
                val existingInvoiceItem = invoiceItemRepository.findByUid(invoiceItem.uid)
                invoiceItem.id = existingInvoiceItem?.id ?: 0
            }
            invoiceItem.invoiceId = invoice.uid
            invoiceItemRepository.save(invoiceItem)
        }

        // Publish events
        if (isNewInvoice) {
            eventPublisher.publishEvent(
                InvoiceCreatedEvent(
                    source = this,
                    workspaceId = getWorkspaceId(),
                    entityId = updatedInvoice.uid,
                    userId = getUserId(),
                    deviceId = getDeviceId(),
                    invoiceNumber = updatedInvoice.invoiceNumber,
                    customerName = updatedInvoice.toCustomerName,
                    totalAmount = updatedInvoice.totalCost
                )
            )
        } else {
            eventPublisher.publishEvent(
                InvoiceUpdatedEvent(
                    source = this,
                    workspaceId = getWorkspaceId(),
                    entityId = updatedInvoice.uid,
                    userId = getUserId(),
                    deviceId = getDeviceId(),
                    fieldChanges = mapOf("invoice" to "updated", "items" to invoiceItems.size)
                )
            )

            // Publish status changed event if status changed
            if (oldStatus != null && oldStatus != updatedInvoice.status) {
                eventPublisher.publishEvent(
                    InvoiceStatusChangedEvent(
                        source = this,
                        workspaceId = getWorkspaceId(),
                        entityId = updatedInvoice.uid,
                        userId = getUserId(),
                        deviceId = getDeviceId(),
                        invoiceNumber = updatedInvoice.invoiceNumber,
                        oldStatus = oldStatus.name,
                        newStatus = updatedInvoice.status.name
                    )
                )
            }
        }

        return invoice.toResponse(invoiceItems)
    }

    @Transactional(readOnly = true)
    fun getInvoices(lastUpdated: Instant?): List<Invoice> {
        val invoices =
            invoicePagingRepository.findAllByUpdatedAtGreaterThanEqual(
                lastUpdated ?: Instant.MIN, PageRequest.of(0, 50, Sort.by("lastUpdated").ascending())
            )
        return invoices
    }

    fun getInvoice(uid: String): Invoice? {
        return invoiceRepository.findByUid(uid = uid)
    }


}