package com.ampairs.auth.db

import com.ampairs.auth.api.AuthApi
import com.ampairs.auth.api.model.AuthComplete
import com.ampairs.auth.api.model.AuthInit
import com.ampairs.auth.api.model.AuthInitResponse
import com.ampairs.auth.api.model.Token
import com.ampairs.auth.api.model.UserApiModel
import com.ampairs.auth.db.dao.UserDao
import com.ampairs.auth.db.dao.UserTokenDao
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.auth.domain.DeviceSession
import com.ampairs.auth.domain.UserToken
import com.ampairs.auth.domain.asDatabaseModel
import com.ampairs.auth.domain.asDomainModel
import com.ampairs.auth.service.RecaptchaService
import com.ampairs.common.DeviceService
import com.ampairs.network.model.GenericSuccess
import com.ampairs.network.model.Response

class UserRepository(
    val authApi: AuthApi,
    val userTokenDao: UserTokenDao,
    val userDao: UserDao,
    val deviceService: DeviceService,
    val recaptchaService: RecaptchaService,
) {
    suspend fun initAuth(phoneNumber: String): Response<AuthInitResponse> {
        val deviceInfo = deviceService.getDeviceInfo()
        val recaptchaToken = recaptchaService.executeLogin()
        
        return authApi.initAuth(
            AuthInit(
                countryCode = 91,
                phone = phoneNumber,
                recaptchaToken = recaptchaToken,
                deviceId = deviceInfo.deviceId,
                deviceName = deviceInfo.deviceName,
                deviceType = deviceInfo.deviceType,
                platform = deviceInfo.platform,
                browser = deviceInfo.browser,
                os = deviceInfo.os
            )
        )
    }

    suspend fun completeAuth(sessionId: String, otp: String): Response<Token> {
        val deviceInfo = deviceService.getDeviceInfo()
        val recaptchaToken = recaptchaService.executeVerifyOtp()
        
        val completeAuth = authApi.completeAuth(
            AuthComplete(
                sessionId = sessionId,
                otp = otp,
                authMode = "SMS",
                recaptchaToken = recaptchaToken,
                deviceId = deviceInfo.deviceId,
                deviceName = deviceInfo.deviceName,
            )
        )
        authApi.clearToken()
        return completeAuth
    }

    suspend fun getUserApi(): Response<UserApiModel> {
        return authApi.getUser()
    }

    suspend fun getToken(): UserToken? {
        return userTokenDao.selectById()?.asDomainModel()
    }

    suspend fun getUser(): UserEntity? {
        return userDao.selectAll().firstOrNull()
    }

    suspend fun saveUser(user: UserApiModel) {
        userDao.insert(user.asDatabaseModel())
    }

    suspend fun getDeviceSessions(): Response<List<DeviceSession>> {
        return authApi.getDeviceSessions()
    }

    suspend fun logoutDevice(deviceId: String): Response<GenericSuccess> {
        return authApi.logoutDevice(deviceId)
    }

    suspend fun logoutAllDevices(): Response<GenericSuccess> {
        return authApi.logoutAllDevices()
    }

    suspend fun resendOtp(phoneNumber: String): Response<AuthInitResponse> {
        val deviceInfo = deviceService.getDeviceInfo()
        val recaptchaToken = recaptchaService.executeResendOtp()
        
        return authApi.initAuth(
            AuthInit(
                countryCode = 91,
                phone = phoneNumber,
                recaptchaToken = recaptchaToken,
                deviceId = deviceInfo.deviceId,
                deviceName = deviceInfo.deviceName,
                deviceType = deviceInfo.deviceType,
                platform = deviceInfo.platform,
                browser = deviceInfo.browser,
                os = deviceInfo.os
            )
        )
    }

}