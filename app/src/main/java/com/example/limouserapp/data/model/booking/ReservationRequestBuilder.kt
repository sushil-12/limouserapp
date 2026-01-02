package com.example.limouserapp.data.model.booking

import android.util.Log
import com.example.limouserapp.data.model.dashboard.ProfileData
import com.example.limouserapp.ui.utils.DebugTags

/**
 * ReservationRequestBuilder - Helper to build CreateReservationRequest
 * Matches iOS logic for building reservation requests
 */
object ReservationRequestBuilder {
    
    /**
     * Map transfer type from pickupType and dropoffType
     * Matches iOS mapTransferTypeToAPI logic
     */
    fun mapTransferType(pickupType: String, dropoffType: String): String {
        val pickup = pickupType.lowercase().replace("cruise port", "cruise").replace("cruise_port", "cruise").trim()
        val dropoff = dropoffType.lowercase().replace("cruise port", "cruise").replace("cruise_port", "cruise").trim()
        
        return when {
            pickup == "city" && dropoff == "city" -> "city_to_city"
            pickup == "city" && dropoff == "airport" -> "city_to_airport"
            pickup == "airport" && dropoff == "city" -> "airport_to_city"
            pickup == "airport" && dropoff == "airport" -> "airport_to_airport"
            pickup == "city" && dropoff == "cruise" -> "city_to_cruise"
            pickup == "cruise" && dropoff == "city" -> "cruise_to_city"
            pickup == "airport" && dropoff == "cruise" -> "airport_to_cruise"
            pickup == "cruise" && dropoff == "airport" -> "cruise_to_airport"
            pickup == "cruise" && dropoff == "cruise" -> "cruise_to_cruise"
            else -> "city_to_city"
        }
    }
    
    /**
     * Map return transfer type (reverse of outbound)
     * Matches iOS mapReturnTransferTypeToAPI logic
     */
    fun mapReturnTransferType(transferType: String): String {
        return when (transferType) {
            "city_to_city" -> "city_to_city"
            "city_to_airport" -> "airport_to_city"
            "airport_to_city" -> "city_to_airport"
            "airport_to_airport" -> "airport_to_airport"
            "city_to_cruise" -> "cruise_to_city"
            "airport_to_cruise" -> "cruise_to_airport"
            "cruise_to_city" -> "city_to_cruise"
            "cruise_to_airport" -> "airport_to_cruise"
            "cruise_to_cruise" -> "cruise_to_cruise"
            else -> "city_to_city"
        }
    }
    
    /**
     * Map service type to API format
     * Matches iOS mapServiceTypeToAPI logic
     */
    fun mapServiceType(serviceType: String): String {
        return when (serviceType.lowercase()) {
            "one_way" -> "one_way"
            "round_trip" -> "round_trip"
            "charter_tour", "charter/tour" -> "charter_tour"
            else -> "one_way"
        }
    }
    
    /**
     * Construct a basic BookingRateArray from vehicle rate breakdown
     * This is a fallback when booking rates API is not available
     * In production, you should call the booking rates API first
     */
    fun constructRateArrayFromVehicle(
        rateBreakdown: com.example.limouserapp.data.model.booking.RateBreakdown?,
        grandTotal: Double
    ): BookingRateArray {
        val allInclusiveRates = mutableMapOf<String, RateItem>()
        
        // Extract Base_Rate from trip_rate
        val tripRate = rateBreakdown?.rateArray?.allInclusiveRates?.tripRate
        if (tripRate != null) {
            allInclusiveRates["Base_Rate"] = RateItem(
                rateLabel = tripRate.rateLabel ?: "Base Rate",
                baserate = tripRate.baserate ?: 0.0,
                multiple = tripRate.multiple,
                percentage = tripRate.percentage,
                amount = tripRate.amount ?: 0.0,
                type = null,
                flatBaserate = null
            )
        }
        
        // Add other rates as needed
        allInclusiveRates["Stops"] = RateItem(
            rateLabel = "Stops",
            baserate = 0.0,
            multiple = null,
            percentage = 25.0,
            amount = 0.0,
            type = null,
            flatBaserate = null
        )
        
        allInclusiveRates["Wait"] = RateItem(
            rateLabel = "Wait",
            baserate = 0.0,
            multiple = null,
            percentage = null,
            amount = 0.0,
            type = null,
            flatBaserate = null
        )
        
        // Extract ELH_Charges if available (this would come from booking rates API)
        // For now, set to 0 (matches web format - percentage should be null, not 25.0)
        allInclusiveRates["ELH_Charges"] = RateItem(
            rateLabel = "Early AM / Late PM / Holiday Charge",
            baserate = 0.0,
            multiple = null,
            percentage = null, // Should be null to match web format
            amount = 0.0,
            type = null,
            flatBaserate = null
        )
        
        // Construct taxes map (empty for now, should come from booking rates API)
        val taxes = mapOf<String, TaxItem>()
        
        // Construct amenities map (empty for now)
        val amenities = mapOf<String, RateItem>()
        
        // Construct misc map
        val misc = mapOf<String, RateItem>(
            "Extra_Gratuity" to RateItem(
                rateLabel = "Extra Gratuity",
                baserate = 0.0,
                multiple = null,
                percentage = null,
                amount = 0.0,
                type = null,
                flatBaserate = null
            )
        )
        
        return BookingRateArray(
            allInclusiveRates = allInclusiveRates,
            amenities = amenities,
            taxes = taxes,
            misc = misc
        )
    }
    
    /**
     * Format time for API (convert to HH:mm:ss format expected by API)
     * Handles both 12-hour format (e.g., "12:00 PM") and 24-hour format (e.g., "13:39:32")
     */
    fun formatTimeForAPI(time: String): String {
        if (time.isBlank()) {
            return time
        }
        
        return try {
            // Check if input is already in 12-hour format (contains AM/PM)
            if (time.contains("AM", ignoreCase = true) || time.contains("PM", ignoreCase = true)) {
                // Already in 12-hour format, return as-is (API expects "4:26 PM" format)
                time.trim()
            } else {
                // Input is in 24-hour format (HH:mm:ss or HH:mm), convert to 12-hour format with AM/PM
                // API expects format like "4:26 PM" (matches iOS formatTimeForAPI)
                val inputFormatter = if (time.contains(":")) {
                    if (time.split(":").size == 3) {
                        // HH:mm:ss format
                        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                    } else {
                        // HH:mm format
                        java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    }
                } else {
                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                }
                val outputFormatter = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                val date = inputFormatter.parse(time.trim())
                if (date != null) {
                    outputFormatter.format(date)
                } else {
                    Log.e(DebugTags.BookingProcess, "Failed to parse 24-hour time format: $time")
                    time
                }
            }
        } catch (e: Exception) {
            Log.e(DebugTags.BookingProcess, "Error formatting time for API: $time", e)
            time
        }
    }
}

