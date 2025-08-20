package com.ampairs.auth.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.db.dao.UserSessionDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.auth.db.entity.UserSessionEntity
import com.ampairs.auth.db.entity.UserTokenEntity
import kotlinx.coroutines.runBlocking

class TokenRepositoryImpl(
    val userTokenDao: UserTokenDao,
    val userSessionDao: UserSessionDao
) : TokenRepository {

    var companyIdTemp: String = ""
    private var currentUserId: String? = null

    // Legacy single-user methods (for backward compatibility)
    override fun getRefreshToken(): String? {
        return runBlocking { 
            getCurrentUserId()?.let { userId ->
                getRefreshTokenForUser(userId)
            } ?: userTokenDao.selectById()?.refresh_token
        }
    }

    override fun getAccessToken(): String? {
        return runBlocking { 
            getCurrentUserId()?.let { userId ->
                getAccessTokenForUser(userId)
            } ?: userTokenDao.selectById()?.access_token
        }
    }

    override fun updateToken(accessToken: String, refreshToken: String?) {
        runBlocking {
            getCurrentUserId()?.let { userId ->
                updateTokenForUser(userId, accessToken, refreshToken)
            } ?: run {
                // Fallback to legacy behavior
                userTokenDao.insertUserToken(
                    UserTokenEntity(
                        seq_id = 0, 
                        id = "1", 
                        user_id = "legacy_user",
                        refresh_token = refreshToken ?: "", 
                        access_token = accessToken, 
                        expires_at = 0
                    )
                )
            }
        }
    }

    override fun clearTokens() {
        runBlocking {
            getCurrentUserId()?.let { userId ->
                clearTokensForUser(userId)
            } ?: run {
                // Fallback to legacy behavior
                userTokenDao.insertUserToken(
                    UserTokenEntity(
                        seq_id = 0, 
                        id = "1", 
                        user_id = "legacy_user",
                        refresh_token = "", 
                        access_token = "", 
                        expires_at = 0
                    )
                )
            }
        }
    }

    override fun getCompanyId(): String {
        return runBlocking {
            getCurrentUserId()?.let { userId ->
                getCompanyIdForUser(userId)
            } ?: companyIdTemp
        }
    }

    override fun setCompanyId(companyId: String) {
        runBlocking {
            getCurrentUserId()?.let { userId ->
                setCompanyIdForUser(userId, companyId)
            } ?: run {
                companyIdTemp = companyId
            }
        }
    }

    // New multi-user methods
    override suspend fun getRefreshTokenForUser(userId: String): String? {
        return userTokenDao.selectByUserId(userId)?.refresh_token
    }

    override suspend fun getAccessTokenForUser(userId: String): String? {
        return userTokenDao.selectByUserId(userId)?.access_token
    }

    override suspend fun updateTokenForUser(userId: String, accessToken: String, refreshToken: String?) {
        userTokenDao.insertUserToken(
            UserTokenEntity(
                seq_id = 0,
                id = generateTokenId(userId),
                user_id = userId,
                refresh_token = refreshToken ?: "",
                access_token = accessToken,
                is_active = true,
                last_used = System.currentTimeMillis()
            )
        )
    }

    override suspend fun clearTokensForUser(userId: String) {
        userTokenDao.clearTokensForUser(userId)
    }

    override suspend fun getCompanyIdForUser(userId: String): String {
        return userSessionDao.selectByUserId(userId)?.company_id ?: ""
    }

    override suspend fun setCompanyIdForUser(userId: String, companyId: String) {
        val existingSession = userSessionDao.selectByUserId(userId)
        if (existingSession != null) {
            userSessionDao.updateCompanyId(userId, companyId)
        } else {
            userSessionDao.insert(
                UserSessionEntity(
                    user_id = userId,
                    company_id = companyId,
                    is_current = userId == currentUserId
                )
            )
        }
    }

    override suspend fun getCurrentUserId(): String? {
        if (currentUserId == null) {
            currentUserId = userSessionDao.getCurrentUserSession()?.user_id
        }
        return currentUserId
    }

    override suspend fun setCurrentUser(userId: String) {
        userSessionDao.clearCurrentUser()
        userSessionDao.setCurrentUser(userId)
        userTokenDao.activateUser(userId)
        currentUserId = userId
    }

    override suspend fun getActiveUsers(): List<String> {
        return userTokenDao.selectActiveUsers().map { it.user_id }
    }

    override suspend fun isUserAuthenticated(userId: String): Boolean {
        val token = userTokenDao.selectByUserId(userId)
        return token != null && 
               (token.access_token.isNotBlank() || token.refresh_token.isNotBlank()) &&
               token.is_active
    }

    override suspend fun logoutUser(userId: String) {
        userTokenDao.deactivateUser(userId)
        userSessionDao.deleteByUserId(userId)
        if (currentUserId == userId) {
            currentUserId = null
        }
    }

    override suspend fun addAuthenticatedUser(userId: String, accessToken: String, refreshToken: String?) {
        updateTokenForUser(userId, accessToken, refreshToken)
        
        // Create or update user session
        val existingSession = userSessionDao.selectByUserId(userId)
        if (existingSession != null) {
            userSessionDao.updateLoginInfo(userId)
        } else {
            userSessionDao.insert(
                UserSessionEntity(
                    user_id = userId,
                    is_current = false,
                    last_login = System.currentTimeMillis()
                )
            )
        }
    }

    private fun generateTokenId(userId: String): String {
        return "token_${userId}_${System.currentTimeMillis()}"
    }
}