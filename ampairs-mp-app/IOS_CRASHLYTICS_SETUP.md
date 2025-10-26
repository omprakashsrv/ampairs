# iOS Firebase Crashlytics Setup Guide

This document explains how to configure Firebase Crashlytics symbol upload for the iOS target in Xcode.

## Prerequisites

✅ **Already Completed:**
- Firebase CocoaPods dependencies added to `build.gradle.kts`
- Firebase initialized in `MainViewController.kt`
- Crashlytics implementation created in `FirebaseCrashlytics.ios.kt`

## Required: Add Crashlytics Upload Symbols Build Phase

To enable proper crash symbolication in Firebase Crashlytics, you need to add a build phase script in Xcode that uploads debug symbols (dSYMs) after each build.

### Steps to Add Upload Symbols Script

1. **Open Xcode Project**
   ```bash
   cd iosApp
   open iosApp.xcworkspace
   ```
   ⚠️ **Important**: Open the `.xcworkspace` file, NOT `.xcodeproj` (CocoaPods requirement)

2. **Select the iosApp Target**
   - In the Project Navigator (left sidebar), click on `iosApp` (the blue project icon)
   - In the main editor, select the `iosApp` target under "TARGETS"

3. **Add Build Phase**
   - Click on the "Build Phases" tab
   - Click the "+" button in the top-left corner of the Build Phases section
   - Select "New Run Script Phase"

4. **Configure the Run Script Phase**
   - Rename the phase to: `[Firebase Crashlytics] Upload Symbols`
   - In the script text box, add the following script:

   ```bash
   # Type a script or drag a script file from your workspace to insert its path.
   "${PODS_ROOT}/FirebaseCrashlytics/run"
   ```

5. **Configure Input Files**
   - Expand the "Input Files" section of the Run Script Phase
   - Click the "+" button and add these two input files:

   ```
   ${DWARF_DSYM_FOLDER_PATH}/${DWARF_DSYM_FILE_NAME}/Contents/Resources/DWARF/${TARGET_NAME}
   ${BUILT_PRODUCTS_DIR}/${INFOPLIST_PATH}
   ```

6. **Position the Build Phase**
   - **CRITICAL**: Drag this build phase to be **AFTER** "Compile Sources"
   - **CRITICAL**: Drag this build phase to be **BEFORE** "Thin Binary" (if present)
   - Recommended order:
     1. Dependencies
     2. Compile Sources
     3. **[Firebase Crashlytics] Upload Symbols** ← Add here
     4. Link Binary With Libraries
     5. Embed Frameworks
     6. etc.

7. **Save and Build**
   - Close the settings
   - Build the project (⌘+B)
   - Verify in build logs that the script runs successfully

### Verification

After adding the build phase, check the build output:

✅ **Success indicators:**
- Build log shows: `[Firebase Crashlytics] Uploading dSYM files`
- No error messages about missing Crashlytics scripts
- Firebase Console (Crashlytics section) shows app as "Activated"

❌ **Common Issues:**

**Issue**: `FirebaseCrashlytics/run: No such file or directory`
- **Solution**: Make sure you opened `.xcworkspace` and ran `pod install`

**Issue**: Symbols not appearing in Firebase Console
- **Solution**:
  1. Ensure GoogleService-Info.plist is added to the Xcode project
  2. Verify the script runs AFTER compile and BEFORE code signing
  3. Check that dSYM generation is enabled in Build Settings:
     - Search for "Debug Information Format"
     - Set to "DWARF with dSYM File" for Release builds

**Issue**: Build takes much longer after adding script
- **Solution**: This is expected - uploading symbols adds 10-30 seconds to build time

## Testing Crashlytics Integration

After setup, test that Crashlytics is working:

1. **Force a Test Crash** (in your Kotlin code):
   ```kotlin
   import com.ampairs.common.firebase.crashlytics.FirebaseCrashlytics
   import org.koin.compose.koinInject

   @Composable
   fun TestCrashlyticsScreen() {
       val crashlytics: FirebaseCrashlytics = koinInject()

       Button(onClick = {
           // Log before crash
           crashlytics.log("Test crash button clicked")
           crashlytics.setCustomKey("test_crash", true)

           // Record non-fatal exception
           crashlytics.recordException(Exception("Test exception"))

           // Force a crash (for testing only)
           throw RuntimeException("Test crash for Crashlytics")
       }) {
           Text("Test Crash")
       }
   }
   ```

2. **Run the App**
   - Build and run on iOS device or simulator
   - Tap the test crash button
   - App will crash and restart

3. **Verify in Firebase Console**
   - Go to Firebase Console → Crashlytics
   - Wait 5-10 minutes for crash report to appear
   - You should see the test crash with full stack trace

## Performance Monitoring Script (Optional)

If you also want to enable automatic screen tracking for Performance Monitoring, add another Run Script Phase:

**Script Name**: `[Firebase Performance] Generate Build Info`

**Script**:
```bash
"${PODS_ROOT}/FirebasePerformance/run"
```

**Position**: After "Compile Sources", can be before or after Crashlytics script

## Summary

| Feature | Requires Build Phase? | Status |
|---------|----------------------|---------|
| Firebase Analytics | ❌ No | ✅ Ready |
| Firebase Crashlytics | ✅ **Yes** (symbol upload) | ⚠️ Manual setup required |
| Firebase Performance | ⚠️ Optional (automatic traces) | ℹ️ Optional |

## Additional Resources

- [Firebase Crashlytics iOS Setup Guide](https://firebase.google.com/docs/crashlytics/get-started?platform=ios)
- [Symbolication Troubleshooting](https://firebase.google.com/docs/crashlytics/get-deobfuscated-reports?platform=ios)
- [CocoaPods Firebase Integration](https://firebase.google.com/docs/ios/installation-methods#cocoapods)

## Automation Note

The Crashlytics upload script is automatically configured in Android via the Gradle plugin. For iOS, it must be manually added to the Xcode project as described above.

**Why manual setup?**
- KMP CocoaPods integration doesn't support custom build phases
- Xcode project modifications must be done through Xcode UI or `.xcodeproj` scripting
- This is a one-time setup per developer machine

---

**Last Updated**: January 2025
**KMP Version**: Kotlin 2.2.21, Compose Multiplatform 1.9.1
**Firebase iOS SDK**: 11.13.0
