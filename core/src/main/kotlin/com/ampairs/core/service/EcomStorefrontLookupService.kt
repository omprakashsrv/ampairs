package com.ampairs.core.service

interface EcomStorefrontLookupService {
    fun findStorefrontIdByWorkspaceId(workspaceId: String): String?
}
