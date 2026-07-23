package com.ampairs.customer.domain.dto

import com.ampairs.core.service.EcomContactSummary
import jakarta.validation.constraints.NotBlank

/** A buyer↔CRM-customer link, for the owner's "linked accounts" view on a customer's detail page. */
data class CustomerContactResponse(
    val contactUid: String,
    val customerId: String,
    val customerName: String,
    val name: String,
    val phone: String?,
    val role: String,
    val isDefault: Boolean,
    val active: Boolean,
)

/** Body for `POST /customer/v1/{customerId}/contacts` — manually links the app account with this phone. */
data class LinkContactRequest(
    @field:NotBlank
    val phone: String,
    val name: String? = null,
    val role: String = "OWNER",
    val isDefault: Boolean = false,
)

/** Body for `PATCH /customer/v1/{customerId}/contacts/{contactUid}/status`. */
data class SetContactStatusRequest(
    val active: Boolean,
)

fun EcomContactSummary.asContactResponse() = CustomerContactResponse(
    contactUid = contactUid,
    customerId = customerId,
    customerName = customerName,
    name = name,
    phone = phone,
    role = role,
    isDefault = isDefault,
    active = active,
)
