package com.example.limouserapp.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.ui.theme.LimoGreen
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite

// Local color definitions (ensure these match your theme)
private val DarkText = Color(0xFF1A1A1A)
private val GrayText = Color(0xFF757575)

@Composable
fun BookingSuccessScreen(
    onOK: () -> Unit,
    reservationId: Int? = null,
    orderId: Int? = null,
    returnReservationId: Int? = null,
    hasDriverAssigned: Boolean = false,
    isMasterVehicle: Boolean? = null,
    isEditMode: Boolean = false
) {
    Scaffold(
        containerColor = LimoWhite,
        contentWindowInsets = WindowInsets.statusBars, // Handles top status bar

        // 1. Bottom Bar Implementation
        // This anchors the button to the bottom and handles gesture insets automatically
        bottomBar = {
            Surface(
                color = LimoWhite,
                tonalElevation = 8.dp, // Optional: adds a slight shadow separation
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // CRITICAL: Pushes content up above the gesture bar
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp, top = 16.dp) // Add internal padding
                ) {
                    Button(
                        onClick = onOK,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimoOrange
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "OK",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // 2. Main Content Area
        // We use a Box to perfectly center the icon/text in the remaining space
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding) // Respects the top bar and bottom bar space
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Success Icon
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(LimoGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Title
                Text(
                    text = if (isEditMode) "Booking Updated" else "Booking Confirmed",
                    style = TextStyle(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        letterSpacing = -(0.5).sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Booking Number (show reservation ID or order ID)
                val bookingNumber = orderId?.toString() ?: reservationId?.toString()
                if (bookingNumber != null && !isEditMode) {
                    Text(
                        text = "Booking #$bookingNumber",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkText
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Subtitle - Different messages based on edit mode, driver status, and master vehicle
                val subtitleText = when {
                    isEditMode -> "The Booking has updated successfully."
                    hasDriverAssigned && isMasterVehicle != true -> 
                        "Your ride has been booked successfully.\nWe'll notify you as soon as a driver accepts the booking."
                    else -> 
                        "Your ride has been booked successfully.\nWe'll notify you as soon as a driver is assigned."
                }
                
                Text(
                    text = subtitleText,
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = GrayText,
                        lineHeight = 24.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BookingSuccessPreview() {
    MaterialTheme {
        BookingSuccessScreen(onOK = {})
    }
}