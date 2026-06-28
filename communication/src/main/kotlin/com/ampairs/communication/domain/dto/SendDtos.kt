package com.ampairs.communication.domain.dto

import jakarta.validation.constraints.NotBlank

/**
 * Explicit recipient for a manual send (or an audience LIST). At least one of email/phone/token is
 * required for the chosen channel; `locale` selects the template variant.
 */
data class RecipientDto(
    val customerUid: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val pushToken: String? = null,
    val locale: String? = null,
)

/** Manual/transactional send via `POST /communication/v1/requests`. */
data class SendRequest(
    @field:NotBlank(message = "template_code is required")
    val templateCode: String,
    val channels: List<String> = emptyList(),
    val audienceType: String = "LIST",
    val audienceRef: String? = null,
    val recipients: List<RecipientDto> = emptyList(),
    val variables: Map<String, String> = emptyMap(),
)

data class CommunicationRequestResponse(
    val uid: String,
    val status: String,
    val logs: List<CommunicationLogResponse>,
)

/** Preview rendering (`POST /communication/v1/templates/{code}/preview`). */
data class PreviewRequest(
    @field:NotBlank(message = "channel is required")
    val channel: String,
    val locale: String? = null,
    val variables: Map<String, String> = emptyMap(),
)

data class PreviewResponse(
    val subject: String?,
    val renderedHtml: String?,
    val renderedText: String,
    val missingVariables: List<String>,
)
