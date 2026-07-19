package com.ampairs.ecom.domain.dto

import jakarta.validation.constraints.NotBlank

/** Body for `POST /v1/ecom/account/link` — commits a candidate from `GET .../link-candidate`. */
data class ConfirmLinkRequest(
    @field:NotBlank
    val customerId: String,
)
