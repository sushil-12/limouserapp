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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
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
    var selectedCountry by remember {
        mutableStateOf(
            Countries.getCountryFromCode(
                uiState?.selectedCountryCode?.shortCode?.uppercase() ?: "US"
            )
        )
    }

    var showCountryPicker by remember { mutableStateOf(false) }
    val phone = remember { mutableStateOf(uiState?.phoneNumber ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = AppDimensions.MainScreenPadding)
    ) {
        Spacer(Modifier.height(AppSpacing.xxxxl))

        Text(
            text = "Hi Travel partner!",
            style = AppTextStyles.phoneEntryHeadline.copy(color = AppColors.LimoBlack)
        )
        Text(
            text = "Enter your mobile number",
            style = AppTextStyles.phoneEntryHeadline.copy(color = AppColors.LimoBlack)
        )

        Spacer(Modifier.height(AppSpacing.xl))

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
                    val cleaned = input.filter { it.isDigit() }.take(selectedCountry.phoneLength)
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
                        text = "9876543210",
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
                modifier = Modifier
                    .weight(1f),
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
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Success message display
        uiState?.message?.let {
            Text(
                text = it,
                color = Color.Green,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Next button
        Button(
            onClick = { if (uiState?.isLoading != true) onNext() },
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.LimoOrange),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .align(Alignment.End)
        ) {
            if (uiState?.isLoading == true) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = AppColors.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(1.dp))
//                Text("Sending...", color = AppColors.White)
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Continue", color = AppColors.White)
                }
            }
        }
        Spacer(Modifier.width(1.dp))

        // Country Picker Bottom Sheet
        if (showCountryPicker) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val scope = rememberCoroutineScope()

            ModalBottomSheet(
                onDismissRequest = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showCountryPicker = false
                    }
                },
                sheetState = sheetState
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
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                        )
                    }

                    Divider()

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(Countries.list) { country ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCountry = country
                                        // Convert Country to CountryCode if available
                                        val countryCode = try {
                                            CountryCode.valueOf(country.shortCode.uppercase())
                                        } catch (e: IllegalArgumentException) {
                                            CountryCode.US // Default fallback
                                        }
                                        onEvent?.invoke(
                                            com.example.limouserapp.ui.state.PhoneEntryUiEvent.CountryCodeChanged(
                                                countryCode
                                            )
                                        )
                                        scope.launch { sheetState.hide() }
                                            .invokeOnCompletion { showCountryPicker = false }
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
                                            fontSize = 16.sp,
                                            color = Color.Black
                                        )
                                    )
                                }
                                Text(
                                    text = country.code,
                                    style = TextStyle(
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
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select Country",
                tint = AppColors.LimoBlack.copy(alpha = 1f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PhoneEntryScreenPreview() {
    PhoneEntryScreen(onNext = {}, onBack = {})
}
