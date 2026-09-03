package com.ampairs.cb_employee.exception

class EmployeeNotFoundException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
