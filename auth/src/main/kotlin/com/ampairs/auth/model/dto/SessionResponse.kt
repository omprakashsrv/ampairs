package com.ampairs.auth.model.dto


data class SessionResponse(val id: String, val countryCode: Int, val phone: String, val valid: Boolean)
