package com.ampairs.tax.service

import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.tax.domain.dto.TaxRuleDto
import com.ampairs.tax.domain.dto.UpdateTaxRuleRequest
import com.ampairs.tax.domain.dto.asDto
import com.ampairs.tax.domain.dto.asTaxRuleDtos
import com.ampairs.tax.domain.dto.toEntity
import com.ampairs.tax.repository.TaxRuleRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class TaxRuleService(
    private val taxRuleRepository: TaxRuleRepository
) {

    fun getTaxRules(
        modifiedAfter: Long?,
        taxCode: String?,
        page: Int,
        size: Int
    ): PageResponse<TaxRuleDto> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by("updatedAt").ascending())

        val result = when {
            modifiedAfter != null -> {
                taxRuleRepository.findByUpdatedAtAfter(
                    modifiedAfter = Instant.ofEpochMilli(modifiedAfter),
                    pageable = pageable
                )
            }

            taxCode != null -> {
                taxRuleRepository.findByTaxCode(
                    taxCode = taxCode,
                    pageable = pageable
                )
            }

            else -> {
                taxRuleRepository.findAll(
                    pageable
                )
            }
        }

        return PageResponse.from(result) { it.asDto() }
    }

    fun findByTaxCodeId(taxCodeId: String): List<TaxRuleDto> {
        val rules = taxRuleRepository.findByTaxCodeId(taxCodeId)
        return rules.asTaxRuleDtos()
    }

    @Transactional(readOnly = true)
    fun getById(taxRuleId: String): TaxRuleDto {
        val taxRule = taxRuleRepository.findByUid(taxRuleId)
            ?: throw NotFoundException("Tax rule not found: $taxRuleId")
        return taxRule.asDto()
    }

    @Transactional
    fun updateTaxRule(taxRuleId: String, request: UpdateTaxRuleRequest): TaxRuleDto {
        val taxRule = taxRuleRepository.findByUid(taxRuleId)
            ?: throw NotFoundException("Tax rule not found: $taxRuleId")

        taxRule.apply {
            request.jurisdiction?.let { jurisdiction = it }
            request.jurisdictionLevel?.let { jurisdictionLevel = it }
            request.componentComposition?.let {
                componentComposition = it.mapValues { entry -> entry.value.toEntity() }
            }
            request.isActive?.let { isActive = it }
        }

        return taxRuleRepository.save(taxRule).asDto()
    }

    @Transactional
    fun deleteTaxRule(taxRuleId: String) {
        val taxRule = taxRuleRepository.findByUid(taxRuleId)
            ?: throw NotFoundException("Tax rule not found: $taxRuleId")

        // Soft delete
        taxRule.isActive = false
        taxRuleRepository.save(taxRule)
    }
}
