package com.example.limouserapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.limouserapp.data.model.Countries
import com.example.limouserapp.data.model.Country
import com.example.limouserapp.ui.booking.comprehensivebooking.EditableTextField
import com.example.limouserapp.ui.components.CustomAlertDialog
import com.example.limouserapp.ui.components.LocationAutocomplete
import com.example.limouserapp.ui.components.ShimmerBox
import com.example.limouserapp.ui.components.ShimmerText
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.viewmodel.AccountSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: AccountSettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showCountrySheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Dialog Logic
    LaunchedEffect(uiState.saveSuccess, uiState.saveSuccessMessage) {
        if (uiState.saveSuccess && uiState.saveSuccessMessage.isNotEmpty()) {
            showSuccessDialog = true
            viewModel.clearSaveSuccess()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = Color.White,// Handles status bar & nav bar
        topBar = {
            HeaderView(onBackClick = onBackClick)
        },
        bottomBar = {
            // FIXED: Proper Bottom Action Bar container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(), // Moves up with keyboard
                color = Color.White,
                shadowElevation = 8.dp, // Subtle shadow for separation
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .padding(bottom = 16.dp) // Extra bottom padding for safety
                ) {
                    // Error message above button if needed
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.clearError()
                            viewModel.clearFieldErrors()
                            viewModel.saveAccountSettings()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimoOrange,
                            disabledContainerColor = LimoOrange.copy(alpha = 0.5f)
                        ),
                        enabled = !uiState.isSaving && !uiState.isLoading
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
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
        }
    ) { paddingValues ->
        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Respect Scaffold padding
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 20.dp)
        ) {
            if (uiState.isLoading) {
                AccountSettingsShimmer()
            } else {
                // First Name and Last Name Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        EditableTextField(
                            label = "FIRST NAME *",
                            value = uiState.firstName,
                            onValueChange = viewModel::updateFirstName,
                            error = uiState.firstNameError,
                            onErrorCleared = { viewModel.clearFieldErrors() }
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
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

                // Email
                EditableTextField(
                    label = "EMAIL *",
                    value = uiState.email,
                    onValueChange = viewModel::updateEmail,
                    keyboardType = KeyboardType.Email,
                    error = uiState.emailError,
                    onErrorCleared = { viewModel.clearFieldErrors() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Mobile Number
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
                        // Country Code Dropdown
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
                                Text(text = uiState.selectedCountry.flag, fontSize = 18.sp)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Read-only Phone Number
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
                                    text = uiState.mobileNumber,
                                    fontSize = 16.sp,
                                    color = LimoBlack,
                                    fontFamily = GoogleSansFamily
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Address (Google Autocomplete)
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
                        onLocationSelected = { fullAddress, city, state, zipCode, _, latitude, longitude, country ->
                            viewModel.updateAddressWithLocation(
                                fullAddress, city, state, zipCode, latitude, longitude, country
                            )
                        },
                        placeholder = "Enter address",
                        error = uiState.addressError,
                        onErrorCleared = { viewModel.clearFieldErrors() }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Country
                EditableTextField(
                    label = "COUNTRY *",
                    value = uiState.country,
                    onValueChange = viewModel::updateCountry,
                    error = uiState.countryError,
                    onErrorCleared = { viewModel.clearFieldErrors() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ZIP Code
                EditableTextField(
                    label = "ZIP CODE / COUNTRY ADDRESS CODE *",
                    value = uiState.zipCode,
                    onValueChange = viewModel::updateZipCode,
                    keyboardType = KeyboardType.Text,
                    error = uiState.zipCodeError,
                    onErrorCleared = { viewModel.clearFieldErrors() }
                )
            }
        }
    }

    // Sheets & Dialogs
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

    if (showSuccessDialog) {
        CustomAlertDialog(
            message = uiState.saveSuccessMessage,
            onDismiss = { showSuccessDialog = false }
        )
    }
}

// ----------------------------------------------------------------------------------
// COMPONENT: Optimized Header (Compatible with Scaffold TopBar)
// ----------------------------------------------------------------------------------
@Composable
private fun HeaderView(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
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

            // Balance the layout
            Spacer(modifier = Modifier.width(48.dp))
        }
        Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
    }
}

// ----------------------------------------------------------------------------------
// COMPONENT: Country Selection Bottom Sheet
// ----------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountrySelectionBottomSheet(
    selectedCountry: Country,
    onCountrySelected: (Country) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.7f),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
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

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(Countries.list) { country ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCountrySelected(country) }
                            .padding(16.dp), // Increased tap target
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = country.flag, fontSize = 20.sp)
                            Text(
                                text = country.name,
                                style = TextStyle(fontFamily = GoogleSansFamily, fontSize = 16.sp, color = Color.Black)
                            )
                        }
                        Text(
                            text = country.code,
                            style = TextStyle(fontFamily = GoogleSansFamily, fontSize = 16.sp, color = Color.Gray)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------
// COMPONENT: Shimmer Loading State
// ----------------------------------------------------------------------------------
@Composable
private fun AccountSettingsShimmer() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                ShimmerText(modifier = Modifier.fillMaxWidth(0.4f), height = 11.dp)
                Spacer(Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                ShimmerText(modifier = Modifier.fillMaxWidth(0.4f), height = 11.dp)
                Spacer(Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        repeat(5) {
            Column {
                ShimmerText(modifier = Modifier.fillMaxWidth(0.3f), height = 11.dp)
                Spacer(Modifier.height(6.dp))
                ShimmerBox(modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(8.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}