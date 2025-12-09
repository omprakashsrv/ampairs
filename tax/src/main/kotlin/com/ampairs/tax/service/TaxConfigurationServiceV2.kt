package com.ampairs.tax.service

import com.ampairs.core.exception.NotFoundException
import com.ampairs.tax.domain.dto.TaxConfigurationDto
import com.ampairs.tax.domain.dto.UpdateTaxConfigurationRequest
import com.ampairs.tax.domain.dto.asTaxConfigurationDto
import com.ampairs.tax.domain.model.TaxConfiguration
import com.ampairs.tax.repository.TaxConfigurationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class TaxConfigurationServiceV2(
    private val taxConfigurationRepository: TaxConfigurationRepository
) {

    @Transactional(readOnly = true)
    fun getConfiguration(): TaxConfigurationDto {
        // Multi-tenancy via @TenantId automatically filters by ownerId
        val config = taxConfigurationRepository.findAll().firstOrNull()
            ?: throw NotFoundException("Tax configuration not found for workspace")

        return config.asTaxConfigurationDto()
    }

    fun createConfiguration(request: UpdateTaxConfigurationRequest): TaxConfigurationDto {
        // Multi-tenancy via @TenantId automatically filters by ownerId
        val existingConfig = taxConfigurationRepository.findAll().firstOrNull()
        if (existingConfig != null) {
            throw IllegalStateException("Tax configuration already exists for this workspace")
        }

        // Require essential fields for creation
        requireNotNull(request.countryCode) { "Country code is required for creating tax configuration" }
        requireNotNull(request.taxStrategy) { "Tax strategy is required for creating tax configuration" }
        requireNotNull(request.defaultTaxCodeSystem) { "Default tax code system is required for creating tax configuration" }

        val config = TaxConfiguration().apply {
            countryCode = request.countryCode
            taxStrategy = request.taxStrategy
            defaultTaxCodeSystem = request.defaultTaxCodeSystem
            taxJurisdictions = request.taxJurisdictions ?: emptyList()
            industry = request.industry
            autoSubscribeNewCodes = request.autoSubscribeNewCodes ?: false
        }

        return taxConfigurationRepository.save(config).asTaxConfigurationDto()
    }

    fun updateConfiguration(request: UpdateTaxConfigurationRequest): TaxConfigurationDto {
        // Multi-tenancy via @TenantId automatically filters by ownerId
        val config = taxConfigurationRepository.findAll().firstOrNull()
            ?: throw NotFoundException("Tax configuration not found. Please create configuration first.")

        config.apply {
            request.countryCode?.let { countryCode = it }
            request.taxStrategy?.let { taxStrategy = it }
            request.defaultTaxCodeSystem?.let { defaultTaxCodeSystem = it }
            request.taxJurisdictions?.let { taxJurisdictions = it }
            request.industry?.let { industry = it }
            request.autoSubscribeNewCodes?.let { autoSubscribeNewCodes = it }
        }

        return taxConfigurationRepository.save(config).asTaxConfigurationDto()
    }
}
