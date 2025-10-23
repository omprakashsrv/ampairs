package com.ampairs.business.data.api

import com.ampairs.business.domain.*

/**
 * Business Management API interface (simplified).
 * All business operations use a single unified endpoint.
 */
interface BusinessApi {
    // Main endpoints
    suspend fun getBusiness(): Result<Business>
    suspend fun updateBusiness(request: BusinessUpdateRequest): Result<Business>
    suspend fun createBusiness(request: BusinessCreateRequest): Result<Business>

    // Dashboard overview (optimized for performance)
    suspend fun getBusinessOverview(): Result<BusinessOverview>

    // Utility
    suspend fun checkBusinessExists(): Result<Boolean>
}
