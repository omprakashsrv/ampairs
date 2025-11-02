package com.ampairs.core.controller

import com.ampairs.core.domain.dto.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/test")
class TestController {

    @GetMapping("/rate-limit")
    fun testRateLimit(): ApiResponse<Map<String, Any>> {
        return ApiResponse.success(
            mapOf(
                "message" to "Rate limit test endpoint",
                "timestamp" to LocalDateTime.now()
            )
        )
    }

    @GetMapping("/ping")
    fun ping(): ApiResponse<String> {
        return ApiResponse.success("pong")
    }
}