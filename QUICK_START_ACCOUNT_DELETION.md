# Quick Start Guide - Account Deletion Feature

## 📚 Documentation Index

### Backend Implementation
- **✅ COMPLETE** - Already implemented in `ampairs_service` module
- **API Endpoints:** `POST /api/v1/account/delete-request`, `POST /api/v1/account/delete-cancel`, `GET /api/v1/account/delete-status`
- **Full Docs:** [`ACCOUNT_DELETION_SUMMARY.md`](./ACCOUNT_DELETION_SUMMARY.md)
- **Technical Details:** [`auth/ACCOUNT_DELETION.md`](./auth/ACCOUNT_DELETION.md)

### Frontend Implementation Guides
1. **Angular Web App:** [`ampairs-web/ACCOUNT_DELETION_IMPLEMENTATION.md`](./ampairs-web/ACCOUNT_DELETION_IMPLEMENTATION.md)
2. **KMP Mobile App:** [`ampairs-mp-app/ACCOUNT_DELETION_IMPLEMENTATION.md`](./ampairs-mp-app/ACCOUNT_DELETION_IMPLEMENTATION.md)

---

## 🚀 Quick Implementation Steps

### For Angular Web App (4-6 hours)

1. **Create Models** (`src/app/models/account-deletion.model.ts`)
   ```typescript
   export interface AccountDeletionRequest { ... }
   export interface AccountDeletionResponse { ... }
   ```

2. **Create Service** (`src/app/services/account-deletion.service.ts`)
   ```typescript
   @Injectable({ providedIn: 'root' })
   export class AccountDeletionService { ... }
   ```

3. **Create Components**
   - `delete-account.component.ts` - Main screen
   - `delete-confirmation-dialog.component.ts` - Confirmation
   - `blocking-workspaces-dialog.component.ts` - Blocking workspaces

4. **Add Routes** (in `app-routing.module.ts`)
   ```typescript
   { path: 'settings/delete-account', component: DeleteAccountComponent }
   ```

5. **Add to Settings Menu**
   ```html
   <mat-list-item routerLink="/settings/delete-account">
     <mat-icon color="warn">delete_forever</mat-icon>
     Delete Account
   </mat-list-item>
   ```

**Complete code examples in:** `ampairs-web/ACCOUNT_DELETION_IMPLEMENTATION.md`

---

### For KMP Mobile App (6-8 hours)

1. **Create Data Models** (`commonMain/.../model/AccountDeletionModels.kt`)
   ```kotlin
   @Serializable
   data class AccountDeletionRequest(...)
   ```

2. **Create API Client** (`commonMain/.../api/AccountDeletionApi.kt`)
   ```kotlin
   class AccountDeletionApi(httpClient, baseUrl) { ... }
   ```

3. **Create Repository** (`commonMain/.../repository/AccountDeletionRepository.kt`)
   ```kotlin
   class AccountDeletionRepository(api, tokenProvider) { ... }
   ```

4. **Create Use Cases**
   - `RequestAccountDeletionUseCase.kt`
   - `CancelAccountDeletionUseCase.kt`
   - `GetAccountDeletionStatusUseCase.kt`

5. **Create ViewModel** (`commonMain/.../DeleteAccountViewModel.kt`)
   ```kotlin
   class DeleteAccountViewModel(...) : ScreenModel { ... }
   ```

6. **Create UI** (`commonMain/.../DeleteAccountScreen.kt`)
   ```kotlin
   class DeleteAccountScreen : Screen { ... }
   ```

7. **Configure DI** (Koin)
   ```kotlin
   val accountDeletionModule = module { ... }
   ```

8. **Add Navigation**
   ```kotlin
   ListItem(
     headlineContent = { Text("Delete Account") },
     onClick = { navigator.push(DeleteAccountScreen()) }
   )
   ```

**Complete code examples in:** `ampairs-mp-app/ACCOUNT_DELETION_IMPLEMENTATION.md`

---

## 🔌 API Integration

### Base URL
```
https://yourdomain.com/api/v1/account
```

### Endpoints

#### 1. Request Account Deletion
```http
POST /api/v1/account/delete-request
Authorization: Bearer {jwt_token}
Content-Type: application/json

{
  "confirmed": true,
  "reason": "Optional reason"
}
```

**Response (Success):**
```json
{
  "success": true,
  "data": {
    "user_id": "usr_abc123",
    "deletion_requested": true,
    "deleted_at": "2025-01-15T10:00:00Z",
    "deletion_scheduled_for": "2025-02-14T10:00:00Z",
    "days_until_permanent_deletion": 30,
    "message": "Account deletion requested successfully...",
    "can_restore": true
  }
}
```

**Response (Blocked):**
```json
{
  "success": true,
  "data": {
    "user_id": "usr_abc123",
    "deletion_requested": false,
    "message": "Cannot delete account: You are the sole owner of 2 workspace(s)...",
    "blocking_workspaces": [
      {
        "workspace_id": "wks_xyz",
        "workspace_name": "Acme Corp",
        "workspace_slug": "acme-corp",
        "member_count": 5
      }
    ]
  }
}
```

#### 2. Cancel Account Deletion
```http
POST /api/v1/account/delete-cancel
Authorization: Bearer {jwt_token}
```

#### 3. Get Deletion Status
```http
GET /api/v1/account/delete-status
Authorization: Bearer {jwt_token}
```

---

## 🎨 UI/UX Flow

### User Journey

```
Settings
  └─→ Delete Account
       │
       ├─→ [Active State]
       │   ├─ Show warnings
       │   ├─ Show data deletion list
       │   ├─ Optional reason input
       │   └─ Delete button
       │        │
       │        └─→ Confirmation Dialog
       │             ├─ Type "DELETE" to confirm
       │             └─→ API Call
       │                  │
       │                  ├─→ Success: Logout
       │                  └─→ Blocked: Show workspaces
       │
       └─→ [Pending Deletion State]
           ├─ Show countdown (days remaining)
           ├─ Show deletion date
           └─ Restore button
                │
                └─→ Confirmation Dialog
                     ├─ Type "RESTORE" to confirm
                     └─→ API Call → Account reactivated
```

---

## ⚠️ Important Implementation Notes

### Security
- ✅ All endpoints require JWT authentication
- ✅ User can only delete their own account
- ✅ Tokens are revoked immediately upon deletion request

### Workspace Ownership Validation
- ✅ Backend validates sole ownership before allowing deletion
- ✅ Frontend displays blocking workspaces with details
- ✅ User must transfer ownership or delete workspace first

### Data Privacy
- ✅ Immediate anonymization on deletion request
- ✅ 30-day grace period for restoration
- ✅ Automatic permanent deletion after grace period

### Error Handling
- Handle network errors gracefully
- Show user-friendly error messages
- Allow retry on failures
- Validate "DELETE"/"RESTORE" confirmation text

---

## 🧪 Testing Checklist

### Backend (✅ Complete)
- [x] Request deletion with active account
- [x] Request deletion when sole owner (blocked)
- [x] Request deletion after transferring ownership
- [x] Cancel deletion within grace period
- [x] Attempt to cancel after grace period (error)
- [x] Verify tokens revoked
- [x] Verify workspace memberships deactivated
- [x] Scheduled job runs daily at 2 AM
- [x] Permanent deletion after 30 days

### Frontend (To Do)
- [ ] Delete account button visible in settings
- [ ] Warning dialog displays correctly
- [ ] Blocking workspaces dialog shows all sole-owner workspaces
- [ ] Success message shows grace period days
- [ ] User logged out immediately after deletion
- [ ] Restore account flow works correctly
- [ ] Loading states displayed properly
- [ ] Error handling works for all edge cases
- [ ] Responsive design on mobile
- [ ] Accessibility (keyboard nav, screen readers)

---

## 📱 Google Play Store Configuration

**Required for Data Safety Section:**

**Delete Account URL:**
```
https://yourdomain.com/delete-account.html
```

This page is already created at:
```
ampairs_service/src/main/resources/static/delete-account.html
```

**Data Safety Answers:**
- ✅ Data collection: **Yes**
- ✅ Data encrypted in transit: **Yes**
- ✅ Account creation methods: **Username, password, and other authentication** (phone + OTP)
- ✅ Users can request deletion: **Yes**
- ✅ Deletion URL: **https://yourdomain.com/delete-account.html**

---

## 💡 Key Features

✅ **Soft delete** - 30-day grace period
✅ **Workspace protection** - Blocks if sole owner
✅ **Immediate anonymization** - Privacy protection
✅ **Token revocation** - Instant logout
✅ **Scheduled cleanup** - Automatic permanent deletion
✅ **Restoration option** - Cancel within grace period
✅ **GDPR/CCPA compliant**
✅ **Google Play compliant**

---

## 🆘 Need Help?

### Documentation Links
- **Backend API Reference:** [`ACCOUNT_DELETION_SUMMARY.md`](./ACCOUNT_DELETION_SUMMARY.md)
- **Angular Web Guide:** [`ampairs-web/ACCOUNT_DELETION_IMPLEMENTATION.md`](./ampairs-web/ACCOUNT_DELETION_IMPLEMENTATION.md)
- **KMP Mobile Guide:** [`ampairs-mp-app/ACCOUNT_DELETION_IMPLEMENTATION.md`](./ampairs-mp-app/ACCOUNT_DELETION_IMPLEMENTATION.md)
- **Technical Deep Dive:** [`auth/ACCOUNT_DELETION.md`](./auth/ACCOUNT_DELETION.md)

### File Locations
```
Backend:
├── ampairs_service/src/main/kotlin/com/ampairs/account/
│   ├── controller/AccountDeletionController.kt
│   ├── service/AccountDeletionService.kt
│   ├── service/AccountDeletionScheduler.kt
│   └── dto/*.kt
├── auth/src/main/kotlin/com/ampairs/user/model/User.kt
└── auth/src/main/resources/db/migration/**/V1.0.16__add_user_deletion_fields.sql

Public Page:
└── ampairs_service/src/main/resources/static/delete-account.html
```

---

## ✅ Status Summary

| Component | Status | Time Estimate |
|-----------|--------|---------------|
| Backend API | ✅ Complete | N/A |
| Database Migration | ✅ Complete | N/A |
| Public Deletion Page | ✅ Complete | N/A |
| Documentation | ✅ Complete | N/A |
| Angular Web App | 📝 Ready to implement | 4-6 hours |
| KMP Mobile App | 📝 Ready to implement | 6-8 hours |
| Google Play Store | ⏳ Pending deployment | 30 mins |

---

**Last Updated:** 2025-01-15
**Version:** 1.0
**Ready for Frontend Implementation:** ✅ Yes
