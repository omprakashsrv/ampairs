package com.ampairs.auth.exception

/**
 * Thrown when a login or OTP request is made for an account that is temporarily
 * locked due to repeated failed attempts.
 *
 * @param remainingMinutes minutes until the lockout expires (0 if unknown)
 */
class AccountLockedException(val remainingMinutes: Long) : RuntimeException(
    "Account temporarily locked due to multiple failed attempts. Please try again in $remainingMinutes minutes."
)
