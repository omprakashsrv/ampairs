# subscription module

Plans, billing cycles, payments (Razorpay/Stripe/Google Play/App Store), invoicing, webhook processing.

## Payment providers
`STRIPE` (international), `RAZORPAY` (India), `GOOGLE_PLAY` (Android IAP), `APP_STORE` (iOS IAP)

## Key entities
- `Subscription` — workspaceId, planCode, status, billingCycle, paymentProvider, currentPeriodStart/End
- `SubscriptionPlanDefinition` — planCode, limits (maxMembers, maxStorage…), features, pricing INR/USD
- `Invoice` (billing) — invoiceNumber, billingPeriod, status, totalAmount, lineItems
- `PaymentTransaction` — provider, externalPaymentId, status, amount, paidAt
- `BillingPreferences` — autoPaymentEnabled, defaultPaymentMethodId, billingEmail
- `WebhookEvent` + `WebhookLog` — idempotent webhook processing

## Subscription statuses
`ACTIVE`, `TRIALING`, `PAUSED`, `PAST_DUE`, `CANCELLED`, `EXPIRED`

## Base paths
`/api/v1/subscriptions/**`, `/api/v1/subscription/**`

## Scheduled jobs
Renewal billing, grace period enforcement, trial expiry, invoice generation, payment reminders

## Migrations
`V1.0.30–V1.0.37`

## Full docs
`docs/modules/subscription.md`
