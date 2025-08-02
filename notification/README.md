# Notification Module

The Notification module provides multi-channel communication capabilities with asynchronous processing, multiple
provider support, automatic failover, and retry mechanisms for the Ampairs application.

## Overview

This module handles all notification functionality across multiple channels including:

- **Multi-Channel Support**: SMS, Email, WhatsApp, Push Notifications (extensible)
- **Async Processing**: Non-blocking notification sending with queue-based processing
- **Multiple Providers**: Support for different providers per channel with failover
- **Automatic Failover**: Seamless switching between providers on failure
- **Retry Mechanism**: Intelligent retry with exponential backoff
- **Queue Management**: Database-backed notification queue with persistence
- **Monitoring**: Real-time statistics and health endpoints

## Architecture

### Package Structure

```
com.ampairs.notification/
├── controller/          # REST endpoints for notification operations
├── service/            # Business logic and async processing
├── provider/           # Provider implementations by channel
│   └── sms/           # SMS-specific providers
├── model/              # Entity definitions
└── repository/         # Data access layer
```

### Key Components

#### Notification Providers

- **NotificationProvider Interface**: Base interface for all notification providers
- **SmsNotificationProvider Interface**: SMS-specific provider contract
- **Msg91SmsProvider**: Primary SMS provider using MSG91 API
- **AwsSnsSmsProvider**: Fallback SMS provider using AWS SNS

#### Async Processing

- **NotificationService**: Main service with scheduler-based processing and parallel execution
- **NotificationDatabaseService**: Dedicated service for isolated database transactions
- **NotificationQueue Entity**: Database persistence for notification requests
- **NotificationQueueRepository**: Data access for notification queue operations

#### Monitoring

- **NotificationController**: REST endpoints for statistics and testing

## Features

### 🔄 **Multi-Channel Support**

- **SMS**: MSG91 (primary), AWS SNS (fallback)
- **Email**: Future support for SMTP, AWS SES, SendGrid
- **WhatsApp**: Future support for Twilio, Meta Business API
- **Push Notifications**: Future support for FCM, APNS

### 🔀 **Provider Failover**

- Channel-specific provider chains
- Automatic failover on provider failure
- Configurable provider priorities

### 🔁 **Retry Mechanism**

- Configurable retry attempts (default: 3)
- Exponential backoff delay
- Failed message tracking with exhaustion handling

### 📊 **Queue Management**

- Database persistence with MySQL
- Status tracking (PENDING, SENT, FAILED, RETRYING, EXHAUSTED)
- Channel-aware processing
- Automatic cleanup of old records

### 📈 **Monitoring & Statistics**

- Overall and channel-specific statistics
- Provider performance tracking
- Test notification functionality
- Health check endpoints

## Configuration

### Application Properties

```yaml
# Notification Configuration
notification:
  # Batch processing settings
  batch-size: 10
  retry-delay-minutes: 5
  cleanup-days: 30
  
  # Parallel processing settings
  parallel-threads: 5
  thread-pool:
    queue-capacity: 100
    keep-alive-seconds: 60
  
  # SMS Configuration
  sms:
    primary-provider: MSG91
    msg91:
      enabled: true
      auth-key: ${MSG91_AUTH_KEY}
      template-id: ${MSG91_TEMPLATE_ID}
      sender-id: AMPAIR
      api-url: https://control.msg91.com/api/v5/otp
    aws-sns:
      enabled: true
  
  # Email Configuration (future)
  email:
    enabled: false
    primary-provider: SMTP
  
  # WhatsApp Configuration (future)
  whatsapp:
    enabled: false
    primary-provider: TWILIO
```

### Environment Variables

```bash
# MSG91 Configuration
MSG91_AUTH_KEY=your_msg91_auth_key
MSG91_TEMPLATE_ID=your_msg91_template_id

# Notification Settings  
NOTIFICATION_BATCH_SIZE=10
NOTIFICATION_RETRY_DELAY_MINUTES=5
NOTIFICATION_CLEANUP_DAYS=30

# Parallel Processing Settings
NOTIFICATION_PARALLEL_THREADS=5
NOTIFICATION_THREAD_POOL_QUEUE_CAPACITY=100
NOTIFICATION_THREAD_POOL_KEEP_ALIVE_SECONDS=60

# SMS Settings
SMS_PRIMARY_PROVIDER=MSG91
SMS_MSG91_ENABLED=true
SMS_AWS_SNS_ENABLED=true
```

## API Endpoints

### Notification Statistics

```http
GET /notification/v1/stats
```

**Response:**

```json
{
  "success": true,
  "data": {
    "overall": {
      "pending": 15,
      "retrying": 3,
      "sent": 2000,
      "failed": 25,
      "exhausted": 2
    },
    "byChannel": {
      "SMS": {
        "pending": 10,
        "sent": 1500,
        "failed": 20
      },
      "EMAIL": {
        "pending": 5,
        "sent": 500,
        "failed": 5
      }
    }
  },
  "message": "Notification statistics retrieved successfully"
}
```

### SMS Statistics (Legacy Support)

```http
GET /notification/v1/sms/stats
```

### Test Notification

```http
POST /notification/v1/test?recipient=+919876543210&message=Test&channel=SMS
```

**Response:**

```json
{
  "success": true,
  "data": {
    "notificationId": "uuid-generated-id",
    "channel": "SMS"
  },
  "message": "Test notification queued successfully"
}
```

### Test SMS (Legacy Support)

```http
POST /notification/v1/sms/test?phoneNumber=+919876543210&message=Test SMS
```

### Immediate Notification

```http
POST /notification/v1/send/immediate?recipient=user@example.com&message=Urgent&channel=EMAIL
```

## Usage

### Basic Notification Sending

```kotlin
@Autowired
private lateinit var notificationService: NotificationService

// Send SMS
val smsId = notificationService.sendNotification(
    recipient = "+919876543210",
    message = "Your OTP is 123456",
    channel = NotificationChannel.SMS
)

// Send Email (future)
val emailId = notificationService.sendNotification(
    recipient = "user@example.com",
    message = "Welcome to Ampairs!",
    channel = NotificationChannel.EMAIL
)

// Queue with delay
val delayedId = notificationService.queueNotification(
    recipient = "+919876543210", 
    message = "Reminder: Your appointment is tomorrow",
    channel = NotificationChannel.SMS,
    delayMinutes = 30
)
```

### Legacy SMS Support

```kotlin
// Legacy SMS methods for backward compatibility
val smsId = notificationService.queueSms(
    phoneNumber = "+919876543210",
    message = "Your OTP is 123456"
)
```

### Immediate Notification (for urgent messages)

```kotlin
val result = notificationService.sendImmediateNotification(
    recipient = "+919876543210",
    message = "Emergency alert message",
    channel = NotificationChannel.SMS
)

if (result.success) {
    println("Notification sent via ${result.providerName}")
} else {
    println("Notification failed: ${result.errorMessage}")
}
```

### Integration with Auth Module

```kotlin
// In AuthService
@Autowired
private lateinit var notificationService: NotificationService

fun sendOtp(phoneNumber: String, otp: String) {
    notificationService.queueSms(
        phoneNumber,
        "$otp is your one time password to verify the phone number."
    )
}
```

## Data Models

### NotificationQueue Entity

```kotlin
@Entity
@Table(name = "notification_queue")
class NotificationQueue : OwnableBaseDomain() {
    var recipient: String = ""
    var message: String = ""
    var channel: NotificationChannel = NotificationChannel.SMS
    var status: NotificationStatus = NotificationStatus.PENDING
    var retryCount: Int = 0
    var maxRetries: Int = 3
    var scheduledAt: LocalDateTime = LocalDateTime.now()
    var lastAttemptAt: LocalDateTime? = null
    var providerUsed: String? = null
    var providerMessageId: String? = null
    var errorMessage: String? = null
    var providerResponse: String? = null
}
```

### Notification Channels

```kotlin
enum class NotificationChannel {
    SMS,                // SMS messages
    EMAIL,              // Email messages
    WHATSAPP,           // WhatsApp messages
    PUSH_NOTIFICATION,  // Mobile push notifications
    SLACK,              // Slack messages
    DISCORD             // Discord messages
}
```

### Notification Status

```kotlin
enum class NotificationStatus {
    PENDING,    // Queued for sending
    SENT,       // Successfully sent
    FAILED,     // Failed to send
    RETRYING,   // Marked for retry
    EXHAUSTED   // Max retries exceeded
}
```

## Scheduler Jobs

### Process Pending Notifications

- **Frequency**: Every 30 seconds
- **Function**: Processes pending notifications in parallel batches
- **Batch Size**: Configurable (default: 10)
- **Parallel Processing**: Configurable thread pool (default: 5 threads)
- **Performance**: Up to 5x faster processing with parallel execution

### Retry Failed Notifications

- **Frequency**: Every 5 minutes
- **Function**: Retries failed notifications in parallel
- **Retry Logic**: Exponential backoff with max attempts
- **Parallel Processing**: Same thread pool as pending notifications

### Cleanup Old Records

- **Frequency**: Daily at 2 AM
- **Function**: Removes old notification records (SENT/EXHAUSTED)
- **Retention**: Configurable (default: 30 days)

## Provider Details

### SMS Providers

#### MSG91 Provider

**Features:**

- Template-based OTP sending
- Indian SMS provider with competitive rates
- Real-time delivery reports
- OTP variable substitution

**Configuration:**

- `auth-key`: MSG91 authentication key
- `template-id`: Pre-approved OTP template ID
- `sender-id`: Sender ID for SMS (default: AMPAIR)

#### AWS SNS Provider

**Features:**

- Global SMS delivery via Amazon SNS
- Integration with existing AWS infrastructure
- 100 free SMS/month (AWS Free Tier)
- Reliable cloud-based delivery

**Configuration:**

- Uses existing AWS SNS configuration
- Leverages `SnsSmsTemplate` from Spring Cloud AWS

### Future Providers

#### Email Providers (Planned)

- **SMTP Provider**: Standard email via SMTP
- **AWS SES Provider**: Amazon Simple Email Service
- **SendGrid Provider**: SendGrid API integration

#### WhatsApp Providers (Planned)

- **Twilio WhatsApp Provider**: Twilio WhatsApp API
- **Meta Business API Provider**: Official WhatsApp Business API

## Error Handling

### Provider-Level Errors

- Network timeouts and connection failures
- API authentication errors
- Rate limiting and quota exceeded
- Invalid recipient formats
- Provider-specific error codes

### System-Level Errors

- Database connection issues
- Transaction failures
- Scheduler execution errors
- Configuration validation errors

### Error Recovery

- Automatic failover to secondary providers
- Retry with exponential backoff
- Dead letter queue for exhausted messages
- Comprehensive error logging and monitoring

## Testing

### Unit Tests

```bash
# Run notification module tests
./gradlew :notification:test
```

### Integration Testing

- Mock notification providers for testing
- Database integration with TestContainers
- Scheduler testing with test profiles
- End-to-end notification flow testing

### Test Endpoints

```bash
# Test SMS
curl -X POST "http://localhost:8080/notification/v1/sms/test?phoneNumber=+919876543210&message=Test SMS"

# Test any channel
curl -X POST "http://localhost:8080/notification/v1/test?recipient=user@example.com&message=Test&channel=EMAIL"

# Send immediate notification
curl -X POST "http://localhost:8080/notification/v1/send/immediate?recipient=+919876543210&message=Urgent&channel=SMS"
```

## Monitoring & Observability

### Metrics

- Notification queue size and processing rate
- Provider success/failure rates by channel
- Average processing time per channel
- Retry attempt statistics
- Channel usage distribution

### Logging

- Structured logging with correlation IDs
- Provider-specific log messages
- Channel-aware error tracking
- Performance metrics logging

### Health Checks

- Provider availability checks by channel
- Database connectivity
- Queue processing status
- Configuration validation

## Dependencies

### Core Dependencies

- **Spring Boot 3.5.3**: Core framework
- **Spring Data JPA**: Database access
- **Spring Scheduling**: Async task execution
- **Jackson**: JSON processing

### Provider Dependencies

- **AWS Spring Cloud**: SNS integration
- **RestTemplate**: HTTP client for MSG91
- **MySQL Connector**: Database driver

### Testing Dependencies

- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework
- **TestContainers**: Integration testing

## Security Considerations

### Data Protection

- Notification content encryption at rest (planned)
- Recipient information protection
- Audit trail for all notification operations
- Configurable data retention policies

### API Security

- Authentication required for test endpoints
- Rate limiting on test notification functionality
- Audit logging for all operations
- Channel-specific access controls

### Provider Security

- Secure API key management
- HTTPS-only communication
- Token-based authentication where available
- Regular credential rotation recommended

## Performance Optimization

### Parallel Processing

- **Multi-threaded execution**: Configurable thread pool for concurrent notification processing
- **Default configuration**: 5 parallel threads with queue capacity of 100
- **Performance gain**: Up to 5x faster processing compared to sequential execution
- **Thread pool management**: Automatic scaling and thread lifecycle management
- **Backpressure handling**: Graceful degradation when queue is full (executes in caller thread)

### Transaction Optimization

- **Separated transactions**: Database operations isolated from external API calls
- **Short-lived transactions**: Database updates happen in quick, separate transactions
- **Connection pool efficiency**: No database connections held during API latency
- **Dedicated database service**: `NotificationDatabaseService` handles all transactional operations
- **Improved throughput**: System can handle more concurrent operations without connection exhaustion

### Batch Processing

- Configurable batch sizes for queue processing
- Parallel processing within batches
- Channel-aware batch optimization
- Optimal database query patterns

### Caching

- Provider availability caching
- Configuration value caching
- Connection pooling for HTTP clients
- Channel routing cache

### Database Optimization

- Proper indexing on query columns
- Partitioning for large notification volumes
- Automated cleanup of old records
- Channel-specific query optimization

## Backward Compatibility

### Legacy SMS Support

The module maintains full backward compatibility with the previous SMS-only implementation:

```kotlin
// Legacy aliases available
typealias SmsQueue = NotificationQueue
typealias SmsQueueRepository = NotificationQueueRepository
typealias AsyncSmsService = NotificationService
typealias SmsResult = NotificationResult

// Legacy methods available
notificationService.queueSms(phoneNumber, message)
notificationService.getSmsStatistics()
notificationService.sendImmediateSms(phoneNumber, message)
```

### Migration Path

Existing code using the old SMS module will continue to work without changes:

- All SMS methods are still available
- SMS endpoints remain functional
- Configuration properties work with new structure
- Database schema is backward compatible

## Deployment

### Module Structure

The notification module is a library module (no main class) that gets included in the main application.

### Database Migration

```sql
-- Notification queue table replaces sms_queue
-- Auto-created via JPA/Hibernate with proper indexes:
CREATE INDEX idx_notification_queue_status_scheduled ON notification_queue(status, scheduled_at);
CREATE INDEX idx_notification_queue_channel_status ON notification_queue(channel, status);
CREATE INDEX idx_notification_queue_recipient_status ON notification_queue(recipient, status);
```

### Environment Setup

1. Configure notification providers per channel
2. Set up provider credentials (MSG91, AWS, etc.)
3. Configure environment variables
4. Deploy with main application

### Production Checklist

- [ ] MSG91 API credentials configured
- [ ] AWS SNS permissions set up
- [ ] Database indexes created
- [ ] Monitoring alerts configured
- [ ] Log aggregation set up
- [ ] Backup procedures in place
- [ ] Provider quotas and limits verified

## Troubleshooting

### Common Issues

**Notifications not being sent:**

1. Check provider configurations per channel
2. Verify recipient format for channel
3. Check API credentials and quotas
4. Review scheduler execution logs

**High retry rates:**

1. Monitor provider API status by channel
2. Check network connectivity
3. Verify rate limiting configurations
4. Review error logs for patterns

**Database performance:**

1. Check notification queue table size
2. Verify index usage
3. Monitor cleanup job execution
4. Consider partitioning for high volume

### Debug Commands

```bash
# Check notification statistics
curl http://localhost:8080/notification/v1/stats

# Check SMS-specific statistics
curl http://localhost:8080/notification/v1/sms/stats

# Send test notification
curl -X POST "http://localhost:8080/notification/v1/test?recipient=+919876543210&channel=SMS"

# Check application logs
tail -f logs/production.log | grep NOTIFICATION
```

## Roadmap

### Immediate (Current Release)

- ✅ **Multi-channel architecture**
- ✅ **SMS providers (MSG91, AWS SNS)**
- ✅ **Backward compatibility**
- ✅ **Enhanced monitoring**

### Short Term (Next 2-3 Releases)

- **Email providers**: SMTP, AWS SES, SendGrid
- **WhatsApp providers**: Twilio, Meta Business API
- **Push notification providers**: FCM, APNS
- **Template management system**

### Medium Term (Next 6 Months)

- **Multi-language notification support**
- **Advanced scheduling with cron expressions**
- **Delivery report tracking**
- **Cost optimization algorithms**
- **A/B testing for providers**

### Long Term (Next Year)

- **Real-time monitoring dashboard**
- **Machine learning for provider selection**
- **Advanced analytics and reporting**
- **Integration with external CRM systems**
- **White-label notification API**

## Migration from SMS Module

If you're migrating from the old SMS module, here's what you need to know:

### What Changed

- **Module name**: `sms` → `notification`
- **Package names**: `com.ampairs.sms` → `com.ampairs.notification`
- **Service architecture**: Enhanced to support multiple channels
- **Configuration structure**: Reorganized under `notification` prefix

### What Stayed the Same

- All SMS functionality works exactly the same
- Legacy method names and signatures preserved
- Database table structure compatible
- API endpoints still functional

### No Action Required

Your existing code will continue to work without any changes due to the comprehensive backward compatibility layer.

## Support

For questions or issues with the notification module:

1. Check the troubleshooting section above
2. Review application logs for error details
3. Test with the provided endpoints
4. Check provider status and quotas
5. Verify configuration settings

The notification module is designed to be robust, scalable, and easy to extend for future communication channels.