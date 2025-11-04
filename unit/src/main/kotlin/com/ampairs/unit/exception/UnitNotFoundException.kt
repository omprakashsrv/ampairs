package com.ampairs.unit.exception

class UnitNotFoundException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
