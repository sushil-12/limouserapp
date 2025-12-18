package com.example.limouserapp.ui.liveride

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.socket.ActiveRide
import org.json.JSONObject

/**
 * TEMPORARY TEST HELPER - REMOVE AFTER TESTING
 * Provides quick test button to trigger live ride screen
 */
@Composable
fun LiveRideTestButton(
    onNavigateToLiveRide: (bookingId: String?) -> Unit,
    socketService: com.example.limouserapp.data.socket.SocketService
) {
    var showTestOption by remember { mutableStateOf(false) }
    
    // Floating Test Button - REMOVE IN PRODUCTION
    FloatingActionButton(
        onClick = { showTestOption = true },
        modifier = Modifier.padding(16.dp),
        containerColor = MaterialTheme.colorScheme.error
    ) {
        Text("TEST", style = MaterialTheme.typography.labelSmall)
    }
    
    if (showTestOption) {
        AlertDialog(
            onDismissRequest = { showTestOption = false },
            title = { Text("Test Live Ride") },
            text = { 
                Column {
                    Text("Select test scenario:")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val bookingId = triggerTestLiveRide(socketService, "en_route_pu")
                        onNavigateToLiveRide(bookingId)
                        showTestOption = false
                    }) {
                        Text("Driver On Way")
                    }
                    Button(onClick = {
                        val bookingId = triggerTestLiveRide(socketService, "on_location")
                        onNavigateToLiveRide(bookingId)
                        showTestOption = false
                    }) {
                        Text("Driver Arrived")
                    }
                    Button(onClick = {
                        val bookingId = triggerTestLiveRide(socketService, "en_route_do")
                        onNavigateToLiveRide(bookingId)
                        showTestOption = false
                    }) {
                        Text("En Route to Drop")
                    }
                }
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { showTestOption = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Trigger test live ride event
 */
fun triggerTestLiveRide(
    socketService: com.example.limouserapp.data.socket.SocketService,
    status: String = "en_route_pu"
): String {
    val bookingId = "TEST${System.currentTimeMillis()}"
    val mockRide = ActiveRide(
        bookingId = bookingId,
        driverId = "DRIVER456",
        customerId = "CUSTOMER789",
        status = status,
        driverLatitude = 37.7749,
        driverLongitude = -122.4194,
        pickupLatitude = 37.7849,
        pickupLongitude = -122.4094,
        dropoffLatitude = 37.7649,
        dropoffLongitude = -122.4294,
        pickupAddress = "123 Test St, San Francisco",
        dropoffAddress = "456 Demo Ave, San Francisco",
        timestamp = System.currentTimeMillis().toString()
    )

    socketService.setTestActiveRide(mockRide)
    Log.d("TEST", "✅ Test live ride triggered: status=$status, bookingId=$bookingId")
    return bookingId
}
