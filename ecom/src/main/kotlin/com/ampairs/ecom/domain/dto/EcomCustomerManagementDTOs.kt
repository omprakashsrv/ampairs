package com.ampairs.ecom.domain.dto

import com.ampairs.core.service.EcomContactSummary

/**
 * A buyer↔CRM-customer link, for the owner's workspace-wide "ecom users" management screen —
 * `GET/PATCH /v1/ecom/management/customers`. See `CustomerContactResponse` in the `customer` module
 * for the per-customer-detail-page equivalent (same underlying data, different scope).
 */
data class EcomContactResponse(
    val contactUid: String,
    val customerId: String,
    val customerName: String,
    val name: String,
    val phone: String?,
    val role: String,
    val isDefault: Boolean,
    val active: Boolean,
)

/** Body for `PATCH /v1/ecom/management/customers/{contactUid}/status`. */
data class SetEcomContactStatusRequest(
    val active: Boolean,
)

fun EcomContactSummary.asEcomContactResponse() = EcomContactResponse(
    contactUid = contactUid,
    customerId = customerId,
    customerName = customerName,
    name = name,
    phone = phone,
    role = role,
    isDefault = isDefault,
    active = active,
)
