package com.example.limouserapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.limouserapp.R
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.model.Countries
import com.example.limouserapp.data.model.Country
import com.example.limouserapp.domain.validation.CountryCode
import com.example.limouserapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneEntryScreen(
    onNext: () -> Unit,
    onBack: (() -> Unit)? = null,
    uiState: com.example.limouserapp.ui.state.PhoneEntryUiState? = null,
    onEvent: ((com.example.limouserapp.ui.state.PhoneEntryUiEvent) -> Unit)? = null
) {
    // Derive selectedCountry from uiState with proper remember tracking
    // This ensures UI recomposes when the selectedCountryCode changes
    val selectedCountry = remember(uiState?.selectedCountryCode?.shortCode) {
        Countries.getCountryFromCode(
            uiState?.selectedCountryCode?.shortCode?.uppercase() ?: "US"
        )
    }

    var showCountryPicker by remember { mutableStateOf(false) }
    val phone = remember { mutableStateOf(uiState?.phoneNumber ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(AppSpacing.xxxxl))

        Text(
            text = "Hi Travel partner!",
            style = AppTextStyles.phoneEntryHeadline.copy(color = AppColors.LimoBlack)
        )
        Spacer(Modifier.height(AppSpacing.sm))
        Text(
            text = "Enter your mobile number",
            style = AppTextStyles.phoneEntryHeadline.copy(color = AppColors.LimoBlack, fontSize = 25.sp)
        )

        Spacer(Modifier.height(AppSpacing.md))

        Text(
            text = "We'll send a verification code on this number.",
            style = AppTextStyles.phoneEntryDescription.copy(color = AppColors.LimoBlack)
        )

        Spacer(Modifier.height(AppSpacing.xxxxl))

        // Phone Input Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CountryPickerButton(
                country = selectedCountry,
                onClick = { showCountryPicker = true },
                modifier = Modifier
                    .width(AppDimensions.countryPickerWidth)
                    .height(AppDimensions.countryPickerHeight)
            )

            Spacer(Modifier.width(AppSpacing.lg))

            OutlinedTextField(
                value = phone.value,
                onValueChange = { input ->
                    // Allow up to 15 digits (reasonable max for international phone numbers)
                    // Validation will enforce country-specific length
                    val cleaned = input.filter { it.isDigit() }.take(15)
                    phone.value = cleaned
                    onEvent?.invoke(
                        com.example.limouserapp.ui.state.PhoneEntryUiEvent.PhoneNumberChanged(cleaned)
                    )
                },
                prefix = {
                    Text(
                        text = selectedCountry.code + "  ",
                        style = AppTextStyles.phoneNumberInput.copy(
                            color = AppColors.LimoBlack,
                            fontWeight = FontWeight.Normal

                        )
                    )
                },
                placeholder = {
                    Text(
                        text = "1234567890",
                        style = AppTextStyles.phoneNumberInput.copy(
                            color = AppColors.LimoBlack.copy(alpha = 0.4f)
                        )
                    )
                },
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.passkey),
                        contentDescription = "Passkey",
                        modifier = Modifier.size(18.dp),
                        tint = AppColors.LimoBlack
                    )
                },
                singleLine = true,
                textStyle = AppTextStyles.phoneNumberInput.copy(color = AppColors.LimoBlack),
                modifier = Modifier.weight(1f),
//                    .height(AppDimensions.phoneInputHeight),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.LimoBlack.copy(alpha = 0.2f),
                    unfocusedBorderColor = AppColors.LimoBlack.copy(alpha = 0.2f),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )


        }

        // Error message display
        uiState?.error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = Color(0xFFDC2626), // LimoRed - matching driver app
                style = MaterialTheme.typography.bodySmall
            )
        }

        // Success message display
        uiState?.message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = Color(0xFF059669), // LimoGreen - matching driver app
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Next button
        Button(
            onClick = { if (uiState?.isLoading != true) onNext() },
            enabled = uiState?.isLoading != true,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColors.LimoOrange,
                disabledContainerColor = AppColors.LimoOrange.copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.End)
        ) {
            Text(
                text = if (uiState?.isLoading == true) "Sending..." else "Continue",
                color = AppColors.White
            )
        }
        Spacer(Modifier.width(1.dp))

        // Country Picker Bottom Sheet
        if (showCountryPicker) {
            CountryPickerBottomSheet(
                onDismiss = { showCountryPicker = false },
                onCountrySelected = { country ->
                    val countryCode = try {
                        CountryCode.valueOf(country.shortCode.uppercase())
                    } catch (e: IllegalArgumentException) {
                        CountryCode.US
                    }
                    // Update ViewModel state - selectedCountry will be automatically updated via derivation
                    onEvent?.invoke(
                        com.example.limouserapp.ui.state.PhoneEntryUiEvent.CountryCodeChanged(
                            countryCode, 
                            country.phoneLength
                        )
                    )
                    showCountryPicker = false
                }
            )
        }
    }
}

/**
 * Country Picker Button Component
 */
@Composable
fun CountryPickerButton(
    country: Country,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = AppColors.CountryPickerBackground,
                shape = RoundedCornerShape(AppDimensions.countryPickerCornerRadius)
            )
            .clickable { onClick() }
            .padding(
                horizontal = AppDimensions.countryPickerPaddingHorizontal,
                vertical = AppDimensions.countryPickerPaddingVertical
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.countryPickerGap)
        ) {
            Text(text = country.flag, style = AppTextStyles.bodyLarge)
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Country",
                tint = AppColors.LimoBlack.copy(alpha = 1f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Extracted Bottom Sheet for cleanliness
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryPickerBottomSheet(
    onDismiss: () -> Unit,
    onCountrySelected: (Country) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            Countries.list
        } else {
            Countries.list.filter { country ->
                country.name.contains(searchQuery, ignoreCase = true) ||
                        country.code.contains(searchQuery, ignoreCase = true) ||
                        country.shortCode.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text(
                text = "Select Country",
                style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = AppColors.LimoBlack
                ),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp)
            )
            HorizontalDivider(color = AppColors.LimoBlack.copy(alpha = 0.2f))

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search country...") },
                leadingIcon = { 
                    Icon(Icons.Default.Search, contentDescription = null) 
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppColors.LimoOrange,
                    focusedTextColor = Color.Black,
                    unfocusedBorderColor = AppColors.LimoBlack.copy(alpha = 0.2f),
                    cursorColor = AppColors.LimoOrange,
                )
            )

            LazyColumn {
                items(filteredCountries) { country ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCountrySelected(country)
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = country.flag, fontSize = 24.sp)
                            Spacer(Modifier.width(16.dp))
                            Text(text = country.name, fontSize = 16.sp, color = AppColors.LimoBlack)
                        }
                        Text(text = country.code, fontSize = 16.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PhoneEntryScreenPreview() {
    PhoneEntryScreen(onNext = {}, onBack = {})
}
