package com.ampairs.unit.exception

class UnitInUseException(
    val unitUid: String,
    val entityIds: List<String> = emptyList(),
    val conversionIds: List<String> = emptyList(),
    message: String = "Unit $unitUid is in use and cannot be deleted",
    cause: Throwable? = null
) : RuntimeException(message, cause)
