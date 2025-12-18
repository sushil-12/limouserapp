package com.example.limouserapp.data.model.registration

import kotlinx.serialization.Serializable

/**
 * User registration data models
 */

@Serializable
data class BasicDetailsRequest(
    val name: String,
    val email: String
)

@Serializable
data class BasicDetailsResponse(
    val success: Boolean,
    val message: String,
    val data: BasicDetailsData,
    val timestamp: String,
    val code: Int
)

@Serializable
data class BasicDetailsData(
    val account_id: Int,
    val next_step: String
)

@Serializable
data class CreditCardRequest(
    val number: String,
    val exp_month: String,
    val exp_year: String,
    val cvc: String,
    val name: String,
    val location: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?,
    val card_type: String?,
    val smsOptIn: Boolean?
)

@Serializable
data class AddCreditCardRequest(
    val card_type: String,
    val number: String,
    val cvc: String,
    val exp_month: String,
    val exp_year: String,
    val name: String
)

@Serializable
data class CreditCardResponse(
    val success: Boolean,
    val message: String,
    val data: CreditCardData,
    val timestamp: String,
    val code: Int
)

@Serializable
data class CreditCardData(
    val stripe_customer_id: String? = null,
    val card_id: String? = null,
    val account_id: Int? = null,
    val next_step: String? = null
)
