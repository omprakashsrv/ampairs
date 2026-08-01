package com.ampairs.pricing.service

import com.ampairs.pricing.domain.dto.PriceListItemRequest
import com.ampairs.pricing.domain.dto.PriceListItemResponse
import com.ampairs.pricing.domain.dto.PriceListRequest
import com.ampairs.pricing.domain.dto.PriceListResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface PriceListService {
    // ── price lists ───────────────────────────────────────────────────────────────────
    fun findByUid(uid: String): PriceListResponse?
    fun getListsAfterSync(lastSync: String?, pageable: Pageable): Page<PriceListResponse>
    fun bulkUpsertLists(requests: List<PriceListRequest>): List<PriceListResponse>
    fun delete(uid: String)

    // ── price-list items ───────────────────────────────────────────────────────────────
    fun itemsForList(priceListUid: String): List<PriceListItemResponse>
    fun getItemsAfterSync(lastSync: String?, pageable: Pageable): Page<PriceListItemResponse>
    fun bulkUpsertItems(requests: List<PriceListItemRequest>): List<PriceListItemResponse>
}
