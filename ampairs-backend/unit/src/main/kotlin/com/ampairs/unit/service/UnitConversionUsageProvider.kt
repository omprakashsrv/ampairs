package com.ampairs.unit.service

import com.ampairs.unit.repository.UnitConversionRepository
import org.springframework.stereotype.Component

@Component
class UnitConversionUsageProvider(
    private val unitConversionRepository: UnitConversionRepository
) : UnitUsageProvider {

    override fun findUsage(unitUid: String): UnitUsageSnapshot {
        val conversions = unitConversionRepository.findAllLinkedToUnit(unitUid)
            .filter { it.active }

        if (conversions.isEmpty()) {
            return UnitUsageSnapshot(unitUid = unitUid)
        }

        val conversionIds = conversions.map { it.uid }
        return UnitUsageSnapshot(
            unitUid = unitUid,
            conversionIds = conversionIds
        )
    }
}
