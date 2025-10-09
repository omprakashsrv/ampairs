package com.ampairs.product.repository

import com.ampairs.product.domain.model.Unit
import org.springframework.data.repository.CrudRepository

interface UnitRepository : CrudRepository<Unit, Long> {
    fun findByUid(uid: String?): Unit?
    fun findByRefId(refId: String?): Unit?


}