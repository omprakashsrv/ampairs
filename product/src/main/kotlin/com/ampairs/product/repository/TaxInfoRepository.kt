package com.ampairs.product.repository

import com.ampairs.product.domain.model.TaxInfo
import org.springframework.data.repository.CrudRepository

interface TaxInfoRepository : CrudRepository<TaxInfo, Long> {
    fun findBySeqId(seqId: String?): TaxInfo?
    fun findByRefId(refId: String?): TaxInfo?

}