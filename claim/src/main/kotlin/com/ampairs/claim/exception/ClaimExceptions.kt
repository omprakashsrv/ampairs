package com.ampairs.claim.exception

/** An illegal claim-lifecycle transition was attempted → HTTP 409. */
class ClaimStateException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** Generic claim validation failure → HTTP 422. */
class ClaimException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
