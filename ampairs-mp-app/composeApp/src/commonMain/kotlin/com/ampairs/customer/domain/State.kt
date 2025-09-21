package com.ampairs.customer.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class State(
    val id: String = "",
    val name: String = "",
    @SerialName("gst_code")
    val gstCode: Int = 0
)

@Serializable
data class StateListItem(
    val id: String,
    val name: String,
    val gstCode: Int
)

fun State.toListItem(): StateListItem = StateListItem(
    id = id,
    name = name,
    gstCode = gstCode
)