package com.ampairs.business.data.api

import com.ampairs.business.domain.*

/**
 * Business Management API interface (simplified).
 * All business operations use a single unified endpoint.
 */
interface BusinessApi {
    // Main endpoints
    suspend fun getBusiness(): Result<Business>
    suspend fun updateBusiness(request: BusinessPayload): Result<Business>
    suspend fun createBusiness(request: BusinessPayload): Result<Business>

    // Dashboard overview (optimized for performance)
    suspend fun getBusinessOverview(): Result<BusinessOverview>

    // Specific section endpoints
    suspend fun createBusinessProfile(request: BusinessCreateRequest): Result<BusinessProfile>
    suspend fun getBusinessProfile(): Result<BusinessProfile>
    suspend fun updateBusinessProfile(request: BusinessProfileUpdateRequest): Result<BusinessProfile>

    suspend fun getBusinessOperations(): Result<BusinessOperations>
    suspend fun updateBusinessOperations(request: BusinessOperationsUpdateRequest): Result<BusinessOperations>

    suspend fun getTaxConfiguration(): Result<TaxConfiguration>
    suspend fun updateTaxConfiguration(request: TaxConfigurationUpdateRequest): Result<TaxConfiguration>

    // Utility
    suspend fun checkBusinessExists(): Result<Boolean>
}
