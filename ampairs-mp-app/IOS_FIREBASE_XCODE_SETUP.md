# iOS Firebase Setup in Xcode - Step by Step

## Overview

This guide shows how to add Firebase Authentication to your iOS app using **Swift Package Manager** in Xcode, following the official Firebase documentation: https://firebase.google.com/docs/auth/ios/start

## Prerequisites

- Xcode 15+ installed
- Firebase project created in Firebase Console
- iOS app registered in Firebase Console with bundle ID: `com.ampairs.app`

## Step 1: Register iOS App in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project (same project as Android)
3. Click **Add app** → Select **iOS** (Apple icon)
4. Enter configuration:
   - **iOS bundle ID**: `com.ampairs.app` ✅
   - **App nickname**: `Ampairs iOS` (optional)
   - **App Store ID**: Leave blank
5. Click **Register app**

## Step 2: Download GoogleService-Info.plist

1. After registration, click **Download GoogleService-Info.plist**
2. Save the file to your Downloads folder
3. Click **Next** → **Continue to console**

## Step 3: Enable Phone Authentication in Firebase

1. In Firebase Console, navigate to **Authentication**
2. Click **Get Started** (if not already enabled)
3. Go to **Sign-in method** tab
4. Find **Phone** provider → Click to expand
5. Toggle **Enable** → Click **Save**

### Optional: Add Test Phone Numbers

For development without SMS costs:

1. In **Authentication > Sign-in method**
2. Scroll to **Phone numbers for testing**
3. Click **Add phone number**
4. Add: `+919999999999` → Verification code: `123456`
5. Click **Add**

## Step 4: Open Xcode Project

```bash
cd /Users/omprakashsrv/IdeaProjects/ampairs/ampairs-mp-app
open iosApp/iosApp.xcodeproj
```

## Step 5: Add GoogleService-Info.plist to Xcode

1. In Xcode's **Project Navigator** (left sidebar), select the **`iosApp`** folder (yellow folder icon)
2. Right-click on `iosApp` folder → **Add Files to "iosApp"...**
3. Navigate to your **Downloads** folder
4. Select `GoogleService-Info.plist`
5. **IMPORTANT** - Check these options:
   - ✅ **Copy items if needed**
   - ✅ **Create groups** (not folder references)
   - ✅ **Add to targets:** `iosApp`
6. Click **Add**

### Verify File is Added

1. Click on `GoogleService-Info.plist` in Project Navigator
2. In **File Inspector** (right panel):
   - ✅ **Target Membership**: `iosApp` should be checked
   - File path should show `iosApp/iosApp/GoogleService-Info.plist`

## Step 6: Add Firebase SDK via Swift Package Manager

1. In Xcode, with `iosApp.xcodeproj` open
2. Navigate to: **File → Add Package Dependencies...**
3. In the search bar at top right, enter:
   ```
   https://github.com/firebase/firebase-ios-sdk
   ```
4. Press **Enter**
5. Wait for package to load (may take a minute)

### Select Firebase Products

When prompted to choose package products:

1. **Dependency Rule**: Select **Up to Next Major Version**
2. **Version**: Enter `11.0.0` (or latest shown)
3. Click **Add Package**
4. Select these products for target `iosApp`:
   - ✅ **FirebaseAuth** (Required)
   - ✅ **FirebaseCore** (Required - automatically selected)
5. Click **Add Package**

Xcode will download and integrate Firebase SDK (takes 1-2 minutes).

## Step 7: Add FirebaseAuthManager.swift to Xcode Project

The Swift wrapper file `FirebaseAuthManager.swift` has been created in `iosApp/iosApp/`.

1. In Xcode Project Navigator, right-click on **`iosApp`** folder
2. Select **Add Files to "iosApp"...**
3. Navigate to:
   ```
   /Users/omprakashsrv/IdeaProjects/ampairs/ampairs-mp-app/iosApp/iosApp/
   ```
4. Select **`FirebaseAuthManager.swift`**
5. **Check**:
   - ⬜ **Copy items if needed** (UNCHECK - file already in folder)
   - ✅ **Create groups**
   - ✅ **Add to targets:** `iosApp`
6. Click **Add**

### Verify Swift File is Added

1. `FirebaseAuthManager.swift` should appear in Project Navigator under `iosApp` folder
2. Click on the file
3. In **File Inspector** (right panel):
   - ✅ **Target Membership**: `iosApp` checked

## Step 8: Enable Push Notifications (Required for Firebase Phone Auth)

1. In Xcode, click on **iosApp** project (blue icon at top of navigator)
2. Select **iosApp** target (under TARGETS)
3. Click **Signing & Capabilities** tab
4. Click **+ Capability** button
5. Search for and double-click **Push Notifications**

You should now see "Push Notifications" listed in capabilities.

### Optional: Enable Background Modes

For better authentication experience:

1. Click **+ Capability** again
2. Add **Background Modes**
3. Under Background Modes, check:
   - ✅ **Remote notifications**

## Step 9: Update Kotlin iOS Implementation

The Kotlin implementation needs to call the Swift FirebaseAuthManager. Since Kotlin/Native cannot directly call Swift without additional configuration, we'll use a bridging approach.

**For now, the iOS implementation will remain using Backend API until we set up proper Kotlin/Swift interop.**

The infrastructure is ready, but connecting Kotlin to Swift requires:
- Creating a bridging header, OR
- Using a third-party library like GitLive Firebase Kotlin SDK

## Step 10: Build and Run

1. In Xcode, select **iosApp** scheme
2. Choose **iPhone 15 Pro** simulator (or any iOS 17.2+ simulator)
3. Click **Run** (▶️) button or press `Cmd+R`
4. App should build and launch

### Build the Kotlin Framework First (Optional)

If Xcode build fails, build the Kotlin framework manually first:

```bash
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

Then try building in Xcode again.

## Step 11: Test Firebase Authentication

### Current Status

**Authentication on iOS**: Backend API (default)

Firebase infrastructure is now set up in Xcode, but the Kotlin implementation still uses Backend API because Kotlin/Swift interop requires additional configuration.

### To Enable Firebase in Kotlin (Next Steps)

**Option 1: Use GitLive Firebase Kotlin SDK (Recommended)**

Add to `composeApp/build.gradle.kts`:

```kotlin
val iosMain by creating {
    dependencies {
        implementation("dev.gitlive:firebase-auth:2.3.0")
        implementation("dev.gitlive:firebase-app:2.3.0")
    }
}
```

Then update `FirebaseAuthProvider.ios.kt` to use GitLive API instead of Swift wrapper.

**Option 2: Create Objective-C Bridging Header**

1. Create `iosApp-Bridging-Header.h` in Xcode
2. Import FirebaseAuthManager
3. Configure bridging header path in Xcode settings
4. Update Kotlin to call Objective-C functions

## Troubleshooting

### "No such module 'FirebaseCore'" or "'FirebaseAuth'"

**Fix**:
1. Verify Firebase packages are added: **File → Packages → Resolve Package Versions**
2. Clean build: **Product → Clean Build Folder** (Shift+Cmd+K)
3. Rebuild project

### "GoogleService-Info.plist not found"

**Fix**:
1. Check file is in Project Navigator
2. Verify **Target Membership** shows `iosApp` checked
3. Re-add file if needed

### Build fails with "Cannot find 'FirebaseAuthManager'"

**Fix**:
1. Verify `FirebaseAuthManager.swift` is in Project Navigator
2. Check **Target Membership** is `iosApp`
3. Verify file appears in **Build Phases → Compile Sources**

### Phone verification not working

**For Development**:
- Use test phone numbers configured in Firebase Console
- Test number: `+919999999999` → Code: `123456`

**For Production**:
- Configure APNs (Apple Push Notification service)
- See [Firebase iOS Phone Auth Documentation](https://firebase.google.com/docs/auth/ios/phone-auth#send-a-verification-code-to-the-users-phone)

## Summary

After completing this setup:

✅ **Firebase SDK added to Xcode** via Swift Package Manager
✅ **GoogleService-Info.plist** in project
✅ **FirebaseAuthManager.swift** ready to use
✅ **Push Notifications** capability enabled
⏳ **Kotlin/Swift interop** needs additional configuration

**Current Authentication**: Backend API (fully functional)

**Next Step**: Choose Option 1 (GitLive SDK) or Option 2 (Bridging Header) to enable Firebase in Kotlin.

---

**Reference**: [Firebase iOS Setup Documentation](https://firebase.google.com/docs/auth/ios/start)
**Last Updated**: January 2025
