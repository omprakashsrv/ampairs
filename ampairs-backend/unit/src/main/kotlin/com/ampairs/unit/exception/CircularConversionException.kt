package com.ampairs.unit.exception

class CircularConversionException(
    val cycle: List<String>,
    message: String = "Circular unit conversion detected: ${cycle.joinToString(" -> ")}",
    cause: Throwable? = null
) : RuntimeException(message, cause)
