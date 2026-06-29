package com.ampairs.sfa.domain.dto

import com.ampairs.sfa.domain.model.Leave
import com.ampairs.sfa.domain.model.VisitSurveyResponse
import jakarta.validation.constraints.NotBlank
import java.time.Instant

// ──────────────────────────── Leave (manager CRUD) ────────────────────────────

data class LeaveRequest(
    val uid: String? = null,
    val repMemberUid: String? = null,
    val leaveDate: Instant? = null,
    val reason: String? = null,
    val markedBy: String? = null,
    val active: Boolean? = null,
)

data class LeaveResponse(
    val uid: String,
    val repMemberUid: String,
    val leaveDate: Instant,
    val reason: String?,
    val markedBy: String?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun LeaveRequest.toEntity(): Leave = Leave().apply {
    uid = this@toEntity.uid ?: ""
    repMemberUid = this@toEntity.repMemberUid ?: ""
    leaveDate = this@toEntity.leaveDate ?: Instant.EPOCH
    reason = this@toEntity.reason
    markedBy = this@toEntity.markedBy
    active = this@toEntity.active ?: true
}

fun Leave.asResponse(): LeaveResponse = LeaveResponse(
    uid = uid, repMemberUid = repMemberUid, leaveDate = leaveDate, reason = reason,
    markedBy = markedBy, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

// ──────────────────────────── VisitSurveyResponse (offline /sync) ────────────────────────────

data class VisitSurveyResponseRequest(
    val uid: String? = null,
    @field:NotBlank(message = "visit_uid is required")
    val visitUid: String? = null,
    val repMemberUid: String? = null,
    val responses: String? = null,
    val active: Boolean? = null,
)

data class VisitSurveyResponseResponse(
    val uid: String,
    val visitUid: String,
    val repMemberUid: String?,
    val responses: String?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun VisitSurveyResponseRequest.toEntity(): VisitSurveyResponse = VisitSurveyResponse().apply {
    uid = this@toEntity.uid ?: ""
    visitUid = this@toEntity.visitUid ?: ""
    repMemberUid = this@toEntity.repMemberUid
    responses = this@toEntity.responses
    active = this@toEntity.active ?: true
}

fun VisitSurveyResponse.asResponse(): VisitSurveyResponseResponse = VisitSurveyResponseResponse(
    uid = uid, visitUid = visitUid, repMemberUid = repMemberUid, responses = responses,
    active = active, createdAt = createdAt, updatedAt = updatedAt,
)

// ──────────────────────────── Read-models ────────────────────────────

/** Attendance summary for a rep over a period (FR-AS1–7). Hours are wall-clock check-in→check-out. */
data class AttendanceSummaryResponse(
    val repMemberUid: String,
    val daysPresent: Int,
    val totalWorkingHours: Double,
    val openDays: Int,
    val leaveDays: Int,
)

/** Visit productivity for a rep over a period (FR-VP1–7). */
data class VisitProductivityResponse(
    val repMemberUid: String,
    val totalVisits: Int,
    val productiveVisits: Int,
    val productivePercent: Double,
    val uniqueOutlets: Int,
    val adHocVisits: Int,
)
