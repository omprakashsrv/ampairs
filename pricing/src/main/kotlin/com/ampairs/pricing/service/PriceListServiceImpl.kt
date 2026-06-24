package com.ampairs.pricing.service

import com.ampairs.pricing.config.Constants
import com.ampairs.pricing.domain.dto.PriceListItemRequest
import com.ampairs.pricing.domain.dto.PriceListItemResponse
import com.ampairs.pricing.domain.dto.PriceListRequest
import com.ampairs.pricing.domain.dto.PriceListResponse
import com.ampairs.pricing.domain.dto.applyRequest
import com.ampairs.pricing.domain.dto.asResponse
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.exception.InvalidPriceTiersException
import com.ampairs.pricing.exception.PriceListNotFoundException
import com.ampairs.pricing.repository.PriceListItemRepository
import com.ampairs.pricing.repository.PriceListRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class PriceListServiceImpl(
    private val priceListRepository: PriceListRepository,
    private val priceListItemRepository: PriceListItemRepository,
) : PriceListService {

    private val logger = LoggerFactory.getLogger(PriceListServiceImpl::class.java)

    // ── price lists ───────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    override fun findByUid(uid: String): PriceListResponse? =
        uid.takeIf { it.isNotBlank() }?.let { priceListRepository.findByUid(it)?.asResponse() }

    @Transactional(readOnly = true)
    override fun getListsAfterSync(lastSync: String?, pageable: Pageable): Page<PriceListResponse> =
        syncPage(lastSync, pageable, priceListRepository::findAllForSync, priceListRepository::findByUpdatedAtAfter)
            .map { it.asResponse() }

    @Transactional
    override fun bulkUpsertLists(requests: List<PriceListRequest>): List<PriceListResponse> = requests.map { req ->
        val existing = req.uid?.takeIf { it.isNotBlank() }?.let { priceListRepository.findByUid(it) }
        val list = (existing ?: PriceList()).applyRequest(req)
        priceListRepository.save(list).asResponse()
    }

    @Transactional
    override fun delete(uid: String) {
        val list = priceListRepository.findByUid(uid)
            ?: throw PriceListNotFoundException("Price list not found for uid: $uid")
        list.active = false
        priceListRepository.save(list)
    }

    // ── price-list items ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    override fun itemsForList(priceListUid: String): List<PriceListItemResponse> {
        val currency = priceListRepository.findByUid(priceListUid)?.currency ?: Constants.DEFAULT_CURRENCY
        return priceListItemRepository.findByPriceListId(priceListUid).map { it.asResponse(currency) }
    }

    @Transactional(readOnly = true)
    override fun getItemsAfterSync(lastSync: String?, pageable: Pageable): Page<PriceListItemResponse> {
        val currencyByList = HashMap<String, String>()
        return syncPage(lastSync, pageable, priceListItemRepository::findAllForSync, priceListItemRepository::findByUpdatedAtAfter)
            .map { item ->
                val currency = currencyByList.getOrPut(item.priceListId) {
                    priceListRepository.findByUid(item.priceListId)?.currency ?: Constants.DEFAULT_CURRENCY
                }
                item.asResponse(currency)
            }
    }

    @Transactional
    override fun bulkUpsertItems(requests: List<PriceListItemRequest>): List<PriceListItemResponse> = requests.map { req ->
        validateTiers(req)
        val currency = priceListRepository.findByUid(req.priceListId)?.currency ?: Constants.DEFAULT_CURRENCY
        val existing = req.uid?.takeIf { it.isNotBlank() }?.let { priceListItemRepository.findByUid(it) }
        val item = (existing ?: PriceListItem()).applyRequest(req, currency)
        priceListItemRepository.save(item).asResponse(currency)
    }

    /** Tiers must be strictly ascending by minQty and each minQty > 0 (contiguous, non-overlapping). */
    private fun validateTiers(req: PriceListItemRequest) {
        if (req.tiers.isEmpty()) return
        val sorted = req.tiers.sortedBy { it.minQty }
        var prev: BigDecimal? = null
        for (t in sorted) {
            if (t.minQty <= BigDecimal.ZERO) throw InvalidPriceTiersException("Tier minQty must be > 0")
            if (prev != null && t.minQty <= prev) throw InvalidPriceTiersException("Tier minQty values must be strictly ascending")
            prev = t.minQty
        }
    }

    private fun <T : Any> syncPage(
        lastSync: String?,
        pageable: Pageable,
        full: (Pageable) -> Page<T>,
        incremental: (Instant, Pageable) -> Page<T>,
    ): Page<T> {
        if (lastSync.isNullOrBlank()) return full(pageable)
        return try {
            incremental(Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8)), pageable)
        } catch (e: Exception) {
            logger.warn("Invalid last_sync '{}', full sync", lastSync, e)
            full(pageable)
        }
    }
}
