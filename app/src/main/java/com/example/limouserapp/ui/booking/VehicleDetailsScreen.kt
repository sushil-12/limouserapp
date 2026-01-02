package com.example.limouserapp.ui.booking

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.rememberAsyncImagePainter
import com.example.limouserapp.R
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.data.model.booking.VehicleDetails
import com.example.limouserapp.data.model.booking.RateBreakdown
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite
import com.example.limouserapp.ui.theme.LimouserappTheme
import com.example.limouserapp.ui.utils.DebugTags
import com.example.limouserapp.ui.booking.dialogs.SubtotalDialog
import com.example.limouserapp.ui.booking.dialogs.DriverInfoDialog
import com.example.limouserapp.ui.booking.dialogs.AmenitiesDialog
import com.example.limouserapp.ui.theme.LimoGreen

/**
 * Vehicle Details Screen
 * Matches the design in image_ebc826.png
 */
@Composable
fun VehicleDetailsScreen(
    rideData: RideData,
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onBookNow: () -> Unit
) {
    var showSubtotalDialog by remember { mutableStateOf(false) }
    var showDriverInfoDialog by remember { mutableStateOf(false) }
    var showAmenitiesDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LimoWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        // --- Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LimoBlack)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "Booking details",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LimoBlack)
            )
            Spacer(modifier = Modifier.weight(1f))
            // Invisible placeholder to balance the back button
            Spacer(modifier = Modifier.width(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            // --- Vehicle Card (Redesigned) ---
            VehicleSelectionCard(
                rideData = rideData,
                vehicle = vehicle,
                onViewSubtotals = { showSubtotalDialog = true },
                onDriverInfo = { showDriverInfoDialog = true },
                onAmenities = { showAmenitiesDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Pickup Details Card ---
            PickupDetailsCard(rideData = rideData)

            Spacer(modifier = Modifier.height(24.dp))

            // --- Map Section ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                RouteMapView(
                    rideData = rideData,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // --- Next Button (Sticky Bottom) ---
        // Matches the "Next" text from your screenshot provided in this prompt
        Button(
            onClick = {
                Log.d(DebugTags.BookingProcess, "Book Now clicked for vehicle: ${vehicle.name}")
                onBookNow()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height(52.dp), // Slightly taller for modern look
            colors = ButtonDefaults.buttonColors(
                containerColor = LimoOrange
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                "Book Now",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            )
        }
    }

    // --- Dialogs (Functionality Preserved) ---
    if (showSubtotalDialog) {
        SubtotalDialog(
            vehicle = vehicle,
            serviceType = rideData.serviceType,
            onDismiss = { showSubtotalDialog = false }
        )
    }

    if (showDriverInfoDialog) {
        DriverInfoDialog(
            vehicle = vehicle,
            onDismiss = { showDriverInfoDialog = false }
        )
    }

    if (showAmenitiesDialog) {
        AmenitiesDialog(
            vehicle = vehicle,
            onDismiss = { showAmenitiesDialog = false }
        )
    }
}

@Composable
private fun VehicleSelectionCard(
    rideData: RideData,
    vehicle: Vehicle,
    onViewSubtotals: () -> Unit,
    onDriverInfo: () -> Unit,
    onAmenities: () -> Unit
) {
    // Determine active service type for checkbox logic
    val isRoundTrip = rideData.serviceType.equals("round_trip", ignoreCase = true)

    // We try to fetch prices for both to display them like the screenshot
    // If one is missing, we only show the active one.
    val priceOneWay = vehicle.rateBreakdownOneWay?.grandTotal
    val priceRoundTrip = vehicle.rateBreakdownRoundTrip?.grandTotal

    // Fallback if breakdown is null but current price exists
    val currentPrice = vehicle.getPrice(rideData.serviceType)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        // Thick black border matching the screenshot
        border = androidx.compose.foundation.BorderStroke(2.dp, LimoBlack),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // -- 1. Image & Title Section --
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Image
                val vehicleImageUrl = vehicle.vehicleImages?.firstOrNull() ?: vehicle.image
                if (!vehicleImageUrl.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(vehicleImageUrl),
                        contentDescription = vehicle.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp) // Taller image area
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        Text("Vehicle Image Unavailable", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name
                Text(
                    vehicle.name,
                    style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LimoBlack)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tags (Gray Background)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    vehicle.vehicleDetails?.make?.takeIf { it.isNotEmpty() }?.let { VehicleTag(it) }
                    vehicle.vehicleDetails?.model?.takeIf { it.isNotEmpty() }?.let { VehicleTag(it) }
                    vehicle.vehicleDetails?.year?.takeIf { it.isNotEmpty() }?.let { VehicleTag(it) }
                }
            }

            // Divider
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // -- 2. Pricing & Pax Section --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left Column: Service Type Checkboxes & Prices
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // One Way Row
                    if (priceOneWay != null || !isRoundTrip) {
                        ServiceTypeRow(
                            label = "One way",
                            price = priceOneWay ?: currentPrice,
                            isChecked = !isRoundTrip
                        )
                    }

                    // Round Trip Row
//                    if (priceRoundTrip != null || isRoundTrip) {
//                        ServiceTypeRow(
//                            label = "Round trip",
//                            price = priceRoundTrip ?: currentPrice,
//                            isChecked = isRoundTrip
//                        )
//                    }
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        Text(
                            "All Inclusive Rates *",
                            style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                        )
                        Text(
                            "(Some Taxes, and Tolls are additional)",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Normal, color = Color.Gray)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Column: Pax & Luggage (Preserved Style)
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    VehicleSpecTag(
                        iconResId = R.drawable.passenger,
                        label = "Pax",
                        count = String.format("%02d", vehicle.getCapacity())
                    )

                    VehicleSpecTag(
                        iconResId = R.drawable.luggage,
                        label = "Lug",
                        count = String.format("%02d", vehicle.luggage ?: 0)
                    )
                }
            }

            // Disclaimer


            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // -- 3. Action Buttons --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View Subtotals (Orange) - Weight 1 to fill space
                Button(
                    onClick = onViewSubtotals,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LimoOrange),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("View Subtotals", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // Driver Info (Black)
                Button(
                    onClick = onDriverInfo,
                    modifier = Modifier
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LimoBlack),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Driver Info", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }

                // Amenities (Black)
                Button(
                    onClick = onAmenities,
                    modifier = Modifier
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LimoBlack),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("Amenities", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ServiceTypeRow(
    label: String,
    price: Double?,
    isChecked: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox Visual
        Icon(
            imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = LimoOrange,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            label,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Price Tag
        if (price != null) {
            Surface(
                color = LimoOrange,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "$${"%.2f".format(price)}",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun VehicleTag(text: String) {
    Surface(
        color = Color(0xFFF5F5F5), // Light gray like screenshot
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = LimoBlack
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun PickupDetailsCard(rideData: RideData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        // Adding a thin border to separate it cleanly on white bg
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0)),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .padding(top = 20.dp)

        ) {
            Text(
                "Pickup details",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LimoBlack),
                modifier = Modifier.padding(start = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Route Graphic with full gray background
            Surface(
                color = Color.Gray.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Line Graphic
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(LimoOrange, androidx.compose.foundation.shape.CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(LimoOrange)
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(LimoOrange, RoundedCornerShape(0.dp))
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Text
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            getDisplayLocation(rideData.pickupType, rideData.pickupLocation, rideData.selectedPickupAirport),
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                        )
                        Text(
                            getDisplayLocation(rideData.dropoffType, rideData.destinationLocation, rideData.selectedDestinationAirport),
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(12.dp))

            // Date, Time, and Trip Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 20.dp),

                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = LimoOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        formatDate(rideData.pickupDate, rideData.pickupTime),
                        style = TextStyle(fontSize = 13.sp, color = LimoBlack, fontWeight = FontWeight.Medium)
                    )
                }
                
                // Trip Type Tag
                Surface(
                    color = LimoOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(end = 20.dp)
                ) {
                    Text(
                        text = getTripTypeText(rideData),
                        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = LimoBlack),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(12.dp))

            // Passenger and Luggage Tags
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),

            ) {
                PassengerLuggageTag(
                    iconResId = R.drawable.passenger,
                    label = "No. of Pax",
                    count = String.format("%02d", rideData.noOfPassenger)
                )
                
                PassengerLuggageTag(
                    iconResId = R.drawable.luggage,
                    label = "No. of Luggage",
                    count = String.format("%02d", rideData.noOfLuggage)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF5F5F5))
            Spacer(modifier = Modifier.height(12.dp))

            // Total Travel Time
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = LimoOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Total Travel Time",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
                    )
                }
                Text(
                    text = getDynamicTravelInfo(rideData),
                    style = TextStyle(fontSize = 13.sp, color = LimoOrange, fontWeight = FontWeight.Normal),
                    modifier = Modifier.padding(start = 24.dp)
                )
            }
        }
    }
}

// --- Preserved Logic Helpers ---

/**
 * Get the correct display location based on type
 * If type is "airport", use selectedAirport, otherwise use location
 */
private fun getDisplayLocation(
    type: String,
    location: String,
    selectedAirport: String
): String {
    return if (type.equals("airport", ignoreCase = true) && selectedAirport.isNotEmpty()) {
        selectedAirport
    } else {
        location.ifEmpty { selectedAirport }
    }
}

private fun getSelectedServiceTypeName(serviceType: String): String {
    return when (serviceType.lowercase()) {
        "one_way" -> "One way"
        "round_trip" -> "Round trip"
        "charter_tour" -> "Charter Tour"
        else -> "One way"
    }
}

private fun getTripTypeText(rideData: RideData): String {
    val serviceTypeText = when (rideData.serviceType.lowercase()) {
        "one_way" -> "One Way"
        "round_trip" -> "Round Trip"
        "charter_tour" -> "Charter Tour"
        else -> "One Way"
    }
    
    val transferTypeText = when {
        rideData.pickupType.equals("city", ignoreCase = true) && 
        rideData.dropoffType.equals("city", ignoreCase = true) -> "City to City"
        rideData.pickupType.equals("city", ignoreCase = true) && 
        rideData.dropoffType.equals("airport", ignoreCase = true) -> "City to Airport"
        rideData.pickupType.equals("airport", ignoreCase = true) && 
        rideData.dropoffType.equals("city", ignoreCase = true) -> "Airport to City"
        rideData.pickupType.equals("airport", ignoreCase = true) && 
        rideData.dropoffType.equals("airport", ignoreCase = true) -> "Airport to Airport"
        rideData.pickupType.equals("city", ignoreCase = true) && 
        rideData.dropoffType.equals("cruise", ignoreCase = true) -> "City to Cruise Port"
        rideData.pickupType.equals("cruise", ignoreCase = true) && 
        rideData.dropoffType.equals("city", ignoreCase = true) -> "Cruise Port to City"
        else -> "City to City"
    }
    
    return "$serviceTypeText/ $transferTypeText"
}

private fun formatDate(dateString: String, timeString: String): String {
    return try {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        val displayDateFormat = java.text.SimpleDateFormat("EEE, MMM dd", java.util.Locale.getDefault())
        val displayTimeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())

        val date = dateFormat.parse(dateString)
        val time = timeFormat.parse(timeString)

        if (date != null && time != null) {
            "${displayDateFormat.format(date)} ${displayTimeFormat.format(time)}"
        } else {
            "$dateString $timeString"
        }
    } catch (e: Exception) {
        "$dateString $timeString"
    }
}

private fun getDynamicTravelInfo(rideData: RideData): String {
    if (rideData.serviceType.equals("charter_tour", ignoreCase = true)) {
        val hoursString = rideData.bookingHour.replace(" hours minimum", "").replace(" hours", "")
        val hours = hoursString.toIntOrNull() ?: 0
        if (hours > 0) return "$hours hours (Charter Tour)"
        return "Charter Tour"
    }

    // Use pre-calculated distance and duration from Google Maps Directions API if available
    // This avoids recalculating distance that was already calculated in TimeSelectionScreen
    if (rideData.distanceMeters != null && rideData.durationSeconds != null) {
        val distanceMiles = rideData.distanceMeters / 1609.34 // Convert meters to miles
        val durationMinutes = rideData.durationSeconds / 60
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        val distanceMilesFormatted = String.format("%.1f", distanceMiles)

        return if (hours > 0) {
            "$hours hours, $minutes minutes / $distanceMilesFormatted miles"
        } else {
            "$minutes minutes / $distanceMilesFormatted miles"
        }
    }

    // Fallback to haversine calculation only if distance wasn't pre-calculated
    val pickupLat = rideData.pickupLat
    val pickupLong = rideData.pickupLong
    val destLat = rideData.destinationLat
    val destLong = rideData.destinationLong

    if (pickupLat != null && pickupLong != null && destLat != null && destLong != null) {
        val distance = calculateDistance(pickupLat, pickupLong, destLat, destLong)
        val estimatedTimeMinutes = (distance * 1.7).toInt()
        val hours = estimatedTimeMinutes / 60
        val minutes = estimatedTimeMinutes % 60
        val distanceMiles = String.format("%.1f", distance)

        return if (hours > 0) {
            "$hours hours, $minutes minutes / $distanceMiles miles"
        } else {
            "$minutes minutes / $distanceMiles miles"
        }
    }

    return "Calculating route..."
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 3959.0

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)

    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadius * c
}

// --- Passenger/Luggage Tag Component ---
@Composable
private fun PassengerLuggageTag(
    iconResId: Int,
    label: String,
    count: String
) {
    Surface(
        color = Color.Gray.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = label,
                tint = LimoBlack,
                modifier = Modifier.size(12.dp)
            )
            Text(
                "$label $count",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = LimoBlack
                )
            )
        }
    }
}

// --- Preserved Pax/Luggage Tag (As requested) ---
@Composable
private fun VehicleSpecTag(
    iconResId: Int,
    label: String,
    count: String
) {
    Row(
        modifier = Modifier
            .background(
                LimoOrange.copy(alpha = 0.1f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .height(22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = label,
            tint = LimoOrange,
            modifier = Modifier.size(12.dp)
        )

        Text(
            text = "$label " ,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 18.sp,
                color = LimoBlack
            )
        )
        Text(
            text = "$count ",
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp,
                color = LimoBlack
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VehicleDetailsScreenPreview() {
    LimouserappTheme {
        VehicleDetailsScreen(
            rideData = RideData(
                serviceType = "one_way",
                bookingHour = "2",
                pickupType = "city",
                dropoffType = "airport",
                pickupDate = "2024-12-25",
                pickupTime = "14:30:00",
                pickupLocation = "Manhattan, New York, NY, USA",
                destinationLocation = "LGA-La Guardia, NY, USA",
                selectedPickupAirport = "",
                selectedDestinationAirport = "LGA-La Guardia",
                noOfPassenger = 4,
                noOfLuggage = 2,
                noOfVehicles = 1,
                pickupLat = 40.7128,
                pickupLong = -74.0060,
                destinationLat = 40.7769,
                destinationLong = -73.8740
            ),
            vehicle = Vehicle(
                id = 1,
                name = "Mid-Size Sedan",
                price = 438.55,
                image = null,
                vehicleImages = listOf(""), // Trigger placeholder
                capacity = 4,
                passenger = 4,
                luggage = 2,
                rateBreakdownOneWay = RateBreakdown(grandTotal = 438.55),
                rateBreakdownRoundTrip = RateBreakdown(grandTotal = 976.55),
                vehicleDetails = VehicleDetails("BMW", "1 Series M", "2019")
            ),
            onDismiss = {},
            onBookNow = {}
        )
    }
}
