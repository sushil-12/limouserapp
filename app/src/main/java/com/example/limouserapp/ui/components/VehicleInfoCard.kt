package com.example.limouserapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.limouserapp.data.model.booking.EditReservationData
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite

/**
 * Vehicle Info Card - matches iOS vehicleDetailsCard
 * Displays vehicle information in Transportation Details section
 * Supports both create mode (using Vehicle) and edit mode (using EditReservationData)
 */
@Composable
fun VehicleInfoCard(
    editData: EditReservationData? = null,
    vehicle: Vehicle? = null,
    modifier: Modifier = Modifier
) {
    // Extract vehicle info from editData (edit mode) or vehicle (create mode)
    val vehicleImages = editData?.vehicleImages ?: vehicle?.vehicleImages ?: emptyList()
    val vehicleName = editData?.vehicleTypeName ?: vehicle?.name ?: ""
    val vehicleMake = editData?.vehicleMakeName ?: vehicle?.vehicleDetails?.make ?: ""
    val vehicleModel = editData?.vehicleModelName ?: vehicle?.vehicleDetails?.model ?: ""
    val vehicleYear = editData?.vehicleYearName ?: vehicle?.vehicleDetails?.year ?: ""
    val vehicleColor = editData?.vehicleColorName ?: ""
    val vehicleSeats = editData?.vehicleSeatsName?.toString() ?: vehicle?.capacity?.toString() ?: ""
    val vehicleLicensePlate = editData?.vehicleLicensePlateName ?: ""
    
    // Don't show card if no vehicle info available
    if (vehicleName.isEmpty() && vehicleImages.isEmpty()) {
        return
    }
    
    // Get context at composable level
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LimoWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title
            Text(
                text = "Vehicle Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LimoBlack
            )
            
            // Vehicle Image
            if (vehicleImages.isNotEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(vehicleImages.first())
                        .crossfade(true)
                        .build(),
                    contentDescription = "Vehicle Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Vehicle Name/Type
            if (vehicleName.isNotEmpty()) {
                Text(
                    text = vehicleName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = LimoBlack
                )
            }
            
            Divider()
            
            // Vehicle Details as Tags
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Make
                if (vehicleMake.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = LimoOrange.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = vehicleMake,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = LimoOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Model
                if (vehicleModel.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = LimoOrange.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = vehicleModel,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = LimoOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Year
                if (vehicleYear.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = LimoOrange.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = vehicleYear,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = LimoOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Additional Vehicle Information
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (vehicleColor.isNotEmpty()) {
                    Text(
                        text = "Color: $vehicleColor",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                if (vehicleSeats.isNotEmpty()) {
                    Text(
                        text = "Seats: $vehicleSeats",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                if (vehicleLicensePlate.isNotEmpty()) {
                    Text(
                        text = "License Plate: $vehicleLicensePlate",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

