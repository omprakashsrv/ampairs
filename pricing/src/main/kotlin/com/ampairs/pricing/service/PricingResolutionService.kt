package com.ampairs.pricing.service

import com.ampairs.pricing.domain.dto.PriceResolutionRequest
import com.ampairs.pricing.domain.dto.PriceResolutionResponse

/**
 * Single-sourced price-resolution algorithm. Used server-side on the ecom path and mirrored offline by
 * the KMP app; the merchant `order`/`invoice` flow resolves client-side and only stores the snapshot.
 */
interface PricingResolutionService {
    fun resolve(request: PriceResolutionRequest): PriceResolutionResponse
}
