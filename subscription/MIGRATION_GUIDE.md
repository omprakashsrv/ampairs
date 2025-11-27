# Webhook Tables Migration Guide

**Issue**: `Schema-validation: missing table [webhook_events]`

**Root Cause**: Hibernate schema validation (`ddl-auto=validate`) runs before Flyway migrations execute, causing the application to fail when new tables don't exist yet.

---

## Quick Solution

### Option 1: Temporarily Disable Schema Validation (Recommended)

Run the application with schema validation disabled to allow Flyway to create the tables:

```bash
JPA_DDL_AUTO=none ./gradlew :ampairs_service:bootRun
```

Or set the environment variable:

```bash
export JPA_DDL_AUTO=none
./gradlew :ampairs_service:bootRun
```

**What happens**:
1. Application starts with `hibernate.ddl-auto=none`
2. Flyway runs migrations V1.0.34 and V1.0.35
3. Tables `webhook_events` and `webhook_logs` are created
4. Application continues startup successfully

**After first successful run**, you can remove the environment variable and use the default `validate` mode.

### Option 2: Use the Migration Script

We've provided a helper script:

```bash
./scripts/run-migrations.sh
```

This script automatically sets the environment variable and starts the application.

---

## What Was Added

### New Database Tables

#### 1. `webhook_events` (Idempotency)
Prevents duplicate webhook processing by tracking unique event IDs from payment providers.

**Columns**:
- `id` - Primary key
- `uid` - Unique identifier (format: WHE-XXXXXXXXXX)
- `provider` - Payment provider (GOOGLE_PLAY, APP_STORE, RAZORPAY, STRIPE)
- `event_id` - Provider's unique event ID
- `event_type` - Type of webhook event
- `payload` - Full webhook payload (TEXT)
- `processed_at` - When the event was processed
- `external_subscription_id` - Provider's subscription ID
- `workspace_id` - Tenant workspace ID
- `created_at`, `updated_at` - Audit timestamps

**Key Constraint**: Unique index on `(provider, event_id)` ensures each event is only processed once.

#### 2. `webhook_logs` (Debugging and Retry)
Logs all incoming webhooks for debugging, auditing, and retry mechanism.

**Columns**:
- `id` - Primary key
- `uid` - Unique identifier (format: WHL-XXXXXXXXXX)
- `provider` - Payment provider
- `payload` - Raw webhook payload
- `signature` - Webhook signature for verification
- `status` - Processing status (RECEIVED, PROCESSING, PROCESSED, FAILED, SIGNATURE_FAILED, RETRY_SCHEDULED)
- `received_at` - When webhook was received
- `processed_at` - When processing completed
- `error_message` - Error details if failed
- `retry_count` - Number of retry attempts
- `next_retry_at` - When to retry (exponential backoff: 1min → 5min → 30min → 2h → 12h)
- `headers` - HTTP headers (TEXT)
- `created_at`, `updated_at` - Audit timestamps

### New Columns in `subscription_plans`

8 new columns for payment provider product IDs:
- `google_play_product_id_monthly`
- `google_play_product_id_annual`
- `app_store_product_id_monthly`
- `app_store_product_id_annual`
- `razorpay_plan_id_monthly`
- `razorpay_plan_id_annual`
- `stripe_price_id_monthly`
- `stripe_price_id_annual`

**Sample Data Inserted**:
- FREE plan: All product IDs set to NULL
- PROFESSIONAL plan: Product IDs configured (some are PLACEHOLDER values)
- BUSINESS plan: Product IDs configured (some are PLACEHOLDER values)
- ENTERPRISE plan: Product IDs configured (some are PLACEHOLDER values)

---

## Migration Files

### MySQL
- `subscription/src/main/resources/db/migration/mysql/V1.0.34__create_webhook_tables.sql`
- `subscription/src/main/resources/db/migration/mysql/V1.0.35__add_payment_provider_product_ids.sql`

### PostgreSQL
- `subscription/src/main/resources/db/migration/postgresql/V1.0.34__create_webhook_tables.sql`
- `subscription/src/main/resources/db/migration/postgresql/V1.0.35__add_payment_provider_product_ids.sql`

---

## Verification Steps

### 1. Check Migration Status

After the application starts successfully, verify the migrations were applied:

**MySQL**:
```sql
SELECT version, description, installed_on, success
FROM flyway_schema_history
WHERE version IN ('1.0.34', '1.0.35')
ORDER BY installed_rank DESC;
```

**PostgreSQL**:
```sql
SELECT version, description, installed_on, success
FROM flyway_schema_history
WHERE version IN ('1.0.34', '1.0.35')
ORDER BY installed_rank DESC;
```

Expected output:
```
+----------+------------------------------------------+---------------------+---------+
| version  | description                              | installed_on        | success |
+----------+------------------------------------------+---------------------+---------+
| 1.0.35   | add payment provider product ids         | 2025-01-27 ...      | true    |
| 1.0.34   | create webhook tables                    | 2025-01-27 ...      | true    |
+----------+------------------------------------------+---------------------+---------+
```

### 2. Verify Tables Created

**MySQL**:
```sql
SHOW TABLES LIKE 'webhook%';
```

**PostgreSQL**:
```sql
SELECT table_name
FROM information_schema.tables
WHERE table_name LIKE 'webhook%';
```

Expected output:
```
webhook_events
webhook_logs
```

### 3. Verify Table Structure

**Check `webhook_events` table**:
```sql
DESCRIBE webhook_events;  -- MySQL
\d webhook_events         -- PostgreSQL
```

**Check `webhook_logs` table**:
```sql
DESCRIBE webhook_logs;    -- MySQL
\d webhook_logs           -- PostgreSQL
```

### 4. Verify Product ID Columns

```sql
SELECT plan_code,
       google_play_product_id_monthly,
       app_store_product_id_monthly,
       razorpay_plan_id_monthly,
       stripe_price_id_monthly
FROM subscription_plans;
```

Expected output:
```
+---------------+-----------------------------------+----------------------------------+------------------------------------------+--------------------------------------+
| plan_code     | google_play_product_id_monthly    | app_store_product_id_monthly     | razorpay_plan_id_monthly                 | stripe_price_id_monthly              |
+---------------+-----------------------------------+----------------------------------+------------------------------------------+--------------------------------------+
| FREE          | NULL                              | NULL                             | NULL                                     | NULL                                 |
| PROFESSIONAL  | ampairs_professional_monthly      | com.ampairs.professional.monthly | RAZORPAY_PLAN_ID_MONTHLY_PLACEHOLDER     | STRIPE_PRICE_ID_MONTHLY_PLACEHOLDER  |
+---------------+-----------------------------------+----------------------------------+------------------------------------------+--------------------------------------+
```

---

## Next Steps

### 1. Replace Placeholder Values

Before production deployment, replace PLACEHOLDER values with actual IDs from payment provider dashboards.

#### Razorpay
1. Go to [Razorpay Dashboard](https://dashboard.razorpay.com)
2. Navigate to **Subscriptions > Plans**
3. Create subscription plans
4. Copy the `plan_id` (format: `plan_XXXXXXXXXXXX`)
5. Run UPDATE queries:

```sql
UPDATE subscription_plans
SET razorpay_plan_id_monthly = 'plan_YOUR_ACTUAL_ID',
    razorpay_plan_id_annual = 'plan_YOUR_ACTUAL_ID'
WHERE plan_code = 'PROFESSIONAL';
```

#### Stripe
1. Go to [Stripe Dashboard](https://dashboard.stripe.com)
2. Navigate to **Products > Add product**
3. Create subscription product with monthly and annual prices
4. Copy the `price_id` (format: `price_XXXXXXXXXXXX`)
5. Run UPDATE queries:

```sql
UPDATE subscription_plans
SET stripe_price_id_monthly = 'price_YOUR_ACTUAL_ID',
    stripe_price_id_annual = 'price_YOUR_ACTUAL_ID'
WHERE plan_code = 'PROFESSIONAL';
```

### 2. Test Webhook Endpoints

Once the application is running, test webhook endpoints:

```bash
# Test Razorpay webhook (with logging and idempotency)
curl -X POST http://localhost:8080/webhooks/razorpay \
  -H "Content-Type: application/json" \
  -H "X-Razorpay-Signature: test_signature" \
  -d '{"event":"subscription.charged","id":"event_test123","payload":{"subscription":{"entity":{"id":"sub_test"}}}}'

# Check webhook log
SELECT * FROM webhook_logs ORDER BY created_at DESC LIMIT 1;

# Test duplicate (should be ignored)
# Run the same curl command again and verify duplicate=true in response
```

---

## Troubleshooting

### Issue: Migration not found

**Symptom**: Flyway doesn't find the migration files

**Solution**: Ensure `subscription` module is included in `ampairs_service` dependencies:

```kotlin
// ampairs_service/build.gradle.kts
dependencies {
    implementation(project(":subscription"))
    // ...
}
```

### Issue: Checksum mismatch

**Symptom**: `FlywayException: Validate failed: Migration checksum mismatch`

**Cause**: Migration file was modified after being applied

**Solution**:
1. **Never modify applied migrations**
2. Create a new migration with a higher version number
3. Or run `./gradlew :ampairs_service:flywayRepair` (only in development)

### Issue: Duplicate version

**Symptom**: `FlywayException: Found more than one migration with version 1.0.34`

**Cause**: Multiple files with same version number

**Solution**: Check for duplicate files:
```bash
find . -name "V1.0.34*.sql"
```

### Issue: Out of order migration

**Symptom**: `FlywayException: Detected out of order migration`

**Cause**: New migration has version lower than already applied migrations

**Solution**:
1. Rename migration with next available version
2. Or enable `flyway.out-of-order=true` in development only

---

## Database-Specific Notes

### MySQL
- Uses `AUTO_INCREMENT` for primary keys
- Uses `ON UPDATE CURRENT_TIMESTAMP` for automatic `updated_at`
- Uses `ENGINE=InnoDB` and `utf8mb4` charset
- Table comments via `ALTER TABLE ... COMMENT`

### PostgreSQL
- Uses `BIGSERIAL` for auto-incrementing primary keys
- Uses triggers for automatic `updated_at` timestamp
- Uses `CREATE OR REPLACE FUNCTION` for trigger functions
- Table/column comments via `COMMENT ON TABLE/COLUMN`

---

## Related Files

- **WebhookController**: `subscription/src/main/kotlin/com/ampairs/subscription/controller/WebhookController.kt`
- **WebhookIdempotencyService**: `subscription/src/main/kotlin/com/ampairs/subscription/domain/service/WebhookIdempotencyService.kt`
- **WebhookEvent Entity**: `subscription/src/main/kotlin/com/ampairs/subscription/domain/model/WebhookEvent.kt`
- **WebhookLog Entity**: `subscription/src/main/kotlin/com/ampairs/subscription/domain/model/WebhookLog.kt`
- **Implementation Guide**: `subscription/CRITICAL_FIXES_COMPLETED.md`

---

## Summary

✅ **MySQL migrations created**: V1.0.34, V1.0.35
✅ **PostgreSQL migrations created**: V1.0.34, V1.0.35
✅ **Webhook idempotency infrastructure**: Complete
✅ **Payment provider product ID schema**: Complete

**To run migrations**: `JPA_DDL_AUTO=none ./gradlew :ampairs_service:bootRun`

After first successful run, migrations are applied and the application will start normally with schema validation enabled.
