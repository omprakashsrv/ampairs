package com.ampairs.unit.connector

import com.ampairs.core.connector.AbstractRefIdEntityWriter
import com.ampairs.unit.domain.model.Unit
import com.ampairs.unit.repository.UnitRepository
import org.springframework.stereotype.Component

/**
 * Applies connector-mapped columns onto Unit entities (spec 013, Principle IX). Matched by `refId`
 * (e.g. Tally GUID) or `uid`; otherwise a new unit is created.
 */
@Component
class ConnectorUnitWriter(
    private val repository: UnitRepository,
) : AbstractRefIdEntityWriter<Unit>() {
    override val entityType: String = "unit"
    override fun findByRefId(refId: String): Unit? = repository.findByRefId(refId)
    override fun findByUid(uid: String): Unit? = repository.findByUid(uid)
    override fun newInstance(): Unit = Unit()
    override fun persist(entity: Unit): Unit = repository.save(entity)
}
