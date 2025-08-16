package com.ampairs.common

import com.ampairs.auth.api.TokenRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun httpClient(engine: HttpClientEngine, tokenRepository: TokenRepository) = HttpClient(engine) {
    
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            }
        )
    }
    
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                println("HTTP: $message")
            }
        }
        level = LogLevel.INFO
    }
    
    install(HttpTimeout) {
        val timeout = 30000L
        connectTimeoutMillis = timeout
        requestTimeoutMillis = timeout
        socketTimeoutMillis = timeout
    }
    
    // Disable expectSuccess so we can manually handle 401s
    expectSuccess = false
    
    // Default request configuration
    defaultRequest {
        // Add company header if available
        val companyId = tokenRepository.getCompanyId()
        if (companyId.isNotEmpty()) {
            header("X-Company", companyId)
        }
        
        // Add bearer token if available
        val accessToken = tokenRepository.getAccessToken()
        if (!accessToken.isNullOrEmpty()) {
            header("Authorization", "Bearer $accessToken")
        }
        
        // Set default content type
        contentType(ContentType.Application.Json)
    }
}