package com.ampairs.tax.sync

import com.ampairs.core.sync.SyncCheckpointContributor
import com.ampairs.tax.repository.TaxCodeRepository
import com.ampairs.tax.repository.TaxComponentRepository
import com.ampairs.tax.repository.TaxConfigurationRepository
import com.ampairs.tax.repository.TaxRuleRepository
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Contributes the tax module's sync checkpoint for the current workspace. The mobile `tax`
 * SyncEntity aggregates all tenant-scoped tax tables, so the checkpoint is the latest `updatedAt`
 * across TaxCode, TaxConfiguration, TaxComponent and TaxRule. (Master* tables are global, excluded.)
 * Queries are `@TenantId`-filtered, so they are automatically workspace-scoped.
 */
@Component
class TaxCheckpointContributor(
    private val taxCodeRepository: TaxCodeRepository,
    private val taxConfigurationRepository: TaxConfigurationRepository,
    private val taxComponentRepository: TaxComponentRepository,
    private val taxRuleRepository: TaxRuleRepository,
) : SyncCheckpointContributor {

    override fun checkpoints(): Map<String, Instant?> {
        val maxTax = listOfNotNull(
            taxCodeRepository.findMaxUpdatedAt(),
            taxConfigurationRepository.findMaxUpdatedAt(),
            taxComponentRepository.findMaxUpdatedAt(),
            taxRuleRepository.findMaxUpdatedAt(),
        ).maxOrNull()
        return mapOf("tax" to maxTax)
    }
}
