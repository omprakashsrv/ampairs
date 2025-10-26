# iOS CocoaPods Build Configuration Fix

**Issue**: `embedAndSignAppleFrameworkForXcode` task is incompatible with CocoaPods dependencies

**Solution**: Use CocoaPods workflow for building, not Gradle embedAndSign tasks

## The Problem

When using CocoaPods (for Firebase integration), you **cannot** use the standard Gradle `embedAndSignAppleFrameworkForXcode` task because:

1. CocoaPods manages the framework itself via podspec
2. The framework is built as part of `pod install` workflow
3. Using both causes conflicts in Xcode build phases

## The Solution

### Option 1: Use CocoaPods Workflow (Recommended for Firebase)

Since we're using Firebase via CocoaPods, follow this workflow:

#### Step 1: Generate Dummy Framework
```bash
./gradlew :composeApp:generateDummyFramework
```

This creates the initial framework structure that CocoaPods expects.

#### Step 2: Install Pods (If you have a Podfile in iosApp/)
If you have set up a Podfile in the `iosApp/` directory:
```bash
cd iosApp
pod install
cd ..
```

**If no Podfile exists**, CocoaPods integration is handled automatically through the Gradle plugin.

#### Step 3: Open Workspace in Xcode
If `pod install` created a workspace:
```bash
open iosApp/iosApp.xcworkspace  # NOT .xcodeproj
```

If no workspace exists (automatic integration):
```bash
open iosApp/iosApp.xcodeproj
```

#### Step 4: Build in Xcode
- Select the `iosApp` scheme
- Press Cmd+R to build and run
- Xcode will automatically trigger Kotlin compilation via Build Phases

### Option 2: Remove CocoaPods and Use SPM Only

If you prefer to avoid CocoaPods entirely:

#### 1. Remove CocoaPods Configuration
Edit `composeApp/build.gradle.kts` and remove:
```kotlin
cocoapods {
    pod("FirebaseAuth")
    pod("FirebaseCore")
}
```

And remove the plugin:
```kotlin
plugins {
    // Remove this line:
    alias(libs.plugins.kotlinCocoapods)
}
```

#### 2. Add Firebase Manually in Xcode
1. Open Xcode
2. File → Add Package Dependencies
3. Add `https://github.com/firebase/firebase-ios-sdk`
4. Select FirebaseAuth and FirebaseCore

#### 3. Update iOS Implementation
Change imports in `FirebaseAuthProvider.ios.kt`:
```kotlin
// FROM (CocoaPods):
import cocoapods.FirebaseAuth.*
import cocoapods.FirebaseCore.FIRApp

// TO (Manual framework):
// You'll need to create a Swift wrapper or use cinterop with manual framework
```

**Note**: This approach requires more manual setup and may lose some type safety.

## Recommended Approach

**Stick with CocoaPods** (Option 1) because:
- ✅ Firebase integration is already configured
- ✅ Type-safe Kotlin bindings via cinterop
- ✅ Automatic dependency management
- ✅ Code already written and working

## Build Process with CocoaPods

### For Development (in Xcode)

1. **Initial Setup** (once):
   ```bash
   ./gradlew :composeApp:generateDummyFramework
   ```

2. **Open in Xcode**:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

3. **Build and Run**:
   - Just press Cmd+R in Xcode
   - Xcode's build phases will call Kotlin compilation automatically

### For CI/CD or Command Line

#### Build iOS App from Command Line
```bash
# 1. Generate framework
./gradlew :composeApp:generateDummyFramework

# 2. Build using xcodebuild
xcodebuild -project iosApp/iosApp.xcodeproj \
    -scheme iosApp \
    -configuration Debug \
    -sdk iphonesimulator \
    -destination 'platform=iOS Simulator,name=iPhone 15 Pro'
```

#### Or use Gradle wrapper (if configured)
```bash
# This might work if your Gradle is configured for iOS builds
./gradlew iosApp:build
```

## Xcode Build Phase Configuration

When using CocoaPods, your Xcode project should have these build phases:

### Current Build Phases (Check in Xcode)

1. **Dependencies** (automatic)
2. **Sources** (compile Swift/Objective-C)
3. **Frameworks** (link frameworks including CocoaPods)
4. **Compile Kotlin Framework** (custom script phase)
   ```bash
   cd "$SRCROOT/.."
   ./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
   ```
   ⚠️ **THIS SCRIPT SHOULD BE REMOVED OR MODIFIED**

### Correct Build Phase for CocoaPods

Replace "Compile Kotlin Framework" script with:
```bash
set -ev
cd "$SRCROOT/.."

# Use podSpec task instead of embedAndSign
./gradlew :composeApp:syncFramework \
    -Pkotlin.native.cocoapods.platform=$PLATFORM_NAME \
    -Pkotlin.native.cocoapods.archs="$ARCHS" \
    -Pkotlin.native.cocoapods.configuration="$CONFIGURATION"
```

**Or simply remove it** and let Xcode's native build system handle it through CocoaPods.

## Fixing "Based on dependency analysis" Warning

To fix the warning about running during every build:

### In Xcode:

1. Select **iosApp** project
2. Select **iosApp** target
3. Go to **Build Phases** tab
4. Find **"Compile Kotlin Framework"** script phase
5. Click on it to expand
6. **Uncheck** "Based on dependency analysis"

   OR add output files:
   ```
   Output Files:
   $(SRCROOT)/../composeApp/build/cocoapods/framework/ComposeApp.framework
   ```

## Testing the Fix

### 1. Clean Everything
```bash
# Clean Gradle builds
./gradlew clean

# Clean Xcode builds
cd iosApp
rm -rf build/
rm -rf DerivedData/
xcodebuild clean -project iosApp.xcodeproj -scheme iosApp
cd ..
```

### 2. Regenerate Framework
```bash
./gradlew :composeApp:generateDummyFramework
```

### 3. Build in Xcode
```bash
open iosApp/iosApp.xcodeproj
# Press Cmd+R to build and run
```

## Common Errors and Solutions

### Error: "Kotlin framework 'ComposeApp' doesn't exist"
**Solution**: Run `./gradlew :composeApp:generateDummyFramework`

### Error: "embedAndSignAppleFrameworkForXcode task not found"
**Solution**: Use `syncFramework` or `podInstall` tasks instead

### Error: "No such module 'FirebaseCore'"
**Solution**:
1. Ensure `pod install` has run (or Gradle sync completed)
2. Check Xcode Framework Search Paths include CocoaPods frameworks

### Error: "Build phase 'Compile Kotlin Framework' will run every build"
**Solution**: Add output files to the script phase (see above)

## Quick Reference

| Task | CocoaPods Workflow | EmbedAndSign Workflow |
|------|-------------------|---------------------|
| Initial Setup | `generateDummyFramework` | `podInstall` |
| Daily Development | Build in Xcode (Cmd+R) | Build in Xcode |
| Clean Build | `./gradlew clean` + Xcode Clean | `./gradlew clean` |
| Framework Task | `syncFramework` | `embedAndSignAppleFrameworkForXcode` ❌ |
| Dependencies | CocoaPods (Podfile.lock) | Manual in Xcode |

## Conclusion

**For this project**: Use CocoaPods workflow since Firebase is already configured.

**Don't use**: `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`

**Do use**:
- `./gradlew :composeApp:generateDummyFramework` (once)
- Build directly in Xcode (Cmd+R) for development
- `./gradlew :composeApp:syncFramework` for CI/CD if needed

---

**Last Updated**: January 2025
