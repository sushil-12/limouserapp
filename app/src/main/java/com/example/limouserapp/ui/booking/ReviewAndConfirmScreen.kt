package com.example.limouserapp.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite
import com.example.limouserapp.ui.viewmodel.ReservationViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags

@Composable
fun ReviewAndConfirmScreen(
    rideData: RideData,
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onSuccess: (com.example.limouserapp.data.model.booking.ReservationData?) -> Unit
) {
    val vm: ReservationViewModel = hiltViewModel()
    val loading by vm.loading.collectAsState()
    val result by vm.result.collectAsState()

    LaunchedEffect(result) {
        result?.onSuccess { res ->
            if (res.success) {
                onSuccess(res.data)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LimoWhite)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LimoBlack)
            }
            Text(
                "Review your booking",
                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = LimoBlack)
            )
            Spacer(modifier = Modifier.size(48.dp))
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            color = Color.Gray.copy(alpha = 0.3f)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
                .imePadding()
        ) {
            Spacer(Modifier.height(16.dp))

            // Summary (simplified)
            SummaryRow("Pickup", rideData.pickupLocation)
            SummaryRow("Dropoff", rideData.destinationLocation)
            SummaryRow("Date", rideData.pickupDate)
            SummaryRow("Time", rideData.pickupTime)
            SummaryRow("Passengers", rideData.noOfPassenger.toString())
            SummaryRow("Luggage", rideData.noOfLuggage.toString())
            SummaryRow("Vehicles", rideData.noOfVehicles.toString())
            SummaryRow("Vehicle", vehicle.name)

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    android.util.Log.d(com.example.limouserapp.ui.utils.DebugTags.BookingProcess, "===========================================")
                    android.util.Log.d(com.example.limouserapp.ui.utils.DebugTags.BookingProcess, "📱 BOOK NOW BUTTON CLICKED")
                    android.util.Log.d(com.example.limouserapp.ui.utils.DebugTags.BookingProcess, "===========================================")
                    android.util.Log.d(com.example.limouserapp.ui.utils.DebugTags.BookingProcess, "Ride Data: serviceType=${rideData.serviceType}, pickup=${rideData.pickupLocation}, dropoff=${rideData.destinationLocation}")
                    android.util.Log.d(com.example.limouserapp.ui.utils.DebugTags.BookingProcess, "Vehicle: id=${vehicle.id}, name=${vehicle.name}")
                    android.util.Log.d(com.example.limouserapp.ui.utils.DebugTags.BookingProcess, "Calling vm.createReservation()...")
                    android.util.Log.d(com.example.limouserapp.ui.utils.DebugTags.BookingProcess, "===========================================")
                    vm.createReservation(rideData, vehicle)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(bottom = 24.dp),
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = LimoOrange)
            ) {
                if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp) else Text("Confirm & Book", color = Color.White)
            }
        }
    }
}

/**
 * Preview for Review And Confirm Screen
 */
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun ReviewAndConfirmScreenPreview() {
    MaterialTheme {
        ReviewAndConfirmScreen(
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
                destinationLong = 0.0
            ),
            vehicle = Vehicle(
                id = 1,
                name = "Sedan",
                capacity = 4,
                luggage = 2,
                price = 50.0,
                image = null
            ),
            onDismiss = {},
            onSuccess = {}
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = TextStyle(fontSize = 14.sp, color = Color.Gray))
        Text(value, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LimoBlack))
    }
}



