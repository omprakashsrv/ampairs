package com.ampairs.product.repository

import com.ampairs.product.domain.model.Unit
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface UnitRepository : CrudRepository<Unit, String> {
    fun findByRefId(refId: String?): Unit?

    @Query("SELECT unit FROM unit unit WHERE unit.id = :id")
    override fun findById(id: String): Optional<Unit>


}