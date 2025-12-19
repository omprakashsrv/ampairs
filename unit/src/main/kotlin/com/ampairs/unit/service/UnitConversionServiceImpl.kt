package com.ampairs.unit.service

import com.ampairs.unit.domain.dto.UnitConversionRequest
import com.ampairs.unit.domain.dto.UnitConversionResponse
import com.ampairs.unit.domain.dto.asUnitConversionResponse
import com.ampairs.unit.domain.dto.asUnitConversionResponses
import com.ampairs.unit.domain.dto.applyRequest
import com.ampairs.unit.domain.model.UnitConversion
import com.ampairs.unit.exception.CircularConversionException
import com.ampairs.unit.exception.UnitNotFoundException
import com.ampairs.unit.repository.UnitConversionRepository
import com.ampairs.unit.repository.UnitRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.collections.ArrayDeque

@Service
class UnitConversionServiceImpl(
    private val unitConversionRepository: UnitConversionRepository,
    private val unitRepository: UnitRepository
) : UnitConversionService {

    @Transactional(readOnly = true)
    override fun findByUid(uid: String): UnitConversionResponse? {
        return unitConversionRepository.findByUid(uid)?.asUnitConversionResponse()
    }

    @Transactional(readOnly = true)
    override fun findByEntityId(entityId: String): List<UnitConversionResponse> {
        return unitConversionRepository.findByEntityIdAndActiveTrue(entityId).asUnitConversionResponses()
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<UnitConversionResponse> {
        return unitConversionRepository.findAllActive().asUnitConversionResponses()
    }

    @Transactional(readOnly = true)
    override fun convert(quantity: Double, fromUnitId: String, toUnitId: String, entityId: String?): Double {
        if (fromUnitId == toUnitId) {
            return quantity
        }

        val conversion = findExactConversion(fromUnitId, toUnitId, entityId)
        if (conversion != null) {
            return quantity * conversion.multiplier.toDouble()
        }

        val inverse = findExactConversion(toUnitId, fromUnitId, entityId)
        if (inverse != null && inverse.multiplier != BigDecimal.ZERO) {
            return quantity / inverse.multiplier.toDouble()
        }

        val path = findConversionPath(fromUnitId, toUnitId, entityId)
        if (path != null) {
            return applyPathConversion(quantity, path)
        }

        throw IllegalArgumentException("No conversion available from $fromUnitId to $toUnitId")
    }

    @Transactional
    override fun create(request: UnitConversionRequest): UnitConversionResponse {
        validateRequest(request)
        validateNoCircularConversionInternal(request.baseUnitId, request.derivedUnitId, request.entityId, excludeUid = null)

        val entity = UnitConversion().apply {
            applyRequest(request)
            active = true
        }

        val saved = unitConversionRepository.save(entity)
        return saved.asUnitConversionResponse()
    }

    @Transactional
    override fun update(uid: String, request: UnitConversionRequest): UnitConversionResponse {
        val existing = unitConversionRepository.findByUid(uid)
            ?: throw UnitNotFoundException("Unit conversion not found for uid: $uid")

        validateRequest(request)
        validateNoCircularConversionInternal(request.baseUnitId, request.derivedUnitId, request.entityId, excludeUid = uid)

        existing.applyRequest(request.copy(uid = uid))
        val saved = unitConversionRepository.save(existing)
        return saved.asUnitConversionResponse()
    }

    @Transactional
    override fun delete(uid: String) {
        val conversion = unitConversionRepository.findByUid(uid)
            ?: throw UnitNotFoundException("Unit conversion not found for uid: $uid")
        conversion.active = false
        unitConversionRepository.save(conversion)
    }

    private fun validateNoCircularConversionInternal(
        baseUnitId: String,
        derivedUnitId: String,
        entityId: String?,
        excludeUid: String?
    ) {
        if (baseUnitId == derivedUnitId) {
            throw CircularConversionException(listOf(baseUnitId, derivedUnitId))
        }

        val adjacency = buildAdjacencyMap(entityId, excludeUid)
        adjacency.getOrPut(baseUnitId) { mutableSetOf() }.add(derivedUnitId)

        val cyclePath = findPath(adjacency, derivedUnitId, baseUnitId)
        if (cyclePath != null) {
            val cycle = listOf(baseUnitId) + cyclePath
            throw CircularConversionException(cycle)
        }
    }

    override fun validateNoCircularConversion(baseUnitId: String, derivedUnitId: String, entityId: String?) {
        validateNoCircularConversionInternal(baseUnitId, derivedUnitId, entityId, excludeUid = null)
    }

    private fun validateRequest(request: UnitConversionRequest) {
        if (request.baseUnitId == request.derivedUnitId) {
            throw IllegalArgumentException("Base unit and derived unit cannot be the same")
        }
        if (request.multiplier <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Conversion multiplier must be greater than zero")
        }

        unitRepository.findByUid(request.baseUnitId)
            ?: throw UnitNotFoundException("Base unit not found: ${request.baseUnitId}")
        unitRepository.findByUid(request.derivedUnitId)
            ?: throw UnitNotFoundException("Derived unit not found: ${request.derivedUnitId}")

        val duplicate = unitConversionRepository.findActiveExactConversion(request.baseUnitId, request.derivedUnitId, request.entityId)
        if (duplicate != null && duplicate.uid != request.uid) {
            throw IllegalArgumentException("Conversion already exists for ${request.baseUnitId} -> ${request.derivedUnitId}")
        }
    }

    private fun findExactConversion(baseUnitId: String, derivedUnitId: String, entityId: String?): UnitConversion? {
        val scoped = unitConversionRepository.findActiveExactConversion(baseUnitId, derivedUnitId, entityId)
        if (scoped != null) return scoped
        if (entityId != null) {
            return unitConversionRepository.findActiveExactConversion(baseUnitId, derivedUnitId, null)
        }
        return null
    }

    private fun buildAdjacencyMap(entityId: String?, excludeUid: String?): MutableMap<String, MutableSet<String>> {
        val adjacency = mutableMapOf<String, MutableSet<String>>()
        unitConversionRepository.findAllActive().forEach { conversion ->
            if (excludeUid != null && conversion.uid == excludeUid) return@forEach
            if (!isConversionApplicable(conversion, entityId)) return@forEach
            adjacency.getOrPut(conversion.baseUnitId) { mutableSetOf() }.add(conversion.derivedUnitId)
        }
        return adjacency
    }

    private fun isConversionApplicable(conversion: UnitConversion, entityId: String?): Boolean {
        return conversion.entityId == null || conversion.entityId == entityId
    }

    private fun findPath(graph: Map<String, Set<String>>, start: String, target: String): List<String>? {
        val visited = mutableSetOf<String>()
        val path = mutableListOf<String>()

        fun dfs(node: String): Boolean {
            if (!visited.add(node)) return false
            path.add(node)
            if (node == target) {
                return true
            }
            for (next in graph[node].orEmpty()) {
                if (dfs(next)) {
                    return true
                }
            }
            path.removeAt(path.size - 1)
            return false
        }

        return if (dfs(start)) path.toList() else null
    }

    private fun findConversionPath(fromUnitId: String, toUnitId: String, entityId: String?): List<UnitConversion>? {
        val adjacency = buildAdjacencyMap(entityId, excludeUid = null)
        val queue: ArrayDeque<Pair<String, List<String>>> = ArrayDeque()
        val visited = mutableSetOf<String>()

        queue.add(fromUnitId to listOf())
        visited.add(fromUnitId)

        while (queue.isNotEmpty()) {
            val (current, path) = queue.removeFirst()
            if (current == toUnitId) {
                val conversionSequence = path.mapIndexed { index, unitId ->
                    val nextUnit = if (index + 1 < path.size) path[index + 1] else toUnitId
                    findExactConversion(unitId, nextUnit, entityId)
                }
                val resolved = conversionSequence.filterNotNull()
                if (resolved.size == conversionSequence.size) {
                    return resolved
                }
            }

            val neighbors = adjacency[current].orEmpty()
            neighbors.forEach { neighbor ->
                if (visited.add(neighbor)) {
                    queue.add(neighbor to (path + listOf(current)))
                }
            }
        }

        return null
    }

    private fun applyPathConversion(quantity: Double, path: List<UnitConversion>): Double {
        var result = quantity
        path.forEach { conversion ->
            result *= conversion.multiplier.toDouble()
        }
        return result
    }
}
