package com.ampairs.pricing.repository

import com.ampairs.pricing.domain.model.CouponRedemption
import org.springframework.data.repository.CrudRepository

interface CouponRedemptionRepository : CrudRepository<CouponRedemption, Long> {

    fun countByCouponOfferUid(couponOfferUid: String): Long

    fun countByCouponOfferUidAndCustomerId(couponOfferUid: String, customerId: String): Long

    fun existsByCouponOfferUidAndOrderRef(couponOfferUid: String, orderRef: String): Boolean
}
