package com.example.limouserapp.data.model.booking

import com.google.gson.annotations.SerializedName

/**
 * Booking Rate Array - matches iOS BookingRateArray structure
 * Used for creating reservation requests with detailed rate breakdown
 */
data class BookingRateArray(
    @SerializedName("all_inclusive_rates") val allInclusiveRates: Map<String, RateItem> = emptyMap(),
    @SerializedName("amenities") val amenities: Map<String, RateItem> = emptyMap(),
    @SerializedName("taxes") val taxes: Map<String, TaxItem> = emptyMap(),
    @SerializedName("misc") val misc: Map<String, RateItem> = emptyMap()
)

/**
 * Rate Item - matches iOS RateItem structure
 */
data class RateItem(
    @SerializedName("rate_label") val rateLabel: String,
    @SerializedName("baserate") val baserate: Double,
    @SerializedName("multiple") val multiple: Double? = null,
    @SerializedName("percentage") val percentage: Double? = null,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String? = null,
    @SerializedName("flat_baserate") val flatBaserate: Double? = null
)

/**
 * Tax Item - matches iOS TaxItem structure
 */
data class TaxItem(
    @SerializedName("rate_label") val rateLabel: String,
    @SerializedName("baserate") val baserate: Double,
    @SerializedName("flat_baserate") val flatBaserate: Double? = null,
    @SerializedName("multiple") val multiple: Double? = null,
    @SerializedName("percentage") val percentage: Double? = null,
    @SerializedName("amount") val amount: Double,
    @SerializedName("type") val type: String? = null
)

/**
 * Shares Array - matches iOS SharesArray structure
 * Contains calculated share breakdown for reservation
 */
data class SharesArray(
    @SerializedName("baseRate") val baseRate: Double,
    @SerializedName("grandTotal") val grandTotal: Double,
    @SerializedName("stripeFee") val stripeFee: Double,
    @SerializedName("adminShare") val adminShare: Double,
    @SerializedName("deducted_admin_share") val deductedAdminShare: Double,
    @SerializedName("affiliateShare") val affiliateShare: Double,
    @SerializedName("travelAgentShare") val travelAgentShare: Double? = null,
    @SerializedName("farmoutShare") val farmoutShare: Double? = null,
    @SerializedName("returnGrandTotal") val returnGrandTotal: Double? = null
)

