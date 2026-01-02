package com.example.limouserapp.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.limouserapp.data.model.dashboard.UserProfile
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User state manager for persisting user authentication and profile state
 * Uses EncryptedSharedPreferences for secure storage
 */
@Singleton
class UserStateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "user_state_prefs"
        private const val KEY_IS_PROFILE_COMPLETED = "is_profile_completed"
        private const val KEY_HAS_DRIVER_REGISTRATION = "has_driver_registration"
        private const val KEY_IS_DRIVER_REGISTRATION_COMPLETED = "is_driver_registration_completed"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_PHONE_NUMBER = "phone_number"
        private const val KEY_ACCOUNT_ID = "account_id"
        private const val KEY_NEXT_STEP = "next_step"
        private const val KEY_BASIC_DETAILS_NAME = "basic_details_name"
        private const val KEY_BASIC_DETAILS_EMAIL = "basic_details_email"
        private const val KEY_JUST_NAVIGATED_BACK_FROM_CREDIT_CARD = "just_navigated_back_from_credit_card"
        private const val KEY_USER_PROFILE_DATA = "user_profile_data"
        private const val KEY_PROFILE_UPDATED_FROM_ACCOUNT_SETTINGS = "profile_updated_from_account_settings"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Save user profile completion status
     */
    fun setProfileCompleted(isCompleted: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_IS_PROFILE_COMPLETED, isCompleted)
            .apply()
    }
    
    /**
     * Get user profile completion status
     */
    fun isProfileCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_PROFILE_COMPLETED, false)
    }
    
    /**
     * Save driver registration status
     */
    fun setDriverRegistrationStatus(hasRegistration: Boolean, isCompleted: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_HAS_DRIVER_REGISTRATION, hasRegistration)
            .putBoolean(KEY_IS_DRIVER_REGISTRATION_COMPLETED, isCompleted)
            .apply()
    }
    
    /**
     * Get driver registration status
     */
    fun hasDriverRegistration(): Boolean {
        return sharedPreferences.getBoolean(KEY_HAS_DRIVER_REGISTRATION, false)
    }
    
    /**
     * Get driver registration completion status
     */
    fun isDriverRegistrationCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_DRIVER_REGISTRATION_COMPLETED, false)
    }
    
    /**
     * Save user ID
     */
    fun setUserId(userId: Int) {
        sharedPreferences.edit()
            .putInt(KEY_USER_ID, userId)
            .apply()
    }
    
    /**
     * Get user ID
     */
    fun getUserId(): Int? {
        val userId = sharedPreferences.getInt(KEY_USER_ID, -1)
        return if (userId != -1) userId else null
    }
    
    /**
     * Save phone number
     */
    fun setPhoneNumber(phoneNumber: String) {
        sharedPreferences.edit()
            .putString(KEY_PHONE_NUMBER, phoneNumber)
            .apply()
    }
    
    /**
     * Get phone number
     */
    fun getPhoneNumber(): String? {
        return sharedPreferences.getString(KEY_PHONE_NUMBER, null)
    }
    
    /**
     * Save account ID
     */
    fun setAccountId(accountId: Int) {
        sharedPreferences.edit()
            .putInt(KEY_ACCOUNT_ID, accountId)
            .apply()
    }
    
    /**
     * Get account ID
     */
    fun getAccountId(): Int? {
        val accountId = sharedPreferences.getInt(KEY_ACCOUNT_ID, -1)
        return if (accountId != -1) accountId else null
    }
    
    /**
     * Save next step in registration flow
     */
    fun setNextStep(nextStep: String) {
        sharedPreferences.edit()
            .putString(KEY_NEXT_STEP, nextStep)
            .apply()
    }
    
    /**
     * Get next step in registration flow
     */
    fun getNextStep(): String? {
        return sharedPreferences.getString(KEY_NEXT_STEP, null)
    }
    
    /**
     * Save basic details (name and email) for persistence across app restarts
     */
    fun setBasicDetails(name: String, email: String) {
        sharedPreferences.edit()
            .putString(KEY_BASIC_DETAILS_NAME, name)
            .putString(KEY_BASIC_DETAILS_EMAIL, email)
            .apply()
    }
    
    /**
     * Get saved basic details name
     */
    fun getBasicDetailsName(): String? {
        return sharedPreferences.getString(KEY_BASIC_DETAILS_NAME, null)
    }
    
    /**
     * Get saved basic details email
     */
    fun getBasicDetailsEmail(): String? {
        return sharedPreferences.getString(KEY_BASIC_DETAILS_EMAIL, null)
    }
    
    /**
     * Clear basic details
     */
    fun clearBasicDetails() {
        sharedPreferences.edit()
            .remove(KEY_BASIC_DETAILS_NAME)
            .remove(KEY_BASIC_DETAILS_EMAIL)
            .apply()
    }

    /**
     * Set flag indicating we just navigated back from credit card screen
     */
    fun setJustNavigatedBackFromCreditCard(navigatedBack: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_JUST_NAVIGATED_BACK_FROM_CREDIT_CARD, navigatedBack)
            .apply()
    }

    /**
     * Get flag indicating we just navigated back from credit card screen
     */
    fun getJustNavigatedBackFromCreditCard(): Boolean {
        return sharedPreferences.getBoolean(KEY_JUST_NAVIGATED_BACK_FROM_CREDIT_CARD, false)
    }

    /**
     * Set flag indicating profile was updated from AccountSettings
     * Dashboard will check this flag and refresh profile if needed
     */
    fun setProfileUpdatedFromAccountSettings(updated: Boolean) {
        Timber.d("UserStateManager: setProfileUpdatedFromAccountSettings($updated)")
        sharedPreferences.edit()
            .putBoolean(KEY_PROFILE_UPDATED_FROM_ACCOUNT_SETTINGS, updated)
            .apply()
        Timber.d("UserStateManager: Flag saved to SharedPreferences")
    }

    /**
     * Get flag indicating profile was updated from AccountSettings
     */
    fun getProfileUpdatedFromAccountSettings(): Boolean {
        val value = sharedPreferences.getBoolean(KEY_PROFILE_UPDATED_FROM_ACCOUNT_SETTINGS, false)
        Timber.d("UserStateManager: getProfileUpdatedFromAccountSettings() = $value")
        return value
    }

    /**
     * Save user profile data for offline access
     */
    fun saveUserProfile(profile: UserProfile) {
        val gson = Gson()
        val profileJson = gson.toJson(profile)
        sharedPreferences.edit()
            .putString(KEY_USER_PROFILE_DATA, profileJson)
            .apply()
    }

    /**
     * Get cached user profile data
     */
    fun getCachedUserProfile(): UserProfile? {
        val profileJson = sharedPreferences.getString(KEY_USER_PROFILE_DATA, null)
        return if (profileJson != null) {
            try {
                val gson = Gson()
                gson.fromJson(profileJson, UserProfile::class.java)
            } catch (e: Exception) {
                null // Return null if deserialization fails
            }
        } else {
            null
        }
    }

    /**
     * Clear cached user profile data
     */
    fun clearUserProfileCache() {
        sharedPreferences.edit()
            .remove(KEY_USER_PROFILE_DATA)
            .apply()
    }
    
    /**
     * Clear all user state data
     */
    fun clearUserState() {
        sharedPreferences.edit()
            .remove(KEY_IS_PROFILE_COMPLETED)
            .remove(KEY_HAS_DRIVER_REGISTRATION)
            .remove(KEY_IS_DRIVER_REGISTRATION_COMPLETED)
            .remove(KEY_USER_ID)
            .remove(KEY_PHONE_NUMBER)
            .remove(KEY_ACCOUNT_ID)
            .remove(KEY_NEXT_STEP)
            .remove(KEY_BASIC_DETAILS_NAME)
            .remove(KEY_BASIC_DETAILS_EMAIL)
            .remove(KEY_USER_PROFILE_DATA)
            .apply()
    }
}
