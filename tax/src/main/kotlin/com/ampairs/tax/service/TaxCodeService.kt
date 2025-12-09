package com.ampairs.tax.service

import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.tax.domain.dto.*
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.domain.model.TaxRule
import com.ampairs.tax.repository.MasterTaxCodeRepository
import com.ampairs.tax.repository.TaxCodeRepository
import com.ampairs.tax.repository.TaxRuleRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional
class TaxCodeService(
    private val taxCodeRepository: TaxCodeRepository,
    private val masterTaxCodeRepository: MasterTaxCodeRepository,
    private val taxRuleRepository: TaxRuleRepository,
    private val gstRuleTemplateService: GstRuleTemplateService
) {

    fun subscribe(request: SubscribeTaxCodeRequest): TaxCodeDto {
        // 1. Fetch master tax code
        val masterCode = masterTaxCodeRepository.findByUid(request.masterTaxCodeId)
            ?: throw NotFoundException("Master tax code not found: ${request.masterTaxCodeId}")

        // 2. Check if already subscribed
        val existing = taxCodeRepository.findByMasterTaxCodeId(request.masterTaxCodeId)
        if (existing != null) {
            throw NotFoundException("Already subscribed to this tax code")
        }

        // 3. Create workspace tax code
        val taxCode = TaxCode().apply {
            masterTaxCodeId = masterCode.uid

            // Cache master data for offline access
            code = masterCode.code
            codeType = masterCode.codeType
            description = masterCode.description
            shortDescription = masterCode.shortDescription

            // Workspace-specific configuration
            customName = request.customName
            usageCount = 0
            lastUsedAt = null
            isFavorite = request.isFavorite
            notes = request.notes

            isActive = true
        }

        val savedTaxCode = taxCodeRepository.save(taxCode)

        // 4. Auto-create tax rule if custom rule not specified
        if (request.customTaxRuleId == null && masterCode.countryCode == "IN" && masterCode.defaultTaxRate != null) {
            createDefaultTaxRule(savedTaxCode, masterCode)
        }

        return savedTaxCode.asDto()
    }

    /**
     * Creates a default tax rule with standard GST component breakdown.
     */
    private fun createDefaultTaxRule(taxCode: TaxCode, masterCode: com.ampairs.tax.domain.model.MasterTaxCode) {
        val taxRate = masterCode.defaultTaxRate ?: return

        // Generate standard GST composition
        val componentComposition = gstRuleTemplateService.generateStandardGstComposition(taxRate)

        val taxRule = TaxRule().apply {
            countryCode = masterCode.countryCode
            taxCodeId = taxCode.uid
            this.taxCode = masterCode.code
            taxCodeType = masterCode.codeType
            taxCodeDescription = masterCode.description
            jurisdiction = "INDIA"
            jurisdictionLevel = "COUNTRY"
            this.componentComposition = componentComposition
            isActive = true
        }

        taxRuleRepository.save(taxRule)
    }

    @Transactional(readOnly = true)
    fun getTaxCodes(
        modifiedAfter: Long?,
        page: Int,
        size: Int
    ): PageResponse<TaxCodeDto> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by("updatedAt").ascending())

        val result = if (modifiedAfter != null) {
            taxCodeRepository.findByUpdatedAtAfter(
                modifiedAfter = Instant.ofEpochMilli(modifiedAfter),
                pageable = pageable
            )
        } else {
            taxCodeRepository.findAllActive(pageable)
        }

        return PageResponse.from(result) { it.asDto() }
    }

    @Transactional(readOnly = true)
    fun getFavorites(page: Int, size: Int): PageResponse<TaxCodeDto> {
        val pageable: Pageable = PageRequest.of(page, size)
        val result = taxCodeRepository.findFavorites(pageable)

        return PageResponse.from(result) { it.asDto() }
    }

    fun unsubscribe(taxCodeId: String) {
        val taxCode = taxCodeRepository.findByUid(taxCodeId)
            ?: throw NotFoundException("Tax code not found: $taxCodeId")

        // Soft delete
        taxCode.isActive = false
        taxCodeRepository.save(taxCode)
    }

    fun updateConfiguration(taxCodeId: String, request: UpdateTaxCodeRequest): TaxCodeDto {
        val taxCode = taxCodeRepository.findByUid(taxCodeId)
            ?: throw NotFoundException("Tax code not found: $taxCodeId")

        taxCode.apply {
            request.isFavorite?.let { isFavorite = it }
            request.notes?.let { notes = it }
            request.customName?.let { customName = it }
        }

        return taxCodeRepository.save(taxCode).asDto()
    }

    fun incrementUsage(taxCodeId: String) {
        val taxCode = taxCodeRepository.findByUid(taxCodeId)
            ?: throw NotFoundException("Tax code not found: $taxCodeId")

        taxCode.apply {
            usageCount += 1
            lastUsedAt = Instant.now()
        }

        taxCodeRepository.save(taxCode)
    }

    fun bulkSubscribe(request: BulkSubscribeTaxCodesRequest): BulkSubscribeResultDto {
        val subscribedCodes = mutableListOf<TaxCodeDto>()
        val errors = mutableListOf<BulkOperationErrorDto>()

        request.masterTaxCodeIds.forEach { masterCodeId ->
            try {
                val subscribeRequest = SubscribeTaxCodeRequest(
                    masterTaxCodeId = masterCodeId,
                    isFavorite = false,
                    notes = null,
                    customName = null
                )
                val taxCode = subscribe(subscribeRequest)
                subscribedCodes.add(taxCode)
            } catch (e: Exception) {
                errors.add(
                    BulkOperationErrorDto(
                        masterTaxCodeId = masterCodeId,
                        errorMessage = e.message ?: "Subscription failed"
                    )
                )
            }
        }

        return BulkSubscribeResultDto(
            successCount = subscribedCodes.size,
            failureCount = errors.size,
            subscribedCodes = subscribedCodes,
            errors = errors
        )
    }

    @Transactional(readOnly = true)
    fun getById(taxCodeId: String): TaxCodeDto {
        val taxCode = taxCodeRepository.findByUid(taxCodeId)
            ?: throw NotFoundException("Tax code not found: $taxCodeId")
        return taxCode.asDto()
    }

    fun setFavorite(taxCodeId: String, isFavorite: Boolean): TaxCodeDto {
        val taxCode = taxCodeRepository.findByUid(taxCodeId)
            ?: throw NotFoundException("Tax code not found: $taxCodeId")

        taxCode.isFavorite = isFavorite
        return taxCodeRepository.save(taxCode).asDto()
    }
}
