package com.ampairs.core.exception

import com.ampairs.core.domain.dto.ErrorResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.*


@ControllerAdvice
class AuthExceptionHandler {
    @Value("\${drees.stacktrace}")
    var stackTrace = false

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    fun processAllError(ex: Exception): ErrorResponse {
        if (stackTrace) {
            logger.error("Exception", ex)
        }
        return ErrorResponse(500, ex.message ?: "System Error")
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}