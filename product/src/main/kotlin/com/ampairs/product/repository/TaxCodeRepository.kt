package com.ampairs.product.repository

import com.ampairs.product.domain.model.TaxCode
import org.springframework.data.repository.CrudRepository

interface TaxCodeRepository : CrudRepository<TaxCode, Long> {
    fun findBySeqId(seqId: String?): TaxCode?
    fun findByRefId(refId: String?): TaxCode?
    fun findByCode(code: String?): TaxCode?
}