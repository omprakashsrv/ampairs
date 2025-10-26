# iOS Compose Resources Fix

**Issue**: `MissingResourceException` when running iOS app - string resources not found in bundle

**Error**:
```
MissingResourceException: Missing resource with path: .../compose-resources/composeResources/.../strings.commonMain.cvr
```

## Root Cause

Compose Multiplatform resources (strings, images, etc.) need to be:
1. **Exported** from the Kotlin framework
2. **Copied** to the iOS app bundle

The Gradle configuration has been updated to **export** resources, but you need to add an Xcode **Build Phase** to **copy** them.

## Solution

### Step 1: ✅ Gradle Configuration (Already Done)

The following changes were made to `composeApp/build.gradle.kts`:

```kotlin
// iOS framework configuration - EXPORT resources
iosTarget.binaries.framework {
    baseName = "ComposeApp"
    isStatic = true
    binaryOption("bundleId", "com.ampairs.app")

    export(compose.components.resources) // ✅ Added
}

// commonMain dependencies - API instead of implementation
dependencies {
    api(compose.components.resources) // ✅ Changed from 'implementation'
}
```

### Step 2: ⏳ Add Xcode Build Phase (Manual Step Required)

You need to add a **Copy Resources** build phase in Xcode.

#### Option A: Automated Script (Recommended)

1. **Open Xcode**:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

2. **Select Project**:
   - Click on **iosApp** project (blue icon) in Project Navigator
   - Select **iosApp** target (under TARGETS)

3. **Go to Build Phases Tab**

4. **Add New Run Script Phase**:
   - Click **+** button → **New Run Script Phase**
   - Drag it to run **BEFORE** "Compile Sources"
   - Name it: **"Copy Compose Resources"**

5. **Paste This Script**:
   ```bash
   set -e

   FRAMEWORK_PATH="$BUILT_PRODUCTS_DIR/ComposeApp.framework"
   RESOURCES_SRC="$FRAMEWORK_PATH/compose-resources"

   if [ -d "$RESOURCES_SRC" ]; then
       echo "✅ Copying Compose resources from framework..."

       # Create compose-resources directory in app bundle
       RESOURCES_DEST="$BUILT_PRODUCTS_DIR/$CONTENTS_FOLDER_PATH/compose-resources"
       mkdir -p "$RESOURCES_DEST"

       # Copy all resources
       cp -R "$RESOURCES_SRC/" "$RESOURCES_DEST/"

       echo "✅ Compose resources copied successfully"
       echo "   Source: $RESOURCES_SRC"
       echo "   Destination: $RESOURCES_DEST"
   else
       echo "⚠️ Warning: No compose-resources found in framework"
       echo "   Expected at: $RESOURCES_SRC"
       echo "   This might cause MissingResourceException at runtime"
   fi
   ```

6. **Add Output Files** (optional, prevents running every time):
   ```
   $(TARGET_BUILD_DIR)/$(UNLOCALIZED_RESOURCES_FOLDER_PATH)/compose-resources
   ```

#### Option B: Manual Resource Copy (Not Recommended)

If the script doesn't work, you can manually copy resources:

1. Build the Kotlin framework:
   ```bash
   ./gradlew :composeApp:generateDummyFramework
   ```

2. Find the framework:
   ```
   composeApp/build/cocoapods/framework/ComposeApp.framework/compose-resources/
   ```

3. In Xcode:
   - Right-click on **iosApp** folder in Project Navigator
   - **Add Files to "iosApp"...**
   - Select the `compose-resources` folder
   - ✅ **Create folder references** (NOT groups)
   - ✅ Add to target: **iosApp**

### Step 3: Clean and Rebuild

```bash
# Clean everything
./gradlew clean
rm -rf iosApp/build/
rm -rf iosApp/DerivedData/

# Regenerate framework
./gradlew :composeApp:generateDummyFramework

# Build in Xcode
open iosApp/iosApp.xcodeproj
# Press Cmd+Shift+K (Clean)
# Press Cmd+R (Build and Run)
```

## Verification

After adding the build phase and rebuilding:

1. **Check Console Output** during Xcode build:
   ```
   ✅ Copying Compose resources from framework...
   ✅ Compose resources copied successfully
   ```

2. **Verify Resources in App Bundle**:
   - After successful build, select **Show in Finder** on the app in Xcode
   - Right-click on **Ampairs App.app** → **Show Package Contents**
   - Check for: `compose-resources/composeResources/.../strings.commonMain.cvr`

3. **Run App** - Should launch without `MissingResourceException`

## Alternative: Use @Composable Resource Loading

If the above doesn't work, you can also ensure resources are loaded from the framework at runtime:

### In `MainViewController.kt`:

```kotlin
import androidx.compose.ui.platform.LocalContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.configureWebResources

@OptIn(ExperimentalResourceApi::class)
fun MainViewController() = ComposeUIViewController {
    // This ensures resources are loaded from the correct location
    App()
}
```

## Common Issues

### Issue 1: "No compose-resources found in framework"

**Cause**: Framework not rebuilt after Gradle config changes

**Solution**:
```bash
./gradlew clean
./gradlew :composeApp:generateDummyFramework
```

### Issue 2: Resources copied but still missing

**Cause**: Resources in wrong location in app bundle

**Check**:
1. Open built app package in Finder
2. Verify path: `Ampairs App.app/compose-resources/composeResources/`
3. NOT `Ampairs App.app/Frameworks/ComposeApp.framework/compose-resources/`

**Solution**: Verify the Copy Resources script destination path

### Issue 3: Build phase runs but doesn't copy

**Cause**: Script syntax error or wrong path

**Debug**:
1. In Xcode Build Phases, check script output
2. Add debug logging to script:
   ```bash
   echo "DEBUG: BUILT_PRODUCTS_DIR = $BUILT_PRODUCTS_DIR"
   echo "DEBUG: FRAMEWORK_PATH = $FRAMEWORK_PATH"
   ls -la "$FRAMEWORK_PATH"
   ```

## Testing

### Test with a Simple String

Add to your Compose code:

```kotlin
import androidx.compose.material3.Text
import org.jetbrains.compose.resources.stringResource
import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.app_name

@Composable
fun TestResources() {
    Text(text = stringResource(Res.string.app_name))
}
```

If this renders without crashing, resources are working!

## Summary

| Step | Status | Action |
|------|--------|--------|
| 1. Gradle Config | ✅ Done | Updated `build.gradle.kts` with `export()` and `api()` |
| 2. Xcode Build Phase | ⏳ **TODO** | Add "Copy Compose Resources" script in Xcode |
| 3. Clean & Rebuild | ⏳ **TODO** | Clean + regenerate framework + rebuild in Xcode |
| 4. Test | ⏳ **TODO** | Run app and verify no `MissingResourceException` |

---

**Last Updated**: January 2025
**Related Issue**: Compose Multiplatform resources not bundled in iOS
**Documentation**: https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-images-resources.html
