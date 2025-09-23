package com.ampairs.customer.util

/**
 * Simple KMP-compatible logger for Customer module
 * Replaces println statements with structured logging
 */
object CustomerLogger {

    private const val TAG = "Customer"

    /**
     * Log informational messages
     */
    fun info(message: String) {
        println("[$TAG] INFO: $message")
    }

    /**
     * Log warning messages
     */
    fun warn(message: String) {
        println("[$TAG] WARN: $message")
    }

    /**
     * Log error messages
     */
    fun error(message: String) {
        println("[$TAG] ERROR: $message")
    }

    /**
     * Log debug messages - typically used for development
     */
    fun debug(message: String) {
        println("[$TAG] DEBUG: $message")
    }
}