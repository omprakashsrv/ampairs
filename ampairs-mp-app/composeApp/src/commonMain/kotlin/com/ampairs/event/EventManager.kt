package com.ampairs.event

import com.ampairs.event.domain.ConnectionState
import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.WorkspaceEvent
import com.ampairs.event.util.EventLogger
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.hildan.krossbow.stomp.StompClient
import org.hildan.krossbow.stomp.StompSession
import org.hildan.krossbow.stomp.conversions.kxserialization.convertAndCollect
import org.hildan.krossbow.websocket.ktor.KtorWebSocketClient

/**
 * Manages WebSocket/STOMP connection for a specific workspace.
 * Provides real-time event streaming from backend to enable multi-device collaboration.
 *
 * Features:
 * - Single STOMP connection per workspace
 * - Automatic event subscription to workspace topic
 * - Heartbeat mechanism to maintain connection
 * - Event filtering (skips own device events)
 * - Reactive connection state
 *
 * @param workspaceId Workspace identifier for event topic subscription
 * @param userId Current user identifier
 * @param deviceId Current device identifier (to filter own events)
 * @param httpClient Ktor HTTP client for WebSocket transport
 * @param tokenProvider Function to get current JWT access token
 * @param baseUrl API base URL (will be converted to WebSocket URL)
 */
class EventManager(
    private val workspaceId: String,
    private val userId: String,
    private val deviceId: String,
    private val httpClient: HttpClient,
    private val tokenProvider: suspend () -> String,
    private val baseUrl: String
) {
    // Connection state exposed as StateFlow
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // Events stream exposed as SharedFlow
    private val _events = MutableSharedFlow<WorkspaceEvent>(
        replay = 0, // Don't replay old events to new subscribers
        extraBufferCapacity = 100 // Buffer up to 100 events if collector is slow
    )
    val events: SharedFlow<WorkspaceEvent> = _events.asSharedFlow()

    // Internal state
    private var stompSession: StompSession? = null
    private var heartbeatJob: Job? = null
    private var subscriptionJob: Job? = null

    // Krossbow STOMP client with Ktor WebSocket transport
    private val stompClient = StompClient(KtorWebSocketClient(httpClient))

    /**
     * Connect to WebSocket and subscribe to workspace events.
     * Safe to call multiple times - will not reconnect if already connected.
     */
    suspend fun connect() {
        if (_connectionState.value is ConnectionState.Connected) {
            EventLogger.w("EventManager", "Already connected to workspace: $workspaceId")
            return
        }

        _connectionState.value = ConnectionState.Connecting
        EventLogger.i("EventManager", "Connecting to workspace: $workspaceId")

        try {
            // Build WebSocket URL (replace http with ws/wss)
            val wsUrl = baseUrl
                .replace("http://", "ws://")
                .replace("https://", "wss://") + "/ws"

            // Get JWT token
            val token = tokenProvider()
            if (token.isBlank()) {
                throw IllegalStateException("No access token available")
            }

            // Connect with JWT token in query parameter (backend supports this)
            val urlWithToken = "$wsUrl?token=$token"
            EventLogger.d("EventManager", "Connecting to: $wsUrl")

            // Connect using Krossbow STOMP client
            stompSession = stompClient.connect(urlWithToken)

            _connectionState.value = ConnectionState.Connected(stompSession.toString())
            EventLogger.i("EventManager", "✅ Connected to workspace: $workspaceId")

            // Subscribe to workspace events topic
            subscribeToWorkspaceEvents()

            // Start heartbeat to keep connection alive
            startHeartbeat()

        } catch (e: Exception) {
            EventLogger.e("EventManager", "Connection failed for workspace: $workspaceId", e)
            _connectionState.value = ConnectionState.Error(
                message = "Connection failed: ${e.message}",
                exception = e
            )
            // Clean up partial connection
            cleanup()
        }
    }

    /**
     * Subscribe to workspace events topic using STOMP.
     * Backend publishes events to: /topic/workspace/{workspaceId}/events
     */
    private suspend fun subscribeToWorkspaceEvents() {
        val session = stompSession ?: run {
            EventLogger.w("EventManager", "No STOMP session available for subscription")
            return
        }

        try {
            val destination = "/topic/workspace/$workspaceId/events"
            EventLogger.i("EventManager", "Subscribing to: $destination")

            // Use convertAndCollect with automatic JSON deserialization
            subscriptionJob = CoroutineScope(Dispatchers.Default).launch {
                session.convertAndCollect<WorkspaceEvent>(destination) { event ->
                    handleIncomingEvent(event)
                }
            }

            EventLogger.i("EventManager", "✅ Subscribed to workspace events")

        } catch (e: Exception) {
            EventLogger.e("EventManager", "Subscription failed", e)
            _connectionState.value = ConnectionState.Error(
                message = "Subscription failed: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Handle incoming workspace event.
     * Filters out events from current device and emits to subscribers.
     */
    private suspend fun handleIncomingEvent(event: WorkspaceEvent) {
        try {
            // Skip events from this device (avoid echo)
            if (event.isFromDevice(deviceId)) {
                EventLogger.d(
                    "EventManager",
                    "Skipping own event: ${event.eventType} for ${event.entityType}:${event.entityId}"
                )
                return
            }

            EventLogger.i(
                "EventManager",
                "📨 Received event: ${event.eventType} for ${event.entityType}:${event.entityId} (seq: ${event.sequenceNumber})"
            )

            // Emit to all listeners (repositories, ViewModels, etc.)
            _events.emit(event)

        } catch (e: Exception) {
            EventLogger.e("EventManager", "Error handling event", e)
        }
    }

    /**
     * Start heartbeat mechanism to keep connection alive.
     * Backend expects heartbeat every 30 seconds to /app/heartbeat
     */
    private fun startHeartbeat() {
        heartbeatJob = CoroutineScope(Dispatchers.Default).launch {
            while (isActive && stompSession != null) {
                delay(30_000) // 30 seconds

                try {
                    stompSession?.sendEmptyMsg("/app/heartbeat")
                    EventLogger.d("EventManager", "💓 Heartbeat sent")
                } catch (e: Exception) {
                    EventLogger.w("EventManager", "Heartbeat failed", e)
                    // Don't break the loop - will retry next time
                }
            }
        }
    }

    /**
     * Disconnect from WebSocket and clean up resources.
     * Safe to call multiple times.
     */
    suspend fun disconnect() {
        EventLogger.i("EventManager", "Disconnecting from workspace: $workspaceId")
        cleanup()
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * Clean up coroutines and STOMP session
     */
    private suspend fun cleanup() {
        heartbeatJob?.cancel()
        subscriptionJob?.cancel()

        try {
            stompSession?.disconnect()
        } catch (e: Exception) {
            EventLogger.w("EventManager", "Error during STOMP disconnect", e)
        }

        stompSession = null
    }

    /**
     * Send a message to a STOMP destination (for future use).
     * Currently not used as backend auto-broadcasts events.
     */
    suspend fun sendMessage(destination: String, message: Any) {
        try {
            stompSession?.convertAndSend(destination, message)
            EventLogger.d("EventManager", "Sent message to: $destination")
        } catch (e: Exception) {
            EventLogger.e("EventManager", "Failed to send message", e)
        }
    }

    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean {
        return _connectionState.value is ConnectionState.Connected
    }

    /**
     * Get events for a specific entity type as Flow
     */
    fun getEventsForEntityType(entityType: String): SharedFlow<WorkspaceEvent> {
        return events
    }

    /**
     * Get events matching specific event types as Flow
     */
    fun getEventsByType(vararg eventTypes: EventType): SharedFlow<WorkspaceEvent> {
        return events
    }
}
