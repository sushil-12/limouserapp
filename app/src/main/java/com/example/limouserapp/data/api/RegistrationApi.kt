package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.BaseResponse
import com.example.limouserapp.data.model.registration.AddCreditCardRequest
import com.example.limouserapp.data.model.registration.BasicDetailsData
import com.example.limouserapp.data.model.registration.BasicDetailsRequest
import com.example.limouserapp.data.model.registration.CreditCardData
import com.example.limouserapp.data.model.registration.CreditCardRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Registration API service interface
 * Handles user registration and profile completion
 */
interface RegistrationApi {
    
    /**
     * Submit basic user details (name, email)
     * @param request User's basic information
     * @return Response containing account ID and next step
     */
    @POST("api/mobile/v1/user/registration/basic-details")
    suspend fun submitBasicDetails(
        @Body request: BasicDetailsRequest
    ): BaseResponse<BasicDetailsData>
    
    /**
     * Submit credit card during registration
     * @param request Credit card information with location data
     * @return Response containing Stripe customer ID and card ID
     */
    @POST("api/mobile/v1/user/registration/credit-card")
    suspend fun submitCreditCard(
        @Body request: CreditCardRequest
    ): BaseResponse<CreditCardData>
    
    /**
     * Add additional credit card to existing account
     * @param request Credit card information
     * @return Response containing Stripe customer ID and card ID
     */
    @POST("api/individual/add-credit-card")
    suspend fun addCreditCard(
        @Body request: AddCreditCardRequest
    ): BaseResponse<CreditCardData>
}
