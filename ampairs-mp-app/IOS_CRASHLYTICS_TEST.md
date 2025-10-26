# Firebase Crashlytics Test - iOS

This guide explains how to test Firebase Crashlytics integration on iOS using the built-in test crash button.

## Overview

A test crash button has been integrated into the iOS app to verify Firebase Crashlytics is properly configured and sending crash reports to Firebase Console.

## Test Crash Button

### Location
- **Visible**: DEBUG builds only (automatically hidden in release builds)
- **Position**: Bottom-right corner of the screen
- **Appearance**: Red button with warning icon labeled "Test Crash"

### Implementation Files
- `iosApp/iosApp/CrashTestView.swift` - Test crash button component
- `iosApp/iosApp/ContentView.swift` - Integrates test button as overlay
- `iosApp/iosApp/AppDelegate.swift` - Firebase initialization and Crashlytics setup

## How to Test Crashlytics

### Step 1: Build and Run Without Debugger

**IMPORTANT**: The Xcode debugger prevents crash reports from being sent to Firebase. You must disconnect the debugger before testing.

#### In Xcode:
1. Click **▶️ Build and Run** (Cmd+R) to build and install the app
2. Wait for the app to launch on device/simulator
3. Click **⏹️ Stop** (Cmd+.) to disconnect the debugger
4. **Manually launch the app** from the iOS home screen

### Step 2: Trigger Test Crash

1. Open the app from the home screen (not from Xcode)
2. Locate the **"Test Crash"** button in the bottom-right corner
3. Tap the button
4. The app will crash immediately with "Index out of range" error

**What happens when you tap "Test Crash":**
- Logs a message to Crashlytics: `"Test crash button pressed - verifying Crashlytics integration"`
- Sets custom keys:
  - `crash_type: "test"`
  - `crash_timestamp: <current unix timestamp>`
- Forces an array index out of bounds crash

### Step 3: Send Crash Report to Firebase

1. **Relaunch the app** from the home screen (or from Xcode)
2. When the app starts, Firebase SDK automatically sends the crash report
3. Crash report is uploaded to Firebase in the background

### Step 4: View Crash Report in Firebase Console

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to **Crashlytics** in the left sidebar
4. Wait up to 5 minutes for the crash report to appear

**Expected Result:**
- Crash report titled: `"Fatal Exception: NSRangeException"`
- Error message: `"Index 1 beyond bounds [0 .. 0]"`
- Custom keys visible:
  - `crash_type = "test"`
  - `crash_timestamp = <timestamp>`
- Log message: `"Test crash button pressed - verifying Crashlytics integration"`

## Debug Logging

If crash reports are not appearing in Firebase Console after 5 minutes, enable debug logging:

### Enable Crashlytics Debug Logging

#### Option 1: Xcode Scheme Arguments
1. In Xcode, select **Product > Scheme > Edit Scheme**
2. Select **Run > Arguments**
3. Add the following to **Arguments Passed On Launch**:
   ```
   -FIRDebugEnabled
   -FIRAnalyticsDebugEnabled
   ```

#### Option 2: Command Line
```bash
# Run with debug logging
xcodebuild -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  DEBUG_INFORMATION_FORMAT=dwarf-with-dsym \
  -FIRDebugEnabled -FIRAnalyticsDebugEnabled
```

### Check Console Logs

Look for Firebase Crashlytics logs in Xcode Console:
- `[Firebase/Crashlytics]` - Crashlytics activity
- `CLSPackageReportOperation` - Report upload status
- `Completed report submission` - Successful upload

## Troubleshooting

### Crash Report Not Appearing

**Problem**: Crash report doesn't show up in Firebase Console after 5+ minutes

**Solutions**:
1. **Verify Firebase is configured**: Check `GoogleService-Info.plist` is included in project
2. **Check internet connection**: Crash reports require network to upload
3. **Verify dSYM upload**: Ensure build phase script is configured (see Step 2 of Firebase setup)
4. **Enable debug logging**: Use `-FIRDebugEnabled` flag and check console logs
5. **Wait longer**: Sometimes reports take up to 10 minutes to process

### Test Button Not Visible

**Problem**: "Test Crash" button doesn't appear in the app

**Cause**: Button is only visible in DEBUG builds

**Solutions**:
1. Verify you're running a DEBUG build (not Release)
2. Check `CrashTestView.swift` contains `#if DEBUG` directive
3. Clean build folder: **Product > Clean Build Folder** (Cmd+Shift+K)

### App Doesn't Crash

**Problem**: Tapping "Test Crash" doesn't crash the app

**Solutions**:
1. Stop and relaunch the app from home screen (not Xcode)
2. Ensure debugger is disconnected (click ⏹️ Stop in Xcode first)
3. Check console for "Test crash button pressed" log message

### dSYM Upload Issues

**Problem**: Crash reports appear but are not symbolicated (show hex addresses)

**Solution**: Configure dSYM automatic upload

1. In Xcode, select your project > Target > Build Phases
2. Add **New Run Script Phase**
3. Ensure it's the **last** build phase
4. Add script:
   ```bash
   "${BUILD_DIR%/Build/*}/SourcePackages/checkouts/firebase-ios-sdk/Crashlytics/run"
   ```
5. Add Input Files:
   ```
   ${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}
   ${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}/Contents/Resources/DWARF/${PRODUCT_NAME}
   ${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}/Contents/Info.plist
   $(TARGET_BUILD_DIR)/$(UNLOCALIZED_RESOURCES_FOLDER_PATH)/GoogleService-Info.plist
   $(TARGET_BUILD_DIR)/$(EXECUTABLE_PATH)
   ```

## Production Considerations

### Removing Test Button for Release

The test crash button is automatically hidden in **Release** builds due to `#if DEBUG` directive. No manual changes needed.

### Verify Release Build
1. Select **Product > Scheme > Edit Scheme**
2. Change **Run** configuration to **Release**
3. Build and verify test button is not visible

### Production Crashlytics

In production, Firebase Crashlytics will automatically:
- Capture all unhandled exceptions and crashes
- Send reports when app restarts
- Provide symbolicated stack traces (if dSYM upload is configured)
- Track crash-free users percentage
- Group similar crashes together

## Testing Best Practices

1. **Test on Real Device**: Simulators may have different behavior than real devices
2. **Test Different iOS Versions**: Verify Crashlytics works on all supported versions
3. **Test Network Conditions**: Verify crash reports upload on slow/intermittent networks
4. **Verify dSYM Upload**: Check Firebase Console shows symbolicated stack traces
5. **Monitor First 24 Hours**: Watch for real crashes after release

## Additional Resources

- [Firebase Crashlytics iOS Setup](https://firebase.google.com/docs/crashlytics/get-started?platform=ios)
- [Symbolicate iOS Crash Reports](https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports?platform=ios)
- [Test Crashlytics Implementation](https://firebase.google.com/docs/crashlytics/force-a-crash?platform=ios)

## Implementation Details

### CrashTestView Component

```swift
// Only visible in DEBUG builds
struct CrashTestView: View {
    var body: some View {
        #if DEBUG
        // Red button in bottom-right corner
        Button(action: { triggerTestCrash() }) {
            HStack {
                Image(systemName: "exclamationmark.triangle.fill")
                Text("Test Crash")
            }
            .padding()
            .background(Color.red)
            .foregroundColor(.white)
            .cornerRadius(8)
        }
        #endif
    }

    private func triggerTestCrash() {
        Crashlytics.crashlytics().log("Test crash button pressed")
        Crashlytics.crashlytics().setCustomValue("test", forKey: "crash_type")
        let numbers = [0]
        let _ = numbers[1]  // Fatal crash: Index out of range
    }
}
```

### Integration in ContentView

```swift
struct ContentView: View {
    var body: some View {
        ZStack {
            ComposeView()  // KMP Compose UI
            CrashTestView()  // Overlay test button (DEBUG only)
        }
    }
}
```

---

**Status**: ✅ Test crash button fully integrated (January 2025)
**Platforms**: iOS (Device and Simulator)
**Build Configuration**: DEBUG only (automatically hidden in Release)
