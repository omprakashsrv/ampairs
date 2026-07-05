package com.ampairs.ecom.domain.dto

import com.ampairs.ecom.domain.enums.StorefrontAccessMode
import com.ampairs.ecom.domain.model.Storefront

data class StorefrontUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val logoUrl: String? = null,
    val bannerUrl: String? = null,
    val accessMode: StorefrontAccessMode? = null,
    val brandColorArgb: Long? = null,
)

fun StorefrontUpdateRequest.applyTo(storefront: Storefront): Storefront {
    name?.let { storefront.name = it }
    description?.let { storefront.description = it }
    logoUrl?.let { storefront.logoUrl = it }
    bannerUrl?.let { storefront.bannerUrl = it }
    accessMode?.let { storefront.accessMode = it }
    brandColorArgb?.let { storefront.brandColorArgb = it }
    return storefront
}
