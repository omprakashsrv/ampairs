package com.ampairs.product.connector

import com.ampairs.core.connector.DslEntityWriter
import com.ampairs.inventory.domain.model.Inventory
import com.ampairs.inventory.repository.InventoryRepository
import com.ampairs.product.domain.model.Product
import com.ampairs.product.repository.ProductRepository
import org.springframework.stereotype.Component

/**
 * Tally closing-balance → Product stock (spec 013). Demonstrates the mapping DSL for a COMPLEX target:
 * stock lives in `Inventory` child rows (`Product.inventory` is a non-cascading `@OneToMany`), so the
 * `stockQuantity` field is applied via a custom DSL binding that upserts the Inventory row by
 * `productId`, rather than via flat reflection.
 *
 * Matched by the product's `refId` (Tally GUID); `createIfMissing = false` so a stock row never
 * fabricates a bare product — it's skipped until the product itself syncs.
 */
@Component
class ConnectorProductStockWriter(
    private val productRepository: ProductRepository,
    private val inventoryRepository: InventoryRepository,
) : DslEntityWriter<Product>() {

    override val entityType: String = "stock_balance"
    override val createIfMissing: Boolean = false

    override fun findByRefId(refId: String): Product? = productRepository.findByRefId(refId)
    override fun findByUid(uid: String): Product? = productRepository.findByUid(uid)
    override fun newInstance(): Product = Product() // unused (createIfMissing = false)
    override fun persist(entity: Product): Product = entity // product unchanged; Inventory saved in the DSL binding

    override fun defineFields(f: FieldBinding<Product>) {
        f.field("stockQuantity") { product, value ->
            val qty = (value as? Number)?.toDouble()
                ?: value?.toString()?.trim()?.toDoubleOrNull()
                ?: return@field
            val inventory = inventoryRepository.findByProductId(product.uid)
                ?: Inventory().apply {
                    productId = product.uid
                    description = product.name
                }
            inventory.stock = qty
            inventoryRepository.save(inventory)
        }
    }
}
