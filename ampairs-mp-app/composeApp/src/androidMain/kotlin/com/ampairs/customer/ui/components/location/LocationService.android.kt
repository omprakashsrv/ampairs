package com.ampairs.customer.ui.components.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Android implementation of LocationService using Google Play Services
 * Provides real GPS location, Google Maps integration, and Geocoding
 */
@OptIn(ExperimentalTime::class)
actual class LocationService(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val geocoder: Geocoder? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Modern Android (API 33+) - callback-based geocoder
        Geocoder(context)
    } else if (Geocoder.isPresent()) {
        // Legacy Android with geocoder support
        Geocoder(context)
    } else {
        null
    }

    actual suspend fun getCurrentLocation(): Result<LocationData> {
        return withContext(Dispatchers.IO) {
            try {
                // Check permissions
                if (!hasLocationPermissionInternal()) {
                    return@withContext Result.failure(LocationError.PermissionDenied)
                }

                // Request current location with high accuracy
                val cancellationToken = CancellationTokenSource()
                val location = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationToken.token
                ).await()

                if (location != null) {
                    // Get address from coordinates
                    val address = try {
                        val addressData = reverseGeocode(location.latitude, location.longitude).getOrNull()
                        addressData?.formattedAddress
                    } catch (e: Exception) {
                        null
                    }

                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address,
                        accuracy = location.accuracy.toDouble(),
                        timestamp = location.time
                    )
                    Result.success(locationData)
                } else {
                    Result.failure(LocationError.LocationUnavailable)
                }
            } catch (e: SecurityException) {
                Result.failure(LocationError.PermissionDenied)
            } catch (e: Exception) {
                Result.failure(LocationError.LocationUnavailable)
            }
        }
    }

    actual suspend fun selectLocationFromMap(initialLocation: LocationData?): Result<LocationData> {
        return withContext(Dispatchers.Main) {
            try {
                // This will be handled by the MapPickerScreen composable
                // The actual implementation involves launching an Activity/composable
                // For now, return a failure to indicate it needs UI integration
                Result.failure(LocationError.ServiceUnavailable)
            } catch (e: Exception) {
                Result.failure(LocationError.ServiceUnavailable)
            }
        }
    }

    actual suspend fun reverseGeocode(latitude: Double, longitude: Double): Result<AddressData> {
        return withContext(Dispatchers.IO) {
            try {
                if (geocoder == null) {
                    return@withContext Result.failure(LocationError.ServiceUnavailable)
                }

                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Modern Android (API 33+) - use callback-based API
                    callbackFlow {
                        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                            trySend(addresses)
                        }
                        awaitClose {}
                    }.first()
                } else {
                    // Legacy Android - use synchronous API
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                }

                if (addresses.isNullOrEmpty()) {
                    return@withContext Result.failure(LocationError.ServiceUnavailable)
                }

                val address = addresses.first()
                val addressData = address.toAddressData()
                Result.success(addressData)
            } catch (e: Exception) {
                Result.failure(LocationError.NetworkError)
            }
        }
    }

    actual suspend fun searchLocations(query: String): Result<List<LocationSearchResult>> {
        return withContext(Dispatchers.IO) {
            try {
                if (geocoder == null) {
                    return@withContext Result.failure(LocationError.ServiceUnavailable)
                }

                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Modern Android (API 33+) - use callback-based API
                    callbackFlow {
                        geocoder.getFromLocationName(query, 5) { addresses ->
                            trySend(addresses)
                        }
                        awaitClose {}
                    }.first()
                } else {
                    // Legacy Android - use synchronous API
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 5)
                }

                if (addresses.isNullOrEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val results = addresses.map { address ->
                    LocationSearchResult(
                        location = LocationData(
                            latitude = address.latitude,
                            longitude = address.longitude,
                            address = address.getAddressLine(0)
                        ),
                        address = address.toAddressData(),
                        displayName = address.getAddressLine(0) ?: query,
                        type = determineLocationType(address)
                    )
                }

                Result.success(results)
            } catch (e: Exception) {
                Result.failure(LocationError.NetworkError)
            }
        }
    }

    actual suspend fun hasLocationPermission(): Boolean {
        return hasLocationPermissionInternal()
    }

    actual suspend fun requestLocationPermission(): Boolean {
        // Permission requests must be done from Activity/Fragment context
        // This will be handled by the UI layer using Accompanist Permissions
        // Return current permission status
        return hasLocationPermissionInternal()
    }

    private fun hasLocationPermissionInternal(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    private fun Address.toAddressData(): AddressData {
        return AddressData(
            formattedAddress = getAddressLine(0) ?: "",
            street = thoroughfare,
            streetNumber = subThoroughfare,
            city = locality,
            state = adminArea,
            pincode = postalCode,
            country = countryName,
            countryCode = countryCode,
            locality = subLocality,
            subLocality = featureName
        )
    }

    private fun determineLocationType(address: Address): LocationType {
        return when {
            address.featureName != null && address.thoroughfare != null -> LocationType.ADDRESS
            address.locality != null && address.adminArea != null -> LocationType.CITY
            address.adminArea != null -> LocationType.STATE
            address.countryName != null -> LocationType.COUNTRY
            else -> LocationType.UNKNOWN
        }
    }
}
