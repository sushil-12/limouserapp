package com.example.limouserapp.data.model.booking

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/**
 * AirportOption - matches iOS AirportOption structure exactly
 */
data class AirportOption(
    @SerializedName("id") val id: Int,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("city") val city: String,
    @SerializedName("country") val country: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("long") val long: Double,
    @SerializedName("formatted_name") val formattedName: String
)

/**
 * AirlineOption - matches iOS AirlineOption structure exactly
 */
data class AirlineOption(
    @SerializedName("id") val id: Int,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("country") val country: String,
    @SerializedName("formatted_name") val formattedName: String
)

/**
 * Create Reservation Request - matches iOS CreateReservationRequest structure exactly
 */
data class CreateReservationRequest(
    @SerializedName("service_type") val serviceType: String,
    @SerializedName("transfer_type") val transferType: String,
    @SerializedName("return_transfer_type") val returnTransferType: String,
    @SerializedName("number_of_hours") val numberOfHours: Int,
    @SerializedName("account_type") val accountType: String,
    @SerializedName("change_individual_data") val changeIndividualData: Boolean,
    @SerializedName("passenger_name") val passengerName: String,
    @SerializedName("passenger_email") val passengerEmail: String,
    @SerializedName("passenger_cell") val passengerCell: String,
    @SerializedName("passenger_cell_isd") val passengerCellIsd: String,
    @SerializedName("passenger_cell_country") val passengerCellCountry: String,
    @SerializedName("total_passengers") val totalPassengers: Int,
    @SerializedName("luggage_count") val luggageCount: Int,
    @SerializedName("booking_instructions") val bookingInstructions: String,
    @SerializedName("return_booking_instructions") val returnBookingInstructions: String,
    @SerializedName("affiliate_type") val affiliateType: String,
    @SerializedName("affiliate_id") val affiliateId: String,
    @SerializedName("lose_affiliate_name") val loseAffiliateName: String,
    @SerializedName("lose_affiliate_phone") val loseAffiliatePhone: String,
    @SerializedName("lose_affiliate_phone_isd") val loseAffiliatePhoneIsd: String,
    @SerializedName("lose_affiliate_phone_country") val loseAffiliatePhoneCountry: String,
    @SerializedName("lose_affiliate_email") val loseAffiliateEmail: String,
    @SerializedName("vehicle_type") val vehicleType: String,
    @SerializedName("vehicle_type_name") val vehicleTypeName: String,
    @SerializedName("vehicle_id") val vehicleId: Int,
    @SerializedName("vehicle_make") val vehicleMake: String,
    @SerializedName("vehicle_make_name") val vehicleMakeName: String,
    @SerializedName("vehicle_model") val vehicleModel: String,
    @SerializedName("vehicle_model_name") val vehicleModelName: String,
    @SerializedName("vehicle_year") val vehicleYear: String,
    @SerializedName("vehicle_year_name") val vehicleYearName: String,
    @SerializedName("vehicle_color") val vehicleColor: String,
    @SerializedName("vehicle_color_name") val vehicleColorName: String,
    @SerializedName("vehicle_license_plate") val vehicleLicensePlate: String,
    @SerializedName("vehicle_seats") val vehicleSeats: String,
    @SerializedName("driver_id") val driverId: String,
    @SerializedName("driver_name") val driverName: String,
    @SerializedName("driver_gender") val driverGender: String,
    @SerializedName("driver_cell") val driverCell: String,
    @SerializedName("driver_cell_isd") val driverCellIsd: String,
    @SerializedName("driver_cell_country") val driverCellCountry: String,
    @SerializedName("driver_email") val driverEmail: String,
    @SerializedName("driver_phone_type") val driverPhoneType: String,
    @SerializedName("driver_image_id") val driverImageId: String,
    @SerializedName("vehicle_image_id") val vehicleImageId: String,
    @SerializedName("meet_greet_choices") val meetGreetChoices: Int,
    @SerializedName("meet_greet_choices_name") val meetGreetChoicesName: String,
    @SerializedName("number_of_vehicles") val numberOfVehicles: Int,
    @SerializedName("pickup_date") val pickupDate: String,
    @SerializedName("pickup_time") val pickupTime: String,
    @SerializedName("extra_stops") val extraStops: List<ExtraStopRequest> = emptyList(),
    @SerializedName("pickup") val pickup: String,
    @SerializedName("pickup_latitude") val pickupLatitude: String,
    @SerializedName("pickup_longitude") val pickupLongitude: String,
    @JsonAdapter(AirportOptionTypeAdapter::class)
    @SerializedName("pickup_airport_option") val pickupAirportOption: AirportOption? = null,
    @SerializedName("pickup_airport") val pickupAirport: String = "", // String to match web (empty string when not airport)
    @SerializedName("pickup_airport_name") val pickupAirportName: String = "",
    @SerializedName("pickup_airport_latitude") val pickupAirportLatitude: String = "", // String to match web (empty string when not airport)
    @SerializedName("pickup_airport_longitude") val pickupAirportLongitude: String = "", // String to match web (empty string when not airport)
    @JsonAdapter(AirlineOptionTypeAdapter::class)
    @SerializedName("pickup_airline_option") val pickupAirlineOption: AirlineOption? = null,
    @SerializedName("pickup_airline") val pickupAirline: String = "", // String to match web
    @SerializedName("pickup_airline_name") val pickupAirlineName: String = "",
    @SerializedName("pickup_flight") val pickupFlight: String = "",
    @SerializedName("origin_airport_city") val originAirportCity: String = "",
    @SerializedName("cruise_port") val cruisePort: String = "",
    @SerializedName("cruise_name") val cruiseName: String = "",
    @SerializedName("cruise_time") val cruiseTime: String = "",
    @SerializedName("dropoff") val dropoff: String = "",
    @SerializedName("dropoff_latitude") val dropoffLatitude: String = "",
    @SerializedName("dropoff_longitude") val dropoffLongitude: String = "",
    @JsonAdapter(AirportOptionTypeAdapter::class)
    @SerializedName("dropoff_airport_option") val dropoffAirportOption: AirportOption? = null,
    @SerializedName("dropoff_airport") val dropoffAirport: String = "", // String to match web (empty string when not airport)
    @SerializedName("dropoff_airport_name") val dropoffAirportName: String = "",
    @SerializedName("dropoff_airport_latitude") val dropoffAirportLatitude: String = "", // String to match web (empty string when not airport)
    @SerializedName("dropoff_airport_longitude") val dropoffAirportLongitude: String = "", // String to match web (empty string when not airport)
    @JsonAdapter(AirlineOptionTypeAdapter::class)
    @SerializedName("dropoff_airline_option") val dropoffAirlineOption: AirlineOption? = null,
    @SerializedName("dropoff_airline") val dropoffAirline: String = "", // String to match web
    @SerializedName("dropoff_airline_name") val dropoffAirlineName: String = "",
    @SerializedName("dropoff_flight") val dropoffFlight: String = "",
    @SerializedName("return_meet_greet_choices") val returnMeetGreetChoices: Int = 1,
    @SerializedName("return_meet_greet_choices_name") val returnMeetGreetChoicesName: String = "",
    @SerializedName("return_pickup_date") val returnPickupDate: String = "",
    @SerializedName("return_pickup_time") val returnPickupTime: String = "",
    @SerializedName("return_extra_stops") val returnExtraStops: List<ExtraStopRequest> = emptyList(),
    @SerializedName("return_pickup") val returnPickup: String = "",
    @SerializedName("return_pickup_latitude") val returnPickupLatitude: String = "", // String to match web
    @SerializedName("return_pickup_longitude") val returnPickupLongitude: String = "", // String to match web
    @JsonAdapter(AirportOptionTypeAdapter::class)
    @SerializedName("return_pickup_airport_option") val returnPickupAirportOption: AirportOption? = null,
    @SerializedName("return_pickup_airport") val returnPickupAirport: String = "", // String to match web (empty string when not airport)
    @SerializedName("return_pickup_airport_name") val returnPickupAirportName: String = "",
    @SerializedName("return_pickup_airport_latitude") val returnPickupAirportLatitude: String = "", // String to match web (empty string when not airport)
    @SerializedName("return_pickup_airport_longitude") val returnPickupAirportLongitude: String = "", // String to match web (empty string when not airport)
    @JsonAdapter(AirlineOptionTypeAdapter::class)
    @SerializedName("return_pickup_airline_option") val returnPickupAirlineOption: AirlineOption? = null,
    @SerializedName("return_pickup_airline") val returnPickupAirline: String = "", // String to match web
    @SerializedName("return_pickup_airline_name") val returnPickupAirlineName: String = "",
    @SerializedName("return_pickup_flight") val returnPickupFlight: String = "",
    @SerializedName("return_cruise_port") val returnCruisePort: String = "",
    @SerializedName("return_cruise_name") val returnCruiseName: String = "",
    @SerializedName("return_cruise_time") val returnCruiseTime: String = "",
    @SerializedName("return_dropoff") val returnDropoff: String = "",
    @SerializedName("return_dropoff_latitude") val returnDropoffLatitude: String = "", // String to match web
    @SerializedName("return_dropoff_longitude") val returnDropoffLongitude: String = "", // String to match web
    @JsonAdapter(AirportOptionTypeAdapter::class)
    @SerializedName("return_dropoff_airport_option") val returnDropoffAirportOption: AirportOption? = null,
    @SerializedName("return_dropoff_airport") val returnDropoffAirport: String = "", // String to match web (empty string when not airport)
    @SerializedName("return_dropoff_airport_name") val returnDropoffAirportName: String = "",
    @SerializedName("return_dropoff_airport_latitude") val returnDropoffAirportLatitude: String = "", // String to match web (empty string when not airport)
    @SerializedName("return_dropoff_airport_longitude") val returnDropoffAirportLongitude: String = "", // String to match web (empty string when not airport)
    @JsonAdapter(AirlineOptionTypeAdapter::class)
    @SerializedName("return_dropoff_airline_option") val returnDropoffAirlineOption: AirlineOption? = null,
    @SerializedName("return_dropoff_airline") val returnDropoffAirline: String = "", // String to match web
    @SerializedName("return_dropoff_airline_name") val returnDropoffAirlineName: String = "",
    @SerializedName("return_dropoff_flight") val returnDropoffFlight: String = "",
    @SerializedName("driver_languages") val driverLanguages: List<Int> = emptyList(),
    @SerializedName("driver_dresses") val driverDresses: List<String> = emptyList(),
    @SerializedName("amenities") val amenities: List<String> = emptyList(),
    @SerializedName("chargedAmenities") val chargedAmenities: List<String> = emptyList(),
    @SerializedName("journeyDistance") val journeyDistance: Int = 0,
    @SerializedName("journeyTime") val journeyTime: Int = 0,
    @SerializedName("returnJourneyDistance") val returnJourneyDistance: String = "", // String to match web (empty string when not round trip)
    @SerializedName("returnJourneyTime") val returnJourneyTime: String = "", // String to match web (empty string when not round trip)
    @SerializedName("reservation_id") val reservationId: String = "",
    @SerializedName("updateType") val updateType: String = "",
    @SerializedName("departing_airport_city") val departingAirportCity: String = "",
    @SerializedName("currency") val currency: String = "USD",
    @SerializedName("is_master_vehicle") val isMasterVehicle: Boolean = false,
    @SerializedName("proceed") val proceed: Boolean = true,
    @SerializedName("rateArray") val rateArray: BookingRateArray,
    @SerializedName("returnRateArray") val returnRateArray: BookingRateArray? = null,
    @SerializedName("grand_total") val grandTotal: Double,
    @SerializedName("sub_total") val subTotal: Double,
    @SerializedName("return_sub_total") val returnSubTotal: Double? = null,
    @SerializedName("return_grand_total") val returnGrandTotal: Double? = null,
    @SerializedName("min_rate_involved") val minRateInvolved: Boolean = false,
    @SerializedName("shares_array") val sharesArray: SharesArray,
    @SerializedName("return_shares_array") val returnSharesArray: SharesArray? = null,
    @SerializedName("return_affiliate_type") val returnAffiliateType: String = "",
    @SerializedName("return_distance") val returnDistance: Int = 0,
    @SerializedName("return_vehicle_id") val returnVehicleId: Int = 0,
    @SerializedName("no_of_hours") val noOfHours: String = "",
    @SerializedName("fbo_address") val fboAddress: String = "",
    @SerializedName("fbo_name") val fboName: String = "",
    @SerializedName("return_fbo_address") val returnFboAddress: String = "",
    @SerializedName("return_fbo_name") val returnFboName: String = "",
    @SerializedName("return_affiliate_id") val returnAffiliateId: String = "", // Added to match web/iOS payload
    @SerializedName("created_by_role") val createdByRole: String? = null // Added to match web/iOS payload
)

/**
 * Extra Stop Request - matches iOS ExtraStopRequest
 */
data class ExtraStopRequest(
    @SerializedName("address") val address: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("rate") val rate: String = "out_town",
    @SerializedName("booking_instructions") val bookingInstructions: String = ""
)

/**
 * Reservation data from API response
 */
data class ReservationData(
    @SerializedName("reservation_id") val reservationId: Int?,
    @SerializedName("order_id") val orderId: Int?,
    @SerializedName("return_reservation_id") val returnReservationId: Int? = null
)

data class CreateReservationResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: ReservationData?,
    @SerializedName("bookingId") val bookingId: Int? // Keep for backward compatibility
)

/**
 * Edit Reservation Request - matches iOS EditReservationRequest structure exactly
 * Note: Many fields are String type to match web API expectations (unlike CreateReservationRequest)
 */
data class EditReservationRequest(
    @SerializedName("service_type") val serviceType: String,
    @SerializedName("transfer_type") val transferType: String,
    @SerializedName("return_transfer_type") val returnTransferType: String,
    @SerializedName("number_of_hours") val numberOfHours: String, // String to match web
    @SerializedName("account_type") val accountType: String,
    @SerializedName("change_individual_data") val changeIndividualData: String, // String to match web
    @SerializedName("passenger_name") val passengerName: String,
    @SerializedName("passenger_email") val passengerEmail: String,
    @SerializedName("passenger_cell") val passengerCell: String,
    @SerializedName("passenger_cell_isd") val passengerCellIsd: String,
    @SerializedName("passenger_cell_country") val passengerCellCountry: String,
    @SerializedName("total_passengers") val totalPassengers: String, // String to match web
    @SerializedName("luggage_count") val luggageCount: String, // String to match web
    @SerializedName("booking_instructions") val bookingInstructions: String,
    @SerializedName("return_booking_instructions") val returnBookingInstructions: String,
    @SerializedName("affiliate_type") val affiliateType: String,
    @SerializedName("affiliate_id") val affiliateId: String,
    @SerializedName("lose_affiliate_name") val loseAffiliateName: String,
    @SerializedName("lose_affiliate_phone") val loseAffiliatePhone: String,
    @SerializedName("lose_affiliate_phone_isd") val loseAffiliatePhoneIsd: String,
    @SerializedName("lose_affiliate_phone_country") val loseAffiliatePhoneCountry: String,
    @SerializedName("lose_affiliate_email") val loseAffiliateEmail: String,
    @SerializedName("vehicle_type") val vehicleType: String,
    @SerializedName("vehicle_type_name") val vehicleTypeName: String,
    @SerializedName("vehicle_id") val vehicleId: String, // String to match web
    @SerializedName("vehicle_make") val vehicleMake: String,
    @SerializedName("vehicle_make_name") val vehicleMakeName: String,
    @SerializedName("vehicle_model") val vehicleModel: String,
    @SerializedName("vehicle_model_name") val vehicleModelName: String,
    @SerializedName("vehicle_year") val vehicleYear: String,
    @SerializedName("vehicle_year_name") val vehicleYearName: String,
    @SerializedName("vehicle_color") val vehicleColor: String,
    @SerializedName("vehicle_color_name") val vehicleColorName: String,
    @SerializedName("vehicle_license_plate") val vehicleLicensePlate: String,
    @SerializedName("vehicle_seats") val vehicleSeats: String,
    @SerializedName("driver_id") val driverId: String,
    @SerializedName("driver_name") val driverName: String,
    @SerializedName("driver_gender") val driverGender: String,
    @SerializedName("driver_cell") val driverCell: String,
    @SerializedName("driver_cell_isd") val driverCellIsd: String,
    @SerializedName("driver_cell_country") val driverCellCountry: String,
    @SerializedName("driver_email") val driverEmail: String,
    @SerializedName("driver_phone_type") val driverPhoneType: String,
    @SerializedName("driver_image_id") val driverImageId: String,
    @SerializedName("vehicle_image_id") val vehicleImageId: String,
    @SerializedName("meet_greet_choices") val meetGreetChoices: String, // String to match web
    @SerializedName("meet_greet_choices_name") val meetGreetChoicesName: String,
    @SerializedName("number_of_vehicles") val numberOfVehicles: String, // String to match web
    @SerializedName("pickup_date") val pickupDate: String,
    @SerializedName("pickup_time") val pickupTime: String,
    @SerializedName("extra_stops") val extraStops: List<ExtraStopRequest> = emptyList(),
    @SerializedName("pickup") val pickup: String,
    @SerializedName("pickup_latitude") val pickupLatitude: String,
    @SerializedName("pickup_longitude") val pickupLongitude: String,
    @SerializedName("pickup_airport_option") val pickupAirportOption: AirportOption? = null,
    @SerializedName("pickup_airport") val pickupAirport: String, // String to match web
    @SerializedName("pickup_airport_name") val pickupAirportName: String,
    @SerializedName("pickup_airport_latitude") val pickupAirportLatitude: String, // String to match web
    @SerializedName("pickup_airport_longitude") val pickupAirportLongitude: String, // String to match web
    @SerializedName("pickup_airline_option") val pickupAirlineOption: AirlineOption? = null,
    @SerializedName("pickup_airline") val pickupAirline: String, // String to match web
    @SerializedName("pickup_airline_name") val pickupAirlineName: String,
    @SerializedName("pickup_flight") val pickupFlight: String,
    @SerializedName("origin_airport_city") val originAirportCity: String,
    @SerializedName("cruise_port") val cruisePort: String,
    @SerializedName("cruise_name") val cruiseName: String,
    @SerializedName("cruise_time") val cruiseTime: String,
    @SerializedName("dropoff") val dropoff: String,
    @SerializedName("dropoff_latitude") val dropoffLatitude: String,
    @SerializedName("dropoff_longitude") val dropoffLongitude: String,
    @SerializedName("dropoff_airport_option") val dropoffAirportOption: AirportOption? = null,
    @SerializedName("dropoff_airport") val dropoffAirport: String, // String to match web
    @SerializedName("dropoff_airport_name") val dropoffAirportName: String,
    @SerializedName("dropoff_airport_latitude") val dropoffAirportLatitude: String, // String to match web
    @SerializedName("dropoff_airport_longitude") val dropoffAirportLongitude: String, // String to match web
    @SerializedName("dropoff_airline_option") val dropoffAirlineOption: AirlineOption? = null,
    @SerializedName("dropoff_airline") val dropoffAirline: String, // String to match web
    @SerializedName("dropoff_airline_name") val dropoffAirlineName: String,
    @SerializedName("dropoff_flight") val dropoffFlight: String,
    @SerializedName("return_meet_greet_choices") val returnMeetGreetChoices: String, // String to match web
    @SerializedName("return_meet_greet_choices_name") val returnMeetGreetChoicesName: String,
    @SerializedName("return_pickup_date") val returnPickupDate: String,
    @SerializedName("return_pickup_time") val returnPickupTime: String,
    @SerializedName("return_extra_stops") val returnExtraStops: List<ExtraStopRequest> = emptyList(),
    @SerializedName("return_pickup") val returnPickup: String,
    @SerializedName("return_pickup_latitude") val returnPickupLatitude: String, // String to match web
    @SerializedName("return_pickup_longitude") val returnPickupLongitude: String, // String to match web
    @SerializedName("return_pickup_airport_option") val returnPickupAirportOption: AirportOption? = null,
    @SerializedName("return_pickup_airport") val returnPickupAirport: String, // String to match web
    @SerializedName("return_pickup_airport_name") val returnPickupAirportName: String,
    @SerializedName("return_pickup_airport_latitude") val returnPickupAirportLatitude: String, // String to match web
    @SerializedName("return_pickup_airport_longitude") val returnPickupAirportLongitude: String, // String to match web
    @SerializedName("return_pickup_airline_option") val returnPickupAirlineOption: AirlineOption? = null,
    @SerializedName("return_pickup_airline") val returnPickupAirline: String, // String to match web
    @SerializedName("return_pickup_airline_name") val returnPickupAirlineName: String,
    @SerializedName("return_pickup_flight") val returnPickupFlight: String,
    @SerializedName("return_cruise_port") val returnCruisePort: String,
    @SerializedName("return_cruise_name") val returnCruiseName: String,
    @SerializedName("return_cruise_time") val returnCruiseTime: String,
    @SerializedName("return_dropoff") val returnDropoff: String,
    @SerializedName("return_dropoff_latitude") val returnDropoffLatitude: String, // String to match web
    @SerializedName("return_dropoff_longitude") val returnDropoffLongitude: String, // String to match web
    @SerializedName("return_dropoff_airport_option") val returnDropoffAirportOption: AirportOption? = null,
    @SerializedName("return_dropoff_airport") val returnDropoffAirport: String, // String to match web
    @SerializedName("return_dropoff_airport_name") val returnDropoffAirportName: String,
    @SerializedName("return_dropoff_airport_latitude") val returnDropoffAirportLatitude: String, // String to match web
    @SerializedName("return_dropoff_airport_longitude") val returnDropoffAirportLongitude: String, // String to match web
    @SerializedName("return_dropoff_airline_option") val returnDropoffAirlineOption: AirlineOption? = null,
    @SerializedName("return_dropoff_airline") val returnDropoffAirline: String, // String to match web
    @SerializedName("return_dropoff_airline_name") val returnDropoffAirlineName: String,
    @SerializedName("return_dropoff_flight") val returnDropoffFlight: String,
    @SerializedName("driver_languages") val driverLanguages: List<String> = emptyList(), // [String] to match web
    @SerializedName("driver_dresses") val driverDresses: List<String> = emptyList(),
    @SerializedName("amenities") val amenities: List<String> = emptyList(),
    @SerializedName("chargedAmenities") val chargedAmenities: List<String> = emptyList(),
    @SerializedName("journeyDistance") val journeyDistance: String, // String to match web
    @SerializedName("journeyTime") val journeyTime: String, // String to match web
    @SerializedName("returnJourneyDistance") val returnJourneyDistance: String, // String to match web
    @SerializedName("returnJourneyTime") val returnJourneyTime: String, // String to match web
    @SerializedName("reservation_id") val reservationId: String,
    @SerializedName("updateType") val updateType: String,
    @SerializedName("departing_airport_city") val departingAirportCity: String,
    @SerializedName("currency") val currency: String = "USD",
    @SerializedName("is_master_vehicle") val isMasterVehicle: String, // String to match web
    @SerializedName("proceed") val proceed: String, // String to match web
    @SerializedName("rateArray") val rateArray: BookingRateArray,
    @SerializedName("returnRateArray") val returnRateArray: BookingRateArray? = null,
    @SerializedName("grand_total") val grandTotal: String, // String to match web
    @SerializedName("sub_total") val subTotal: String, // String to match web
    @SerializedName("return_sub_total") val returnSubTotal: String? = null, // String to match web
    @SerializedName("return_grand_total") val returnGrandTotal: String? = null, // String to match web
    @SerializedName("min_rate_involved") val minRateInvolved: String, // String to match web
    @SerializedName("shares_array") val sharesArray: SharesArray,
    @SerializedName("return_shares_array") val returnSharesArray: SharesArray? = null,
    @SerializedName("return_affiliate_type") val returnAffiliateType: String,
    @SerializedName("return_affiliate_id") val returnAffiliateId: String,
    @SerializedName("return_distance") val returnDistance: String, // String to match web
    @SerializedName("return_vehicle_id") val returnVehicleId: String, // String to match web
    @SerializedName("no_of_hours") val noOfHours: String,
    @SerializedName("fbo_address") val fboAddress: String = "",
    @SerializedName("fbo_name") val fboName: String = "",
    @SerializedName("return_fbo_address") val returnFboAddress: String = "",
    @SerializedName("return_fbo_name") val returnFboName: String = "",
    @SerializedName("created_by_role") val createdByRole: String? = null // Added to match web payload
)

/**
 * Edit Reservation Response - matches iOS EditReservationResponse structure
 */
data class EditReservationResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: EditReservationData,
    @SerializedName("message") val message: String? = null,
    @SerializedName("currency") val currency: EditReservationCurrency? = null
)

/**
 * Edit Reservation Currency - matches iOS EditReservationCurrency structure
 */
data class EditReservationCurrency(
    @SerializedName("country_name") val countryName: String? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("currency_country") val currencyCountry: String? = null,
    @SerializedName("symbol") val symbol: String? = null,
    @SerializedName("date_format") val dateFormat: String? = null
)

/**
 * Loose Customer - matches iOS LooseCustomer structure
 */
data class LooseCustomer(
    @SerializedName("first_name") val firstName: String? = null,
    @SerializedName("last_name") val lastName: String? = null,
    @SerializedName("mobileIsd") val mobileIsd: String? = null,
    @SerializedName("mobile") val mobile: String? = null,
    @SerializedName("gender") val gender: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("address") val address: String? = null,
    @SerializedName("city") val city: String? = null,
    @SerializedName("state") val state: String? = null,
    @SerializedName("country") val country: String? = null
)

/**
 * Edit Reservation Data - matches iOS EditReservationData structure exactly
 * Contains all fields from the edit reservation API response
 */
data class EditReservationData(
    @SerializedName("reservation_id") val reservationId: Int,
    @SerializedName("id") val id: Int,
    @SerializedName("related_to") val relatedTo: String? = null,
    @SerializedName("min_rate_involved") val minRateInvolved: Int,
    @SerializedName("conf_id") val confId: String? = null,
    @SerializedName("acc_id") val accId: Int,
    @SerializedName("travel_client_id") val travelClientId: Int? = null,
    @SerializedName("passenger_id") val passengerId: Int,
    @SerializedName("account_type") val accountType: String? = null,
    @SerializedName("sub_account_type") val subAccountType: String? = null,
    @SerializedName("sub_account_id") val subAccountId: String? = null,
    @SerializedName("affiliate_id") val affiliateId: Int,
    @SerializedName("farmout_affiliate") val farmoutAffiliate: String? = null,
    @SerializedName("reservation_type") val reservationType: String? = null,
    @SerializedName("referral_affiliate") val referralAffiliate: String? = null,
    @SerializedName("lose_affiliate") val loseAffiliate: String? = null,
    @SerializedName("cancellation_hours") val cancellationHours: Int? = null,
    @SerializedName("reservation_preferences_id") val reservationPreferencesId: String? = null,
    @SerializedName("transfer_type") val transferType: String,
    @SerializedName("return_transfer_type") val returnTransferType: String? = null,
    @SerializedName("pickup") val pickup: String? = null,
    @SerializedName("pickup_address") val pickupAddress: String? = null,
    @SerializedName("pickup_airport") val pickupAirport: String? = null,
    @SerializedName("pickup_airline") val pickupAirline: String? = null,
    @SerializedName("pickup_flight") val pickupFlight: String? = null,
    @SerializedName("origin_airport_city") val originAirportCity: String? = null,
    @SerializedName("pickup_latitude") val pickupLatitude: Double? = null,
    @SerializedName("pickup_longitude") val pickupLongitude: Double? = null,
    @SerializedName("dropoff") val dropoff: String? = null,
    @SerializedName("dropoff_address") val dropoffAddress: String? = null,
    @SerializedName("dropoff_airport") val dropoffAirport: String? = null,
    @SerializedName("dropoff_airline") val dropoffAirline: String? = null,
    @SerializedName("dropoff_flight") val dropoffFlight: String? = null,
    @SerializedName("dropoff_latitude") val dropoffLatitude: Double? = null,
    @SerializedName("dropoff_longitude") val dropoffLongitude: Double? = null,
    @SerializedName("return_pickup") val returnPickup: String? = null,
    @SerializedName("return_pickup_address") val returnPickupAddress: String? = null,
    @SerializedName("return_cruise_name") val returnCruiseName: String? = null,
    @SerializedName("return_cruise_port") val returnCruisePort: String? = null,
    @SerializedName("return_cruise_time") val returnCruiseTime: String? = null,
    @SerializedName("cruise_name") val cruiseName: String? = null,
    @SerializedName("cruise_port") val cruisePort: String? = null,
    @SerializedName("cruise_time") val cruiseTime: String? = null,
    @SerializedName("return_pickup_airport") val returnPickupAirport: String? = null,
    @SerializedName("return_pickup_airline") val returnPickupAirline: String? = null,
    @SerializedName("return_pickup_flight") val returnPickupFlight: String? = null,
    @SerializedName("return_origin_airport_city") val returnOriginAirportCity: String? = null,
    @SerializedName("return_pickup_latitude") val returnPickupLatitude: Double? = null,
    @SerializedName("return_pickup_longitude") val returnPickupLongitude: Double? = null,
    @SerializedName("return_dropoff") val returnDropoff: String? = null,
    @SerializedName("fbo_address") val fboAddress: String? = null,
    @SerializedName("fbo_name") val fboName: String? = null,
    @SerializedName("return_dropoff_address") val returnDropoffAddress: String? = null,
    @SerializedName("return_dropoff_airport") val returnDropoffAirport: String? = null,
    @SerializedName("return_dropoff_airline") val returnDropoffAirline: String? = null,
    @SerializedName("return_dropoff_flight") val returnDropoffFlight: String? = null,
    @SerializedName("return_dropoff_latitude") val returnDropoffLatitude: Double? = null,
    @SerializedName("return_dropoff_longitude") val returnDropoffLongitude: Double? = null,
    @SerializedName("meet_greet_choices") val meetGreetChoices: Int,
    @SerializedName("return_meet_greet_choices") val returnMeetGreetChoices: Int? = null,
    @JsonAdapter(ExtraStopsStringTypeAdapter::class)
    @SerializedName("extra_stops") val extraStops: String? = null,
    @JsonAdapter(ExtraStopsStringTypeAdapter::class)
    @SerializedName("return_extra_stops") val returnExtraStops: String? = null,
    @SerializedName("service_type") val serviceType: String,
    @SerializedName("pickup_date") val pickupDate: String,
    @SerializedName("pickup_time") val pickupTime: String,
    @SerializedName("return_pickup_date") val returnPickupDate: String? = null,
    @SerializedName("return_pickup_time") val returnPickupTime: String? = null,
    @SerializedName("total_passengers") val totalPassengers: Int,
    @SerializedName("luggage_count") val luggageCount: Int,
    @SerializedName("child_certified") val childCertified: String? = null,
    @SerializedName("baby_seat") val babySeat: String? = null,
    @SerializedName("booster_seat") val boosterSeat: String? = null,
    @SerializedName("pet_friendly") val petFriendly: String? = null,
    @SerializedName("handicap") val handicap: String? = null,
    @SerializedName("vehicle_type") val vehicleType: Int? = null,
    @SerializedName("vehicle_id") val vehicleId: Int? = null,
    @SerializedName("driver_id") val driverId: Int? = null,
    @SerializedName("payment_id") val paymentId: String? = null,
    @SerializedName("quote_amount") val quoteAmount: String? = null,
    @SerializedName("hourly_rate") val hourlyRate: String? = null,
    @SerializedName("number_of_hours") val numberOfHours: Int? = null,
    @SerializedName("number_of_vehicles") val numberOfVehicles: Int? = null,
    @SerializedName("distance") val distance: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("amenities") val amenities: String? = null,
    @SerializedName("driver_languages") val driverLanguages: String? = null,
    @SerializedName("driver_dresses") val driverDresses: String? = null,
    @SerializedName("booking_instructions") val bookingInstructions: String? = null,
    @SerializedName("accepted_on") val acceptedOn: String? = null,
    @SerializedName("payment_status") val paymentStatus: String? = null,
    @SerializedName("booking_status") val bookingStatus: String? = null,
    @SerializedName("charged_amount") val chargedAmount: String? = null,
    @SerializedName("affiliate_charged_amount") val affiliateChargedAmount: String? = null,
    @SerializedName("status_change_flag") val statusChangeFlag: String? = null,
    @SerializedName("email_token") val emailToken: String? = null,
    @SerializedName("created_by_role") val createdByRole: String? = null,
    @SerializedName("created_by") val createdBy: Int? = null,
    @SerializedName("cancellation_reason") val cancellationReason: String? = null,
    @SerializedName("departing_airport_city") val departingAirportCity: String? = null,
    @SerializedName("status") val status: String? = null,
    @SerializedName("reservation_shares") val reservationShares: String? = null,
    @SerializedName("share_array") val shareArray: String? = null,
    @SerializedName("reminder_before_one_hour") val reminderBeforeOneHour: String? = null,
    @SerializedName("reminder_before_one_day") val reminderBeforeOneDay: String? = null,
    @SerializedName("reminder_email_sent") val reminderEmailSent: String? = null,
    @SerializedName("reminder_sms_sent") val reminderSmsSent: String? = null,
    @SerializedName("cancellation_reminder_sms_sent") val cancellationReminderSmsSent: String? = null,
    @SerializedName("changed_fields") val changedFields: String? = null,
    @SerializedName("booking_related_to") val bookingRelatedTo: Int? = null,
    @SerializedName("is_transferred") val isTransferred: Int? = null,
    @SerializedName("waiting_time_in_mins") val waitingTimeInMins: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("passenger_name") val passengerName: String? = null,
    @SerializedName("passenger_cell_isd") val passengerCellIsd: String? = null,
    @SerializedName("passenger_cell_country") val passengerCellCountry: String? = null,
    @SerializedName("passenger_cell") val passengerCell: String? = null,
    @SerializedName("passenger_email") val passengerEmail: String? = null,
    @SerializedName("passenger_image") val passengerImage: String? = null,
    @SerializedName("chargedAmenities") val chargedAmenities: String? = null,
    @SerializedName("vehicle_year") val vehicleYear: String? = null,
    @SerializedName("vehicle_make") val vehicleMake: Int? = null,
    @SerializedName("vehicle_model") val vehicleModel: Int? = null,
    @SerializedName("vehicle_color") val vehicleColor: Int? = null,
    @SerializedName("vehicle_license_plate") val vehicleLicensePlate: String? = null,
    @SerializedName("vehicle_seats") val vehicleSeats: Int? = null,
    @SerializedName("driver_name") val driverName: String? = null,
    @SerializedName("driver_gender") val driverGender: String? = null,
    @SerializedName("driver_cell_isd") val driverCellIsd: String? = null,
    @SerializedName("driver_cell_country") val driverCellCountry: String? = null,
    @SerializedName("driver_cell") val driverCell: String? = null,
    @SerializedName("driver_email") val driverEmail: String? = null,
    @SerializedName("driver_phone_type") val driverPhoneType: String? = null,
    @SerializedName("driver_veteran") val driverVeteran: String? = null,
    @SerializedName("driver_dod") val driverDod: String? = null,
    @SerializedName("driver_foid_card") val driverFoidCard: String? = null,
    @SerializedName("driver_ex_law_officer") val driverExLawOfficer: String? = null,
    @SerializedName("driver_background_certified") val driverBackgroundCertified: String? = null,
    @SerializedName("vehicle_image_id") val vehicleImageId: String? = null,
    @SerializedName("driver_image_id") val driverImageId: String? = null,
    @SerializedName("lose_affiliate_name") val loseAffiliateName: String? = null,
    @SerializedName("lose_affiliate_phone_isd") val loseAffiliatePhoneIsd: String? = null,
    @SerializedName("lose_affiliate_phone_country") val loseAffiliatePhoneCountry: String? = null,
    @SerializedName("lose_affiliate_phone") val loseAffiliatePhone: String? = null,
    @SerializedName("lose_affiliate_email") val loseAffiliateEmail: String? = null,
    @SerializedName("currency") val currency: String? = null,
    @SerializedName("pickup_airport_latitude") val pickupAirportLatitude: Double? = null,
    @SerializedName("pickup_airport_longitude") val pickupAirportLongitude: Double? = null,
    @SerializedName("dropoff_airport_latitude") val dropoffAirportLatitude: Double? = null,
    @SerializedName("dropoff_airport_longitude") val dropoffAirportLongitude: Double? = null,
    @SerializedName("affiliate_type") val affiliateType: String? = null,
    @SerializedName("loose_customer") val looseCustomer: LooseCustomer? = null,
    @SerializedName("vehicle_created_by") val vehicleCreatedBy: Int? = null,
    @SerializedName("vehicle_year_name") val vehicleYearName: String? = null,
    @SerializedName("vehicle_make_name") val vehicleMakeName: String? = null,
    @SerializedName("vehicle_model_name") val vehicleModelName: String? = null,
    @SerializedName("vehicle_color_name") val vehicleColorName: String? = null,
    @SerializedName("vehicle_license_plate_name") val vehicleLicensePlateName: String? = null,
    @SerializedName("vehicle_seats_name") val vehicleSeatsName: Int? = null,
    @SerializedName("vehicle_type_name") val vehicleTypeName: String? = null,
    @SerializedName("vehicle_images") val vehicleImages: List<String>? = null,
    @SerializedName("driver_image") val driverImage: String? = null,
    @SerializedName("meet_greet_choice_name") val meetGreetChoiceName: String? = null
)

/**
 * Edit Reservation Update Response - matches iOS EditReservationUpdateResponse structure
 */
data class EditReservationUpdateResponse(
    @SerializedName("success") val success: Boolean? = null,
    @SerializedName("data") val data: EditReservationUpdateData? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("currency") val currency: EditReservationCurrency? = null
)

/**
 * Edit Reservation Update Data - matches iOS EditReservationUpdateData structure
 */
data class EditReservationUpdateData(
    @SerializedName("reservation_id") val reservationId: String
)



