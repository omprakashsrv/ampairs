package com.ampairs.common

import com.ampairs.auth.api.TokenRepository
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * Custom exception for network-related errors
 */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

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

    // Global exception handling for network connectivity issues
    install(HttpCallValidator) {
        handleResponseExceptionWithRequest { exception, _ ->
            when (exception::class.simpleName) {
                "ConnectException" -> {
                    println("❌ Connection failed: ${exception.message}")
                    throw NetworkException("Unable to connect to server. Please check your network connection.", exception)
                }
                "UnknownHostException" -> {
                    println("❌ Host not found: ${exception.message}")
                    throw NetworkException("Server not found. Please check your network connection.", exception)
                }
                "SocketTimeoutException" -> {
                    println("❌ Request timed out: ${exception.message}")
                    throw NetworkException("Request timed out. Please try again.", exception)
                }
                else -> {
                    println("❌ Network error: ${exception.message}")
                    throw exception
                }
            }
        }
    }

    // Disable expectSuccess so we can manually handle 401s
    expectSuccess = false
    
    // Default request configuration
    defaultRequest {
        // Add workspace header only if available and user has selected a workspace
        val workspaceId = runBlocking { tokenRepository.getWorkspaceId() }
        if (workspaceId.isNotEmpty()) {
            header("X-Workspace-ID", workspaceId)
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