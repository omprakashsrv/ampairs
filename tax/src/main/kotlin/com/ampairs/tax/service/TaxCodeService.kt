package com.ampairs.tax.service

import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.core.exception.NotFoundException
import com.ampairs.tax.domain.dto.*
import com.ampairs.tax.domain.model.ComponentComposition
import com.ampairs.tax.domain.model.ComponentReference
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.tax.domain.model.TaxRule
import com.ampairs.tax.repository.MasterTaxCodeRepository
import com.ampairs.tax.repository.TaxCodeRepository
import com.ampairs.tax.repository.TaxComponentRepository
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
    private val masterTaxRuleService: MasterTaxRuleService,
    private val masterTaxComponentService: MasterTaxComponentService,
    private val taxComponentRepository: TaxComponentRepository,
    private val gstRuleTemplateService: GstRuleTemplateService
) {

    fun subscribe(request: SubscribeTaxCodeRequest): TaxCodeDto {
        // 1. Fetch master tax code
        val masterCode = masterTaxCodeRepository.findByUid(request.masterTaxCodeId)
            ?: throw NotFoundException("Master tax code not found: ${request.masterTaxCodeId}")

        // 2. Check if already subscribed
        val existing = taxCodeRepository.findByMasterTaxCodeId(request.masterTaxCodeId)

        if (existing != null) {
            // 2a. If active, return existing subscription (idempotent)
            if (existing.isActive) {
                // Always update fields (idempotent operation)
                existing.apply {
                    customName = request.customName
                    isFavorite = request.isFavorite
                    notes = request.notes
                }
                return taxCodeRepository.save(existing).asDto()
            }

            // 2b. If inactive (soft deleted), reactivate it
            existing.apply {
                isActive = true
                customName = request.customName
                isFavorite = request.isFavorite
                notes = request.notes
                // Update master data cache in case it changed
                code = masterCode.code
                codeType = masterCode.codeType
                description = masterCode.description
                shortDescription = masterCode.shortDescription
            }

            val reactivated = taxCodeRepository.save(existing)

            // Reactivate or create tax rule if needed
            if (request.customTaxRuleId == null && masterCode.countryCode == "IN" && masterCode.defaultTaxRate != null) {
                reactivateOrCreateTaxRule(reactivated, masterCode)
            }

            return reactivated.asDto()
        }

        // 3. Create new workspace tax code
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
     * This method:
     * 1. Looks up master_tax_rule template for this master_tax_code
     * 2. Creates workspace tax_component records from master_tax_component templates
     * 3. Creates workspace tax_rule with references to workspace components
     */
    private fun createDefaultTaxRule(taxCode: TaxCode, masterCode: com.ampairs.tax.domain.model.MasterTaxCode) {
        val taxRate = masterCode.defaultTaxRate ?: return

        // Try to get master tax rule template
        val masterRuleDto = masterTaxRuleService.getRuleByMasterTaxCodeId(masterCode.uid)

        if (masterRuleDto != null) {
            // Use master_tax_rule as template
            createRuleFromMasterTemplate(taxCode, masterCode, masterRuleDto)
        } else {
            // Fallback: Generate standard GST composition (old behavior)
            createRuleWithGeneratedComposition(taxCode, masterCode, taxRate)
        }
    }

    /**
     * Creates workspace tax_rule and tax_component records from master_tax_rule template.
     */
    private fun createRuleFromMasterTemplate(
        taxCode: TaxCode,
        masterCode: com.ampairs.tax.domain.model.MasterTaxCode,
        masterRule: MasterTaxRuleDto
    ) {
        // Step 1: Extract all unique component IDs from master rule composition
        val masterComponentIds = extractComponentIds(masterRule.componentComposition)

        // Step 2: Create workspace tax_component records from master templates
        val componentIdMapping = mutableMapOf<String, String>() // master_id -> workspace_id

        masterComponentIds.forEach { masterComponentId ->
            val masterComponent = masterTaxComponentService.getComponentById(masterComponentId)

            // Check if workspace component already exists
            val existing = taxComponentRepository.findByUid(masterComponentId)

            if (existing == null) {
                // Create workspace component from master template
                val workspaceComponent = com.ampairs.tax.domain.model.TaxComponent().apply {
                    // Use same UID as master for consistency
                    uid = masterComponentId
                    componentTypeId = masterComponent.componentTypeId
                    componentName = masterComponent.componentName
                    componentDisplayName = masterComponent.componentDisplayName
                    taxType = masterComponent.taxType
                    jurisdiction = masterComponent.jurisdiction
                    jurisdictionLevel = masterComponent.jurisdictionLevel
                    ratePercentage = masterComponent.ratePercentage
                    isCompound = false
                    calculationMethod = "PERCENTAGE"
                    isActive = true
                }
                taxComponentRepository.save(workspaceComponent)
                componentIdMapping[masterComponentId] = masterComponentId
            } else {
                componentIdMapping[masterComponentId] = existing.uid
            }
        }

        // Step 3: Create workspace tax_rule with component_composition from master template
        val taxRule = TaxRule().apply {
            countryCode = masterRule.countryCode
            taxCodeId = taxCode.uid
            this.taxCode = masterRule.taxCode
            taxCodeType = masterRule.taxCodeType
            taxCodeDescription = masterCode.description
            jurisdiction = masterRule.jurisdiction
            jurisdictionLevel = masterRule.jurisdictionLevel
            // Convert Map<String, Any> to Map<String, ComponentComposition>
            this.componentComposition = convertToComponentComposition(masterRule.componentComposition)
            isActive = true
        }

        taxRuleRepository.save(taxRule)
    }

    /**
     * Fallback method: Creates tax_rule with generated composition (old behavior).
     */
    private fun createRuleWithGeneratedComposition(
        taxCode: TaxCode,
        masterCode: com.ampairs.tax.domain.model.MasterTaxCode,
        taxRate: Double
    ) {
        // Generate standard GST composition
        val componentComposition = gstRuleTemplateService.generateStandardGstComposition(taxRate)

        // Create workspace components for generated composition
        val componentIds = extractComponentIds(componentComposition)
        componentIds.forEach { componentId ->
            val existing = taxComponentRepository.findByUid(componentId)
            if (existing == null) {
                // Extract rate from component ID (e.g., "COMP_CGST_9" -> 9.0)
                val parts = componentId.split("_")
                val rate = parts.lastOrNull()?.toDoubleOrNull() ?: 0.0
                val componentType = parts.getOrNull(1) ?: "CGST"

                val workspaceComponent = com.ampairs.tax.domain.model.TaxComponent().apply {
                    uid = componentId
                    componentTypeId = "TYPE_$componentType"
                    componentName = componentType
                    componentDisplayName = "$componentType $rate%"
                    taxType = "GST"
                    jurisdiction = "INDIA"
                    jurisdictionLevel = if (componentType == "SGST") "STATE" else "COUNTRY"
                    ratePercentage = rate
                    isCompound = false
                    calculationMethod = "PERCENTAGE"
                    isActive = true
                }
                taxComponentRepository.save(workspaceComponent)
            }
        }

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

    /**
     * Extracts all unique component IDs from component_composition JSON.
     */
    private fun extractComponentIds(composition: Map<String, Any>): Set<String> {
        val componentIds = mutableSetOf<String>()

        composition.values.forEach { scenario ->
            if (scenario is Map<*, *>) {
                val components = scenario["components"]
                if (components is List<*>) {
                    components.forEach { component ->
                        if (component is Map<*, *>) {
                            val id = component["id"] as? String
                            if (id != null) {
                                componentIds.add(id)
                            }
                        }
                    }
                }
            }
        }

        return componentIds
    }

    /**
     * Converts Map<String, Any> from master rule to Map<String, ComponentComposition>.
     */
    private fun convertToComponentComposition(composition: Map<String, Any>): Map<String, ComponentComposition> {
        return composition.mapValues { (_, value) ->
            if (value is Map<*, *>) {
                val scenario = value["scenario"] as? String ?: ""
                val totalRate = (value["totalRate"] as? Number)?.toDouble() ?: 0.0
                val componentsData = value["components"] as? List<*> ?: emptyList<Any>()

                val components = componentsData.mapNotNull { comp ->
                    if (comp is Map<*, *>) {
                        ComponentReference(
                            id = comp["id"] as? String ?: "",
                            name = comp["name"] as? String ?: "",
                            rate = (comp["rate"] as? Number)?.toDouble() ?: 0.0,
                            order = (comp["order"] as? Number)?.toInt() ?: 0
                        )
                    } else {
                        null
                    }
                }

                ComponentComposition(
                    scenario = scenario,
                    components = components,
                    totalRate = totalRate
                )
            } else {
                ComponentComposition("", emptyList(), 0.0)
            }
        }
    }

    /**
     * Reactivates an existing inactive tax rule or creates a new one if it doesn't exist.
     */
    private fun reactivateOrCreateTaxRule(taxCode: TaxCode, masterCode: com.ampairs.tax.domain.model.MasterTaxCode) {
        // Check if tax rule exists for this tax code
        val existingRules = taxRuleRepository.findByTaxCodeId(taxCode.uid)

        if (existingRules.isNotEmpty()) {
            // Reactivate all inactive rules
            existingRules.forEach { rule ->
                if (!rule.isActive) {
                    rule.isActive = true
                    taxRuleRepository.save(rule)
                }
            }
        } else {
            // No rules exist, create default one
            createDefaultTaxRule(taxCode, masterCode)
        }
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
