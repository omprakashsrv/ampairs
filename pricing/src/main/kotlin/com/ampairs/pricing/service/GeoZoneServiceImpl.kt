package com.ampairs.pricing.service

import com.ampairs.pricing.domain.dto.GeoZoneRequest
import com.ampairs.pricing.domain.dto.GeoZoneResponse
import com.ampairs.pricing.domain.dto.applyRequest
import com.ampairs.pricing.domain.dto.asResponse
import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.domain.model.GeoZoneMembers
import com.ampairs.pricing.repository.GeoZoneRepository
import com.ampairs.pricing.util.PricingJson
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class GeoZoneServiceImpl(
    private val geoZoneRepository: GeoZoneRepository,
) : GeoZoneService {

    private val logger = LoggerFactory.getLogger(GeoZoneServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun findByUid(uid: String): GeoZoneResponse? =
        uid.takeIf { it.isNotBlank() }?.let { geoZoneRepository.findByUid(it)?.asResponse() }

    @Transactional(readOnly = true)
    override fun getAfterSync(lastSync: String?, pageable: Pageable): Page<GeoZoneResponse> {
        val page = if (lastSync.isNullOrBlank()) {
            geoZoneRepository.findAllForSync(pageable)
        } else {
            try {
                val instant = Instant.parse(URLDecoder.decode(lastSync, StandardCharsets.UTF_8))
                geoZoneRepository.findByUpdatedAtAfter(instant, pageable)
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', full geo-zone sync", lastSync, e)
                geoZoneRepository.findAllForSync(pageable)
            }
        }
        return page.map { it.asResponse() }
    }

    @Transactional
    override fun bulkUpsert(requests: List<GeoZoneRequest>): List<GeoZoneResponse> = requests.map { req ->
        val existing = req.uid?.takeIf { it.isNotBlank() }?.let { geoZoneRepository.findByUid(it) }
        val zone = (existing ?: GeoZone()).applyRequest(req)
        geoZoneRepository.save(zone).asResponse()
    }

    @Transactional(readOnly = true)
    override fun zoneForPincode(pincode: String?, state: String?): String? {
        if (pincode.isNullOrBlank() && state.isNullOrBlank()) return null
        return geoZoneRepository.findAllByActiveTrue()
            .firstOrNull { zone ->
                val members = PricingJson.read(zone.membersJson, GeoZoneMembers())
                pincode?.let { members.contains(it, state) } ?: (state != null && members.states.any { it.equals(state, true) })
            }
            ?.uid
    }
}
