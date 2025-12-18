package com.example.limouserapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.limouserapp.data.model.Country
import com.example.limouserapp.data.model.Countries
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.viewmodel.AccountSettingsViewModel
import com.example.limouserapp.ui.components.LocationAutocomplete
import com.example.limouserapp.ui.components.ShimmerBox
import com.example.limouserapp.ui.components.ShimmerText
import com.example.limouserapp.ui.components.CustomAlertDialog
import com.example.limouserapp.ui.booking.comprehensivebooking.EditableTextField
import java.text.SimpleDateFormat
import java.util.*

/**
 * Account Settings Screen
 * Matches iOS AccountSettingsView exactly
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: AccountSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var showCountrySheet by remember { mutableStateOf(false) }
    
    // Show error dialog
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // Error is displayed in UI
        }
    }
    
    // Show success message (matches iOS showSaveSuccess alert)
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.saveSuccess, uiState.saveSuccessMessage) {
        if (uiState.saveSuccess && uiState.saveSuccessMessage.isNotEmpty()) {
            showSuccessDialog = true
            viewModel.clearSaveSuccess()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Header
        HeaderView(onBackClick = onBackClick)
        
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp)
        ) {
            if (uiState.isLoading) {
                // Shimmer loading state
                AccountSettingsShimmer()
            } else {
                // First Name and Last Name Row (matches iOS - first field)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // First Name
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        EditableTextField(
                            label = "FIRST NAME *",
                            value = uiState.firstName,
                            onValueChange = viewModel::updateFirstName,
                            error = uiState.firstNameError,
                            onErrorCleared = { viewModel.clearFieldErrors() }
                        )
                    }
                    
                    // Last Name
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        EditableTextField(
                            label = "LAST NAME *",
                            value = uiState.lastName,
                            onValueChange = viewModel::updateLastName,
                            error = uiState.lastNameError,
                            onErrorCleared = { viewModel.clearFieldErrors() }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Email Field
                EditableTextField(
                    label = "EMAIL *",
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    keyboardType = KeyboardType.Email,
                    error = uiState.emailError,
                    onErrorCleared = { viewModel.clearFieldErrors() }
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Mobile Number Field - Fixed design to match other fields
                Column {
                    Text(
                        "MOBILE *",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = GoogleSansFamily
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Country Code Dropdown (enabled) - Updated to match EditableTextField design
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .clickable { showCountrySheet = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = uiState.selectedCountry.flag,
                                    fontSize = 18.sp,
                                    color = LimoBlack
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        // Phone Number Field (read-only display) - Updated to match EditableTextField design
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = uiState.selectedCountry.code,
                                    fontSize = 16.sp,
                                    color = LimoBlack,
                                    fontFamily = GoogleSansFamily
                                )
                                Text(
                                    text = uiState.mobileNumber.ifEmpty { "" },
                                    fontSize = 16.sp,
                                    color = LimoBlack,
                                    fontFamily = GoogleSansFamily
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Address Field with Google Places Autocomplete
                Column {
                    Text(
                        "ADDRESS *",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = GoogleSansFamily
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    LocationAutocomplete(
                        value = uiState.address,
                        onValueChange = viewModel::updateAddress,
                        onLocationSelected = { fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country ->
                            viewModel.updateAddressWithLocation(
                                fullAddress = fullAddress,
                                city = city,
                                state = state,
                                zipCode = zipCode,
                                latitude = latitude,
                                longitude = longitude,
                                country = country
                            )
                        },
                        placeholder = "Enter address",
                        error = uiState.addressError,
                        onErrorCleared = { viewModel.clearFieldErrors() }
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Country Field (auto-filled from Address selection, matches iOS)
                EditableTextField(
                    label = "COUNTRY *",
                    value = uiState.country,
                    onValueChange = viewModel::updateCountry,
                    error = uiState.countryError,
                    onErrorCleared = { viewModel.clearFieldErrors() }
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // ZIP Code Field (auto-filled from Address selection, matches iOS)
                EditableTextField(
                    label = "ZIP CODE / COUNTRY ADDRESS CODE *",
                    value = uiState.zipCode,
                    onValueChange = viewModel::updateZipCode,
                    keyboardType = KeyboardType.Text,
                    error = uiState.zipCodeError,
                    onErrorCleared = { viewModel.clearFieldErrors() }
                )
                
                Spacer(modifier = Modifier.height(100.dp)) // Space for the save button
            }
        }
        
        // Save Button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            // Error message
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            Button(
                onClick = {
                    viewModel.clearError()
                    viewModel.clearFieldErrors()
                    viewModel.saveAccountSettings()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimoOrange
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = !uiState.isSaving && !uiState.isLoading
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White
                        )
                    }
                    Text(
                        text = if (uiState.isSaving) "Saving..." else "Save",
                        style = TextStyle(
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    )
                }
            }
        }
    }
    
    // Country Selection Bottom Sheet
    if (showCountrySheet) {
        CountrySelectionBottomSheet(
            selectedCountry = uiState.selectedCountry,
            onCountrySelected = { country ->
                viewModel.updateSelectedCountry(country)
                showCountrySheet = false
            },
            onDismiss = { showCountrySheet = false }
        )
    }
    
    // Success Alert Dialog (matches iOS showSaveSuccess alert)
    if (showSuccessDialog) {
        CustomAlertDialog(
            message = uiState.saveSuccessMessage,
            onDismiss = { showSuccessDialog = false }
        )
    }
}

/**
 * Header view matching MyCardsScreen style
 */
@Composable
private fun HeaderView(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            // CRITICAL FIX: Adds padding matching the system status bar height
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }

            Text(
                text = "Account Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = GoogleSansFamily,
                color = Color.Black
            )

            // Dummy spacer to center the title perfectly
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}


/**
 * Date Picker Dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    date: Date,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = date.time
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onDateSelected(Date(it))
                }
            }) {
                Text(
                    "OK",
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight.Medium,
                        color = LimoOrange
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                )
            }
        },
        text = {
            DatePicker(state = datePickerState)
        }
    )
}

/**
 * Shimmer loading state for Account Settings
 */
@Composable
private fun AccountSettingsShimmer() {
    Column {
        // First Name and Last Name Row Shimmer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First Name Shimmer
            Column(modifier = Modifier.weight(1f)) {
                ShimmerText(modifier = Modifier.fillMaxWidth(0.4f), height = 11.dp)
                Spacer(Modifier.height(6.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
            
            // Last Name Shimmer
            Column(modifier = Modifier.weight(1f)) {
                ShimmerText(modifier = Modifier.fillMaxWidth(0.4f), height = 11.dp)
                Spacer(Modifier.height(6.dp))
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Email Field Shimmer
        Column {
            ShimmerText(modifier = Modifier.fillMaxWidth(0.3f), height = 11.dp)
            Spacer(Modifier.height(6.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Mobile Number Field Shimmer
        Column {
            ShimmerText(modifier = Modifier.fillMaxWidth(0.3f), height = 11.dp)
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Country Code Dropdown Shimmer
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                
                // Phone Number Field Shimmer
                ShimmerBox(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Address Field Shimmer
        Column {
            ShimmerText(modifier = Modifier.fillMaxWidth(0.3f), height = 11.dp)
            Spacer(Modifier.height(6.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Country Field Shimmer
        Column {
            ShimmerText(modifier = Modifier.fillMaxWidth(0.3f), height = 11.dp)
            Spacer(Modifier.height(6.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // ZIP Code Field Shimmer
        Column {
            ShimmerText(modifier = Modifier.fillMaxWidth(0.6f), height = 11.dp)
            Spacer(Modifier.height(6.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(100.dp)) // Space for the save button
    }
}

/**
 * Country selection bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountrySelectionBottomSheet(
    selectedCountry: Country,
    onCountrySelected: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.7f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Select Country",
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                )
            }
            
            Divider()
            
            // Country List
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(Countries.list) { country ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCountrySelected(country)
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = country.flag,
                                fontSize = 20.sp
                            )
                            Text(
                                text = country.name,
                                style = TextStyle(
                                    fontFamily = GoogleSansFamily,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                            )
                        }
                        Text(
                            text = country.code,
                            style = TextStyle(
                                fontFamily = GoogleSansFamily,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }
    }
}

