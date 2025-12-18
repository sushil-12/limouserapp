package com.example.limouserapp.data.model.booking

import com.google.gson.annotations.SerializedName

/**
 * Filter Response from API
 */
data class FilterResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: FilterData,
    @SerializedName("message") val message: String?,
    @SerializedName("currency") val currency: CurrencyInfo?
)

data class FilterData(
    @SerializedName("affiliate-preferences") val affiliatePreferences: List<AffiliatePreference> = emptyList(),
    @SerializedName("vehicle-type") val vehicleType: List<VehicleTypeFilter> = emptyList(),
    @SerializedName("amenities") val amenities: List<AmenityFilter> = emptyList(),
    @SerializedName("special-amenities") val specialAmenities: List<SpecialAmenityFilter> = emptyList(),
    @SerializedName("interiors") val interiors: List<InteriorFilter> = emptyList(),
    @SerializedName("make") val make: List<MakeFilter> = emptyList(),
    @SerializedName("model") val model: List<ModelFilter> = emptyList(),
    @SerializedName("years") val years: List<YearFilter> = emptyList(),
    @SerializedName("colors") val colors: List<ColorFilter> = emptyList(),
    @SerializedName("driver-languages") val driverLanguages: List<DriverLanguageFilter> = emptyList(),
    @SerializedName("driver-dresses") val driverDresses: List<DriverDressFilter> = emptyList(),
    @SerializedName("driver-gender") val driverGender: List<DriverGenderFilter> = emptyList(),
    @SerializedName("vehicle-service-area") val vehicleServiceArea: List<VehicleServiceAreaFilter> = emptyList()
)

// CurrencyInfo is defined in VehicleModels.kt to avoid duplication

/**
 * Filter Item Models
 */
data class AffiliatePreference(
    @SerializedName("slug") val slug: String,
    @SerializedName("name") val name: String
)

data class VehicleTypeFilter(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String,
    @SerializedName("sort_order") val sortOrder: Int? = null
)

data class AmenityFilter(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("chargeable") val chargeable: String,
    @SerializedName("category") val category: String
)

data class SpecialAmenityFilter(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("chargeable") val chargeable: String? = null,
    @SerializedName("category") val category: String? = null
)

data class InteriorFilter(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class MakeFilter(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class ModelFilter(
    @SerializedName("id") val id: Int,
    @SerializedName("make_id") val makeId: Int,
    @SerializedName("name") val name: String
)

data class YearFilter(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class ColorFilter(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class DriverLanguageFilter(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class DriverDressFilter(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class DriverGenderFilter(
    @SerializedName("slug") val slug: String,
    @SerializedName("name") val name: String
)

data class VehicleServiceAreaFilter(
    @SerializedName("slug") val slug: String,
    @SerializedName("name") val name: String
)

