package com.ampairs.cb_store.exception

class StoreNotFoundException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class ZonalOfficeNotFoundException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
