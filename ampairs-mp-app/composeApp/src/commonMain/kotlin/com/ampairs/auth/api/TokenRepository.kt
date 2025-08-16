package com.ampairs.auth.api

interface TokenRepository {
    fun getRefreshToken(): String?
    fun getAccessToken(): String?
    fun updateToken(accessToken: String, refreshToken: String?)
    fun clearTokens()
    fun getCompanyId(): String
    fun setCompanyId(companyId: String)
}