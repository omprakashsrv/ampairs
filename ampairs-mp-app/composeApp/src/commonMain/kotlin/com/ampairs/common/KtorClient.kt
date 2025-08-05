package com.ampairs.common

import com.ampairs.auth.api.AUTH_ENDPOINT
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.model.RefreshToken
import com.ampairs.auth.api.model.Token
import com.ampairs.auth.domain.asRefreshTokens
import com.ampairs.network.model.Response
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun httpClient(engine: HttpClientEngine, tokenRepository: TokenRepository) = HttpClient(engine) {
    expectSuccess = true
    defaultRequest {
        if (tokenRepository.getCompanyId().isNotEmpty()) {
            header("X-Company", tokenRepository.getCompanyId())
        }
        contentType(ContentType.Application.Json)
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            }
        )
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                println("message = ${message}")
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
    install(Auth) {
        bearer {
            loadTokens {
                BearerTokens(
                    tokenRepository.getAccessToken() ?: "",
                    tokenRepository.getRefreshToken() ?: ""
                )
            }
            refreshTokens {
                // Check if current request is an auth endpoint - if so, don't refresh
                val currentUrl = response.request.url.toString()
                if (currentUrl.contains("/auth/v1/init") ||
                    currentUrl.contains("/auth/v1/verify") ||
                    currentUrl.contains("/auth/v1/logout") ||
                    currentUrl.contains("/auth/v1/refresh_token")
                ) {
                    return@refreshTokens null
                }
                
                val tokenResponse: Response<Token> = client.post {
                    url("$AUTH_ENDPOINT/auth/v1/refresh_token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        RefreshToken(
                            oldTokens?.refreshToken ?: tokenRepository.getRefreshToken()
                        )
                    )
                    markAsRefreshTokenRequest()
                }.body()
                val refreshTokens = tokenResponse.response?.asRefreshTokens()
                refreshTokens?.let { tokenRepository.updateToken(it.accessToken, it.refreshToken) }
                refreshTokens
            }
            // Exclude auth endpoints from bearer token authentication
            sendWithoutRequest { request ->
                val url = request.url.toString()
                url.contains("/auth/v1/init") ||
                        url.contains("/auth/v1/verify") ||
                        url.contains("/auth/v1/logout") ||
                        url.contains("/auth/v1/refresh_token")
            }
        }
    }
}