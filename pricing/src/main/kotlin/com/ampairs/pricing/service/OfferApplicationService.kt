package com.ampairs.pricing.service

import com.ampairs.pricing.domain.dto.OfferApplicationRequest
import com.ampairs.pricing.domain.dto.OfferApplicationResponse

/**
 * Single-sourced promotion application engine. Given a cart (lines already priced by
 * [PricingResolutionService]) and the buyer's segment, it selects eligible active offers, resolves
 * stacking/exclusivity by priority, and returns the applied discounts + free-goods lines.
 *
 * Order of operations: **price resolve (009) → offer apply (this) → tax → snapshot.** This service
 * never re-resolves prices. Mirrored offline by the KMP app for the merchant flow; run server-side
 * for ecom checkout so both paths produce identical results from identical inputs.
 */
interface OfferApplicationService {
    fun apply(request: OfferApplicationRequest): OfferApplicationResponse
}
