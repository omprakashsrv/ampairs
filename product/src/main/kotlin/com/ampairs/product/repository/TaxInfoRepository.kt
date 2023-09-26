package com.ampairs.product.repository

import com.ampairs.product.domain.model.TaxInfo
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface TaxInfoRepository : CrudRepository<TaxInfo, String> {
    fun findByRefId(refId: String?): TaxInfo?

    @Query("SELECT ti FROM tax_info ti WHERE ti.id = :id")
    override fun findById(id: String): Optional<TaxInfo>

}