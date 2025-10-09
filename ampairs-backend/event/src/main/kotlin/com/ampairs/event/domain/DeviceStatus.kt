package com.ampairs.event.domain

enum class DeviceStatus {
    ONLINE,   // Active WebSocket connection, recent heartbeat
    AWAY,     // Connected but no activity for 30s
    OFFLINE   // Disconnected or stale (2min no heartbeat)
}
