package com.example.limouserapp.ui.booking.comprehensivebooking.sections

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
// import com.example.limouserapp.ui.theme.LimoWhite // Not used in text-only mode

/**
 * Booking Summary Section (Rates)
 * Simplified, clean text layout.
 */
@Composable
fun BookingSummarySection(
    selectedServiceType: String,
    selectedReturnServiceType: String?,
    subtotal: Double,
    grandTotal: Double,
    numberOfVehicles: Int,
    currencySymbol: String,
    returnSubtotal: Double = 0.0,
    returnGrandTotal: Double = 0.0
) {
    // Helper lambda to render consistent rows
    val SummaryRow = @Composable { label: String, value: String, isTotal: Boolean, valueColor: Color ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = if (isTotal) LimoBlack else Color.Gray,
                    fontSize = if (isTotal) 16.sp else 14.sp,
                    fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
                )
            )
            Text(
                text = value,
                style = TextStyle(
                    color = valueColor,
                    fontSize = if (isTotal) 16.sp else 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        // --- TRIP 1 DETAILS ---
        SummaryRow(
            "Sub Total",
            "$currencySymbol ${String.format(java.util.Locale.US, "%.2f", subtotal)}",
            false,
            LimoBlack
        )

        SummaryRow(
            "No. of Vehicles",
            "x $numberOfVehicles",
            false,
            LimoBlack
        )

        // If it is NOT a full round trip calculation, show Grand Total here.
        val isRoundTripConfig = selectedServiceType == "Round Trip" && selectedReturnServiceType == "Round Trip"

        if (!isRoundTripConfig) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            SummaryRow(
                "Total",
                "$currencySymbol ${String.format(java.util.Locale.US, "%.2f", grandTotal)}",
                true,
                LimoBlack
            )
        } else {
            // Just show the leg total without emphasis if there is more to come
            SummaryRow(
                "Leg Total",
                "$currencySymbol ${String.format(java.util.Locale.US, "%.2f", grandTotal)}",
                false,
                LimoBlack
            )
        }

        // --- TRIP 2 DETAILS (Round Trip Only) ---
        if (isRoundTripConfig) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            SummaryRow(
                "Return Sub Total",
                "$currencySymbol ${String.format(java.util.Locale.US, "%.2f", returnSubtotal)}",
                false,
                LimoBlack
            )

            SummaryRow(
                "No. of Vehicles",
                "x $numberOfVehicles",
                false,
                LimoBlack
            )

            SummaryRow(
                "Return Leg Total",
                "$currencySymbol ${String.format(java.util.Locale.US, "%.2f", returnGrandTotal)}",
                false,
                LimoBlack
            )

            // --- FINAL COMBINED TOTAL ---
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            SummaryRow(
                "Grand Total",
                "$currencySymbol ${String.format(java.util.Locale.US, "%.2f", grandTotal + returnGrandTotal)}",
                true,
                LimoOrange
            )
        }
    }
}

/**
 * Distance Information Section
 * Display format: "Total Distance: X Miles / Y Km" and "Estimated time: X hours, Y minutes"
 */
@Composable
fun DistanceInformationSection(
    outboundDistance: Pair<String, String>, // (distance, duration)
    serviceType: String,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (isLoading) {
            TextShimmerRow()
        } else {
            // Outbound Leg (or Total Trip for one-way/charter)
            TripLegRow(
                label = if (serviceType.equals("Round Trip", ignoreCase = true)) "OUTBOUND TRIP" else "TOTAL TRIP",
                distance = outboundDistance.first,
                duration = outboundDistance.second
            )
        }
    }
}

/**
 * Return Distance Information Section
 * Shows return trip distance and duration (for round trips only)
 */
@Composable
fun ReturnDistanceInformationSection(
    returnDistance: Pair<String, String>, // (distance, duration)
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        if (isLoading) {
            TextShimmerRow()
        } else {
            TripLegRow(
                label = "RETURN TRIP",
                distance = returnDistance.first,
                duration = returnDistance.second
            )
        }
    }
}

// --- Helper Components ---

@Composable
private fun TripLegRow(
    label: String,
    distance: String,
    duration: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Label (e.g., "TOTAL TRIP")
        

        // Distance Row: "Total Distance: X Miles / Y Km"
        val formattedDistance = formatDistanceWithMilesAndKm(distance)
        Text(
            text = "Total Distance: $formattedDistance",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = LimoBlack,
                lineHeight = 22.sp
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Duration Row: "Estimated time: X hours, Y minutes"
        val formattedDuration = formatDurationWithComma(duration)
        Text(
            text = "Estimated time: $formattedDuration",
            style = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = LimoBlack,
                lineHeight = 22.sp
            )
        )
    }
}

/**
 * Parse distance string (e.g., "50.1 km" or "500m") and convert to "X Miles / Y Km" format
 */
private fun formatDistanceWithMilesAndKm(distanceStr: String): String {
    return try {
        // Parse the distance string - could be "50.1 km", "500m", etc.
        val distanceRegex = Regex("""([\d.]+)\s*(km|m|miles|mi)""", RegexOption.IGNORE_CASE)
        val match = distanceRegex.find(distanceStr)
        
        if (match != null) {
            val value = match.groupValues[1].toDouble()
            val unit = match.groupValues[2].lowercase()
            
            // Convert to meters first
            val meters = when (unit) {
                "km" -> value * 1000
                "m" -> value
                "miles", "mi" -> value * 1609.34
                else -> value * 1000 // Default to km
            }
            
            // Convert to miles and kilometers
            val miles = meters / 1609.34
            val km = meters / 1000.0
            
            // Format: "X.XX Miles / Y.YY Km"
            String.format(
                java.util.Locale.US,
                "%.2f Miles / %.2f Km",
                miles,
                km
            )
        } else {
            // Fallback: return original string
            distanceStr
        }
    } catch (e: Exception) {
        // Fallback: return original string
        distanceStr
    }
}

/**
 * Parse duration string (e.g., "1 hours 8 mins" or "8 mins") and format as "X hours, Y minutes"
 */
private fun formatDurationWithComma(durationStr: String): String {
    return try {
        // Parse duration - could be "1 hours 8 mins", "8 mins", etc.
        val hoursRegex = Regex("""(\d+)\s*hours?""", RegexOption.IGNORE_CASE)
        val minutesRegex = Regex("""(\d+)\s*mins?""", RegexOption.IGNORE_CASE)
        
        val hoursMatch = hoursRegex.find(durationStr)
        val minutesMatch = minutesRegex.find(durationStr)
        
        val hours = hoursMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = minutesMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
        
        when {
            hours > 0 && minutes > 0 -> "$hours hours, $minutes minutes"
            hours > 0 -> "$hours hours"
            minutes > 0 -> "$minutes minutes"
            else -> durationStr // Fallback
        }
    } catch (e: Exception) {
        // Fallback: return original string
        durationStr
    }
}

// --- Loading Shimmer (Text-Based) ---

@Composable
private fun TextShimmerRow() {
    Column {
        // Label Shimmer
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .textShimmerEffect()
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Distance Shimmer (full width)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .textShimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        // Duration Shimmer (full width)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .textShimmerEffect()
        )
    }
}

private fun Modifier.textShimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "TextShimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat(),
        targetValue = 2 * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "Offset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFFF5F5F5),
                Color(0xFFFFFFFF),
                Color(0xFFF5F5F5),
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}