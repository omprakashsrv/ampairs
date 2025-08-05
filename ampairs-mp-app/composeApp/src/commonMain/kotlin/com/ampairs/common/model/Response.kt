package com.ampairs.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Response<T>(
    @SerialName("response") var response: T? = null,
    @SerialName("error") var error: Error? = null,
)


inline fun <T> Response<T>.onSuccess(
    crossinline onResult: T.() -> Unit,
): Response<T> {
    if (this.response != null) {
        onResult(this.response!!)
    }
    return this
}

inline fun <T> Response<T>.onError(
    crossinline onResult: Error.() -> Unit,
): Response<T> {
    if (this.error != null || this.response == null) {
        onResult(this.error ?: Error())
    }
    return this
}

