package com.ampairs.cb_maintenance.domain.dto

import com.ampairs.cb_maintenance.domain.model.ChecklistItemResult
import com.ampairs.cb_maintenance.domain.model.PmEntry
import com.ampairs.cb_maintenance.domain.model.PmEntrySource
import com.ampairs.cb_maintenance.domain.model.PmEntryStatus
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class PmEntryRequest(
    val uid: String? = null,
    @field:NotBlank(message = "Store is required")
    val storeId: String,
    val zonalOfficeId: String? = null,
    @field:NotBlank(message = "Asset category is required")
    val assetCategory: String,
    val pmScheduleId: String? = null,
    val source: PmEntrySource = PmEntrySource.SCHEDULED,
    val dueDate: Instant? = null,
    val status: PmEntryStatus = PmEntryStatus.DUE,
    val assignedToEmployeeId: String? = null,
    val assistedByEmployeeIds: List<String>? = null,
    val completedAt: Instant? = null,
    val completedByEmployeeId: String? = null,
    val checklistResult: List<ChecklistItemResult>? = null,
    val ticketId: String? = null,
    val active: Boolean = true,
    val refId: String? = null,
)

data class PmEntryResponse(
    val uid: String,
    val refId: String?,
    val storeId: String,
    val zonalOfficeId: String,
    val assetCategory: String,
    val pmScheduleId: String?,
    val source: PmEntrySource,
    val dueDate: Instant,
    val status: PmEntryStatus,
    val assignedToEmployeeId: String?,
    val assistedByEmployeeIds: List<String>?,
    val completedAt: Instant?,
    val completedByEmployeeId: String?,
    val checklistResult: List<ChecklistItemResult>?,
    val ticketId: String?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

/** Body for the complete-PM action. */
data class CompletePmEntryRequest(
    val checklistResult: List<ChecklistItemResult>? = null,
    val note: String? = null,
)

/**
 * Applies request fields. `zonalOfficeId` is denormalized by the service from the store when the
 * request omits it; this only copies an explicitly supplied value.
 */
fun PmEntry.applyRequest(request: PmEntryRequest): PmEntry = apply {
    request.uid?.let { uid = it }
    storeId = request.storeId.trim()
    request.zonalOfficeId?.takeIf { it.isNotBlank() }?.let { zonalOfficeId = it }
    assetCategory = request.assetCategory.trim()
    pmScheduleId = request.pmScheduleId?.takeIf { it.isNotBlank() }
    source = request.source
    request.dueDate?.let { dueDate = it }
    status = request.status
    assignedToEmployeeId = request.assignedToEmployeeId?.takeIf { it.isNotBlank() }
    assistedByEmployeeIds = request.assistedByEmployeeIds?.takeIf { it.isNotEmpty() }
    completedAt = request.completedAt
    completedByEmployeeId = request.completedByEmployeeId?.takeIf { it.isNotBlank() }
    checklistResult = request.checklistResult?.takeIf { it.isNotEmpty() }
    ticketId = request.ticketId?.takeIf { it.isNotBlank() }
    active = request.active
    request.refId?.takeIf { it.isNotBlank() }?.let { refId = it }
}

fun PmEntry.asPmEntryResponse(): PmEntryResponse = PmEntryResponse(
    uid = uid,
    refId = refId,
    storeId = storeId,
    zonalOfficeId = zonalOfficeId,
    assetCategory = assetCategory,
    pmScheduleId = pmScheduleId,
    source = source,
    dueDate = dueDate,
    status = status,
    assignedToEmployeeId = assignedToEmployeeId,
    assistedByEmployeeIds = assistedByEmployeeIds,
    completedAt = completedAt,
    completedByEmployeeId = completedByEmployeeId,
    checklistResult = checklistResult,
    ticketId = ticketId,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<PmEntry>.asPmEntryResponses(): List<PmEntryResponse> = map { it.asPmEntryResponse() }
