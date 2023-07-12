package com.ampairs.product.service

import com.ampairs.product.domain.dto.asDatabaseModel
import com.ampairs.product.domain.model.Product
import com.ampairs.product.repository.*
import com.ampairs.tally.model.TallyMessage
import com.ampairs.tally.model.TallyXML
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class ProductService(
    val productPagingRepository: ProductPagingRepository,
    val unitRepository: UnitRepository,
    val productGroupRepository: ProductGroupRepository,
    val productCategoryRepository: ProductCategoryRepository,
    val productRepository: ProductRepository
) {

    fun getProducts(ownerId: String, lastUpdated: Long?): List<Product> {
        return productPagingRepository.findAllByOwnerIdAndLastUpdatedGreaterThanEqual(
            ownerId,
            lastUpdated ?: 0,
            Sort.by("lastUpdated").ascending()
        )
    }

    private fun updateMasters(tallyMessage: TallyMessage?) {
        tallyMessage?.unit?.asDatabaseModel()?.let {
            val unit = unitRepository.findByRefId(it.refId)
            it.seqId = unit?.seqId
            it.id = unit?.id ?: ""
            unitRepository.save(it)
        }
        tallyMessage?.stockGroup?.asDatabaseModel()?.let {
            val productGroup = productGroupRepository.findByRefId(it.refId)
            it.seqId = productGroup?.seqId
            it.id = productGroup?.id ?: ""
            productGroupRepository.save(it)
        }
        tallyMessage?.stockCategory?.asDatabaseModel()?.let {
            val productCategory = productCategoryRepository.findByRefId(it.refId)
            it.seqId = productCategory?.seqId
            it.id = productCategory?.id ?: ""
            productCategoryRepository.save(it)
        }
    }

    @Transactional
    fun updateTallyXml(tallyXML: TallyXML?) {
        for (tallyMessage in tallyXML?.body?.importData?.requestData?.tallyMessage.orEmpty()) {
            updateMasters(tallyMessage)
        }
        val productCategories = productCategoryRepository.findAll()
        val productGroups = productGroupRepository.findAll()
        val products = tallyXML?.body?.importData?.requestData?.tallyMessage.orEmpty().map {
            val product = it?.stockItem?.asDatabaseModel()
            product?.groupId = productGroups.find { it1 ->
                it1.name == it?.stockItem?.parent
            }?.id ?: ""
            product?.categoryId = productCategories.find { it1 ->
                it1.name == it?.stockItem?.category
            }?.id ?: ""
            product
        }
//        updateProducts(products)
    }

    private fun updateProducts(products: List<Product?>) {
        products.forEach {
            it?.let {
                val product = productRepository.findByRefId(it.refId)
                it.seqId = product?.seqId
                it.id = product?.id ?: ""
                productRepository.save(it)
            }
        }
    }


}