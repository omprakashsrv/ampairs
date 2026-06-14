package com.ampairs.tax.domain.dto

import com.ampairs.tax.domain.model.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.Instant

// ==================== Tax Configuration DTOs ====================

data class TaxConfigurationDto(
    val id: String,
    val countryCode: String,
    val gstin: String?,
    val taxStrategy: String,
    val defaultTaxCodeSystem: String,
    val taxJurisdictions: List<String>,
    val industry: String?,
    val autoSubscribeNewCodes: Boolean,
    val syncedAt: Instant,
    val metadata: Map<String, String>
)

data class TaxConfigurationRequest(
    val countryCode: String,
    val gstin: String? = null,
    val taxStrategy: String,
    val defaultTaxCodeSystem: String,
    val taxJurisdictions: List<String>,
    val industry: String? = null,
    val autoSubscribeNewCodes: Boolean = true
)

fun TaxConfiguration.asDto(): TaxConfigurationDto {
    return TaxConfigurationDto(
        id = this.uid,
        countryCode = this.countryCode,
        gstin = this.gstin,
        taxStrategy = this.taxStrategy,
        defaultTaxCodeSystem = this.defaultTaxCodeSystem,
        taxJurisdictions = this.taxJurisdictions,
        industry = this.industry,
        autoSubscribeNewCodes = this.autoSubscribeNewCodes,
        syncedAt = this.syncedAt,
        metadata = this.metadata ?: emptyMap()
    )
}

// ==================== Master Tax Code DTOs ====================

data class MasterTaxCodeDto(
    val id: String,
    val countryCode: String,
    val codeType: String,
    val code: String,
    val description: String,
    val shortDescription: String,
    val chapter: String? = null,
    val heading: String? = null,
    val subHeading: String? = null,
    val category: String? = null,
    val defaultTaxRate: Double? = null,
    val defaultTaxSlabId: String? = null,
    val isActive: Boolean = true,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant,
    val updatedAt: Instant
)

fun MasterTaxCode.asDto(): MasterTaxCodeDto {
    return MasterTaxCodeDto(
        id = this.uid,
        countryCode = this.countryCode,
        codeType = this.codeType,
        code = this.code,
        description = this.description,
        shortDescription = this.shortDescription,
        chapter = this.chapter,
        heading = this.heading,
        subHeading = this.subHeading,
        category = this.category,
        defaultTaxRate = this.defaultTaxRate,
        defaultTaxSlabId = this.defaultTaxSlabId,
        isActive = this.isActive,
        metadata = this.metadata ?: emptyMap(),
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

fun List<MasterTaxCode>.asDtos(): List<MasterTaxCodeDto> = this.map { it.asDto() }

// ==================== Master Tax Component DTOs ====================

data class MasterTaxComponentDto(
    val id: String,
    val componentTypeId: String,
    val componentName: String,
    val componentDisplayName: String,
    val taxType: String,
    val jurisdiction: String,
    val jurisdictionLevel: String,
    val ratePercentage: Double,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

fun MasterTaxComponent.asDto(): MasterTaxComponentDto {
    return MasterTaxComponentDto(
        id = this.uid,
        componentTypeId = this.componentTypeId,
        componentName = this.componentName,
        componentDisplayName = this.componentDisplayName,
        taxType = this.taxType,
        jurisdiction = this.jurisdiction,
        jurisdictionLevel = this.jurisdictionLevel,
        ratePercentage = this.ratePercentage,
        isActive = this.isActive,
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

fun List<MasterTaxComponent>.asMasterComponentDtos(): List<MasterTaxComponentDto> = this.map { it.asDto() }

// ==================== Master Tax Rule DTOs ====================

data class MasterTaxRuleDto(
    val id: String,
    val countryCode: String,
    val masterTaxCodeId: String,
    val taxCode: String,
    val taxCodeType: String,
    val taxRate: Double,
    val jurisdiction: String,
    val jurisdictionLevel: String,
    val componentComposition: Map<String, ComponentCompositionDto>,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

fun MasterTaxRule.asDto(): MasterTaxRuleDto {
    return MasterTaxRuleDto(
        id = this.uid,
        countryCode = this.countryCode,
        masterTaxCodeId = this.masterTaxCodeId,
        taxCode = this.taxCode,
        taxCodeType = this.taxCodeType,
        taxRate = this.taxRate,
        jurisdiction = this.jurisdiction,
        jurisdictionLevel = this.jurisdictionLevel,
        componentComposition = this.componentComposition.toCompositionDtos(),
        isActive = this.isActive,
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

fun List<MasterTaxRule>.asMasterRuleDtos(): List<MasterTaxRuleDto> = this.map { it.asDto() }

// ==================== Tax Code DTOs ====================

data class TaxCodeDto(
    val id: String,
    val masterTaxCodeId: String,
    val code: String,
    val codeType: String,
    val description: String,
    val shortDescription: String,
    val customName: String?,
    val customTaxRuleId: String?,
    val usageCount: Int = 0,
    val lastUsedAt: Instant?,
    val isFavorite: Boolean,
    val notes: String?,
    val isActive: Boolean,
    val addedAt: Instant,
    val updatedAt: Instant,
    val syncStatus: String = "SYNCED"
)

data class SubscribeTaxCodeRequest(
    @field:NotBlank val masterTaxCodeId: String,
    val customTaxRuleId: String? = null,
    val isFavorite: Boolean = false,
    val notes: String? = null,
    val customName: String? = null
)

data class BulkSubscribeTaxCodesRequest(
    @field:NotEmpty @field:Size(max = 100) val masterTaxCodeIds: List<String>,
    val applyDefaultRules: Boolean = true
)

data class BulkSubscribeResultDto(
    val successCount: Int,
    val failureCount: Int,
    val subscribedCodes: List<TaxCodeDto>,
    val errors: List<BulkOperationErrorDto>
)

data class BulkOperationErrorDto(
    val masterTaxCodeId: String,
    val errorMessage: String
)

data class UpdateTaxCodeConfigRequest(
    val isFavorite: Boolean? = null,
    val notes: String? = null,
    val customTaxRuleId: String? = null
)

data class IncrementUsageRequest(
    val timestamp: Long
)

fun TaxCode.asDto(): TaxCodeDto {
    return TaxCodeDto(
        id = this.uid,
        masterTaxCodeId = this.masterTaxCodeId,
        code = this.code,
        codeType = this.codeType,
        description = this.description,
        shortDescription = this.shortDescription,
        customName = this.customName,
        customTaxRuleId = this.customTaxRuleId,
        usageCount = this.usageCount,
        lastUsedAt = this.lastUsedAt,
        isFavorite = this.isFavorite,
        notes = this.notes,
        isActive = this.isActive,
        addedAt = this.addedAt,
        updatedAt = this.updatedAt ?: Instant.now(),
        syncStatus = this.syncStatus
    )
}

fun List<TaxCode>.asTaxCodeDtos(): List<TaxCodeDto> = this.map { it.asDto() }

// ==================== Tax Rule DTOs ====================

data class TaxRuleDto(
    val id: String,
    val countryCode: String,
    val taxCodeId: String,
    val taxCode: String,
    val taxCodeType: String,
    val taxCodeDescription: String?,
    val jurisdiction: String,
    val jurisdictionLevel: String,
    val componentComposition: Map<String, ComponentCompositionDto>,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class ComponentCompositionDto(
    val scenario: String,
    val components: List<ComponentReferenceDto>,
    val totalRate: Double
)

data class ComponentReferenceDto(
    val id: String,
    val name: String,
    val rate: Double,
    val order: Int
)

fun ComponentComposition.asDto(): ComponentCompositionDto = ComponentCompositionDto(
    scenario = this.scenario,
    components = this.components.map { it.asDto() },
    totalRate = this.totalRate
)

fun ComponentReference.asDto(): ComponentReferenceDto = ComponentReferenceDto(
    id = this.id,
    name = this.name,
    rate = this.rate,
    order = this.order
)

fun ComponentCompositionDto.toEntity(): ComponentComposition = ComponentComposition(
    scenario = this.scenario,
    components = this.components.map { it.toEntity() },
    totalRate = this.totalRate
)

fun ComponentReferenceDto.toEntity(): ComponentReference = ComponentReference(
    id = this.id,
    name = this.name,
    rate = this.rate,
    order = this.order
)

fun TaxRule.asDto(): TaxRuleDto {
    return TaxRuleDto(
        id = this.uid,
        countryCode = this.countryCode,
        taxCodeId = this.taxCodeId,
        taxCode = this.taxCode,
        taxCodeType = this.taxCodeType,
        taxCodeDescription = this.taxCodeDescription,
        jurisdiction = this.jurisdiction,
        jurisdictionLevel = this.jurisdictionLevel,
        componentComposition = this.componentComposition.mapValues { it.value.asDto() },
        isActive = this.isActive,
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

fun List<TaxRule>.asTaxRuleDtos(): List<TaxRuleDto> = this.map { it.asDto() }

// ==================== Tax Component DTOs ====================

data class TaxComponentDto(
    val id: String,
    val componentTypeId: String,
    val componentName: String,
    val componentDisplayName: String,
    val taxType: String,
    val jurisdiction: String,
    val jurisdictionLevel: String,
    val ratePercentage: Double,
    val isCompound: Boolean,
    val calculationMethod: String,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

fun TaxComponent.asDto(): TaxComponentDto {
    return TaxComponentDto(
        id = this.uid,
        componentTypeId = this.componentTypeId,
        componentName = this.componentName,
        componentDisplayName = this.componentDisplayName,
        taxType = this.taxType,
        jurisdiction = this.jurisdiction,
        jurisdictionLevel = this.jurisdictionLevel,
        ratePercentage = this.ratePercentage,
        isCompound = this.isCompound,
        calculationMethod = this.calculationMethod,
        isActive = this.isActive,
        createdAt = this.createdAt ?: Instant.now(),
        updatedAt = this.updatedAt ?: Instant.now()
    )
}

fun List<TaxComponent>.asTaxComponentDtos(): List<TaxComponentDto> = this.map { it.asDto() }

// ==================== Tax Calculation DTOs ====================

data class TaxCalculationRequest(
    val taxCode: String,
    val taxCodeType: String,
    val baseAmount: Double,
    val quantity: Int,
    val sourceLocation: JurisdictionDto,
    val destinationLocation: JurisdictionDto,
    val transactionType: String,
    val transactionContext: TransactionContextDto
)

data class JurisdictionDto(
    val country: String,
    val state: String? = null,
    val county: String? = null,
    val city: String? = null,
    val postalCode: String? = null
)

data class TransactionContextDto(
    val businessType: String = "REGULAR",
    val isReverseCharge: Boolean = false,
    val exemptionReason: String? = null,
    val specialConditions: Map<String, String> = emptyMap()
)

data class TaxCalculationResultDto(
    val taxCode: String,
    val codeType: String,
    val baseAmount: Double,
    val quantity: Int,
    val taxComponents: List<TaxComponentResultDto>,
    val totalTaxAmount: Double,
    val totalAmount: Double,
    val jurisdiction: JurisdictionDto,
    val countryCode: String,
    val metadata: Map<String, String>
)

data class TaxComponentResultDto(
    val componentId: String,
    val componentName: String,
    val taxType: String,
    val ratePercentage: Double,
    val taxableAmount: Double,
    val taxAmount: Double,
    val description: String,
    val isCompound: Boolean
)

// ==================== Update Request DTOs ====================

data class UpdateTaxConfigurationRequest(
    val countryCode: String? = null,
    val gstin: String? = null,
    val taxStrategy: String? = null,
    val defaultTaxCodeSystem: String? = null,
    val taxJurisdictions: List<String>? = null,
    val industry: String? = null,
    val autoSubscribeNewCodes: Boolean? = null
)

data class UpdateTaxCodeRequest(
    val isFavorite: Boolean? = null,
    val notes: String? = null,
    val customName: String? = null
)

data class UpdateTaxRuleRequest(
    val jurisdiction: String? = null,
    val jurisdictionLevel: String? = null,
    val componentComposition: Map<String, ComponentCompositionDto>? = null,
    val isActive: Boolean? = null
)

// ==================== Tax Component Type DTOs ====================

data class TaxComponentTypeDto(
    val id: String,
    val name: String,
    val displayName: String,
    val countryCode: String,
    val taxType: String,
    val isCompound: Boolean,
    val calculationMethod: String,
    val description: String?
)

// ==================== Helpers ====================

fun TaxConfiguration.asTaxConfigurationDto(): TaxConfigurationDto = this.asDto()

private fun Map<String, Any>.toCompositionDtos(): Map<String, ComponentCompositionDto> =
    mapValues { (_, value) ->
        val v = value as? Map<*, *> ?: return@mapValues ComponentCompositionDto("", emptyList(), 0.0)
        ComponentCompositionDto(
            scenario = v["scenario"] as? String ?: "",
            totalRate = (v["totalRate"] as? Number)?.toDouble() ?: 0.0,
            components = (v["components"] as? List<*>)?.mapNotNull { comp ->
                (comp as? Map<*, *>)?.let {
                    ComponentReferenceDto(
                        id = it["id"] as? String ?: "",
                        name = it["name"] as? String ?: "",
                        rate = (it["rate"] as? Number)?.toDouble() ?: 0.0,
                        order = (it["order"] as? Number)?.toInt() ?: 0
                    )
                }
            } ?: emptyList()
        )
    }

// ==================== Workspace Tax Component DTOs ====================

/**
 * Workspace tax component, shaped for the mobile `WorkspaceTaxComponent` model.
 *
 * `jurisdictionLevel` is normalised to the client enum (FEDERAL/STATE/COUNTY/CITY/SPECIAL);
 * the workspace entity stores "COUNTRY" for country-level components, which maps to FEDERAL.
 * `effectiveFrom` has no entity column, so it mirrors `createdAt`.
 */
data class WorkspaceTaxComponentDto(
    val id: String,
    val componentTypeId: String,
    val jurisdiction: String,
    val jurisdictionLevel: String,
    val ratePercentage: Double,
    val isCompoundTax: Boolean,
    val effectiveFrom: Instant,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant
)

private fun normaliseJurisdictionLevel(level: String): String = when (level.uppercase()) {
    "STATE" -> "STATE"
    "COUNTY" -> "COUNTY"
    "CITY" -> "CITY"
    "SPECIAL" -> "SPECIAL"
    else -> "FEDERAL" // COUNTRY and any unknown/empty value map to the client's top-level enum
}

fun TaxComponent.asWorkspaceComponentDto(): WorkspaceTaxComponentDto {
    val created = this.createdAt ?: Instant.now()
    return WorkspaceTaxComponentDto(
        id = this.uid,
        componentTypeId = this.componentTypeId,
        jurisdiction = this.jurisdiction,
        jurisdictionLevel = normaliseJurisdictionLevel(this.jurisdictionLevel),
        ratePercentage = this.ratePercentage,
        isCompoundTax = this.isCompound,
        effectiveFrom = created,
        isActive = this.isActive,
        createdAt = created,
        updatedAt = this.updatedAt ?: Instant.now()
    )
}
