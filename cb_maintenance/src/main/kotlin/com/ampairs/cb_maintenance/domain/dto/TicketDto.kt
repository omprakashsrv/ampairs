package com.ampairs.cb_maintenance.domain.dto

import com.ampairs.cb_maintenance.domain.model.Ticket
import com.ampairs.cb_maintenance.domain.model.TicketStatus
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class TicketRequest(
    val uid: String? = null,
    @field:NotBlank(message = "Store is required")
    val storeId: String,
    /** Optional on input — the server denormalizes it from the store when blank. */
    val zonalOfficeId: String? = null,
    @field:NotBlank(message = "Asset category is required")
    val assetCategory: String,
    @field:NotBlank(message = "Sub-category is required")
    val subCategory: String,
    /** Link to the ticket_bucket taxonomy leaf this ticket was classified under (for reporting). */
    val ticketBucketId: String? = null,
    val description: String? = null,
    val status: TicketStatus = TicketStatus.OPEN,
    val assignedToEmployeeId: String? = null,
    val assistedByEmployeeIds: List<String>? = null,
    val raisedByEmployeeId: String? = null,
    val raisedAt: Instant? = null,
    val resolvedAt: Instant? = null,
    val originPmEntryId: String? = null,
    val suggestedSparePart: String? = null,
    val active: Boolean = true,
    val refId: String? = null,
)

data class TicketResponse(
    val uid: String,
    val refId: String?,
    val storeId: String,
    val zonalOfficeId: String,
    val assetCategory: String,
    val subCategory: String,
    val ticketBucketId: String?,
    val description: String?,
    val status: TicketStatus,
    val assignedToEmployeeId: String?,
    val assistedByEmployeeIds: List<String>?,
    val raisedByEmployeeId: String?,
    val raisedAt: Instant,
    val resolvedAt: Instant?,
    val originPmEntryId: String?,
    val suggestedSparePart: String?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

/** Body for the reassign action. */
data class ReassignRequest(
    @field:NotBlank(message = "New assignee is required")
    val newAssigneeId: String,
)

/**
 * Applies request fields. Note: `zonalOfficeId` is denormalized by the service from the store when
 * the request omits it; this only copies an explicitly supplied value.
 */
fun Ticket.applyRequest(request: TicketRequest): Ticket = apply {
    request.uid?.let { uid = it }
    storeId = request.storeId.trim()
    request.zonalOfficeId?.takeIf { it.isNotBlank() }?.let { zonalOfficeId = it }
    assetCategory = request.assetCategory.trim()
    subCategory = request.subCategory.trim()
    ticketBucketId = request.ticketBucketId?.takeIf { it.isNotBlank() }
    description = request.description?.trim()
    status = request.status
    assignedToEmployeeId = request.assignedToEmployeeId?.takeIf { it.isNotBlank() }
    assistedByEmployeeIds = request.assistedByEmployeeIds?.takeIf { it.isNotEmpty() }
    raisedByEmployeeId = request.raisedByEmployeeId?.takeIf { it.isNotBlank() }
    request.raisedAt?.let { raisedAt = it }
    resolvedAt = request.resolvedAt
    request.originPmEntryId?.takeIf { it.isNotBlank() }?.let { originPmEntryId = it }
    suggestedSparePart = request.suggestedSparePart?.trim()
    active = request.active
    request.refId?.takeIf { it.isNotBlank() }?.let { refId = it }
}

fun Ticket.asTicketResponse(): TicketResponse = TicketResponse(
    uid = uid,
    refId = refId,
    storeId = storeId,
    zonalOfficeId = zonalOfficeId,
    assetCategory = assetCategory,
    subCategory = subCategory,
    ticketBucketId = ticketBucketId,
    description = description,
    status = status,
    assignedToEmployeeId = assignedToEmployeeId,
    assistedByEmployeeIds = assistedByEmployeeIds,
    raisedByEmployeeId = raisedByEmployeeId,
    raisedAt = raisedAt,
    resolvedAt = resolvedAt,
    originPmEntryId = originPmEntryId,
    suggestedSparePart = suggestedSparePart,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<Ticket>.asTicketResponses(): List<TicketResponse> = map { it.asTicketResponse() }
