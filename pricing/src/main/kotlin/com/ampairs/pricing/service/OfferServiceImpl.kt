package com.ampairs.pricing.service

import com.ampairs.pricing.domain.dto.OfferRequest
import com.ampairs.pricing.domain.dto.OfferResponse
import com.ampairs.pricing.domain.dto.applyRequest
import com.ampairs.pricing.domain.dto.asResponse
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.repository.OfferRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class OfferServiceImpl(
    private val offerRepository: OfferRepository,
) : OfferService {

    private val logger = LoggerFactory.getLogger(OfferServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun findByUid(uid: String): OfferResponse? =
        uid.takeIf { it.isNotBlank() }?.let { offerRepository.findByUid(it)?.asResponse() }

    @Transactional(readOnly = true)
    override fun getAfterSync(lastSync: String?, pageable: Pageable): Page<OfferResponse> {
        val page = if (lastSync.isNullOrBlank()) {
            offerRepository.findAllForSync(pageable)
        } else {
            try {
                val instant = Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8))
                offerRepository.findByUpdatedAtAfter(instant, pageable)
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', full offer sync", lastSync, e)
                offerRepository.findAllForSync(pageable)
            }
        }
        return page.map { it.asResponse() }
    }

    @Transactional
    override fun bulkUpsert(requests: List<OfferRequest>): List<OfferResponse> = requests.map { req ->
        val existing = req.uid?.takeIf { it.isNotBlank() }?.let { offerRepository.findByUid(it) }
        val offer = (existing ?: Offer()).applyRequest(req)
        offerRepository.save(offer).asResponse()
    }
}
