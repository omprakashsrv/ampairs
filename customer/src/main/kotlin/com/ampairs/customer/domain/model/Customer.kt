package com.ampairs.customer.domain.model

import com.ampairs.core.domain.model.BaseDomain
import com.ampairs.customer.config.Constants
import jakarta.persistence.Entity

@Entity()
class Customer : BaseDomain() {
    override fun obtainIdPrefix(): String {
        return Constants.CUSTOMER_PREFIX
    }
}