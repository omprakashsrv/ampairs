package com.ampairs.tax.service

import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.tax.domain.dto.TaxComponentDto
import com.ampairs.tax.domain.dto.WorkspaceTaxComponentDto
import com.ampairs.tax.domain.dto.asDto
import com.ampairs.tax.domain.dto.asWorkspaceComponentDto
import com.ampairs.tax.repository.TaxComponentRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class TaxComponentService(
    private val taxComponentRepository: TaxComponentRepository
) {

    fun getTaxComponents(
        modifiedAfter: Long?,
        taxType: String?,
        jurisdiction: String?,
        page: Int,
        size: Int
    ): PageResponse<TaxComponentDto> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by("componentName").ascending())

        val result = when {
            modifiedAfter != null -> {
                taxComponentRepository.findByUpdatedAtAfter(
                    modifiedAfter = Instant.ofEpochMilli(modifiedAfter),
                    pageable = pageable
                )
            }
            taxType != null -> {
                taxComponentRepository.findByTaxType(
                    taxType = taxType,
                    pageable = pageable
                )
            }
            jurisdiction != null -> {
                taxComponentRepository.findByJurisdiction(
                    jurisdiction = jurisdiction,
                    pageable = pageable
                )
            }
            else -> {
                taxComponentRepository.findAllActive(pageable)
            }
        }

        return PageResponse.from(result) { it.asDto() }
    }

    /**
     * Workspace components shaped for the mobile `WorkspaceTaxComponent` model
     * (normalised jurisdiction level + `effectiveFrom`). Backs `GET /tax/v1/components`,
     * pulled after a tax-code subscribe so the client can resolve newly subscribed codes.
     */
    fun getWorkspaceComponents(
        modifiedAfter: Long?,
        page: Int,
        size: Int
    ): PageResponse<WorkspaceTaxComponentDto> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by("updatedAt").ascending())

        val result = if (modifiedAfter != null) {
            taxComponentRepository.findByUpdatedAtAfter(
                modifiedAfter = Instant.ofEpochMilli(modifiedAfter),
                pageable = pageable
            )
        } else {
            taxComponentRepository.findAllActive(pageable)
        }

        return PageResponse.from(result) { it.asWorkspaceComponentDto() }
    }
}
