package com.ampairs.cb_maintenance.domain.dto

import com.ampairs.cb_maintenance.domain.model.FrequencyUnit
import com.ampairs.cb_maintenance.domain.model.PmSchedule
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class PmScheduleRequest(
    val uid: String? = null,
    /** Taxonomy department (denormalized alongside [assetCategory]). */
    val department: String = "",
    @field:NotBlank(message = "Asset category is required")
    val assetCategory: String,
    /** Exact ticket_bucket taxonomy leaf (Department › Category › Issue [› Issue-detail]). */
    val ticketBucketId: String? = null,
    @field:NotBlank(message = "Task name is required")
    val taskName: String,
    val checklist: List<String>? = null,
    val frequencyUnit: FrequencyUnit = FrequencyUnit.MONTH,
    @field:Min(value = 1, message = "Frequency interval must be at least 1")
    val frequencyInterval: Int = 1,
    val active: Boolean = true,
    val refId: String? = null,
)

data class PmScheduleResponse(
    val uid: String,
    val refId: String?,
    val department: String,
    val assetCategory: String,
    val ticketBucketId: String?,
    val taskName: String,
    val checklist: List<String>?,
    val frequencyUnit: FrequencyUnit,
    val frequencyInterval: Int,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun PmSchedule.applyRequest(request: PmScheduleRequest): PmSchedule = apply {
    request.uid?.let { uid = it }
    department = request.department.trim()
    assetCategory = request.assetCategory.trim()
    ticketBucketId = request.ticketBucketId?.trim()?.takeIf { it.isNotBlank() }
    taskName = request.taskName.trim()
    checklist = request.checklist?.takeIf { it.isNotEmpty() }
    frequencyUnit = request.frequencyUnit
    frequencyInterval = request.frequencyInterval
    active = request.active
    request.refId?.takeIf { it.isNotBlank() }?.let { refId = it }
}

fun PmSchedule.asPmScheduleResponse(): PmScheduleResponse = PmScheduleResponse(
    uid = uid,
    refId = refId,
    department = department,
    assetCategory = assetCategory,
    ticketBucketId = ticketBucketId,
    taskName = taskName,
    checklist = checklist,
    frequencyUnit = frequencyUnit,
    frequencyInterval = frequencyInterval,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
