package com.ampairs.communication.service.send

import com.ampairs.communication.domain.enums.Channel
import com.ampairs.communication.domain.enums.DeliveryStatus
import com.ampairs.communication.domain.enums.SkipReason
import com.ampairs.communication.domain.enums.TriggerType
import com.ampairs.communication.domain.model.CommunicationLog
import com.ampairs.communication.domain.model.CommunicationRequest
import com.ampairs.communication.domain.model.MessageTemplate
import com.ampairs.communication.domain.model.TemplateVariant
import com.ampairs.communication.port.Recipient
import com.ampairs.communication.repository.CommunicationLogRepository
import com.ampairs.communication.repository.CommunicationRequestRepository
import com.ampairs.communication.service.CommunicationConfigService
import com.ampairs.communication.service.consent.SuppressionService
import com.ampairs.communication.service.template.TemplateRenderer
import com.ampairs.notification.service.NotificationDispatchService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.ObjectProvider

class CommunicationDispatchServiceTest {

    private val requestRepo: CommunicationRequestRepository = mock()
    private val logRepo: CommunicationLogRepository = mock()
    private val configService: CommunicationConfigService = mock()
    private val suppressionService: SuppressionService = mock()
    private val notificationDispatch: NotificationDispatchService = mock()
    private val dispatchProvider: ObjectProvider<NotificationDispatchService> = mock()

    private val service = CommunicationDispatchService(
        requestRepo, logRepo, TemplateRenderer(), configService, suppressionService, dispatchProvider,
    )

    private val template = MessageTemplate().apply { uid = "CTPL1"; code = "INV"; category = "TRANSACTIONAL"; defaultLocale = "en" }
    private val emailVariant = TemplateVariant().apply { uid = "V1"; templateUid = "CTPL1"; channel = "EMAIL"; locale = "en"; subject = "Hi"; htmlBody = "<p>{{name}}</p>" }

    init {
        whenever(requestRepo.findByDedupKey(any())).thenReturn(null)
        whenever(requestRepo.save(any<CommunicationRequest>())).thenAnswer { it.arguments[0] }
        whenever(logRepo.save(any<CommunicationLog>())).thenAnswer { it.arguments[0] }
        whenever(dispatchProvider.ifAvailable).thenReturn(notificationDispatch)
        whenever(notificationDispatch.enqueue(any())).thenReturn("NQ1")
        whenever(suppressionService.isSuppressed(any(), any())).thenReturn(false)
    }

    private fun dispatch(recipient: Recipient) = service.dispatch(
        template, listOf(emailVariant), listOf(Channel.EMAIL), listOf(recipient),
        mapOf("name" to "Asha"), TriggerType.MANUAL, null, null,
    )

    @Test
    fun `valid recipient is enqueued and logged QUEUED`() {
        dispatch(Recipient(email = "a@b.com"))
        verify(notificationDispatch).enqueue(any())
        val captor = argumentCaptor<CommunicationLog>()
        verify(logRepo, org.mockito.kotlin.atLeastOnce()).save(captor.capture())
        assertEquals(DeliveryStatus.QUEUED.name, captor.lastValue.status)
        assertEquals("NQ1", captor.lastValue.notificationUid)
    }

    @Test
    fun `missing address is SKIPPED NO_ADDRESS and not enqueued`() {
        dispatch(Recipient(phone = "+91999")) // no email for the EMAIL channel
        verify(notificationDispatch, org.mockito.kotlin.never()).enqueue(any())
        val captor = argumentCaptor<CommunicationLog>()
        verify(logRepo).save(captor.capture())
        assertEquals(DeliveryStatus.SKIPPED.name, captor.firstValue.status)
        assertEquals(SkipReason.NO_ADDRESS.name, captor.firstValue.skipReason)
    }

    @Test
    fun `suppressed address is SKIPPED SUPPRESSED and not enqueued`() {
        whenever(suppressionService.isSuppressed(eq(Channel.EMAIL), eq("a@b.com"))).thenReturn(true)
        dispatch(Recipient(email = "a@b.com"))
        verify(notificationDispatch, org.mockito.kotlin.never()).enqueue(any())
        val captor = argumentCaptor<CommunicationLog>()
        verify(logRepo).save(captor.capture())
        assertEquals(SkipReason.SUPPRESSED.name, captor.firstValue.skipReason)
    }
}
