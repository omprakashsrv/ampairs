package com.ampairs.core.exception

import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.sql.SQLException


@ControllerAdvice
class AuthExceptionHandler {
    @ExceptionHandler(SQLException::class)
    fun sqlError(ex: Exception?): ResponseEntity<Any?>? {
        logger.error("SQL error", ex)
        val body: MutableMap<String, Any?> = HashMap()
        body["status"] = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
        body["error"] = "SQL Exception"
        body["message"] = "SQL Exception"
        return ResponseEntity.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).body(body)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}