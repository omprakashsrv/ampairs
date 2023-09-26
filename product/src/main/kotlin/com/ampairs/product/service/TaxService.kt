package com.ampairs.product.service

import com.ampairs.product.domain.model.TaxCode
import com.ampairs.product.domain.model.TaxInfo
import com.ampairs.product.repository.TaxCodeRepository
import com.ampairs.product.repository.TaxInfoRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import kotlin.jvm.optionals.getOrNull


@Service
class TaxService(
    val taxInfoRepository: TaxInfoRepository,
    val taxCodeRepository: TaxCodeRepository,
) {
    @Transactional
    fun updateTaxInfos(taxInfos: List<TaxInfo>): List<TaxInfo> {
        taxInfos.forEach {
            if (it.id.isNotEmpty()) {
                val group = taxInfoRepository.findById(it.id).getOrNull()
                it.seqId = group?.seqId
                it.refId = group?.refId ?: ""
            }
            taxInfoRepository.save(it)
        }
        return taxInfos
    }

    fun getTaxInfos(): List<TaxInfo> {
        return taxInfoRepository.findAll().toList()
    }

    fun getTaxCodes(): List<TaxCode> {
        return taxCodeRepository.findAll().toList()
    }

    @Transactional
    fun updateTaxCodes(taxCodes: List<TaxCode>): List<TaxCode> {
        taxCodes.forEach {
            if (it.id.isNotEmpty()) {
                val group = taxCodeRepository.findById(it.id).getOrNull()
                it.seqId = group?.seqId
                it.refId = group?.refId ?: ""
            }
            taxCodeRepository.save(it)
        }
        return taxCodes
    }


}