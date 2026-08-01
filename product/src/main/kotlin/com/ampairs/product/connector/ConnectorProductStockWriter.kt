package com.ampairs.product.connector

import com.ampairs.core.connector.DslEntityWriter
import com.ampairs.product.domain.model.Product
import com.ampairs.product.repository.ProductRepository
import org.springframework.stereotype.Component

/**
 * Tally closing-balance → Product stock (spec 013), demonstrating the mapping DSL for a COMPLEX target.
 *
 * NOTE (merge of `main`, spec 014): the inventory subsystem was redesigned into a multi-warehouse,
 * transaction/ledger-backed model (`com.ampairs.inventory`) where stock is a per-(product, warehouse)
 * `InventoryItem` mutated through movement commands (`InventoryStockService.applySale/applyPurchase`,
 * idempotent per source line) rather than a single settable "stock" value. Tally's *absolute* closing
 * balance no longer maps cleanly onto that model (which warehouse? snapshot vs. movement? — a design
 * decision, not a mechanical port), so the `stockQuantity` binding is intentionally a **no-op** until
 * it is re-mapped as a COUNT-style adjustment into a default warehouse. The writer stays registered so
 * `stock_balance` rows are still accepted and matched to their product without error. Follow-up: spec 013.
 *
 * Matched by the product's `refId` (Tally GUID); `createIfMissing = false` so a stock row never
 * fabricates a bare product — it's skipped until the product itself syncs.
 */
@Component
class ConnectorProductStockWriter(
    private val productRepository: ProductRepository,
) : DslEntityWriter<Product>() {

    override val entityType: String = "stock_balance"
    override val createIfMissing: Boolean = false

    override fun findByRefId(refId: String): Product? = productRepository.findByRefId(refId)
    override fun findByUid(uid: String): Product? = productRepository.findByUid(uid)
    override fun newInstance(): Product = Product() // unused (createIfMissing = false)
    override fun persist(entity: Product): Product = entity // product unchanged; stock write is disabled (see KDoc)

    override fun defineFields(f: FieldBinding<Product>) {
        // stockQuantity intentionally not applied — pending warehouse-aware re-mapping onto the
        // redesigned inventory model (spec 014). Registered so the field is accepted, not rejected.
        f.field("stockQuantity") { _, _ -> /* no-op */ }
    }
}
