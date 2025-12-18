package com.example.limouserapp.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.example.limouserapp.data.model.dashboard.LocationData
import com.example.limouserapp.data.model.dashboard.LocationPermissionStatus
import com.example.limouserapp.data.model.dashboard.LocationServiceState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Location manager for handling GPS and location services
 * Provides location updates, geocoding, and permission management
 */
@Singleton
class LocationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    private val geocoder = Geocoder(context, Locale.getDefault())
    
    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if background location permission is granted
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get current location permission status
     */
    fun getLocationPermissionStatus(): LocationPermissionStatus {
        return when {
            hasLocationPermission() -> LocationPermissionStatus.GRANTED
            else -> LocationPermissionStatus.NOT_DETERMINED
        }
    }
    
    /**
     * Get current location service state
     */
    fun getLocationServiceState(): LocationServiceState {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
            
            when {
                isGpsEnabled || isNetworkEnabled -> LocationServiceState.ENABLED
                else -> LocationServiceState.DISABLED
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking location service state")
            LocationServiceState.NOT_AVAILABLE
        }
    }
    
    /**
     * Get current location
     */
    suspend fun getCurrentLocation(): Result<LocationData> {
        return try {
            if (!hasLocationPermission()) {
                return Result.failure(SecurityException("Location permission not granted"))
            }
            
            val location = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
            
            if (location != null) {
                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = "Current Location" // Will be updated by reverse geocoding
                )
                
                // Reverse geocode to get address
                val geocodedLocation = reverseGeocode(location.latitude, location.longitude)
                Result.success(geocodedLocation)
            } else {
                Result.failure(Exception("Unable to get current location"))
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Location permission denied")
            Result.failure(e)
        } catch (e: Exception) {
            Timber.e(e, "Error getting current location")
            Result.failure(e)
        }
    }
    
    /**
     * Reverse geocode coordinates to get address
     */
    suspend fun reverseGeocode(latitude: Double, longitude: Double): LocationData {
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            
            if (addresses?.isNotEmpty() == true) {
                val address = addresses[0]
                LocationData(
                    latitude = latitude,
                    longitude = longitude,
                    address = buildAddressString(address),
                    city = address.locality,
                    state = address.adminArea,
                    country = address.countryName,
                    postalCode = address.postalCode,
                    formattedAddress = address.getAddressLine(0)
                )
            } else {
                LocationData(
                    latitude = latitude,
                    longitude = longitude,
                    address = "Unknown Location"
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error reverse geocoding location")
            LocationData(
                latitude = latitude,
                longitude = longitude,
                address = "Current Location"
            )
        }
    }
    
    /**
     * Geocode address to get coordinates
     */
    suspend fun geocodeAddress(address: String): Result<LocationData> {
        return try {
            val addresses = geocoder.getFromLocationName(address, 1)
            
            if (addresses?.isNotEmpty() == true) {
                val addressObj = addresses[0]
                val locationData = LocationData(
                    latitude = addressObj.latitude,
                    longitude = addressObj.longitude,
                    address = address,
                    city = addressObj.locality,
                    state = addressObj.adminArea,
                    country = addressObj.countryName,
                    postalCode = addressObj.postalCode,
                    formattedAddress = addressObj.getAddressLine(0)
                )
                Result.success(locationData)
            } else {
                Result.failure(Exception("No results found for address: $address"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error geocoding address: $address")
            Result.failure(e)
        }
    }
    
    /**
     * Build address string from Address object
     */
    private fun buildAddressString(address: android.location.Address): String {
        val parts = mutableListOf<String>()
        
        address.subThoroughfare?.let { parts.add(it) }
        address.thoroughfare?.let { parts.add(it) }
        address.locality?.let { parts.add(it) }
        address.adminArea?.let { parts.add(it) }
        address.countryName?.let { parts.add(it) }
        
        return parts.joinToString(", ")
    }
    
    /**
     * Check if location services are available
     */
    fun isLocationServiceAvailable(): Boolean {
        return getLocationServiceState() == LocationServiceState.ENABLED
    }
    
    /**
     * Get default location (New York City)
     */
    fun getDefaultLocation(): LocationData {
        return LocationData(
            latitude = 40.7128,
            longitude = -74.0060,
            address = "New York, NY, USA",
            city = "New York",
            state = "NY",
            country = "USA"
        )
    }
}
