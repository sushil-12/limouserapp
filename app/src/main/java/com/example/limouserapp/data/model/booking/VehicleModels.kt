package com.example.limouserapp.data.model.booking

import com.google.gson.annotations.SerializedName

data class VehicleListingRequest(
    @SerializedName("service_type") val serviceType: String,
    @SerializedName("booking_hour") val bookingHour: String,
    @SerializedName("pickup_type") val pickupType: String,
    @SerializedName("dropoff_type") val dropoffType: String,
    @SerializedName("pickup_date") val pickupDate: String,
    @SerializedName("pickup_time") val pickupTime: String,
    @SerializedName("pickup_airport") val pickupAirport: Int? = null,
    @SerializedName("pickup_airport_name") val pickupAirportName: String? = null,
    @SerializedName("pickup_airport_lat") val pickupAirportLat: Double? = null,
    @SerializedName("pickup_airport_long") val pickupAirportLong: Double? = null,
    @SerializedName("pickup_address") val pickupAddress: String? = null,
    @SerializedName("pickup_address_lat") val pickupAddressLat: Double? = null,
    @SerializedName("pickup_address_long") val pickupAddressLong: Double? = null,
    @SerializedName("dropoff_airport") val dropoffAirport: Int? = null,
    @SerializedName("dropoff_airport_name") val dropoffAirportName: String? = null,
    @SerializedName("dropoff_airport_lat") val dropoffAirportLat: Double? = null,
    @SerializedName("dropoff_airport_long") val dropoffAirportLong: Double? = null,
    @SerializedName("dropoff_address") val dropoffAddress: String? = null,
    @SerializedName("dropoff_address_lat") val dropoffAddressLat: Double? = null,
    @SerializedName("dropoff_address_long") val dropoffAddressLong: Double? = null,
    @SerializedName("return_pickup_date") val returnPickupDate: String = "",
    @SerializedName("return_pickup_time") val returnPickupTime: String = "",
    @SerializedName("return_pickup_airport") val returnPickupAirport: Int? = null,
    @SerializedName("return_pickup_airport_name") val returnPickupAirportName: String? = null,
    @SerializedName("return_pickup_airport_lat") val returnPickupAirportLat: Double? = null,
    @SerializedName("return_pickup_airport_long") val returnPickupAirportLong: Double? = null,
    @SerializedName("return_pickup_address") val returnPickupAddress: String? = null,
    @SerializedName("return_pickup_address_lat") val returnPickupAddressLat: Double? = null,
    @SerializedName("return_pickup_address_long") val returnPickupAddressLong: Double? = null,
    @SerializedName("return_dropoff_airport") val returnDropoffAirport: Int? = null,
    @SerializedName("return_dropoff_airport_name") val returnDropoffAirportName: String? = null,
    @SerializedName("return_dropoff_airport_lat") val returnDropoffAirportLat: Double? = null,
    @SerializedName("return_dropoff_airport_long") val returnDropoffAirportLong: Double? = null,
    @SerializedName("return_dropoff_address") val returnDropoffAddress: String? = null,
    @SerializedName("return_dropoff_address_lat") val returnDropoffAddressLat: Double? = null,
    @SerializedName("return_dropoff_address_long") val returnDropoffAddressLong: Double? = null,
    @SerializedName("no_of_passenger") val noOfPassenger: Int,
    @SerializedName("no_of_luggage") val noOfLuggage: Int,
    @SerializedName("location_info") val locationInfo: List<LocationInfo> = emptyList(),
    @SerializedName("other_details") val otherDetails: OtherDetails = OtherDetails(),
    @SerializedName("filters") val filters: FiltersRequest = FiltersRequest(),
    @SerializedName("user_id") val userId: Int = 0
)

data class LocationInfo(
    @SerializedName("type") val type: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("lat") val lat: Double? = null,
    @SerializedName("long") val long: Double? = null,
    @SerializedName("distance") val distance: DistanceInfo? = null,
    @SerializedName("duration") val duration: DurationInfo? = null
)

data class DistanceInfo(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int
)

data class DurationInfo(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int
)

data class OtherDetails(
    @SerializedName("notes") val notes: String? = null,
    @SerializedName("pickup_airport_name") val pickupAirportName: String? = null,
    @SerializedName("dropoff_airport_name") val dropoffAirportName: String? = null,
    @SerializedName("return_pickup_airport_name") val returnPickupAirportName: String? = null,
    @SerializedName("return_dropoff_airport_name") val returnDropoffAirportName: String? = null
)

data class FiltersRequest(
    @SerializedName("vehicle-type") val vehicleType: List<Int>? = null,
    @SerializedName("driver-dresses") val driverDresses: List<Int>? = null,
    @SerializedName("driver-languages") val driverLanguages: List<Int>? = null,
    @SerializedName("driver-gender") val driverGender: List<String>? = null,
    @SerializedName("amenities") val amenities: List<Int>? = null,
    @SerializedName("make") val make: List<Int>? = null,
    @SerializedName("model") val model: List<Int>? = null,
    @SerializedName("years") val years: List<Int>? = null,
    @SerializedName("colors") val colors: List<Int>? = null,
    @SerializedName("interiors") val interiors: List<Int>? = null,
    @SerializedName("special-amenities") val specialAmenities: List<Int>? = null,
    @SerializedName("vehicle-service-area") val vehicleServiceArea: List<String>? = null,
    @SerializedName("affiliate-preferences") val affiliatePreferences: List<String>? = null
)

/**
 * Master Vehicle Listing Response
 * Matches iOS VehicleQuoteResponseActual
 * data is a direct array, not wrapped in an object
 */
data class VehicleMasterListingResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: List<VehicleActual>?,
    @SerializedName("message") val message: String? = null,
    @SerializedName("currency") val currency: CurrencyInfo? = null
)

/**
 * Vehicle Listing Response (for vehicle listing API)
 */
data class VehicleListingArrayResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("data") val data: List<Vehicle>?
)

/**
 * VehicleActual - matches iOS VehicleActual structure
 * Used for master vehicle listing API response
 */
data class VehicleActual(
    @SerializedName("id") val id: Int,
    @SerializedName("vehicle_id") val vehicleId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("passenger") val passenger: Int,
    @SerializedName("luggage") val luggage: Int,
    @SerializedName("cat_img") val catImg: String?,
    @SerializedName("rate_breakdown_charter_tour") val rateBreakdownCharterTour: RateBreakdown? = null,
    @SerializedName("rate_breakdown_one_way") val rateBreakdownOneWay: RateBreakdown? = null,
    @SerializedName("rate_breakdown_round_trip") val rateBreakdownRoundTrip: RateBreakdown? = null
) {
    /**
     * Get price based on service type (matches iOS logic)
     */
    fun getPrice(serviceType: String): Double {
        val breakdown = when (serviceType.lowercase()) {
            "one_way" -> rateBreakdownOneWay
            "round_trip" -> rateBreakdownRoundTrip
            "charter_tour" -> rateBreakdownCharterTour
            else -> null
        }
        return breakdown?.grandTotal ?: breakdown?.total ?: breakdown?.subTotal ?: 0.0
    }
    
    /**
     * Convert to Vehicle for UI display
     */
    fun toVehicle(serviceType: String): Vehicle {
        return Vehicle(
            id = id,
            name = name,
            image = catImg,
            capacity = passenger,
            luggage = luggage,
            price = getPrice(serviceType)
        )
    }
}

/**
 * Rate Breakdown - matches iOS RateBreakdown
 */
data class RateBreakdown(
    @SerializedName("rateArray") val rateArray: RateArray? = null,
    @SerializedName("sub_total") val subTotal: Double? = null,
    @SerializedName("grand_total") val grandTotal: Double? = null,
    @SerializedName("total") val total: Double? = null
)

data class RateArray(
    @SerializedName("all_inclusive_rates") val allInclusiveRates: AllInclusiveRates? = null
)

data class AllInclusiveRates(
    @SerializedName("trip_rate") val tripRate: TripRate? = null,
    @SerializedName("gratuity") val gratuity: Gratuity? = null,
    @SerializedName("trip_tax") val tripTax: TripTax? = null
)

data class TripRate(
    @SerializedName("rate_label") val rateLabel: String? = null,
    @SerializedName("baserate") val baserate: Double? = null,
    @SerializedName("multiple") val multiple: Double? = null,
    @SerializedName("percentage") val percentage: Double? = null,
    @SerializedName("amount") val amount: Double? = null
)

data class Gratuity(
    @SerializedName("rate_label") val rateLabel: String? = null,
    @SerializedName("baserate") val baserate: Double? = null,
    @SerializedName("multiple") val multiple: Double? = null,
    @SerializedName("percentage") val percentage: Double? = null,
    @SerializedName("amount") val amount: Double? = null
)

data class TripTax(
    @SerializedName("rate_label") val rateLabel: String? = null,
    @SerializedName("baserate") val baserate: Double? = null,
    @SerializedName("multiple") val multiple: Double? = null,
    @SerializedName("percentage") val percentage: Double? = null,
    @SerializedName("amount") val amount: Double? = null
)

data class CurrencyInfo(
    @SerializedName("countryName") val countryName: String? = null,
    @SerializedName("currency") val currency: String,
    @SerializedName("currencyCountry") val currencyCountry: String,
    @SerializedName("symbol") val symbol: String,
    @SerializedName("dateFormat") val dateFormat: String? = null
)

/**
 * Vehicle - model for UI display
 * Extended to match iOS VehicleListingItem structure for dialogs
 */
data class Vehicle(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("image") val image: String?,
    @SerializedName("vehicle_images") val vehicleImages: List<String>? = null,
    @SerializedName("capacity") val capacity: Int?,
    @SerializedName("passenger") val passenger: Int? = null,
    @SerializedName("luggage") val luggage: Int?,
    @SerializedName("price") val price: Double?,
    // Affiliate information - matches iOS VehicleListingItem
    @SerializedName("affiliate_id") val affiliateId: Int? = null,
    // Rate breakdowns for dialogs
    @SerializedName("rate_breakdown_one_way") val rateBreakdownOneWay: RateBreakdown? = null,
    @SerializedName("rate_breakdown_round_trip") val rateBreakdownRoundTrip: RateBreakdown? = null,
    @SerializedName("rate_breakdown_charter_tour") val rateBreakdownCharterTour: RateBreakdown? = null,
    // Driver information
    @SerializedName("driverInformation") val driverInformation: DriverInformation? = null,
    // Amenities
    @SerializedName("amenities") val amenities: List<Amenity>? = null,
    // Vehicle details
    @SerializedName("vehicle_details") val vehicleDetails: VehicleDetails? = null,
    // Master vehicle flag - matches iOS isMasterVehicle property
    @SerializedName("is_master_vehicle") val isMasterVehicle: Boolean? = null
) {
    /**
     * Get price based on service type
     */
    fun getPrice(serviceType: String): Double? {
        val breakdown = when (serviceType.lowercase()) {
            "one_way" -> rateBreakdownOneWay
            "round_trip" -> rateBreakdownRoundTrip
            "charter_tour" -> rateBreakdownCharterTour
            else -> null
        }
        return breakdown?.grandTotal ?: breakdown?.total ?: breakdown?.subTotal ?: price
    }
    
    /**
     * Get capacity (prefer passenger, fallback to capacity)
     */
    fun getCapacity(): Int {
        return passenger ?: capacity ?: 0
    }
    
    /**
     * Get rate breakdown based on service type
     */
    fun getRateBreakdown(serviceType: String): RateBreakdown? {
        return when (serviceType.lowercase()) {
            "one_way" -> rateBreakdownOneWay
            "round_trip" -> rateBreakdownRoundTrip
            "charter_tour" -> rateBreakdownCharterTour
            else -> null
        }
    }
}

/**
 * Driver Information - matches iOS DriverInformation
 */
data class DriverInformation(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("phone") val phone: String? = null,
    @SerializedName("cell_isd") val cellIsd: String? = null,
    @SerializedName("cell_number") val cellNumber: String? = null,
    @SerializedName("star_rating") val starRating: String? = null,
    @SerializedName("background") val background: String? = null,
    @SerializedName("dress") val dress: String? = null,
    @SerializedName("languages") val languages: String? = null,
    @SerializedName("experience") val experience: String? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("insurance_limit") val insuranceLimit: String? = null
)

/**
 * Amenity - matches iOS Amenity
 */
data class Amenity(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("chargeable") val chargeable: String
)

/**
 * Vehicle Details - matches iOS VehicleDetails
 */
data class VehicleDetails(
    @SerializedName("make") val make: String? = null,
    @SerializedName("model") val model: String? = null,
    @SerializedName("year") val year: String? = null
)



