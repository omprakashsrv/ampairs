package com.ampairs.auth.db

import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.auth.db.entity.UserTokenEntity
import kotlinx.coroutines.runBlocking

class TokenRepositoryImpl(val userTokenDao: UserTokenDao) : TokenRepository {

    var companyIdTemp: String = ""

    override fun getRefreshToken(): String? {
        return runBlocking { userTokenDao.selectById()?.refresh_token }
    }

    override fun getAccessToken(): String? {
        return runBlocking { userTokenDao.selectById()?.access_token }
    }

    override fun updateToken(accessToken: String, refreshToken: String?) {
        runBlocking {
            userTokenDao.insertUserToken(
                UserTokenEntity(
                    seq_id = 0, 
                    id = "1", 
                    refresh_token = refreshToken ?: "", 
                    access_token = accessToken, 
                    expires_at = 0
                )
            )
        }
    }

    override fun clearTokens() {
        runBlocking {
            userTokenDao.insertUserToken(
                UserTokenEntity(
                    seq_id = 0, 
                    id = "1", 
                    refresh_token = "", 
                    access_token = "", 
                    expires_at = 0
                )
            )
        }
    }

    override fun getCompanyId(): String {
        return this.companyIdTemp
    }

    override fun setCompanyId(companyId: String) {
        this.companyIdTemp = companyId
    }

}