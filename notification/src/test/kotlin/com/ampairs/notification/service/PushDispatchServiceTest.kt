package com.ampairs.notification.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.notification.model.NotificationLog
import com.ampairs.notification.port.DevicePushTokenPort
import com.ampairs.notification.port.PushTarget
import com.ampairs.notification.repository.NotificationLogRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider

class PushDispatchServiceTest {

    private val notificationLogRepository: NotificationLogRepository = mock()
    private val notificationService: NotificationService = mock()
    private val port: DevicePushTokenPort = mock()
    private val portProvider: ObjectProvider<DevicePushTokenPort> = mock()

    private val service = PushDispatchService(notificationLogRepository, notificationService, portProvider)

    private fun stubSavedLog(uid: String = "NL-1") {
        whenever(notificationLogRepository.save(any<NotificationLog>()))
            .thenAnswer { (it.arguments[0] as NotificationLog).apply { this.uid = uid } }
    }

    @AfterEach
    fun tearDown() = TenantContextHolder.clearTenantContext()

    @Test
    fun `dispatch persists a NotificationLog and fans a push out per workspace token`() {
        stubSavedLog()
        whenever(portProvider.ifAvailable).thenReturn(port)
        whenever(port.tokensForWorkspace("ws-1")).thenReturn(
            listOf(
                PushTarget("tok-A", "FCM", "dev-A", "user-A"),
                PushTarget("tok-B", "FCM", "dev-B", "user-B"),
            )
        )

        val uid = service.dispatch(
            workspaceId = "ws-1",
            type = "system_announcement",
            title = "Hello",
            body = "World",
            data = mapOf("deep_link" to "ampairs://x"),
        )

        assertEquals("NL-1", uid)

        // The notification record carries the dispatched content + tenant.
        val logCaptor = argumentCaptor<NotificationLog>()
        verify(notificationLogRepository).save(logCaptor.capture())
        assertEquals("ws-1", logCaptor.firstValue.ownerId)
        assertEquals("system_announcement", logCaptor.firstValue.type)
        assertEquals("Hello", logCaptor.firstValue.title)

        // One push enqueued per resolved token, scoped to the workspace.
        verify(notificationService, times(2)).queuePush(any(), eq("Hello"), eq("World"), any(), eq("ws-1"))
        verify(notificationService).queuePush(eq("tok-A"), any(), any(), any(), eq("ws-1"))
        verify(notificationService).queuePush(eq("tok-B"), any(), any(), any(), eq("ws-1"))
    }

    @Test
    fun `dispatch targets a single user when targetUserId is set`() {
        stubSavedLog()
        whenever(portProvider.ifAvailable).thenReturn(port)
        whenever(port.tokensForUser("user-Z")).thenReturn(listOf(PushTarget("tok-Z", "FCM", "dev-Z", "user-Z")))

        service.dispatch("ws-1", "workspace_invitation", "Joined", "X joined", targetUserId = "user-Z")

        verify(port).tokensForUser("user-Z")
        verify(port, never()).tokensForWorkspace(any())
        verify(notificationService).queuePush(eq("tok-Z"), any(), any(), any(), eq("ws-1"))
    }

    @Test
    fun `dispatch still persists the log but sends no push when no token port is available`() {
        stubSavedLog("NL-2")
        whenever(portProvider.ifAvailable).thenReturn(null)

        val uid = service.dispatch("ws-1", "system_announcement", "T", "B")

        assertEquals("NL-2", uid)
        verify(notificationLogRepository).save(any<NotificationLog>())
        verify(notificationService, never()).queuePush(any(), any(), any(), any(), anyOrNull())
    }
}
