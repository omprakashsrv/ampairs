package com.ampairs.pricing.service

import com.ampairs.pricing.domain.dto.OfferRequest
import com.ampairs.pricing.domain.dto.OfferResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * Public service for promotions/offers. Offers apply on top of resolved prices; this service backs
 * the offline-sync `/pricing/v1/offers/sync` contract (incremental pull + UID-keyed bulk upsert).
 */
interface OfferService {
    fun findByUid(uid: String): OfferResponse?
    fun getAfterSync(lastSync: String?, pageable: Pageable): Page<OfferResponse>
    fun bulkUpsert(requests: List<OfferRequest>): List<OfferResponse>
}
