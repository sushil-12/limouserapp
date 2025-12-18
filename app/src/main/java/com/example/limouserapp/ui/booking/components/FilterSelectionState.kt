package com.example.limouserapp.ui.booking.components

/**
 * State holder for filter selections
 * Matches iOS FilterSelectionState
 */
data class FilterSelectionState(
    val selectedVehicleTypes: Set<Int> = emptySet(),
    val selectedAmenities: Set<Int> = emptySet(),
    val selectedSpecialAmenities: Set<Int> = emptySet(),
    val selectedInteriors: Set<Int> = emptySet(),
    val selectedMakes: Set<Int> = emptySet(),
    val selectedModels: Set<Int> = emptySet(),
    val selectedYears: Set<Int> = emptySet(),
    val selectedColors: Set<Int> = emptySet(),
    val selectedDriverLanguages: Set<Int> = emptySet(),
    val selectedDriverDresses: Set<Int> = emptySet(),
    val selectedDriverGenders: Set<String> = emptySet(),
    val selectedDriverBackgrounds: Set<Int> = emptySet(),
    val selectedVehicleServiceAreas: Set<String> = emptySet(),
    val selectedAffiliatePreferences: Set<String> = emptySet()
) {
    fun clearAll(): FilterSelectionState {
        return FilterSelectionState()
    }
    
    fun hasAnySelection(): Boolean {
        return selectedVehicleTypes.isNotEmpty() ||
                selectedAmenities.isNotEmpty() ||
                selectedSpecialAmenities.isNotEmpty() ||
                selectedInteriors.isNotEmpty() ||
                selectedMakes.isNotEmpty() ||
                selectedModels.isNotEmpty() ||
                selectedYears.isNotEmpty() ||
                selectedColors.isNotEmpty() ||
                selectedDriverLanguages.isNotEmpty() ||
                selectedDriverDresses.isNotEmpty() ||
                selectedDriverGenders.isNotEmpty() ||
                selectedDriverBackgrounds.isNotEmpty() ||
                selectedVehicleServiceAreas.isNotEmpty() ||
                selectedAffiliatePreferences.isNotEmpty()
    }
    
    /**
     * Convert to FiltersRequest for API
     */
    fun toFiltersRequest(): com.example.limouserapp.data.model.booking.FiltersRequest {
        return com.example.limouserapp.data.model.booking.FiltersRequest(
            vehicleType = selectedVehicleTypes.takeIf { it.isNotEmpty() }?.toList(),
            driverDresses = selectedDriverDresses.takeIf { it.isNotEmpty() }?.toList(),
            driverLanguages = selectedDriverLanguages.takeIf { it.isNotEmpty() }?.toList(),
            driverGender = selectedDriverGenders.takeIf { it.isNotEmpty() }?.toList(),
            amenities = selectedAmenities.takeIf { it.isNotEmpty() }?.toList(),
            make = selectedMakes.takeIf { it.isNotEmpty() }?.toList(),
            model = selectedModels.takeIf { it.isNotEmpty() }?.toList(),
            years = selectedYears.takeIf { it.isNotEmpty() }?.toList(),
            colors = selectedColors.takeIf { it.isNotEmpty() }?.toList(),
            interiors = selectedInteriors.takeIf { it.isNotEmpty() }?.toList(),
            specialAmenities = selectedSpecialAmenities.takeIf { it.isNotEmpty() }?.toList(),
            vehicleServiceArea = selectedVehicleServiceAreas.takeIf { it.isNotEmpty() }?.toList(),
            affiliatePreferences = selectedAffiliatePreferences.takeIf { it.isNotEmpty() }?.toList()
        )
    }
}

