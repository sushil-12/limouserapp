package com.example.limouserapp.ui.booking.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.limouserapp.data.model.booking.RateBreakdown
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange

/**
 * Subtotal Dialog
 * Matches iOS SubtotalDialog design
 */
@Composable
fun SubtotalDialog(
    vehicle: Vehicle,
    serviceType: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7F7F7))
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Estimated Rate Slip",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = LimoBlack)
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, color = LimoBlack)
                    }
                }
                
                // Gratuity Tag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF7F7F7))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = Color(0xFFFFE8AB), // Yellow pill background
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            "Gratuity included (hard to extra at own discretion)",
                            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF921000)),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Rate Table
                Column {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFF7ED))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "Rate Type",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LimoOrange),
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "Rate",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LimoOrange),
                            modifier = Modifier.width(90.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                        Text(
                            "Amount",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LimoOrange),
                            modifier = Modifier.width(90.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                    
                    HorizontalDivider()
                    
                    // Trip Fare row
                    RateTableRow(
                        label = "Trip Fare",
                        rate = getTripFare(vehicle, serviceType),
                        amount = getTripFare(vehicle, serviceType)
                    )
                    
                    HorizontalDivider()
                    
                    // Tax row
                    RateTableRow(
                        label = "Tax",
                        rate = getTaxAmount(vehicle, serviceType),
                        amount = getTaxAmount(vehicle, serviceType)
                    )
                    
                    // Line above total
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 2.dp),
                        thickness = 1.dp,
                        color = LimoBlack
                    )
                    
                    // Total row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Text(
                            "Total",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LimoBlack),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(90.dp))
                        Text(
                            formatCurrency(getTotalAmount(vehicle, serviceType)),
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = LimoBlack),
                            modifier = Modifier.width(90.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Close Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .width(90.dp)
                        .height(36.dp)
                        .align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimoBlack
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Close",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun RateTableRow(label: String, rate: Double, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = TextStyle(fontSize = 14.sp, color = LimoBlack),
            modifier = Modifier.weight(1f)
        )
        Text(
            formatCurrency(rate),
            style = TextStyle(fontSize = 14.sp, color = LimoBlack),
            modifier = Modifier.width(90.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        Text(
            formatCurrency(amount),
            style = TextStyle(fontSize = 14.sp, color = LimoBlack),
            modifier = Modifier.width(90.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

private fun getTripFare(vehicle: Vehicle, serviceType: String): Double {
    val breakdown = getRateBreakdown(vehicle, serviceType)
    return breakdown?.rateArray?.allInclusiveRates?.tripRate?.amount ?: 0.0
}

private fun getTaxAmount(vehicle: Vehicle, serviceType: String): Double {
    val breakdown = getRateBreakdown(vehicle, serviceType)
    return breakdown?.rateArray?.allInclusiveRates?.tripTax?.amount ?: 0.0
}

private fun getTotalAmount(vehicle: Vehicle, serviceType: String): Double {
    val breakdown = getRateBreakdown(vehicle, serviceType)
    return breakdown?.grandTotal ?: breakdown?.total ?: breakdown?.subTotal ?: 0.0
}

private fun getRateBreakdown(vehicle: Vehicle, serviceType: String): RateBreakdown? {
    return when (serviceType.lowercase()) {
        "one_way" -> vehicle.rateBreakdownOneWay
        "round_trip" -> vehicle.rateBreakdownRoundTrip
        "charter_tour" -> vehicle.rateBreakdownCharterTour
        else -> null
    }
}

private fun formatCurrency(amount: Double): String {
    return String.format("$%.2f", amount)
}


