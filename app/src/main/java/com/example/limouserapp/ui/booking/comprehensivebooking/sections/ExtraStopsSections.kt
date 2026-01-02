package com.example.limouserapp.ui.booking.comprehensivebooking.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.ui.components.LocationAutocomplete
import com.example.limouserapp.ui.booking.comprehensivebooking.ExtraStop
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange

/**
 * Extra Stops Section
 */
@Composable
fun ExtraStopsSection(
    extraStops: List<ExtraStop>,
    onAddStop: () -> Unit,
    onRemoveStop: (ExtraStop) -> Unit,
    onLocationSelected: (ExtraStop, String, String, String, String, String, Double?, Double?, String?) -> Unit,
    onLocationChange: (ExtraStop, String) -> Unit,
    onInstructionsChange: (ExtraStop, String) -> Unit
) {
    BaseExtraStopsLayout(
        title = "Extra Stops",
        stops = extraStops,
        onAddStop = onAddStop,
        onRemoveStop = onRemoveStop,
        onLocationSelected = onLocationSelected,
        onLocationChange = onLocationChange,
        onInstructionsChange = onInstructionsChange
    )
}

/**
 * Return Extra Stops Section
 */
@Composable
fun ReturnExtraStopsSection(
    returnExtraStops: List<ExtraStop>,
    onAddStop: () -> Unit,
    onRemoveStop: (ExtraStop) -> Unit,
    onLocationSelected: (ExtraStop, String, String, String, String, String, Double?, Double?, String?) -> Unit,
    onLocationChange: (ExtraStop, String) -> Unit,
    onInstructionsChange: (ExtraStop, String) -> Unit
) {
    BaseExtraStopsLayout(
        title = "Return Trip Stops",
        stops = returnExtraStops,
        onAddStop = onAddStop,
        onRemoveStop = onRemoveStop,
        onLocationSelected = onLocationSelected,
        onLocationChange = onLocationChange,
        onInstructionsChange = onInstructionsChange,
        isReturn = true
    )
}

/**
 * Shared Layout
 */
@Composable
private fun BaseExtraStopsLayout(
    title: String,
    stops: List<ExtraStop>,
    onAddStop: () -> Unit,
    onRemoveStop: (ExtraStop) -> Unit,
    onLocationSelected: (ExtraStop, String, String, String, String, String, Double?, Double?, String?) -> Unit,
    onLocationChange: (ExtraStop, String) -> Unit,
    onInstructionsChange: (ExtraStop, String) -> Unit,
    isReturn: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Section Header
        Text(
            text = title,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isReturn) LimoOrange else LimoBlack
            )
        )

        // 2. List of Stops (Above the button)
        if (stops.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                stops.forEachIndexed { index, stop ->
                    ExtraStopCard(
                        stop = stop,
                        index = index + 1,
                        onRemove = { onRemoveStop(stop) },
                        onLocationSelected = { fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country ->
                            onLocationSelected(
                                stop,
                                fullAddress,
                                city,
                                state,
                                zipCode,
                                locationDisplay,
                                latitude,
                                longitude,
                                country
                            )
                        },
                        onLocationChange = { newValue ->
                            onLocationChange(stop, newValue)
                        }
                    )
                }
            }
        }

        // 3. "Add Extra Stop" Button (Below list, not full width)
        Button(
            onClick = onAddStop,
            modifier = Modifier
                .wrapContentWidth() // Keeps button small
                .heightIn(min = 44.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LimoBlack
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Add Extra Stop",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            )
        }
    }
}

/**
 * Individual Stop Row
 */
@Composable
private fun ExtraStopCard(
    stop: ExtraStop,
    index: Int,
    onRemove: () -> Unit,
    onLocationSelected: (String, String, String, String, String, Double?, Double?, String?) -> Unit,
    onLocationChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom, // Align bottom so text field and button align at baseline
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Location Input Field
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "STOP $index",
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )

            LocationAutocomplete(
                value = stop.address,
                onValueChange = onLocationChange,
                onLocationSelected = onLocationSelected,
                placeholder = "Enter stop address"
            )
        }

        // Trash/Remove Button
        // Height 56.dp matches standard Material TextField height
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Delete, // Or Icons.Filled.Cancel depending on preference
                contentDescription = "Remove stop",
                tint = Color(0xFFFF4444),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}