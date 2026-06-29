package com.ampairs.sfa.domain.dto

import com.ampairs.sfa.domain.enums.AttendanceStatus
import com.ampairs.sfa.domain.enums.GeoFenceStatus
import com.ampairs.sfa.domain.enums.PlannedVisitStatus
import com.ampairs.sfa.domain.enums.VisitOutcome
import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.domain.model.Beat
import com.ampairs.sfa.domain.model.BeatOutlet
import com.ampairs.sfa.domain.model.FieldOrder
import com.ampairs.sfa.domain.model.JourneyPlan
import com.ampairs.sfa.domain.model.PlannedVisit
import com.ampairs.sfa.domain.model.Visit
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.Instant

/*
 * Offline-sync DTOs for the SFA module. Requests carry the client-generated `uid` (UID-keyed
 * upsert) and the soft-delete `active` flag so deletions ride in-band. Responses expose only
 * client-relevant fields plus audit timestamps.
 */

// ──────────────────────────── Beat ────────────────────────────

data class BeatRequest(
    val uid: String? = null,
    @field:NotBlank(message = "Beat name is required")
    val name: String? = null,
    val description: String? = null,
    val repMemberUid: String? = null,
    val scheduledDays: String? = null,
    val active: Boolean? = null,
)

data class BeatResponse(
    val uid: String,
    val name: String,
    val description: String?,
    val repMemberUid: String?,
    val scheduledDays: String?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun BeatRequest.toEntity(): Beat = Beat().apply {
    uid = this@toEntity.uid ?: ""
    name = this@toEntity.name ?: ""
    description = this@toEntity.description
    repMemberUid = this@toEntity.repMemberUid
    scheduledDays = this@toEntity.scheduledDays
    active = this@toEntity.active ?: true
}

fun Beat.asResponse(): BeatResponse = BeatResponse(
    uid = uid, name = name, description = description, repMemberUid = repMemberUid,
    scheduledDays = scheduledDays, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

// ──────────────────────────── BeatOutlet ────────────────────────────

data class BeatOutletRequest(
    val uid: String? = null,
    @field:NotBlank(message = "beat_uid is required")
    val beatUid: String? = null,
    @field:NotBlank(message = "customer_uid is required")
    val customerUid: String? = null,
    val visitSequence: Int? = null,
    val visitDay: String? = null,
    val active: Boolean? = null,
)

data class BeatOutletResponse(
    val uid: String,
    val beatUid: String,
    val customerUid: String,
    val visitSequence: Int,
    val visitDay: String?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun BeatOutletRequest.toEntity(): BeatOutlet = BeatOutlet().apply {
    uid = this@toEntity.uid ?: ""
    beatUid = this@toEntity.beatUid ?: ""
    customerUid = this@toEntity.customerUid ?: ""
    visitSequence = this@toEntity.visitSequence ?: 0
    visitDay = this@toEntity.visitDay
    active = this@toEntity.active ?: true
}

fun BeatOutlet.asResponse(): BeatOutletResponse = BeatOutletResponse(
    uid = uid, beatUid = beatUid, customerUid = customerUid, visitSequence = visitSequence,
    visitDay = visitDay, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

// ──────────────────────────── JourneyPlan ────────────────────────────

data class JourneyPlanRequest(
    val uid: String? = null,
    @field:NotBlank(message = "rep_member_uid is required")
    val repMemberUid: String? = null,
    @field:NotBlank(message = "beat_uid is required")
    val beatUid: String? = null,
    val weekday: String? = null,
    val active: Boolean? = null,
)

data class JourneyPlanResponse(
    val uid: String,
    val repMemberUid: String,
    val beatUid: String,
    val weekday: String?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun JourneyPlanRequest.toEntity(): JourneyPlan = JourneyPlan().apply {
    uid = this@toEntity.uid ?: ""
    repMemberUid = this@toEntity.repMemberUid ?: ""
    beatUid = this@toEntity.beatUid ?: ""
    weekday = this@toEntity.weekday
    active = this@toEntity.active ?: true
}

fun JourneyPlan.asResponse(): JourneyPlanResponse = JourneyPlanResponse(
    uid = uid, repMemberUid = repMemberUid, beatUid = beatUid, weekday = weekday,
    active = active, createdAt = createdAt, updatedAt = updatedAt,
)

// ──────────────────────────── PlannedVisit ────────────────────────────

data class PlannedVisitRequest(
    val uid: String? = null,
    val journeyPlanUid: String? = null,
    val beatUid: String? = null,
    @field:NotBlank(message = "customer_uid is required")
    val customerUid: String? = null,
    @field:NotBlank(message = "rep_member_uid is required")
    val repMemberUid: String? = null,
    val plannedDate: Instant? = null,
    val status: PlannedVisitStatus? = null,
    val visitSequence: Int? = null,
    val active: Boolean? = null,
)

data class PlannedVisitResponse(
    val uid: String,
    val journeyPlanUid: String?,
    val beatUid: String?,
    val customerUid: String,
    val repMemberUid: String,
    val plannedDate: Instant,
    val status: PlannedVisitStatus,
    val visitSequence: Int,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun PlannedVisitRequest.toEntity(): PlannedVisit = PlannedVisit().apply {
    uid = this@toEntity.uid ?: ""
    journeyPlanUid = this@toEntity.journeyPlanUid
    beatUid = this@toEntity.beatUid
    customerUid = this@toEntity.customerUid ?: ""
    repMemberUid = this@toEntity.repMemberUid ?: ""
    plannedDate = this@toEntity.plannedDate ?: Instant.EPOCH
    status = this@toEntity.status ?: PlannedVisitStatus.PENDING
    visitSequence = this@toEntity.visitSequence ?: 0
    active = this@toEntity.active ?: true
}

fun PlannedVisit.asResponse(): PlannedVisitResponse = PlannedVisitResponse(
    uid = uid, journeyPlanUid = journeyPlanUid, beatUid = beatUid, customerUid = customerUid,
    repMemberUid = repMemberUid, plannedDate = plannedDate, status = status,
    visitSequence = visitSequence, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

// ──────────────────────────── Visit ────────────────────────────

data class VisitRequest(
    val uid: String? = null,
    @field:NotBlank(message = "customer_uid is required")
    val customerUid: String? = null,
    @field:NotBlank(message = "rep_member_uid is required")
    val repMemberUid: String? = null,
    val plannedVisitUid: String? = null,
    val outcome: VisitOutcome? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val distanceMeters: Double? = null,
    val adHoc: Boolean? = null,
    val notes: String? = null,
    val orderUid: String? = null,
    val visitedAt: Instant? = null,
    val active: Boolean? = null,
)

data class VisitResponse(
    val uid: String,
    val customerUid: String,
    val repMemberUid: String,
    val plannedVisitUid: String?,
    val outcome: VisitOutcome,
    val latitude: Double?,
    val longitude: Double?,
    val distanceMeters: Double?,
    val geoFenceStatus: GeoFenceStatus,
    val adHoc: Boolean,
    val notes: String?,
    val orderUid: String?,
    val visitedAt: Instant,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun VisitRequest.toEntity(): Visit = Visit().apply {
    uid = this@toEntity.uid ?: ""
    customerUid = this@toEntity.customerUid ?: ""
    repMemberUid = this@toEntity.repMemberUid ?: ""
    plannedVisitUid = this@toEntity.plannedVisitUid
    outcome = this@toEntity.outcome ?: VisitOutcome.NO_ORDER
    latitude = this@toEntity.latitude
    longitude = this@toEntity.longitude
    distanceMeters = this@toEntity.distanceMeters
    adHoc = this@toEntity.adHoc ?: false
    notes = this@toEntity.notes
    orderUid = this@toEntity.orderUid
    visitedAt = this@toEntity.visitedAt ?: Instant.EPOCH
    active = this@toEntity.active ?: true
    // geoFenceStatus is computed server-side in VisitService.
}

fun Visit.asResponse(): VisitResponse = VisitResponse(
    uid = uid, customerUid = customerUid, repMemberUid = repMemberUid, plannedVisitUid = plannedVisitUid,
    outcome = outcome, latitude = latitude, longitude = longitude, distanceMeters = distanceMeters,
    geoFenceStatus = geoFenceStatus, adHoc = adHoc, notes = notes, orderUid = orderUid,
    visitedAt = visitedAt, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

// ──────────────────────────── Attendance ────────────────────────────

data class AttendanceRequest(
    val uid: String? = null,
    @field:NotBlank(message = "rep_member_uid is required")
    val repMemberUid: String? = null,
    val checkInAt: Instant? = null,
    val checkInLatitude: Double? = null,
    val checkInLongitude: Double? = null,
    val checkOutAt: Instant? = null,
    val checkOutLatitude: Double? = null,
    val checkOutLongitude: Double? = null,
    val active: Boolean? = null,
)

data class AttendanceResponse(
    val uid: String,
    val repMemberUid: String,
    val checkInAt: Instant?,
    val checkInLatitude: Double?,
    val checkInLongitude: Double?,
    val checkOutAt: Instant?,
    val checkOutLatitude: Double?,
    val checkOutLongitude: Double?,
    val status: AttendanceStatus,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun AttendanceRequest.toEntity(): Attendance = Attendance().apply {
    uid = this@toEntity.uid ?: ""
    repMemberUid = this@toEntity.repMemberUid ?: ""
    checkInAt = this@toEntity.checkInAt
    checkInLatitude = this@toEntity.checkInLatitude
    checkInLongitude = this@toEntity.checkInLongitude
    checkOutAt = this@toEntity.checkOutAt
    checkOutLatitude = this@toEntity.checkOutLatitude
    checkOutLongitude = this@toEntity.checkOutLongitude
    active = this@toEntity.active ?: true
    // status is derived server-side in AttendanceService (OPEN until checkout).
}

fun Attendance.asResponse(): AttendanceResponse = AttendanceResponse(
    uid = uid, repMemberUid = repMemberUid, checkInAt = checkInAt, checkInLatitude = checkInLatitude,
    checkInLongitude = checkInLongitude, checkOutAt = checkOutAt, checkOutLatitude = checkOutLatitude,
    checkOutLongitude = checkOutLongitude, status = status, active = active,
    createdAt = createdAt, updatedAt = updatedAt,
)

// ──────────────────────────── FieldOrder ────────────────────────────

data class FieldOrderRequest(
    val uid: String? = null,
    val visitUid: String? = null,
    @field:NotBlank(message = "customer_uid is required")
    val customerUid: String? = null,
    @field:NotBlank(message = "rep_member_uid is required")
    val repMemberUid: String? = null,
    val orderUid: String? = null,
    val amount: BigDecimal? = null,
    val active: Boolean? = null,
)

data class FieldOrderResponse(
    val uid: String,
    val visitUid: String?,
    val customerUid: String,
    val repMemberUid: String,
    val orderUid: String?,
    val amount: BigDecimal,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun FieldOrderRequest.toEntity(): FieldOrder = FieldOrder().apply {
    uid = this@toEntity.uid ?: ""
    visitUid = this@toEntity.visitUid
    customerUid = this@toEntity.customerUid ?: ""
    repMemberUid = this@toEntity.repMemberUid ?: ""
    orderUid = this@toEntity.orderUid
    amount = this@toEntity.amount ?: BigDecimal.ZERO
    active = this@toEntity.active ?: true
}

fun FieldOrder.asResponse(): FieldOrderResponse = FieldOrderResponse(
    uid = uid, visitUid = visitUid, customerUid = customerUid, repMemberUid = repMemberUid,
    orderUid = orderUid, amount = amount, active = active, createdAt = createdAt, updatedAt = updatedAt,
)

// ──────────────────────────── Adherence (read-model) ────────────────────────────

data class AdherenceSummary(
    val repMemberUid: String,
    val plannedCount: Int,
    val visitedCount: Int,
    val missedCount: Int,
    val pendingCount: Int,
    val adHocCount: Int,
    val adherencePercent: Double,
)
