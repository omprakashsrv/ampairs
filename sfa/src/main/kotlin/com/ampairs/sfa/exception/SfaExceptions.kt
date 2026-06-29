package com.ampairs.sfa.exception

/** Validation failure in the SFA module (e.g. an ad-hoc rule violation). Maps to HTTP 422. */
class SfaValidationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
