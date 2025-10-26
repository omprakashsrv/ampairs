# iOS Framework Search Paths Fixed

**Date**: January 2025
**Status**: ✅ Fixed

## Problem

Xcode was looking for the Kotlin framework in the wrong location:
```
❌ Search path '.../shared/build/xcode-frameworks/Debug/iphonesimulator18.5' not found
❌ Search path '.../composeApp/build/xcode-frameworks/Debug/iphonesimulator18.5' not found
❌ No such module 'ComposeApp'
```

## Root Cause

The Xcode project had outdated framework search paths configured for the old `embedAndSign` workflow:
```
❌ OLD: $(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
```

When using CocoaPods, the framework is built to a different location:
```
✅ CORRECT: $(SRCROOT)/../composeApp/build/cocoapods/framework
```

## Solution Applied

**Modified**: `iosApp/iosApp.xcodeproj/project.pbxproj`

### Before
```xml
FRAMEWORK_SEARCH_PATHS = (
    "$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)\n
     $(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)",
);
```

### After
```xml
FRAMEWORK_SEARCH_PATHS = (
    "$(SRCROOT)/../composeApp/build/cocoapods/framework",
);
```

## Verification

```bash
# Check framework exists
ls composeApp/build/cocoapods/framework/ComposeApp.framework
# Output: ✅ ComposeApp.framework found

# Verify Xcode configuration
grep "FRAMEWORK_SEARCH_PATHS" iosApp/iosApp.xcodeproj/project.pbxproj
# Output: ✅ Points to cocoapods/framework
```

## Next Steps

Now you can build the iOS app:

```bash
# Open in Xcode
open iosApp/iosApp.xcodeproj

# Or build from command line
xcodebuild -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -sdk iphonesimulator \
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro'
```

## Changes Summary

| Configuration | Old Path | New Path |
|---------------|----------|----------|
| Debug | `xcode-frameworks/Debug/$(SDK_NAME)` | `cocoapods/framework` |
| Release | `xcode-frameworks/Release/$(SDK_NAME)` | `cocoapods/framework` |

## Related Fixes

This is part of the CocoaPods migration. Related changes:

1. ✅ **Build Phase**: Changed from `embedAndSign` to `syncFramework`
2. ✅ **Framework Paths**: Updated to CocoaPods location
3. ⏳ **Compose Resources**: Need to add copy build phase
4. ⏳ **Firebase Config**: Need to add GoogleService-Info.plist in Xcode

## Troubleshooting

### If "No such module 'ComposeApp'" persists

**Clean and rebuild**:
```bash
# Clean Xcode build
cd iosApp
xcodebuild clean -project iosApp.xcodeproj
cd ..

# Regenerate framework
./gradlew clean
./gradlew :composeApp:generateDummyFramework

# Rebuild in Xcode
open iosApp/iosApp.xcodeproj
# Cmd+Shift+K (Clean)
# Cmd+R (Build and Run)
```

### If framework not found at cocoapods/framework

**Rebuild framework**:
```bash
./gradlew :composeApp:podInstall
./gradlew :composeApp:generateDummyFramework
```

---

**Last Updated**: January 2025
**Status**: ✅ Framework paths fixed for CocoaPods
