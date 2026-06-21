package com.ampairs.printing.domain.dto

import com.ampairs.printing.domain.model.PrintTemplate
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Push payload from the app's `/printing/v1/templates/sync` POST. Field names match the app's
 * `TemplateDto` (global Jackson SNAKE_CASE handles the camelCase → snake_case mapping):
 * `id` (= uid), `document_type`, `printer_class`, `name`, `template_json`, `version`, `active`.
 * `updated_at` is sent by the client but ignored — the server's timestamp is authoritative.
 */
data class PrintTemplateRequest(
    @field:NotBlank(message = "Template id (uid) is required")
    val id: String,

    @field:NotBlank(message = "document_type is required")
    @field:Size(max = 50, message = "document_type must not exceed 50 characters")
    val documentType: String,

    @field:NotBlank(message = "printer_class is required")
    @field:Size(max = 30, message = "printer_class must not exceed 30 characters")
    val printerClass: String,

    @field:NotBlank(message = "Template name is required")
    @field:Size(max = 150, message = "name must not exceed 150 characters")
    val name: String,

    @field:NotBlank(message = "template_json is required")
    val templateJson: String,

    val version: Long = 1,

    val active: Boolean = true,

    val isDefault: Boolean = false,
)

/**
 * Pull/echo payload returned by both GET and POST `/printing/v1/templates/sync`. Mirrors the app's
 * `TemplateDto`: `id` is the uid; `updated_at` is the server's ISO-8601 (UTC) timestamp string.
 */
data class PrintTemplateResponse(
    val id: String,
    val documentType: String,
    val printerClass: String,
    val name: String,
    val templateJson: String,
    val version: Long,
    val active: Boolean,
    val isDefault: Boolean,
    val updatedAt: String?,
)

fun PrintTemplate.applyRequest(request: PrintTemplateRequest): PrintTemplate = apply {
    if (uid.isBlank()) uid = request.id
    documentType = request.documentType.trim()
    printerClass = request.printerClass.trim()
    name = request.name.trim()
    templateJson = request.templateJson
    templateVersion = request.version
    active = request.active
    isDefault = request.isDefault
}

fun PrintTemplate.asResponse(): PrintTemplateResponse = PrintTemplateResponse(
    id = uid,
    documentType = documentType,
    printerClass = printerClass,
    name = name,
    templateJson = templateJson,
    version = templateVersion,
    active = active,
    isDefault = isDefault,
    updatedAt = updatedAt?.toString(),
)

fun List<PrintTemplate>.asResponses(): List<PrintTemplateResponse> = map { it.asResponse() }
