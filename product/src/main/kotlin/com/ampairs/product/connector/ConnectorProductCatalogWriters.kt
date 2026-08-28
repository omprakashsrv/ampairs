package com.ampairs.product.connector

import com.ampairs.core.connector.AbstractRefIdEntityWriter
import com.ampairs.product.domain.model.group.ProductCategory
import com.ampairs.product.domain.model.group.ProductGroup
import com.ampairs.product.repository.ProductCategoryRepository
import com.ampairs.product.repository.ProductGroupRepository
import org.springframework.stereotype.Component

/** Tally StockGroup → ProductGroup (spec 013). */
@Component
class ConnectorProductGroupWriter(
    private val repository: ProductGroupRepository,
) : AbstractRefIdEntityWriter<ProductGroup>() {
    override val entityType: String = "product_group"
    override fun findByRefId(refId: String): ProductGroup? = repository.findByRefId(refId)
    override fun findByUid(uid: String): ProductGroup? = repository.findByUid(uid)
    override fun newInstance(): ProductGroup = ProductGroup()
    override fun persist(entity: ProductGroup): ProductGroup = repository.save(entity)
}

/** Tally StockCategory → ProductCategory (spec 013). */
@Component
class ConnectorProductCategoryWriter(
    private val repository: ProductCategoryRepository,
) : AbstractRefIdEntityWriter<ProductCategory>() {
    override val entityType: String = "product_category"
    override fun findByRefId(refId: String): ProductCategory? = repository.findByRefId(refId)
    override fun findByUid(uid: String): ProductCategory? = repository.findByUid(uid)
    override fun newInstance(): ProductCategory = ProductCategory()
    override fun persist(entity: ProductCategory): ProductCategory = repository.save(entity)
}
