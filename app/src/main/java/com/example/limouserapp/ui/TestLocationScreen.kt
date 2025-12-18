package com.example.limouserapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.ui.components.LocationAutocomplete
import com.example.limouserapp.ui.theme.GoogleSansFamily
import androidx.compose.ui.graphics.Color

@Composable
fun TestLocationScreen() {
    var location by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Location Autocomplete Test",
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                color = Color(0xFF121212)
            )
        )
        
        Text(
            text = "Type at least 2 characters to see suggestions",
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = Color(0xFF121212).copy(alpha = 0.6f)
            )
        )
        
        LocationAutocomplete(
            value = location,
            onValueChange = { location = it },
            onLocationSelected = { fullAddress, selectedCity, selectedState, zipCode, displayText, latitude, longitude, country ->
                selectedLocation = fullAddress
                postalCode = zipCode
                city = selectedCity
                state = selectedState
                location = displayText
            },
            placeholder = "Enter your address"
        )
        
        if (selectedLocation.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Selected Location:",
                        style = TextStyle(
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color(0xFF121212)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = selectedLocation,
                        style = TextStyle(
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = Color(0xFF121212)
                        )
                    )
                    if (postalCode.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Postal Code: $postalCode",
                            style = TextStyle(
                                fontFamily = GoogleSansFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = Color(0xFF121212).copy(alpha = 0.6f)
                            )
                        )
                    }
                    if (city.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "City: $city",
                            style = TextStyle(
                                fontFamily = GoogleSansFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = Color(0xFF121212).copy(alpha = 0.6f)
                            )
                        )
                    }
                    if (state.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "State: $state",
                            style = TextStyle(
                                fontFamily = GoogleSansFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = Color(0xFF121212).copy(alpha = 0.6f)
                            )
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Debug Info:",
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFF121212)
            )
        )
        
        Text(
            text = "Current input: '$location'",
            style = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color(0xFF121212).copy(alpha = 0.6f)
            )
        )
    }
}
