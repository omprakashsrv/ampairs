package com.ampairs.core.exception

import com.ampairs.core.domain.dto.ApiResponse
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.io.IOException

@Component
class AuthEntryPointJwt(private val objectMapper: ObjectMapper) : AuthenticationEntryPoint {
    @Throws(IOException::class, ServletException::class)
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        logger.error("Unauthorized error", authException)
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        val errorResponse = ApiResponse.error<Any>(
            code = "UNAUTHORIZED",
            message = "Unauthorized",
            path = request.requestURI
        )
        objectMapper.writeValue(response.outputStream, errorResponse)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthEntryPointJwt::class.java)
    }
}
