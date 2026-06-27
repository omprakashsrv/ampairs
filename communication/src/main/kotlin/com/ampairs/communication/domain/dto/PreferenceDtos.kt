package com.ampairs.communication.domain.dto

import com.ampairs.communication.domain.model.CommunicationPreference
import jakarta.validation.constraints.NotBlank

data class PreferenceRequest(
    @field:NotBlank(message = "Preference uid is required")
    val uid: String,
    @field:NotBlank(message = "customer_uid is required")
    val customerUid: String,
    @field:NotBlank(message = "channel is required")
    val channel: String,
    val category: String = "PROMOTIONAL",
    val optedIn: Boolean = true,
    val source: String? = null,
    val active: Boolean = true,
)

data class PreferenceResponse(
    val uid: String,
    val customerUid: String,
    val channel: String,
    val category: String,
    val optedIn: Boolean,
    val source: String?,
    val active: Boolean,
    val updatedAt: String?,
)

fun CommunicationPreference.applyRequest(request: PreferenceRequest): CommunicationPreference = apply {
    if (uid.isBlank()) uid = request.uid
    customerUid = request.customerUid.trim()
    channel = request.channel.trim().uppercase()
    category = request.category.trim().uppercase()
    optedIn = request.optedIn
    source = request.source
    active = request.active
}

fun CommunicationPreference.asResponse(): PreferenceResponse = PreferenceResponse(
    uid = uid,
    customerUid = customerUid,
    channel = channel,
    category = category,
    optedIn = optedIn,
    source = source,
    active = active,
    updatedAt = updatedAt?.toString(),
)
