package com.example.limouserapp.ui.booking

import com.google.android.gms.maps.model.LatLng
import com.example.limouserapp.data.PlacePrediction
import com.example.limouserapp.data.model.booking.LocationCoordinate

// State for MapLocationPickerScreen
data class MapPickerState(
    val currentCameraPosition: LatLng,
    val selectedLocation: LatLng?,
    val selectedAddress: String?,
    val isSearching: Boolean,
    val searchInput: String,
    val searchPredictions: List<PlacePrediction>,
    val isLoadingSearch: Boolean,
    val userLocation: LatLng?,
    val showUserLocationButton: Boolean,
    val focusedField: String?
)

// Events for MapLocationPickerScreen
data class MapPickerEvents(
    val onBack: () -> Unit,
    val onCameraMove: (LatLng) -> Unit,
    val onConfirmLocation: (LocationCoordinate, String) -> Unit,
    val onSearchInputChanged: (String) -> Unit,
    val onSearchClear: () -> Unit,
    val onSearchSuggestionSelected: (PlacePrediction) -> Unit,
    val onRecenterMap: () -> Unit,
    val onSearchFocusChanged: (Boolean) -> Unit
)

