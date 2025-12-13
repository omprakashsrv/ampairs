package com.ampairs.tax.service

import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.tax.domain.dto.MasterTaxRuleDto
import com.ampairs.tax.domain.dto.asDto
import com.ampairs.tax.domain.dto.asMasterRuleDtos
import com.ampairs.tax.domain.model.MasterTaxRule
import com.ampairs.tax.repository.MasterTaxRuleRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MasterTaxRuleService(
    private val masterTaxRuleRepository: MasterTaxRuleRepository
) {

    fun getAllRules(page: Int, size: Int): PageResponse<MasterTaxRuleDto> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result = masterTaxRuleRepository.findByIsActiveTrue(pageable)
        return PageResponse.from(result) { it.asDto() }
    }

    fun searchRules(
        countryCode: String,
        taxCodeType: String?,
        page: Int,
        size: Int
    ): PageResponse<MasterTaxRuleDto> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result = masterTaxRuleRepository.searchRules(
            countryCode = countryCode.uppercase(),
            taxCodeType = taxCodeType,
            pageable = pageable
        )
        return PageResponse.from(result) { it.asDto() }
    }

    fun getRuleById(id: String): MasterTaxRuleDto {
        val rule = masterTaxRuleRepository.findByUid(id)
            ?: throw NotFoundException("Master tax rule not found: $id")
        return rule.asDto()
    }

    fun findRulesByMasterTaxCode(masterTaxCodeId: String): List<MasterTaxRuleDto> {
        val rules = masterTaxRuleRepository.findByMasterTaxCodeId(masterTaxCodeId)
        return rules.asMasterRuleDtos()
    }

    fun getRuleByMasterTaxCodeId(masterTaxCodeId: String): MasterTaxRuleDto? {
        val rules = masterTaxRuleRepository.findByMasterTaxCodeId(masterTaxCodeId)
        return rules.firstOrNull()?.asDto()
    }
}
