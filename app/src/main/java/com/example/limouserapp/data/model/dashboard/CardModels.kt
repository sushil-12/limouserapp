package com.example.limouserapp.data.model.dashboard

import com.google.gson.annotations.SerializedName

/**
 * Profile data response matching iOS structure
 */
data class ProfileDataResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: ProfileData,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("currency")
    val currency: CurrencyInfo? = null
)

/**
 * Profile data with cards - matches iOS ProfileData structure
 */
data class ProfileData(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("first_name")
    val firstName: String,
    
    @SerializedName("last_name")
    val lastName: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("mobile")
    val mobile: String,
    
    @SerializedName("cards")
    val cards: List<CardData> = emptyList(),
    
    // Location and address fields
    @SerializedName("city")
    val city: String? = null,
    
    @SerializedName("state")
    val state: String? = null,
    
    @SerializedName("country")
    val country: String? = null,
    
    @SerializedName("zip")
    val zip: String? = null,
    
    @SerializedName("address")
    val address: String? = null,
    
    @SerializedName("street")
    val street: String? = null,
    
    // Phone number fields
    @SerializedName("mobileIsd")
    val mobileIsd: String? = null,
    
    @SerializedName("mobileCountry")
    val mobileCountry: String? = null,
    
    // Optional fields
    @SerializedName("title")
    val title: String? = null,
    
    @SerializedName("middle_name")
    val middleName: String? = null,
    
    @SerializedName("gender")
    val gender: String? = null
) {
    /**
     * Get full name
     */
    val fullName: String
        get() = "$firstName $lastName"
}

/**
 * Card data from profile API
 */
data class CardData(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("ID")
    val ID: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("brand")
    val brand: String,
    
    @SerializedName("exp_month")
    val expMonth: Int,
    
    @SerializedName("exp_year")
    val expYear: Int,
    
    @SerializedName("last4")
    val last4: String,
    
    @SerializedName("card_type")
    val cardType: String,
    
    @SerializedName("cc_prority")
    val ccPriority: String? = null
) {
    /**
     * Check if card is primary
     */
    val isPrimary: Boolean
        get() = ccPriority?.lowercase() == "primary"
    
    /**
     * Get formatted card number (e.g., XXXX-XXXX-XXXX-1234)
     */
    val formattedCardNumber: String
        get() = if (last4.startsWith("XXXX-XXXX-XXXX-")) {
            last4
        } else {
            "XXXX-XXXX-XXXX-$last4"
        }
    
    /**
     * Get brand in uppercase
     */
    val brandUppercase: String
        get() = brand.uppercase()
}

/**
 * Add credit card request
 */
data class AddCreditCardRequest(
    @SerializedName("card_type")
    val cardType: String,
    
    @SerializedName("number")
    val number: String,
    
    @SerializedName("cvc")
    val cvc: String,
    
    @SerializedName("exp_month")
    val expMonth: String,
    
    @SerializedName("exp_year")
    val expYear: String,
    
    @SerializedName("name")
    val name: String
)

/**
 * Add credit card response
 */
data class AddCreditCardResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("data")
    val data: AddCreditCardData,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("currency")
    val currency: CurrencyInfo
)

/**
 * Add credit card response data
 */
data class AddCreditCardData(
    @SerializedName("id")
    val id: String
)

