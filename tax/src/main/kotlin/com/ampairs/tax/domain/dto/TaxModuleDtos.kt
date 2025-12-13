package com.ampairs.tax.domain.dto

import com.ampairs.tax.domain.model.*
import java.time.Instant

// ==================== Tax Configuration DTOs ====================

data class TaxConfigurationDto(
    val id: String,
    val countryCode: String,
    val taxStrategy: String,
    val defaultTaxCodeSystem: String,
    val taxJurisdictions: List<String>,
    val industry: String?,
    val autoSubscribeNewCodes: Boolean,
    val syncedAt: Long,
    val metadata: Map<String, String>
)

data class TaxConfigurationRequest(
    val countryCode: String,
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
        taxStrategy = this.taxStrategy,
        defaultTaxCodeSystem = this.defaultTaxCodeSystem,
        taxJurisdictions = this.taxJurisdictions,
        industry = this.industry,
        autoSubscribeNewCodes = this.autoSubscribeNewCodes,
        syncedAt = this.syncedAt.toEpochMilli(),
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
    val createdAt: Long,
    val updatedAt: Long
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
        createdAt = this.createdAt?.toEpochMilli() ?: Instant.now().toEpochMilli(),
        updatedAt = this.updatedAt?.toEpochMilli() ?: Instant.now().toEpochMilli()
    )
}

fun List<MasterTaxCode>.asDtos(): List<MasterTaxCodeDto> = this.map { it.asDto() }

// ==================== Workspace Tax Code DTOs ====================

data class WorkspaceTaxCodeDto(
    val id: String,
    val masterTaxCodeId: String,
    val code: String,
    val codeType: String,
    val description: String,
    val shortDescription: String,
    val customName: String?,
    val customTaxRuleId: String?,
    val usageCount: Int = 0,
    val lastUsedAt: Long?,
    val isFavorite: Boolean,
    val notes: String?,
    val isActive: Boolean,
    val addedAt: Long,
    val updatedAt: Long,
    val syncStatus: String = "SYNCED"
)

data class SubscribeTaxCodeRequest(
    val masterTaxCodeId: String,
    val customTaxRuleId: String? = null,
    val isFavorite: Boolean = false,
    val notes: String? = null,
    val customName: String? = null
)

data class BulkSubscribeTaxCodesRequest(
    val masterTaxCodeIds: List<String>,
    val applyDefaultRules: Boolean = true
)

data class BulkSubscribeResultDto(
    val successCount: Int,
    val failureCount: Int,
    val subscribedCodes: List<WorkspaceTaxCodeDto>,
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

fun TaxCode.asDto(): WorkspaceTaxCodeDto {
    return WorkspaceTaxCodeDto(
        id = this.uid,
        masterTaxCodeId = this.masterTaxCodeId,
        code = this.code,
        codeType = this.codeType,
        description = this.description,
        shortDescription = this.shortDescription,
        customName = this.customName,
        customTaxRuleId = this.customTaxRuleId,
        usageCount = this.usageCount,
        lastUsedAt = this.lastUsedAt?.toEpochMilli(),
        isFavorite = this.isFavorite,
        notes = this.notes,
        isActive = this.isActive,
        addedAt = this.addedAt.toEpochMilli(),
        updatedAt = this.updatedAt?.toEpochMilli() ?: Instant.now().toEpochMilli(),
        syncStatus = this.syncStatus
    )
}

fun List<TaxCode>.asWorkspaceTaxCodeDtos(): List<WorkspaceTaxCodeDto> = this.map { it.asDto() }

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
    val createdAt: Long,
    val updatedAt: Long
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

fun ComponentComposition.asDto(): ComponentCompositionDto {
    return ComponentCompositionDto(
        scenario = this.scenario,
        components = this.components.map { it.asDto() },
        totalRate = this.totalRate
    )
}

fun ComponentReference.asDto(): ComponentReferenceDto {
    return ComponentReferenceDto(
        id = this.id,
        name = this.name,
        rate = this.rate,
        order = this.order
    )
}

// Extension functions to convert from DTO to entity
fun ComponentCompositionDto.toEntity(): ComponentComposition {
    return ComponentComposition(
        scenario = this.scenario,
        components = this.components.map { it.toEntity() },
        totalRate = this.totalRate
    )
}

fun ComponentReferenceDto.toEntity(): ComponentReference {
    return ComponentReference(
        id = this.id,
        name = this.name,
        rate = this.rate,
        order = this.order
    )
}

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
        createdAt = this.createdAt?.toEpochMilli() ?: Instant.now().toEpochMilli(),
        updatedAt = this.updatedAt?.toEpochMilli() ?: Instant.now().toEpochMilli()
    )
}

fun List<TaxRule>.asTaxRuleDtos(): List<TaxRuleDto> = this.map { it.asDto() }

// ==================== Tax Component DTOs ====================

data class WorkspaceTaxComponentDto(
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
    val createdAt: Long,
    val updatedAt: Long
)

fun TaxComponent.asDto(): WorkspaceTaxComponentDto {
    return WorkspaceTaxComponentDto(
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
        createdAt = this.createdAt?.toEpochMilli() ?: Instant.now().toEpochMilli(),
        updatedAt = this.updatedAt?.toEpochMilli() ?: Instant.now().toEpochMilli()
    )
}

fun List<TaxComponent>.asComponentDtos(): List<WorkspaceTaxComponentDto> = this.map { it.asDto() }

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

// ==================== Additional DTOs for V2 Controllers ====================

// Type alias for backward compatibility with guide naming
typealias TaxCodeDto = WorkspaceTaxCodeDto
typealias TaxComponentDto = WorkspaceTaxComponentDto

// Extension functions for TaxCode
fun TaxConfiguration.asTaxConfigurationDto(): TaxConfigurationDto = this.asDto()

fun List<TaxCode>.asTaxCodeDtos(): List<TaxCodeDto> = this.asWorkspaceTaxCodeDtos()

fun List<TaxComponent>.asTaxComponentDtos(): List<TaxComponentDto> = this.asComponentDtos()

// Update/Patch request DTOs
data class UpdateTaxConfigurationRequest(
    val countryCode: String? = null,
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

// ==================== Tax Rule Update DTOs ====================

data class UpdateTaxRuleRequest(
    val jurisdiction: String? = null,
    val jurisdictionLevel: String? = null,
    val componentComposition: Map<String, ComponentCompositionDto>? = null,
    val isActive: Boolean? = null
)

// ==================== Tax Component Type DTOs ====================

data class TaxComponentTypeDto(
    val id: String,
    val name: String,              // "CGST", "SGST", "IGST"
    val displayName: String,       // "Central GST", "State GST"
    val countryCode: String,       // "IN"
    val taxType: String,           // "GST"
    val isCompound: Boolean,
    val calculationMethod: String,
    val description: String?
)

