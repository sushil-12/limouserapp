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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite

/**
 * Booking Success Screen
 * Professional, clean design with fixed button visibility.
 */
@Composable
fun BookingSuccessScreen(
    onOK: () -> Unit,
    hasDriverAssigned: Boolean = false,
    reservationId: Int? = null,
    orderId: Int? = null,
    returnReservationId: Int? = null
) {
    // 1. Root container handles the background and overall insets
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LimoWhite)
            .windowInsetsPadding(WindowInsets.statusBars) // Avoid status bar overlap
    ) {
        // 2. Center Content (Icon & Text)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(bottom = 100.dp), // Extra padding at bottom to clear the button area
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success Icon with a soft background circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color(0xFFE8F5E9), CircleShape), // Very light green
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    modifier = Modifier.size(50.dp),
                    tint = Color(0xFF4CAF50) // Material Green 500
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title
            Text(
                text = "Booking Confirmed!",
                style = TextStyle(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = LimoBlack,
                    letterSpacing = (-0.5).sp
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Friendly, readable description
            Text(
                text = "Thank you for booking with us. Your ride has been scheduled successfully.",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Gray,
                    lineHeight = 24.sp // Better readability
                ),
                textAlign = TextAlign.Center
            )
            
            // Reservation Numbers
            if (reservationId != null || orderId != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    color = Color(0xFFF5F5F5), // Light gray background
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Booking Details",
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Reservation ID
                        if (reservationId != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Reservation #",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = LimoBlack
                                    )
                                )
                                Text(
                                    text = reservationId.toString(),
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LimoOrange
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Order ID
                        if (orderId != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Order #",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = LimoBlack
                                    )
                                )
                                Text(
                                    text = orderId.toString(),
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LimoOrange
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Return Reservation ID (for round trips)
                        if (returnReservationId != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Return Reservation #",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = LimoBlack
                                    )
                                )
                                Text(
                                    text = returnReservationId.toString(),
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = LimoOrange
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Driver Status Badge
            if (!hasDriverAssigned) {
                Surface(
                    color = Color(0xFFFFF3E0), // Light Orange background
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "We are currently locating a driver nearby.\nYou will be notified once assigned.",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFEF6C00), // Darker Orange text
                            lineHeight = 20.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else {
                Surface(
                    color = Color(0xFFE3F2FD), // Light Blue
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "A driver has been assigned to your trip.",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF1565C0) // Dark Blue
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }
        }
        
        // 3. Bottom Button - Fixed Visibility
        // Using Align BottomCenter + navigationBarsPadding ensures it's never hidden
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp) // Visual padding from bottom
                .windowInsetsPadding(WindowInsets.navigationBars) // CRITICAL: Pushes up above gesture bar
        ) {
            Button(
                onClick = onOK,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LimoOrange
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = "Done",
                    style = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                )
            }
        }
    }
}