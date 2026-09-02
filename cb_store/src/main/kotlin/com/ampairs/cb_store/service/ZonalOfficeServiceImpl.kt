package com.ampairs.cb_store.service

import com.ampairs.cb_store.domain.dto.ZonalOfficeRequest
import com.ampairs.cb_store.domain.dto.ZonalOfficeResponse
import com.ampairs.cb_store.domain.dto.applyRequest
import com.ampairs.cb_store.domain.dto.asZonalOfficeResponse
import com.ampairs.cb_store.domain.model.ZonalOffice
import com.ampairs.cb_store.exception.ZonalOfficeNotFoundException
import com.ampairs.cb_store.repository.ZonalOfficeRepository
import com.ampairs.core.sync.EntityChangePublisher
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

@Service
class ZonalOfficeServiceImpl(
    private val zonalOfficeRepository: ZonalOfficeRepository,
    private val entityChangePublisher: EntityChangePublisher,
) : ZonalOfficeService {

    private val logger = LoggerFactory.getLogger(ZonalOfficeServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun findByUid(uid: String): ZonalOfficeResponse? {
        if (uid.isBlank()) return null
        return zonalOfficeRepository.findByUid(uid)?.asZonalOfficeResponse()
    }

    @Transactional(readOnly = true)
    override fun getByUid(uid: String): ZonalOfficeResponse =
        findByUid(uid) ?: throw ZonalOfficeNotFoundException("Zonal office not found for uid: $uid")

    @Transactional(readOnly = true)
    override fun findAllActive(): List<ZonalOfficeResponse> =
        zonalOfficeRepository.findByActiveTrueOrderByName().map { it.asZonalOfficeResponse() }

    @Transactional(readOnly = true)
    override fun getZonalOfficesAfterSync(lastSync: String?, pageable: Pageable): Page<ZonalOfficeResponse> {
        val page: Page<ZonalOffice> = if (lastSync.isNullOrBlank()) {
            zonalOfficeRepository.findAllForSync(pageable)
        } else {
            try {
                val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
                zonalOfficeRepository.findByUpdatedAtAfter(Instant.parse(decoded), pageable)
            } catch (e: Exception) {
                logger.warn("Invalid last_sync '{}', falling back to full sync feed", lastSync, e)
                zonalOfficeRepository.findAllForSync(pageable)
            }
        }
        return page.map { it.asZonalOfficeResponse() }
    }

    @Transactional
    override fun bulkUpsert(requests: List<ZonalOfficeRequest>): List<ZonalOfficeResponse> =
        requests.map { request ->
            val existing = request.uid?.takeIf { it.isNotBlank() }?.let { zonalOfficeRepository.findByUid(it) }
            if (existing != null) {
                existing.applyRequest(request)
                zonalOfficeRepository.save(existing)
                    .also { entityChangePublisher.updated("cb_zonal_office", it.uid) }
                    .asZonalOfficeResponse()
            } else {
                val office = ZonalOffice().applyRequest(request)
                zonalOfficeRepository.save(office)
                    .also { entityChangePublisher.created("cb_zonal_office", it.uid) }
                    .asZonalOfficeResponse()
            }
        }

    @Transactional
    override fun create(request: ZonalOfficeRequest): ZonalOfficeResponse {
        val saved = zonalOfficeRepository.save(ZonalOffice().applyRequest(request))
        entityChangePublisher.created("cb_zonal_office", saved.uid)
        return saved.asZonalOfficeResponse()
    }

    @Transactional
    override fun delete(uid: String) {
        val office = zonalOfficeRepository.findByUid(uid)
            ?: throw ZonalOfficeNotFoundException("Zonal office not found for uid: $uid")
        office.active = false
        zonalOfficeRepository.save(office)
        entityChangePublisher.deleted("cb_zonal_office", office.uid)
    }
}
