package com.ampairs.tally.repository

import com.ampairs.api.repository.UserPreferencesRepository
import com.ampairs.network.auth.model.Token

class TokenStoreRepository : UserPreferencesRepository {
    override suspend fun getToken(): Result<Token> {
        return Result.success(
            Token(
                accessToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5MTk1OTE3ODE2NjIiLCJpYXQiOjE2OTA2OTc5NDUsImV4cCI6MTY5MDc4NDM0NX0.WlR-JQXNCG-c5JCt-2xX92iSyKpKIO50q6xuAsWw8NY",
                refreshToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI5MTk1OTE3ODE2NjIiLCJpYXQiOjE2OTA2OTc5NDYsImV4cCI6MTcwNjI0OTk0Nn0.pKL3tnZlWpzTxBnBYTFZt4A4LVH4HW7IG5KcgpsupvM"
            )
        )
    }

    override suspend fun setToken(token: Token) {

    }

    override suspend fun setCompanyId(companyId: String) {

    }

    override suspend fun getCompanyId(): Result<String> {
        return Result.success("CMP12345")
    }
}