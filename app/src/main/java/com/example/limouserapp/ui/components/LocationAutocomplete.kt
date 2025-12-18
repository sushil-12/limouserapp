package com.example.limouserapp.ui.components

import androidx.compose.animation.*
import androidx.compose.ui.zIndex
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.PlacePrediction
import com.example.limouserapp.data.PlacesService
import com.example.limouserapp.ui.theme.GoogleSansFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import com.example.limouserapp.ui.components.ShimmerListItem

@Composable
fun LocationAutocomplete(
    value: String,
    onValueChange: (String) -> Unit,
    onLocationSelected: (String, String, String, String, String, Double?, Double?, String?) -> Unit, // (fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country)
    modifier: Modifier = Modifier,
    placeholder: String = "Search your location",
    error: String? = null,
    onErrorCleared: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val placesService = remember(isPreview) { 
        if (isPreview) null else try { PlacesService(context) } catch (e: Exception) { 
            Log.e("LocationAutocomplete", "Failed to initialize PlacesService", e)
            null 
        }
    }
    val coroutineScope = rememberCoroutineScope()

    var predictions by remember { mutableStateOf(emptyList<PlacePrediction>()) }
    var isLoading by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }
    var suppressSearch by remember { mutableStateOf(false) }
    var hasUserInteracted by remember { mutableStateOf(false) }
    var initialValue by remember { mutableStateOf(value) }
    var currentValue by remember { mutableStateOf(value) }

    // Update currentValue when value prop changes (e.g., from profile data)
    LaunchedEffect(value) {
        if (value != currentValue) {
            currentValue = value
            initialValue = value
            // Reset user interaction flag if value is set externally
            if (value.isNotEmpty() && !hasUserInteracted) {
                hasUserInteracted = false
            }
        }
    }

    // 🔍 Debounced search (disabled temporarily after selection)
    LaunchedEffect(currentValue, placesService, suppressSearch) {
        if (suppressSearch) return@LaunchedEffect
        
        // Don't show suggestions on initial load - only after user interaction
        if (!hasUserInteracted && currentValue == initialValue) {
            showSuggestions = false
            return@LaunchedEffect
        }
        
        if (currentValue.length >= 2) {
            if (placesService != null) {
                delay(500) // Increased delay to reduce API calls
                isLoading = true
                try {
                    Log.d("LocationAutocomplete", "Searching for: $currentValue")
                    predictions = placesService.getPlacePredictions(currentValue)
                    Log.d("LocationAutocomplete", "Found ${predictions.size} predictions")
                    showSuggestions = predictions.isNotEmpty()
                } catch (e: Exception) {
                    Log.e("LocationAutocomplete", "Error getting predictions", e)
                    e.printStackTrace()
                    predictions = emptyList()
                    showSuggestions = false
                }
                isLoading = false
            } else {
                // Fallback: Create a simple prediction from the input
                Log.d("LocationAutocomplete", "Using fallback for: $currentValue")
                predictions = listOf(
                    PlacePrediction(
                        placeId = "fallback_${currentValue.hashCode()}",
                        primaryText = currentValue,
                        secondaryText = "Enter manually",
                        fullText = currentValue
                    )
                )
                showSuggestions = true
            }
        } else {
            predictions = emptyList()
            showSuggestions = false
        }
    }

    Column(modifier = modifier.zIndex(1f)) {
        OutlinedTextField(
            value = currentValue,
            onValueChange = {
                hasUserInteracted = true
                currentValue = it
                onValueChange(it)
                onErrorCleared?.invoke()
                suppressSearch = false
                if (it.isEmpty()) showSuggestions = false
            },
            placeholder = {
                // Ensure placeholder text is vertically centered and uses light grey color
                Box(
                    modifier = Modifier.fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = placeholder,
                        style = TextStyle(
                            fontFamily = GoogleSansFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = Color.Gray // Light grey color for placeholder
                        )
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(8.dp),
            textStyle = TextStyle(
                fontFamily = GoogleSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = Color(0xFF121212)
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (error != null) Color.Red else Color(0xFFE0E0E0),
                unfocusedBorderColor = if (error != null) Color.Red else Color(0xFFE0E0E0),
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
                cursorColor = Color(0xFF121212)
            )
        )
        // Error message (moved outside the OutlinedTextField to be below it)
        error?.let { errorMessage ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                fontFamily = GoogleSansFamily, // Changed to GoogleSansFamily for consistency
                modifier = Modifier.padding(start = 16.dp) // Add padding to align with the text field's content
            )
        }

        AnimatedVisibility(
            visible = showSuggestions,
            enter = fadeIn(tween(200)) + slideInVertically(
                initialOffsetY = { -it / 4 },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(150)) + slideOutVertically(
                targetOffsetY = { -it / 4 },
                animationSpec = tween(150, easing = FastOutSlowInEasing)
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp) // Adjusted padding to reduce the gap
                    .clip(RoundedCornerShape(8.dp)) // Changed to 8.dp for consistency
                    .zIndex(10f),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                when {
                    isLoading -> LoadingIndicator()
                    predictions.isNotEmpty() -> SuggestionList(predictions) { prediction ->
                        suppressSearch = true
                        onValueChange(prediction.fullText)
                        
                        // Fetch full place details asynchronously
                        coroutineScope.launch {
                            if (placesService != null && !prediction.placeId.startsWith("fallback_")) {
                                try {
                                    Log.d("LocationAutocomplete", "Fetching place details for placeId: ${prediction.placeId}")
                                    val placeDetails = placesService.getPlaceDetails(prediction.placeId)
                                    if (placeDetails != null) {
                                        // Use full address from PlaceDetails, fallback to fullText if not available
                                        val fullAddress = placeDetails.address.ifEmpty { prediction.fullText }
                                        val city = placeDetails.city
                                        val state = placeDetails.state
                                        val postalCode = placeDetails.postalCode
                                        val locationDisplay = prediction.fullText // Display text for the input field
                                        val latitude = placeDetails.latitude
                                        val longitude = placeDetails.longitude
                                        val country = placeDetails.country
                                        
                                        Log.d("LocationAutocomplete", "Place details fetched - FullAddress: $fullAddress, City: $city, State: $state, PostalCode: $postalCode, Country: $country, Lat: $latitude, Lng: $longitude")
                                        
                                        onLocationSelected(fullAddress, city, state, postalCode, locationDisplay, latitude, longitude, country)
                                    } else {
                                        Log.w("LocationAutocomplete", "Place details fetch returned null for placeId: ${prediction.placeId}")
                                        // Fallback if place details fetch fails
                                        onLocationSelected(prediction.fullText, "", "", "", prediction.fullText, null, null, null)
                                    }
                                } catch (e: Exception) {
                                    Log.e("LocationAutocomplete", "Error fetching place details", e)
                                    e.printStackTrace()
                                    // Fallback on error
                                    onLocationSelected(prediction.fullText, "", "", "", prediction.fullText, null, null, null)
                                }
                            } else {
                                Log.d("LocationAutocomplete", "Using fallback (no PlacesService or fallback prediction)")
                                // For fallback predictions, just use the text as entered
                                onLocationSelected(prediction.fullText, "", "", "", prediction.fullText, null, null, null)
                            }
                        }
                        
                        showSuggestions = false
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingIndicator() {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
    ) {
        items(3) {
            ShimmerListItem()
            if (it < 2) {
                HorizontalDivider(
                    color = Color(0xFF121212).copy(alpha = 0.08f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SuggestionList(
    predictions: List<PlacePrediction>,
    onItemClick: (PlacePrediction) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
    ) {
        itemsIndexed(predictions) { index, prediction ->
            SuggestionItem(prediction, onClick = { onItemClick(prediction) })
            if (index < predictions.size - 1) {
                HorizontalDivider(
                    color = Color(0xFF121212).copy(alpha = 0.08f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SuggestionItem(
    prediction: PlacePrediction,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = prediction.primaryText,
                style = TextStyle(
                    fontFamily = GoogleSansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = Color(0xFF121212)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (prediction.secondaryText.isNotBlank()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = prediction.secondaryText,
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = Color(0xFF121212).copy(alpha = 0.6f)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
