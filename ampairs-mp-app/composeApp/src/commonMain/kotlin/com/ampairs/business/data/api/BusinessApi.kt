package com.ampairs.business.data.api

import com.ampairs.business.domain.*

/**
 * Business Management API interface.
 * Provides access to business configuration endpoints.
 */
interface BusinessApi {
    // Creation
    suspend fun createBusinessProfile(request: BusinessCreateRequest): Result<BusinessProfile>
    suspend fun checkBusinessExists(): Result<Boolean>

    // Overview
    suspend fun getBusinessOverview(): Result<BusinessOverview>

    // Profile & Registration
    suspend fun getBusinessProfile(): Result<BusinessProfile>
    suspend fun updateBusinessProfile(request: BusinessProfileUpdateRequest): Result<BusinessProfile>

    // Operations
    suspend fun getBusinessOperations(): Result<BusinessOperations>
    suspend fun updateBusinessOperations(request: BusinessOperationsUpdateRequest): Result<BusinessOperations>

    // Tax Configuration
    suspend fun getTaxConfiguration(): Result<TaxConfiguration>
    suspend fun updateTaxConfiguration(request: TaxConfigurationUpdateRequest): Result<TaxConfiguration>

    // Legacy methods (kept for backward compatibility with existing repository)
    suspend fun getBusiness(): Result<Business>
    suspend fun createBusiness(payload: BusinessPayload): Result<Business>
    suspend fun updateBusiness(payload: BusinessPayload): Result<Business>
}
