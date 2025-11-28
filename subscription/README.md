# Ampairs Subscription Module

Complete subscription and payment management system supporting Google Play, Apple App Store, Razorpay, and Stripe payment providers.

---

## Features

✅ Multi-platform payment support (Android, iOS, Desktop/Web)
✅ Subscription management (create, upgrade, downgrade, cancel)
✅ Webhook processing with idempotency
✅ Automatic renewals and grace periods
✅ Usage-based billing with limits
✅ Seasonal and pre-launch discounts
✅ Multi-workspace subscriptions
✅ Auto-downgrade on cancellation

---

## Documentation

### Backend Implementation

- **[CRITICAL_FIXES_COMPLETED.md](./CRITICAL_FIXES_COMPLETED.md)** - Production readiness report and all critical bug fixes
- **[MIGRATION_GUIDE.md](./MIGRATION_GUIDE.md)** - Database migration instructions for webhook tables
- **[PRODUCTION_READINESS_REPORT.md](./PRODUCTION_READINESS_REPORT.md)** - Complete production readiness audit

### Frontend/Mobile Implementation

- **[KMP_PAYMENT_INTEGRATION_GUIDE.md](./KMP_PAYMENT_INTEGRATION_GUIDE.md)** - Complete Kotlin Multiplatform integration guide
  - Google Play (Android)
  - Apple App Store (iOS)
  - Razorpay (Desktop/Web)

### Deployment & CI/CD

- **[../.github/SECRETS.md](../.github/SECRETS.md)** - GitHub secrets configuration for deployment
- **[../ampairs_service/src/main/resources/keys/README.md](../ampairs_service/src/main/resources/keys/README.md)** - Payment provider credentials management

---

## Quick Start

### Backend Setup

1. **Database Migrations**:
   ```bash
   JPA_DDL_AUTO=none ./gradlew :ampairs_service:bootRun
   ```

2. **Configure Payment Providers** (see `.env.example`):
   ```bash
   # Google Play
   export GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH=/path/to/service-account.json

   # Apple App Store
   export APPLE_SHARED_SECRET=your_shared_secret

   # Razorpay
   export RAZORPAY_KEY_ID=rzp_live_xxxxx
   export RAZORPAY_KEY_SECRET=your_secret
   export RAZORPAY_WEBHOOK_SECRET=your_webhook_secret

   # Stripe
   export STRIPE_SECRET_KEY=sk_live_xxxxx
   export STRIPE_WEBHOOK_SECRET=whsec_xxxxx
   ```

3. **Update Product IDs** (in database):
   ```sql
   UPDATE subscription_plans
   SET razorpay_plan_id_monthly = 'plan_YOUR_ACTUAL_ID'
   WHERE plan_code = 'PROFESSIONAL';
   ```

### KMP App Setup

1. **Add Dependencies** (see KMP_PAYMENT_INTEGRATION_GUIDE.md):
   ```kotlin
   // Android
   implementation("com.android.billingclient:billing-ktx:6.1.0")

   // iOS - StoreKit (native)

   // Desktop - Razorpay via backend API
   ```

2. **Initialize Payment Service**:
   ```kotlin
   val paymentService = PaymentServiceFactory.create()
   paymentService.initialize()
   ```

3. **Purchase Subscription**:
   ```kotlin
   paymentService.purchaseSubscription("plan_id")
       .onSuccess { /* Handle success */ }
       .onFailure { /* Handle error */ }
   ```

---

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  KMP Mobile/Desktop App                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐              │
│  │ Google   │  │  Apple   │  │ Razorpay │              │
│  │  Play    │  │  Store   │  │  (Web)   │              │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘              │
└───────┼─────────────┼─────────────┼────────────────────┘
        │             │             │
        ▼             ▼             ▼
    Purchase      Receipt      Payment
     Token        Data         Response
        │             │             │
        └─────────────┼─────────────┘
                      ▼
        ┌─────────────────────────┐
        │   Backend API Verify    │
        │  /payment/{provider}/   │
        │        verify           │
        └────────────┬────────────┘
                     │
        ┌────────────▼────────────┐
        │  PaymentProviderService │
        │  - GooglePlayBilling    │
        │  - AppStoreService      │
        │  - RazorpayService      │
        │  - StripeService        │
        └────────────┬────────────┘
                     │
        ┌────────────▼────────────┐
        │  SubscriptionService    │
        │  - Create/Update        │
        │  - Usage Tracking       │
        │  - Auto-renewal         │
        └────────────┬────────────┘
                     │
        ┌────────────▼────────────┐
        │   Webhook Handlers      │
        │  - Idempotency Check    │
        │  - Event Processing     │
        │  - Retry Mechanism      │
        └─────────────────────────┘
```

---

## Payment Provider Setup

### Google Play (Android)

1. **Google Play Console**:
   - Create subscription products
   - Product IDs: `ampairs_professional_monthly`, `ampairs_professional_annual`
   - Set pricing for each country

2. **Google Cloud Console**:
   - Create service account
   - Download JSON key
   - Grant permissions in Play Console

3. **Backend**:
   ```bash
   export GOOGLE_PLAY_SERVICE_ACCOUNT_JSON_PATH=/path/to/key.json
   ```

### Apple App Store (iOS)

1. **App Store Connect**:
   - Create in-app purchase subscriptions
   - Product IDs: `com.ampairs.professional.monthly`, `com.ampairs.professional.annual`
   - Submit for review

2. **Backend**:
   ```bash
   export APPLE_SHARED_SECRET=your_shared_secret
   export APPLE_PRODUCTION=true
   ```

### Razorpay (India)

1. **Razorpay Dashboard**:
   - Create subscription plans
   - Copy plan IDs

2. **Update Database**:
   ```sql
   UPDATE subscription_plans
   SET razorpay_plan_id_monthly = 'plan_xxxxx'
   WHERE plan_code = 'PROFESSIONAL';
   ```

3. **Backend**:
   ```bash
   export RAZORPAY_KEY_ID=rzp_live_xxxxx
   export RAZORPAY_KEY_SECRET=your_secret
   export RAZORPAY_WEBHOOK_SECRET=webhook_secret
   ```

### Stripe (International)

1. **Stripe Dashboard**:
   - Create subscription products
   - Copy price IDs

2. **Update Database**:
   ```sql
   UPDATE subscription_plans
   SET stripe_price_id_monthly = 'price_xxxxx'
   WHERE plan_code = 'PROFESSIONAL';
   ```

3. **Backend**:
   ```bash
   export STRIPE_SECRET_KEY=sk_live_xxxxx
   export STRIPE_WEBHOOK_SECRET=whsec_xxxxx
   ```

---

## API Endpoints

### Payment Verification

```
POST /api/v1/payment/google-play/verify
POST /api/v1/payment/app-store/verify
POST /api/v1/payment/razorpay/initiate
POST /api/v1/payment/stripe/checkout
```

### Subscription Management

```
GET    /api/v1/subscription/plans
GET    /api/v1/subscription/status
POST   /api/v1/subscription/upgrade
POST   /api/v1/subscription/downgrade
POST   /api/v1/subscription/cancel
GET    /api/v1/subscription/usage
```

### Webhooks (Public - No Auth)

```
POST /webhooks/google-play
POST /webhooks/app-store
POST /webhooks/razorpay
POST /webhooks/stripe
```

---

## Database Schema

### Core Tables

- `subscription_plans` - Available subscription tiers and pricing
- `subscriptions` - User subscription records
- `payment_transactions` - Payment history
- `subscription_usage` - Usage tracking for limits
- `webhook_events` - Idempotency tracking
- `webhook_logs` - Webhook processing logs

### Migrations

**MySQL**:
- `V1.0.30__create_subscription_module_tables.sql`
- `V1.0.31__add_multi_workspace_discount.sql`
- `V1.0.32__add_seasonal_discount_fields.sql`
- `V1.0.33__add_pre_launch_discount.sql`
- `V1.0.34__create_webhook_tables.sql`
- `V1.0.35__add_payment_provider_product_ids.sql`

**PostgreSQL**: Same versions, PostgreSQL-specific syntax

---

## Testing

### Sandbox Mode

All payment providers support sandbox/test mode:

- **Google Play**: Use test accounts from Play Console
- **Apple**: Use sandbox accounts from App Store Connect
- **Razorpay**: Use test API keys (rzp_test_)
- **Stripe**: Use test API keys (sk_test_)

### Test Checklist

Backend:
- [ ] Subscription creation
- [ ] Payment verification
- [ ] Webhook processing
- [ ] Idempotency (send duplicate webhooks)
- [ ] Auto-renewal simulation
- [ ] Downgrade on cancellation

Mobile/Desktop:
- [ ] Purchase flow
- [ ] Receipt verification
- [ ] Restore purchases
- [ ] Subscription status sync
- [ ] Error handling

---

## Monitoring

### Key Metrics

- Webhook processing success rate
- Payment verification failures
- Subscription churn rate
- Revenue by payment provider
- Failed payment retries

### Logs

```bash
# Webhook processing
grep "Webhook processed successfully" logs/application.log

# Payment verification
grep "Payment verified" logs/application.log

# Errors
grep "ERROR" logs/application.log | grep -E "webhook|payment"
```

### Database Queries

```sql
-- Check webhook processing status
SELECT status, COUNT(*)
FROM webhook_logs
GROUP BY status;

-- Failed webhooks needing retry
SELECT * FROM webhook_logs
WHERE status = 'FAILED'
AND retry_count < 5;

-- Active subscriptions by provider
SELECT provider, COUNT(*)
FROM subscriptions
WHERE status = 'ACTIVE'
GROUP BY provider;
```

---

## Troubleshooting

### Common Issues

1. **"Webhook signature verification failed"**
   - Check webhook secret configuration
   - Verify payload format matches provider spec

2. **"Product ID not found"**
   - Verify product IDs match between app and database
   - Check subscription_plans table

3. **"Purchase token verification failed"**
   - Check service account permissions (Google Play)
   - Verify receipt is not expired (Apple)

4. **"Duplicate webhook ignored"**
   - This is normal - idempotency working correctly
   - Check webhook_events table for processed events

### Debug Mode

Enable detailed logging:
```bash
export LOGGING_LEVEL_COM_AMPAIRS=DEBUG
```

---

## Production Checklist

Backend:
- [ ] All payment provider credentials configured
- [ ] Product IDs updated with real values (no PLACEHOLDER)
- [ ] Webhook URLs configured in provider dashboards
- [ ] SSL certificates valid
- [ ] Database migrations applied
- [ ] GitHub secrets configured for CI/CD

Mobile/Desktop:
- [ ] Payment libraries integrated
- [ ] Product IDs match backend configuration
- [ ] Error handling implemented
- [ ] Analytics tracking added
- [ ] App reviewed by stores (iOS/Android)

---

## Support

- **Backend Logs**: Check `logs/application.log`
- **Webhook Logs**: Query `webhook_logs` table
- **Payment Provider Dashboards**: Check for errors/notifications
- **Documentation**: See links at top of this README

---

## License

Proprietary - Ampairs Private Limited
