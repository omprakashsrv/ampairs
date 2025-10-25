# Firebase Authentication Setup Guide

This guide explains the Firebase Authentication integration for the Ampairs Mobile Application.

## ⚠️ Current Status: Stub Implementation

**Firebase Phone Authentication is currently NOT ENABLED.**

The app has a **stub implementation** of Firebase Authentication. All platforms currently use **Backend API Authentication only**. Firebase integration is planned for a future release when the GitLive Firebase KMP SDK's phone auth APIs are fully integrated.

## Overview

The app supports **Backend API Authentication**:

1. **Backend API Authentication** ✅ **ACTIVE** - Phone + OTP via Spring Boot backend server
2. **Firebase Authentication** ⏳ **PLANNED** - Phone + OTP via Firebase SDK (architecture in place, not yet functional)

### Platform Support

| Platform | Backend API | Firebase Phone Auth | Status |
|----------|-------------|---------------------|--------|
| Android  | ✅ Active | ⏳ Stub (Not Functional) | Backend API Only |
| iOS      | ✅ Active | ⏳ Stub (Not Functional) | Backend API Only |
| Desktop  | ✅ Active | ⏳ Stub (Not Functional) | Backend API Only |

## Why Stub Implementation?

The GitLive Firebase Kotlin SDK has different APIs than native Firebase SDKs:

- **Phone Authentication requires:**
  - Android: Activity context for reCAPTCHA verification
  - iOS: APNs configuration and proper lifecycle integration
  - Platform-specific callbacks and verification flows

- **Current Challenge:**
  - GitLive SDK's phone auth API differs from native Firebase
  - Proper integration requires Activity/UIViewController context injection
  - Need to handle platform-specific verification callbacks

- **Architecture Ready:**
  - ✅ All interfaces and data models defined
  - ✅ Repository layer implemented
  - ✅ ViewModel integration complete
  - ✅ UI components ready with method toggle
  - ⏳ Actual Firebase SDK integration pending

## Firebase Project Setup (For Future Implementation)

When Firebase Phone Auth is fully implemented, you'll need to configure Firebase projects.

### 1. Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **Add Project** or select existing project
3. Enter project name (e.g., "Ampairs Mobile")
4. Disable Google Analytics (optional)
5. Click **Create Project**

### 2. Enable Phone Authentication

1. In Firebase Console, navigate to **Authentication**
2. Click **Get Started**
3. Go to **Sign-in method** tab
4. Find **Phone** provider and click to enable
5. Click **Save**

### 3. Configure Phone Number Test Accounts (Optional)

For development and testing without SMS costs:

1. In **Authentication > Sign-in method**
2. Scroll to **Phone numbers for testing**
3. Add test phone numbers with verification codes (e.g., `+919999999999` → `123456`)

## Android Configuration

### 1. Register Android App in Firebase

1. In Firebase Console, click **Add app** > **Android**
2. **Android package name**: `com.ampairs.app` (must match `applicationId` in `build.gradle.kts`)
3. **App nickname**: "Ampairs Android"
4. **Debug SHA-1**: Run this command to get your debug key:
   ```bash
   cd ~/.android
   keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Copy the SHA-1 fingerprint and paste it into Firebase Console
6. Click **Register app**

### 2. Download google-services.json

1. Download `google-services.json` from Firebase Console
2. Place it in: `/ampairs-mp-app/composeApp/google-services.json`

### 3. Add Google Services Plugin

**Already configured in the project**, but verify:

```kotlin
// composeApp/build.gradle.kts
plugins {
    // ... existing plugins
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

### 4. Verify Configuration

Ensure `google-services.json` is in the correct location:
```
ampairs-mp-app/
└── composeApp/
    ├── google-services.json  ← HERE
    ├── build.gradle.kts
    └── src/
```

### 5. Test Android App

```bash
./gradlew composeApp:assembleDebug
./gradlew composeApp:installDebug
```

## iOS Configuration

### 1. Register iOS App in Firebase

1. In Firebase Console, click **Add app** > **iOS**
2. **iOS bundle ID**: `com.ampairs.app` (must match your Xcode project)
3. **App nickname**: "Ampairs iOS"
4. Click **Register app**

### 2. Download GoogleService-Info.plist

1. Download `GoogleService-Info.plist` from Firebase Console
2. Place it in: `/ampairs-mp-app/iosApp/GoogleService-Info.plist`

### 3. Add to Xcode Project

1. Open `iosApp/iosApp.xcodeproj` in Xcode
2. Drag `GoogleService-Info.plist` into the project navigator
3. Ensure **Copy items if needed** is checked
4. Select target: **iosApp**
5. Click **Finish**

### 4. Configure Push Notifications (Required for Firebase Auth)

1. In Xcode, select your project in the navigator
2. Select **iosApp** target
3. Go to **Signing & Capabilities** tab
4. Click **+ Capability**
5. Add **Push Notifications**

### 5. Enable Background Modes (Optional)

For better auth experience:

1. Add **Background Modes** capability
2. Check **Remote notifications**

### 6. Verify Configuration

Ensure `GoogleService-Info.plist` is in the correct location:
```
ampairs-mp-app/
└── iosApp/
    ├── GoogleService-Info.plist  ← HERE
    ├── iosApp.xcodeproj
    └── iosApp/
```

### 7. Test iOS App

```bash
# For Simulator
./gradlew composeApp:embedAndSignAppleFrameworkForXcode

# Then run in Xcode
```

## Desktop (Future Implementation)

Currently, Firebase Phone Auth is **not supported** on Desktop. The implementation returns an error message.

### Planned QR Code Authentication

Desktop will use QR code authentication similar to WhatsApp Web:

1. User opens desktop app
2. Desktop generates QR code with session token
3. User scans QR code with mobile app (Android/iOS)
4. Mobile app authenticates and confirms desktop session
5. Desktop receives auth tokens and completes login

**Status**: Stub implementation exists, full implementation planned for future release.

## Current Usage in App

### Authentication Method

Currently, **all platforms use Backend API authentication only**:

- **Android**: Backend API (Firebase stub returns "not supported")
- **iOS**: Backend API (Firebase stub returns "not supported")
- **Desktop**: Backend API (Firebase stub returns "not supported")

### No UI Toggle

Since `FirebaseAuthProvider.isSupported()` returns `false` on all platforms:

```kotlin
// LoginViewModel.kt
val isFirebaseSupported: Boolean = firebaseAuthRepository.isSupported() // Returns false

// PhoneScreen.kt - Firebase toggle is HIDDEN
if (viewModel.isFirebaseSupported) { // This is false, so toggle doesn't show
    // Auth method selector
}
```

**Result:** Users see only the phone input and login button, no auth method toggle.

### Current User Flow

**Login (Backend API Only):**
1. User enters phone number
2. Clicks "Login" (no auth method selection shown)
3. Backend sends OTP via SMS
4. User enters OTP code
5. Backend verifies and authenticates
6. User logged in

## Troubleshooting

### Android Issues

**Problem**: Firebase not initializing
- **Solution**: Verify `google-services.json` is in `composeApp/` directory
- **Check**: SHA-1 fingerprint is registered in Firebase Console

**Problem**: SMS not received
- **Solution**:
  - Check phone number format includes country code (+91...)
  - Verify Firebase quota limits not exceeded
  - Use test phone numbers for development

### iOS Issues

**Problem**: Firebase not initializing
- **Solution**:
  - Verify `GoogleService-Info.plist` is added to Xcode project
  - Check bundle ID matches Firebase Console

**Problem**: SMS not received
- **Solution**:
  - Ensure Push Notifications capability is enabled
  - Check Apple Developer account has valid certificates

### Desktop

**Expected**: Firebase auth not supported message
- This is normal behavior - Desktop uses Backend API only

## Security Considerations

### Production Deployment

1. **Enable App Check** (recommended):
   - Prevents abuse and unauthorized access
   - Follow [Firebase App Check documentation](https://firebase.google.com/docs/app-check)

2. **Configure reCAPTCHA** (web-based flows):
   - Add authorized domains in Firebase Console

3. **Rate Limiting**:
   - Firebase has built-in SMS quota limits
   - Monitor usage in Firebase Console > Usage tab

4. **Test Phone Numbers**:
   - Remove test numbers before production release
   - Use only for development/QA testing

### Backend Integration

After Firebase authenticates user, you may want to:

1. Send Firebase UID to backend
2. Link Firebase account with backend user account
3. Sync user data between Firebase and backend

**Note**: Current implementation completes Firebase auth but doesn't sync with backend yet.

## File Structure

```
ampairs-mp-app/
├── composeApp/
│   ├── google-services.json              # Android Firebase config
│   └── src/
│       ├── commonMain/kotlin/com/ampairs/auth/
│       │   ├── domain/
│       │   │   ├── AuthMethod.kt         # Auth method enum
│       │   │   └── FirebaseAuthResult.kt # Result types
│       │   ├── firebase/
│       │   │   ├── FirebaseAuthProvider.kt      # Expect interface
│       │   │   └── FirebaseAuthRepository.kt    # Repository
│       │   └── viewmodel/
│       │       └── LoginViewModel.kt     # Updated with Firebase support
│       ├── androidMain/kotlin/com/ampairs/auth/firebase/
│       │   └── FirebaseAuthProvider.android.kt  # Android implementation
│       ├── iosMain/kotlin/com/ampairs/auth/firebase/
│       │   └── FirebaseAuthProvider.ios.kt      # iOS implementation
│       └── desktopMain/kotlin/com/ampairs/auth/firebase/
│           └── FirebaseAuthProvider.desktop.kt  # Desktop stub
└── iosApp/
    └── GoogleService-Info.plist          # iOS Firebase config
```

## Additional Resources

- [Firebase Phone Auth Documentation](https://firebase.google.com/docs/auth/android/phone-auth)
- [GitLive Firebase Kotlin SDK](https://github.com/GitLiveApp/firebase-kotlin-sdk)
- [Firebase Console](https://console.firebase.google.com/)
- [Kotlin Multiplatform Firebase Best Practices](https://firebase.google.com/docs/android/kotlin-multiplatform)

## Support

For issues or questions:
- Check existing GitHub issues: [ampairs/ampairs-mp-app](https://github.com/ampairs/ampairs-mp-app/issues)
- Review Firebase Console logs for authentication errors
- Contact development team for Firebase project access

---

## Implementation Roadmap

### Phase 1: ✅ **COMPLETE** - Architecture & Stub
- ✅ Firebase SDK dependencies configured
- ✅ Expect/actual pattern for platform implementations
- ✅ Domain models and result types
- ✅ Repository and ViewModel integration
- ✅ UI components with conditional Firebase toggle
- ✅ Stub implementations (returns "not supported")

### Phase 2: ⏳ **PLANNED** - Android Implementation
- ⏳ Activity context injection for reCAPTCHA
- ⏳ GitLive SDK phone auth API integration
- ⏳ Verification callback handling
- ⏳ Testing with real phone numbers

### Phase 3: ⏳ **PLANNED** - iOS Implementation
- ⏳ APNs configuration
- ⏳ UIViewController context integration
- ⏳ iOS-specific verification flows
- ⏳ Silent push notification handling

### Phase 4: ⏳ **FUTURE** - Desktop QR Code Auth
- ⏳ QR code generation with session tokens
- ⏳ Mobile app QR scanner
- ⏳ Desktop session confirmation flow
- ⏳ Backend session management

---

**Last Updated**: January 2025
**Status**: Stub Implementation (Backend API Only)
**Firebase SDK Version**: GitLive Firebase Kotlin SDK 2.3.0
**KMP Version**: Kotlin 2.2.20
