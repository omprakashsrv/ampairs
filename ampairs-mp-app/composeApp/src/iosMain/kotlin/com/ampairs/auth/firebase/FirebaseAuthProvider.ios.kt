package com.ampairs.auth.firebase

import com.ampairs.auth.domain.FirebaseAuthResult
import com.ampairs.auth.domain.PhoneVerificationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS stub implementation of Firebase Phone Authentication
 *
 * TODO: Full Firebase Phone Auth integration with GitLive SDK
 *
 * The GitLive Firebase KMP SDK has different APIs than native Firebase SDK.
 * Phone authentication on iOS requires special handling with APNs configuration.
 *
 * For now, this returns "not supported" to default to backend API authentication.
 * Full Firebase integration will be implemented in a future update when the
 * GitLive SDK's phone auth APIs are properly integrated with iOS lifecycle.
 */
actual class FirebaseAuthProvider {

    private val _verificationState = MutableStateFlow<PhoneVerificationState>(PhoneVerificationState.Idle)
    actual val verificationState: StateFlow<PhoneVerificationState> = _verificationState.asStateFlow()

    actual suspend fun initialize(): FirebaseAuthResult<Unit> {
        return FirebaseAuthResult.Error(
            "Firebase Phone Authentication not yet implemented for iOS. " +
            "Please use Backend API authentication. " +
            "Firebase integration coming in future update."
        )
    }

    actual suspend fun sendVerificationCode(phoneNumber: String): FirebaseAuthResult<String> {
        return FirebaseAuthResult.Error(
            "Firebase Phone Authentication not available. Please use Backend API authentication."
        )
    }

    actual suspend fun verifyCode(verificationId: String, code: String): FirebaseAuthResult<String> {
        return FirebaseAuthResult.Error(
            "Firebase Phone Authentication not available. Please use Backend API authentication."
        )
    }

    actual suspend fun resendVerificationCode(phoneNumber: String): FirebaseAuthResult<String> {
        return FirebaseAuthResult.Error(
            "Firebase Phone Authentication not available. Please use Backend API authentication."
        )
    }

    actual suspend fun getCurrentUserId(): String? {
        return null
    }

    actual suspend fun signOut(): FirebaseAuthResult<Unit> {
        return FirebaseAuthResult.Error("Not supported")
    }

    actual fun isSupported(): Boolean = false
}
