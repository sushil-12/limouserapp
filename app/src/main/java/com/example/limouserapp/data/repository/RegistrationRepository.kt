package com.example.limouserapp.data.repository

import com.example.limouserapp.data.api.RegistrationApi
import com.example.limouserapp.data.model.BaseResponse
import com.example.limouserapp.data.model.registration.AddCreditCardRequest
import com.example.limouserapp.data.model.registration.BasicDetailsData
import com.example.limouserapp.data.model.registration.BasicDetailsRequest
import com.example.limouserapp.data.model.registration.CreditCardData
import com.example.limouserapp.data.model.registration.CreditCardRequest
import com.example.limouserapp.data.network.error.ErrorHandler
import com.example.limouserapp.data.network.error.NetworkError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registration repository
 * Handles user registration and profile completion
 */
@Singleton
class RegistrationRepository @Inject constructor(
    private val registrationApi: RegistrationApi,
    private val errorHandler: ErrorHandler
) {
    
    /**
     * Submit basic user details
     */
    suspend fun submitBasicDetails(
        name: String,
        email: String
    ): Result<BaseResponse<BasicDetailsData>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = BasicDetailsRequest(
                    name = name.trim(),
                    email = email.trim().lowercase()
                )
                
                val response = registrationApi.submitBasicDetails(request)
                Result.success(response)
            } catch (e: Exception) {
                // Preserve the original exception for proper error handling
                Result.failure(e)
            }
        }
    }
    
    /**
     * Submit credit card during registration
     */
    suspend fun submitCreditCard(
        cardNumber: String,
        expMonth: String,
        expYear: String,
        cvc: String,
        cardHolderName: String,
        location: String? = null,
        city: String? = null,
        state: String? = null,
        zipCode: String? = null,
        cardType: String? = null,
        smsOptIn: Boolean? = null
    ): Result<BaseResponse<CreditCardData>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = CreditCardRequest(
                    number = cardNumber.replace(" ", "").replace("-", ""),
                    exp_month = expMonth,
                    exp_year = expYear,
                    cvc = cvc,
                    name = cardHolderName.trim(),
                    location = location?.trim(),
                    city = city?.trim(),
                    state = state?.trim(),
                    zipCode = zipCode?.trim(),
                    card_type = cardType,
                    smsOptIn = smsOptIn
                )
                
                val response = registrationApi.submitCreditCard(request)
                Result.success(response)
            } catch (e: Exception) {
                // Preserve the original exception for proper error handling
                Result.failure(e)
            }
        }
    }
    
    /**
     * Add additional credit card to existing account
     */
    suspend fun addCreditCard(
        cardNumber: String,
        expMonth: String,
        expYear: String,
        cvc: String,
        cardHolderName: String,
        cardType: String
    ): Result<BaseResponse<CreditCardData>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = AddCreditCardRequest(
                    card_type = cardType,
                    number = cardNumber.replace(" ", "").replace("-", ""),
                    cvc = cvc,
                    exp_month = expMonth,
                    exp_year = expYear,
                    name = cardHolderName.trim()
                )
                
                val response = registrationApi.addCreditCard(request)
                Result.success(response)
            } catch (e: Exception) {
                // Preserve the original exception for proper error handling
                Result.failure(e)
            }
        }
    }
    
}
