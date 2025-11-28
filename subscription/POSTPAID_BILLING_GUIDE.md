# Postpaid Billing System - Implementation Guide

Complete guide for the invoice-based postpaid billing system with hybrid payment options.

---

## Overview

The postpaid billing system allows workspaces to use the service first and pay later via monthly invoices. It supports:

- ✅ **Hybrid Payment**: Auto-charge saved payment methods OR manual payment links
- ✅ **Monthly Billing Cycle**: Automatic invoice generation on the 1st of every month
- ✅ **Grace Period**: 15-day grace period before workspace suspension
- ✅ **Payment Reminders**: Automated email reminders before/after due date
- ✅ **Multi-Provider**: Razorpay (India) and Stripe (International)
- ✅ **Client-Side Read-Only Mode**: No API restrictions, UI enforcement only

---

## Architecture

```
┌──────────────────────────────────────────────────────┐
│         Monthly Invoice Generation Job                │
│         (Runs 1st of every month at 2 AM UTC)         │
└────────────────┬─────────────────────────────────────┘
                 │
                 ▼
    ┌────────────────────────┐
    │ Active Subscriptions   │
    │ (Postpaid Mode Only)   │
    └────────────┬───────────┘
                 │
                 ▼
    ┌────────────────────────┐
    │   Generate Invoice     │
    │   - Subscription fee   │
    │   - Addon charges      │
    │   - Tax calculation    │
    └────────────┬───────────┘
                 │
         ┌───────┴───────┐
         │               │
         ▼               ▼
  ┌──────────┐    ┌──────────────┐
  │ Auto-Pay │    │ Payment Link │
  │ Enabled? │    │  (Manual)    │
  └────┬─────┘    └──────┬───────┘
       │                 │
       ▼                 ▼
┌─────────────┐   ┌──────────────┐
│ Auto-Charge │   │ Email with   │
│ Saved Card  │   │ Payment Link │
└─────────────┘   └──────────────┘
```

---

## Database Schema

### Tables Created

1. **`billing_preferences`** - Workspace billing settings
2. **`invoices`** - Invoice records
3. **`invoice_line_items`** - Invoice line items (charges)
4. **`payment_methods`** - Updated with customer IDs

### Migration File

Location: `src/main/resources/db/migration/mysql/V1.0.36__create_invoice_and_billing_tables.sql`

---

## Key Components

### 1. Entities

#### `Invoice.kt`
- **Purpose**: Represents a billing invoice
- **Key Fields**:
  - `invoiceNumber`: Unique identifier (e.g., `INV-2025-0001`)
  - `billingPeriodStart/End`: Billing period
  - `dueDate`: Payment due date (generation + grace period)
  - `status`: `DRAFT`, `PENDING`, `PAID`, `OVERDUE`, `SUSPENDED`
  - `autoPaymentEnabled`: Auto-charge flag
  - `paymentLinkUrl`: Razorpay/Stripe payment link

#### `InvoiceLineItem.kt`
- **Purpose**: Individual charges on an invoice
- **Types**: `SUBSCRIPTION`, `ADDON`, `USAGE`, `DISCOUNT`

#### `BillingPreferences.kt`
- **Purpose**: Workspace billing configuration
- **Key Fields**:
  - `billingMode`: `PREPAID` or `POSTPAID`
  - `autoPaymentEnabled`: Enable auto-charge
  - `defaultPaymentMethodId`: FK to `payment_methods`
  - `gracePeriodDays`: Days before suspension (default 15)
  - `billingEmail`: Contact for invoices

#### `PaymentMethod.kt` (Enhanced)
- **New Fields**:
  - `razorpayCustomerId`: Razorpay customer ID
  - `stripeCustomerId`: Stripe customer ID
- **Purpose**: Store customer tokens for auto-charge

---

### 2. Services

#### `InvoiceGenerationService.kt`
- **Scheduled Job**: `@Scheduled(cron = "0 0 2 1 * ?")` - Runs 1st of month at 2 AM UTC
- **Responsibilities**:
  - Generate monthly invoices for active postpaid subscriptions
  - Calculate charges (subscription + addons)
  - Apply taxes based on country
  - Initiate payment (auto-charge or payment link)

#### `InvoicePaymentService.kt`
- **Responsibilities**:
  - Generate Razorpay/Stripe payment links
  - Process auto-charge using saved payment methods
  - Verify manual payments from webhooks
  - Mark invoices as paid

#### `WorkspaceSuspensionService.kt`
- **Scheduled Jobs**:
  - `checkOverdueInvoices()` - Daily at midnight UTC
  - `sendPreDueReminders()` - Daily at 10 AM UTC
- **Responsibilities**:
  - Send payment reminders (3 days before due, 3/7/14 days after)
  - Suspend workspaces after 15 days overdue
  - Manage grace period enforcement

#### `EmailNotificationService.kt`
- **Responsibilities**:
  - Send invoice emails with payment links
  - Send payment reminders
  - Send suspension/reactivation notifications
- **Status**: Stub implementation (TODO: integrate with SendGrid/AWS SES)

---

### 3. Controllers

#### `InvoiceController.kt`

**Endpoints:**

```
GET    /api/v1/billing/invoices              # List all invoices
GET    /api/v1/billing/invoices/{uid}        # Get invoice details
POST   /api/v1/billing/invoices/generate     # Manual invoice generation (Admin only)
POST   /api/v1/billing/invoices/{uid}/pay    # Initiate payment
POST   /api/v1/billing/invoices/{uid}/retry-payment  # Retry failed payment
GET    /api/v1/billing/invoices/summary      # Invoice dashboard summary
GET    /api/v1/billing/invoices/{uid}/download # Download PDF (TODO)
```

#### `BillingPreferencesController.kt`

**Endpoints:**

```
GET    /api/v1/billing/preferences            # Get billing settings
PUT    /api/v1/billing/preferences            # Update billing settings
```

---

## API Examples

### 1. Get Invoices for Workspace

```bash
GET /api/v1/billing/invoices?workspaceId=WS123&status=PENDING
Authorization: Bearer {jwt_token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "uid": "INV-001",
        "invoiceNumber": "INV-2025-0001",
        "billingPeriodStart": "2025-01-01T00:00:00Z",
        "billingPeriodEnd": "2025-01-31T23:59:59Z",
        "dueDate": "2025-02-15T23:59:59Z",
        "status": "PENDING",
        "totalAmount": 1499.00,
        "paidAmount": 0.00,
        "remainingBalance": 1499.00,
        "currency": "INR",
        "paymentLinkUrl": "https://rzp.io/i/xxxxx",
        "isOverdue": false,
        "daysPastDue": 0,
        "lineItems": [
          {
            "description": "Professional Plan - January 2025",
            "itemType": "SUBSCRIPTION",
            "quantity": 1,
            "unitPrice": 1499.00,
            "amount": 1499.00
          }
        ]
      }
    ]
  }
}
```

---

### 2. Pay Invoice (Manual Payment Link)

```bash
POST /api/v1/billing/invoices/INV-001/pay
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "invoiceUid": "INV-001",
  "useAutoCharge": false
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "invoiceUid": "INV-001",
    "paymentLinkUrl": "https://rzp.io/i/payment-link-xxxxx",
    "expiresAt": null
  }
}
```

---

### 3. Enable Auto-Payment

```bash
PUT /api/v1/billing/preferences?workspaceId=WS123
Content-Type: application/json
Authorization: Bearer {jwt_token}

{
  "autoPaymentEnabled": true,
  "defaultPaymentMethodId": 456,
  "billingEmail": "billing@company.com",
  "gracePeriodDays": 15
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "workspaceId": "WS123",
    "autoPaymentEnabled": true,
    "defaultPaymentMethodId": 456,
    "billingMode": "POSTPAID",
    "isAutoPaymentConfigured": true
  }
}
```

---

## Payment Flow

### Scenario 1: Auto-Charge (Saved Payment Method)

1. **1st of Month**: Invoice generated automatically
2. **Auto-Charge**: System charges saved payment method
3. **Success**: Invoice marked as `PAID`, email receipt sent
4. **Failure**: Fallback to payment link, email sent

### Scenario 2: Manual Payment Link

1. **1st of Month**: Invoice generated automatically
2. **Email Sent**: Payment link sent to billing email
3. **Customer Pays**: Customer clicks link, completes payment
4. **Webhook**: Razorpay/Stripe webhook confirms payment
5. **Invoice Updated**: Status changed to `PAID`

---

## Grace Period & Suspension

### Timeline

| Day | Event |
|-----|-------|
| **Day 0** | Invoice generated, due date = Day 15 |
| **Day -3** | Pre-due reminder email sent |
| **Day 0** | Invoice due date |
| **Day 3** | Overdue reminder #1 |
| **Day 7** | Overdue reminder #2 |
| **Day 14** | Final warning email |
| **Day 15** | **Workspace suspended** (status = `SUSPENDED`) |

### Suspension Enforcement

**Backend:**
- Invoice status changed to `SUSPENDED`
- Workspace marked as suspended (for UI)
- **No API restrictions** (per requirements)

**Frontend (Angular):**
```typescript
// Guard to prevent write operations
@Injectable()
export class WorkspaceGuard {
  canActivateWrite(): boolean {
    const workspace = this.workspaceService.getCurrentWorkspace();

    if (workspace.hasPendingInvoices && workspace.status === 'SUSPENDED') {
      this.showPaymentRequiredDialog();
      return false; // Block write operations
    }
    return true;
  }
}
```

---

## Integration with Payment Providers

### Razorpay (India - INR)

**Invoice Creation:**
```kotlin
val razorpayInvoice = razorpayService.createInvoice(
    workspaceId = "WS123",
    amount = BigDecimal("1499.00"),
    currency = "INR",
    description = "Invoice INV-2025-0001",
    notes = mapOf("invoice_uid" to "INV-001")
)
// Returns payment link: razorpayInvoice.shortUrl
```

**Auto-Charge:**
```kotlin
val payment = razorpayService.chargePaymentMethod(
    customerId = "cust_xxxxx",
    paymentMethodId = "pm_xxxxx",
    amount = BigDecimal("1499.00"),
    currency = "INR",
    description = "Invoice INV-2025-0001"
)
```

### Stripe (International - USD/EUR)

**Invoice Creation:**
```kotlin
val stripeInvoice = stripeService.createInvoice(
    workspaceId = "WS123",
    amount = BigDecimal("19.99"),
    currency = "USD",
    description = "Invoice INV-2025-0001",
    metadata = mapOf("invoice_uid" to "INV-001")
)
// Returns payment link: stripeInvoice.hostedInvoiceUrl
```

**Auto-Charge:**
```kotlin
val paymentIntent = stripeService.chargePaymentMethod(
    customerId = "cus_xxxxx",
    paymentMethodId = "pm_xxxxx",
    amount = BigDecimal("19.99"),
    currency = "USD",
    description = "Invoice INV-2025-0001"
)
```

---

## Webhook Integration

### Razorpay Webhook

**Endpoint:** `POST /webhooks/razorpay`

**Events to Handle:**
- `invoice.paid` - Invoice paid successfully
- `payment.failed` - Payment attempt failed
- `payment.captured` - Payment captured

### Stripe Webhook

**Endpoint:** `POST /webhooks/stripe`

**Events to Handle:**
- `invoice.payment_succeeded` - Invoice paid
- `invoice.payment_failed` - Payment failed
- `payment_intent.succeeded` - Auto-charge succeeded

---

## Frontend Implementation (Angular)

### 1. Invoice List Component

```typescript
// ampairs-web/src/app/billing/invoices/invoice-list.component.ts

@Component({
  selector: 'app-invoice-list',
  templateUrl: './invoice-list.component.html'
})
export class InvoiceListComponent implements OnInit {
  invoices: InvoiceResponse[] = [];
  summary: InvoiceSummaryResponse;

  ngOnInit() {
    this.loadInvoices();
    this.loadSummary();
  }

  loadInvoices() {
    this.billingService.getInvoices().subscribe({
      next: (response) => {
        this.invoices = response.data.content;
      }
    });
  }

  payInvoice(invoice: InvoiceResponse) {
    this.billingService.payInvoice(invoice.uid, false).subscribe({
      next: (response) => {
        // Open payment link in new window
        window.open(response.data.paymentLinkUrl, '_blank');
      }
    });
  }
}
```

### 2. Read-Only Mode Guard

```typescript
// ampairs-web/src/app/core/guards/workspace-write.guard.ts

@Injectable()
export class WorkspaceWriteGuard implements CanActivate {
  constructor(
    private billingService: BillingService,
    private dialog: MatDialog
  ) {}

  canActivate(): Observable<boolean> {
    return this.billingService.getInvoiceSummary().pipe(
      map(response => {
        const summary = response.data;

        if (summary.overdueInvoices > 0) {
          // Show payment required dialog
          this.dialog.open(PaymentRequiredDialogComponent, {
            data: {
              outstandingAmount: summary.totalOutstanding,
              nextDueDate: summary.nextDueDate
            },
            disableClose: true
          });
          return false; // Block write operations
        }

        return true; // Allow access
      })
    );
  }
}
```

### 3. Apply Guard to Routes

```typescript
// ampairs-web/src/app/app-routing.module.ts

const routes: Routes = [
  {
    path: 'invoices',
    children: [
      { path: 'create', component: InvoiceCreateComponent, canActivate: [WorkspaceWriteGuard] },
      { path: 'edit/:id', component: InvoiceEditComponent, canActivate: [WorkspaceWriteGuard] }
    ]
  }
];
```

---

## Testing

### Manual Testing Checklist

Backend:
- [ ] Monthly invoice generation (simulate by calling service manually)
- [ ] Auto-charge with saved payment method
- [ ] Payment link generation (Razorpay/Stripe)
- [ ] Payment reminder emails
- [ ] Workspace suspension after 15 days
- [ ] Webhook payment verification

Frontend:
- [ ] Invoice list displays correctly
- [ ] Payment link opens in new window
- [ ] Read-only mode activates for overdue invoices
- [ ] Payment required dialog appears
- [ ] Workspace reactivates after payment

### Unit Tests (TODO)

```kotlin
@Test
fun `should generate invoice for active subscription`() {
    // Test invoice generation
}

@Test
fun `should send overdue reminder after 3 days`() {
    // Test reminder logic
}

@Test
fun `should suspend workspace after 15 days`() {
    // Test suspension
}
```

---

## Configuration

### Application Properties

```yaml
# application.yml

spring:
  task:
    scheduling:
      enabled: true  # Enable scheduled jobs

subscription:
  billing:
    grace-period-days: 15
    reminder-days: [3, 7, 14]
    invoice-prefix: "INV"
```

---

## Deployment Checklist

- [ ] Database migration applied (`V1.0.36__create_invoice_and_billing_tables.sql`)
- [ ] Razorpay/Stripe credentials configured
- [ ] Email service integrated (SendGrid/AWS SES)
- [ ] Scheduled jobs enabled
- [ ] Webhook endpoints configured in Razorpay/Stripe dashboards
- [ ] Frontend guard implemented for read-only mode
- [ ] Monitoring and alerting configured

---

## Troubleshooting

### Invoice Not Generated

**Check:**
1. Billing mode is `POSTPAID` in `billing_preferences`
2. Subscription status is `ACTIVE`
3. Scheduled job is enabled
4. Check logs for errors in `InvoiceGenerationService`

### Auto-Charge Failed

**Check:**
1. Payment method is verified (`isVerified()` returns true)
2. Payment method belongs to correct workspace
3. Razorpay/Stripe customer ID is set
4. Check payment provider dashboard for errors

### Workspace Not Suspending

**Check:**
1. Invoice is marked as `OVERDUE`
2. Days past due >= 15
3. `WorkspaceSuspensionService` scheduled job is running
4. Check logs for suspension errors

---

## Future Enhancements

- [ ] PDF invoice generation and download
- [ ] Partial payment support
- [ ] Late fee calculation
- [ ] Invoice dispute handling
- [ ] Custom billing cycles (weekly, quarterly)
- [ ] Multi-currency support expansion
- [ ] Invoice templates customization
- [ ] Payment plan installments

---

## Support

For issues or questions:
- **Backend Logs**: Check `logs/application.log` for `InvoiceGenerationService`, `InvoicePaymentService`, `WorkspaceSuspensionService`
- **Database**: Query `invoices`, `billing_preferences`, `invoice_line_items` tables
- **Payment Provider**: Check Razorpay/Stripe dashboard for payment errors

---

## License

Proprietary - Ampairs Private Limited
