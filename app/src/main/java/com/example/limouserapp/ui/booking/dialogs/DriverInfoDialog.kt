package com.example.limouserapp.ui.booking.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.ui.theme.LimoBlack

/**
 * Driver Info Dialog
 * Matches iOS DriverInfoDialog design
 */
@Composable
fun DriverInfoDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit
) {
    val driverInfo = vehicle.driverInformation
    
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
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF2F2F7))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Driver Details",
                        style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = LimoBlack)
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, color = LimoBlack)
                    }
                }
                
                // Profile section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Profile image
                    AsyncImage(
                        model = driverInfo?.imageUrl ?: "",
                        contentDescription = "Driver",
                        modifier = Modifier.size(70.dp),
                        contentScale = ContentScale.Crop,
//                        error = {
//                            Box(
//                                modifier = Modifier
//                                    .size(70.dp)
//                                    .background(Color.Gray.copy(alpha = 0.3f), CircleShape),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                Text("👤", fontSize = 40.sp)
//                            }
//                        }
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Info texts
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            driverInfo?.name ?: "John Smith",
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LimoBlack)
                        )
                        Text(
                            "${driverInfo?.cellIsd ?: "+1"} ${driverInfo?.cellNumber ?: "98765 43210"}",
                            style = TextStyle(fontSize = 14.sp, color = Color.Gray)
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Attribute section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    color = Color(0xFFFFF7ED),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DriverAttributeRow("Gender", driverInfo?.gender ?: "Male")
                        DriverAttributeRow("Dress", driverInfo?.dress ?: "Business Casual")
                        DriverAttributeRow("Experience", driverInfo?.experience ?: "20+ Years")
                        DriverAttributeRow("Languages", driverInfo?.languages ?: "English")
                        DriverAttributeRow("Insurance Limit", driverInfo?.insuranceLimit ?: "$500,000")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 20.dp)
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
            }
        }
    }
}

@Composable
private fun DriverAttributeRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = TextStyle(fontSize = 14.sp, color = LimoBlack.copy(alpha = 0.8f)),
            modifier = Modifier.width(140.dp)
        )
        Text(
            value,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LimoBlack),
            modifier = Modifier.weight(1f)
        )
    }
}

