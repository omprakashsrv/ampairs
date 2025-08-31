package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.enums.WorkspaceType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * Request DTO for creating a new workspace
 */
data class CreateWorkspaceRequest(
    @field:NotBlank(message = "Workspace name is required")
    @field:Size(min = 2, max = 100, message = "Workspace name must be between 2 and 100 characters")
    val name: String,

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
)