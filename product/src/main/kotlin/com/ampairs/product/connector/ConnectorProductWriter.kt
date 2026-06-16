package com.ampairs.product.connector

import com.ampairs.core.connector.AbstractRefIdEntityWriter
import com.ampairs.product.domain.model.Product
import com.ampairs.product.repository.ProductRepository
import org.springframework.stereotype.Component

/**
 * Applies connector-mapped columns onto Product entities (spec 013, Principle IX). Matched by `refId`
 * (e.g. Tally GUID) or `uid`; otherwise a new product is created. Only present, allowlisted columns
 * are written.
 */
@Component
class ConnectorProductWriter(
    private val repository: ProductRepository,
) : AbstractRefIdEntityWriter<Product>() {
    override val entityType: String = "product"
    override fun findByRefId(refId: String): Product? = repository.findByRefId(refId)
    override fun findByUid(uid: String): Product? = repository.findByUid(uid)
    override fun newInstance(): Product = Product()
    override fun persist(entity: Product): Product = repository.save(entity)
}
