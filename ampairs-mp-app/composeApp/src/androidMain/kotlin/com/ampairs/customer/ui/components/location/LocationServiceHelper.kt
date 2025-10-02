package com.ampairs.customer.ui.components.location

import android.Manifest
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Composable that listens for location service requests (map selection and permissions)
 * and handles them with appropriate UI components.
 *
 * Place this at the root of your Android app to enable:
 * - selectLocationFromMap() functionality
 * - Automatic permission request when getCurrentLocation() is called
 *
 * Example:
 * ```
 * @Composable
 * fun MyApp() {
 *     LocationServiceMapHandler()
 *     // ... rest of your app
 * }
 * ```
 */
@Composable
fun LocationServiceMapHandler() {
    val locationService: LocationService = koinInject()
    val scope = rememberCoroutineScope()

    // Map selection state
    var showMapPicker by remember { mutableStateOf(false) }
    var currentMapRequest by remember { mutableStateOf<MapSelectionRequest?>(null) }

    // Permission request state
    var showPermissionDialog by remember { mutableStateOf(false) }
    var currentPermissionRequest by remember { mutableStateOf<PermissionRequest?>(null) }

    // Collect map selection requests
    LaunchedEffect(Unit) {
        scope.launch {
            locationService.mapSelectionRequests.collectLatest { request ->
                currentMapRequest = request
                showMapPicker = true
            }
        }
    }

    // Collect permission requests
    LaunchedEffect(Unit) {
        scope.launch {
            locationService.permissionRequests.collectLatest { request ->
                currentPermissionRequest = request
                showPermissionDialog = true
            }
        }
    }

    // Show permission handler when requested
    if (showPermissionDialog && currentPermissionRequest != null) {
        PermissionRequestDialog(
            onPermissionResult = { granted ->
                locationService.completePermissionRequest(granted)
                showPermissionDialog = false
                currentPermissionRequest = null
            },
            onDismiss = {
                locationService.cancelPermissionRequest()
                showPermissionDialog = false
                currentPermissionRequest = null
            }
        )
    }

    // Show map picker when requested
    if (showMapPicker && currentMapRequest != null) {
        Dialog(
            onDismissRequest = {
                locationService.cancelMapSelection()
                showMapPicker = false
                currentMapRequest = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MapPickerScreen(
                initialLocation = currentMapRequest?.initialLocation,
                onLocationSelected = { location ->
                    locationService.completeMapSelection(location)
                    showMapPicker = false
                    currentMapRequest = null
                },
                onDismiss = {
                    locationService.cancelMapSelection()
                    showMapPicker = false
                    currentMapRequest = null
                }
            )
        }
    }
}

/**
 * Internal composable that wraps LocationPermissionHandler and provides result callback
 * Automatically launches the system permission request dialog
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionRequestDialog(
    onPermissionResult: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Automatically launch permission request when dialog shows
    LaunchedEffect(Unit) {
        if (!locationPermissionState.allPermissionsGranted) {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    // Monitor permission state changes
    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            onPermissionResult(true)
        }
    }

    // Monitor if user denied permission
    LaunchedEffect(locationPermissionState.permissions.map { it.status }) {
        // If not all permissions granted and request has been made
        if (!locationPermissionState.allPermissionsGranted) {
            // Check if any permission was explicitly denied (not just not granted yet)
            val hasBeenDenied = locationPermissionState.permissions.any { perm ->
                when (perm.status) {
                    is com.google.accompanist.permissions.PermissionStatus.Denied -> true
                    else -> false
                }
            }

            if (hasBeenDenied) {
                onPermissionResult(false)
            }
        }
    }

    // Show the permission handler UI
    LocationPermissionHandler(
        onPermissionGranted = {
            // Permission was granted, callback already handled by LaunchedEffect
        }
    )
}
