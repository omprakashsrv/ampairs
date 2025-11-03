# Account Deletion Feature - Implementation Summary

## ✅ Implementation Complete

The account deletion feature has been successfully implemented with proper module architecture.

---

## 📁 Architecture

### Module Structure
```
ampairs_service/  (Application Layer - Orchestrator)
└── com.ampairs.account/
    ├── controller/
    │   └── AccountDeletionController.kt    ← REST API endpoints
    ├── service/
    │   ├── AccountDeletionService.kt       ← Business logic
    │   └── AccountDeletionScheduler.kt     ← Scheduled cleanup (daily 2 AM)
    └── dto/
        ├── AccountDeletionRequest.kt
        ├── AccountDeletionResponse.kt
        └── AccountDeletionStatusResponse.kt

auth/  (Domain Layer)
└── com.ampairs.user.model/
    └── User.kt                              ← Added deletion fields

workspace/  (Domain Layer)
└── (No changes - used by AccountDeletionService)
```

### Why This Architecture?

**Problem:** Originally placed in `auth` module, but needed `workspace` module imports
- ❌ `auth` → `workspace` = BAD (domain modules shouldn't depend on each other)

**Solution:** Moved to `ampairs_service` module
- ✅ `ampairs_service` → (`auth` + `workspace`) = GOOD (application layer coordinates domains)

---

## 🔌 API Endpoints

### Base URL
All account deletion endpoints are under:
```
/api/v1/account
```

### 1. Request Account Deletion
```http
POST /api/v1/account/delete-request
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "confirmed": true,
  "reason": "Optional deletion reason"
}
```

**Response (Success):**
```json
{
  "success": true,
  "data": {
    "userId": "usr_abc123",
    "deletionRequested": true,
    "deletedAt": "2025-01-15T10:00:00Z",
    "deletionScheduledFor": "2025-02-14T10:00:00Z",
    "daysUntilPermanentDeletion": 30,
    "message": "Account deletion requested successfully. Your data will be permanently deleted in 30 days.",
    "canRestore": true,
    "blockingWorkspaces": null
  }
}
```

**Response (Blocked - Sole Owner):**
```json
{
  "success": true,
  "data": {
    "userId": "usr_abc123",
    "deletionRequested": false,
    "deletedAt": null,
    "deletionScheduledFor": null,
    "daysUntilPermanentDeletion": null,
    "message": "Cannot delete account: You are the sole owner of 2 workspace(s). Please transfer ownership or delete these workspaces first.",
    "blockingWorkspaces": [
      {
        "workspaceId": "wks_xyz789",
        "workspaceName": "Acme Corp",
        "workspaceSlug": "acme-corp",
        "memberCount": 5
      }
    ],
    "canRestore": false
  }
}
```

---

### 2. Cancel Account Deletion (Restore)
```http
POST /api/v1/account/delete-cancel
Authorization: Bearer {jwt_token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": "usr_abc123",
    "deletionRequested": false,
    "deletedAt": null,
    "deletionScheduledFor": null,
    "daysUntilPermanentDeletion": null,
    "message": "Account restoration successful. Your account has been reactivated.",
    "canRestore": false
  }
}
```

---

### 3. Check Deletion Status
```http
GET /api/v1/account/delete-status
Authorization: Bearer {jwt_token}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "isDeleted": true,
    "deletedAt": "2025-01-15T10:00:00Z",
    "deletionScheduledFor": "2025-02-14T10:00:00Z",
    "daysRemaining": 28,
    "canRestore": true,
    "deletionReason": "No longer need the service",
    "statusMessage": "Your account is scheduled for deletion in 28 days"
  }
}
```

---

## 🗄️ Database Changes

### Migration: V1.0.16__add_user_deletion_fields.sql

**Added Columns to `app_user` table:**
```sql
deleted                 BOOLEAN      NOT NULL DEFAULT FALSE
deleted_at              TIMESTAMP    NULL
deletion_scheduled_for  TIMESTAMP    NULL
deletion_reason         VARCHAR(500) NULL
```

**Indexes:**
```sql
idx_app_user_deleted
idx_app_user_deletion_scheduled
```

---

## 🔒 Security & Business Rules

### Workspace Ownership Protection

**Rule:** Cannot delete account if user is **sole OWNER** of any workspace

**Reason:** Prevents orphaned workspaces with business data

**User Actions Required:**
1. **Transfer Ownership:** Promote another member to OWNER role
2. **Delete Workspace:** Remove the workspace entirely

**Allowed Deletion Scenarios:**
- ✅ User is not a workspace owner
- ✅ User is owner but workspace has multiple owners
- ✅ User is member/admin (not owner)

---

## ⏰ Deletion Flow

### 1. Request Deletion (Immediate Effects)
- Account marked for deletion (`deleted = true`)
- 30-day grace period starts
- **Immediate anonymization:**
  - Name → "Deleted User"
  - Email → `null`
  - Phone → "0000000000"
  - Username → "deleted_{uid}"
- All JWT tokens revoked
- Workspace memberships deactivated (non-owner roles)

### 2. Grace Period (30 Days)
- User cannot login (tokens revoked)
- Data retained but anonymized
- User can restore account (if they contact support or use restore link)

### 3. Permanent Deletion (After 30 Days)
- **Scheduled Job:** Runs daily at 2 AM
- **Deleted Data:**
  - User entity (entire record)
  - All authentication tokens
  - All workspace memberships
  - Session data

---

## 🌐 Google Play Store Configuration

### Data Safety Section

**"Does your app collect or share any of the required user data types?"**
- ✅ Yes

**"Is all user data encrypted in transit?"**
- ✅ Yes (HTTPS)

**"Account creation methods?"**
- ✅ Username, password, and other authentication (phone + OTP)

**"Delete account URL"**
```
https://yourdomain.com/delete-account.html
```

### Public Deletion Page

**Location:** `/ampairs_service/src/main/resources/static/delete-account.html`

**URL:** `https://yourdomain.com/delete-account.html`

**Content:**
- Instructions for deleting account via mobile app
- Data deletion details
- Grace period information
- Deep link to app settings: `ampairs://settings/delete-account`

---

## 📱 Mobile App Implementation Guide

### UI Location
```
Settings → Account Settings → Delete Account
```

### Implementation Steps

1. **Add Settings Screen Item**
```kotlin
// Settings.kt
MenuItem(
    title = "Delete Account",
    icon = Icons.Default.DeleteForever,
    onClick = { navController.navigate("delete-account") }
)
```

2. **Create Delete Account Screen**
```kotlin
@Composable
fun DeleteAccountScreen() {
    var showDialog by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }

    Column {
        Text(
            "⚠️ Warning: This action will delete your account permanently after 30 days.",
            color = Color.Red
        )

        TextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("Reason (optional)") }
        )

        Button(onClick = { showDialog = true }) {
            Text("Delete My Account")
        }
    }

    if (showDialog) {
        DeleteConfirmationDialog(
            reason = reason,
            onConfirm = { requestAccountDeletion(reason) },
            onDismiss = { showDialog = false }
        )
    }
}
```

3. **API Call Implementation**
```kotlin
suspend fun requestAccountDeletion(reason: String? = null) {
    try {
        val request = AccountDeletionRequest(
            confirmed = true,
            reason = reason
        )

        val response = apiClient.post<ApiResponse<AccountDeletionResponse>>(
            "/api/v1/account/delete-request",
            body = request
        )

        if (response.data.deletionRequested) {
            // Success - show grace period info
            showDeletionSuccess(response.data.daysUntilPermanentDeletion)

            // Logout immediately
            authService.logout()
        } else {
            // Blocked by workspace ownership
            showBlockingWorkspaces(response.data.blockingWorkspaces)
        }
    } catch (e: Exception) {
        showError(e.message)
    }
}
```

4. **Handle Blocking Workspaces**
```kotlin
@Composable
fun BlockingWorkspacesDialog(workspaces: List<WorkspaceOwnershipInfo>) {
    AlertDialog(
        title = { Text("Cannot Delete Account") },
        text = {
            Column {
                Text("You are the sole owner of these workspaces:")
                workspaces.forEach { workspace ->
                    Text("• ${workspace.workspaceName} (${workspace.memberCount} members)")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Please transfer ownership or delete these workspaces first.")
            }
        },
        onDismissRequest = { },
        confirmButton = {
            TextButton(onClick = { /* Navigate to workspaces */ }) {
                Text("Manage Workspaces")
            }
        }
    )
}
```

---

## 🧪 Testing Checklist

### Backend Tests
- [ ] Request deletion with active account (should succeed)
- [ ] Request deletion when sole owner (should be blocked)
- [ ] Request deletion after transferring ownership (should succeed)
- [ ] Cancel deletion within grace period (should restore)
- [ ] Attempt to cancel after grace period (should fail)
- [ ] Verify tokens revoked after deletion request
- [ ] Verify workspace memberships deactivated
- [ ] Verify scheduled job runs at 2 AM
- [ ] Verify permanent deletion after 30 days

### Mobile App Tests
- [ ] Delete account button visible in settings
- [ ] Warning dialog displays correctly
- [ ] Blocking workspaces dialog shows all sole-owner workspaces
- [ ] Success message shows grace period days
- [ ] User logged out immediately after deletion
- [ ] Deep link opens app to delete account screen

### Google Play Store
- [ ] Deletion URL is accessible without login
- [ ] URL displays clear deletion instructions
- [ ] URL lists data types that will be deleted
- [ ] URL specifies 30-day grace period

---

## 📊 Monitoring & Alerts

### Scheduled Job Monitoring

**Log Messages to Monitor:**
```
INFO: Starting scheduled account deletion job
INFO: Found X accounts ready for permanent deletion
INFO: Scheduled account deletion completed: X succeeded, Y failed
ERROR: Failed to permanently delete account {userId}
ERROR: Error during scheduled account deletion job
```

**Recommended Alerts:**
- Alert if scheduled job fails to run
- Alert if more than 5 account deletions fail in single run
- Daily report of accounts deleted

---

## 🔧 Troubleshooting

### Issue: User can't delete account (blocked)

**Cause:** User is sole owner of workspace(s)

**Solution:**
1. Check blocking workspaces in API response
2. Guide user to transfer ownership OR delete workspace
3. Retry account deletion

---

### Issue: Grace period expired, user wants restoration

**Cause:** 30-day window passed

**Solution:** Manual database update (support team only)
```sql
-- Check current status
SELECT uid, deleted, deleted_at, deletion_scheduled_for
FROM app_user
WHERE uid = 'usr_xxx';

-- Restore if needed (support approval required)
UPDATE app_user
SET deleted = false,
    deleted_at = NULL,
    deletion_scheduled_for = NULL,
    deletion_reason = NULL,
    active = true
WHERE uid = 'usr_xxx';
```

---

### Issue: Scheduled job didn't run

**Check:**
1. Application logs for errors
2. Verify scheduler bean is loaded: `AccountDeletionScheduler`
3. Verify cron expression: `0 0 2 * * *` (2 AM daily)
4. Check server timezone settings

---

## 📚 Documentation Links

- **Full Documentation:** `auth/ACCOUNT_DELETION.md`
- **Public Page:** `ampairs_service/src/main/resources/static/delete-account.html`
- **API Controller:** `ampairs_service/src/main/kotlin/com/ampairs/account/controller/AccountDeletionController.kt`
- **Service Logic:** `ampairs_service/src/main/kotlin/com/ampairs/account/service/AccountDeletionService.kt`

---

## 🚀 Deployment Checklist

- [ ] Run database migrations (both MySQL and PostgreSQL)
- [ ] Verify indexes created
- [ ] Deploy backend with account deletion feature
- [ ] Verify `/delete-account.html` is accessible
- [ ] Update Google Play Store with deletion URL
- [ ] Deploy mobile app with delete account UI
- [ ] Configure monitoring for scheduled job
- [ ] Set up alerts for deletion failures
- [ ] Test end-to-end deletion flow in staging
- [ ] Communicate grace period policy to users

---

## 📞 Support

For questions or issues:
- **Email:** support@ampairs.com
- **Docs:** https://docs.ampairs.com/account-deletion
- **GitHub Issues:** [Link to your repository]

---

**Last Updated:** 2025-01-15
**Version:** 1.0
**Status:** ✅ Ready for Deployment
