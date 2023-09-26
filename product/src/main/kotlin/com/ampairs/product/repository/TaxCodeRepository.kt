package com.ampairs.product.repository

import com.ampairs.product.domain.model.TaxCode
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.*

interface TaxCodeRepository : CrudRepository<TaxCode, String> {
    fun findByRefId(refId: String?): TaxCode?
    fun findByCode(code: String?): TaxCode?

    @Query("SELECT tc FROM tax_code tc WHERE tc.id = :id")
    override fun findById(id: String): Optional<TaxCode>
}