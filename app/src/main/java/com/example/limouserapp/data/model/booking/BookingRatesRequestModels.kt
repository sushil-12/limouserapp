package com.example.limouserapp.data.model.booking

import com.google.gson.annotations.SerializedName

/**
 * Booking Rates Request - matches iOS BookingRatesRequest structure
 * Used to fetch detailed rate breakdown from booking-rates-vehicle API
 * Note: ExtraStopRequest is already defined in ReservationModels.kt
 */
data class BookingRatesRequest(
    @SerializedName("vehicle_id") val vehicleId: Int,
    @SerializedName("transfer_type") val transferType: String,
    @SerializedName("service_type") val serviceType: String,
    @SerializedName("number_of_vehicles") val numberOfVehicles: Int,
    @SerializedName("distance") val distance: Int,
    @SerializedName("return_distance") val returnDistance: Int,
    @SerializedName("no_of_hours") val noOfHours: String,
    @SerializedName("is_master_vehicle") val isMasterVehicle: Boolean,
    @SerializedName("extra_stops") val extraStops: List<ExtraStopRequest> = emptyList(),
    @SerializedName("return_extra_stops") val returnExtraStops: List<ExtraStopRequest> = emptyList(),
    @SerializedName("pickup_time") val pickupTime: String,
    @SerializedName("return_pickup_time") val returnPickupTime: String,
    @SerializedName("return_vehicle_id") val returnVehicleId: Int,
    @SerializedName("return_affiliate_type") val returnAffiliateType: String
)

/**
 * Booking Rates Response - matches iOS BookingRatesResponse structure
 */
data class BookingRatesResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: BookingRatesData,
    @SerializedName("message") val message: String,
    @SerializedName("currency") val currency: BookingRatesCurrency? = null
)

/**
 * Booking Rates Data - matches iOS BookingRatesData structure
 */
data class BookingRatesData(
    @SerializedName("sub_total") val subTotal: Double,
    @SerializedName("grand_total") val grandTotal: Double,
    @SerializedName("min_rate_involved") val minRateInvolved: Boolean? = null,
    @SerializedName("rateArray") val rateArray: BookingRateArray,
    @SerializedName("retrunRateArray") val retrunRateArray: BookingRateArray? = null
)

/**
 * Booking Rates Currency - matches iOS BookingRatesCurrency structure
 */
data class BookingRatesCurrency(
    @SerializedName("countryName") val countryName: String,
    @SerializedName("currency") val currency: String,
    @SerializedName("currencyCountry") val currencyCountry: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("dateFormat") val dateFormat: String
)

