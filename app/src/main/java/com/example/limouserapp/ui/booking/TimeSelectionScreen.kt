package com.example.limouserapp.ui.booking

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
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
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.service.DirectionsService
import com.example.limouserapp.ui.booking.components.CancellationPolicyWebView
import com.example.limouserapp.ui.booking.components.DatePickerDialog
import com.example.limouserapp.ui.booking.components.RoundTripDatePickerDialog
import com.example.limouserapp.ui.booking.components.TimePickerDialog
import com.example.limouserapp.ui.theme.*
import com.example.limouserapp.ui.utils.DebugTags
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    // State for standard pickers (One-Way)
    var showDatePicker by remember { mutableStateOf(false) }

    // State for new Round Trip picker
    var showRoundTripDatePicker by remember { mutableStateOf(false) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showCancellationPolicy by remember { mutableStateOf(false) }

    // Check if it's a round trip
    val isRoundTrip = remember(rideData.serviceType) {
        rideData.serviceType.equals("round_trip", ignoreCase = true)
    }

    // Tab selection for legacy round trip logic tracking (0 = Pickup, 1 = Return)
    // Even in the new UI, we use this to know which *time* is being edited
    var selectedTab by remember { mutableStateOf(0) }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // --- State Initialization ---

    var pickupDate by remember {
        val date = try {
            dateFormatter.parse(rideData.pickupDate) ?: Date()
        } catch (e: Exception) { Date() }
        mutableStateOf(date)
    }

    var pickupTime by remember {
        val time = try {
            timeFormatter.parse(rideData.pickupTime) ?: Date()
        } catch (e: Exception) {
            Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 15) }.time
        }
        mutableStateOf(time)
    }

    var returnDate by remember {
        val date = if (isRoundTrip && rideData.returnPickupDate != null) {
            try { dateFormatter.parse(rideData.returnPickupDate) ?: Date() } catch (e: Exception) { Date() }
        } else { Date() }
        mutableStateOf(date)
    }

    var returnTime by remember {
        val time = if (isRoundTrip && rideData.returnPickupTime != null) {
            try { timeFormatter.parse(rideData.returnPickupTime) ?: Date() } catch (e: Exception) {
                Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 15) }.time
            }
        } else {
            Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 15) }.time
        }
        mutableStateOf(time)
    }

    // Logic Variables
    var calculatedDistanceMeters by remember { mutableStateOf<Double?>(null) }
    var calculatedDurationSeconds by remember { mutableStateOf<Int?>(null) }
    var isReturnTimeManuallySet by remember { mutableStateOf(isRoundTrip && rideData.returnPickupTime != null) }
    var outboundDropoffTime by remember { mutableStateOf<Date?>(null) }

    // Determine active Date/Time for standard pickers
    val selectedDate = if (selectedTab == 0) pickupDate else returnDate
    val selectedTime = if (selectedTab == 0) pickupTime else returnTime

    // --- Helper Functions ---

    fun updateSelectedDate(date: Date) {
        if (selectedTab == 0) {
            pickupDate = date
        } else {
            returnDate = date
            isReturnTimeManuallySet = true
        }
    }

    fun updateSelectedTime(time: Date) {
        if (selectedTab == 0) {
            pickupTime = time
        } else {
            returnTime = time
            isReturnTimeManuallySet = true
        }
    }

    // Auto-calculate return time
    LaunchedEffect(pickupTime, pickupDate, outboundDropoffTime, isReturnTimeManuallySet) {
        if (isRoundTrip && !isReturnTimeManuallySet && outboundDropoffTime != null) {
            val returnCalendar = Calendar.getInstance()
            returnCalendar.time = outboundDropoffTime
            returnCalendar.add(Calendar.MINUTE, 30) // Buffer

            // Only update if dates align significantly (simple logic)
            returnDate = returnCalendar.time
            returnTime = returnCalendar.time
        }
    }

    // --- UI Structure ---

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LimoWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        // Header
        TimeSelectionHeader(onDismiss = onDismiss)

        if (isRoundTrip) {
            // =========================================================
            // MODERN ROUND TRIP UI
            // =========================================================

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // 1. Date Selection Row (Unified Split Container)
                SubtleSplitInputRow(
                    startLabel = "DEPART",
                    startValue = formatDate(pickupDate),
                    startIcon = Icons.Default.CalendarToday,
                    endLabel = "RETURN",
                    endValue = formatDate(returnDate),
                    endIcon = Icons.Default.Event, // Or keep CalendarToday
                    onStartClick = { showRoundTripDatePicker = true },
                    onEndClick = { showRoundTripDatePicker = true }
                )

                // 2. Time Selection Row (Unified Split Container)
                SubtleSplitInputRow(
                    startLabel = "PICKUP TIME",
                    startValue = formatTime(pickupTime),
                    startIcon = Icons.Default.Schedule,
                    endLabel = "RETURN TIME",
                    endValue = formatTime(returnTime),
                    endIcon = Icons.Default.Schedule,
                    onStartClick = {
                        selectedTab = 0
                        showTimePicker = true
                    },
                    onEndClick = {
                        selectedTab = 1
                        showTimePicker = true
                    }
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                color = Color.Gray.copy(alpha = 0.1f)
            )
        }
        else {
            // =========================================================
            // ONE WAY UI (Legacy Logic)
            // =========================================================

            // Date display - tappable
            TextButton(
                onClick = {
                    selectedTab = 0
                    showDatePicker = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    formatDate(pickupDate),
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LimoBlack),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = Color.Gray.copy(alpha = 0.3f))

            // Time display - tappable
            TextButton(
                onClick = {
                    selectedTab = 0
                    showTimePicker = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    formatTime(pickupTime),
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LimoBlack),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = Color.Gray.copy(alpha = 0.3f))
        }

        // --- Shared Footer Section ---

        Spacer(modifier = Modifier.weight(0.1f))

        // Trip Details (Rates/Distance)
        TripDetailsSection(
            rideData = rideData,
            pickupDate = if (selectedTab == 0) pickupDate else returnDate,
            pickupTime = if (selectedTab == 0) pickupTime else returnTime,
            directionsService = directionsService,
            onOutboundDropoffCalculated = { dropoffTime, duration ->
                if (selectedTab == 0) { // Only update based on outbound leg
                    outboundDropoffTime = dropoffTime
                    calculatedDurationSeconds = duration
                }
            },
            onDistanceCalculated = { distance, _ ->
                if (selectedTab == 0) calculatedDistanceMeters = distance
            },
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.weight(0.7f))

        DisclaimerText(
            onCancellationPolicyClick = { showCancellationPolicy = true },
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )

        Button(
            onClick = {
                val updatedRideData = rideData.copy(
                    pickupDate = dateFormatter.format(pickupDate),
                    pickupTime = timeFormatter.format(pickupTime),
                    returnPickupDate = if (isRoundTrip) dateFormatter.format(returnDate) else null,
                    returnPickupTime = if (isRoundTrip) timeFormatter.format(returnTime) else null,
                    distanceMeters = calculatedDistanceMeters,
                    durationSeconds = calculatedDurationSeconds
                )
                onNavigateToPaxLuggageVehicle(updatedRideData)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LimoOrange),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Next", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    // --- DIALOGS ---

    // 1. New Round Trip Picker (Unified)
    if (showRoundTripDatePicker) {
        RoundTripDatePickerDialog(
            initialStartDate = pickupDate,
            initialEndDate = returnDate,
            onDismiss = { showRoundTripDatePicker = false },
            onDateRangeSelected = { start, end ->
                pickupDate = start
                returnDate = end
                isReturnTimeManuallySet = true
                showRoundTripDatePicker = false
            }
        )
    }

    // 2. Standard Date Picker (One-Way)
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

    // 3. Time Picker (Used for both)
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

    if (showCancellationPolicy) {
        CancellationPolicyWebView(onDismiss = { showCancellationPolicy = false })
    }
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
                "Select Date & Time",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LimoBlack
                )
            )

            // Invisible IconButton to center title
            IconButton(onClick = {}, enabled = false) {
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

// --- Trip Details Section & Shimmer ---

@Composable
fun TripDetailsSection(
    rideData: RideData,
    pickupDate: Date,
    pickupTime: Date,
    directionsService: DirectionsService,
    onOutboundDropoffCalculated: ((Date, Int) -> Unit)? = null,
    onDistanceCalculated: ((Double, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var tripDetails by remember { mutableStateOf<TripDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

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

                    onDistanceCalculated?.invoke(distanceMeters.toDouble(), durationSeconds)

                    val distanceKm = distanceMeters / 1000.0
                    val durationMinutes = durationSeconds / 60

                    val pickupCalendar = Calendar.getInstance()
                    pickupCalendar.time = pickupDate
                    val pickupTimeCalendar = Calendar.getInstance()
                    pickupTimeCalendar.time = pickupTime
                    pickupCalendar.set(Calendar.HOUR_OF_DAY, pickupTimeCalendar.get(Calendar.HOUR_OF_DAY))
                    pickupCalendar.set(Calendar.MINUTE, pickupTimeCalendar.get(Calendar.MINUTE))
                    pickupCalendar.set(Calendar.SECOND, pickupTimeCalendar.get(Calendar.SECOND))

                    pickupCalendar.add(Calendar.SECOND, durationSeconds)
                    val dropoffTime = pickupCalendar.time

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

@Composable
fun TripDetailsShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )

        Box(
            modifier = Modifier
                .height(16.dp)
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )

        Box(
            modifier = Modifier
                .height(16.dp)
                .fillMaxWidth(0.6f)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
    }
}

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
            colors = listOf(
                Color(0xFFF2F2F2),
                Color(0xFFFCFCFC),
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

// --- Format Utilities ---

private fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    return formatter.format(date)
}

private fun formatTime(date: Date): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(date)
}

/**
 * Tab button for pickup/return selection (Used only for legacy One-Way views)
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

@Composable
fun SubtleSplitInputRow(
    startLabel: String,
    startValue: String,
    startIcon: androidx.compose.ui.graphics.vector.ImageVector,
    endLabel: String,
    endValue: String,
    endIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onStartClick: () -> Unit,
    onEndClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF9F9F9), // Very light gray fill
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)), // Subtle outline
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min) // Important for vertical divider
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {

            // --- Left Section ---
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onStartClick)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Icon(
                    imageVector = startIcon,
                    contentDescription = null,
                    tint = LimoOrange, // or Color.Gray for even more subtlety
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                // Text Data
                Column {
                    Text(
                        text = startLabel,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = startValue,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LimoBlack
                        ),
                        maxLines = 1
                    )
                }
            }

            // --- Vertical Divider ---
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp) // Leave some gap at top/bottom
                    .background(Color.Gray.copy(alpha = 0.2f))
            )

            // --- Right Section ---
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEndClick)
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Icon(
                    imageVector = endIcon,
                    contentDescription = null,
                    tint = LimoOrange,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))

                // Text Data
                Column {
                    Text(
                        text = endLabel,
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = endValue,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LimoBlack
                        ),
                        maxLines = 1
                    )
                }
            }
        }
    }
}