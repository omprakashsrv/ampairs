package com.ampairs.tax.service

import com.ampairs.tax.domain.dto.MasterTaxCodeDto
import com.ampairs.tax.domain.dto.PageResponse
import com.ampairs.tax.domain.dto.asDto
import com.ampairs.tax.domain.dto.asDtos
import com.ampairs.tax.domain.model.MasterTaxCode
import com.ampairs.tax.repository.MasterTaxCodeRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MasterTaxCodeService(
    private val masterTaxCodeRepository: MasterTaxCodeRepository
) {

    fun searchCodes(
        query: String,
        countryCode: String,
        codeType: String?,
        category: String?,
        page: Int,
        size: Int
    ): PageResponse<MasterTaxCodeDto> {
        val pageable: Pageable = PageRequest.of(page, size)

        val result = masterTaxCodeRepository.searchCodes(
            query = query,
            countryCode = countryCode.uppercase(),
            codeType = codeType,
            category = category,
            pageable = pageable
        )

        return PageResponse(
            content = result.content.asDtos(),
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext()
        )
    }

    fun getPopularCodes(
        countryCode: String,
        industry: String?,
        limit: Int
    ): List<MasterTaxCodeDto> {
        val pageable: Pageable = PageRequest.of(0, limit)

        val result = masterTaxCodeRepository.findPopularCodes(
            countryCode = countryCode.uppercase(),
            industry = industry,
            pageable = pageable
        )

        return result.content.asDtos()
    }

    fun findById(id: String): MasterTaxCode? {
        return masterTaxCodeRepository.findByUid(id)
    }

    fun findByCountryCodeAndCodeTypeAndCode(
        countryCode: String,
        codeType: String,
        code: String
    ): MasterTaxCode? {
        return masterTaxCodeRepository.findByCountryCodeAndCodeTypeAndCode(
            countryCode = countryCode.uppercase(),
            codeType = codeType,
            code = code
        )
    }
}
