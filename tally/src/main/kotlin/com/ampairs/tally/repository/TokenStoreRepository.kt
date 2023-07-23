package com.ampairs.tally.repository

import com.ampairs.api.repository.UserPreferencesRepository
import com.ampairs.network.auth.model.Token

class TokenStoreRepository : UserPreferencesRepository {
    override suspend fun getToken(): Result<Token> {
        return Result.success(Token(accessToken = "", refreshToken = ""))
    }

    override suspend fun setToken(token: Token) {

    }

    override suspend fun setCompanyId(companyId: String) {

    }

    override suspend fun getCompanyId(): Result<String> {
        return Result.success("")
    }
}