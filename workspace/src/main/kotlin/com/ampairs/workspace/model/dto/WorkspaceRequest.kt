package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.enums.WorkspaceType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request DTO for creating a new workspace
 */
data class CreateWorkspaceRequest(
    @field:NotBlank(message = "Workspace name is required")
    @field:Size(min = 2, max = 100, message = "Workspace name must be between 2 and 100 characters")
    val name: String = "",

    @field:Pattern(
        regexp = "^[a-z0-9-]+$",
        message = "Slug must contain only lowercase letters, numbers, and hyphens"
    )
    @field:Size(min = 2, max = 50, message = "Slug must be between 2 and 50 characters")
    val slug: String? = null,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null,

    val workspaceType: WorkspaceType = WorkspaceType.BUSINESS,

    val avatarUrl: String? = null,

    val timezone: String = "UTC",

    val language: String = "en",

    @field:Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    val addressLine1: String? = null,

    @field:Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    val addressLine2: String? = null,

    @field:Size(max = 100, message = "City must not exceed 100 characters")
    val city: String? = null,

    @field:Size(max = 100, message = "State must not exceed 100 characters")
    val state: String? = null,

    @field:Size(max = 20, message = "Postal code must not exceed 20 characters")
    val postalCode: String? = null,

    @field:Size(max = 100, message = "Country must not exceed 100 characters")
    val country: String? = null,

    @field:Pattern(
        regexp = "^[+]?[0-9\\s\\-()]+$",
        message = "Phone number format is invalid"
    )
    @field:Size(max = 20, message = "Phone number must not exceed 20 characters")
    val phone: String? = null,

    @field:Email(message = "Email format is invalid")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String? = null,

    @field:Size(max = 255, message = "Website URL must not exceed 255 characters")
    val website: String? = null,

    @field:Size(max = 50, message = "Tax ID must not exceed 50 characters")
    val taxId: String? = null,

    @field:Size(max = 100, message = "Registration number must not exceed 100 characters")
    val registrationNumber: String? = null,

    val currency: String = "INR",

    val dateFormat: String = "DD-MM-YYYY",

    val timeFormat: String = "12H",

    @field:Pattern(
        regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$",
        message = "Business hours start must be in HH:MM format"
    )
    val businessHoursStart: String? = null,

    @field:Pattern(
        regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$",
        message = "Business hours end must be in HH:MM format"
    )
    val businessHoursEnd: String? = null,
)

/**
 * Request DTO for updating workspace information
 */
data class UpdateWorkspaceRequest(
    @field:Size(min = 2, max = 100, message = "Workspace name must be between 2 and 100 characters")
    val name: String? = null,

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    val description: String? = null,

    val workspaceType: WorkspaceType? = null,

    val avatarUrl: String? = null,

    val timezone: String? = null,

    val language: String? = null,

    @field:Size(max = 255, message = "Address line 1 must not exceed 255 characters")
    val addressLine1: String? = null,

    @field:Size(max = 255, message = "Address line 2 must not exceed 255 characters")
    val addressLine2: String? = null,

    @field:Size(max = 100, message = "City must not exceed 100 characters")
    val city: String? = null,

    @field:Size(max = 100, message = "State must not exceed 100 characters")
    val state: String? = null,

    @field:Size(max = 20, message = "Postal code must not exceed 20 characters")
    val postalCode: String? = null,

    @field:Size(max = 100, message = "Country must not exceed 100 characters")
    val country: String? = null,

    @field:Pattern(
        regexp = "^[+]?[0-9\\s\\-()]+$",
        message = "Phone number format is invalid"
    )
    @field:Size(max = 20, message = "Phone number must not exceed 20 characters")
    val phone: String? = null,

    @field:Email(message = "Email format is invalid")
    @field:Size(max = 255, message = "Email must not exceed 255 characters")
    val email: String? = null,

    @field:Size(max = 255, message = "Website URL must not exceed 255 characters")
    val website: String? = null,

    @field:Size(max = 50, message = "Tax ID must not exceed 50 characters")
    val taxId: String? = null,

    @field:Size(max = 100, message = "Registration number must not exceed 100 characters")
    val registrationNumber: String? = null,

    val currency: String? = null,

    val dateFormat: String? = null,

    val timeFormat: String? = null,

    @field:Pattern(
        regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$",
        message = "Business hours start must be in HH:MM format"
    )
    val businessHoursStart: String? = null,

    @field:Pattern(
        regexp = "^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$",
        message = "Business hours end must be in HH:MM format"
    )
    val businessHoursEnd: String? = null,
)