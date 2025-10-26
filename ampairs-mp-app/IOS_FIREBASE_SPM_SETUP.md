# iOS Firebase Setup with Swift Package Manager

## Overview

This guide shows how to add Firebase Authentication to your iOS app using **Swift Package Manager (SPM)**, which is the recommended and simplest approach for Xcode projects.

## iOS Bundle ID

Your iOS app bundle identifier is: **`com.ampairs.app`**

(Configured in `iosApp/Configuration/Config.xcconfig`)

## Step 1: Configure Firebase Project for iOS

### 1.1 Add iOS App to Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your existing project (same as Android)
3. Click **Add app** → Select **iOS** (Apple icon)
4. Enter configuration:
   - **iOS bundle ID**: `com.ampairs.app` ✅
   - **App nickname**: `Ampairs iOS` (optional)
   - **App Store ID**: Leave blank for now
5. Click **Register app**

### 1.2 Download GoogleService-Info.plist

1. After registration, download **`GoogleService-Info.plist`**
2. Save it somewhere accessible (you'll add it to Xcode next)
3. Click **Next** → **Continue to console**

### 1.3 Enable Phone Authentication

1. In Firebase Console, go to **Authentication**
2. Click **Sign-in method** tab
3. Find **Phone** provider → Click to expand
4. Toggle **Enable** → Click **Save**

### 1.4 Optional: Add Test Phone Numbers

For development without SMS costs:

1. In **Authentication > Sign-in method**
2. Scroll down to **Phone numbers for testing**
3. Click **Add phone number**
4. Add: **`+919999999999`** → Verification code: **`123456`**
5. Click **Save**

## Step 2: Add GoogleService-Info.plist to Xcode Project

### 2.1 Open Xcode Project

```bash
cd /Users/omprakashsrv/IdeaProjects/ampairs/ampairs-mp-app
open iosApp/iosApp.xcodeproj
```

### 2.2 Add GoogleService-Info.plist File

1. In Xcode's **Project Navigator** (left sidebar), select the **`iosApp`** folder (yellow folder icon)
2. Right-click on `iosApp` folder → **Add Files to "iosApp"...**
3. Navigate to your downloaded `GoogleService-Info.plist`
4. **IMPORTANT - Check these options:**
   - ✅ **Copy items if needed**
   - ✅ **Create groups** (not folder references)
   - ✅ **Add to targets:** `iosApp`
5. Click **Add**

### 2.3 Verify File is Added Correctly

1. Click on `GoogleService-Info.plist` in Project Navigator
2. In the **File Inspector** (right panel):
   - Verify **Target Membership** shows ✅ `iosApp`
   - File should be in `iosApp/iosApp/` directory

## Step 3: Add Firebase SDK via Swift Package Manager

### 3.1 Add Firebase iOS SDK Package

1. In Xcode, with `iosApp.xcodeproj` open
2. Navigate to: **File → Add Package Dependencies...**
3. In the search bar at top right, paste:
   ```
   https://github.com/firebase/firebase-ios-sdk
   ```
4. Press **Enter** or click the search icon

### 3.2 Select SDK Version

1. **Dependency Rule**: Select **Up to Next Major Version**
2. **Version**: Enter `11.0.0` (or latest version shown)
3. Click **Add Package**

### 3.3 Choose Firebase Products

When prompted to choose package products, select:

- ✅ **FirebaseAuth** (Required for phone authentication)
- ✅ **FirebaseCore** (Required - automatically selected)
- ⬜ **FirebaseAnalytics** (Optional - skip for now)

Click **Add Package**

Xcode will download and integrate the Firebase SDK (this may take a minute).

## Step 4: Initialize Firebase in iOS App

### 4.1 Update iOSApp.swift

Open `/Users/omprakashsrv/IdeaProjects/ampairs/ampairs-mp-app/iosApp/iosApp/iOSApp.swift`

Replace the entire file with:

```swift
import SwiftUI
import FirebaseCore  // ← Add this import

@main
struct iOSApp: App {

    // Initialize Firebase when app launches
    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

**Key changes:**
- Added `import FirebaseCore`
- Added `init()` method with `FirebaseApp.configure()`

## Step 5: Enable Push Notifications (Required for Firebase Auth)

### 5.1 Add Push Notifications Capability

1. In Xcode, click on **iosApp** project (blue icon at top of navigator)
2. Select **iosApp** target (under TARGETS)
3. Click **Signing & Capabilities** tab
4. Click **+ Capability** button
5. Search for and double-click **Push Notifications**

You should now see "Push Notifications" listed in capabilities.

### 5.2 Optional: Enable Background Modes

For better authentication experience:

1. Click **+ Capability** again
2. Add **Background Modes**
3. Under Background Modes, check:
   - ✅ **Remote notifications**

## Step 6: Build and Test

### 6.1 Build the Kotlin Framework

```bash
cd /Users/omprakashsrv/IdeaProjects/ampairs/ampairs-mp-app

# Build the iOS framework
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

### 6.2 Build in Xcode

1. Select **iosApp** scheme at the top
2. Choose **iPhone 15 Pro** (or any simulator)
3. Click **Run** (▶️) button or press `Cmd+R`

### 6.3 Test Firebase Authentication

1. App should launch in simulator
2. On login screen, you should see:
   - **Backend API** option (default)
   - **Firebase** option
3. Select **Firebase**
4. Enter test phone number: `+919999999999`
5. Use test code: `123456` (if configured in Firebase Console)

## Step 7: Production Configuration (When Ready)

### 7.1 APNs Authentication Key

For production Firebase Phone Auth on real devices:

1. Go to [Apple Developer Portal](https://developer.apple.com/account/)
2. **Certificates, Identifiers & Profiles** → **Keys**
3. Click **+** to create a new key
4. Name: `Ampairs APNs Key`
5. Check **Apple Push Notifications service (APNs)**
6. Click **Continue** → **Register**
7. **Download** the `.p8` file (⚠️ You can only download once!)
8. Note the **Key ID**

### 7.2 Upload APNs Key to Firebase

1. Firebase Console → **Project Settings** (gear icon)
2. **Cloud Messaging** tab
3. Scroll to **Apple app configuration**
4. Under **APNs Authentication Key**:
   - Click **Upload**
   - Select your `.p8` file
   - Enter **Key ID**
   - Enter **Team ID** (from Apple Developer account)
5. Click **Upload**

## Troubleshooting

### Error: "No such module 'FirebaseCore'"

**Solution:**
1. Clean build folder: `Product → Clean Build Folder` (Shift+Cmd+K)
2. Verify Firebase package is added: File → Packages → Resolve Package Versions
3. Rebuild project

### Error: "FirebaseApp.configure() failed"

**Solution:**
1. Verify `GoogleService-Info.plist` is in the project
2. Check Target Membership is set to `iosApp`
3. Clean and rebuild

### Phone Verification Not Working

**Checklist:**
- ✅ Firebase Console → Authentication → Phone is enabled
- ✅ Push Notifications capability added in Xcode
- ✅ For test numbers: Added in Firebase Console
- ✅ For real numbers: APNs configured (production only)
- ✅ Check Xcode console for Firebase error logs

### Build Error: "Command PhaseScriptExecution failed"

**Solution:**
1. Ensure Gradle build succeeds first:
   ```bash
   ./gradlew :composeApp:compileKotlinIosSimulatorArm64
   ```
2. If Gradle fails, check JDK version:
   ```bash
   ./gradlew --version
   # Should show "Daemon JVM: ...jbr-17..."
   ```
3. Clean Xcode: `Product → Clean Build Folder`

### Swift Package Manager Issues

**Solution:**
1. File → Packages → Reset Package Caches
2. File → Packages → Resolve Package Versions
3. Restart Xcode

## Summary

✅ **What you configured:**
- Firebase iOS app registered with bundle ID `com.ampairs.app`
- GoogleService-Info.plist added to Xcode project
- Firebase SDK added via Swift Package Manager (SPM)
- Firebase initialized in `iOSApp.swift`
- Push Notifications capability enabled

✅ **Current Status:**
- ✅ iOS app builds successfully without CocoaPods
- ✅ Firebase infrastructure is set up and ready
- ⏳ Firebase Auth implementation is stubbed - uses Backend API by default
- ✅ Backend API authentication works fully

⏳ **To Enable Firebase Auth on iOS (Optional):**
- Create a Swift wrapper class for Firebase Auth SDK
- Expose it to Kotlin via @objc annotations
- Update FirebaseAuthProvider.ios.kt to call the Swift wrapper

✅ **What works NOW:**
- Backend API Phone Authentication on iOS (fully functional)
- Dual method selection UI (Backend API + Firebase)
- All login flows via Backend API
- Test phone numbers for development

## Resources

- [Firebase iOS Setup](https://firebase.google.com/docs/ios/setup)
- [Firebase Phone Auth iOS](https://firebase.google.com/docs/auth/ios/phone-auth)
- [Swift Package Manager Guide](https://developer.apple.com/documentation/xcode/adding-package-dependencies-to-your-app)

---

**Bundle ID Reference:**
- Android: `com.ampairs.app` (in `build.gradle.kts`)
- iOS: `com.ampairs.app` (in `Config.xcconfig`)
