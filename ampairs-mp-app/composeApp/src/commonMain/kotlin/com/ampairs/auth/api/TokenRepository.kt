package com.ampairs.auth.api

interface TokenRepository {
    fun getRefreshToken(): String?
    fun getAccessToken(): String?
    fun updateToken(accessToken: String, refreshToken: String?)
    fun clearTokens()
    fun getCompanyId(): String
    fun setCompanyId(companyId: String)
}

/**
 * Extension function to check if user is authenticated
 */
fun TokenRepository.isAuthenticated(): Boolean {
    val accessToken = getAccessToken()
    val refreshToken = getRefreshToken()
    return !accessToken.isNullOrBlank() || !refreshToken.isNullOrBlank()
}