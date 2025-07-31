package com.ampairs.product.repository

import com.ampairs.product.domain.model.group.ProductGroup
import org.springframework.data.repository.CrudRepository

interface ProductGroupRepository : CrudRepository<ProductGroup, Long> {
    fun findBySeqId(seqId: String?): ProductGroup?
    fun findByRefId(refId: String?): ProductGroup?

}