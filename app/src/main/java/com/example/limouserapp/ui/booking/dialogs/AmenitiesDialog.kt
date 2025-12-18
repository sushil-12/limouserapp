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
import com.example.limouserapp.data.model.booking.Amenity
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange

/**
 * Amenities Dialog
 * Matches iOS AmenitiesDialog design
 */
@Composable
fun AmenitiesDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit
) {
    val chargeableAmenities = vehicle.amenities?.filter { 
        it.chargeable.lowercase() == "yes" || 
        it.chargeable.lowercase() == "true" || 
        it.chargeable == "1"
    } ?: emptyList()
    
    val nonChargeableAmenities = vehicle.amenities?.filter { 
        it.chargeable.lowercase() == "no" || 
        it.chargeable.lowercase() == "false" || 
        it.chargeable == "0"
    } ?: emptyList()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(12.dp),
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
                        .background(Color.Gray.copy(alpha = 0.1f))
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Amenities",
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LimoBlack)
                    )
                    IconButton(onClick = onDismiss) {
                        Text("✕", fontSize = 16.sp, color = LimoBlack)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Chargeable Amenities Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "CHARGABLE AMENITIES",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        )
                        Text(
                            "In $",
                            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LimoOrange)
                        )
                    }
                    
                    // Dynamic amenities list in two columns
                    if (chargeableAmenities.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Left column
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chargeableAmenities.forEachIndexed { index, amenity ->
                                    if (index % 2 == 0) {
                                        AmenityRow(amenity.name)
                                    }
                                }
                            }
                            
                            // Right column
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chargeableAmenities.forEachIndexed { index, amenity ->
                                    if (index % 2 == 1) {
                                        AmenityRow(amenity.name)
                                    }
                                }
                            }
                        }
                    } else {
                        // Fallback to hardcoded values
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AmenityRow("Baby Seat")
                                AmenityRow("Bike Rack")
                                AmenityRow("Security/Guard")
                                AmenityRow("Per Diem")
                                AmenityRow("Red Carpet")
                                AmenityRow("Luggage Trailer")
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AmenityRow("Golf Bags")
                                AmenityRow("Skis")
                                AmenityRow("Wedding Package")
                                AmenityRow("(Decorations & Champaign)")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
                
                // Non-Chargeable Amenities Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LimoOrange.copy(alpha = 0.2f))
                ) {
                    Text(
                        "NON-CHARGABLE AMENITIES",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)
                    )
                    
                    // Dynamic amenities list in two columns
                    if (nonChargeableAmenities.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Left column
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                nonChargeableAmenities.forEachIndexed { index, amenity ->
                                    if (index % 2 == 0) {
                                        AmenityRow(amenity.name)
                                    }
                                }
                            }
                            
                            // Right column
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                nonChargeableAmenities.forEachIndexed { index, amenity ->
                                    if (index % 2 == 1) {
                                        AmenityRow(amenity.name)
                                    }
                                }
                            }
                        }
                    } else {
                        // Fallback to hardcoded values
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AmenityRow("LGBTQA+ Friendly")
                                AmenityRow("Handicap Friendly")
                                AmenityRow("Snacks")
                                AmenityRow("Pet Friendly")
                                AmenityRow("Water")
                                AmenityRow("iPad")
                                AmenityRow("PA/Intercom")
                                AmenityRow("Dash-Cam")
                                AmenityRow("Tinted Glass")
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AmenityRow("Ice Chest")
                                AmenityRow("Mask")
                                AmenityRow("Magazines")
                                AmenityRow("Soda")
                                AmenityRow("Pillow")
                                AmenityRow("USB Charger")
                                AmenityRow("3 Pin Laptop Charger")
                                AmenityRow("CD Player")
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                }
                
                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .width(75.dp)
                        .height(34.dp)
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
private fun AmenityRow(name: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "•",
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
        )
        Text(
            name,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = LimoBlack)
        )
    }
}


