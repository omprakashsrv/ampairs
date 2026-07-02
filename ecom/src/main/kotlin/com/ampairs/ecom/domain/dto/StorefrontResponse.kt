package com.ampairs.ecom.domain.dto

import com.ampairs.ecom.domain.enums.StorefrontAccessMode
import com.ampairs.ecom.domain.enums.StorefrontStatus
import com.ampairs.ecom.domain.model.Storefront
import java.time.Instant

data class StorefrontResponse(
    val uid: String,
    val name: String,
    val slug: String,
    val description: String?,
    val logoUrl: String?,
    val bannerUrl: String?,
    val status: StorefrontStatus,
    val accessMode: StorefrontAccessMode,
    val defaultChannel: com.ampairs.core.domain.enums.SalesChannel,
    val publishedAt: Instant?,
    val unpublishedAt: Instant?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

fun Storefront.asStorefrontResponse() = StorefrontResponse(
    uid = uid,
    name = name,
    slug = slug,
    description = description,
    logoUrl = logoUrl,
    bannerUrl = bannerUrl,
    status = status,
    accessMode = accessMode,
    defaultChannel = defaultChannel,
    publishedAt = publishedAt,
    unpublishedAt = unpublishedAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun StorefrontRequest.toStorefront(ownerId: String): Storefront {
    val storefront = Storefront()
    storefront.ownerId = ownerId
    storefront.name = name
    storefront.slug = slug
    storefront.description = description
    storefront.logoUrl = logoUrl
    storefront.bannerUrl = bannerUrl
    return storefront
}
