package com.ampairs.ecom.service

import com.ampairs.ecom.domain.enums.StockStatus
import com.ampairs.ecom.domain.model.EcomListedProduct
import com.ampairs.ecom.repository.EcomListedProductRepository
import com.ampairs.event.domain.kafka.EcomCatalogEvent
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CatalogSyncService(
    private val listedProductRepository: EcomListedProductRepository,
) {

    @Transactional
    fun handleProductListed(event: EcomCatalogEvent) {
        val product = listedProductRepository.findByStorefrontIdAndManagementProductId(
            event.storefrontId, event.managementProductId
        ) ?: EcomListedProduct().also {
            it.storefrontId = event.storefrontId
            it.managementProductId = event.managementProductId
            it.ownerId = event.workspaceId
        }

        product.name = event.name ?: product.name
        product.brand = event.brand
        product.category = event.category
        product.subcategory = event.subcategory
        product.unit = event.unit
        product.price = event.price ?: product.price
        product.mrp = event.mrp ?: product.mrp
        product.stockQuantity = event.stockQuantity ?: product.stockQuantity
        product.stockStatus = resolveStockStatus(product.stockQuantity)
        product.imageUrls = event.imageUrls ?: product.imageUrls
        product.description = event.description
        product.isVisible = true
        product.lastSyncedAt = Instant.now()

        listedProductRepository.save(product)
    }

    @Transactional
    fun handleProductUnlisted(event: EcomCatalogEvent) {
        listedProductRepository.findByStorefrontIdAndManagementProductId(
            event.storefrontId, event.managementProductId
        )?.let {
            it.isVisible = false
            listedProductRepository.save(it)
        }
    }

    @Transactional
    fun handlePriceUpdated(event: EcomCatalogEvent) {
        listedProductRepository.findByStorefrontIdAndManagementProductId(
            event.storefrontId, event.managementProductId
        )?.let {
            event.price?.let { p -> it.price = p }
            event.mrp?.let { m -> it.mrp = m }
            it.lastSyncedAt = Instant.now()
            listedProductRepository.save(it)
        }
    }

    @Transactional
    fun handleStockUpdated(event: EcomCatalogEvent) {
        listedProductRepository.findByStorefrontIdAndManagementProductId(
            event.storefrontId, event.managementProductId
        )?.let {
            event.stockQuantity?.let { qty ->
                it.stockQuantity = qty
                it.stockStatus = resolveStockStatus(qty)
            }
            it.lastSyncedAt = Instant.now()
            listedProductRepository.save(it)
        }
    }

    @Transactional
    fun handleDetailsUpdated(event: EcomCatalogEvent) {
        listedProductRepository.findByStorefrontIdAndManagementProductId(
            event.storefrontId, event.managementProductId
        )?.let {
            event.name?.let { v -> it.name = v }
            event.brand?.let { v -> it.brand = v }
            event.category?.let { v -> it.category = v }
            event.subcategory?.let { v -> it.subcategory = v }
            event.unit?.let { v -> it.unit = v }
            event.mrp?.let { v -> it.mrp = v }
            event.imageUrls?.let { v -> it.imageUrls = v }
            event.description?.let { v -> it.description = v }
            it.lastSyncedAt = Instant.now()
            listedProductRepository.save(it)
        }
    }

    fun resolveStockStatus(qty: Int): StockStatus = when {
        qty > 10 -> StockStatus.IN_STOCK
        qty in 1..10 -> StockStatus.LIMITED
        else -> StockStatus.OUT_OF_STOCK
    }
}
