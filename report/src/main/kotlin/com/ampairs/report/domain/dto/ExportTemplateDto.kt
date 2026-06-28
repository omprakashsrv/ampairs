package com.ampairs.report.domain.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant

/** A single typed filter row in an export template (e.g. `{column_key:"group_uid", op:"eq", value:"CGR1"}`). */
data class ExportFilterDto(
    val columnKey: String,
    val op: String,
    val value: String? = null,
)

data class ExportTemplateRequest(
    val uid: String? = null,

    @field:NotBlank(message = "module_key is required")
    @field:Size(max = 100, message = "module_key must not exceed 100 characters")
    val moduleKey: String,

    @field:NotBlank(message = "name is required")
    @field:Size(max = 200, message = "name must not exceed 200 characters")
    val name: String,

    val selectedColumns: List<String> = emptyList(),

    val filters: List<ExportFilterDto> = emptyList(),

    @field:Size(max = 100, message = "sort_by must not exceed 100 characters")
    val sortBy: String? = null,

    @field:Pattern(regexp = "ASC|DESC", message = "sort_dir must be ASC or DESC")
    val sortDir: String = "ASC",

    @field:Pattern(regexp = "CSV|JSON|XML|EXCEL", message = "default_format must be CSV, JSON, XML or EXCEL")
    val defaultFormat: String = "CSV",

    @field:Pattern(regexp = "CLIENT|SERVER", message = "default_location must be CLIENT or SERVER")
    val defaultLocation: String = "CLIENT",

    val includeInactive: Boolean = false,

    val active: Boolean = true,

    val refId: String? = null,
)

data class ExportTemplateResponse(
    val uid: String,
    val refId: String?,
    val moduleKey: String,
    val name: String,
    val selectedColumns: List<String>,
    val filters: List<ExportFilterDto>,
    val sortBy: String?,
    val sortDir: String,
    val defaultFormat: String,
    val defaultLocation: String,
    val includeInactive: Boolean,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)
