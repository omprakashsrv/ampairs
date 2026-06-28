package com.ampairs.communication.domain.dto

import com.ampairs.communication.domain.model.MessageTemplate
import com.ampairs.communication.domain.model.TemplateVariant
import jakarta.validation.constraints.NotBlank

/**
 * Aggregate push/pull payload for `/communication/v1/templates/sync` — a template header plus its
 * channel/locale variants, synced together (delete-by-absence for variants; `base_version`
 * optimistic concurrency).
 */
data class TemplateAggregateRequest(
    @field:NotBlank(message = "Template uid is required")
    val uid: String,
    @field:NotBlank(message = "Template code is required")
    val code: String,
    val name: String = "",
    val category: String = "TRANSACTIONAL",
    val defaultLocale: String = "en",
    val description: String? = null,
    val baseVersion: Int = 1,
    val active: Boolean = true,
    val variants: List<TemplateVariantRequest> = emptyList(),
)

data class TemplateVariantRequest(
    @field:NotBlank(message = "Variant uid is required")
    val uid: String,
    @field:NotBlank(message = "channel is required")
    val channel: String,
    val locale: String = "en",
    val subject: String? = null,
    val htmlBody: String? = null,
    val textBody: String? = null,
    val providerTemplateId: String? = null,
    val providerParamsJson: String? = null,
    val active: Boolean = true,
)

data class TemplateAggregateResponse(
    val uid: String,
    val code: String,
    val name: String,
    val category: String,
    val defaultLocale: String,
    val description: String?,
    val baseVersion: Int,
    val active: Boolean,
    val variants: List<TemplateVariantResponse>,
    val updatedAt: String?,
)

data class TemplateVariantResponse(
    val uid: String,
    val channel: String,
    val locale: String,
    val subject: String?,
    val htmlBody: String?,
    val textBody: String?,
    val providerTemplateId: String?,
    val providerParamsJson: String?,
    val active: Boolean,
)

fun MessageTemplate.applyHeader(request: TemplateAggregateRequest): MessageTemplate = apply {
    if (uid.isBlank()) uid = request.uid
    code = request.code.trim()
    name = request.name.trim()
    category = request.category.trim().uppercase()
    defaultLocale = request.defaultLocale.trim()
    description = request.description
    baseVersion = request.baseVersion
    active = request.active
}

fun TemplateVariant.applyRequest(request: TemplateVariantRequest, templateUid: String): TemplateVariant = apply {
    if (uid.isBlank()) uid = request.uid
    this.templateUid = templateUid
    channel = request.channel.trim().uppercase()
    locale = request.locale.trim()
    subject = request.subject
    htmlBody = request.htmlBody
    textBody = request.textBody
    providerTemplateId = request.providerTemplateId
    providerParamsJson = request.providerParamsJson
    active = request.active
}

fun TemplateVariant.asResponse(): TemplateVariantResponse = TemplateVariantResponse(
    uid = uid,
    channel = channel,
    locale = locale,
    subject = subject,
    htmlBody = htmlBody,
    textBody = textBody,
    providerTemplateId = providerTemplateId,
    providerParamsJson = providerParamsJson,
    active = active,
)

fun MessageTemplate.asResponse(variants: List<TemplateVariant>): TemplateAggregateResponse = TemplateAggregateResponse(
    uid = uid,
    code = code,
    name = name,
    category = category,
    defaultLocale = defaultLocale,
    description = description,
    baseVersion = baseVersion,
    active = active,
    variants = variants.map { it.asResponse() },
    updatedAt = updatedAt?.toString(),
)
