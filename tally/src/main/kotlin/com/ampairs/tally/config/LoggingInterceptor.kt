package com.ampairs.tally.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.nio.charset.StandardCharsets

val LOGGER: Logger = LoggerFactory.getLogger(LoggingInterceptor::class.java)

class LoggingInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        LOGGER.info("Request body: {}", String(body, StandardCharsets.UTF_8))
        return execution.execute(request, body)
    }
}