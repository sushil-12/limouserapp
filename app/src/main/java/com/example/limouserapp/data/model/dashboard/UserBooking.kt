package com.example.limouserapp.data.model.dashboard

import com.google.gson.annotations.SerializedName

/**
 * Represents a user booking with comprehensive booking details
 * Mirrors the iOS UserBooking model with proper Android conventions
 */
data class UserBooking(
    @SerializedName("booking_id")
    val bookingId: Int,
    
    @SerializedName("account_type")
    val accountType: String?,
    
    @SerializedName("sub_account_type")
    val subAccountType: String?,
    
    @SerializedName("lose_affiliate")
    val loseAffiliate: Int?,
    
    @SerializedName("pickup_date")
    val pickupDate: String,
    
    @SerializedName("pickup_time")
    val pickupTime: String,
    
    @SerializedName("service_type")
    val serviceType: String,
    
    @SerializedName("payment_status")
    val paymentStatus: String,
    
    @SerializedName("payment_method")
    val paymentMethod: String?,
    
    @SerializedName("booking_status")
    val bookingStatus: String,
    
    @SerializedName("status")
    val status: Boolean,
    
    @SerializedName("affiliate_id")
    val affiliateId: Int,
    
    @SerializedName("is_transferred")
    val isTransferred: Int,
    
    @SerializedName("passenger_name")
    val passengerName: String?,
    
    @SerializedName("passenger_email")
    val passengerEmail: String?,
    
    @SerializedName("passenger_cell_isd")
    val passengerCellIsd: String?,
    
    @SerializedName("passenger_cell_country")
    val passengerCellCountry: String?,
    
    @SerializedName("passenger_cell")
    val passengerCell: String?,
    
    @SerializedName("pickup_address")
    val pickupAddress: String,
    
    @SerializedName("dropoff_address")
    val dropoffAddress: String,
    
    @SerializedName("vehicle_cat_name")
    val vehicleCategoryName: String?,
    
    @SerializedName("affiliate_type")
    val affiliateType: String?,
    
    @SerializedName("company_name")
    val companyName: String?,
    
    @SerializedName("affiliate_dispatch_isd")
    val affiliateDispatchIsd: String?,
    
    @SerializedName("affiliate_dispatch_number")
    val affiliateDispatchNumber: String?,
    
    @SerializedName("dispatchEmail")
    val dispatchEmail: String?,
    
    @SerializedName("gig_cell_isd")
    val gigCellIsd: String?,
    
    @SerializedName("gig_cell_mobile")
    val gigCellMobile: String?,
    
    @SerializedName("gig_email")
    val gigEmail: String?,
    
    @SerializedName("driver_first_name")
    val driverFirstName: String?,
    
    @SerializedName("driver_last_name")
    val driverLastName: String?,
    
    @SerializedName("driver_cell_isd")
    val driverCellIsd: String?,
    
    @SerializedName("driver_cell_number")
    val driverCellNumber: String?,
    
    @SerializedName("loose_affiliate_name")
    val looseAffiliateName: String?,
    
    @SerializedName("loose_affiliate_phone_isd")
    val looseAffiliatePhoneIsd: String?,
    
    @SerializedName("loose_affiliate_phone")
    val looseAffiliatePhone: String?,
    
    @SerializedName("loose_affiliate_email")
    val looseAffiliateEmail: String?,
    
    @SerializedName("loose_aff_driver_name")
    val looseAffDriverName: String?,
    
    @SerializedName("reservation_type")
    val reservationType: String?,
    
    @SerializedName("grand_total")
    val grandTotal: Double?,
    
    @SerializedName("currency")
    val currency: String?,
    
    @SerializedName("farmout_affiliate")
    val farmoutAffiliate: String?,
    
    @SerializedName("pickup_day")
    val pickupDay: String,
    
    @SerializedName("pax_tel")
    val paxTel: String?,
    
    @SerializedName("pax_isd")
    val paxIsd: String?,
    
    @SerializedName("currency_symbol")
    val currencySymbol: String?,
    
    @SerializedName("driver_name")
    val driverName: String?,
    
    @SerializedName("agent_type")
    val agentType: String,
    
    @SerializedName("driver_tel")
    val driverTel: String?,

    @SerializedName("transfer_type")
    var transferType: String?,


) {
    /**
     * Computed properties for better UI handling
     */
    val fullDriverName: String
        get() = when {
            !driverFirstName.isNullOrEmpty() && !driverLastName.isNullOrEmpty() -> 
                "$driverFirstName $driverLastName"
            !driverName.isNullOrEmpty() -> driverName
            else -> "Driver"
        }
    
    val fullPassengerName: String
        get() = passengerName ?: "Passenger"
    
    val formattedPhoneNumber: String
        get() = when {
            !passengerCellIsd.isNullOrEmpty() && !passengerCell.isNullOrEmpty() -> 
                "$passengerCellIsd $passengerCell"
            !passengerCell.isNullOrEmpty() -> passengerCell
            else -> ""
        }
    
    val formattedDriverPhone: String
        get() = when {
            !driverCellIsd.isNullOrEmpty() && !driverCellNumber.isNullOrEmpty() -> 
                "$driverCellIsd $driverCellNumber"
            !driverTel.isNullOrEmpty() -> driverTel
            else -> ""
        }
    
    val formattedTotal: String
        get() = when {
            grandTotal != null && !currencySymbol.isNullOrEmpty() -> 
                "$currencySymbol${String.format("%.2f", grandTotal)}"
            grandTotal != null -> 
                String.format("%.2f", grandTotal)
            else -> "N/A"
        }
    
    val isUpcoming: Boolean
        get() = bookingStatus.lowercase() in listOf("confirmed", "assigned", "in_progress")
    
    val isCompleted: Boolean
        get() = bookingStatus.lowercase() in listOf("completed", "finished")
    
    val isCancelled: Boolean
        get() = bookingStatus.lowercase() in listOf("cancelled", "cancelled_by_user", "cancelled_by_driver")
    
    val statusDisplayText: String
        get() = when (bookingStatus.lowercase()) {
            "confirmed" -> "Confirmed"
            "assigned" -> "Driver Assigned"
            "in_progress" -> "In Progress"
            "completed" -> "Completed"
            "cancelled" -> "Cancelled"
            "cancelled_by_user" -> "Cancelled by You"
            "cancelled_by_driver" -> "Cancelled by Driver"
            else -> bookingStatus.replace("_", " ").split(" ").joinToString(" ") { 
                it.replaceFirstChar { char -> char.uppercaseChar() }
            }
        }
}

/**
 * Booking status enumeration for type safety
 */
enum class BookingStatus(val value: String) {
    CONFIRMED("confirmed"),
    ASSIGNED("assigned"),
    IN_PROGRESS("in_progress"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    CANCELLED_BY_USER("cancelled_by_user"),
    CANCELLED_BY_DRIVER("cancelled_by_driver");
    
    companion object {
        fun fromString(status: String): BookingStatus? {
            return values().find { it.value == status.lowercase() }
        }
    }
}

/**
 * Payment status enumeration
 */
enum class PaymentStatus(val value: String) {
    PENDING("pending"),
    PAID("paid"),
    FAILED("failed"),
    REFUNDED("refunded");
    
    companion object {
        fun fromString(status: String): PaymentStatus? {
            return values().find { it.value == status.lowercase() }
        }
    }
}
