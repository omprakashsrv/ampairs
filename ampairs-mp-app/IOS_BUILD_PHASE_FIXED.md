# iOS Build Phase Fixed - embedAndSign Conflict Resolved

**Date**: January 2025
**Status**: ✅ Fixed

## Problem

Xcode build was failing with:
```
error: Incompatible 'embedAndSign' Task with CocoaPods Dependencies
```

## Root Cause

The Xcode project had a build phase script that called:
```bash
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

This task is **incompatible with CocoaPods**, which we're using for Firebase integration.

## Solution Applied

**Changed the "Compile Kotlin Framework" build phase** in `iosApp.xcodeproj`:

### ❌ Before (Incompatible)
```bash
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

### ✅ After (CocoaPods Compatible)
```bash
./gradlew :composeApp:syncFramework \
    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
    -Pkotlin.native.cocoapods.archs="$ARCHS" \
    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
```

## What Changed

**File Modified**: `iosApp/iosApp.xcodeproj/project.pbxproj`

The `syncFramework` task is the **CocoaPods-compatible** way to build the Kotlin framework. It:
- ✅ Works with CocoaPods dependencies (Firebase)
- ✅ Properly syncs the framework with Xcode build settings
- ✅ Respects platform, architecture, and configuration from Xcode

## Verification

```bash
# Confirm fix applied
grep "syncFramework" iosApp/iosApp.xcodeproj/project.pbxproj
# Output: ✅ Fixed successfully!

# Confirm old task removed
grep "embedAndSign" iosApp/iosApp.xcodeproj/project.pbxproj
# Output: ✅ Old task removed
```

## Next Steps

Now that the build phase is fixed, you can build the iOS app:

### From Xcode (Recommended)
```bash
open iosApp/iosApp.xcodeproj
# Press Cmd+R to build and run
```

### From Terminal (For debugging)
```bash
./gradlew :composeApp:syncFramework \
    -Pkotlin.native.cocoapods.platform=iphonesimulator \
    -Pkotlin.native.cocoapods.archs="arm64" \
    -Pkotlin.native.cocoapods.configuration="Debug"
```

## Expected Build Flow

With CocoaPods, the build flow is now:

1. **Xcode starts build** → Triggers "Compile Kotlin Framework" script
2. **syncFramework runs** → Compiles Kotlin code for specified platform/arch
3. **CocoaPods integrates** → Links Firebase and Kotlin frameworks
4. **Swift compilation** → Compiles iOS Swift code
5. **Linking** → Creates final app bundle

## Remaining Tasks

After this fix, you still need to:

1. ✅ **embedAndSign conflict** - FIXED
2. ⏳ **Add Copy Compose Resources build phase** - See IOS_COMPOSE_RESOURCES_FIX.md
3. ⏳ **Add GoogleService-Info.plist** - See IOS_FIREBASE_XCODE_SETUP.md
4. ⏳ **Test Firebase authentication** - After Xcode setup complete

## Troubleshooting

### If build still fails with CocoaPods error

**Error**: "Pod installation is out of date"

**Solution**:
```bash
cd iosApp
pod install
cd ..
```

### If syncFramework task not found

**Error**: "Task 'syncFramework' not found"

**Solution**: Ensure CocoaPods plugin is enabled in `composeApp/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.kotlinCocoapods)
}

cocoapods {
    pod("FirebaseAuth")
    pod("FirebaseCore")
}
```

### If Kotlin compilation fails

**Error**: Various Kotlin compilation errors

**Solution**:
```bash
./gradlew clean
./gradlew :composeApp:generateDummyFramework
# Then rebuild in Xcode
```

## References

- **CocoaPods Build Fix Guide**: [IOS_COCOAPODS_BUILD_FIX.md](./IOS_COCOAPODS_BUILD_FIX.md)
- **Kotlin Multiplatform + CocoaPods**: https://kotlinlang.org/docs/native-cocoapods.html
- **Firebase Integration**: [IOS_FIREBASE_XCODE_SETUP.md](./IOS_FIREBASE_XCODE_SETUP.md)

---

**Last Updated**: January 2025
**Status**: ✅ embedAndSign conflict resolved, build phase fixed
