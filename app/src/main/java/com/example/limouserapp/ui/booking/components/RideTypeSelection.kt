package com.example.limouserapp.ui.booking.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.model.booking.RideType
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.GoogleSansFamily

/**
 * Ride type selection buttons (One-Way, Round Trip, etc.)
 */
@Composable
fun RideTypeSelection(
    selectedRideType: RideType,
    onRideTypeSelected: (RideType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RideType.allCases.forEach { rideType ->
            val isSelected = rideType == selectedRideType
            val borderColor = Color(0xFF252C3E)
            
            if (isSelected) {
                Button(
                    onClick = { onRideTypeSelected(rideType) },
                    shape = RoundedCornerShape(4.dp),
                    elevation = ButtonDefaults.buttonElevation(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimoOrange,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Text(
                        rideType.displayName,
                        fontFamily = GoogleSansFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp
                    )
                }
            } else {
                OutlinedButton(
                    onClick = { onRideTypeSelected(rideType) },
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, borderColor),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = borderColor
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Text(
                        rideType.displayName,
                        fontFamily = GoogleSansFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
    Divider(color = Color(0xFFEAEAEA), thickness = 1.dp)
}

