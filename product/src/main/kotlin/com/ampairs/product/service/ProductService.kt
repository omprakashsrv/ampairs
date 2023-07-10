package com.ampairs.product.service

import com.ampairs.product.domain.dto.asDatabaseModel
import com.ampairs.product.domain.model.Product
import com.ampairs.product.repository.ProductGroupRepository
import com.ampairs.product.repository.ProductPagingRepository
import com.ampairs.product.repository.ProductRepository
import com.ampairs.product.repository.UnitRepository
import com.ampairs.tally.model.TallyMessage
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
class ProductService(
    val productPagingRepository: ProductPagingRepository,
    val unitRepository: UnitRepository,
    val productGroupRepository: ProductGroupRepository, private val productRepository: ProductRepository
) {

    fun getProducts(ownerId: String, lastUpdated: Long?): List<Product> {
        return productPagingRepository.findAllByOwnerIdAndLastUpdatedGreaterThanEqual(
            ownerId,
            lastUpdated ?: 0,
            Sort.by("lastUpdated").ascending()
        )
    }

    @Transactional
    fun updateMasters(tallyMessage: TallyMessage?) {
        tallyMessage?.unit?.asDatabaseModel()?.let {
            unitRepository.save(it)
        }
        tallyMessage?.stockGroup?.asDatabaseModel()?.let {
            productGroupRepository.save(it)
        }
        tallyMessage?.stockItem?.asDatabaseModel()?.let {
            productRepository.save(it)
        }
    }


}