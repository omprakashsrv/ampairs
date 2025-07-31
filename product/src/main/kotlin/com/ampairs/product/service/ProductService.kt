package com.ampairs.product.service

import com.ampairs.product.domain.model.Product
import com.ampairs.product.domain.model.TaxCode
import com.ampairs.product.domain.model.Unit
import com.ampairs.product.domain.model.group.ProductBrand
import com.ampairs.product.domain.model.group.ProductCategory
import com.ampairs.product.domain.model.group.ProductGroup
import com.ampairs.product.domain.model.group.ProductSubCategory
import com.ampairs.product.repository.*
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    val productPagingRepository: ProductPagingRepository,
    val unitRepository: UnitRepository,
    val productGroupRepository: ProductGroupRepository,
    val productBrandRepository: ProductBrandRepository,
    val productCategoryRepository: ProductCategoryRepository,
    val productSubCategoryRepository: ProductSubCategoryRepository,
    val productRepository: ProductRepository,
    private val taxCodeRepository: TaxCodeRepository,
) {

    fun getProducts(lastUpdated: Long?): List<Product> {
        return productPagingRepository.findAllByLastUpdatedGreaterThanEqual(
            lastUpdated ?: 0,
            PageRequest.of(0, 1000, Sort.by("lastUpdated").ascending())
        )
    }


    @Transactional
    fun updateTaxCodes(taxCodes: List<TaxCode>): List<TaxCode> {
        taxCodes.forEach {
            val taxCode = taxCodeRepository.findByCode(it.code)
            it.id = taxCode?.id ?: 0
            it.seqId = taxCode?.seqId ?: ""
            taxCodeRepository.save(it)
        }
        return taxCodes
    }

    @Transactional
    fun updateProducts(products: List<Product>): List<Product> {
        products.forEach {
            if (it.seqId.isNotEmpty()) {
                val group = productRepository.findBySeqId(it.seqId)
                it.id = group?.id ?: 0
                it.refId = group?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val group = productRepository.findByRefId(it.refId)
                it.id = group?.id ?: 0
                it.seqId = group?.seqId ?: ""
            }
            productRepository.save(it)
        }
        return products
    }


    @Transactional
    fun updateUnits(units: List<Unit>): List<Unit> {
        units.forEach {
            if (it.seqId.isNotEmpty()) {
                val unit = unitRepository.findBySeqId(it.seqId)
                it.id = unit?.id ?: 0
                it.refId = unit?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val unit = unitRepository.findByRefId(it.refId)
                it.id = unit?.id ?: 0
                it.seqId = unit?.seqId ?: ""
            }
            unitRepository.save(it)
        }
        return units
    }

    @Transactional
    fun updateProductGroups(groups: List<ProductGroup>): List<ProductGroup> {
        groups.forEach {
            if (it.seqId.isNotEmpty()) {
                val group = productGroupRepository.findBySeqId(it.seqId)
                it.id = group?.id ?: 0
                it.refId = group?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val group = productGroupRepository.findByRefId(it.refId)
                it.id = group?.id ?: 0
                it.seqId = group?.seqId ?: ""
            }
            productGroupRepository.save(it)
        }
        return groups
    }

    @Transactional
    fun updateProductBrands(brands: List<ProductBrand>): List<ProductBrand> {
        brands.forEach {
            if (it.seqId.isNotEmpty()) {
                val group = productBrandRepository.findBySeqId(it.seqId)
                it.id = group?.id ?: 0
                it.refId = group?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val group = productBrandRepository.findByRefId(it.refId)
                it.id = group?.id ?: 0
                it.seqId = group?.seqId ?: ""
            }
            it.lastUpdated = System.currentTimeMillis()
            productBrandRepository.save(it)
        }
        return brands
    }


    @Transactional
    fun updateProductCategories(productCategories: List<ProductCategory>): List<ProductCategory> {
        productCategories.forEach {
            if (it.seqId.isNotEmpty()) {
                val productCategory = productCategoryRepository.findBySeqId(it.seqId)
                it.id = productCategory?.id ?: 0
                it.refId = productCategory?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val productCategory = productCategoryRepository.findByRefId(it.refId)
                it.id = productCategory?.id ?: 0
                it.seqId = productCategory?.seqId ?: ""
            }
            productCategoryRepository.save(it)
        }
        return productCategories
    }


    @Transactional
    fun updateProductSubCategories(productSubCategories: List<ProductSubCategory>): List<ProductSubCategory> {
        productSubCategories.forEach {
            if (it.seqId.isNotEmpty()) {
                val productCategory = productSubCategoryRepository.findBySeqId(it.seqId)
                it.id = productCategory?.id ?: 0
                it.refId = productCategory?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val productCategory = productSubCategoryRepository.findByRefId(it.refId)
                it.id = productCategory?.id ?: 0
                it.seqId = productCategory?.seqId ?: ""
            }
            productSubCategoryRepository.save(it)
        }
        return productSubCategories
    }

    fun getGroups(): List<ProductGroup> {
        return productGroupRepository.findAll().toList()
    }

    fun getUnits(): List<Unit> {
        return unitRepository.findAll().toList()
    }

    fun getBrands(): List<ProductBrand> {
        return productBrandRepository.findAll().toList()
    }

    fun getSubCategories(): List<ProductSubCategory> {
        return productSubCategoryRepository.findAll().toList()
    }

    fun getProducts(groupId: String): List<Product> {
        return productRepository.getProduct(groupId)
    }

    fun getCategories(ids: Set<String>): List<ProductCategory> {
        return productCategoryRepository.findBySeqIds(ids.toList())
    }

    fun getCategories(): List<ProductCategory> {
        return productCategoryRepository.findAll().toMutableList()
    }

}