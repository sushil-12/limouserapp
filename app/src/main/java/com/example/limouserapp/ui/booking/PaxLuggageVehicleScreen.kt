package com.example.limouserapp.ui.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.painterResource
import com.example.limouserapp.R
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite
import android.util.Log
import com.example.limouserapp.ui.theme.AppDimensions
import com.example.limouserapp.ui.utils.DebugTags

@Composable
fun PaxLuggageVehicleScreen(
    rideData: RideData,
    onDismiss: () -> Unit,
    onNext: (RideData) -> Unit
) {
    var passengers by remember { mutableIntStateOf(rideData.noOfPassenger) }
    var luggage by remember { mutableIntStateOf(rideData.noOfLuggage) }
    var vehicles by remember { mutableIntStateOf(rideData.noOfVehicles) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LimoWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        // Header with proper centering
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LimoBlack)
            }
            Text(
                "Pax, Luggage & Vehicle Info",
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = LimoBlack),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp),
            color = Color.Gray.copy(alpha = 0.3f)
        )

        // Sections
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            QuantitySection(
                title = "NO. OF PASSENGERS",
                value = passengers,
                onChange = { passengers = it }
            )
            QuantitySection(
                title = "NO. OF LUGGAGE",
                value = luggage,
                onChange = { luggage = it }
            )
            QuantitySection(
                title = "NO. OF VEHICLES",
                value = vehicles,
                onChange = { vehicles = it }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                Log.d(DebugTags.BookingProcess, "Pax/Luggage selected pax=${passengers} lug=${luggage} vehicles=${vehicles}")
                onNext(
                    rideData.copy(
                        noOfPassenger = passengers,
                        noOfLuggage = luggage,
                        noOfVehicles = vehicles
                    )
                )
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
                Text("Next", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White))
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

/**
 * Preview for Pax Luggage Vehicle Screen
 */
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun PaxLuggageVehicleScreenPreview() {
    MaterialTheme {
        PaxLuggageVehicleScreen(
            rideData = RideData(
                serviceType = "ONE_WAY",
                bookingHour = "2",
                pickupType = "CITY_FBO",
                dropoffType = "AIRPORT",
                pickupDate = "2024-01-15",
                pickupTime = "14:30:00",
                pickupLocation = "123 Main St",
                destinationLocation = "Airport",
                selectedPickupAirport = "",
                selectedDestinationAirport = "",
                noOfPassenger = 1,
                noOfLuggage = 1,
                noOfVehicles = 1,
                pickupLat = 0.00,
                pickupLong = 0.00,
                destinationLat = 0.00,
                destinationLong = 0.00
            ),
            onDismiss = {},
            onNext = {}
        )
    }
}

@Composable
private fun QuantitySection(
    title: String,
    value: Int,
    onChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            title,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        QuantitySelector(value = value, onChange = onChange)
    }
}

@Composable
private fun QuantitySelector(
    value: Int,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Decrement button - square orange button
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(LimoOrange, RoundedCornerShape(4.dp))
                .clickable(enabled = value > 1) {
                    if (value > 1) onChange(value - 1)
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "Decrement",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }

        // Value display - centered
        Text(
            String.format("%02d", value),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = LimoBlack
            ),
            textAlign = TextAlign.Center
        )

        // Increment button - square orange button
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(LimoOrange, RoundedCornerShape(4.dp))
                .clickable { onChange(value + 1) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Increment",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}



