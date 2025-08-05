package com.ampairs.auth.domain

import com.ampairs.auth.api.model.Token
import com.ampairs.auth.db.entity.UserTokenEntity
import io.ktor.client.plugins.auth.providers.BearerTokens

data class UserToken(
    val accessToken: String,
    val refreshToken: String,
)

fun Token.asDatabaseModel(): UserTokenEntity {
    return UserTokenEntity(seq_id = 0, id = "1", refresh_token = refreshToken, access_token = accessToken, expires_at = 0)
}

fun UserTokenEntity.asDomainModel(): UserToken {
    return UserToken(access_token, refresh_token)
}

fun Token.asRefreshTokens(): BearerTokens {
    return BearerTokens(accessToken = accessToken, refreshToken = refreshToken)
}