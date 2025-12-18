package com.example.limouserapp.data.model.dashboard

import com.google.gson.annotations.SerializedName
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Represents an invoice with comprehensive invoice details
 * Mirrors the iOS Invoice model with proper Android conventions
 */
data class Invoice(
    @SerializedName("booking_id")
    val bookingId: Int,
    
    @SerializedName("driver_id")
    val driverId: Int,
    
    @SerializedName("aff_vehicle_type")
    val vehicleType: String,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("invoice_number")
    val invoiceNumber: Int,
    
    @SerializedName("reservation_preferences_id")
    val reservationPreferencesId: Int?,
    
    @SerializedName("date")
    val date: String,
    
    @SerializedName("pickup_address")
    val pickupAddress: String,
    
    @SerializedName("dropoff_address")
    val dropoffAddress: String,
    
    @SerializedName("driver_company")
    val driverCompany: String?,
    
    @SerializedName("account_first_name")
    val accountFirstName: String,
    
    @SerializedName("currency")
    val currency: String,
    
    @SerializedName("stripe_customer_id")
    val stripeCustomerId: String,
    
    @SerializedName("account_last_name")
    val accountLastName: String,
    
    @SerializedName("account_email")
    val accountEmail: String,
    
    @SerializedName("payment_method")
    val paymentMethod: String,
    
    @SerializedName("booking_total")
    val bookingTotal: Double,
    
    @SerializedName("card_id")
    val cardId: String?,
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("city")
    val city: String,
    
    @SerializedName("state")
    val state: String,
    
    @SerializedName("zip")
    val zip: String,
    
    @SerializedName("charge_object_id")
    val chargeObjectId: String?,
    
    @SerializedName("driver_name")
    val driverName: String,
    
    @SerializedName("driver_phone")
    val driverPhone: String,
    
    @SerializedName("passenger_name")
    val passengerName: String,
    
    @SerializedName("passenger_phone")
    val passengerPhone: String
) {
    /**
     * Computed property for formatted date
     * Input format: "MMM dd, yyyy" (e.g., "Sep 11, 2025")
     * Output format: "MMM dd, yyyy"
     */
    val formattedDate: String
        get() {
            return try {
                val inputFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                val dateObj = inputFormatter.parse(date)
                dateObj?.let {
                    inputFormatter.format(it)
                } ?: date
            } catch (e: Exception) {
                date // Return original if parsing fails
            }
        }
    
    /**
     * Computed property for formatted total
     * booking_total is in cents, so divide by 100
     */
    val formattedTotal: String
        get() {
            val amountInDollars = bookingTotal / 100.0
            val currencySymbol = getCurrencySymbol()
            return "$currencySymbol${String.format("%.2f", amountInDollars)}"
        }
    
    /**
     * Computed property for status color name
     */
    val statusColor: String
        get() {
            return when (status.lowercase()) {
                "paid" -> "green"
                "paid_cash" -> "blue"
                "pending" -> "orange"
                "failed" -> "red"
                else -> "gray"
            }
        }
    
    /**
     * Computed property for payment method display
     */
    val paymentMethodDisplay: String
        get() {
            return when (paymentMethod.lowercase()) {
                "credit_card" -> "Credit Card"
                "cash" -> "Cash"
                "debit_card" -> "Debit Card"
                else -> paymentMethod.replaceFirstChar { 
                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                }
            }
        }
    
    /**
     * Helper function to get currency symbol
     */
    private fun getCurrencySymbol(): String {
        return when (currency.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "CAD" -> "C$"
            "AUD" -> "A$"
            else -> "$"
        }
    }
}

/**
 * Invoice API Response Model
 */
data class InvoiceResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: InvoiceData,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("currency")
    val currency: CurrencyInfo?
)

/**
 * Invoice Data (matches PaginatedData structure)
 */
data class InvoiceData(
    @SerializedName("current_page")
    val currentPage: Int,
    
    @SerializedName("data")
    val data: List<Invoice>,
    
    @SerializedName("first_page_url")
    val firstPageUrl: String?,
    
    @SerializedName("from")
    val from: Int?,
    
    @SerializedName("last_page")
    val lastPage: Int,
    
    @SerializedName("last_page_url")
    val lastPageUrl: String?,
    
    @SerializedName("links")
    val links: List<PageLink>,
    
    @SerializedName("next_page_url")
    val nextPageUrl: String?,
    
    @SerializedName("path")
    val path: String,
    
    @SerializedName("per_page")
    val perPage: Int,
    
    @SerializedName("prev_page_url")
    val prevPageUrl: String?,
    
    @SerializedName("to")
    val to: Int?,
    
    @SerializedName("total")
    val total: Int
)

