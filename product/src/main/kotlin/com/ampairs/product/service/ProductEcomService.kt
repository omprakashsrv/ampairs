package com.ampairs.product.service

import com.ampairs.core.exception.NotFoundException
import com.ampairs.core.service.EcomStorefrontLookupService
import com.ampairs.event.domain.kafka.CatalogEventType
import com.ampairs.event.domain.kafka.EcomCatalogEvent
import com.ampairs.product.domain.model.Product
import com.ampairs.product.kafka.EcomCatalogKafkaProducer
import com.ampairs.product.repository.ProductRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Service
class ProductEcomService(
    private val productRepository: ProductRepository,
    private val ecomCatalogKafkaProducer: EcomCatalogKafkaProducer,
    private val ecomStorefrontLookupService: EcomStorefrontLookupService,
) {
    private val log = LoggerFactory.getLogger(ProductEcomService::class.java)

    @Transactional
    fun listProductOnEcom(productId: String, workspaceId: String): Product {
        val product = productRepository.findByUid(productId)
            ?: throw NotFoundException("Product not found: $productId")
        product.isEcomListed = true
        val saved = productRepository.save(product)

        val storefrontId = ecomStorefrontLookupService.findStorefrontIdByWorkspaceId(workspaceId)
        if (storefrontId == null) {
            log.warn("No storefront found for workspace {}; skipping catalog event for product {}", workspaceId, productId)
            return saved
        }

        ecomCatalogKafkaProducer.publish(buildCatalogEvent(saved, workspaceId, storefrontId, CatalogEventType.PRODUCT_LISTED))
        return saved
    }

    @Transactional
    fun unlistProductFromEcom(productId: String, workspaceId: String): Product {
        val product = productRepository.findByUid(productId)
            ?: throw NotFoundException("Product not found: $productId")
        product.isEcomListed = false
        val saved = productRepository.save(product)

        val storefrontId = ecomStorefrontLookupService.findStorefrontIdByWorkspaceId(workspaceId)
            ?: return saved

        ecomCatalogKafkaProducer.publish(
            EcomCatalogEvent(
                eventType = CatalogEventType.PRODUCT_UNLISTED,
                workspaceId = workspaceId,
                storefrontId = storefrontId,
                managementProductId = product.uid,
                publishedAt = Instant.now(),
            )
        )
        return saved
    }

    fun publishDetailsUpdatedEvent(product: Product, workspaceId: String) {
        if (!product.isEcomListed) return
        val storefrontId = ecomStorefrontLookupService.findStorefrontIdByWorkspaceId(workspaceId) ?: return
        ecomCatalogKafkaProducer.publish(buildCatalogEvent(product, workspaceId, storefrontId, CatalogEventType.DETAILS_UPDATED))
    }

    fun publishPriceUpdatedEvent(product: Product, workspaceId: String) {
        if (!product.isEcomListed) return
        val storefrontId = ecomStorefrontLookupService.findStorefrontIdByWorkspaceId(workspaceId) ?: return
        ecomCatalogKafkaProducer.publish(
            EcomCatalogEvent(
                eventType = CatalogEventType.PRICE_UPDATED,
                workspaceId = workspaceId,
                storefrontId = storefrontId,
                managementProductId = product.uid,
                price = BigDecimal.valueOf(product.sellingPrice),
                publishedAt = Instant.now(),
            )
        )
    }

    fun publishStockUpdatedEvent(product: Product, workspaceId: String) {
        if (!product.isEcomListed) return
        val storefrontId = ecomStorefrontLookupService.findStorefrontIdByWorkspaceId(workspaceId) ?: return
        val totalStock = product.inventory.sumOf { it.stock }.toInt()
        ecomCatalogKafkaProducer.publish(
            EcomCatalogEvent(
                eventType = CatalogEventType.STOCK_UPDATED,
                workspaceId = workspaceId,
                storefrontId = storefrontId,
                managementProductId = product.uid,
                stockQuantity = totalStock,
                publishedAt = Instant.now(),
            )
        )
    }

    private fun buildCatalogEvent(product: Product, workspaceId: String, storefrontId: String, eventType: CatalogEventType): EcomCatalogEvent {
        val totalStock = product.inventory.sumOf { it.stock }.toInt()
        val imageUrls = product.images.mapNotNull { it.image?.objectKey }
        return EcomCatalogEvent(
            eventType = eventType,
            workspaceId = workspaceId,
            storefrontId = storefrontId,
            managementProductId = product.uid,
            name = product.name,
            brand = product.brand?.name,
            category = product.category?.name,
            subcategory = product.subCategory?.name,
            price = BigDecimal.valueOf(product.sellingPrice),
            stockQuantity = totalStock,
            imageUrls = imageUrls,
            description = product.description,
            publishedAt = Instant.now(),
        )
    }
}
