package com.ampairs.auth.firebase

import cocoapods.FirebaseAuth.*
import cocoapods.FirebaseCore.FIRApp
import com.ampairs.auth.domain.FirebaseAuthResult
import com.ampairs.auth.domain.PhoneVerificationState
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.posix.log
import kotlin.coroutines.resume

/**
 * iOS Firebase Phone Authentication Implementation using Native Firebase iOS SDK
 *
 * Uses Firebase iOS SDK via CocoaPods/SPM for native phone authentication
 * Requires:
 * - GoogleService-Info.plist in Xcode project
 * - Firebase SDK added via Swift Package Manager in Xcode
 * - FirebaseAuth and FirebaseCore packages
 * - Push Notifications capability enabled
 *
 * See IOS_FIREBASE_XCODE_SETUP.md for complete Xcode configuration
 */
@OptIn(ExperimentalForeignApi::class)
actual class FirebaseAuthProvider {

    private val _verificationState = MutableStateFlow<PhoneVerificationState>(PhoneVerificationState.Idle)
    actual val verificationState: StateFlow<PhoneVerificationState> = _verificationState.asStateFlow()

    private var storedVerificationId: String? = null

    actual suspend fun initialize(): FirebaseAuthResult<Unit> {
        return try {
            // Configure Firebase if not already configured
            if (FIRApp.defaultApp() == null) {
                FIRApp.configure()
            }
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

                FIRPhoneAuthProvider.provider().verifyPhoneNumber(
                    phoneNumber = formattedPhone,
                    UIDelegate = null
                ) { verificationID, error ->
                    if (error != null) {
                        val errorMessage = error.localizedDescription ?: "Verification failed"
                        _verificationState.value = PhoneVerificationState.VerificationFailed(errorMessage)
                        continuation.resume(FirebaseAuthResult.Error(errorMessage))
                    } else if (verificationID != null) {
                        storedVerificationId = verificationID
                        _verificationState.value = PhoneVerificationState.CodeSent
                        continuation.resume(FirebaseAuthResult.Success(verificationID))
                    } else {
                        _verificationState.value = PhoneVerificationState.VerificationFailed("No verification ID")
                        continuation.resume(FirebaseAuthResult.Error("No verification ID returned"))
                    }
                }
            } catch (e: Exception) {
                _verificationState.value = PhoneVerificationState.VerificationFailed(
                    e.message ?: "Failed to send verification code"
                )
                continuation.resume(
                    FirebaseAuthResult.Error("Failed to send verification code: ${e.message}", e)
                )
            }
        }
    }

    actual suspend fun verifyCode(verificationId: String, code: String): FirebaseAuthResult<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val credential = FIRPhoneAuthProvider.provider().credentialWithVerificationID(
                    verificationID = verificationId,
                    verificationCode = code
                )

                FIRAuth.auth().signInWithCredential(credential) { authResult, error ->
                    if (error != null) {
                        val errorMessage = error.localizedDescription ?: "Verification failed"
                        _verificationState.value = PhoneVerificationState.VerificationFailed(errorMessage)
                        continuation.resume(FirebaseAuthResult.Error(errorMessage))
                    } else if (authResult?.user() != null) {
                        val user = authResult.user()!!

                        // Get ID token (JWT)
                        user.getIDTokenWithCompletion { idToken, tokenError ->
                            if (tokenError != null) {
                                val errorMessage = tokenError.localizedDescription ?: "Failed to get token"
                                continuation.resume(FirebaseAuthResult.Error(errorMessage))
                            } else if (idToken != null) {
                                _verificationState.value = PhoneVerificationState.VerificationCompleted(user.uid())
                                continuation.resume(FirebaseAuthResult.Success(idToken))
                            } else {
                                continuation.resume(FirebaseAuthResult.Error("No ID token returned"))
                            }
                        }
                    } else {
                        continuation.resume(FirebaseAuthResult.Error("No user returned"))
                    }
                }
            } catch (e: Exception) {
                _verificationState.value = PhoneVerificationState.VerificationFailed(
                    e.message ?: "Invalid verification code"
                )
                continuation.resume(
                    FirebaseAuthResult.Error("Verification failed: ${e.message}", e)
                )
            }
        }
    }

    actual suspend fun resendVerificationCode(phoneNumber: String): FirebaseAuthResult<String> {
        // iOS Firebase SDK doesn't have a separate resend method
        // Just call sendVerificationCode again
        return sendVerificationCode(phoneNumber)
    }

    actual suspend fun getCurrentUserId(): String? {
        return try {
            FIRAuth.auth().currentUser()?.uid()
        } catch (e: Exception) {
            null
        }
    }

    actual suspend fun signOut(): FirebaseAuthResult<Unit> {
        return try {
            memScoped {
                val errorPtr = alloc<ObjCObjectVar<NSError?>>()
                FIRAuth.auth().signOut(errorPtr.ptr)
                val error = errorPtr.value

                if (error != null) {
                    FirebaseAuthResult.Error("Sign out failed: ${error.localizedDescription}")
                } else {
                    _verificationState.value = PhoneVerificationState.Idle
                    storedVerificationId = null
                    FirebaseAuthResult.Success(Unit)
                }
            }
        } catch (e: Exception) {
            FirebaseAuthResult.Error("Sign out failed: ${e.message}", e)
        }
    }

    /**
     * Returns true if Firebase is configured
     * This will be true once Firebase SDK is added in Xcode
     */
    actual fun isSupported(): Boolean {
        return try {
            // Check if Firebase is configured
            FIRApp.defaultApp() != null
        } catch (e: Exception) {
            println(e)
            false
        }
    }
}
