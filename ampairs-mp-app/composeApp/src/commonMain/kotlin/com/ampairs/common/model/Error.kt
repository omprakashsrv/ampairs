package com.ampairs.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Error(
    @SerialName("code")
    val code: Int = 0,
    @SerialName("message")
    val message: String = "",
)