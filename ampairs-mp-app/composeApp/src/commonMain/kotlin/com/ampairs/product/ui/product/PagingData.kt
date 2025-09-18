package com.ampairs.product.ui.product

import androidx.paging.PagingData
import androidx.paging.map
import com.ampairs.aws.s3.S3Client
import com.ampairs.product.db.entity.ProductEntity
import com.ampairs.product.domain.Product
import com.ampairs.product.domain.asDomainModel
import com.ampairs.repository.ProductRepository

fun PagingData<ProductEntity>.toPagingProduct(
    cartProducts: MutableList<Product>,
    productRepository: ProductRepository,
    s3Client: S3Client,
    onProductQtyChanged: (() -> Unit)?
): PagingData<Product> {
    val productPagingData = this.map { productEntity ->
        val product = productEntity.asDomainModel()
        val cartProductIndex = cartProducts.indexOfFirst { it.id == product.id }
        if (cartProductIndex != -1) {
            product.quantity = cartProducts[cartProductIndex].quantity
            cartProducts[cartProductIndex] = product
            onProductQtyChanged?.invoke()
        }
        // product.inventory = inventoryRepository.getProductInventory(product.id)
        product.images = productRepository.getProductImages(listOf(product.id)).map { it.image }
        if (!product.images.isNullOrEmpty()) {
            product.images!!.map { image ->
                image.url = s3Client.getPreSignedUrl(image.bucket, image.objectKey)
            }
        }
        product
    }
    return productPagingData
}