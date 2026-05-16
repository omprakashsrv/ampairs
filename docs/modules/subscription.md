# subscription module

Manages subscription plans, billing cycles, payments, invoicing, device registration, and webhook processing. Integrates with Google Play, App Store, Razorpay, and Stripe.

## REST Endpoints

### Subscriptions (`/api/v1/subscriptions`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/subscriptions/plans` | List all available plans |
| GET | `/api/v1/subscriptions/plans/{planCode}` | Get plan details |
| GET | `/api/v1/subscriptions/current` | Current workspace subscription |
| POST | `/api/v1/subscriptions/purchase/initiate` | Initiate purchase |
| POST | `/api/v1/subscriptions/purchase/verify` | Verify mobile in-app purchase |
| POST | `/api/v1/subscriptions/change-plan` | Upgrade or downgrade plan |
| POST | `/api/v1/subscriptions/cancel` | Cancel subscription |
| POST | `/api/v1/subscriptions/pause` | Pause subscription |
| POST | `/api/v1/subscriptions/resume` | Resume paused subscription |
| POST | `/api/v1/subscriptions/trial` | Start trial |
| POST | `/api/v1/subscriptions/sync` | Sync state (offline-first clients) |
| GET | `/api/v1/subscriptions/usage` | Current resource usage |
| GET | `/api/v1/subscriptions/limits/check` | Check resource limit |
| GET | `/api/v1/subscriptions/features/{feature}` | Check feature availability |

### Billing (`/api/v1/subscriptions/payments`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/subscriptions/payments` | Payment history |
| GET | `/api/v1/subscriptions/payment-methods` | Saved payment methods |
| PUT | `/api/v1/subscriptions/payment-methods/{uid}/default` | Set default method |
| DELETE | `/api/v1/subscriptions/payment-methods/{uid}` | Remove method |

### Invoices (`/api/v1/subscription/invoices`)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/subscription/invoices` | List invoices |
| GET | `/api/v1/subscription/invoices/{uid}` | Get invoice |
| POST | `/api/v1/subscription/invoices/generate` | Generate invoice manually |
| POST | `/api/v1/subscription/invoices/{uid}/pay` | Initiate payment |
| POST | `/api/v1/subscription/invoices/{uid}/retry-payment` | Retry failed payment |
| GET | `/api/v1/subscription/invoices/summary` | Dashboard summary |
| GET | `/api/v1/subscription/invoices/{uid}/download` | Download PDF |
| GET | `/api/v1/subscription/billing-preferences` | Get billing preferences |
| PUT | `/api/v1/subscription/billing-preferences` | Update billing preferences |

### Webhooks

Incoming webhooks from payment providers at `/api/v1/webhooks/{provider}` (idempotent, logged in `WebhookEvent` + `WebhookLog`).

## Key Entities

### Subscription

```kotlin
class Subscription : BaseDomain() {
    val workspaceId: String
    val planCode: String
    val status: SubscriptionStatus    // ACTIVE, TRIALING, PAUSED, PAST_DUE, CANCELLED, EXPIRED
    val billingCycle: BillingCycle    // MONTHLY, ANNUAL
    val paymentProvider: PaymentProvider  // STRIPE, RAZORPAY, GOOGLE_PLAY, APP_STORE
    val externalSubscriptionId: String?
    val externalCustomerId: String?
    val currency: String              // default "INR"
    val currentPeriodStart: Instant
    val currentPeriodEnd: Instant
    val trialEndsAt: Instant?
    val cancelAtPeriodEnd: Boolean
    val nextBillingAmount: BigDecimal
    val lastPaymentStatus: PaymentStatus?
    val lastPaymentAt: Instant?
    val failedPaymentCount: Int
    val gracePeriodEndsAt: Instant?
    val isFree: Boolean
}
```

### SubscriptionPlanDefinition

```kotlin
class SubscriptionPlanDefinition : BaseDomain() {
    val planCode: String              // FREE, STARTER, PROFESSIONAL, ENTERPRISE
    val displayName: String
    val monthlyPriceInr: BigDecimal
    val monthlyPriceUsd: BigDecimal
    // Limits
    val maxWorkspaces: Int
    val maxMembersPerWorkspace: Int
    val maxStorageGb: Int
    val maxCustomers: Int
    val maxProducts: Int
    val maxInvoicesPerMonth: Int
    val maxDevices: Int
    val dataRetentionYears: Int
    // Features (JSON flags)
    val availableModules: List<String>
    val apiAccessEnabled: Boolean
    val customBrandingEnabled: Boolean
    val ssoEnabled: Boolean
    val auditLogsEnabled: Boolean
    val prioritySupport: Boolean
    // Discounts
    val trialDays: Int
    val multiWorkspaceDiscount: BigDecimal?
    val seasonalDiscount: BigDecimal?
    val preLaunchDiscount: BigDecimal?
    // Payment provider product IDs
    val googlePlayProductIds: Map<String, String>?  // billingCycle → productId
    val appStoreProductIds: Map<String, String>?
}
```

### Invoice (subscription billing)

```kotlin
class Invoice : BaseDomain() {
    val workspaceId: String
    val invoiceNumber: String          // unique
    val subscriptionId: Long
    val billingPeriodStart: Instant
    val billingPeriodEnd: Instant
    val dueDate: Instant
    val status: InvoiceStatus          // DRAFT, PENDING, OVERDUE, PARTIALLY_PAID, PAID
    val subtotal: BigDecimal
    val taxAmount: BigDecimal
    val discountAmount: BigDecimal
    val totalAmount: BigDecimal
    val paidAmount: BigDecimal
    val currency: String
    val autoPaymentEnabled: Boolean
    val razorpayInvoiceId: String?
    val stripeInvoiceId: String?
    val paymentLinkUrl: String?
    val generatedAt: Instant
    val paidAt: Instant?
    val lineItems: List<InvoiceLineItem>
}
```

### PaymentTransaction

```kotlin
class PaymentTransaction : BaseDomain() {
    val workspaceId: String
    val paymentProvider: PaymentProvider
    val externalPaymentId: String
    val status: PaymentStatus         // PENDING, PROCESSING, SUCCEEDED, FAILED
    val amount: BigDecimal
    val currency: String
    val taxAmount: BigDecimal
    val discountAmount: BigDecimal
    val paidAt: Instant?
    val failureReason: String?
    val refundAmount: BigDecimal?
    val receiptUrl: String?
}
```

## Payment Providers

| Provider | Market | Integration |
|----------|--------|------------|
| Google Play Billing | Android | Receipt verification via Service Account JSON |
| Apple App Store | iOS | Shared secret + receipt validation |
| Razorpay | India / web | API key + webhook secret |
| Stripe | International / web | Secret key + webhook secret |

## Scheduled Jobs (`SubscriptionScheduledJobs`)

- Renewal billing — charge subscriptions due for renewal
- Grace period enforcement — suspend workspaces with unpaid past-due invoices
- Trial expiry — convert trials to paid or free tier
- Invoice generation — auto-generate monthly billing invoices
- Payment reminders — send email/SMS reminders before due date

## Database Migrations

| File | Description |
|------|-------------|
| `V1.0.30__create_subscription_module_tables.sql` | Core subscription + plan tables |
| `V1.0.31__add_multi_workspace_discount.sql` | Multi-workspace discount fields |
| `V1.0.32__add_seasonal_discount_fields.sql` | Seasonal discount fields |
| `V1.0.33__add_pre_launch_discount.sql` | Pre-launch discount fields |
| `V1.0.34__create_webhook_tables.sql` | Webhook event + log tables |
| `V1.0.35__add_payment_provider_product_ids.sql` | Play/AppStore product IDs |
| `V1.0.36__create_invoice_and_billing_tables.sql` | Invoice + billing preferences tables |
| `V1.0.37__create_invoice_generation_log_table.sql` | Invoice generation audit log |

## Package Structure

```
com.ampairs.subscription
├── config/         — PaymentProviderConfiguration, SchedulingConfig
├── controller/     — SubscriptionController, BillingController, InvoiceController,
│                     PaymentController, WebhookController, DeviceController
├── domain/
│   ├── dto/        — SubscriptionDtos, SubscriptionRequests, InvoiceDtos
│   ├── model/      — Subscription, SubscriptionPlanDefinition, Invoice, InvoiceLineItem,
│   │                  PaymentTransaction, PaymentMethod, BillingPreferences,
│   │                  DeviceRegistration, WebhookEvent, WebhookLog, UsageMetric,
│   │                  SubscriptionAddon, InvoiceGenerationLog
│   ├── repository/ — SubscriptionRepository, SubscriptionPlanRepository,
│   │                  PaymentRepositories, InvoiceGenerationLogRepository,
│   │                  WebhookRepositories, DeviceUsageRepositories
│   └── service/    — SubscriptionService, BillingService, InvoiceGenerationService,
│                     InvoicePaymentService, PaymentProviderService,
│                     SubscriptionDowngradeService, WorkspaceSuspensionService,
│                     UsageTrackingService, WebhookIdempotencyService,
│                     DeviceRegistrationService, EmailNotificationService
├── exception/      — SubscriptionException
├── listener/       — UsageEventListener
├── provider/       — GooglePlayBillingService, AppleAppStoreService,
│                     RazorpayService, StripeService
├── scheduler/      — SubscriptionScheduledJobs
└── webhook/        — WebhookHandlers
```
