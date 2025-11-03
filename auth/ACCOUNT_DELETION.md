# Account Deletion Feature

## Overview

The account deletion feature provides users with the ability to request deletion of their Ampairs account and associated personal data, in compliance with GDPR, CCPA, and Google Play Data Safety requirements.

## Architecture

### Deletion Strategy: Soft Delete with Grace Period

- **Soft Delete**: Account is marked for deletion but data is retained for 30 days
- **Grace Period**: 30-day window during which users can restore their account
- **Permanent Deletion**: Automated scheduled job removes data after grace period expires

### Key Components

1. **User Entity** (`User.kt`)
   - `deleted: Boolean` - Flag indicating account is marked for deletion
   - `deletedAt: Instant?` - Timestamp when deletion was requested
   - `deletionScheduledFor: Instant?` - Scheduled permanent deletion date (30 days from request)
   - `deletionReason: String?` - Optional reason provided by user

2. **AccountDeletionService** (`AccountDeletionService.kt`)
   - Handles deletion requests with workspace ownership validation
   - Manages soft delete and account restoration
   - Coordinates token revocation and membership deactivation

3. **AccountDeletionScheduler** (`AccountDeletionScheduler.kt`)
   - Daily scheduled job (runs at 2 AM)
   - Permanently deletes accounts past grace period

4. **REST API Endpoints** (in `UserController.kt`)
   - `POST /api/v1/user/delete-request` - Request account deletion
   - `POST /api/v1/user/delete-cancel` - Cancel deletion request
   - `GET /api/v1/user/delete-status` - Check deletion status

5. **Public Deletion Page**
   - Static HTML page: `/delete-account.html`
   - Required by Google Play Store Data Safety requirements
   - Provides instructions and deep links to mobile app

## Account Deletion Flow

### 1. Request Deletion

```kotlin
POST /api/v1/user/delete-request
Content-Type: application/json

{
  "confirmed": true,
  "reason": "No longer need the service"  // Optional
}
```

**Validation Steps:**
1. Check if user is sole owner of any workspace(s)
2. If sole owner exists, block deletion and return list of blocking workspaces
3. If no blocking workspaces:
   - Mark account for deletion
   - Set grace period (30 days)
   - Anonymize user data immediately
   - Revoke all authentication tokens
   - Deactivate workspace memberships (non-owner roles)

**Response:**
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

**Blocked Response Example:**
```json
{
  "success": true,
  "data": {
    "userId": "usr_abc123",
    "deletionRequested": false,
    "message": "Cannot delete account: You are the sole owner of 2 workspace(s). Please transfer ownership or delete these workspaces first.",
    "blockingWorkspaces": [
      {
        "workspaceId": "wks_xyz789",
        "workspaceName": "Acme Corp",
        "workspaceSlug": "acme-corp",
        "memberCount": 5
      }
    ]
  }
}
```

### 2. Cancel Deletion (Restore Account)

```kotlin
POST /api/v1/user/delete-cancel
```

**Requirements:**
- Must be within 30-day grace period
- Account must be marked for deletion

**Response:**
```json
{
  "success": true,
  "data": {
    "userId": "usr_abc123",
    "deletionRequested": false,
    "message": "Account restoration successful. Your account has been reactivated.",
    "canRestore": false
  }
}
```

### 3. Check Deletion Status

```kotlin
GET /api/v1/user/delete-status
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

## Data Handling

### Data Deleted During Soft Delete (Immediate Anonymization)

✅ **Anonymized Immediately:**
- First name → "Deleted"
- Last name → "User"
- Email → `null`
- Phone → "0000000000"
- Username → "deleted_{uid}"
- Password → `null`
- Firebase UID → `null`

✅ **Revoked/Deleted:**
- All authentication tokens (JWT)
- Active sessions across all devices

✅ **Deactivated:**
- Workspace memberships (non-owner roles)

### Data Deleted During Permanent Deletion

🗑️ **Permanently Removed:**
- User entity (entire record)
- All tokens
- All workspace memberships
- Associated session data

### Data Retained (Not Deleted)

📋 **Retained for Business Continuity:**
- Workspace data (if user was not sole owner)
- Audit logs (with anonymized user reference)
- Transaction history (with anonymized user reference)

## Workspace Ownership Rules

### Sole Owner Blocking

**Problem:** Deleting a sole owner would orphan the workspace and all its data.

**Solution:** Block account deletion if user is the sole OWNER of any workspace.

**User Actions Required:**
1. **Transfer Ownership:**
   - Promote another workspace member to OWNER role
   - Then user can proceed with account deletion

2. **Delete Workspace:**
   - User deletes the entire workspace (and all its data)
   - Then user can proceed with account deletion

**Code Logic:**
```kotlin
// Count OWNER role members in workspace
val ownerCount = workspaceMemberRepository.countByWorkspaceIdAndRoleAndIsActiveTrue(
    workspaceId,
    WorkspaceRole.OWNER
)

// Check if current user is owner
val userMembership = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
val isOwner = userMembership.map { it.role == WorkspaceRole.OWNER }.orElse(false)

// Block deletion if sole owner
if (isOwner && ownerCount == 1L) {
    // Add to blocking workspaces list
}
```

## Scheduled Permanent Deletion

### Scheduler Configuration

```kotlin
@Scheduled(cron = "0 0 2 * * *")  // Daily at 2 AM
fun executeScheduledAccountDeletions()
```

### Process Flow

1. Find accounts where `deletionScheduledFor < NOW()`
2. For each account:
   - Delete all authentication tokens
   - Delete all workspace memberships
   - Delete user entity
3. Log deletion results (success/failure counts)

### Error Handling

- Individual account deletion failures don't halt entire job
- Errors are logged for manual investigation
- Failed accounts will be retried in next scheduled run

## Database Migrations

### MySQL Migration

```sql
-- V1.0.16__add_user_deletion_fields.sql
ALTER TABLE app_user
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMP NULL,
    ADD COLUMN deletion_scheduled_for TIMESTAMP NULL,
    ADD COLUMN deletion_reason VARCHAR(500) NULL;

CREATE INDEX idx_app_user_deleted ON app_user(deleted);
CREATE INDEX idx_app_user_deletion_scheduled ON app_user(deletion_scheduled_for)
    WHERE deletion_scheduled_for IS NOT NULL;
```

### PostgreSQL Migration

```sql
-- V1.0.16__add_user_deletion_fields.sql
ALTER TABLE app_user
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN deleted_at TIMESTAMP NULL,
    ADD COLUMN deletion_scheduled_for TIMESTAMP NULL,
    ADD COLUMN deletion_reason VARCHAR(500) NULL;

CREATE INDEX idx_app_user_deleted ON app_user(deleted);
CREATE INDEX idx_app_user_deletion_scheduled ON app_user(deletion_scheduled_for)
    WHERE deletion_scheduled_for IS NOT NULL;
```

## Google Play Data Safety Compliance

### Requirements

✅ **Deletion URL:** `https://yourdomain.com/delete-account.html`
- Prominently features deletion steps
- Specifies data types deleted/retained
- Accessible without app login

✅ **In-App Deletion:** Mobile app provides account deletion UI

✅ **Data Transparency:**
- Clear communication of 30-day grace period
- List of data types deleted
- Explanation of workspace ownership requirements

### Store Listing Configuration

**Delete Account URL:**
```
https://yourdomain.com/delete-account.html
```

**Data Collection Disclosure:**
- ✅ All user data encrypted in transit (HTTPS)
- ✅ Users can request account deletion
- ✅ 30-day grace period with restoration option
- ✅ Permanent deletion after grace period

## Mobile App Implementation

### Account Deletion Screen

**Location:** Settings → Account Settings → Delete Account

**UI Flow:**
1. Display warning dialog with consequences
2. List blocking workspaces (if any)
3. Require confirmation checkbox
4. Optional: Ask for deletion reason
5. Call API: `POST /api/v1/user/delete-request`
6. Show success/error message with grace period info

### Sample Code (Conceptual)

```kotlin
// Android/iOS - Kotlin Multiplatform
suspend fun requestAccountDeletion(reason: String? = null) {
    val request = AccountDeletionRequest(
        confirmed = true,
        reason = reason
    )

    val response = apiClient.post<AccountDeletionResponse>(
        "/api/v1/user/delete-request",
        request
    )

    if (response.deletionRequested) {
        // Show success dialog with grace period info
        showDeletionConfirmation(response.daysUntilPermanentDeletion)

        // Logout user immediately
        logout()
    } else {
        // Show blocking workspaces dialog
        showBlockingWorkspacesDialog(response.blockingWorkspaces)
    }
}
```

## Security Considerations

### Authentication

- All deletion endpoints require authentication (JWT token)
- User can only delete their own account
- Tokens are revoked immediately upon deletion request

### Validation

- Workspace ownership checked using native SQL (bypasses tenant filtering)
- Grace period enforced at database level
- Scheduled job validates grace period before permanent deletion

### Audit Trail

- All deletion requests logged with timestamp
- Workspace membership deactivation recorded
- Failed permanent deletions logged for investigation

## Testing

### Manual Testing Checklist

- [ ] Request deletion with active account
- [ ] Request deletion when sole owner (should be blocked)
- [ ] Request deletion after transferring ownership (should succeed)
- [ ] Cancel deletion within grace period (should restore)
- [ ] Attempt to cancel after grace period (should fail)
- [ ] Verify tokens revoked after deletion request
- [ ] Verify workspace memberships deactivated
- [ ] Verify scheduled job runs and permanently deletes accounts

### Integration Tests

```kotlin
@Test
fun `should block deletion when user is sole owner`() {
    // Create workspace with single owner
    // Request account deletion
    // Assert deletion blocked with workspace info
}

@Test
fun `should successfully delete account when not sole owner`() {
    // Create workspace with multiple owners
    // Request account deletion
    // Assert deletion successful
    // Assert tokens revoked
}

@Test
fun `should restore account within grace period`() {
    // Request deletion
    // Cancel deletion
    // Assert account active
}
```

## Deployment Checklist

- [ ] Run database migrations (V1.0.16)
- [ ] Verify indexes created
- [ ] Deploy backend services with new code
- [ ] Verify `/delete-account.html` accessible
- [ ] Update Google Play Store with deletion URL
- [ ] Deploy mobile app update with deletion UI
- [ ] Monitor scheduled job execution
- [ ] Set up alerts for deletion job failures

## Support & Troubleshooting

### Common Issues

**Issue:** User cannot delete account (blocked by workspace ownership)

**Solution:** Guide user to transfer ownership or delete workspace

---

**Issue:** User claims grace period expired but wants restoration

**Solution:** Grace period is strictly enforced. Manual intervention required via database update (support team only)

---

**Issue:** Scheduled job failed to delete account

**Solution:** Check logs for specific error. Account will be retried in next scheduled run.

## Contact

For questions or issues related to account deletion:
- Email: support@ampairs.com
- Documentation: https://docs.ampairs.com/account-deletion

---

**Last Updated:** 2025-01-15
**Version:** 1.0
