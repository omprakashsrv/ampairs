package com.ampairs.communication.domain.dto

import com.ampairs.communication.domain.model.CommunicationLog
import com.ampairs.communication.domain.model.EventTemplateBinding
import jakarta.validation.constraints.NotBlank

// ---- Event→template bindings (standard /sync) ----

data class BindingRequest(
    @field:NotBlank(message = "Binding uid is required")
    val uid: String,
    @field:NotBlank(message = "event_type is required")
    val eventType: String,
    @field:NotBlank(message = "template_uid is required")
    val templateUid: String,
    val channels: String = "",
    val enabled: Boolean = true,
    val active: Boolean = true,
)

data class BindingResponse(
    val uid: String,
    val eventType: String,
    val templateUid: String,
    val channels: String,
    val enabled: Boolean,
    val active: Boolean,
    val updatedAt: String?,
)

fun EventTemplateBinding.applyRequest(request: BindingRequest): EventTemplateBinding = apply {
    if (uid.isBlank()) uid = request.uid
    eventType = request.eventType.trim().uppercase()
    templateUid = request.templateUid.trim()
    channels = request.channels.trim()
    enabled = request.enabled
    active = request.active
}

fun EventTemplateBinding.asResponse(): BindingResponse = BindingResponse(
    uid = uid,
    eventType = eventType,
    templateUid = templateUid,
    channels = channels,
    enabled = enabled,
    active = active,
    updatedAt = updatedAt?.toString(),
)

// ---- Communication log (pull-only /sync) ----

data class CommunicationLogResponse(
    val uid: String,
    val requestUid: String,
    val customerUid: String?,
    val channel: String,
    val recipientAddress: String,
    val category: String,
    val status: String,
    val skipReason: String?,
    val providerMessageId: String?,
    val errorMessage: String?,
    val billingMode: String?,
    val sentAt: String?,
    val deliveredAt: String?,
    val updatedAt: String?,
)

fun CommunicationLog.asResponse(): CommunicationLogResponse = CommunicationLogResponse(
    uid = uid,
    requestUid = requestUid,
    customerUid = customerUid,
    channel = channel,
    recipientAddress = recipientAddress,
    category = category,
    status = status,
    skipReason = skipReason,
    providerMessageId = providerMessageId,
    errorMessage = errorMessage,
    billingMode = billingMode,
    sentAt = sentAt?.toString(),
    deliveredAt = deliveredAt?.toString(),
    updatedAt = updatedAt?.toString(),
)
