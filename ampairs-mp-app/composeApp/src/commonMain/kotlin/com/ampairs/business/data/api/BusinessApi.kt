package com.ampairs.business.data.api

import com.ampairs.business.domain.Business
import com.ampairs.business.domain.BusinessPayload

interface BusinessApi {
    suspend fun getBusiness(): Result<Business>
    suspend fun createBusiness(payload: BusinessPayload): Result<Business>
    suspend fun updateBusiness(payload: BusinessPayload): Result<Business>
}
