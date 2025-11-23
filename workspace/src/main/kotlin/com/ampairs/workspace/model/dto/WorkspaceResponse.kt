package com.ampairs.workspace.model.dto

import com.ampairs.workspace.model.Workspace
import com.ampairs.workspace.model.enums.SubscriptionPlan
import com.ampairs.workspace.model.enums.WorkspaceType
import java.time.Instant

/**
 * Response DTO for workspace information
 */
data class WorkspaceResponse(
        val id: String,

        val name: String,

        val slug: String,

        val description: String?,

        val workspaceType: WorkspaceType,

        val avatarUrl: String?,

        val avatarThumbnailUrl: String?,

        val isActive: Boolean,

        val subscriptionPlan: SubscriptionPlan,

        val maxMembers: Int,

        val storageLimitGb: Int,

        val storageUsedGb: Int,

        val timezone: String,

        val language: String,

        val createdBy: String,

        val createdAt: Instant,

        val updatedAt: Instant,

        val lastActivityAt: Instant?,

        val trialExpiresAt: Instant?,

        val memberCount: Int? = null,

        val isTrial: Boolean? = null,

        val storagePercentage: Double? = null,
        
        // Business address details
        val addressLine1: String? = null,
        val addressLine2: String? = null,
        val city: String? = null,
        val state: String? = null,
        val postalCode: String? = null,
        val country: String? = null,
        
        // Contact information
        val phone: String? = null,
        val email: String? = null,
        val website: String? = null,
        
        // Legal/Tax details
        val taxId: String? = null,
        val registrationNumber: String? = null,
        
        // Business operations
        val currency: String? = null,
        val dateFormat: String? = null,
        val timeFormat: String? = null,
        val businessHoursStart: String? = null,
        val businessHoursEnd: String? = null,
        val workingDays: String? = null,
)

/**
 * Simplified workspace response for lists
 */
data class WorkspaceListResponse(
        val id: String,

        val name: String,

        val slug: String,

        val description: String?,

        val workspaceType: WorkspaceType,

        val avatarUrl: String?,

        val avatarThumbnailUrl: String?,

        val subscriptionPlan: SubscriptionPlan,

        val memberCount: Int,

        val lastActivityAt: Instant?,

        val createdAt: Instant,
        
        // Business contact information for list view
        val phone: String? = null,
        val email: String? = null,
        val city: String? = null,
        val country: String? = null,
)

/**
 * Extension function to convert Workspace entity to WorkspaceResponse
 */
fun Workspace.toResponse(memberCount: Int? = null): WorkspaceResponse {
    return WorkspaceResponse(
        id = this.uid, // Use uid instead of id
        name = this.name,
        slug = this.slug,
        description = this.description,
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl,
        avatarThumbnailUrl = this.avatarThumbnailUrl,
        isActive = this.active,
        subscriptionPlan = this.subscriptionPlan,
        maxMembers = this.maxUsers,
        storageLimitGb = this.storageLimitGb,
        storageUsedGb = this.storageUsedGb,
        timezone = this.timezone,
        language = this.language,
        createdBy = this.createdBy ?: "", // Use createdBy field
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now(),
        lastActivityAt = this.lastActivityAt,
        trialExpiresAt = this.trialExpiresAt,
        memberCount = memberCount,
        isTrial = this.isInTrial(),
        storagePercentage = if (storageLimitGb > 0) (storageUsedGb.toDouble() / storageLimitGb * 100) else 0.0,
        
        // Business address details
        addressLine1 = this.addressLine1,
        addressLine2 = this.addressLine2,
        city = this.city,
        state = this.state,
        postalCode = this.postalCode,
        country = this.country,
        
        // Contact information
        phone = this.phone,
        email = this.email,
        website = this.website,
        
        // Legal/Tax details
        taxId = this.taxId,
        registrationNumber = this.registrationNumber,
        
        // Business operations
        currency = this.currency,
        dateFormat = this.dateFormat,
        timeFormat = this.timeFormat,
        businessHoursStart = this.businessHoursStart,
        businessHoursEnd = this.businessHoursEnd,
        workingDays = this.workingDays,
    )
}

/**
 * Extension function to convert Workspace entity to WorkspaceListResponse
 */
fun Workspace.toListResponse(memberCount: Int): WorkspaceListResponse {
    return WorkspaceListResponse(
        id = this.uid, // Use uid instead of id
        name = this.name,
        slug = this.slug,
        description = this.description,
        workspaceType = this.workspaceType,
        avatarUrl = this.avatarUrl,
        avatarThumbnailUrl = this.avatarThumbnailUrl,
        subscriptionPlan = this.subscriptionPlan,
        memberCount = memberCount,
        lastActivityAt = this.lastActivityAt,
        createdAt = this.createdAt ?: Instant.now(),
        
        // Business contact information for list view
        phone = this.phone,
        email = this.email,
        city = this.city,
        country = this.country,
    )
}