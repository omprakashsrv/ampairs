package com.ampairs.product.service

import com.ampairs.product.repository.ProductRepository
import com.ampairs.unit.service.UnitUsageProvider
import com.ampairs.unit.service.UnitUsageSnapshot
import org.springframework.stereotype.Component

@Component
class UnitProductUsageProvider(
    private val productRepository: ProductRepository
) : UnitUsageProvider {

    override fun findUsage(unitUid: String): UnitUsageSnapshot {
        val products = productRepository.findAllByUnitId(unitUid)

        if (products.isEmpty()) {
            return UnitUsageSnapshot(unitUid = unitUid)
        }

        return UnitUsageSnapshot(
            unitUid = unitUid,
            productIds = products.map { it.uid }
        )
    }
}
