package com.ampairs.tally.service

import com.ampairs.network.model.Response
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.ResolvableType
import org.springframework.http.*
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate


const val API_END_POINT = "http://localhost:8080"

class ApiClient(
    val apiRestTemplate: RestTemplate
) {
    val headers = HttpHeaders()

    init {
        headers.setContentType(MediaType.APPLICATION_JSON)
    }

    @Throws(RestClientException::class)
    fun <T> post(
        path: String,
        request: Any?,
        responseType: Class<T>
    ): T? {
        return exchange(HttpMethod.POST, path, request, responseType)
    }

    @Throws(RestClientException::class)
    fun <T> get(
        path: String,
        responseType: Class<T>
    ): T? {
        return exchange(HttpMethod.GET, path, null, responseType)
    }

    private fun <T> exchange(
        method: HttpMethod,
        path: String,
        request: Any?,
        responseType: Class<T>
    ): T? {
        try {
            val resolvableType: ResolvableType =
                ResolvableType.forClassWithGenerics(Response::class.java, responseType)
            val typeRef: ParameterizedTypeReference<Response<T>> =
                ParameterizedTypeReference.forType(resolvableType.type)
            val request = HttpEntity<Any>(request, headers)
            val response = apiRestTemplate.exchange(
                API_END_POINT + path, method, request,
                typeRef
            )
            return response.body?.response
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }


}