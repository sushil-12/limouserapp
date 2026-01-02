package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.api.DashboardApi
import com.example.limouserapp.data.model.Country
import com.example.limouserapp.data.model.Countries
import com.example.limouserapp.data.model.dashboard.ProfileData
import com.example.limouserapp.data.model.dashboard.UpdateProfileRequest
import com.example.limouserapp.domain.usecase.dashboard.RefreshUserProfileUseCase
import com.example.limouserapp.data.local.UserStateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Account Settings screen
 * Manages account settings state and business logic
 */
@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val dashboardApi: DashboardApi,
    private val refreshUserProfileUseCase: RefreshUserProfileUseCase,
    private val userStateManager: UserStateManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AccountSettingsUiState())
    val uiState: StateFlow<AccountSettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadProfileData()
    }
    
    /**
     * Load profile data from API
     */
    fun loadProfileData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val response = dashboardApi.getUserProfileV1()
                if (response.success && response.data != null) {
                    populateFields(response.data.user)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message ?: "Failed to load profile data"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading profile data")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "An error occurred"
                )
            }
        }
    }
    
    /**
     * Populate form fields with profile data
     */
    private fun populateFields(user: com.example.limouserapp.data.model.dashboard.UserProfileDetails) {
        val country = if (!user.phoneIsd.isNullOrEmpty()) {
            Countries.list.find { it.code == user.phoneIsd } ?: Countries.list.first()
        } else {
            Countries.list.first() // Default to United States
        }
        
        // Parse latitude and longitude
        val latitude = user.latitude?.toDoubleOrNull()
        val longitude = user.longitude?.toDoubleOrNull()
        
        // Parse and format date of birth (handle "0000-00-00" and invalid dates)
        val dateOfBirth = if (!user.dob.isNullOrEmpty() && user.dob != "0000-00-00") {
            // Try to parse and reformat to ensure it's in correct format
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val parsedDate = dateFormat.parse(user.dob)
                if (parsedDate != null) {
                    dateFormat.format(parsedDate)
                } else {
                    user.dob
                }
            } catch (e: Exception) {
                // If parsing fails, use original value
                user.dob
            }
        } else {
            ""
        }
        
        _uiState.value = _uiState.value.copy(
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email ?: "",
            mobileNumber = user.phone ?: "",
            selectedCountry = country,
            address = user.address ?: "",
            city = user.city ?: "",
            state = user.state ?: "",
            country = user.country ?: "",
            zipCode = user.zip ?: "",
            latitude = latitude,
            longitude = longitude,
            gender = user.gender ?: "",
            dateOfBirth = dateOfBirth
        )
    }
    
    /**
     * Update first name
     */
    fun updateFirstName(firstName: String) {
        _uiState.value = _uiState.value.copy(firstName = firstName)
    }
    
    /**
     * Update last name
     */
    fun updateLastName(lastName: String) {
        _uiState.value = _uiState.value.copy(lastName = lastName)
    }
    
    /**
     * Update email
     */
    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }
    
    /**
     * Update mobile number
     */
    fun updateMobileNumber(mobileNumber: String) {
        _uiState.value = _uiState.value.copy(mobileNumber = mobileNumber)
    }
    
    /**
     * Update selected country for phone
     */
    fun updateSelectedCountry(country: Country) {
        _uiState.value = _uiState.value.copy(selectedCountry = country)
    }
    
    /**
     * Update address
     */
    fun updateAddress(address: String) {
        _uiState.value = _uiState.value.copy(address = address)
    }
    
    /**
     * Update country display field
     */
    fun updateCountry(country: String) {
        _uiState.value = _uiState.value.copy(country = country)
    }
    
    /**
     * Update zip code
     */
    fun updateZipCode(zipCode: String) {
        _uiState.value = _uiState.value.copy(zipCode = zipCode)
    }
    
    /**
     * Update city
     */
    fun updateCity(city: String) {
        _uiState.value = _uiState.value.copy(city = city)
    }
    
    /**
     * Update state
     */
    fun updateState(state: String) {
        _uiState.value = _uiState.value.copy(state = state)
    }
    
    /**
     * Update gender
     */
    fun updateGender(gender: String) {
        _uiState.value = _uiState.value.copy(gender = gender)
    }
    
    /**
     * Update date of birth from string
     */
    fun updateDateOfBirth(dateOfBirth: String) {
        _uiState.value = _uiState.value.copy(dateOfBirth = dateOfBirth)
    }
    
    /**
     * Update date of birth from Date object
     * Formats the date as "yyyy-MM-dd" for API
     */
    fun updateDateOfBirthFromDate(date: Date) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formattedDate = dateFormat.format(date)
        _uiState.value = _uiState.value.copy(dateOfBirth = formattedDate)
    }
    
    /**
     * Update address with location details from Google Places
     */
    fun updateAddressWithLocation(
        fullAddress: String,
        city: String,
        state: String,
        zipCode: String,
        latitude: Double?,
        longitude: Double?,
        country: String?
    ) {
        _uiState.value = _uiState.value.copy(
            address = fullAddress,
            city = city,
            state = state,
            zipCode = zipCode,
            latitude = latitude,
            longitude = longitude,
            country = country ?: _uiState.value.country
        )
    }
    
    /**
     * Save account settings
     */
    fun saveAccountSettings() {
        val currentState = _uiState.value

        // Clear all field errors first
        _uiState.value = currentState.copy(
            firstNameError = null,
            lastNameError = null,
            emailError = null,
            addressError = null,
            countryError = null,
            zipCodeError = null,
            error = null
        )

        // Validation (matches iOS exactly) - set field-specific errors
        val trimmedFirstName = currentState.firstName.trim()
        val trimmedLastName = currentState.lastName.trim()
        val trimmedEmail = currentState.email.trim()
        val trimmedAddress = currentState.address.trim()
        val trimmedCountry = currentState.country.trim()
        val trimmedZip = currentState.zipCode.trim()

        var hasErrors = false

        if (trimmedFirstName.isEmpty()) {
            _uiState.value = _uiState.value.copy(firstNameError = "First name is required.")
            hasErrors = true
        }

        if (trimmedLastName.isEmpty()) {
            _uiState.value = _uiState.value.copy(lastNameError = "Last name is required.")
            hasErrors = true
        }

        if (trimmedEmail.isEmpty()) {
            _uiState.value = _uiState.value.copy(emailError = "Email is required.")
            hasErrors = true
        } else if (!isValidEmail(trimmedEmail)) {
            _uiState.value = _uiState.value.copy(emailError = "Please enter a valid email address.")
            hasErrors = true
        }

        if (trimmedAddress.isEmpty()) {
            _uiState.value = _uiState.value.copy(addressError = "Address is required.")
            hasErrors = true
        }

        if (trimmedCountry.isEmpty()) {
            _uiState.value = _uiState.value.copy(countryError = "Country is required.")
            hasErrors = true
        }

        if (trimmedZip.isEmpty()) {
            _uiState.value = _uiState.value.copy(zipCodeError = "ZIP code is required.")
            hasErrors = true
        }

        // If there are validation errors, don't proceed with API call
        if (hasErrors) {
            return
        }
        
        viewModelScope.launch {
            _uiState.value = currentState.copy(
                isSaving = true,
                error = null
            )
            
            try {
                // Resolve gender and DOB with defaults if empty (matches iOS)
                // Normalize DOB format (matches iOS normalizeDOB)
                val formattedDateOfBirth = if (currentState.dateOfBirth.isNotBlank() && currentState.dateOfBirth != "0000-00-00") {
                    try {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val parsedDate = dateFormat.parse(currentState.dateOfBirth)
                        if (parsedDate != null) {
                            dateFormat.format(parsedDate)
                        } else {
                            // Try MM/dd/yyyy format (matches iOS fallback)
                            try {
                                val fallbackFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                                val fallbackDate = fallbackFormat.parse(currentState.dateOfBirth)
                                if (fallbackDate != null) {
                                    dateFormat.format(fallbackDate)
                                } else {
                                    "1990-01-01" // Default DOB
                                }
                            } catch (e: Exception) {
                                "1990-01-01" // Default DOB
                            }
                        }
                    } catch (e: Exception) {
                        "1990-01-01" // Default DOB
                    }
                } else {
                    "1990-01-01" // Default DOB (matches iOS defaultDOB)
                }
                val resolvedGender = if (currentState.gender.isNotBlank()) {
                    currentState.gender
                } else {
                    // Use default from existing profile data or "male"
                    "male"
                }
                
                // Resolve city and state (can be empty, will use existing values if available)
                val resolvedCity = currentState.city.trim().takeIf { it.isNotEmpty() } ?: ""
                val resolvedState = currentState.state.trim().takeIf { it.isNotEmpty() } ?: ""
                
                // Resolve coordinates (matches iOS)
                val resolvedLatitude = currentState.latitude ?: 0.0
                val resolvedLongitude = currentState.longitude ?: 0.0
                
                val request = com.example.limouserapp.data.model.dashboard.UpdateUserProfileRequest(
                    firstName = trimmedFirstName,
                    lastName = trimmedLastName,
                    email = trimmedEmail,
                    gender = resolvedGender,
                    dateOfBirth = formattedDateOfBirth,
                    address = trimmedAddress,
                    city = resolvedCity,
                    state = resolvedState,
                    country = trimmedCountry,
                    zip = trimmedZip,
                    latitude = resolvedLatitude,
                    longitude = resolvedLongitude
                )
                
                val response = dashboardApi.updateUserProfileV1(request)
                
                if (response.success && response.data != null) {
                    populateFields(response.data.user)

                    // Refresh cached profile data for NavigationDrawer and other components
                    viewModelScope.launch {
                        refreshUserProfileUseCase()
                    }

                    // Set flag to notify Dashboard to refresh profile when user navigates back
                    // This is optimized: only Dashboard checks this flag, avoiding unnecessary API calls
                    userStateManager.setProfileUpdatedFromAccountSettings(true)
                    Timber.d("AccountSettings: Profile updated successfully, flag set to true")

                    // Store success message from API response (matches iOS)
                    val successMessage = response.message ?: "Profile updated successfully"
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = null,
                        saveSuccess = true,
                        saveSuccessMessage = successMessage
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = response.message ?: "Unable to update profile. Please try again."
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error saving account settings")
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = e.message ?: "An error occurred while saving"
                )
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear field-specific errors
     */
    fun clearFieldErrors() {
        _uiState.value = _uiState.value.copy(
            firstNameError = null,
            lastNameError = null,
            emailError = null,
            addressError = null,
            countryError = null,
            zipCodeError = null
        )
    }
    
    /**
     * Clear save success flag
     */
    fun clearSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
    
    /**
     * Validate email format (matches iOS isValidEmail)
     */
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$".toRegex()
        return emailRegex.matches(email)
    }
}

/**
 * UI State for Account Settings screen
 */
data class AccountSettingsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val saveSuccessMessage: String = "", // Success message from API (matches iOS)

    // Form fields
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val selectedCountry: Country = Countries.list.first(),
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val zipCode: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gender: String = "",
    val dateOfBirth: String = "",

    // Field-specific errors
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val emailError: String? = null,
    val addressError: String? = null,
    val countryError: String? = null,
    val zipCodeError: String? = null
)


