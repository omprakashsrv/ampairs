package com.ampairs.customer.ui.components.location

import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject

/**
 * Composable that listens for map selection requests from LocationService
 * and shows MapPickerScreen when needed.
 *
 * Place this at the root of your Android app to enable selectLocationFromMap() functionality.
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
    var showMapPicker by remember { mutableStateOf(false) }
    var currentRequest by remember { mutableStateOf<MapSelectionRequest?>(null) }

    // Collect map selection requests
    LaunchedEffect(Unit) {
        locationService.mapSelectionRequests.collectLatest { request ->
            currentRequest = request
            showMapPicker = true
        }
    }

    // Show map picker when requested
    if (showMapPicker && currentRequest != null) {
        Dialog(
            onDismissRequest = {
                locationService.cancelMapSelection()
                showMapPicker = false
                currentRequest = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            MapPickerScreen(
                initialLocation = currentRequest?.initialLocation,
                onLocationSelected = { location ->
                    locationService.completeMapSelection(location)
                    showMapPicker = false
                    currentRequest = null
                },
                onDismiss = {
                    locationService.cancelMapSelection()
                    showMapPicker = false
                    currentRequest = null
                }
            )
        }
    }
}
