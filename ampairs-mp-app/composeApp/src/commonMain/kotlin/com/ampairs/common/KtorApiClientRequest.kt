package com.ampairs.common

import com.ampairs.network.model.ErrorResponse
import com.ampairs.network.model.Response
import com.ampairs.network.model.toResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.content.PartData

suspend inline fun <reified T> post(client: HttpClient, url: String, body: Any?): T {
    val responseBody = runCatching {
        client.post {
            url(url)
            setBody(body)
        }.body<T>()
    }
    return handleResponseBody(responseBody)
}

suspend inline fun <reified T> postMultiPart(
    client: HttpClient,
    url: String,
    parts: List<PartData>
): T {
    val responseBody = runCatching {
        client.submitFormWithBinaryData(formData = parts) {
            url(url)
        }.body<T>()
    }
    return handleResponseBody(responseBody)
}

suspend inline fun <reified T> get(client: HttpClient, url: String): T {
    return get(client, url, null)
}

suspend inline fun <reified T> get(
    client: HttpClient,
    url: String,
    parameters: Map<String, Any>?
): T {
    val responseBody = runCatching {
        client.get {
            url(url)
            parameters?.map {
                parameter(it.key, it.value)
            }
        }.body<T>()
    }
    return handleResponseBody(responseBody)
}

suspend inline fun <reified T> handleResponseBody(responseBody: Result<T>): T {
    if (responseBody.isSuccess) {
        return responseBody.getOrNull() ?: Response(
            error = com.ampairs.network.model.Error(
                code = 0,
                message = "Null response body"
            ),
            data = null
        ) as T
    }

    val exception = responseBody.exceptionOrNull()
    if (exception is ServerResponseException) {
        val errorResponse = runCatching {
            val errorResponse = exception.response.body<ErrorResponse>()
            errorResponse.toResponse()
        }
        return errorResponse.getOrNull() as? T ?: Response(
            error = com.ampairs.network.model.Error(
                code = exception.response.status.value,
                message = "Failed to parse error response"
            ),
            data = null
        ) as T
    }

    return Response(
        error = com.ampairs.network.model.Error(
            code = 0,
            message = exception?.message ?: "Unknown error"
        ),
        data = null
    ) as T
}