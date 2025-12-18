package com.example.limouserapp.ui.booking

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.painterResource
import com.example.limouserapp.R
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.input.KeyboardType
import java.text.SimpleDateFormat
import java.util.*
import java.util.TimeZone
import android.util.Log
import androidx.compose.foundation.text.ClickableText
import kotlin.math.*
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoWhite
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.theme.AppDimensions
import com.example.limouserapp.ui.utils.DebugTags
import com.example.limouserapp.ui.booking.components.CancellationPolicyWebView
import com.example.limouserapp.ui.booking.components.DatePickerDialog
import com.example.limouserapp.ui.booking.components.TimePickerDialog
import com.example.limouserapp.data.service.DirectionsService
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Tab button for pickup/return selection
 */
@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) LimoOrange else Color.White,
            contentColor = if (isSelected) Color.White else LimoBlack
        ),
        shape = RoundedCornerShape(8.dp),
        border = if (!isSelected) {
            BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
        } else null
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = GoogleSansFamily
            )
        )
    }
}

/**
 * Time Selection Screen - Main composable
 */
@Composable
fun TimeSelectionScreen(
    rideData: RideData,
    onDismiss: () -> Unit,
    onNavigateToPaxLuggageVehicle: (RideData) -> Unit,
    directionsService: DirectionsService,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCancellationPolicy by remember { mutableStateOf(false) }

    // Check if it's a round trip
    val isRoundTrip = remember(rideData.serviceType) {
        rideData.serviceType.equals("round_trip", ignoreCase = true)
    }

    // Tab selection for round trip (0 = Pickup at, 1 = Return at)
    var selectedTab by remember { mutableStateOf(0) }

    // Parse initial date and time from rideData
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Log.d(DebugTags.BookingProcess, "Open TimeSelection with initial date=${rideData.pickupDate} time=${rideData.pickupTime}")

    // Pickup date and time
    var pickupDate by remember {
        val date = try {
            dateFormatter.parse(rideData.pickupDate) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        mutableStateOf(date)
    }

    var pickupTime by remember {
        val time = try {
            timeFormatter.parse(rideData.pickupTime) ?: Date()
        } catch (e: Exception) {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 15)
            }.time
        }
        mutableStateOf(time)
    }

    // Track if return time has been manually set by user
    var isReturnTimeManuallySet by remember {
        mutableStateOf(isRoundTrip && rideData.returnPickupTime != null)
    }

    // Store calculated outbound dropoff time for return calculation
    var outboundDropoffTime by remember { mutableStateOf<Date?>(null) }
    var outboundDurationSeconds by remember { mutableStateOf(0) }

    // Return date and time (for round trip)
    var returnDate by remember {
        val date = if (isRoundTrip && rideData.returnPickupDate != null) {
            try {
                dateFormatter.parse(rideData.returnPickupDate) ?: Date()
            } catch (e: Exception) {
                Date()
            }
        } else {
            Date()
        }
        mutableStateOf(date)
    }

    var returnTime by remember {
        val time = if (isRoundTrip && rideData.returnPickupTime != null) {
            try {
                timeFormatter.parse(rideData.returnPickupTime) ?: Date()
            } catch (e: Exception) {
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 15)
                }.time
            }
        } else {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 15)
            }.time
        }
        mutableStateOf(time)
    }

    // Track last calculated values to prevent recalculation loops
    var lastCalculatedPickupTime by remember { mutableStateOf<Long?>(null) }
    var lastCalculatedDropoffTime by remember { mutableStateOf<Long?>(null) }

    // Current selected date and time based on active tab
    val selectedDate = if (selectedTab == 0) pickupDate else returnDate
    val selectedTime = if (selectedTab == 0) pickupTime else returnTime

    // Update selected date/time based on active tab
    fun updateSelectedDate(date: Date) {
        if (selectedTab == 0) {
            pickupDate = date
            // Reset last calculated values so return time can be recalculated
            if (isRoundTrip && !isReturnTimeManuallySet) {
                lastCalculatedPickupTime = null
                lastCalculatedDropoffTime = null
            }
        } else {
            returnDate = date
            // Mark return time as manually set when user changes return date
            isReturnTimeManuallySet = true
            Log.d(DebugTags.BookingProcess, "✏️ Return date manually set by user: ${dateFormatter.format(date)}")
        }
    }

    fun updateSelectedTime(time: Date) {
        if (selectedTab == 0) {
            pickupTime = time
            // Reset last calculated values so return time can be recalculated
            if (isRoundTrip && !isReturnTimeManuallySet) {
                lastCalculatedPickupTime = null
                lastCalculatedDropoffTime = null
            }
        } else {
            returnTime = time
            // Mark return time as manually set when user changes it
            isReturnTimeManuallySet = true
            Log.d(DebugTags.BookingProcess, "✏️ Return time manually set by user: ${timeFormatter.format(time)}")
        }
    }

    // Calculate return time based on outbound dropoff + buffer (30 minutes default)
    fun calculateReturnTimeFromDropoff(dropoffTime: Date, bufferMinutes: Int = 30) {
        if (isRoundTrip && !isReturnTimeManuallySet) {
            // Calculate return time: dropoff time + buffer
            val returnCalendar = Calendar.getInstance()
            returnCalendar.time = dropoffTime
            returnCalendar.add(Calendar.MINUTE, bufferMinutes)
            
            returnDate = returnCalendar.time
            returnTime = returnCalendar.time
            
            Log.d(DebugTags.BookingProcess, "🔄 Auto-calculated return time: ${dateFormatter.format(returnDate)} ${timeFormatter.format(returnTime)} (dropoff + $bufferMinutes min buffer)")
        }
    }

    // Recalculate return time when pickup time or outbound duration changes (only if not manually set)
    // Only recalculate if values actually changed
    LaunchedEffect(
        pickupTime.time,
        pickupDate.time,
        outboundDropoffTime?.time,
        isReturnTimeManuallySet
    ) {
        val dropoffTime = outboundDropoffTime
        if (isRoundTrip && !isReturnTimeManuallySet && dropoffTime != null) {
            val currentPickupTime = pickupTime.time
            val currentDropoffTime = dropoffTime.time
            
            // Only recalculate if pickup time or dropoff time actually changed
            if (lastCalculatedPickupTime != currentPickupTime || 
                lastCalculatedDropoffTime != currentDropoffTime) {
                calculateReturnTimeFromDropoff(dropoffTime)
                lastCalculatedPickupTime = currentPickupTime
                lastCalculatedDropoffTime = currentDropoffTime
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LimoWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        // Header
        TimeSelectionHeader(onDismiss = onDismiss)

        // Tabs for round trip (Pickup at / Return at)
        if (isRoundTrip) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pickup at tab
                TabButton(
                    text = "Pickup at",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                // Return at tab
                TabButton(
                    text = "Return at",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Date display - tappable
        TextButton(
            onClick = { showDatePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                formatDate(selectedDate),
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LimoBlack
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = Color.Gray.copy(alpha = 0.3f)
        )

        // Time display - tappable
        TextButton(
            onClick = { showTimePicker = true },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                formatTime(selectedTime),
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = LimoBlack
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = Color.Gray.copy(alpha = 0.3f)
        )

        // Top spacer for centering
        Spacer(modifier = Modifier.weight(0.1f))
        // Trip Details Section - centered
        TripDetailsSection(
            rideData = rideData,
            pickupDate = if (selectedTab == 0) pickupDate else returnDate,
            pickupTime = if (selectedTab == 0) pickupTime else returnTime,
            directionsService = directionsService,
            onOutboundDropoffCalculated = if (selectedTab == 0) { dropoffTime, durationSeconds ->
                // Store outbound dropoff time and duration for return calculation
                outboundDropoffTime = dropoffTime
                outboundDurationSeconds = durationSeconds
            } else null,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        // Bottom spacer for centering
        Spacer(modifier = Modifier.weight(0.7f))

        // Disclaimer text
        DisclaimerText(
            onCancellationPolicyClick = { showCancellationPolicy = true },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        // Next button
        Button(
            onClick = {
                // Update ride data with selected date and time
                val updatedRideData = rideData.copy(
                    pickupDate = dateFormatter.format(pickupDate),
                    pickupTime = timeFormatter.format(pickupTime),
                    returnPickupDate = if (isRoundTrip) dateFormatter.format(returnDate) else null,
                    returnPickupTime = if (isRoundTrip) timeFormatter.format(returnTime) else null
                )
                Log.d(DebugTags.BookingProcess, "Time selected pickup date=${updatedRideData.pickupDate} time=${updatedRideData.pickupTime}")
                if (isRoundTrip) {
                    Log.d(DebugTags.BookingProcess, "Return date=${updatedRideData.returnPickupDate} time=${updatedRideData.returnPickupTime}")
                }
                onNavigateToPaxLuggageVehicle(updatedRideData)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LimoOrange),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Next",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                updateSelectedDate(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            selectedTime = selectedTime,
            onTimeSelected = { time ->
                updateSelectedTime(time)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    // Cancellation Policy WebView
    if (showCancellationPolicy) {
        CancellationPolicyWebView(
            onDismiss = { showCancellationPolicy = false }
        )
    }
}

enum class TimeType {
    PICKUP_AT,
    DROPOFF_BY
}

@Composable
private fun TimeSelectionHeader(onDismiss: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LimoBlack)
            }

            Text(
                "Select Pickup Date & Time",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LimoBlack
                )
            )

            // Invisible IconButton to center title
            IconButton(onClick = {}) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Transparent)
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = Color.Gray.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun DisclaimerText(
    onCancellationPolicyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fullText = "Pick-up and drop-off times may vary due to traffic conditions in the area. Vehicles and rates are subject to availability. Mountain rates may differ, and toll charges are additional. Read our Cancellation Policy here."
    val policyText = "Cancellation Policy"

    val annotatedString = buildAnnotatedString {
        append(fullText)
        addStyle(
            style = SpanStyle(color = Color.Gray),
            start = 0,
            end = fullText.length
        )

        val policyStart = fullText.indexOf(policyText)
        if (policyStart != -1) {
            addStyle(
                style = SpanStyle(
                    color = LimoOrange,
                    textDecoration = TextDecoration.Underline
                ),
                start = policyStart,
                end = policyStart + policyText.length
            )
            addStringAnnotation(
                tag = "POLICY",
                annotation = "",
                start = policyStart,
                end = policyStart + policyText.length
            )
        }
    }

    ClickableText(
        text = annotatedString,
        onClick = { offset ->
            annotatedString.getStringAnnotations("POLICY", offset, offset)
                .firstOrNull()?.let {
                    onCancellationPolicyClick()
                }
        },
        style = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 18.sp
        ),
        modifier = modifier
    )
}

@Composable
private fun TripDetailsSection(
    rideData: RideData,
    pickupDate: Date,
    pickupTime: Date,
    directionsService: DirectionsService,
    onOutboundDropoffCalculated: ((Date, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var tripDetails by remember { mutableStateOf<TripDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    
    // Calculate trip details using DirectionsService
    LaunchedEffect(rideData.pickupLat, rideData.pickupLong, rideData.destinationLat, rideData.destinationLong, pickupDate, pickupTime) {
        if (rideData.pickupLat != null && rideData.pickupLong != null && 
            rideData.destinationLat != null && rideData.destinationLong != null) {
            isLoading = true
            coroutineScope.launch {
                try {
                    val (distanceMeters, durationSeconds) = directionsService.calculateDistance(
                        rideData.pickupLat!!, rideData.pickupLong!!,
                        rideData.destinationLat!!, rideData.destinationLong!!
                    )
                    
                    val distanceKm = distanceMeters / 1000.0
                    val durationMinutes = durationSeconds / 60
                    
                    // Combine pickup date and time properly
                    val pickupCalendar = Calendar.getInstance()
                    pickupCalendar.time = pickupDate
                    val pickupTimeCalendar = Calendar.getInstance()
                    pickupTimeCalendar.time = pickupTime
                    pickupCalendar.set(Calendar.HOUR_OF_DAY, pickupTimeCalendar.get(Calendar.HOUR_OF_DAY))
                    pickupCalendar.set(Calendar.MINUTE, pickupTimeCalendar.get(Calendar.MINUTE))
                    pickupCalendar.set(Calendar.SECOND, pickupTimeCalendar.get(Calendar.SECOND))
                    
                    // Calculate dropoff time: pickup datetime + duration
                    pickupCalendar.add(Calendar.SECOND, durationSeconds)
                    val dropoffTime = pickupCalendar.time
                    
                    // Notify parent about calculated dropoff time (for return time calculation)
                    onOutboundDropoffCalculated?.invoke(dropoffTime, durationSeconds)
                    
                    val dropoffTimeFormatter = SimpleDateFormat("h:mm a z", Locale.getDefault())
                    dropoffTimeFormatter.timeZone = TimeZone.getDefault()
                    val dropoffTimeStr = "${dropoffTimeFormatter.format(dropoffTime)} drop-off time"
                    
                    val hours = durationMinutes / 60
                    val minutes = durationMinutes % 60
                    val durationStr = if (hours > 0) {
                        "About $hours hrs ${minutes} mins trip"
                    } else {
                        "About $minutes mins trip"
                    }
                    
                    val distanceStr = "${String.format("%.1f", distanceKm)} km Estimated Distance"
                    
                    tripDetails = TripDetails(
                        dropoffTime = dropoffTimeStr,
                        duration = durationStr,
                        distance = distanceStr
                    )
                    isLoading = false
                } catch (e: Exception) {
                    Log.e(DebugTags.BookingProcess, "Error calculating trip details: ${e.message}", e)
                    // Fallback to default values
                    val calendar = Calendar.getInstance()
                    calendar.time = pickupTime
                    calendar.add(Calendar.HOUR, 2)
                    calendar.add(Calendar.MINUTE, 10)
                    val dropoffTimeFormatter = SimpleDateFormat("h:mm a z", Locale.getDefault())
                    dropoffTimeFormatter.timeZone = TimeZone.getDefault()
                    tripDetails = TripDetails(
                        dropoffTime = "${dropoffTimeFormatter.format(calendar.time)} drop-off time",
                        duration = "About 2 hrs 10 mins trip",
                        distance = "25 km Estimated Distance"
                    )
                    isLoading = false
                }
            }
        } else {
            // Default values if coordinates are not available
            val calendar = Calendar.getInstance()
            calendar.time = pickupTime
            calendar.add(Calendar.HOUR, 2)
            calendar.add(Calendar.MINUTE, 10)
            val dropoffTimeFormatter = SimpleDateFormat("h:mm a z", Locale.getDefault())
            dropoffTimeFormatter.timeZone = TimeZone.getDefault()
            tripDetails = TripDetails(
                dropoffTime = "${dropoffTimeFormatter.format(calendar.time)} drop-off time",
                duration = "About 2 hrs 10 mins trip",
                distance = "25 km Estimated Distance"
            )
            isLoading = false
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            // NEW: Nice lighter shimmer effect
            TripDetailsShimmer(
                modifier = Modifier.width(246.dp)
            )
        } else if (tripDetails != null) {
            Column(
                modifier = Modifier
                    .width(246.67.dp)
                    .wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Drop-off time - larger, more prominent
                Text(
                    text = tripDetails!!.dropoffTime,
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp,
                        color = Color(0xFF121212),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Trip duration - medium size, secondary info
                Text(
                    text = tripDetails!!.duration,
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp,
                        color = Color(0xFF121212).copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Estimated distance - same as trip duration
                Text(
                    text = tripDetails!!.distance,
                    style = TextStyle(
                        fontFamily = GoogleSansFamily,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 20.sp,
                        color = Color(0xFF121212).copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// --- NEW SHIMMER COMPONENTS ---

@Composable
fun TripDetailsShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Drop-off time placeholder (Large)
        Box(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )

        // Duration placeholder (Medium)
        Box(
            modifier = Modifier
                .height(16.dp)
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )

        // Distance placeholder (Medium)
        Box(
            modifier = Modifier
                .height(16.dp)
                .fillMaxWidth(0.6f)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
    }
}

// Lighter shimmer effect extension
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerOffset"
    )

    background(
        brush = Brush.linearGradient(
            // Much lighter gray colors for a subtle effect
            colors = listOf(
                Color(0xFFF2F2F2),
                Color(0xFFFCFCFC), // Nearly white highlight
                Color(0xFFF2F2F2),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

private data class TripDetails(
    val dropoffTime: String,
    val duration: String,
    val distance: String
)

private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371.0 // Earth's radius in km

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)

    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadius * c
}

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    return formatter.format(date)
}

private fun formatTime(date: Date): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(date)
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun TimeSelectionScreenPreview() {
    MaterialTheme {
        Text("Time Selection Screen Preview - DirectionsService required for full functionality")
    }
}