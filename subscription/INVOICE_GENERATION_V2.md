# Invoice Generation V2 - Improved with Reconciliation

## Overview

The improved invoice generation system addresses critical issues in the original implementation:

### ✅ **Fixed Issues:**

1. **❌ Old:** Single monthly job - if missed, invoices never generated
   - **✅ New:** Daily reconciliation job checks last 3 months

2. **❌ Old:** No idempotency - could create duplicate invoices
   - **✅ New:** `InvoiceGenerationLog` table prevents duplicates

3. **❌ Old:** No failure tracking - failed invoices lost forever
   - **✅ New:** Tracks failures with exponential backoff retry (5 attempts max)

4. **❌ Old:** Single transaction - one failure rolls back everything
   - **✅ New:** Per-subscription transactions - failures isolated

5. **❌ Old:** Payment link failures not tracked
   - **✅ New:** Separate payment processing status tracking

6. **❌ Old:** No way to see what failed and why
   - **✅ New:** Complete error tracking with stack traces

7. **❌ Old:** Invoice number generation not persisted (AtomicInteger resets)
   - **✅ New:** Database-based sequence with format `INV-YYYY-MM-NNNNNN`

---

## Architecture

### **Components:**

```
┌─────────────────────────────────────────────────────────────┐
│            Daily Reconciliation Job (2 AM UTC)              │
│                 @Scheduled(cron = "0 0 2 * * ?")            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │   1. Generate Missing Invoices          │
        │      - Current month                     │
        │      - Last 3 months (if missed)        │
        └─────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │   2. Retry Failed Generations           │
        │      - Exponential backoff               │
        │      - Max 5 attempts                    │
        └─────────────────────────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────┐
        │   3. Process Pending Payment Links      │
        │      - For successfully generated        │
        │      - Retry failed payment links        │
        └─────────────────────────────────────────┘
```

### **Per-Subscription Flow:**

```
For each subscription:
  │
  ├─► Check InvoiceGenerationLog (idempotency)
  │   └─► If exists → Skip
  │
  ├─► Create Log Entry (status: PENDING)
  │
  ├─► Mark as IN_PROGRESS
  │
  ├─► Try Generate Invoice
  │   ├─► Success
  │   │   ├─► Mark log as SUCCESS
  │   │   ├─► Store invoice_id & invoice_number
  │   │   └─► Process Payment
  │   │       ├─► Auto-charge configured?
  │   │       │   ├─► Yes → Try auto-charge
  │   │       │   │   ├─► Success → Mark AUTO_CHARGE_SUCCESS
  │   │       │   │   └─► Fail → Send payment link
  │   │       │   └─► No → Send payment link
  │   │       └─► Payment Link
  │   │           ├─► Success → Mark LINK_SENT
  │   │           └─► Fail → Mark LINK_FAILED (retry later)
  │   │
  │   └─► Failure
  │       ├─► Mark log as FAILED
  │       ├─► Store error message & stack trace
  │       ├─► Calculate next_retry_at (exponential backoff)
  │       └─► Set should_retry = true (if attempts < 5)
  │
  └─► Commit transaction (per subscription)
```

---

## Database Schema

### **invoice_generation_logs Table:**

```sql
CREATE TABLE invoice_generation_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uid VARCHAR(200) UNIQUE,

    -- Idempotency key
    workspace_id VARCHAR(200),
    billing_period_year INT,
    billing_period_month INT,
    UNIQUE (workspace_id, billing_period_year, billing_period_month),

    -- Status tracking
    status VARCHAR(20),  -- PENDING, IN_PROGRESS, SUCCESS, FAILED

    -- Result
    invoice_id BIGINT,
    invoice_number VARCHAR(50),

    -- Retry mechanism
    attempt_count INT DEFAULT 0,
    next_retry_at TIMESTAMP,
    should_retry BOOLEAN DEFAULT TRUE,

    -- Error tracking
    error_message TEXT,
    error_stack_trace TEXT,

    -- Payment processing
    payment_status VARCHAR(20),  -- NOT_STARTED, AUTO_CHARGING, LINK_SENT, etc.
    payment_link_sent_at TIMESTAMP,
    payment_link_error TEXT
)
```

---

## Retry Strategy

### **Exponential Backoff:**

| Attempt | Backoff Time | Total Time Since First Attempt |
|---------|-------------|--------------------------------|
| 1       | Immediate   | 0 minutes                      |
| 2       | 5 minutes   | 5 minutes                      |
| 3       | 15 minutes  | 20 minutes                     |
| 4       | 1 hour      | 1 hour 20 minutes              |
| 5       | 4 hours     | 5 hours 20 minutes             |
| 6       | 12 hours    | 17 hours 20 minutes            |

After 5 failed attempts, `should_retry` is set to `false` and manual intervention required.

---

## Reconciliation Logic

### **Daily Job Checks:**

1. **Current Month (if after 1st):**
   - Check all ACTIVE POSTPAID subscriptions
   - Generate invoices for those without a log entry

2. **Last 3 Months (missed invoices):**
   - Same logic for each of the previous 3 months
   - Catches missed schedules or newly added POSTPAID subscriptions

3. **Failed Invoices:**
   - Query logs with `status = FAILED` and `next_retry_at < now()`
   - Retry with same logic

4. **Pending Payment Links:**
   - Query logs with `status = SUCCESS` but `payment_status != LINK_SENT`
   - Retry payment link generation

---

## Idempotency Guarantees

### **How Duplicate Prevention Works:**

```kotlin
// Step 1: Check if log exists (unique constraint on workspace_id + year + month)
val existingLog = invoiceGenerationLogRepository
    .findByWorkspaceIdAndBillingPeriodYearAndBillingPeriodMonth(
        workspaceId, year, month
    )

if (existingLog != null) {
    // Already attempted or completed - skip
    return
}

// Step 2: Create log entry FIRST (establishes lock)
val log = InvoiceGenerationLog().apply {
    workspaceId = subscription.workspaceId
    billingPeriodYear = year
    billingPeriodMonth = month
    status = PENDING
}
log = invoiceGenerationLogRepository.save(log)  // Unique constraint prevents duplicates

// Step 3: Check if invoice already exists (double-check)
val existingInvoice = subscriptionInvoiceRepository
    .findByWorkspaceIdAndBillingPeriod(workspaceId, start, end)

if (existingInvoice != null) {
    // Mark log as success with existing invoice
    log.markSucceeded(existingInvoice.id, existingInvoice.invoiceNumber)
    return
}

// Step 4: Create invoice
```

**Race Condition Protection:**
- Unique constraint on `(workspace_id, year, month)` in database
- Per-subscription transactions prevent interference
- Double-check before invoice creation

---

## Usage Examples

### **Automatic (Daily Job):**

```kotlin
// Runs automatically at 2 AM UTC daily
@Scheduled(cron = "0 0 2 * * ?", zone = "UTC")
fun dailyInvoiceReconciliation() {
    generateMissingInvoices()  // Current + last 3 months
    retryFailedGenerations()   // Retry with backoff
    processPendingPaymentLinks() // Retry failed payment links
}
```

### **Manual Generation (Admin/Testing):**

```kotlin
// Generate invoice for specific workspace and period
val invoice = invoiceGenerationServiceV2.manuallyGenerateInvoice(
    workspaceId = "WS-001",
    year = 2025,
    month = 1
)
```

### **Get Generation Statistics:**

```kotlin
val stats = invoiceGenerationServiceV2.getGenerationStats(
    year = 2025,
    month = 1
)

println("Total: ${stats.totalAttempts}")
println("Success: ${stats.successCount}")
println("Failed: ${stats.failedCount}")
println("Pending: ${stats.pendingCount}")

stats.failedWorkspaces.forEach { (workspaceId, error) ->
    println("$workspaceId: $error")
}
```

---

## Monitoring & Alerts

### **Key Metrics to Monitor:**

1. **Failed Generations:**
   ```sql
   SELECT COUNT(*) FROM invoice_generation_logs
   WHERE status = 'FAILED'
   AND should_retry = true
   AND billing_period_year = 2025
   AND billing_period_month = 1;
   ```

2. **Max Retry Reached (needs manual intervention):**
   ```sql
   SELECT workspace_id, error_message, attempt_count
   FROM invoice_generation_logs
   WHERE status = 'FAILED'
   AND should_retry = false
   AND attempt_count >= 5;
   ```

3. **Payment Link Failures:**
   ```sql
   SELECT workspace_id, invoice_number, payment_link_error
   FROM invoice_generation_logs
   WHERE status = 'SUCCESS'
   AND payment_status IN ('LINK_FAILED', 'AUTO_CHARGE_FAILED');
   ```

4. **Stuck In Progress (possible job crash):**
   ```sql
   SELECT workspace_id, last_attempt_at
   FROM invoice_generation_logs
   WHERE status = 'IN_PROGRESS'
   AND last_attempt_at < NOW() - INTERVAL 1 HOUR;
   ```

---

## Error Handling

### **Transactional Boundaries:**

```kotlin
// Main job - NO transaction (allows per-subscription isolation)
fun dailyInvoiceReconciliation() {
    // Process each subscription independently
}

// Per-subscription - NEW transaction for each
@Transactional(propagation = Propagation.REQUIRES_NEW)
fun generateInvoiceForSubscription(...) {
    try {
        // Create invoice
        // If this fails, only THIS subscription's transaction rolls back
    } catch (e: Exception) {
        // Log failure
        // Other subscriptions continue processing
    }
}
```

### **Payment Processing Isolation:**

Payment processing failures **do NOT fail** invoice generation:

```kotlin
try {
    // Generate invoice - THIS is critical
    val invoice = createInvoice(...)
    log.markSucceeded(invoice.id, invoice.invoiceNumber)

    // Try payment processing - failures tracked separately
    try {
        processPayment(invoice)
    } catch (e: Exception) {
        log.markPaymentProcessing(LINK_FAILED, e.message)
        // Invoice still exists, payment can be retried later
    }
} catch (e: Exception) {
    // Only invoice creation failures are retried
    log.markFailed(e)
}
```

---

## Migration from V1 to V2

### **Steps:**

1. **Deploy V2 alongside V1:**
   ```kotlin
   // V1 continues running (monthly)
   @Service
   class InvoiceGenerationService { ... }

   // V2 starts running (daily)
   @Service
   class InvoiceGenerationServiceV2 { ... }
   ```

2. **V2 will backfill logs:**
   - Daily job checks last 3 months
   - Creates logs for existing invoices (marks as SUCCESS)
   - Generates missing invoices

3. **Monitor for one billing cycle:**
   - Ensure V2 generates all expected invoices
   - Compare V1 and V2 results

4. **Disable V1:**
   ```kotlin
   // Comment out @Scheduled annotation
   // @Scheduled(cron = "0 0 2 1 * ?", zone = "UTC")
   fun generateMonthlyInvoices() { ... }
   ```

5. **Remove V1 after validation:**
   - Delete `InvoiceGenerationService.kt` (old version)
   - Rename `InvoiceGenerationServiceV2` → `InvoiceGenerationService`

---

## Testing

### **Test Scenarios:**

1. **First-time generation (happy path):**
   ```kotlin
   @Test
   fun testFirstTimeGeneration() {
       // Given: No log exists
       // When: Daily job runs
       // Then: Invoice generated, log created with SUCCESS status
   }
   ```

2. **Idempotency (duplicate prevention):**
   ```kotlin
   @Test
   fun testDuplicatePrevention() {
       // Given: Log exists for workspace/period
       // When: Job runs again
       // Then: No duplicate invoice, existing log returned
   }
   ```

3. **Failure and retry:**
   ```kotlin
   @Test
   fun testFailureRetry() {
       // Given: Invoice generation fails
       // When: First attempt
       // Then: Log marked FAILED, next_retry_at = now + 5 minutes

       // When: Retry after backoff
       // Then: Attempt count incremented, new next_retry_at calculated
   }
   ```

4. **Max retries reached:**
   ```kotlin
   @Test
   fun testMaxRetriesReached() {
       // Given: 5 failed attempts
       // When: Check retry logic
       // Then: should_retry = false, manual intervention needed
   }
   ```

5. **Payment link failure doesn't fail invoice:**
   ```kotlin
   @Test
   fun testPaymentLinkFailureIsolated() {
       // Given: Invoice created successfully
       // When: Payment link generation fails
       // Then: Invoice exists, payment_status = LINK_FAILED
   }
   ```

6. **Missed schedule recovery:**
   ```kotlin
   @Test
   fun testMissedScheduleRecovery() {
       // Given: Invoice for 3 months ago not generated
       // When: Daily job runs
       // Then: Missing invoice generated
   }
   ```

---

## Comparison: V1 vs V2

| Feature | V1 (Old) | V2 (New) |
|---------|----------|----------|
| **Schedule** | Monthly (1st at 2 AM) | Daily (2 AM) |
| **Missed schedule** | ❌ Never recovered | ✅ Checks last 3 months |
| **Idempotency** | ⚠️ Simple check only | ✅ Database-enforced |
| **Failure tracking** | ❌ None | ✅ Full error tracking |
| **Retry logic** | ❌ None | ✅ Exponential backoff |
| **Transaction scope** | ❌ All-or-nothing | ✅ Per-subscription |
| **Payment tracking** | ❌ Not tracked | ✅ Separate status |
| **Invoice number** | ⚠️ AtomicInteger (resets) | ✅ Database sequence |
| **Monitoring** | ❌ Logs only | ✅ Queryable status |
| **Manual trigger** | ⚠️ Limited | ✅ Full admin API |

---

## Production Checklist

- [ ] Database migration applied (`V1.0.37__create_invoice_generation_log_table.sql`)
- [ ] Monitoring alerts configured for failed generations
- [ ] Alert for `should_retry = false` (manual intervention needed)
- [ ] Alert for stuck `IN_PROGRESS` jobs
- [ ] Dashboard showing generation stats per month
- [ ] Runbook for manual intervention on persistent failures
- [ ] V2 deployed alongside V1 for one billing cycle
- [ ] V1 disabled after successful validation
- [ ] Load testing for large number of subscriptions

---

## Troubleshooting

### **Q: Invoice stuck in IN_PROGRESS for > 1 hour**

**A:** Job may have crashed mid-execution.

```sql
-- Reset stuck generations
UPDATE invoice_generation_logs
SET status = 'PENDING', last_attempt_at = NULL
WHERE status = 'IN_PROGRESS'
AND last_attempt_at < NOW() - INTERVAL 1 HOUR;
```

### **Q: Invoice failed 5 times, what now?**

**A:** Manual investigation required.

```kotlin
// 1. Check error logs
val log = invoiceGenerationLogRepository.findByWorkspaceIdAndBillingPeriodYearAndBillingPeriodMonth(...)
println(log.errorMessage)
println(log.errorStackTrace)

// 2. Fix underlying issue (e.g., missing billing preferences)

// 3. Reset retry counter
log.attemptCount = 0
log.shouldRetry = true
log.status = InvoiceGenerationStatus.PENDING
log.nextRetryAt = Instant.now()
invoiceGenerationLogRepository.save(log)
```

### **Q: Payment link failed but invoice exists**

**A:** Payment processing is separate from invoice generation.

```sql
-- Find invoices with failed payment processing
SELECT i.invoice_number, l.payment_link_error
FROM invoice_generation_logs l
JOIN invoices i ON l.invoice_id = i.id
WHERE l.payment_status = 'LINK_FAILED';

-- Reset for retry
UPDATE invoice_generation_logs
SET payment_status = 'NOT_STARTED', payment_link_error = NULL
WHERE payment_status = 'LINK_FAILED';
```

---

## Future Enhancements

1. **Webhook notifications** for failed generations
2. **Admin dashboard** showing real-time generation status
3. **Configurable retry policy** per workspace
4. **Parallel processing** for large number of subscriptions
5. **Dry-run mode** for testing without creating invoices
6. **Auto-remediation** for common failure scenarios
