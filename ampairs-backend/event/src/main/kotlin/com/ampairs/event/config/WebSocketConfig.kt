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
import java.net.InetSocketAddress
import java.net.Socket
import java.security.Principal

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val jwtService: JwtService
) : WebSocketMessageBrokerConfigurer {

    private val logger = LoggerFactory.getLogger(WebSocketConfig::class.java)

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        val relayHost = System.getenv("RABBITMQ_HOST") ?: "localhost"
        val relayPort = (System.getenv("RABBITMQ_STOMP_PORT") ?: "61613").toInt()
        val useSimpleBroker = System.getenv("WEBSOCKET_SIMPLE_BROKER")?.toBooleanStrictOrNull()
            ?: !isBrokerAvailable(relayHost, relayPort)

        if (useSimpleBroker) {
            logger.warn(
                "STOMP broker relay at {}:{} unavailable or disabled. Falling back to simple broker.",
                relayHost, relayPort
            )
            registry.enableSimpleBroker("/topic", "/queue")
        } else {
            logger.info("Using STOMP broker relay at {}:{}", relayHost, relayPort)
            registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin("${System.getenv("RABBITMQ_USER") ?: "guest"}")
                .setClientPasscode("${System.getenv("RABBITMQ_PASSWORD") ?: "guest"}")
                .setSystemLogin("${System.getenv("RABBITMQ_USER") ?: "guest"}")
                .setSystemPasscode("${System.getenv("RABBITMQ_PASSWORD") ?: "guest"}")
                .setVirtualHost("${System.getenv("RABBITMQ_VHOST") ?: "/"}")
        }

        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // Native WebSocket endpoint for clients that use standard WS (KMP, desktop, etc.)
        registry.addEndpoint("/ws")
            .addInterceptors(WebSocketAuthInterceptor(jwtService))
            .setAllowedOriginPatterns("*")

        // SockJS fallback endpoint for legacy browsers
        registry.addEndpoint("/ws")
            .addInterceptors(WebSocketAuthInterceptor(jwtService))
            .setAllowedOriginPatterns("*")
            .withSockJS()
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(WebSocketChannelInterceptor(jwtService))
    }

    private fun isBrokerAvailable(host: String, port: Int): Boolean {
        return runCatching {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 500)
            }
        }.onFailure {
            logger.debug("STOMP broker probe failed for {}:{} -> {}", host, port, it.message)
        }.isSuccess
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
            var tenantId = jwtService.extractTenantId(token)
            val deviceId = jwtService.extractDeviceId(token)

            // Allow workspace to be provided explicitly when not embedded in token (e.g., mobile clients)
            val workspaceFromRequest = resolveWorkspaceId(request)
            if (tenantId.isNullOrBlank() && !workspaceFromRequest.isNullOrBlank()) {
                tenantId = workspaceFromRequest
            } else if (!tenantId.isNullOrBlank() && !workspaceFromRequest.isNullOrBlank() && tenantId != workspaceFromRequest) {
                logger.warn(
                    "Workspace mismatch: token tenant {} differs from requested workspace {}",
                    tenantId, workspaceFromRequest
                )
            }

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
            workspaceFromRequest?.let { attributes["workspaceId"] = it }

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

    private fun resolveWorkspaceId(request: ServerHttpRequest): String? {
        if (request is ServletServerHttpRequest) {
            request.servletRequest.getParameter("workspaceId")
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }

        return request.headers.getFirst("X-Workspace-ID")?.takeIf { it.isNotBlank() }
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
        val workspaceId = sessionAttributes["workspaceId"] as? String
        val deviceId = sessionAttributes["deviceId"] as? String

        logger.debug("WebSocket CONNECT: user={}, tenant={}, device={}", userId, tenantId, deviceId)

        // Set context
        val effectiveTenant = tenantId?.takeIf { it.isNotBlank() } ?: workspaceId
        effectiveTenant?.let { TenantContextHolder.setCurrentTenant(it) }
        deviceId?.let { DeviceContextHolder.setCurrentDevice(it) }

        // Set user principal
        userId?.let { accessor.user = SimplePrincipal(it) }
    }

    private fun handleSubscribe(accessor: StompHeaderAccessor) {
        val destination = accessor.destination ?: return
        val sessionAttributes = accessor.sessionAttributes ?: return

        val userId = sessionAttributes["userId"] as? String
        val tenantId = sessionAttributes["tenantId"] as? String
        val workspaceAttr = sessionAttributes["workspaceId"] as? String
        val deviceId = sessionAttributes["deviceId"] as? String

        logger.debug(
            "WebSocket SUBSCRIBE: destination={}, user={}, tenant={}, device={}",
            destination, userId, tenantId ?: workspaceAttr, deviceId
        )

        // Validate workspace access
        extractWorkspaceId(destination)?.let { workspaceId ->
            if (!tenantId.isNullOrBlank() && workspaceId != tenantId) {
                logger.warn(
                    "Access denied: User {} attempted to subscribe to workspace {}",
                    userId, workspaceId
                )
                throw SecurityException("Access denied to workspace")
            }

            // Remember resolved workspace for sessions where tenant is inferred from destination
            sessionAttributes["workspaceId"] = workspaceId
            if (tenantId.isNullOrBlank()) {
                TenantContextHolder.setCurrentTenant(workspaceId)
            }
        }

        // Set context for this message
        val effectiveTenant = tenantId?.takeIf { it.isNotBlank() } ?: (sessionAttributes["workspaceId"] as? String)
        effectiveTenant?.let { TenantContextHolder.setCurrentTenant(it) }
        deviceId?.let { DeviceContextHolder.setCurrentDevice(it) }
    }

    private fun handleSend(accessor: StompHeaderAccessor) {
        val sessionAttributes = accessor.sessionAttributes ?: return

        val tenantId = sessionAttributes["tenantId"] as? String
        val workspaceId = sessionAttributes["workspaceId"] as? String
        val deviceId = sessionAttributes["deviceId"] as? String

        // Set context for this message
        val effectiveTenant = tenantId?.takeIf { it.isNotBlank() } ?: workspaceId
        effectiveTenant?.let { TenantContextHolder.setCurrentTenant(it) }
        deviceId?.let { DeviceContextHolder.setCurrentDevice(it) }
    }

    private fun extractWorkspaceId(destination: String): String? {
        return when {
            destination.startsWith(Constants.WORKSPACE_EVENTS_TOPIC_PREFIX) ->
                destination.removePrefix(Constants.WORKSPACE_EVENTS_TOPIC_PREFIX)
            destination.startsWith(Constants.WORKSPACE_STATUS_TOPIC_PREFIX) ->
                destination.removePrefix(Constants.WORKSPACE_STATUS_TOPIC_PREFIX)
            else -> null
        }
    }
}

/**
 * Simple Principal implementation for WebSocket
 */
class SimplePrincipal(private val name: String) : Principal {
    override fun getName(): String = name
}
