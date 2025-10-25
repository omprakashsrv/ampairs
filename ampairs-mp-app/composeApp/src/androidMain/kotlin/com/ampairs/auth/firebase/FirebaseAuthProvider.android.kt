package com.ampairs.auth.firebase

import android.app.Activity
import android.content.Context
import androidx.activity.ComponentActivity
import com.ampairs.auth.domain.FirebaseAuthResult
import com.ampairs.auth.domain.PhoneVerificationState
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Native Android Firebase Phone Authentication Implementation
 *
 * Uses official Firebase Android SDK for phone authentication
 * Requires:
 * - google-services.json in composeApp/
 * - Activity context for reCAPTCHA verification
 * - Firebase project with Phone Auth enabled
 */
actual class FirebaseAuthProvider : KoinComponent {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val context: Context by inject() // Get context from Koin

    // Get activity from context
    private val activity: Activity
        get() = context as? ComponentActivity
            ?: throw IllegalStateException("Firebase Phone Auth requires Activity context")

    private val _verificationState = MutableStateFlow<PhoneVerificationState>(PhoneVerificationState.Idle)
    actual val verificationState: StateFlow<PhoneVerificationState> = _verificationState.asStateFlow()

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    actual suspend fun initialize(): FirebaseAuthResult<Unit> {
        return try {
            // Firebase is auto-initialized via google-services.json
            FirebaseAuthResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseAuthResult.Error("Firebase initialization failed: ${e.message}", e)
        }
    }

    actual suspend fun sendVerificationCode(phoneNumber: String): FirebaseAuthResult<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                _verificationState.value = PhoneVerificationState.Idle

                val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+$phoneNumber"

                val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(formattedPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            // Auto-verification succeeded
                            _verificationState.value = PhoneVerificationState.CodeSent
                            storedVerificationId = credential.smsCode ?: ""

                            // Auto-sign in
                            signInWithCredential(credential) { result ->
                                when (result) {
                                    is FirebaseAuthResult.Success -> {
                                        _verificationState.value = PhoneVerificationState.VerificationCompleted(result.data)
                                        if (continuation.isActive) {
                                            continuation.resume(FirebaseAuthResult.Success(storedVerificationId ?: "auto"))
                                        }
                                    }
                                    is FirebaseAuthResult.Error -> {
                                        if (continuation.isActive) {
                                            continuation.resume(result)
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }

                        override fun onVerificationFailed(exception: FirebaseException) {
                            _verificationState.value = PhoneVerificationState.VerificationFailed(
                                exception.message ?: "Verification failed"
                            )
                            if (continuation.isActive) {
                                continuation.resume(
                                    FirebaseAuthResult.Error("Verification failed: ${exception.message}", exception)
                                )
                            }
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            storedVerificationId = verificationId
                            resendToken = token
                            _verificationState.value = PhoneVerificationState.CodeSent

                            if (continuation.isActive) {
                                continuation.resume(FirebaseAuthResult.Success(verificationId))
                            }
                        }
                    })
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(options)

            } catch (e: Exception) {
                _verificationState.value = PhoneVerificationState.VerificationFailed(
                    e.message ?: "Failed to send verification code"
                )
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    actual suspend fun verifyCode(verificationId: String, code: String): FirebaseAuthResult<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val credential = PhoneAuthProvider.getCredential(verificationId, code)

                signInWithCredential(credential) { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

            } catch (e: Exception) {
                _verificationState.value = PhoneVerificationState.VerificationFailed(
                    e.message ?: "Invalid verification code"
                )
                if (continuation.isActive) {
                    continuation.resume(FirebaseAuthResult.Error("Verification failed: ${e.message}", e))
                }
            }
        }
    }

    private fun signInWithCredential(
        credential: PhoneAuthCredential,
        onComplete: (FirebaseAuthResult<String>) -> Unit
    ) {
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid
                if (userId != null) {
                    _verificationState.value = PhoneVerificationState.VerificationCompleted(userId)
                    onComplete(FirebaseAuthResult.Success(userId))
                } else {
                    _verificationState.value = PhoneVerificationState.VerificationFailed("No user ID returned")
                    onComplete(FirebaseAuthResult.Error("Authentication succeeded but no user ID returned"))
                }
            }
            .addOnFailureListener { exception ->
                _verificationState.value = PhoneVerificationState.VerificationFailed(
                    exception.message ?: "Authentication failed"
                )
                onComplete(FirebaseAuthResult.Error("Sign in failed: ${exception.message}", exception))
            }
    }

    actual suspend fun resendVerificationCode(phoneNumber: String): FirebaseAuthResult<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+$phoneNumber"

                val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(formattedPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            _verificationState.value = PhoneVerificationState.CodeSent
                            if (continuation.isActive) {
                                continuation.resume(FirebaseAuthResult.Success(storedVerificationId ?: "auto"))
                            }
                        }

                        override fun onVerificationFailed(exception: FirebaseException) {
                            _verificationState.value = PhoneVerificationState.VerificationFailed(
                                exception.message ?: "Verification failed"
                            )
                            if (continuation.isActive) {
                                continuation.resume(
                                    FirebaseAuthResult.Error("Resend failed: ${exception.message}", exception)
                                )
                            }
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            storedVerificationId = verificationId
                            resendToken = token
                            _verificationState.value = PhoneVerificationState.CodeSent

                            if (continuation.isActive) {
                                continuation.resume(FirebaseAuthResult.Success(verificationId))
                            }
                        }
                    })

                // Use resend token if available
                resendToken?.let { optionsBuilder.setForceResendingToken(it) }

                PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())

            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    actual suspend fun getCurrentUserId(): String? {
        return try {
            firebaseAuth.currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }

    actual suspend fun signOut(): FirebaseAuthResult<Unit> {
        return try {
            firebaseAuth.signOut()
            _verificationState.value = PhoneVerificationState.Idle
            storedVerificationId = null
            resendToken = null
            FirebaseAuthResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseAuthResult.Error("Sign out failed: ${e.message}", e)
        }
    }

    actual fun isSupported(): Boolean = true
}
