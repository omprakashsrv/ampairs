package com.ampairs.event.config

interface Constants {
    companion object {
        const val ID_LENGTH = 34
        const val WORKSPACE_EVENT_PREFIX = "WEV"
        const val DEVICE_SESSION_PREFIX = "DSS"
        const val WORKSPACE_EVENTS_TOPIC_PREFIX = "/topic/workspace.events."
        const val WORKSPACE_STATUS_TOPIC_PREFIX = "/topic/workspace.status."
    }
}
