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
                val taxInfo = taxInfoRepository.findById(it.id).getOrNull()
                it.seqId = taxInfo?.seqId
                it.refId = taxInfo?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val taxInfo = taxInfoRepository.findByRefId(it.refId)
                it.seqId = taxInfo?.seqId
                it.id = taxInfo?.id ?: ""
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
                val taxCode = taxCodeRepository.findById(it.id).getOrNull()
                it.seqId = taxCode?.seqId
                it.refId = taxCode?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val taxCode = taxCodeRepository.findByRefId(it.refId)
                it.seqId = taxCode?.seqId
                it.id = taxCode?.id ?: ""
            }
            taxCodeRepository.save(it)
        }
        return taxCodes
    }


}