package com.example.limouserapp.data.model.registration

import com.google.gson.annotations.SerializedName

/**
 * Request model for basic details registration
 */
data class BasicDetailsRequest(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("email")
    val email: String
)

/**
 * Response data for basic details registration
 */
data class BasicDetailsData(
    @SerializedName("account_id")
    val accountId: Int,
    
    @SerializedName("next_step")
    val nextStep: String
)

/**
 * Request model for credit card registration (during signup)
 */
data class CreditCardRequest(
    @SerializedName("number")
    val number: String,
    
    @SerializedName("exp_month")
    val expMonth: String,
    
    @SerializedName("exp_year")
    val expYear: String,
    
    @SerializedName("cvc")
    val cvc: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("location")
    val location: String?,
    
    @SerializedName("city")
    val city: String?,
    
    @SerializedName("state")
    val state: String?,
    
    @SerializedName("zipCode")
    val zipCode: String?,
    
    @SerializedName("card_type")
    val cardType: String?,
    
    @SerializedName("smsOptIn")
    val smsOptIn: Boolean?
)

/**
 * Request model for adding additional credit cards
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
 * Response data for credit card operations
 */
data class CreditCardData(
    @SerializedName("stripe_customer_id")
    val stripeCustomerId: String,
    
    @SerializedName("card_id")
    val cardId: String
)

/**
 * Credit card information model
 */
data class CreditCardInfo(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("last4")
    val last4: String,
    
    @SerializedName("brand")
    val brand: String,
    
    @SerializedName("exp_month")
    val expMonth: Int,
    
    @SerializedName("exp_year")
    val expYear: Int,
    
    @SerializedName("is_default")
    val isDefault: Boolean,
    
    @SerializedName("created_at")
    val createdAt: String
)
