# iOS Firebase Phone Auth - Complete Fix (October 2025)

## Problems Fixed

This document describes the complete fix for Firebase Phone Authentication on iOS, addressing **two critical issues**:

### Issue 1: Fatal Crash - Missing URL Scheme
**Error**: `FirebaseAuth/PhoneAuthProvider.swift:109: Fatal error: Unexpectedly found nil while implicitly unwrapping an Optional value`

**Root Cause**: Missing custom URL scheme in `Info.plist` for OAuth callbacks.

### Issue 2: APNs Notification Handling Error
**Error**: "If app delegate swizzling is disabled, remote notification received by UIApplicationDelegate need to be forwarded to FirebaseAuth's canHandleNotification method"

**Root Cause**: AppDelegate not forwarding remote notifications to Firebase.

---

## How Firebase Phone Auth Works on iOS

Firebase Phone Authentication on iOS can work in **two modes**:

### Mode 1: Silent APNs Push (Production & Device)
1. User enters phone number
2. Firebase sends verification request to backend
3. Firebase backend sends **silent push notification** to device with verification code
4. APNs delivers notification to app
5. **AppDelegate forwards notification to Firebase** ← This was missing
6. Firebase auto-verifies without user interaction
7. User authenticated

### Mode 2: reCAPTCHA Fallback (Simulator & Development)
1. User enters phone number
2. Firebase opens reCAPTCHA in Safari/SFSafariViewController
3. User completes reCAPTCHA
4. **Safari redirects back to app using custom URL scheme** ← This was missing
5. Firebase sends SMS with verification code
6. User enters code manually
7. User authenticated

**Both modes were broken** - URL scheme was missing, and notification handling was not implemented.

---

## Complete Fix Applied

### Fix 1: Info.plist - URL Scheme Configuration

**File**: `iosApp/iosApp/Info.plist`

**Added**:
- Standard iOS bundle configuration keys
- `CFBundleURLTypes` for Firebase OAuth callback
- URL scheme: `app-1-682032206651-ios-d4ddd669b68a2e38a55d1e` (based on GOOGLE_APP_ID)

**What this enables**:
- reCAPTCHA flow can redirect back to app after user verification
- Firebase can complete the authentication handshake

### Fix 2: AppDelegate - Firebase Integration & APNs Handling

**File**: `iosApp/iosApp/AppDelegate.swift`

**Changes Made**:

#### 1. Firebase Initialization (Launch)
```swift
func application(_ application: UIApplication,
                 didFinishLaunchingWithOptions launchOptions: ...) -> Bool {
    // Initialize Firebase FIRST
    if FirebaseApp.app() == nil {
        FirebaseApp.configure()
    }

    // Register for notifications
    UNUserNotificationCenter.current().delegate = self
    application.registerForRemoteNotifications()

    // Then initialize KMP
    KMPInitializerKt.onDidFinishLaunchingWithOptions()
}
```

#### 2. APNs Token Registration
```swift
func application(_ application: UIApplication,
                 didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
    // Forward token to Firebase - CRITICAL
    Auth.auth().setAPNSToken(deviceToken, type: .unknown)
}
```

#### 3. Remote Notification Forwarding - **THE KEY FIX**
```swift
func application(_ application: UIApplication,
                 didReceiveRemoteNotification userInfo: ...,
                 fetchCompletionHandler completionHandler: ...) {
    // Forward to Firebase - THIS FIXES THE ERROR
    if Auth.auth().canHandleNotification(userInfo) {
        completionHandler(.noData)
        return
    }
}
```

#### 4. UNUserNotificationCenterDelegate
- Handles notifications in foreground
- Handles user taps on notifications
- All notifications forwarded to Firebase first

**What this enables**:
- Silent push notifications work for phone verification
- APNs tokens properly registered with Firebase
- Notifications forwarded to Firebase for auto-verification
- Development simulator falls back to reCAPTCHA gracefully

### Fix 3: Kotlin iOS Implementation Update

**File**: `composeApp/src/iosMain/kotlin/com/ampairs/auth/firebase/FirebaseAuthProvider.ios.kt`

**Change**: Added check to avoid double-initialization since Firebase is now initialized in AppDelegate.

```kotlin
actual suspend fun initialize(): FirebaseAuthResult<Unit> {
    if (FIRApp.defaultApp() == null) {
        println("FirebaseAuthProvider: Firebase not initialized, configuring now")
        FIRApp.configure()
    } else {
        println("FirebaseAuthProvider: Firebase already initialized by AppDelegate")
    }
    return FirebaseAuthResult.Success(Unit)
}
```

---

## Testing the Fix

### Test Environment Setup

1. **iOS Simulator** (Development):
   - APNs not available (expected)
   - Firebase falls back to reCAPTCHA
   - URL scheme enables callback after reCAPTCHA

2. **Physical Device** (Real Testing):
   - APNs available
   - Silent push verification works
   - Faster authentication flow

### Expected Behavior After Fix

#### On iOS Simulator:
1. User enters phone number
2. App shows notification permission dialog (first time)
3. Console log: "AppDelegate: Failed to register for remote notifications" ← **Normal for simulator**
4. Console log: "AppDelegate: Running on simulator - APNs not available, will use reCAPTCHA"
5. Safari opens with reCAPTCHA
6. User completes reCAPTCHA
7. Safari redirects back to app using URL scheme
8. SMS sent to phone
9. User enters code
10. ✅ Authentication successful

#### On Physical Device:
1. User enters phone number
2. App shows notification permission dialog (first time)
3. Console log: "AppDelegate: APNs token registered"
4. Silent push notification received
5. Console log: "AppDelegate: Notification handled by Firebase Auth"
6. ✅ Auto-verification - user authenticated without manual code entry

### Troubleshooting Console Logs

**Good Logs** (Working):
```
AppDelegate: didFinishLaunchingWithOptions - Starting initialization
AppDelegate: Firebase configured successfully
AppDelegate: Notification authorization granted: true
AppDelegate: APNs token registered (device) OR Failed to register (simulator)
FirebaseAuthProvider: Firebase already initialized by AppDelegate
AppDelegate: Notification handled by Firebase Auth
```

**Problem Logs** (Issues):
```
Firebase not initialized
No root view controller
Notification not handled by Firebase
```

---

## File Changes Summary

### Modified Files:
1. ✅ `iosApp/iosApp/Info.plist` - Added URL scheme and bundle config
2. ✅ `iosApp/iosApp/AppDelegate.swift` - Complete Firebase integration with APNs
3. ✅ `composeApp/src/iosMain/kotlin/com/ampairs/auth/firebase/FirebaseAuthProvider.ios.kt` - Avoid double init

### Required Files (Already Present):
- ✅ `iosApp/Firebase/GoogleService-Info-Development.plist`
- ✅ `iosApp/Firebase/GoogleService-Info-Production.plist`
- ✅ Firebase SDK added via CocoaPods

---

## Why This Fix Works

### Problem 1: URL Scheme
Firebase Phone Auth uses reCAPTCHA as a fallback when APNs is unavailable (simulator, first-time device). The reCAPTCHA flow requires:
- User verification in Safari
- **Callback to app via custom URL scheme** ← This was missing
- Without URL scheme: Fatal crash when Firebase checks for registration

### Problem 2: APNs Notification Handling
Firebase Phone Auth on real devices uses silent push for verification:
- Firebase sends verification via APNs
- App receives notification
- **App must forward notification to Firebase** ← This was missing
- Without forwarding: Error message about swizzling/forwarding

---

## Additional Configuration (Optional)

### For Production Deployment

When deploying to TestFlight or App Store, you'll need:

1. **APNs Authentication Key** (Recommended) OR **APNs Certificate**
   - Go to [Apple Developer Portal](https://developer.apple.com/)
   - Certificates, Identifiers & Profiles → Keys
   - Create new key with APNs enabled
   - Download `.p8` file

2. **Upload to Firebase Console**
   - Firebase Console → Project Settings → Cloud Messaging
   - iOS app configuration → APNs Authentication Key
   - Upload the `.p8` file
   - Enter Key ID and Team ID

3. **Enable Background Modes** (Already in capabilities if added earlier)
   - Xcode → Target → Signing & Capabilities
   - Background Modes → Remote notifications

### Development vs Production

| Environment | APNs | Verification Method | Speed |
|-------------|------|-------------------|-------|
| Simulator | ❌ Not available | reCAPTCHA + Manual SMS | Slower |
| Debug Device | ✅ Available | Silent push (auto) | Fast |
| TestFlight | ✅ Available | Silent push (auto) | Fast |
| Production | ✅ Available | Silent push (auto) | Fast |

---

## Verification Checklist

After applying all fixes, verify:

- [ ] App builds successfully in Xcode
- [ ] No crash when clicking "Send OTP" on login screen
- [ ] Console shows Firebase initialization logs
- [ ] Notification permission dialog appears (first time)
- [ ] On simulator: reCAPTCHA opens in Safari
- [ ] On simulator: Safari redirects back to app after reCAPTCHA
- [ ] SMS received with verification code
- [ ] Code verification completes successfully
- [ ] User authenticated and navigated to home screen

---

## Common Issues & Solutions

### Issue: App still crashes at login

**Check**:
1. `Info.plist` has `CFBundleURLTypes` configured
2. URL scheme matches format: `app-1-{PROJECT_NUMBER}-ios-{APP_ID_SUFFIX}`
3. Firebase initialized in AppDelegate before phone auth call

### Issue: Notification permission never appears

**Check**:
1. Reset simulator: Device → Erase All Content and Settings
2. Delete app from device and reinstall
3. Check iOS Settings → Notifications → [App Name]

### Issue: reCAPTCHA doesn't redirect back

**Check**:
1. URL scheme in `Info.plist` exactly matches Google App ID format
2. No typos in bundle identifier
3. Safari can access the callback URL

### Issue: "Invalid verification code" on all codes

**Possible causes**:
1. Using production phone number with test configuration
2. Firebase quota exceeded (unlikely in dev)
3. Check Firebase Console → Authentication → Phone for test numbers

---

## Related Documentation

- `IOS_URL_SCHEME_FIX.md` - Original URL scheme investigation
- `IOS_FIREBASE_XCODE_SETUP.md` - Initial Firebase setup guide
- [Firebase iOS Phone Auth](https://firebase.google.com/docs/auth/ios/phone-auth)
- [APNs Setup Guide](https://firebase.google.com/docs/cloud-messaging/ios/client)

---

## Technical Architecture

### Firebase Phone Auth Flow Diagram

```
User enters phone
       ↓
iOS attempts APNs verification
       ↓
    ┌─────┴─────┐
    ↓           ↓
APNs        No APNs
Available   (Simulator)
    ↓           ↓
Silent      reCAPTCHA
Push        in Safari
    ↓           ↓
AppDelegate URL Scheme
forwards    callback
to Firebase     ↓
    ↓       Firebase
Auto-verify sends SMS
    ↓           ↓
    └─────┬─────┘
          ↓
    User verifies
          ↓
  Authenticated
```

### Code Integration Points

1. **App Launch** (`AppDelegate.didFinishLaunchingWithOptions`)
   - Firebase initialization
   - APNs registration
   - Notification delegate setup

2. **Login Flow** (KMP Compose)
   - User enters phone
   - Kotlin calls `FirebaseAuthProvider.sendVerificationCode()`
   - Kotlin delegates to native iOS SDK

3. **Notification Handling** (`AppDelegate`)
   - APNs token received → Forward to Firebase
   - Remote notification received → Check if Firebase auth → Forward

4. **OAuth Callback** (iOS System)
   - Safari completes reCAPTCHA
   - Redirect to `{URL_SCHEME}://` URL
   - iOS routes to app
   - Firebase completes flow

---

**Status**: ✅ **Complete fix applied** - Both URL scheme and APNs notification handling implemented.

**Testing**: Ready for testing on iOS Simulator and physical devices.

**Created**: October 26, 2025
**Related Issues**:
- Fatal crash at PhoneAuthProvider.swift:109
- APNs notification forwarding error