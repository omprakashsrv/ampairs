# Mobile App Implementation Guide - Multi-Workspace Discounts

## Overview

This guide explains how to integrate the multi-workspace discount system into the mobile app (Kotlin Multiplatform).

## Backend Changes Summary

The backend now automatically applies volume-based discounts when users have multiple workspaces:
- **Default Policy**: 20% discount for 3+ workspaces
- **Automatic Application**: Discounts calculated on all subscription operations
- **Stacking Discounts**: Multi-workspace + annual billing discounts stack

---

## API Changes

### 1. Get Available Plans API

**Endpoint**: `GET /api/v1/subscription/plans`

**Response Updated** - New field `multiWorkspaceDiscount`:

```json
{
  "success": true,
  "data": [
    {
      "uid": "PLAN20251123090255640...",
      "planCode": "PROFESSIONAL",
      "displayName": "Professional",
      "description": "For growing businesses",
      "monthlyPriceInr": 500.00,
      "monthlyPriceUsd": 6.00,
      "limits": { ... },
      "features": { ... },
      "trialDays": 14,
      "multiWorkspaceDiscount": {
        "minWorkspaces": 3,
        "discountPercent": 20,
        "isAvailable": true
      },
      "displayOrder": 2
    }
  ]
}
```

**New Fields**:
- `multiWorkspaceDiscount.minWorkspaces`: Minimum workspaces required for discount
- `multiWorkspaceDiscount.discountPercent`: Discount percentage (0-100)
- `multiWorkspaceDiscount.isAvailable`: Whether discount is configured for this plan

---

## UI Implementation

### 1. Plan Selection Screen

**Display Discount Badge**:

```kotlin
// Kotlin Multiplatform (Compose)
@Composable
fun PlanCard(plan: PlanResponse) {
    Card {
        Column {
            Text(plan.displayName, style = MaterialTheme.typography.h5)

            // Price display
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatPrice(plan.monthlyPriceInr),
                    style = MaterialTheme.typography.h4
                )
                Text("/workspace/month")
            }

            // Show discount badge if available
            if (plan.multiWorkspaceDiscount.isAvailable) {
                DiscountBadge(discount = plan.multiWorkspaceDiscount)
            }

            // Features list
            plan.features.forEach { feature ->
                FeatureItem(feature)
            }
        }
    }
}

@Composable
fun DiscountBadge(discount: MultiWorkspaceDiscountResponse) {
    Surface(
        color = MaterialTheme.colors.secondary,
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalOffer,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${discount.discountPercent}% OFF for ${discount.minWorkspaces}+ workspaces",
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
```

---

### 2. Pricing Calculator Widget

**Show Real-Time Discount Calculation**:

```kotlin
@Composable
fun PricingCalculator(
    plan: PlanResponse,
    userWorkspaceCount: Int
) {
    val discount = plan.multiWorkspaceDiscount
    val basePrice = plan.monthlyPriceInr
    val hasDiscount = userWorkspaceCount >= discount.minWorkspaces && discount.isAvailable

    val discountedPrice = if (hasDiscount) {
        basePrice * (100 - discount.discountPercent) / 100
    } else {
        basePrice
    }

    val totalPrice = discountedPrice * userWorkspaceCount
    val savings = if (hasDiscount) {
        (basePrice - discountedPrice) * userWorkspaceCount
    } else {
        BigDecimal.ZERO
    }

    Column {
        Text(
            text = "Your Price Breakdown",
            style = MaterialTheme.typography.h6
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Workspace count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${userWorkspaceCount} workspace(s)")
            Text("₹${basePrice} each")
        }

        // Discount applied
        if (hasDiscount) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Multi-workspace discount (${discount.discountPercent}%)",
                    color = MaterialTheme.colors.secondary
                )
                Text(
                    text = "-₹${savings}",
                    color = MaterialTheme.colors.secondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Total per month",
                style = MaterialTheme.typography.h6
            )
            Text(
                text = "₹${totalPrice}",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )
        }

        // Encourage message for more workspaces
        if (!hasDiscount && discount.isAvailable) {
            val workspacesNeeded = discount.minWorkspaces - userWorkspaceCount
            InfoBanner(
                message = "Add ${workspacesNeeded} more workspace(s) to get ${discount.discountPercent}% off!"
            )
        }
    }
}
```

---

### 3. Subscription Confirmation Screen

**Before Purchase - Show Final Price**:

```kotlin
@Composable
fun SubscriptionConfirmation(
    plan: PlanResponse,
    billingCycle: BillingCycle,
    userWorkspaceCount: Int
) {
    val discount = plan.multiWorkspaceDiscount
    val hasMultiWorkspaceDiscount = userWorkspaceCount >= discount.minWorkspaces && discount.isAvailable
    val hasAnnualDiscount = billingCycle == BillingCycle.ANNUAL

    Column {
        Text("Subscription Summary", style = MaterialTheme.typography.h5)

        Spacer(modifier = Modifier.height(16.dp))

        // Plan name
        SummaryRow("Plan", plan.displayName)

        // Billing cycle
        SummaryRow("Billing Cycle", billingCycle.displayName)

        // Workspace count
        SummaryRow("Workspaces", userWorkspaceCount.toString())

        Spacer(modifier = Modifier.height(16.dp))

        // Pricing breakdown
        Text("Pricing Breakdown", style = MaterialTheme.typography.subtitle1)

        val basePrice = plan.monthlyPriceInr * userWorkspaceCount
        PriceRow("Base Price", basePrice)

        // Multi-workspace discount
        if (hasMultiWorkspaceDiscount) {
            val discountAmount = basePrice * discount.discountPercent / 100
            PriceRow(
                label = "Multi-workspace discount (${discount.discountPercent}%)",
                amount = -discountAmount,
                isDiscount = true
            )
        }

        // Annual discount
        if (hasAnnualDiscount) {
            val annualDiscountPercent = 10 // Example: 10% for annual
            val subtotal = if (hasMultiWorkspaceDiscount) {
                basePrice * (100 - discount.discountPercent) / 100
            } else {
                basePrice
            }
            val annualDiscountAmount = subtotal * annualDiscountPercent / 100
            PriceRow(
                label = "Annual discount (${annualDiscountPercent}%)",
                amount = -annualDiscountAmount,
                isDiscount = true
            )
        }

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // Total (backend will calculate exact amount)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total", style = MaterialTheme.typography.h6)
            Text(
                text = "₹${calculateFinalPrice()}",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.primary
            )
        }

        // Note about backend calculation
        Text(
            text = "Final price will be calculated by the server",
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun PriceRow(label: String, amount: BigDecimal, isDiscount: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = if (isDiscount) MaterialTheme.colors.secondary else Color.Unspecified
        )
        Text(
            text = if (amount >= BigDecimal.ZERO) "₹${amount}" else "-₹${amount.abs()}",
            color = if (isDiscount) MaterialTheme.colors.secondary else Color.Unspecified,
            fontWeight = if (isDiscount) FontWeight.Bold else FontWeight.Normal
        )
    }
}
```

---

### 4. Workspace Selection Screen

**When Initiating Purchase**:

```kotlin
@Composable
fun WorkspaceSelectionScreen(
    viewModel: SubscriptionViewModel,
    availablePlans: List<PlanResponse>
) {
    val userWorkspaces by viewModel.userWorkspaces.collectAsState()
    val selectedPlan by viewModel.selectedPlan.collectAsState()

    Column {
        Text(
            text = "You have ${userWorkspaces.size} workspace(s)",
            style = MaterialTheme.typography.h6
        )

        // Show discount eligibility
        selectedPlan?.let { plan ->
            val discount = plan.multiWorkspaceDiscount
            if (discount.isAvailable) {
                if (userWorkspaces.size >= discount.minWorkspaces) {
                    SuccessBanner(
                        message = "🎉 You're eligible for ${discount.discountPercent}% discount!"
                    )
                } else {
                    InfoBanner(
                        message = "Create ${discount.minWorkspaces - userWorkspaces.size} more workspace(s) to unlock ${discount.discountPercent}% discount"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Workspace list
        LazyColumn {
            items(userWorkspaces) { workspace ->
                WorkspaceItem(workspace)
            }
        }

        // Create workspace button
        Button(
            onClick = { viewModel.navigateToCreateWorkspace() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create New Workspace")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Proceed button
        Button(
            onClick = { viewModel.proceedToCheckout() },
            modifier = Modifier.fillMaxWidth(),
            enabled = userWorkspaces.isNotEmpty()
        ) {
            Text("Continue to Payment")
        }
    }
}
```

---

## Data Models

### Update Your DTOs

```kotlin
// Add to your subscription DTOs file

data class PlanResponse(
    val uid: String,
    val planCode: String,
    val displayName: String,
    val description: String?,
    val monthlyPriceInr: BigDecimal,
    val monthlyPriceUsd: BigDecimal,
    val limits: PlanLimitsResponse,
    val features: PlanFeaturesResponse,
    val trialDays: Int,
    val multiWorkspaceDiscount: MultiWorkspaceDiscountResponse, // NEW FIELD
    val googlePlayProductIdMonthly: String?,
    val googlePlayProductIdAnnual: String?,
    val appStoreProductIdMonthly: String?,
    val appStoreProductIdAnnual: String?,
    val displayOrder: Int
)

data class MultiWorkspaceDiscountResponse(
    val minWorkspaces: Int,
    val discountPercent: Int,
    val isAvailable: Boolean
)
```

---

## ViewModel Implementation

```kotlin
class SubscriptionViewModel(
    private val subscriptionRepository: SubscriptionRepository,
    private val workspaceRepository: WorkspaceRepository
) : ViewModel() {

    private val _userWorkspaces = MutableStateFlow<List<Workspace>>(emptyList())
    val userWorkspaces: StateFlow<List<Workspace>> = _userWorkspaces.asStateFlow()

    private val _selectedPlan = MutableStateFlow<PlanResponse?>(null)
    val selectedPlan: StateFlow<PlanResponse?> = _selectedPlan.asStateFlow()

    private val _availablePlans = MutableStateFlow<List<PlanResponse>>(emptyList())
    val availablePlans: StateFlow<List<PlanResponse>> = _availablePlans.asStateFlow()

    init {
        loadUserWorkspaces()
        loadAvailablePlans()
    }

    private fun loadUserWorkspaces() {
        viewModelScope.launch {
            try {
                val workspaces = workspaceRepository.getUserWorkspaces()
                _userWorkspaces.value = workspaces
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun loadAvailablePlans() {
        viewModelScope.launch {
            try {
                val plans = subscriptionRepository.getAvailablePlans()
                _availablePlans.value = plans
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun selectPlan(plan: PlanResponse) {
        _selectedPlan.value = plan
    }

    fun calculatePriceWithDiscount(
        plan: PlanResponse,
        billingCycle: BillingCycle
    ): BigDecimal {
        val workspaceCount = _userWorkspaces.value.size
        val discount = plan.multiWorkspaceDiscount
        val basePrice = plan.monthlyPriceInr

        // Apply multi-workspace discount
        val pricePerWorkspace = if (workspaceCount >= discount.minWorkspaces && discount.isAvailable) {
            basePrice * (100 - discount.discountPercent).toBigDecimal() / 100.toBigDecimal()
        } else {
            basePrice
        }

        val monthlyTotal = pricePerWorkspace * workspaceCount.toBigDecimal()

        // Apply billing cycle discount
        return when (billingCycle) {
            BillingCycle.MONTHLY -> monthlyTotal
            BillingCycle.ANNUAL -> {
                val annualTotal = monthlyTotal * 12.toBigDecimal()
                annualTotal * 0.9.toBigDecimal() // 10% annual discount
            }
        }
    }

    suspend fun initiatePurchase(
        planCode: String,
        billingCycle: BillingCycle
    ): Result<InitiatePurchaseResponse> {
        return try {
            // Backend automatically calculates discount based on workspace count
            val response = subscriptionRepository.initiatePurchase(
                planCode = planCode,
                billingCycle = billingCycle,
                currency = "INR"
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## User Education

### 1. Onboarding Tooltip

Show a tooltip when users first see the pricing page:

```kotlin
@Composable
fun MultiWorkspaceDiscountTooltip() {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Column {
                    Text(
                        text = "💡 Save with Multiple Workspaces!",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Get 20% off when you have 3 or more workspaces")
                }
            }
        },
        state = rememberTooltipState()
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Discount info"
        )
    }
}
```

### 2. Info Dialog

Detailed explanation dialog:

```kotlin
@Composable
fun DiscountInfoDialog(
    discount: MultiWorkspaceDiscountResponse,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Multi-Workspace Discount") },
        text = {
            Column {
                Text(
                    text = "How it works:",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text("• Create ${discount.minWorkspaces} or more workspaces")
                Text("• Get ${discount.discountPercent}% off on your subscription")
                Text("• Discount applies to all your workspaces")
                Text("• Stacks with annual billing discount")

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Example:",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Text("3 workspaces × ₹500 = ₹1,500/month")
                Text("With 20% discount = ₹1,200/month")
                Text("You save ₹300/month! 🎉", color = MaterialTheme.colors.secondary)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!")
            }
        }
    )
}
```

---

## Testing Checklist

### UI Testing

- [ ] Discount badge displays correctly on plan cards
- [ ] Pricing calculator updates when workspace count changes
- [ ] Confirmation screen shows correct discount breakdown
- [ ] Info tooltips and dialogs display properly
- [ ] Empty state handling (0 workspaces)

### Integration Testing

- [ ] API returns `multiWorkspaceDiscount` field
- [ ] Backend correctly calculates discounted price
- [ ] Plan change reflects discount immediately
- [ ] Subscription activation applies discount
- [ ] Workspace creation triggers discount eligibility check

### Edge Cases

- [ ] User with exactly minimum workspaces (e.g., 3)
- [ ] User with 1 workspace (no discount)
- [ ] Free plan (no discount applicable)
- [ ] Plan without discount configured (discountPercent = 0)
- [ ] Multiple workspace creation in quick succession

---

## Analytics Events

Track discount-related user behavior:

```kotlin
// When user views pricing with discount eligibility
analyticsTracker.trackEvent(
    event = "discount_viewed",
    properties = mapOf(
        "plan_code" to planCode,
        "workspace_count" to workspaceCount,
        "is_eligible" to isEligible,
        "discount_percent" to discountPercent
    )
)

// When discount is applied to purchase
analyticsTracker.trackEvent(
    event = "discount_applied",
    properties = mapOf(
        "plan_code" to planCode,
        "workspace_count" to workspaceCount,
        "discount_percent" to discountPercent,
        "savings_amount" to savingsAmount
    )
)

// When user creates workspace to reach discount threshold
analyticsTracker.trackEvent(
    event = "workspace_created_for_discount",
    properties = mapOf(
        "previous_count" to previousCount,
        "new_count" to newCount,
        "discount_unlocked" to (newCount >= minWorkspaces)
    )
)
```

---

## Notes

### Backend Handles All Calculations

- **Important**: The mobile app should display estimated prices for UX, but the backend performs the authoritative calculation
- Always use the price returned from `POST /api/v1/subscription/initiate-purchase` for actual billing
- This ensures consistency and prevents pricing discrepancies

### Workspace Count Caching

- Cache the user's workspace count locally
- Refresh when:
  - App launches
  - User creates/deletes workspace
  - User navigates to subscription screen
  - Pull-to-refresh on workspace list

### Discount Changes

- If admin changes discount percentages, plans API will return updated values
- No app update required
- Consider showing a banner: "New discount available! Refresh to see latest pricing"

---

## Support & Questions

For backend API questions or issues:
- Check `/api/v1/subscription/plans` response structure
- Verify workspace count via `/api/v1/workspaces` endpoint
- Test purchase flow in staging environment first

For UI/UX questions:
- Refer to Material Design 3 guidelines
- Follow existing pricing screen patterns
- Maintain consistency with web app design
