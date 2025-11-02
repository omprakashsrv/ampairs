package com.ampairs.unit.repository

import com.ampairs.unit.domain.model.Unit
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface UnitRepository : CrudRepository<Unit, Long> {
    fun findByUid(uid: String?): Unit?
    fun findByRefId(refId: String?): Unit?

    @EntityGraph("Unit.basic")
    fun findAllByActiveTrueOrderByName(): List<Unit>

    @Query("select u from unit u where u.active = true order by u.name")
    fun findAllActive(): List<Unit>
}
