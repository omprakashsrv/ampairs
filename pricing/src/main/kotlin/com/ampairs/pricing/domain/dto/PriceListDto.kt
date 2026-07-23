package com.ampairs.pricing.domain.dto

import com.ampairs.core.domain.enums.SalesChannel
import com.ampairs.pricing.config.Constants
import com.ampairs.pricing.domain.enums.PriceListStatus
import com.ampairs.pricing.domain.model.AttributePredicate
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.util.PricingJson
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

data class PriceListRequest(
    val uid: String? = null,

    @field:NotBlank(message = "Price list name is required")
    @field:Size(max = 200)
    val name: String,

    val channel: SalesChannel = SalesChannel.RETAIL,

    val customerGroupId: String? = null,
    val customerType: String? = null,
    val customerId: String? = null,
    val brandId: String? = null,
    val categoryId: String? = null,
    val productGroupId: String? = null,
    val geoZoneId: String? = null,

    val attributePredicates: List<AttributePredicate> = emptyList(),

    @field:Size(min = 3, max = 3)
    val currency: String = Constants.DEFAULT_CURRENCY,

    val priority: Int = 0,
    val status: PriceListStatus = PriceListStatus.DRAFT,
    val startsAt: Instant? = null,
    val endsAt: Instant? = null,
    val active: Boolean = true,
    val refId: String? = null,
)

data class PriceListResponse(
    val uid: String,
    val refId: String?,
    val name: String,
    val channel: SalesChannel,
    val customerGroupId: String?,
    val customerType: String?,
    val customerId: String?,
    val brandId: String?,
    val categoryId: String?,
    val productGroupId: String?,
    val geoZoneId: String?,
    val attributePredicates: List<AttributePredicate>,
    val currency: String,
    val priority: Int,
    val status: PriceListStatus,
    val startsAt: Instant?,
    val endsAt: Instant?,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun PriceList.applyRequest(request: PriceListRequest): PriceList = apply {
    request.uid?.takeIf { it.isNotBlank() }?.let { uid = it }
    name = request.name.trim()
    channel = request.channel
    customerGroupId = request.customerGroupId
    customerType = request.customerType
    customerId = request.customerId
    brandId = request.brandId
    categoryId = request.categoryId
    productGroupId = request.productGroupId
    geoZoneId = request.geoZoneId
    attributePredicatesJson = request.attributePredicates.takeIf { it.isNotEmpty() }?.let { PricingJson.write(it) }
    currency = request.currency.uppercase()
    priority = request.priority
    status = request.status
    startsAt = request.startsAt
    endsAt = request.endsAt
    active = request.active
    request.refId?.takeIf { it.isNotBlank() }?.let { refId = it }
}

fun PriceList.asResponse(): PriceListResponse = PriceListResponse(
    uid = uid,
    refId = refId,
    name = name,
    channel = channel,
    customerGroupId = customerGroupId,
    customerType = customerType,
    customerId = customerId,
    brandId = brandId,
    categoryId = categoryId,
    productGroupId = productGroupId,
    geoZoneId = geoZoneId,
    attributePredicates = PricingJson.read(attributePredicatesJson, emptyList()),
    currency = currency,
    priority = priority,
    status = status,
    startsAt = startsAt,
    endsAt = endsAt,
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<PriceList>.asResponses(): List<PriceListResponse> = map { it.asResponse() }
