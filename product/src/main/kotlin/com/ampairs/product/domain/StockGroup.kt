package com.ampairs.product.domain

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.product.config.Constants
import jakarta.persistence.Entity

@Entity
class StockGroup : BaseDomain() {
    override fun obtainIdPrefix(): String {
        return Constants.STOCK_GROUP_PREFIX
    }
}