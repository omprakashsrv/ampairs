package com.ampairs.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.domain.LoginStatus
import com.ampairs.common.DeviceService
import com.ampairs.network.model.onError
import com.ampairs.network.model.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val deviceService: DeviceService,
) : ViewModel() {
    var phoneNumber by mutableStateOf("")
    var otp by mutableStateOf("")
    var sessionId by mutableStateOf("")
    var validPhoneNumber by mutableStateOf(true)
    var displayMessage by mutableStateOf("")
    var loading by mutableStateOf(false)
    var recaptchaLoading by mutableStateOf(false)
    var progressMessage by mutableStateOf("")
    var loginStatus by mutableStateOf(
        LoginStatus.INIT
    )

    fun checkUserLogin(onLoginStatus: (LoginStatus, userEntity: com.ampairs.auth.db.entity.UserEntity?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val token = userRepository.getToken()
            if (token != null) {
                val userEntity = userRepository.getUser()
                if (userEntity == null) {
                    val userApiResponse = userRepository.getUserApi()
                    userApiResponse.onSuccess {
                        val userData = this
                        viewModelScope.launch(Dispatchers.IO) {
                            userRepository.saveUser(userData)
                            val savedUserEntity = userRepository.getUser()
                            delay(1000)
                            viewModelScope.launch(Dispatchers.Main) {
                                loginStatus = LoginStatus.LOGGED_IN
                                onLoginStatus(loginStatus, savedUserEntity)
                            }
                        }
                    }.onError {
                        viewModelScope.launch(Dispatchers.Main) {
                            loginStatus = LoginStatus.LOGIN_FAILED
                            onLoginStatus(loginStatus, null)
                        }
                    }
                } else {
                    viewModelScope.launch(Dispatchers.Main) {
                        loginStatus = LoginStatus.LOGGED_IN
                        onLoginStatus(loginStatus, userEntity)
                    }
                }
            } else {
                viewModelScope.launch(Dispatchers.Main) {
                    loginStatus = LoginStatus.NOT_LOGGED_IN
                    onLoginStatus(loginStatus, null)
                }
            }
        }
    }

    fun authenticate(onAuthSuccess: (String) -> Unit) {
        loading = true
        recaptchaLoading = true
        progressMessage = "Verifying reCAPTCHA..."
        
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.initAuth(phoneNumber).onSuccess {
                viewModelScope.launch(Dispatchers.Main) {
                    loading = false
                    recaptchaLoading = false
                    progressMessage = ""
                    if (this@onSuccess.success && this@onSuccess.sessionId != null) {
                        this@LoginViewModel.sessionId = this@onSuccess.sessionId
                        onAuthSuccess(this@onSuccess.sessionId)
                    } else {
                        displayMessage = this@onSuccess.error?.message ?: "Authentication failed"
                    }
                }
            }.onError {
                displayMessage = this@onError.message
                viewModelScope.launch(Dispatchers.Main) {
                    loading = false
                    recaptchaLoading = false
                    progressMessage = ""
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        loading = false
    }

    fun completeAuthentication(onAuthComplete: () -> Unit) {
        loading = true
        recaptchaLoading = true
        progressMessage = "Verifying reCAPTCHA..."
        
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.completeAuth(sessionId, otp).onSuccess {
                tokenRepository.updateToken(this.accessToken, this.refreshToken)
                viewModelScope.launch(Dispatchers.Main) {
                    delay(1000)
                    onAuthComplete()
                    loading = false
                    recaptchaLoading = false
                    progressMessage = ""
                }
            }.onError {
                displayMessage = this@onError.message
                viewModelScope.launch(Dispatchers.Main) {
                    loading = false
                    recaptchaLoading = false
                    progressMessage = ""
                }
            }
        }
    }

    fun resendOtp(onResendSuccess: (String) -> Unit) {
        loading = true
        recaptchaLoading = true
        progressMessage = "Preparing to resend OTP..."
        
        viewModelScope.launch(Dispatchers.IO) {
            userRepository.resendOtp(phoneNumber).onSuccess {
                viewModelScope.launch(Dispatchers.Main) {
                    loading = false
                    recaptchaLoading = false
                    progressMessage = ""
                    if (this@onSuccess.success && this@onSuccess.sessionId != null) {
                        this@LoginViewModel.sessionId = this@onSuccess.sessionId
                        onResendSuccess(this@onSuccess.sessionId)
                    } else {
                        displayMessage = this@onSuccess.error?.message ?: "Failed to resend OTP"
                    }
                }
            }.onError {
                displayMessage = this@onError.message
                viewModelScope.launch(Dispatchers.Main) {
                    loading = false
                    recaptchaLoading = false
                    progressMessage = ""
                }
            }
        }
    }


}