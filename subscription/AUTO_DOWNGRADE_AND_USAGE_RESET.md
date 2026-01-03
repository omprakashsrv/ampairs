# Auto-Downgrade & Monthly Usage Reset Documentation

## Overview

This document explains how the subscription system handles payment failures and usage tracking:

1. **Auto-Downgrade to FREE**: No CANCELLED/EXPIRED states - always downgrade to FREE plan
2. **Monthly Usage Reset**: Automatic reset of monthly counters on 1st of each month

---

## 1. Auto-Downgrade to FREE Plan

### Problem Solved

**User Question**: "The current plan can't be in cancelled state, On none payment of plan. we auto switch to free plan and free plan restrictions are getting applied."

**Solution**: Subscriptions NEVER enter CANCELLED or EXPIRED states. Instead, they automatically downgrade to the FREE plan with all FREE plan restrictions immediately applied.

### How It Works

```
Payment Flow:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

ACTIVE (Paid Plan)
       ↓
Payment Fails (Attempt 1)
       ↓
PAST_DUE (Grace Period Starts)
       ↓
Payment Fails (Attempt 2)
       ↓
PAST_DUE (Still in Grace Period)
       ↓
Payment Fails (Attempt 3)
       ↓
AUTO-DOWNGRADE TO FREE PLAN
       ↓
ACTIVE (FREE Plan) ← No data loss, immediate restriction
```

### Implementation Details

**Service**: `SubscriptionDowngradeService`

**Key Methods**:

1. **`downgradeToFreePlan(workspaceId, reason)`**
   - Changes plan to FREE
   - Sets status to ACTIVE
   - Clears billing information
   - Preserves user data

2. **`handlePaymentFailure(workspaceId, failedPaymentCount)`**
   - Attempt 1: Mark as PAST_DUE
   - Attempts 2-3: Continue grace period
   - Attempt 3+: Downgrade to FREE

3. **`handleSubscriptionExpiry(workspaceId)`**
   - Checks if subscription period has ended
   - Auto-downgrades if not renewed

4. **`processSubscriptionDowngrades()`**
   - Batch processing for scheduled job
   - Returns count of downgrades processed

### Automatic Processing

**Scheduled Job**: Runs **every hour** at minute 0

```kotlin
@Scheduled(cron = "0 0 * * * *") // Every hour
fun processSubscriptionDowngrades()
```

**What it checks**:
- Subscriptions in PAST_DUE with 3+ failed payments
- Active subscriptions past their `currentPeriodEnd` date

### Example Scenario

**Workspace**: `WSP123`
**Current Plan**: PROFESSIONAL (₹500/month)
**Payment Due**: 2025-01-25

**Timeline**:
```
2025-01-25: Payment fails (Attempt 1)
            → Status: PAST_DUE
            → failedPaymentCount: 1

2025-01-26: Auto-retry fails (Attempt 2)
            → Status: PAST_DUE
            → failedPaymentCount: 2

2025-01-27: Auto-retry fails (Attempt 3)
            → Status: ACTIVE
            → Plan: FREE
            → Restrictions immediately applied:
              - Max customers: 50 (was unlimited)
              - Max products: 50 (was unlimited)
              - Max invoices/month: 20 (was unlimited)
              - No API access
              - No custom branding
```

### Benefits

✅ **No Data Loss**: User data preserved
✅ **Graceful Degradation**: 3-attempt grace period
✅ **Immediate Restriction**: FREE plan limits enforced
✅ **Simple State Model**: Only ACTIVE status with different plans
✅ **User Control**: Users can re-subscribe anytime

---

## 2. Monthly Usage Reset

### Problem Solved

**User Question**: "Do we have any usages reset on month basis?"

**Solution**: YES - Monthly usage counters automatically reset on the 1st of each month, while cumulative counts are carried forward.

### How It Works

**Two Types of Counters**:

| Counter Type | Reset Monthly? | Examples |
|--------------|----------------|----------|
| **Cumulative** | ❌ NO | Customers, Products, Members, Devices, Storage |
| **Monthly** | ✅ YES | Invoices, Orders, API calls, SMS, Emails |

### Reset Logic

**Scheduled Job**: Runs on **1st of every month at 00:05 AM**

```kotlin
@Scheduled(cron = "0 5 0 1 * *") // 1st of month at 00:05 AM
fun resetMonthlyUsageCounters()
```

**Process**:
1. Fetch usage metrics from previous month
2. For each workspace:
   - Create new UsageMetric for current month
   - **Carry forward**: customerCount, productCount, memberCount, deviceCount, storageUsedBytes
   - **Reset to 0**: invoiceCount, orderCount, apiCalls, smsCount, emailCount
   - Reset all limit exceeded flags

### Example

**Workspace**: `WSP123`
**Plan**: PROFESSIONAL
**Limits**:
- Customers: Unlimited
- Products: Unlimited
- Invoices/month: 1000

**December 2024 Usage**:
```
customerCount: 150          (cumulative)
productCount: 300           (cumulative)
invoiceCount: 850           (monthly - within limit)
orderCount: 420             (monthly)
apiCalls: 15,000            (monthly)
```

**January 2025 (After Reset)**:
```
customerCount: 150          ← Carried forward
productCount: 300           ← Carried forward
invoiceCount: 0             ← Reset
orderCount: 0               ← Reset
apiCalls: 0                 ← Reset
```

**February 2025 Usage**:
```
customerCount: 165          ← Incremented (15 new customers)
productCount: 320           ← Incremented (20 new products)
invoiceCount: 920           ← New invoices in February
orderCount: 380             ← New orders in February
apiCalls: 18,500            ← API calls in February
```

### Period-Based Tracking

**Database Structure**:
```sql
CREATE TABLE usage_metrics (
    workspace_id VARCHAR(200),
    period_year INT,        -- e.g., 2025
    period_month INT,       -- 1-12

    -- Cumulative counts
    customer_count INT,
    product_count INT,
    member_count INT,
    device_count INT,
    storage_used_bytes BIGINT,

    -- Monthly counts (reset each period)
    invoice_count INT,
    order_count INT,
    api_calls BIGINT,
    sms_count INT,
    email_count INT,

    UNIQUE(workspace_id, period_year, period_month)
);
```

**Key Points**:
- Each month gets a new row per workspace
- `period_year` + `period_month` = unique period identifier
- Unique constraint prevents duplicate metrics

### Limit Enforcement

**Plan Limits**:
```kotlin
maxCustomers: -1           // Unlimited (cumulative)
maxProducts: -1            // Unlimited (cumulative)
maxInvoicesPerMonth: 1000  // Monthly limit
```

**Enforcement Logic**:
- Cumulative limits checked against total count (all time)
- Monthly limits checked against current period count
- Resets give users fresh monthly quota

### Historical Data

**Cleanup Job**: Runs on **2nd of each month at 02:00 AM**

```kotlin
@Scheduled(cron = "0 0 2 2 * *") // 2nd of month at 02:00 AM
fun cleanupOldUsageMetrics()
```

**Retention**: Keeps last **12 months** of usage data
- Allows annual analytics
- Prevents unbounded growth
- Historical metrics for reports

---

## Configuration

### Enable/Disable Scheduled Jobs

**application.yml**:
```yaml
subscription:
  scheduled-jobs:
    enabled: true  # Set to false to disable all scheduled jobs
```

### Job Schedule Summary

| Job | Schedule | Purpose |
|-----|----------|---------|
| Subscription Downgrade | Hourly at :00 | Process expired/failed subscriptions |
| Monthly Usage Reset | 1st at 00:05 AM | Reset monthly counters |
| Renewal Reminders | Daily at 9:00 AM | Send expiry reminders (TODO) |
| Trial Expiry | Hourly at :15 | Expire trials (TODO) |
| Usage Cleanup | 2nd at 02:00 AM | Delete old usage metrics |

---

## API Impact

### Subscription Status

**Old Behavior** (incorrect):
```json
{
  "status": "CANCELLED",
  "planCode": "PROFESSIONAL"
}
```

**New Behavior** (correct):
```json
{
  "status": "ACTIVE",
  "planCode": "FREE",
  "isFree": true
}
```

### Usage Metrics

**Get Current Usage**: `GET /api/v1/subscription/usage`

**Response**:
```json
{
  "period": "2025-01",
  "customers": 150,      // Cumulative
  "products": 300,       // Cumulative
  "invoices": 45,        // Current month only
  "orders": 23,          // Current month only
  "apiCalls": 1250,      // Current month only
  "limits": {
    "customers": -1,          // Unlimited
    "products": -1,           // Unlimited
    "invoicesPerMonth": 1000  // Monthly limit
  }
}
```

---

## Testing

### Manual Testing

**1. Test Auto-Downgrade**:
```bash
# Simulate payment failure
curl -X POST /api/v1/subscription/test/fail-payment \
  -H "Authorization: Bearer TOKEN" \
  -d '{"workspaceId": "WSP123", "failureCount": 3}'

# Verify downgrade
curl /api/v1/subscription \
  -H "Authorization: Bearer TOKEN"

# Expected: status=ACTIVE, planCode=FREE
```

**2. Test Monthly Reset**:
```bash
# Manually trigger reset (admin endpoint)
curl -X POST /api/v1/admin/subscription/reset-usage \
  -H "Authorization: Bearer ADMIN_TOKEN"

# Check usage metrics
curl /api/v1/subscription/usage \
  -H "Authorization: Bearer TOKEN"

# Expected: monthly counts reset to 0
```

### Integration Tests

```kotlin
@Test
fun `should downgrade to FREE after 3 failed payments`() {
    // Given: Subscription in PAST_DUE
    subscription.status = SubscriptionStatus.PAST_DUE
    subscription.failedPaymentCount = 3

    // When: Downgrade job runs
    downgradeService.handlePaymentFailure(workspaceId, 3)

    // Then: Downgraded to FREE
    val result = subscriptionRepository.findByWorkspaceId(workspaceId)
    assertThat(result.planCode).isEqualTo("FREE")
    assertThat(result.status).isEqualTo(SubscriptionStatus.ACTIVE)
    assertThat(result.isFree).isTrue()
}

@Test
fun `should reset monthly counters on new period`() {
    // Given: Usage metric from previous month
    val previousMetric = createUsageMetric(
        invoiceCount = 50,
        customerCount = 100
    )

    // When: Monthly reset runs
    usageTrackingService.resetMonthlyCounters()

    // Then: Monthly counts reset, cumulative carried forward
    val currentMetric = getCurrentPeriodMetric(workspaceId)
    assertThat(currentMetric.invoiceCount).isEqualTo(0)
    assertThat(currentMetric.customerCount).isEqualTo(100)
}
```

---

## Monitoring

### Logs to Watch

**Downgrade Events**:
```
INFO  Downgraded workspace WSP123 to FREE plan. Reason: Payment failed after 3 attempts
ERROR Payment failed 3 times for workspace WSP123, downgraded to FREE plan
```

**Monthly Reset**:
```
INFO  Resetting monthly usage counters for 2025-02
INFO  Monthly usage counter reset completed: 127 workspaces processed
```

**Usage Cleanup**:
```
INFO  Deleting usage metrics older than 2024-02
INFO  Deleted 1523 old usage metric records
```

### Metrics to Track

- Number of downgrades per day/week
- Failed payment attempt distribution (1, 2, 3+ failures)
- Monthly usage reset execution time
- Workspaces approaching monthly limits

---

## FAQs

**Q: What happens to user data when downgraded to FREE?**
A: All data is preserved. Only new operations are restricted based on FREE plan limits.

**Q: Can users upgrade from FREE back to paid?**
A: Yes, anytime. They can re-subscribe through the app.

**Q: How many days grace period after payment failure?**
A: We allow 3 payment retry attempts. After that, immediate downgrade to FREE.

**Q: Are historical usage metrics deleted?**
A: Metrics older than 12 months are automatically deleted. Last 12 months retained for analytics.

**Q: Can monthly limits be exceeded temporarily?**
A: Yes, we allow soft limits. But restrictions prevent further operations after exceeding.

**Q: What if a scheduled job fails?**
A: Jobs run hourly/daily, so next execution will catch missed items. Errors are logged for monitoring.

---

## Future Enhancements (TODO)

1. **Renewal Reminders**: Email/notification 7/3/1 days before expiry
2. **Trial Expiry Handling**: Auto-charge or downgrade when trial ends
3. **Prorated Refunds**: Calculate unused amount for mid-cycle downgrades
4. **Usage Alerts**: Notify users at 80%/100% of monthly limits
5. **Admin Dashboard**: View downgrade trends and usage patterns
