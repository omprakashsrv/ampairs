package com.ampairs.pricing.service

import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.pricing.domain.dto.CouponApplyRequest
import com.ampairs.pricing.domain.dto.CouponRedeemRequest
import com.ampairs.pricing.domain.dto.CouponResponse
import com.ampairs.pricing.domain.enums.CouponRejectionReason
import com.ampairs.pricing.domain.enums.OfferConditionType
import com.ampairs.pricing.domain.enums.OfferStatus
import com.ampairs.pricing.domain.model.CouponRedemption
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.repository.CouponRedemptionRepository
import com.ampairs.pricing.repository.OfferRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CouponServiceImpl(
    private val offerRepository: OfferRepository,
    private val couponRedemptionRepository: CouponRedemptionRepository,
) : CouponService {

    @Transactional(readOnly = true)
    override fun validate(request: CouponApplyRequest): CouponResponse =
        when (val r = resolve(request.code, request.channel, request.customerId)) {
            is Resolved.Ok -> ok(r.offer)
            is Resolved.Rejected -> rejected(r.reason)
        }

    @Transactional
    override fun redeem(request: CouponRedeemRequest): CouponResponse {
        val offer = when (val r = resolve(request.code, request.channel, request.customerId)) {
            is Resolved.Ok -> r.offer
            is Resolved.Rejected -> return rejected(r.reason)
        }
        // Idempotent: this order already redeemed the coupon → success without a second row.
        if (couponRedemptionRepository.existsByCouponOfferUidAndOrderRef(offer.uid, request.orderRef)) {
            return ok(offer)
        }
        val redemption = CouponRedemption().apply {
            couponOfferUid = offer.uid
            couponCode = normalize(request.code)
            customerId = request.customerId
            orderRef = request.orderRef
            redeemedAt = Instant.now()
        }
        return try {
            couponRedemptionRepository.save(redemption)
            ok(offer)
        } catch (e: DataIntegrityViolationException) {
            // A concurrent redemption for the same order won the unique constraint — treat as success.
            ok(offer)
        }
    }

    // ── resolution ───────────────────────────────────────────────────────────────────────

    private sealed interface Resolved {
        data class Ok(val offer: Offer) : Resolved
        data class Rejected(val reason: CouponRejectionReason) : Resolved
    }

    private fun resolve(code: String, channel: SalesChannel, customerId: String?): Resolved {
        val normalized = normalize(code)
        val offer = offerRepository
            .findByChannelAndStatusAndActiveTrueAndCouponCodeIgnoreCase(channel, OfferStatus.ACTIVE, normalized)
            .firstOrNull { it.conditionType == OfferConditionType.COUPON }
            ?: return Resolved.Rejected(CouponRejectionReason.NOT_FOUND)

        val now = Instant.now()
        if (offer.startsAt != null && now.isBefore(offer.startsAt)) return Resolved.Rejected(CouponRejectionReason.OUT_OF_WINDOW)
        if (offer.endsAt != null && now.isAfter(offer.endsAt)) return Resolved.Rejected(CouponRejectionReason.OUT_OF_WINDOW)

        val globalLimit = offer.totalLimit ?: offer.couponLimit
        if (globalLimit != null && couponRedemptionRepository.countByCouponOfferUid(offer.uid) >= globalLimit) {
            return Resolved.Rejected(CouponRejectionReason.GLOBAL_LIMIT_REACHED)
        }
        val perCustomer = offer.perCustomerLimit
        if (perCustomer != null && customerId != null &&
            couponRedemptionRepository.countByCouponOfferUidAndCustomerId(offer.uid, customerId) >= perCustomer
        ) {
            return Resolved.Rejected(CouponRejectionReason.PER_CUSTOMER_LIMIT_REACHED)
        }
        return Resolved.Ok(offer)
    }

    private fun normalize(code: String): String = code.trim().uppercase()

    private fun ok(offer: Offer) = CouponResponse(
        valid = true,
        offerUid = offer.uid,
        offerName = offer.name,
        rewardType = offer.rewardType,
        rewardPercent = offer.rewardPercent,
        rewardFlatMinor = offer.rewardFlatMinor,
        rewardCapMinor = offer.rewardCapMinor,
    )

    private fun rejected(reason: CouponRejectionReason) = CouponResponse(valid = false, rejectionReason = reason)
}
