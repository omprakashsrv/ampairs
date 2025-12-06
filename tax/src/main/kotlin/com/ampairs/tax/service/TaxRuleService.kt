package com.ampairs.tax.service

import com.ampairs.tax.domain.dto.PageResponse
import com.ampairs.tax.domain.dto.TaxRuleDto
import com.ampairs.tax.domain.dto.asTaxRuleDtos
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

        return PageResponse(
            content = result.content.asTaxRuleDtos(),
            page = result.number,
            size = result.size,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            hasNext = result.hasNext()
        )
    }

    fun findByTaxCodeId(taxCodeId: String): List<TaxRuleDto> {
        val rules = taxRuleRepository.findByTaxCodeId(taxCodeId)
        return rules.asTaxRuleDtos()
    }
}
