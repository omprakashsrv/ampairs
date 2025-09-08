package com.ampairs.notification.service

import com.ampairs.notification.model.NotificationQueue
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.provider.NotificationResult
import com.ampairs.notification.provider.NotificationStatus
import com.ampairs.notification.provider.sms.AwsSnsSmsProvider
import com.ampairs.notification.provider.sms.Msg91SmsProvider
import com.ampairs.notification.repository.NotificationQueueRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.Executor

@TestPropertySource(
    properties = [
        "notification.sms.primary-provider=MSG91",
        "notification.batch-size=5",
        "notification.retry-delay-minutes=2"
    ]
)
class NotificationServiceTest {

    @Mock
    private lateinit var notificationQueueRepository: NotificationQueueRepository

    @Mock
    private lateinit var msg91SmsProvider: Msg91SmsProvider

    @Mock
    private lateinit var awsSnsSmsProvider: AwsSnsSmsProvider

    @Mock
    private lateinit var taskExecutor: Executor

    @Mock
    private lateinit var notificationDatabaseService: NotificationDatabaseService

    private lateinit var notificationService: NotificationService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        notificationService = NotificationService(
            notificationQueueRepository,
            msg91SmsProvider,
            awsSnsSmsProvider,
            taskExecutor,
            notificationDatabaseService
        )
    }

    @Test
    fun `should queue notification successfully`() {
        // Given
        val recipient = "+919876543210"
        val message = "Test OTP: 123456"
        val notificationQueue = NotificationQueue().apply {
            uid = "test-id-123"
            this.recipient = recipient
            this.message = message
            this.channel = NotificationChannel.SMS
            this.status = NotificationStatus.PENDING
        }

        `when`(notificationQueueRepository.save(any(NotificationQueue::class.java))).thenReturn(notificationQueue)

        // When
        val result = notificationService.queueNotification(recipient, message, NotificationChannel.SMS)

        // Then
        assertEquals("test-id-123", result)
        verify(notificationQueueRepository, times(1)).save(any(NotificationQueue::class.java))
    }

    @Test
    fun `should queue SMS successfully using legacy method`() {
        // Given
        val phoneNumber = "+919876543210"
        val message = "Test OTP: 123456"
        val notificationQueue = NotificationQueue().apply {
            uid = "test-id-123"
            this.recipient = phoneNumber
            this.message = message
            this.channel = NotificationChannel.SMS
            this.status = NotificationStatus.PENDING
        }

        `when`(notificationQueueRepository.save(any(NotificationQueue::class.java))).thenReturn(notificationQueue)

        // When
        val result = notificationService.queueSms(phoneNumber, message)

        // Then
        assertEquals("test-id-123", result)
        verify(notificationQueueRepository, times(1)).save(any(NotificationQueue::class.java))
    }

    @Test
    fun `should process SMS notification with primary provider success`() {
        // Given
        val notificationQueue = NotificationQueue().apply {
            uid = "test-id-123"
            recipient = "+919876543210"
            message = "Test OTP: 123456"
            channel = NotificationChannel.SMS
            status = NotificationStatus.PENDING
        }

        val successResult = NotificationResult(
            success = true,
            messageId = "msg-123",
            providerName = "MSG91",
            channel = NotificationChannel.SMS
        )

        `when`(msg91SmsProvider.isAvailable()).thenReturn(true)
        `when`(msg91SmsProvider.sendNotification(anyString(), anyString())).thenReturn(successResult)
        `when`(notificationQueueRepository.save(any(NotificationQueue::class.java))).thenReturn(notificationQueue)

        // When
        notificationService.processSingleNotification(notificationQueue)

        // Then
        verify(msg91SmsProvider, times(1)).sendNotification("+919876543210", "Test OTP: 123456")
        verify(notificationDatabaseService, times(1)).updateNotificationAsSent(eq(notificationQueue), any(NotificationResult::class.java))
    }

    @Test
    fun `should fallback to secondary provider when primary fails`() {
        // Given
        val notificationQueue = NotificationQueue().apply {
            uid = "test-id-123"
            recipient = "+919876543210"
            message = "Test OTP: 123456"
            channel = NotificationChannel.SMS
            status = NotificationStatus.PENDING
        }

        val failedResult = NotificationResult(
            success = false,
            errorMessage = "MSG91 API error",
            providerName = "MSG91",
            channel = NotificationChannel.SMS
        )

        val successResult = NotificationResult(
            success = true,
            messageId = "sns-123",
            providerName = "AWS_SNS",
            channel = NotificationChannel.SMS
        )

        `when`(msg91SmsProvider.isAvailable()).thenReturn(true)
        `when`(msg91SmsProvider.sendNotification(anyString(), anyString())).thenReturn(failedResult)
        `when`(awsSnsSmsProvider.isAvailable()).thenReturn(true)
        `when`(awsSnsSmsProvider.sendNotification(anyString(), anyString())).thenReturn(successResult)
        `when`(notificationQueueRepository.save(any(NotificationQueue::class.java))).thenReturn(notificationQueue)

        // When
        notificationService.processSingleNotification(notificationQueue)

        // Then
        verify(msg91SmsProvider, times(1)).sendNotification("+919876543210", "Test OTP: 123456")
        verify(awsSnsSmsProvider, times(1)).sendNotification("+919876543210", "Test OTP: 123456")
        verify(notificationDatabaseService, times(1)).updateNotificationAsSent(eq(notificationQueue), any(NotificationResult::class.java))
    }

    @Test
    fun `should mark notification as failed when all providers fail`() {
        // Given
        val notificationQueue = NotificationQueue().apply {
            uid = "test-id-123"
            recipient = "+919876543210"
            message = "Test OTP: 123456"
            channel = NotificationChannel.SMS
            status = NotificationStatus.PENDING
        }

        val failedResult1 = NotificationResult(
            success = false,
            errorMessage = "MSG91 API error",
            providerName = "MSG91",
            channel = NotificationChannel.SMS
        )

        val failedResult2 = NotificationResult(
            success = false,
            errorMessage = "AWS SNS error",
            providerName = "AWS_SNS",
            channel = NotificationChannel.SMS
        )

        `when`(msg91SmsProvider.isAvailable()).thenReturn(true)
        `when`(msg91SmsProvider.sendNotification(anyString(), anyString())).thenReturn(failedResult1)
        `when`(awsSnsSmsProvider.isAvailable()).thenReturn(true)
        `when`(awsSnsSmsProvider.sendNotification(anyString(), anyString())).thenReturn(failedResult2)
        `when`(notificationQueueRepository.save(any(NotificationQueue::class.java))).thenReturn(notificationQueue)

        // When
        notificationService.processSingleNotification(notificationQueue)

        // Then
        verify(msg91SmsProvider, times(1)).sendNotification("+919876543210", "Test OTP: 123456")
        verify(awsSnsSmsProvider, times(1)).sendNotification("+919876543210", "Test OTP: 123456")
        verify(notificationDatabaseService, times(1)).updateNotificationAsFailed(eq(notificationQueue), eq("AWS_SNS"), eq("AWS SNS error"), isNull())
    }

    @Test
    fun `should get SMS statistics`() {
        // Given
        `when`(
            notificationQueueRepository.countByStatusInAndChannel(
                listOf(NotificationStatus.PENDING),
                NotificationChannel.SMS
            )
        ).thenReturn(5L)
        `when`(
            notificationQueueRepository.countByStatusInAndChannel(
                listOf(NotificationStatus.SENT),
                NotificationChannel.SMS
            )
        ).thenReturn(100L)
        `when`(
            notificationQueueRepository.countByStatusInAndChannel(
                listOf(NotificationStatus.FAILED),
                NotificationChannel.SMS
            )
        ).thenReturn(2L)

        // When
        val stats = notificationService.getSmsStatistics()

        // Then
        assertEquals(5L, stats["pending"])
        assertEquals(100L, stats["sent"])
        assertEquals(2L, stats["failed"])
    }

    @Test
    fun `should get notification statistics by channel`() {
        // Given
        `when`(notificationQueueRepository.countByStatusIn(listOf(NotificationStatus.PENDING))).thenReturn(10L)
        `when`(notificationQueueRepository.countByStatusIn(listOf(NotificationStatus.SENT))).thenReturn(200L)
        `when`(
            notificationQueueRepository.countByStatusInAndChannel(
                listOf(NotificationStatus.PENDING),
                NotificationChannel.SMS
            )
        ).thenReturn(5L)
        `when`(
            notificationQueueRepository.countByStatusInAndChannel(
                listOf(NotificationStatus.SENT),
                NotificationChannel.SMS
            )
        ).thenReturn(100L)
        `when`(
            notificationQueueRepository.countByStatusInAndChannel(
                listOf(NotificationStatus.FAILED),
                NotificationChannel.SMS
            )
        ).thenReturn(2L)

        // When
        val stats = notificationService.getNotificationStatistics()

        // Then
        val overallStats = stats["overall"] as Map<String, Long>
        val channelStats = stats["byChannel"] as Map<NotificationChannel, Map<String, Long>>

        assertEquals(10L, overallStats["pending"])
        assertEquals(200L, overallStats["sent"])
        assertEquals(5L, channelStats[NotificationChannel.SMS]!!["pending"])
        assertEquals(100L, channelStats[NotificationChannel.SMS]!!["sent"])
    }
}