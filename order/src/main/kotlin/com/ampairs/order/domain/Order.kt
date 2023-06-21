package com.ampairs.order.domain

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.order.config.Constants
import jakarta.persistence.Entity

@Entity
class Order : BaseDomain() {
    override fun obtainIdPrefix(): String {
        return Constants.ORDER_PREFIX
    }
}