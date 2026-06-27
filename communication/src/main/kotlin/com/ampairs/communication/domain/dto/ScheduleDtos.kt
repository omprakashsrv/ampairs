package com.ampairs.communication.domain.dto

import com.ampairs.communication.domain.model.CommunicationSchedule
import jakarta.validation.constraints.NotBlank

data class ScheduleRequest(
    @field:NotBlank(message = "Schedule uid is required")
    val uid: String,
    val name: String = "",
    @field:NotBlank(message = "template_uid is required")
    val templateUid: String,
    val channels: String = "",
    val audienceType: String = "SEGMENT",
    val audienceRef: String? = null,
    val variablesJson: String? = null,
    val frequency: String = "MONTHLY",
    val interval: Int = 1,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val timeOfDay: String = "09:00",
    val timezone: String = "UTC",
    val startDate: String? = null,
    val endDate: String? = null,
    val paused: Boolean = false,
    val active: Boolean = true,
)

data class ScheduleResponse(
    val uid: String,
    val name: String,
    val templateUid: String,
    val channels: String,
    val audienceType: String,
    val audienceRef: String?,
    val variablesJson: String?,
    val frequency: String,
    val interval: Int,
    val dayOfWeek: Int?,
    val dayOfMonth: Int?,
    val timeOfDay: String,
    val timezone: String,
    val startDate: String?,
    val endDate: String?,
    val paused: Boolean,
    val active: Boolean,
    val nextRunAt: String?,
    val lastRunAt: String?,
    val updatedAt: String?,
)

fun CommunicationSchedule.applyRequest(request: ScheduleRequest): CommunicationSchedule = apply {
    if (uid.isBlank()) uid = request.uid
    name = request.name.trim()
    templateUid = request.templateUid.trim()
    channels = request.channels.trim()
    audienceType = request.audienceType.trim().uppercase()
    audienceRef = request.audienceRef
    variablesJson = request.variablesJson
    frequency = request.frequency.trim().uppercase()
    interval = if (request.interval < 1) 1 else request.interval
    dayOfWeek = request.dayOfWeek
    dayOfMonth = request.dayOfMonth
    timeOfDay = request.timeOfDay.trim()
    timezone = request.timezone.trim().ifBlank { "UTC" }
    startDate = request.startDate
    endDate = request.endDate
    paused = request.paused
    active = request.active
}

fun CommunicationSchedule.asResponse(): ScheduleResponse = ScheduleResponse(
    uid = uid,
    name = name,
    templateUid = templateUid,
    channels = channels,
    audienceType = audienceType,
    audienceRef = audienceRef,
    variablesJson = variablesJson,
    frequency = frequency,
    interval = interval,
    dayOfWeek = dayOfWeek,
    dayOfMonth = dayOfMonth,
    timeOfDay = timeOfDay,
    timezone = timezone,
    startDate = startDate,
    endDate = endDate,
    paused = paused,
    active = active,
    nextRunAt = nextRunAt?.toString(),
    lastRunAt = lastRunAt?.toString(),
    updatedAt = updatedAt?.toString(),
)
