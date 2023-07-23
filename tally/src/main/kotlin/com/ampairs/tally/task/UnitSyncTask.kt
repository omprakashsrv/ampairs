package com.ampairs.tally.task

import com.ampairs.network.auth.AuthApi
import com.ampairs.network.auth.model.AuthInit
import com.skydoves.sandwich.onError
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.onSuccess
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class UnitSyncTask @Autowired constructor(val authApi: AuthApi) {

    @Scheduled(fixedDelay = 2 * 10 * 1000)
    fun syncUnits() {
        runBlocking {
            authApi.initAuth(AuthInit(91, "9591781662")).onSuccess {
                println("data.response = ${data.response}")
            }.onError {
                println("errorBody = ${errorBody}")
            }.onFailure {
                println("onFailure")
            }
        }
    }
}

