package com.ampairs.event.domain.events

/**
 * Event published when an invoice is created
 */
class InvoiceCreatedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val invoiceNumber: String,
    val customerName: String,
    val totalAmount: Double
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "created",
            "invoiceNumber" to invoiceNumber,
            "customerName" to customerName,
            "totalAmount" to totalAmount
        )
    }
}

/**
 * Event published when an invoice is updated
 */
class InvoiceUpdatedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val fieldChanges: Map<String, Any>
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "updated",
            "changes" to fieldChanges
        )
    }
}

/**
 * Event published when an invoice is paid
 */
class InvoicePaidEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val invoiceNumber: String,
    val paidAmount: Double,
    val paymentMethod: String
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "paid",
            "invoiceNumber" to invoiceNumber,
            "paidAmount" to paidAmount,
            "paymentMethod" to paymentMethod
        )
    }
}

/**
 * Event published when an invoice status changes
 */
class InvoiceStatusChangedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val invoiceNumber: String,
    val oldStatus: String,
    val newStatus: String
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "status_changed",
            "invoiceNumber" to invoiceNumber,
            "oldStatus" to oldStatus,
            "newStatus" to newStatus
        )
    }
}

/**
 * Event published when an invoice reaches the finalized (INVOICED) state. Consumed by the
 * `payment` module to post the SALES_INVOICE ledger entry (LDG_<invoice.uid>). DRAFT/NEW
 * invoices never publish this (FR-013).
 */
class InvoiceFinalizedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val invoiceNumber: String,
    val customerUid: String,
    val totalAmount: Double,
    val invoiceDateEpochMillis: Long,
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "finalized",
            "invoiceNumber" to invoiceNumber,
            "customerUid" to customerUid,
            "totalAmount" to totalAmount
        )
    }
}

/**
 * Event published when a finalized invoice is cancelled/de-finalized (leaves INVOICED). Consumed by
 * the `payment` module to reverse its SALES_INVOICE ledger entry (audit retained — FR-014).
 */
class InvoiceCancelledEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val invoiceNumber: String,
    val customerUid: String,
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "cancelled",
            "invoiceNumber" to invoiceNumber,
            "customerUid" to customerUid
        )
    }
}

/**
 * Event published when an invoice is deleted
 */
class InvoiceDeletedEvent(
    source: Any,
    override val workspaceId: String,
    override val entityId: String,
    override val userId: String,
    override val deviceId: String,
    val invoiceNumber: String
) : BaseEntityEvent(source, workspaceId, entityId, userId, deviceId) {

    override fun getChanges(): Map<String, Any> {
        return mapOf(
            "action" to "deleted",
            "invoiceNumber" to invoiceNumber
        )
    }
}
