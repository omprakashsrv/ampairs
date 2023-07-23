package com.ampairs.product.service

import com.ampairs.product.domain.dto.asDatabaseModel
import com.ampairs.product.domain.model.Product
import com.ampairs.product.domain.model.TaxCode
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
    val productCategoryRepository: ProductCategoryRepository,
    val productRepository: ProductRepository,
    private val taxCodeRepository: TaxCodeRepository
) {

    fun getProducts(ownerId: String, lastUpdated: Long?): List<Product> {
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
                    taxCode.cgst =
                        rateDetailsList?.find { it.gstRateDutyHead == "Central Tax" }?.gstRate?.toDouble() ?: 0.0
                    taxCode.sgst =
                        rateDetailsList?.find { it.gstRateDutyHead == "State Tax" }?.gstRate?.toDouble() ?: 0.0
                    taxCode.igst =
                        rateDetailsList?.find { it.gstRateDutyHead == "Integrated Tax" }?.gstRate?.toDouble() ?: 0.0
                    taxCode.cess = rateDetailsList?.find { it.gstRateDutyHead == "Cess" }?.gstRate?.toDouble() ?: 0.0
                    taxCodeSet.add(taxCode)
                }
            }
        }
        updateTaxCodes(taxCodeSet)
        val productCategories = productCategoryRepository.findAll()
        val productGroups = productGroupRepository.findAll()
        val units = unitRepository.findAll()
        val products = tallyXML?.body?.importData?.requestData?.tallyMessage.orEmpty().map {
            val product = it?.stockItem?.asDatabaseModel()
            product?.groupId = productGroups.find { it1 ->
                it1.name == it?.stockItem?.parent
            }?.id ?: null
            product?.categoryId = productCategories.find { it1 ->
                it1.name == it?.stockItem?.category
            }?.id ?: null
            product?.baseUnitId = units.find { it1 ->
                it1.name == it?.stockItem?.baseUnits
            }?.id ?: null
            product
        }
        updateProducts(products)
    }

    private fun updateTaxCodes(taxCodeSet: MutableSet<TaxCode>) {
        taxCodeSet.forEach {
            val taxCode = taxCodeRepository.findByCode(it.refId)
            it.seqId = taxCode?.seqId
            it.id = taxCode?.id ?: ""
            taxCodeRepository.save(it)
        }
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

    fun updateUnits(ownerId: String, units: List<Unit>): List<Unit> {
        units.forEach {
            val unit = unitRepository.findByRefId(it.refId)
            it.seqId = unit?.seqId
            it.id = unit?.id ?: ""
            unitRepository.save(it)
        }
        return units
    }


}