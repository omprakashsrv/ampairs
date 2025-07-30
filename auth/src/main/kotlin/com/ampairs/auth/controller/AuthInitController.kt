package com.ampairs.auth.controller

import com.ampairs.core.domain.dto.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/auth/v1")
class AuthInitController {

    @PostMapping("/init")
    fun initAuth(): ApiResponse<Map<String, Any>> {
        return ApiResponse.success(
            mapOf(
                "message" to "Auth initialization endpoint",
                "timestamp" to LocalDateTime.now(),
                "status" to "initialized"
            )
        )
    }
}