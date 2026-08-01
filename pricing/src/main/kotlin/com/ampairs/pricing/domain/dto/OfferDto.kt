package com.ampairs.pricing.domain.dto

import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.pricing.domain.enums.OfferConditionType
import com.ampairs.pricing.domain.enums.OfferRewardType
import com.ampairs.pricing.domain.enums.OfferStatus
import com.ampairs.pricing.domain.model.Offer
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

/**
 * Request shape for an offer upsert. Mirrors the mobile/web `Offer` model field-for-field; any extra
 * fields the client sends (e.g. `created_at`/`updated_at`) are ignored on deserialization.
 */
data class OfferRequest(
    val uid: String? = null,

    @field:NotBlank(message = "Offer name is required")
    @field:Size(max = 200)
    val name: String,

    val channel: SalesChannel = SalesChannel.RETAIL,
    val status: OfferStatus = OfferStatus.DRAFT,
    val priority: Int = 0,
    val startsAt: Instant? = null,
    val endsAt: Instant? = null,

    val customerGroupId: String? = null,
    val customerType: String? = null,
    val brandId: String? = null,
    val categoryId: String? = null,
    val geoZoneId: String? = null,

    val conditionType: OfferConditionType = OfferConditionType.NONE,
    val cartMinMinor: Long? = null,
    val quantityMin: Double? = null,
    val couponCode: String? = null,
    val couponLimit: Int? = null,

    val rewardType: OfferRewardType = OfferRewardType.PERCENT,
    val rewardPercent: Double? = null,
    val rewardFlatMinor: Long? = null,
    val rewardCapMinor: Long? = null,
    val bogoBuyQty: Int? = null,
    val bogoGetQty: Int? = null,

    val stackable: Boolean = false,
    val exclusive: Boolean = false,
    val perCustomerLimit: Int? = null,
    val totalLimit: Int? = null,
    val usedCount: Int = 0,
    val active: Boolean = true,
)

data class OfferResponse(
    val uid: String,
    val name: String,
    val channel: SalesChannel,
    val status: OfferStatus,
    val priority: Int,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val customerGroupId: String?,
    val customerType: String?,
    val brandId: String?,
    val categoryId: String?,
    val geoZoneId: String?,
    val conditionType: OfferConditionType,
    val cartMinMinor: Long?,
    val quantityMin: Double?,
    val couponCode: String?,
    val couponLimit: Int?,
    val rewardType: OfferRewardType,
    val rewardPercent: Double?,
    val rewardFlatMinor: Long?,
    val rewardCapMinor: Long?,
    val bogoBuyQty: Int?,
    val bogoGetQty: Int?,
    val stackable: Boolean,
    val exclusive: Boolean,
    val perCustomerLimit: Int?,
    val totalLimit: Int?,
    val usedCount: Int,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun Offer.applyRequest(request: OfferRequest): Offer = apply {
    request.uid?.takeIf { it.isNotBlank() }?.let { uid = it }
    name = request.name.trim()
    channel = request.channel
    status = request.status
    priority = request.priority
    startsAt = request.startsAt
    endsAt = request.endsAt
    customerGroupId = request.customerGroupId
    customerType = request.customerType
    brandId = request.brandId
    categoryId = request.categoryId
    geoZoneId = request.geoZoneId
    conditionType = request.conditionType
    cartMinMinor = request.cartMinMinor
    quantityMin = request.quantityMin
    couponCode = request.couponCode
    couponLimit = request.couponLimit
    rewardType = request.rewardType
    rewardPercent = request.rewardPercent
    rewardFlatMinor = request.rewardFlatMinor
    rewardCapMinor = request.rewardCapMinor
    bogoBuyQty = request.bogoBuyQty
    bogoGetQty = request.bogoGetQty
    stackable = request.stackable
    exclusive = request.exclusive
    perCustomerLimit = request.perCustomerLimit
    totalLimit = request.totalLimit
    usedCount = request.usedCount
    active = request.active
}

fun Offer.asResponse(): OfferResponse = OfferResponse(
    uid = uid,
    name = name,
    channel = channel,
    status = status,
    priority = priority,
    startsAt = startsAt,
    endsAt = endsAt,
    customerGroupId = customerGroupId,
    customerType = customerType,
    brandId = brandId,
    categoryId = categoryId,
    geoZoneId = geoZoneId,
    conditionType = conditionType,
    cartMinMinor = cartMinMinor,
    quantityMin = quantityMin,
    couponCode = couponCode,
    couponLimit = couponLimit,
    rewardType = rewardType,
    rewardPercent = rewardPercent,
    rewardFlatMinor = rewardFlatMinor,
    rewardCapMinor = rewardCapMinor,
    bogoBuyQty = bogoBuyQty,
    bogoGetQty = bogoGetQty,
    stackable = stackable,
    exclusive = exclusive,
    perCustomerLimit = perCustomerLimit,
    totalLimit = totalLimit,
    usedCount = usedCount,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<Offer>.asResponses(): List<OfferResponse> = map { it.asResponse() }
