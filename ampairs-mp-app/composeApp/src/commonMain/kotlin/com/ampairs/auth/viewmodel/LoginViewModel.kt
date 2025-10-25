package com.ampairs.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.auth.domain.AuthMethod
import com.ampairs.auth.domain.FirebaseAuthResult
import com.ampairs.auth.domain.LoginStatus
import com.ampairs.auth.firebase.FirebaseAuthRepository
import com.ampairs.common.DeviceService
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.model.onError
import com.ampairs.common.model.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val deviceService: DeviceService,
    private val firebaseAuthRepository: FirebaseAuthRepository,
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

    // Firebase-specific state
    var firebaseVerificationId by mutableStateOf("")

    // Authentication method selection
    var authMethod by mutableStateOf(
        if (firebaseAuthRepository.isSupported()) AuthMethod.FIREBASE else AuthMethod.BACKEND_API
    )

    // Check if Firebase is supported on this platform
    val isFirebaseSupported: Boolean = firebaseAuthRepository.isSupported()

    // Existing user state
    var existingUser by mutableStateOf<UserEntity?>(null)

    fun checkUserLogin(onLoginStatus: (LoginStatus, userEntity: UserEntity?) -> Unit) {
        viewModelScope.launch(DispatcherProvider.io) {
            val token = userRepository.getToken()
            if (token != null) {
                if (token.refreshToken.isEmpty() || token.accessToken.isEmpty()) {
                    viewModelScope.launch(Dispatchers.Main) {
                        loginStatus = LoginStatus.NOT_LOGGED_IN
                        onLoginStatus(loginStatus, null)
                    }
                    return@launch
                }
                val userEntity = userRepository.getUser()
                if (userEntity == null) {
                    val userApiResponse = userRepository.getUserApi()
                    userApiResponse.onSuccess {
                        val userData = this
                        viewModelScope.launch(DispatcherProvider.io) {
                            // Save the new user
                            userRepository.saveUser(userData)

                            // Associate the current token with this user
                            tokenRepository.addAuthenticatedUser(userData.id, token.accessToken, token.refreshToken)

                            // Set this user as the current user
                            tokenRepository.setCurrentUser(userData.id)

                            // Get the saved user entity
                            val savedUserEntity = userRepository.getUserById(userData.id)
                            
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
                    // User exists, make sure they're set as current user
                    viewModelScope.launch(DispatcherProvider.io) {
                        tokenRepository.setCurrentUser(userEntity.id)
                        viewModelScope.launch(Dispatchers.Main) {
                            loginStatus = LoginStatus.LOGGED_IN
                            onLoginStatus(loginStatus, userEntity)
                        }
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

    fun checkExistingUser(onExistingUserFound: (UserEntity) -> Unit, onNoExistingUser: () -> Unit) {
        viewModelScope.launch(DispatcherProvider.io) {
            val user = userRepository.findExistingUser(countryCode = 91, phone = phoneNumber)
            viewModelScope.launch(Dispatchers.Main) {
                if (user != null) {
                    existingUser = user
                    onExistingUserFound(user)
                } else {
                    existingUser = null
                    onNoExistingUser()
                }
            }
        }
    }

    fun selectExistingUser(userId: String, onUserSelected: () -> Unit) {
        viewModelScope.launch(DispatcherProvider.io) {
            tokenRepository.setCurrentUser(userId)
            viewModelScope.launch(Dispatchers.Main) {
                onUserSelected()
            }
        }
    }

    fun authenticate(onAuthSuccess: (String) -> Unit) {
        loading = true
        recaptchaLoading = true
        progressMessage = "Verifying reCAPTCHA..."

        viewModelScope.launch(DispatcherProvider.io) {
            // Create dummy user session for token operations during auth flow
            tokenRepository.createDummyUserSession()

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

        viewModelScope.launch(DispatcherProvider.io) {
            userRepository.completeAuth(sessionId, otp).onSuccess {
                // Store tokens to dummy session (now getCurrentUserId() will work)
                tokenRepository.updateToken(this.accessToken, this.refreshToken)

                // Store the token response for later use
                val authResponse = this

                // Now fetch user information (API call will work because dummy session has tokens)
                viewModelScope.launch(DispatcherProvider.io) {
                    val userApiResponse = userRepository.getUserApi()
                    userApiResponse.onSuccess {
                        val userData = this
                        viewModelScope.launch(DispatcherProvider.io) {
                            // Save user to database
                            userRepository.saveUser(userData)

                            // Replace dummy session with real user session and tokens
                            tokenRepository.updateDummySessionWithRealUser(
                                userData.id,
                                authResponse.accessToken, authResponse.refreshToken
                            )

                            viewModelScope.launch(Dispatchers.Main) {
                                delay(1000)
                                onAuthComplete()
                                loading = false
                                recaptchaLoading = false
                                progressMessage = ""
                            }
                        }
                    }.onError {
                        // Even if user fetch fails, continue with authentication complete
                        // The user will be fetched later in checkUserLogin()
                        viewModelScope.launch(Dispatchers.Main) {
                            delay(1000)
                            onAuthComplete()
                            loading = false
                            recaptchaLoading = false
                            progressMessage = ""
                        }
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

    fun resendOtp(onResendSuccess: (String) -> Unit) {
        loading = true
        recaptchaLoading = true
        progressMessage = "Preparing to resend OTP..."

        viewModelScope.launch(DispatcherProvider.io) {
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

    // ========== Firebase Authentication Methods ==========

    /**
     * Send OTP via Firebase
     */
    fun authenticateWithFirebase(onAuthSuccess: (String) -> Unit) {
        loading = true
        progressMessage = "Sending verification code..."

        viewModelScope.launch(DispatcherProvider.io) {
            // Extract country code (assuming 91 for now, can be made dynamic)
            val countryCode = "91"

            when (val result = firebaseAuthRepository.sendOtp(countryCode, phoneNumber)) {
                is FirebaseAuthResult.Success -> {
                    firebaseVerificationId = result.data
                    viewModelScope.launch(Dispatchers.Main) {
                        loading = false
                        progressMessage = ""
                        onAuthSuccess(result.data)
                    }
                }
                is FirebaseAuthResult.Error -> {
                    viewModelScope.launch(Dispatchers.Main) {
                        loading = false
                        progressMessage = ""
                        displayMessage = result.message
                    }
                }
                FirebaseAuthResult.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    /**
     * Verify Firebase OTP and complete authentication
     */
    fun completeFirebaseAuthentication(onAuthComplete: () -> Unit) {
        loading = true
        progressMessage = "Verifying code..."

        viewModelScope.launch(DispatcherProvider.io) {
            // Create dummy user session for token operations during auth flow
            tokenRepository.createDummyUserSession()

            when (val result = firebaseAuthRepository.verifyOtp(firebaseVerificationId, otp)) {
                is FirebaseAuthResult.Success -> {
                    val firebaseIdToken = result.data

                    // Get the phone number that was used for Firebase verification
                    val verifiedPhoneNumber = firebaseAuthRepository.getLastPhoneNumber()

                    // After Firebase auth succeeds, verify with backend and get JWT tokens
                    viewModelScope.launch(DispatcherProvider.io) {
                        userRepository.verifyFirebaseAuth(firebaseIdToken, verifiedPhoneNumber).onSuccess {
                            // Store tokens to dummy session (now getCurrentUserId() will work)
                            tokenRepository.updateToken(this.accessToken, this.refreshToken)

                            // Store the token response for later use
                            val authResponse = this

                            // Now fetch user information (API call will work because dummy session has tokens)
                            viewModelScope.launch(DispatcherProvider.io) {
                                val userApiResponse = userRepository.getUserApi()
                                userApiResponse.onSuccess {
                                    val userData = this
                                    viewModelScope.launch(DispatcherProvider.io) {
                                        // Save user to database
                                        userRepository.saveUser(userData)

                                        // Replace dummy session with real user session and tokens
                                        tokenRepository.updateDummySessionWithRealUser(
                                            userData.id,
                                            authResponse.accessToken, authResponse.refreshToken
                                        )

                                        viewModelScope.launch(Dispatchers.Main) {
                                            delay(1000)
                                            onAuthComplete()
                                            loading = false
                                            progressMessage = ""
                                        }
                                    }
                                }.onError {
                                    // Even if user fetch fails, continue with authentication complete
                                    // The user will be fetched later in checkUserLogin()
                                    viewModelScope.launch(Dispatchers.Main) {
                                        delay(1000)
                                        onAuthComplete()
                                        loading = false
                                        progressMessage = ""
                                    }
                                }
                            }
                        }.onError {
                            displayMessage = this@onError.message
                            viewModelScope.launch(Dispatchers.Main) {
                                loading = false
                                progressMessage = ""
                            }
                        }
                    }
                }
                is FirebaseAuthResult.Error -> {
                    viewModelScope.launch(Dispatchers.Main) {
                        loading = false
                        progressMessage = ""
                        displayMessage = result.message
                    }
                }
                FirebaseAuthResult.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

    /**
     * Resend OTP via Firebase
     */
    fun resendFirebaseOtp(onResendSuccess: (String) -> Unit) {
        loading = true
        progressMessage = "Resending verification code..."

        viewModelScope.launch(DispatcherProvider.io) {
            val countryCode = "91"

            when (val result = firebaseAuthRepository.resendOtp(countryCode, phoneNumber)) {
                is FirebaseAuthResult.Success -> {
                    firebaseVerificationId = result.data
                    viewModelScope.launch(Dispatchers.Main) {
                        loading = false
                        progressMessage = ""
                        onResendSuccess(result.data)
                    }
                }
                is FirebaseAuthResult.Error -> {
                    viewModelScope.launch(Dispatchers.Main) {
                        loading = false
                        progressMessage = ""
                        displayMessage = result.message
                    }
                }
                FirebaseAuthResult.Loading -> {
                    // Already in loading state
                }
            }
        }
    }

}