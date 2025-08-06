package com.ampairs.product.service

import com.ampairs.product.domain.model.TaxCode
import com.ampairs.product.domain.model.TaxInfo
import com.ampairs.product.repository.TaxCodeRepository
import com.ampairs.product.repository.TaxInfoRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service


@Service
class TaxService(
    val taxInfoRepository: TaxInfoRepository,
    val taxCodeRepository: TaxCodeRepository,
) {
    @Transactional
    fun updateTaxInfos(taxInfos: List<TaxInfo>): List<TaxInfo> {
        taxInfos.forEach {
            if (it.uid.isNotEmpty()) {
                val taxInfo = taxInfoRepository.findByUid(it.uid)
                it.id = taxInfo?.id ?: 0
                it.refId = taxInfo?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val taxInfo = taxInfoRepository.findByRefId(it.refId)
                it.id = taxInfo?.id ?: 0
                it.uid = taxInfo?.uid ?: ""
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
            if (it.uid.isNotEmpty()) {
                val taxCode = taxCodeRepository.findByUid(it.uid)
                it.id = taxCode?.id ?: 0
                it.refId = taxCode?.refId ?: ""
            } else if (it.refId?.isNotEmpty() == true) {
                val taxCode = taxCodeRepository.findByRefId(it.refId)
                it.id = taxCode?.id ?: 0
                it.uid = taxCode?.uid ?: ""
            }
            taxCodeRepository.save(it)
        }
        return taxCodes
    }


}