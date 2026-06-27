package com.ampairs.notification.service

import com.ampairs.core.sync.EntityChangePublisher
import com.ampairs.notification.domain.dto.NotificationLogRequest
import com.ampairs.notification.model.NotificationLog
import com.ampairs.notification.repository.NotificationLogRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NotificationLogServiceTest {

    private val repository: NotificationLogRepository = mock()
    private val entityChangePublisher: EntityChangePublisher = mock()
    private val service = NotificationLogService(repository, entityChangePublisher)

    init {
        whenever(repository.save(any<NotificationLog>())).thenAnswer { it.arguments[0] }
    }

    private fun existing(uid: String, read: Boolean = false, active: Boolean = true) =
        NotificationLog().apply {
            this.uid = uid
            type = "order_update"
            title = "Order"
            body = "A new order"
            this.read = read
            this.active = active
        }

    @Test
    fun `bulkUpsert applies read and active flags to an existing notification`() {
        whenever(repository.findByUid("NL-1")).thenReturn(existing("NL-1", read = false, active = true))

        val result = service.bulkUpsert(
            listOf(NotificationLogRequest(id = "NL-1", read = true, active = false))
        )

        assertEquals(1, result.size)
        assertTrue(result[0].read)
        assertEquals(false, result[0].active)
        verify(entityChangePublisher).updated(eq("notification_log"), eq("NL-1"))
        verify(entityChangePublisher, never()).created(any(), any())
    }

    @Test
    fun `bulkUpsert creates a row when the uid is unknown`() {
        whenever(repository.findByUid("NL-NEW")).thenReturn(null)

        val result = service.bulkUpsert(
            listOf(NotificationLogRequest(id = "NL-NEW", type = "system_announcement", title = "Hi", body = "There"))
        )

        assertEquals("NL-NEW", result[0].id)
        assertEquals("system_announcement", result[0].type)
        verify(entityChangePublisher).created(eq("notification_log"), eq("NL-NEW"))
    }

    @Test
    fun `markRead flips an unread notification and signals the change`() {
        whenever(repository.findByUid("NL-1")).thenReturn(existing("NL-1", read = false))

        service.markRead("NL-1")

        val saved = argumentCaptorSave()
        assertTrue(saved.read)
        verify(entityChangePublisher).updated(eq("notification_log"), eq("NL-1"))
    }

    @Test
    fun `markRead is a no-op when already read`() {
        whenever(repository.findByUid("NL-1")).thenReturn(existing("NL-1", read = true))

        service.markRead("NL-1")

        verify(repository, never()).save(any<NotificationLog>())
        verify(entityChangePublisher, never()).updated(any(), any())
    }

    private fun argumentCaptorSave(): NotificationLog {
        val captor = org.mockito.kotlin.argumentCaptor<NotificationLog>()
        verify(repository).save(captor.capture())
        return captor.firstValue
    }
}
