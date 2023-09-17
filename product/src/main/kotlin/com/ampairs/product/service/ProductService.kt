package com.ampairs.product.service

import com.ampairs.product.domain.dto.asDatabaseModel
import com.ampairs.product.domain.enums.TaxSpec
import com.ampairs.product.domain.model.*
import com.ampairs.product.domain.model.Unit
import com.ampairs.product.repository.*
import com.ampairs.tally.model.TallyMessage
import com.ampairs.tally.model.TallyXML
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*


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
        tallyXML?.body?.importData?.requestData?.tallyMessage?.forEach {
            updateMasters(it)
        }
        val taxCodeSet = mutableSetOf<TaxCode>()
        tallyXML?.body?.importData?.requestData?.tallyMessage?.forEach {
            it?.stockItem?.let {
                it.gstDetailList?.forEach {
                    val taxCode = TaxCode()
                    taxCode.code = it.hsnCode ?: ""
                    taxCode.effectiveFrom =
                        Timestamp(SimpleDateFormat("yyyyMMdd", Locale.ENGLISH).parse(it.applicableFrom).time)
                    taxCode.description = it.hsnMasterName ?: ""
                    val rateDetailsList = it.stateWiseDetailsList?.get(0)?.rateDetailsList
                    val taxInfos = mutableListOf<TaxInfo>()
                    val cgst =
                        rateDetailsList?.find { it.gstRateDutyHead == "Central Tax" }?.gstRate?.toDouble() ?: 0.0
                    if (cgst > 0) {
                        val cgstInfo = TaxInfo()
                        cgstInfo.name = "CGST " + cgst + "%"
                        cgstInfo.formattedName = "CGST " + cgst + "%"
                        cgstInfo.taxSpec = TaxSpec.INTER
                        cgstInfo.percentage = cgst
                        taxInfos.add(cgstInfo)
                    }
                    val sgst =
                        rateDetailsList?.find { it.gstRateDutyHead == "State Tax" }?.gstRate?.toDouble() ?: 0.0
                    if (sgst > 0) {
                        val sgstInfo = TaxInfo()
                        sgstInfo.name = "SGST " + sgst + "%"
                        sgstInfo.formattedName = "SGST " + sgst + "%"
                        sgstInfo.taxSpec = TaxSpec.INTER
                        sgstInfo.percentage = sgst
                        taxInfos.add(sgstInfo)
                    }
                    val igst =
                        rateDetailsList?.find { it.gstRateDutyHead == "Integrated Tax" }?.gstRate?.toDouble() ?: 0.0
                    if (igst > 0) {
                        val igstInfo = TaxInfo()
                        igstInfo.name = "IGST " + igst + "%"
                        igstInfo.formattedName = "IGST " + igst + "%"
                        igstInfo.taxSpec = TaxSpec.INTER
                        igstInfo.percentage = igst
                        taxInfos.add(igstInfo)
                    }
                    val cess = rateDetailsList?.find { it.gstRateDutyHead == "Cess" }?.gstRate?.toDouble() ?: 0.0
                    if (cess > 0) {
                        val cessInfo = TaxInfo()
                        cessInfo.name = "CESS " + cess + "%"
                        cessInfo.formattedName = "CESS " + cess + "%"
                        cessInfo.taxSpec = TaxSpec.INTER
                        cessInfo.percentage = cess
                        taxInfos.add(cessInfo)
                    }
                    taxCode.taxInfos = taxInfos
                    taxCodeSet.add(taxCode)
                }
            }
        }
        updateTaxCodes(taxCodeSet.toMutableList())
        val productCategories = productCategoryRepository.findAll()
        val productGroups = productGroupRepository.findAll()
        val units = unitRepository.findAll()
        val products = tallyXML?.body?.importData?.requestData?.tallyMessage.orEmpty().map {
            val product = it?.stockItem?.asDatabaseModel()
            product?.groupId = productGroups.find { it1 ->
                it1.name == it?.stockItem?.parent
            }?.id
            product?.categoryId = productCategories.find { it1 ->
                it1.name == it?.stockItem?.category
            }?.id
            product?.baseUnitId = units.find { it1 ->
                it1.name == it?.stockItem?.baseUnits
            }?.id
            product
        }
//        updateProducts(products)
    }

    fun updateTaxCodes(taxCodes: List<TaxCode>): List<TaxCode> {
        taxCodes.forEach {
            val taxCode = taxCodeRepository.findByCode(it.code)
            it.seqId = taxCode?.seqId
            it.id = taxCode?.id ?: ""
            taxCodeRepository.save(it)
        }
        return taxCodes
    }

    fun updateProducts(products: List<Product>): List<Product> {
        products.forEach {
            val product = productRepository.findByRefId(it.refId)
            it.seqId = product?.seqId
            it.id = product?.id ?: ""
            productRepository.save(it)
        }
        return products;
    }

    fun updateUnits(units: List<Unit>): List<Unit> {
        units.forEach {
            val unit = unitRepository.findByRefId(it.refId)
            it.seqId = unit?.seqId
            it.id = unit?.id ?: ""
            unitRepository.save(it)
        }
        return units
    }

    fun updateProductGroups(ownerId: String, groups: List<ProductGroup>): List<ProductGroup> {
        groups.forEach {
            val unit = productGroupRepository.findByRefId(it.refId)
            it.seqId = unit?.seqId
            it.id = unit?.id ?: ""
            productGroupRepository.save(it)
        }
        return groups
    }

    fun updateProductCategories(ownerId: String, productCategories: List<ProductCategory>): List<ProductCategory> {
        productCategories.forEach {
            val category = productCategoryRepository.findByRefId(it.refId)
            it.seqId = category?.seqId
            it.id = category?.id ?: ""
            productCategoryRepository.save(it)
        }
        return productCategories
    }

    fun getGroups(): List<ProductGroup> {
        return productGroupRepository.findAll().toList()
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
        return productCategoryRepository.findByIds(ids.toList())
    }

    fun getCategories(): List<ProductCategory> {
        return productCategoryRepository.findAll().toMutableList()
    }

}