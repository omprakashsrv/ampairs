package com.ampairs.event.config

import com.ampairs.auth.service.JwtService
import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.HandshakeInterceptor
import java.security.Principal

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val jwtService: JwtService
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // Use STOMP broker relay for distributed WebSocket support
        registry.enableStompBrokerRelay("/topic", "/queue")
            .setRelayHost("${System.getenv("RABBITMQ_HOST") ?: "localhost"}")
            .setRelayPort(61613) // STOMP port
            .setClientLogin("${System.getenv("RABBITMQ_USER") ?: "guest"}")
            .setClientPasscode("${System.getenv("RABBITMQ_PASSWORD") ?: "guest"}")
            .setSystemLogin("${System.getenv("RABBITMQ_USER") ?: "guest"}")
            .setSystemPasscode("${System.getenv("RABBITMQ_PASSWORD") ?: "guest"}")
            .setVirtualHost("${System.getenv("RABBITMQ_VHOST") ?: "/"}")

        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")
            .addInterceptors(WebSocketAuthInterceptor(jwtService))
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(WebSocketChannelInterceptor(jwtService))
    }
}

/**
 * HandshakeInterceptor for JWT validation during WebSocket connection
 */
class WebSocketAuthInterceptor(
    private val jwtService: JwtService
) : HandshakeInterceptor {

    private val logger = LoggerFactory.getLogger(WebSocketAuthInterceptor::class.java)

    override fun beforeHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>
    ): Boolean {
        try {
            // Extract JWT from query parameter or header
            val token = extractToken(request)
                ?: run {
                    logger.warn("No JWT token found in WebSocket handshake")
                    return false
                }

            // Validate token
            val username = jwtService.extractUsername(token)
            val userId = jwtService.extractUserId(token)
            val tenantId = jwtService.extractTenantId(token)
            val deviceId = jwtService.extractDeviceId(token)

            if (username.isNullOrBlank()) {
                logger.warn("Invalid JWT token: no username")
                return false
            }

            // Store in session attributes
            attributes["username"] = username
            attributes["userId"] = userId ?: ""
            attributes["tenantId"] = tenantId ?: ""
            attributes["deviceId"] = deviceId ?: ""
            attributes["token"] = token

            logger.debug(
                "WebSocket handshake successful for user: {}, tenant: {}, device: {}",
                username, tenantId, deviceId
            )

            return true
        } catch (e: Exception) {
            logger.error("WebSocket authentication failed", e)
            return false
        }
    }

    override fun afterHandshake(
        request: ServerHttpRequest,
        response: ServerHttpResponse,
        wsHandler: WebSocketHandler,
        exception: Exception?
    ) {
        // No-op
    }

    private fun extractToken(request: ServerHttpRequest): String? {
        // Try query parameter first
        if (request is ServletServerHttpRequest) {
            val token = request.servletRequest.getParameter("token")
            if (!token.isNullOrBlank()) {
                return token
            }
        }

        // Try Authorization header
        val authHeader = request.headers.getFirst("Authorization")
        if (authHeader?.startsWith("Bearer ") == true) {
            return authHeader.substring(7)
        }

        return null
    }
}

/**
 * ChannelInterceptor to set tenant and device context per message
 */
class WebSocketChannelInterceptor(
    private val jwtService: JwtService
) : ChannelInterceptor {

    private val logger = LoggerFactory.getLogger(WebSocketChannelInterceptor::class.java)

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
            ?: return message

        when (accessor.command) {
            StompCommand.CONNECT -> handleConnect(accessor)
            StompCommand.SUBSCRIBE -> handleSubscribe(accessor)
            StompCommand.SEND -> handleSend(accessor)
            else -> {}
        }

        return message
    }

    private fun handleConnect(accessor: StompHeaderAccessor) {
        val sessionAttributes = accessor.sessionAttributes ?: return

        val userId = sessionAttributes["userId"] as? String
        val tenantId = sessionAttributes["tenantId"] as? String
        val deviceId = sessionAttributes["deviceId"] as? String

        logger.debug("WebSocket CONNECT: user={}, tenant={}, device={}", userId, tenantId, deviceId)

        // Set context
        tenantId?.let { TenantContextHolder.setCurrentTenant(it) }
        deviceId?.let { DeviceContextHolder.setCurrentDevice(it) }

        // Set user principal
        userId?.let { accessor.user = SimplePrincipal(it) }
    }

    private fun handleSubscribe(accessor: StompHeaderAccessor) {
        val destination = accessor.destination ?: return
        val sessionAttributes = accessor.sessionAttributes ?: return

        val userId = sessionAttributes["userId"] as? String
        val tenantId = sessionAttributes["tenantId"] as? String
        val deviceId = sessionAttributes["deviceId"] as? String

        logger.debug(
            "WebSocket SUBSCRIBE: destination={}, user={}, tenant={}, device={}",
            destination, userId, tenantId, deviceId
        )

        // Validate workspace access
        if (destination.startsWith("/topic/workspace/")) {
            val workspaceId = destination.split("/").getOrNull(3)

            // Ensure user can only subscribe to their own workspace
            if (workspaceId != null && workspaceId != tenantId) {
                logger.warn(
                    "Access denied: User {} attempted to subscribe to workspace {}",
                    userId, workspaceId
                )
                throw SecurityException("Access denied to workspace")
            }
        }

        // Set context for this message
        tenantId?.let { TenantContextHolder.setCurrentTenant(it) }
        deviceId?.let { DeviceContextHolder.setCurrentDevice(it) }
    }

    private fun handleSend(accessor: StompHeaderAccessor) {
        val sessionAttributes = accessor.sessionAttributes ?: return

        val tenantId = sessionAttributes["tenantId"] as? String
        val deviceId = sessionAttributes["deviceId"] as? String

        // Set context for this message
        tenantId?.let { TenantContextHolder.setCurrentTenant(it) }
        deviceId?.let { DeviceContextHolder.setCurrentDevice(it) }
    }
}

/**
 * Simple Principal implementation for WebSocket
 */
class SimplePrincipal(private val name: String) : Principal {
    override fun getName(): String = name
}
