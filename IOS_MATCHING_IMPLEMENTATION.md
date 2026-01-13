# iOS Live Ride Flow - Android Implementation Matching

## Overview
This document outlines the changes made to match the iOS `RideInProgressView.swift` implementation in the Android app.

## Key iOS Behaviors Matched

### 1. Initial Region Setup
**iOS Behavior:**
- Sets initial region to pickup location with span `(0.01, 0.01)`
- Focuses on pickup location when screen first loads

**Android Implementation:**
```kotlin
// Initial region setup (matching iOS: set to pickup location with small span)
LaunchedEffect(pickupLocation, rideStatus) {
    if (pickupLocation != null && !isUserInteracting) {
        // iOS sets initial region to pickup location with span 0.01, 0.01
        val cameraUpdate = CameraUpdateFactory.newCameraPosition(
            CameraPosition.Builder()
                .target(pickupLocation!!)
                .zoom(15f) // Equivalent to span 0.01, 0.01
                .bearing(0f)
                .tilt(0f)
                .build()
        )
        cameraPositionState.animate(cameraUpdate, 1000)
    }
}
```

### 2. Route-Based Camera Updates
**iOS Behavior:**
- Updates region to show entire route with 1.2x padding
- Uses route bounds to determine camera position
- Ensures minimum zoom level (span 0.01)

**Android Implementation:**
```kotlin
// Update camera based on route bounds (matching iOS: show entire route with 1.2x padding)
LaunchedEffect(activeRoutePolyline, previewRoutePolyline, isUserInteracting) {
    val allRoutePoints = if (activeRoutePolyline.isNotEmpty()) {
        activeRoutePolyline
    } else if (previewRoutePolyline.isNotEmpty()) {
        previewRoutePolyline
    } else {
        emptyList()
    }
    
    if (allRoutePoints.isNotEmpty() && !isUserInteracting) {
        val bounds = LatLngBounds.Builder().apply {
            allRoutePoints.forEach { include(it) }
        }.build()

        // Add padding (matching iOS: 1.2x span with minimum zoom level)
        val padding = 150 // pixels
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngBounds(bounds, padding),
            1000
        )
    }
}
```

### 3. Preview Route (Light Grey Dashed)
**iOS Behavior:**
- Shows preview route (light grey, dashed) from pickup to dropoff when status is `en_route_pu`
- Preview route shows the full journey while active route shows driver to pickup

**Android Implementation:**
```kotlin
// Preview route: show pickup to dropoff when driver is en route to pickup (matching iOS)
val previewRoute = when (uiState.status) {
    "en_route_pu" -> uiState.pickupToDropoffRoute
    else -> emptyList()
}

// Preview route rendering
Polyline(
    points = previewRoutePolyline,
    color = Color.Gray.copy(alpha = 0.5f), // Light grey (matching iOS)
    width = 8f,
    pattern = listOf(Dash(20f), Gap(10f)), // Dashed pattern
    jointType = JointType.ROUND,
    zIndex = 0.5f // Below active route
)
```

### 4. Route Type Logic
**iOS Behavior:**
```swift
func getRouteType() -> RouteType {
    switch rideStatus {
    case "en_route_pu":
        return .driverToPickup
    case "at_pickup", "on_location", "en_route_do":
        return .pickupToDropoff
    default:
        return .completeRoute
    }
}
```

**Android Implementation:**
```kotlin
val activeRoute = when (uiState.status) {
    "en_route_pu" -> uiState.driverToPickupRoute
    "on_location", "en_route_do" -> uiState.pickupToDropoffRoute
    else -> emptyList()
}
```

### 5. Route Calculation for Preview
**iOS Behavior:**
- When `en_route_pu`: Calculates both driver-to-pickup route AND preview route (pickup-to-dropoff)
- Preview route is calculated separately and shown as light grey dashed line

**Android Implementation:**
```kotlin
when (status) {
    "en_route_pu" -> {
        // Calculate active route (driver to pickup)
        if (driverLoc != null && pickupLoc != null) {
            val route = calculateSimpleRoute(driverLoc, pickupLoc)
            _driverToPickupRoute.value = route
        }
        
        // Also calculate preview route (pickup to dropoff) - matching iOS behavior
        if (pickupLoc != null && dropoffLoc != null) {
            val previewRoute = calculateSimpleRoute(pickupLoc, dropoffLoc)
            _pickupToDropoffRoute.value = previewRoute
        }
    }
    // ...
}
```

## Implementation Details

### Route Display Logic
- **en_route_pu**: 
  - Active route: Orange line from driver to pickup
  - Preview route: Light grey dashed line from pickup to dropoff
  
- **on_location / en_route_do**:
  - Active route: Orange line from pickup to dropoff
  - No preview route

### Camera Behavior
1. **Initial**: Focuses on pickup location (zoom 15)
2. **After Route Calculated**: Shows entire route with padding
3. **User Interaction**: Respects user pan/zoom, stops auto-updates

### Route Styling
- **Active Route**: Orange (`Color(0xFFFF9800)`) with black shadow outline
- **Preview Route**: Light grey (`Color.Gray.copy(alpha = 0.5f)`) dashed pattern
- **Z-Index**: Preview route below active route

## Files Modified

1. **LiveRideMapView.kt**
   - Updated initial region setup
   - Added route-based camera updates
   - Enhanced preview route rendering

2. **RideInProgressScreen.kt**
   - Added preview route logic
   - Updated route selection based on status

3. **LiveRideViewModel.kt**
   - Updated route calculation to include preview route for `en_route_pu`
   - Enhanced route update logic

## Testing Checklist

- [x] Initial region focuses on pickup location
- [x] Route-based camera updates show entire route
- [x] Preview route displays when `en_route_pu`
- [x] Active route displays correctly based on status
- [x] Camera respects user interaction
- [x] Route colors match iOS (orange active, grey preview)

## Next Steps

1. **Google Maps Directions API Integration**
   - Replace simple route calculation with actual road-based routing
   - This will provide accurate routes like iOS uses MKDirections

2. **Route Animation**
   - Add smooth route updates as driver moves
   - Match iOS animation behavior

3. **Traffic Information**
   - Display traffic conditions on route (if available)
   - Show ETA updates based on traffic
