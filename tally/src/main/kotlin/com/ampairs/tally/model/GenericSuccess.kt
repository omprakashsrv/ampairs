package com.ampairs.tally.model

import com.fasterxml.jackson.annotation.JsonProperty

data class GenericSuccess(
    @JsonProperty("success")
    val success: Boolean,
    @JsonProperty("message")
    val message: String,
)