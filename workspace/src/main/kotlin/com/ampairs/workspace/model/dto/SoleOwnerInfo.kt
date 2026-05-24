package com.ampairs.workspace.model.dto

data class SoleOwnerInfo(
    val workspaceId: String,
    val workspaceName: String,
    val workspaceSlug: String,
    val memberCount: Long,
)
