package com.ampairs.unit.service

import com.ampairs.unit.domain.dto.UnitRequest
import com.ampairs.unit.domain.dto.UnitResponse
import com.ampairs.unit.domain.dto.UnitUsageResponse
import com.ampairs.unit.domain.dto.asUnitResponse
import com.ampairs.unit.domain.dto.asUnitResponses
import com.ampairs.unit.domain.dto.applyRequest
import com.ampairs.unit.domain.model.Unit
import com.ampairs.unit.exception.UnitInUseException
import com.ampairs.unit.exception.UnitNotFoundException
import com.ampairs.unit.repository.UnitRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UnitServiceImpl(
    private val unitRepository: UnitRepository,
    private val unitUsageProviders: List<UnitUsageProvider>
) : UnitService {

    private val logger = LoggerFactory.getLogger(UnitServiceImpl::class.java)

    @Transactional(readOnly = true)
    override fun findByUid(uid: String): UnitResponse? {
        if (uid.isBlank()) return null
        return unitRepository.findByUid(uid)?.asUnitResponse()
    }

    @Transactional(readOnly = true)
    override fun findByRefId(refId: String): UnitResponse? {
        if (refId.isBlank()) return null
        return unitRepository.findByRefId(refId)?.asUnitResponse()
    }

    @Transactional(readOnly = true)
    override fun findAll(activeOnly: Boolean): List<UnitResponse> {
        val units: List<Unit> = if (activeOnly) {
            unitRepository.findAllByActiveTrueOrderByName()
        } else {
            unitRepository.findAll().toList()
        }
        return units.asUnitResponses()
    }

    @Transactional
    override fun create(request: UnitRequest): UnitResponse {
        val unit = Unit().applyRequest(request).apply {
            active = true
        }
        val saved = unitRepository.save(unit)
        return saved.asUnitResponse()
    }

    @Transactional
    override fun update(uid: String, request: UnitRequest): UnitResponse {
        val existing = unitRepository.findByUid(uid)
            ?: throw UnitNotFoundException("Unit not found for uid: $uid")

        existing.applyRequest(request.copy(uid = uid))
        val saved = unitRepository.save(existing)
        return saved.asUnitResponse()
    }

    @Transactional
    override fun delete(uid: String) {
        val unit = unitRepository.findByUid(uid)
            ?: throw UnitNotFoundException("Unit not found for uid: $uid")

        val usage = aggregateUsage(uid)
        if (usage.inUse) {
            throw UnitInUseException(
                unitUid = uid,
                entityIds = usage.entityIds,
                conversionIds = usage.conversionIds
            )
        }

        unit.active = false
        unitRepository.save(unit)
    }

    @Transactional(readOnly = true)
    override fun isUnitInUse(uid: String): Boolean {
        if (uid.isBlank()) return false
        return aggregateUsage(uid).inUse
    }

    @Transactional(readOnly = true)
    override fun findEntitiesUsingUnit(uid: String): List<String> {
        if (uid.isBlank()) return emptyList()
        return aggregateUsage(uid).entityIds
    }

    @Transactional(readOnly = true)
    override fun getUsage(uid: String): UnitUsageResponse {
        val usage = aggregateUsage(uid)
        return UnitUsageResponse(
            unitId = uid,
            inUse = usage.inUse,
            entityCount = usage.entityCount,
            conversionCount = usage.conversionCount,
            entityIds = usage.entityIds,
            conversionIds = usage.conversionIds
        )
    }

    private fun aggregateUsage(uid: String): UnitUsageSnapshot {
        if (unitUsageProviders.isEmpty()) {
            return UnitUsageSnapshot(unitUid = uid)
        }

        val snapshots = unitUsageProviders.mapNotNull { provider ->
            runCatching { provider.findUsage(uid) }
                .onFailure { error ->
                    logger.warn("UnitUsageProvider ${provider::class.simpleName} failed for unit {}", uid, error)
                }
                .getOrNull()
        }

        if (snapshots.isEmpty()) {
            return UnitUsageSnapshot(unitUid = uid)
        }

        return snapshots.merge(uid)
    }
}
