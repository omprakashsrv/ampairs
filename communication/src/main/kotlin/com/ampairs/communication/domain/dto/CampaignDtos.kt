package com.ampairs.communication.domain.dto

import com.ampairs.communication.domain.model.Campaign
import jakarta.validation.constraints.NotBlank

data class CampaignRequest(
    @field:NotBlank(message = "Campaign uid is required")
    val uid: String,
    val name: String = "",
    @field:NotBlank(message = "template_uid is required")
    val templateUid: String,
    @field:NotBlank(message = "channel is required")
    val channel: String,
    val audienceType: String = "SEGMENT",
    val audienceRef: String? = null,
    val variablesJson: String? = null,
    val scheduledAt: String? = null,
    val throttlePerMinute: Int? = null,
    val active: Boolean = true,
)

/** Campaign response incl. the delivery rollup. `targeted == sent + failed + skipped` (SC-005). */
data class CampaignResponse(
    val uid: String,
    val name: String,
    val templateUid: String,
    val channel: String,
    val audienceType: String,
    val audienceRef: String?,
    val status: String,
    val scheduledAt: String?,
    val throttlePerMinute: Int?,
    val startedAt: String?,
    val completedAt: String?,
    val targetedCount: Int,
    val sentCount: Long,
    val deliveredCount: Long,
    val failedCount: Long,
    val skippedCount: Long,
    val active: Boolean,
    val updatedAt: String?,
)

fun Campaign.applyRequest(request: CampaignRequest): Campaign = apply {
    if (uid.isBlank()) uid = request.uid
    name = request.name.trim()
    templateUid = request.templateUid.trim()
    channel = request.channel.trim().uppercase()
    audienceType = request.audienceType.trim().uppercase()
    audienceRef = request.audienceRef
    variablesJson = request.variablesJson
    scheduledAt = request.scheduledAt?.takeIf { it.isNotBlank() }?.let { runCatching { java.time.Instant.parse(it) }.getOrNull() }
    throttlePerMinute = request.throttlePerMinute
    active = request.active
}
