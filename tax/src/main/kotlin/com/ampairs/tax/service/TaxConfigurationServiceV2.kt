package com.ampairs.tax.service

import com.ampairs.core.exception.NotFoundException
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.sync.EntityChangePublisher
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
    private val taxConfigurationRepository: TaxConfigurationRepository,
    private val entityChangePublisher: EntityChangePublisher,
) {

    @Transactional(readOnly = true)
    fun getConfiguration(): TaxConfigurationDto {
        val ownerId = TenantContextHolder.requireCurrentTenant()
        return taxConfigurationRepository.findByOwnerId(ownerId)?.asTaxConfigurationDto()
            ?: throw NotFoundException("Tax configuration not found for workspace")
    }

    fun createConfiguration(request: UpdateTaxConfigurationRequest): TaxConfigurationDto {
        val ownerId = TenantContextHolder.requireCurrentTenant()
        if (taxConfigurationRepository.findByOwnerId(ownerId) != null) {
            throw IllegalStateException("Tax configuration already exists for this workspace")
        }

        requireNotNull(request.countryCode) { "Country code is required" }
        requireNotNull(request.taxStrategy) { "Tax strategy is required" }
        requireNotNull(request.defaultTaxCodeSystem) { "Default tax code system is required" }

        val config = TaxConfiguration().apply {
            countryCode = request.countryCode
            gstin = request.gstin
            taxStrategy = request.taxStrategy
            defaultTaxCodeSystem = request.defaultTaxCodeSystem
            taxJurisdictions = request.taxJurisdictions ?: emptyList()
            industry = request.industry
            autoSubscribeNewCodes = request.autoSubscribeNewCodes ?: false
        }

        return taxConfigurationRepository.save(config)
            .also { entityChangePublisher.updated("tax", it.uid) }
            .asTaxConfigurationDto()
    }

    fun updateConfiguration(request: UpdateTaxConfigurationRequest): TaxConfigurationDto {
        val ownerId = TenantContextHolder.requireCurrentTenant()
        val config = taxConfigurationRepository.findByOwnerId(ownerId)
            ?: throw NotFoundException("Tax configuration not found. Please create configuration first.")

        config.apply {
            request.countryCode?.let { countryCode = it }
            request.gstin?.let { gstin = it }
            request.taxStrategy?.let { taxStrategy = it }
            request.defaultTaxCodeSystem?.let { defaultTaxCodeSystem = it }
            request.taxJurisdictions?.let { taxJurisdictions = it }
            request.industry?.let { industry = it }
            request.autoSubscribeNewCodes?.let { autoSubscribeNewCodes = it }
        }

        return taxConfigurationRepository.save(config)
            .also { entityChangePublisher.updated("tax", it.uid) }
            .asTaxConfigurationDto()
    }
}
