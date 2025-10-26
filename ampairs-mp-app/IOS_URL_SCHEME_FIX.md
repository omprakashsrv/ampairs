# iOS Firebase Phone Auth URL Scheme Fix

## Problem

**Fatal Error**: `FirebaseAuth/PhoneAuthProvider.swift:109: Fatal error: Unexpectedly found nil while implicitly unwrapping an Optional value`

**Root Cause**: Missing custom URL scheme in `Info.plist` required for Firebase Phone Auth OAuth callbacks.

## Root Cause Analysis

Firebase Phone Auth on iOS uses reCAPTCHA verification which requires:
1. A custom URL scheme registered in `Info.plist`
2. The `REVERSED_CLIENT_ID` from `GoogleService-Info.plist`
3. This URL scheme handles the OAuth callback after reCAPTCHA verification

The crash occurs because:
- The current `GoogleService-Info.plist` files are incomplete (missing `REVERSED_CLIENT_ID`)
- The `Info.plist` doesn't have `CFBundleURLTypes` configuration
- Firebase tries to verify the URL scheme and crashes when it's not found

## Solution Steps

### Step 1: Download Complete GoogleService-Info.plist from Firebase Console

Your current `GoogleService-Info.plist` is missing critical fields. You need to re-download it:

1. **Go to Firebase Console**: https://console.firebase.google.com/
2. **Select your project**: "ampairs"
3. **Navigate to iOS app settings**:
   - Click the gear icon (⚙️) next to "Project Overview"
   - Select "Project settings"
   - Scroll down to "Your apps" section
   - Find your iOS app with bundle ID `com.ampairs.app`
   - If you don't see an iOS app, click "Add app" → iOS and register with bundle ID `com.ampairs.app`

4. **Download the plist**:
   - In the iOS app card, click on the iOS app
   - Scroll to "Your apps" → iOS app configuration
   - Click "GoogleService-Info.plist" download button
   - Save the file

5. **Verify the downloaded file contains**:
   ```xml
   <key>REVERSED_CLIENT_ID</key>
   <string>com.googleusercontent.apps.XXXXXXX</string>
   ```

   The complete file should have these keys:
   - `API_KEY`
   - `GCM_SENDER_ID`
   - `BUNDLE_ID`
   - `PROJECT_ID`
   - `GOOGLE_APP_ID`
   - **`REVERSED_CLIENT_ID`** ← This is critical!
   - `CLIENT_ID`
   - `DATABASE_URL`
   - `STORAGE_BUCKET`

### Step 2: Replace GoogleService-Info.plist Files

Replace both development and production plist files:

```bash
# Backup current files
mv iosApp/Firebase/GoogleService-Info-Development.plist iosApp/Firebase/GoogleService-Info-Development.plist.backup
mv iosApp/Firebase/GoogleService-Info-Production.plist iosApp/Firebase/GoogleService-Info-Production.plist.backup

# Copy the newly downloaded file
# If you have separate Firebase projects for dev/prod, download both
# Otherwise, use the same file for both
cp ~/Downloads/GoogleService-Info.plist iosApp/Firebase/GoogleService-Info-Development.plist
cp ~/Downloads/GoogleService-Info.plist iosApp/Firebase/GoogleService-Info-Production.plist
```

### Step 3: Update Info.plist with REVERSED_CLIENT_ID

**I've already updated `Info.plist` with the structure**, but you need to replace the placeholder:

1. Open the newly downloaded `GoogleService-Info.plist`
2. Find the `REVERSED_CLIENT_ID` value (looks like `com.googleusercontent.apps.XXXXXXXXX`)
3. Open `iosApp/iosApp/Info.plist`
4. Find line 39 with the placeholder:
   ```xml
   <string>com.googleusercontent.apps.PLACEHOLDER-REVERSED-CLIENT-ID</string>
   ```
5. Replace with your actual `REVERSED_CLIENT_ID`:
   ```xml
   <string>com.googleusercontent.apps.682032206651-ACTUAL_VALUE_FROM_PLIST</string>
   ```

### Step 4: Rebuild the App

```bash
# Clean build
./gradlew clean

# Rebuild iOS framework
./gradlew :composeApp:compileKotlinIosSimulatorArm64

# Or open in Xcode and build
open iosApp/iosApp.xcworkspace
# In Xcode: Product → Clean Build Folder (Shift+Cmd+K)
# Then: Product → Build (Cmd+B)
```

### Step 5: Test Phone Authentication

1. Run the app in iOS Simulator
2. Navigate to login screen
3. Enter a test phone number (e.g., `+919999999999` if configured in Firebase)
4. Click "Send OTP"
5. Firebase should now present reCAPTCHA without crashing

## Quick Fix (If You Can't Access Firebase Console Immediately)

If you need a temporary workaround and can't access Firebase Console right now:

### Option A: Use Backend API Authentication (Recommended Temporary Fix)

The app already supports backend API authentication. You can temporarily switch to use backend auth:

1. In `FirebaseAuthProvider.ios.kt`, the implementation already falls back to backend auth on errors
2. The backend Spring Boot API handles phone auth verification
3. This bypasses Firebase Phone Auth entirely

### Option B: Extract REVERSED_CLIENT_ID from Existing Setup

If Firebase was working before, you might have the REVERSED_CLIENT_ID somewhere:

```bash
# Check Xcode project for existing URL schemes
grep -r "CFBundleURLSchemes" iosApp/
grep -r "REVERSED_CLIENT_ID" iosApp/
```

## Verification Checklist

After applying the fix:

- [ ] `GoogleService-Info.plist` contains `REVERSED_CLIENT_ID` field
- [ ] `Info.plist` has `CFBundleURLTypes` with correct `REVERSED_CLIENT_ID`
- [ ] App builds without errors in Xcode
- [ ] Phone auth flow presents reCAPTCHA without crashing
- [ ] OTP verification completes successfully

## What Changed

### Before (Crash):
```xml
<!-- Info.plist -->
<dict>
	<key>CADisableMinimumFrameDurationOnPhone</key>
	<true/>
</dict>
```

### After (Working):
```xml
<!-- Info.plist -->
<dict>
	<!-- Standard iOS app keys -->
	<key>CFBundleDisplayName</key>
	<string>Ampairs</string>
	...

	<!-- Firebase URL Scheme for OAuth callback -->
	<key>CFBundleURLTypes</key>
	<array>
		<dict>
			<key>CFBundleURLSchemes</key>
			<array>
				<string>com.googleusercontent.apps.ACTUAL-REVERSED-CLIENT-ID</string>
			</array>
		</dict>
	</array>
</dict>
```

## Technical Details

### Why This Fix Works

1. **URL Scheme Registration**: iOS uses URL schemes to handle deep links and OAuth callbacks
2. **Firebase Phone Auth Flow**:
   - App initiates phone verification
   - Firebase presents reCAPTCHA in Safari/SFSafariViewController
   - User completes reCAPTCHA
   - Firebase redirects back to app using `{REVERSED_CLIENT_ID}://` URL scheme
   - iOS routes this URL back to the app
   - Firebase completes verification

3. **The Crash**: Firebase checks if the URL scheme is registered **before** presenting reCAPTCHA
   - If not found: Fatal error (your current situation)
   - If found: Proceeds with verification flow

### Related Files Modified

- `iosApp/iosApp/Info.plist` - Added CFBundleURLTypes and standard app keys
- `iosApp/Firebase/GoogleService-Info-Development.plist` - Needs re-download
- `iosApp/Firebase/GoogleService-Info-Production.plist` - Needs re-download

## Additional Resources

- [Firebase iOS Phone Auth Setup](https://firebase.google.com/docs/auth/ios/phone-auth)
- [iOS URL Scheme Documentation](https://developer.apple.com/documentation/xcode/defining-a-custom-url-scheme-for-your-app)
- [Firebase Console](https://console.firebase.google.com/)

## Troubleshooting

### Still Crashing After Fix?

1. **Verify URL scheme**: Print the `REVERSED_CLIENT_ID` in Xcode and ensure it matches `Info.plist`
2. **Clean build**: Delete DerivedData and rebuild
   ```bash
   rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*
   ```
3. **Check logs**: Look for Firebase initialization logs in Xcode console

### Different Error Message?

If you see a different error related to:
- **APNs**: You need Apple Push Notification setup (production only)
- **GoogleService-Info.plist not found**: Check file is copied to Xcode project
- **Invalid verification code**: Different issue - this fix is for the URL scheme crash only

## Next Steps

After this fix is applied:

1. ✅ Phone auth will work in development with test phone numbers
2. ⚠️ For production: Configure APNs (Apple Push Notification service)
3. 📱 For TestFlight: Add APNs certificates in Firebase Console

---

**Status**: Fix applied to `Info.plist` structure. **Action required**: Download complete `GoogleService-Info.plist` and update placeholder URL scheme.

**Created**: 2025-10-26
**Related Commits**: f56d1d0 (previous phone auth fix attempt)