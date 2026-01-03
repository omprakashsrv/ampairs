# Critical Fixes Implementation Guide

**Status**: Webhook idempotency infrastructure complete ✅
**Remaining**: 4 critical fixes to implement

---

## ✅ COMPLETED: Fix #1 - Webhook Idempotency Infrastructure

**What was done**:
- ✅ Created `WebhookEvent` entity for idempotency tracking
- ✅ Created `WebhookLog` entity for logging and replay
- ✅ Created `WebhookIdempotencyService` with all required methods
- ✅ Created database migration V1.0.34__create_webhook_tables.sql
- ✅ Created repositories with efficient queries

**Commit**: da91eee

---

## 🔧 TODO: Fix #2 - Integrate Idempotency in WebhookController

**File**: `subscription/src/main/kotlin/com/ampairs/subscription/controller/WebhookController.kt`

**Changes Required**:

```kotlin
// Add dependency injection
@RestController
@RequestMapping("/webhooks")
class WebhookController(
    private val googlePlayWebhookHandler: GooglePlayWebhookHandler,
    private val appStoreWebhookHandler: AppStoreWebhookHandler,
    private val razorpayWebhookHandler: RazorpayWebhookHandler,
    private val stripeWebhookHandler: StripeWebhookHandler,
    private val webhookIdempotencyService: WebhookIdempotencyService,  // ✅ ADD THIS
    private val objectMapper: ObjectMapper
) {

    // UPDATE: Razorpay webhook with idempotency
    @PostMapping("/razorpay")
    @Hidden
    fun handleRazorpayWebhook(
        @RequestBody payload: String,
        @RequestHeader("X-Razorpay-Signature") signature: String?
    ): ResponseEntity<String> {
        // 1. Log webhook FIRST
        val webhookLog = webhookIdempotencyService.logWebhook(
            provider = PaymentProvider.RAZORPAY,
            payload = payload,
            signature = signature
        )

        return try {
            // 2. Verify signature
            if (signature != null && !razorpayWebhookHandler.verifySignature(payload, signature, null)) {
                webhookIdempotencyService.updateWebhookStatus(
                    webhookLog,
                    WebhookStatus.SIGNATURE_FAILED
                )
                return ResponseEntity.status(401).body("""{"error":"signature_verification_failed"}""")
            }

            // 3. Parse payload
            val body = objectMapper.readTree(payload)
            val eventType = body.path("event").asText()
            val eventId = body.path("id").asText()  // Razorpay event ID

            // 4. Check idempotency
            if (webhookIdempotencyService.isProcessed(PaymentProvider.RAZORPAY, eventId)) {
                logger.info("Duplicate Razorpay webhook ignored: {}", eventId)
                webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSED)
                return ResponseEntity.ok().body("""{"status":"ok"}""")
            }

            // 5. Process event
            webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSING)
            razorpayWebhookHandler.processEvent(eventType, body)

            // 6. Mark as processed
            val subscription = body.path("payload").path("subscription").path("entity")
            val externalSubId = subscription.path("id").asText()

            webhookIdempotencyService.markAsProcessed(
                provider = PaymentProvider.RAZORPAY,
                eventId = eventId,
                eventType = eventType,
                payload = payload,
                externalSubscriptionId = externalSubId
            )

            webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSED)
            ResponseEntity.ok().body("""{"status":"ok"}""")

        } catch (e: Exception) {
            logger.error("Error processing Razorpay webhook", e)
            webhookIdempotencyService.updateWebhookStatus(
                webhookLog,
                WebhookStatus.FAILED,
                errorMessage = e.message
            )
            // Still return 200 to prevent provider retries
            // Use internal retry mechanism instead
            ResponseEntity.ok().body("""{"status":"ok"}""")
        }
    }

    // UPDATE: Stripe webhook with idempotency (similar pattern)
    @PostMapping("/stripe")
    @Hidden
    fun handleStripeWebhook(
        @RequestBody payload: String,
        @RequestHeader("Stripe-Signature") signature: String?
    ): ResponseEntity<String> {
        // Log webhook
        val webhookLog = webhookIdempotencyService.logWebhook(
            provider = PaymentProvider.STRIPE,
            payload = payload,
            signature = signature
        )

        return try {
            // Verify signature
            if (signature != null && !stripeWebhookHandler.verifySignature(payload, signature, null)) {
                webhookIdempotencyService.updateWebhookStatus(
                    webhookLog,
                    WebhookStatus.SIGNATURE_FAILED
                )
                return ResponseEntity.status(401).body("""{"error":"signature_verification_failed"}""")
            }

            // Parse and check idempotency
            val body = objectMapper.readTree(payload)
            val eventType = body.path("type").asText()
            val eventId = body.path("id").asText()  // Stripe event ID

            if (webhookIdempotencyService.isProcessed(PaymentProvider.STRIPE, eventId)) {
                logger.info("Duplicate Stripe webhook ignored: {}", eventId)
                webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSED)
                return ResponseEntity.ok().body("""{"received":true}""")
            }

            // Process
            webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSING)
            stripeWebhookHandler.processEvent(eventType, body)

            // Mark as processed
            val dataObject = body.path("data").path("object")
            val externalSubId = dataObject.path("id").asText()

            webhookIdempotencyService.markAsProcessed(
                provider = PaymentProvider.STRIPE,
                eventId = eventId,
                eventType = eventType,
                payload = payload,
                externalSubscriptionId = externalSubId
            )

            webhookIdempotencyService.updateWebhookStatus(webhookLog, WebhookStatus.PROCESSED)
            ResponseEntity.ok().body("""{"received":true}""")

        } catch (e: Exception) {
            logger.error("Error processing Stripe webhook", e)
            webhookIdempotencyService.updateWebhookStatus(
                webhookLog,
                WebhookStatus.FAILED,
                errorMessage = e.message
            )
            ResponseEntity.ok().body("""{"received":true}""")
        }
    }

    // Google Play and App Store webhooks follow similar pattern
    // ...
}
```

**Estimated Time**: 1-2 hours

---

## 🔧 TODO: Fix #3 - Add @Transactional to PaymentOrchestrationService

**File**: `subscription/src/main/kotlin/com/ampairs/subscription/domain/service/PaymentProviderService.kt`

**Changes Required**:

```kotlin
import org.springframework.transaction.annotation.Transactional

@Service
class PaymentOrchestrationService(
    private val subscriptionService: SubscriptionService,
    private val billingService: BillingService,
    private val subscriptionRepository: SubscriptionRepository,
    private val subscriptionPlanRepository: SubscriptionPlanRepository
) {

    // ADD @Transactional to all webhook handler methods

    @Transactional  // ✅ ADD THIS
    fun handleRenewal(
        provider: PaymentProvider,
        externalSubscriptionId: String,
        orderId: String?,
        amount: BigDecimal,
        currency: String,
        periodStart: java.time.Instant?,
        periodEnd: java.time.Instant?
    ) {
        // Existing implementation...
        // Now atomic: both renew subscription and record transaction succeed or both rollback
    }

    @Transactional  // ✅ ADD THIS
    fun handlePaymentFailure(
        provider: PaymentProvider,
        externalSubscriptionId: String,
        failureCode: String?,
        failureReason: String?
    ) {
        // Existing implementation...
    }

    @Transactional  // ✅ ADD THIS
    fun handleCancellation(
        provider: PaymentProvider,
        externalSubscriptionId: String,
        immediate: Boolean
    ) {
        // Existing implementation...
    }
}
```

**Estimated Time**: 15 minutes

---

## 🔧 TODO: Fix #4 - Add Payment Provider Configuration

**File**: `ampairs_service/src/main/resources/application.yml`

**Changes Required**:

```yaml
# Add at end of file

# =====================================================
# Payment Provider Configuration
# =====================================================

google-play:
  package-name: ${GOOGLE_PLAY_PACKAGE_NAME:com.ampairs.app}
  service-account-json: ${GOOGLE_PLAY_SERVICE_ACCOUNT_JSON}

apple-app-store:
  bundle-id: ${APPLE_BUNDLE_ID:com.ampairs.app}
  shared-secret: ${APPLE_SHARED_SECRET}
  production: ${APPLE_PRODUCTION:false}  # Use sandbox by default

razorpay:
  key-id: ${RAZORPAY_KEY_ID}
  key-secret: ${RAZORPAY_KEY_SECRET}
  webhook-secret: ${RAZORPAY_WEBHOOK_SECRET}

stripe:
  secret-key: ${STRIPE_SECRET_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}
  success-url: ${STRIPE_SUCCESS_URL:https://app.ampairs.com/subscription/success}
  cancel-url: ${STRIPE_CANCEL_URL:https://app.ampairs.com/subscription/cancelled}
```

**Environment Variables to Set** (`.env` file):

```bash
# Google Play
GOOGLE_PLAY_PACKAGE_NAME=com.ampairs.app
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON=<JSON from Google Cloud Console>

# Apple App Store
APPLE_BUNDLE_ID=com.ampairs.app
APPLE_SHARED_SECRET=<From App Store Connect>
APPLE_PRODUCTION=false

# Razorpay (India)
RAZORPAY_KEY_ID=<From Razorpay Dashboard>
RAZORPAY_KEY_SECRET=<From Razorpay Dashboard>
RAZORPAY_WEBHOOK_SECRET=<From Razorpay Dashboard>

# Stripe (International)
STRIPE_SECRET_KEY=<From Stripe Dashboard>
STRIPE_WEBHOOK_SECRET=<From Stripe Dashboard>
STRIPE_SUCCESS_URL=https://app.ampairs.com/subscription/success
STRIPE_CANCEL_URL=https://app.ampairs.com/subscription/cancelled
```

**Estimated Time**: 15 minutes + credential setup

---

## 🔧 TODO: Fix #5 - Database Product IDs Migration

**File**: `subscription/src/main/resources/db/migration/mysql/V1.0.35__add_payment_provider_product_ids.sql`

**Create New File**:

```sql
-- Migration: V1.0.35 - Add payment provider product IDs
-- Purpose: Configure product IDs for all payment providers
-- Date: 2025-01-27

-- Add columns if they don't exist (for PostgreSQL compatibility use IF NOT EXISTS)
ALTER TABLE subscription_plans
ADD COLUMN IF NOT EXISTS google_play_product_id_monthly VARCHAR(255),
ADD COLUMN IF NOT EXISTS google_play_product_id_annual VARCHAR(255),
ADD COLUMN IF NOT EXISTS app_store_product_id_monthly VARCHAR(255),
ADD COLUMN IF NOT EXISTS app_store_product_id_annual VARCHAR(255),
ADD COLUMN IF NOT EXISTS razorpay_plan_id_monthly VARCHAR(255),
ADD COLUMN IF NOT EXISTS razorpay_plan_id_annual VARCHAR(255),
ADD COLUMN IF NOT EXISTS stripe_price_id_monthly VARCHAR(255),
ADD COLUMN IF NOT EXISTS stripe_price_id_annual VARCHAR(255);

-- Update FREE plan (no payment provider IDs needed)
UPDATE subscription_plans
SET
  google_play_product_id_monthly = NULL,
  google_play_product_id_annual = NULL,
  app_store_product_id_monthly = NULL,
  app_store_product_id_annual = NULL,
  razorpay_plan_id_monthly = NULL,
  razorpay_plan_id_annual = NULL,
  stripe_price_id_monthly = NULL,
  stripe_price_id_annual = NULL
WHERE plan_code = 'FREE';

-- Update PROFESSIONAL plan
-- TODO: Replace with actual IDs from payment provider dashboards
UPDATE subscription_plans
SET
  google_play_product_id_monthly = 'ampairs_professional_monthly',
  google_play_product_id_annual = 'ampairs_professional_annual',
  app_store_product_id_monthly = 'com.ampairs.professional.monthly',
  app_store_product_id_annual = 'com.ampairs.professional.annual',
  razorpay_plan_id_monthly = 'plan_REPLACE_WITH_RAZORPAY_ID',  -- From Razorpay Dashboard
  razorpay_plan_id_annual = 'plan_REPLACE_WITH_RAZORPAY_ID',   -- From Razorpay Dashboard
  stripe_price_id_monthly = 'price_REPLACE_WITH_STRIPE_ID',    -- From Stripe Dashboard
  stripe_price_id_annual = 'price_REPLACE_WITH_STRIPE_ID'      -- From Stripe Dashboard
WHERE plan_code = 'PROFESSIONAL';

-- Update BUSINESS plan (if exists)
-- UPDATE subscription_plans
-- SET ...
-- WHERE plan_code = 'BUSINESS';

-- Update ENTERPRISE plan (if exists)
-- UPDATE subscription_plans
-- SET ...
-- WHERE plan_code = 'ENTERPRISE';
```

**Steps to Get Product IDs**:

1. **Google Play Console**: Create subscription products
2. **App Store Connect**: Create in-app purchase subscriptions
3. **Razorpay Dashboard**: Create subscription plans
4. **Stripe Dashboard**: Create subscription prices
5. Replace placeholder IDs in migration with actual IDs

**Estimated Time**: 1-2 hours (setup + testing)

---

## 📋 Quick Checklist

```bash
[ ] Fix #2: Update WebhookController with idempotency (1-2 hours)
[ ] Fix #3: Add @Transactional annotations (15 minutes)
[ ] Fix #4: Add payment configuration to application.yml (15 minutes)
[ ] Fix #5: Create product IDs migration (1-2 hours)
[ ] Test all payment flows in sandbox (2-4 hours)
```

**Total Estimated Time**: 5-8 hours

---

## 🚀 After These Fixes

The system will be:
- ✅ Protected against duplicate webhook processing
- ✅ Atomic webhook operations (no partial updates)
- ✅ Properly configured for all payment providers
- ✅ Ready for sandbox testing

**Remaining for Production**:
- Rate limiting (medium priority)
- Monitoring/alerting (medium priority)
- Comprehensive testing (high priority)

---

## 📞 Need Help?

- Webhook idempotency: See `WebhookIdempotencyService.kt`
- Configuration: See `application.yml` example above
- Database: See migration files in `db/migration/mysql/`
