package com.example.limouserapp.ui.booking.comprehensivebooking.sections

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.limouserapp.ui.booking.comprehensivebooking.Tag
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite

/**
 * Transportation Details Section
 */
@Composable
fun TransportationDetailsSection(
    selectedMeetAndGreet: String,
    onMeetAndGreetChange: (String) -> Unit,
    isEditMode: Boolean = false,
    editData: com.example.limouserapp.data.model.booking.EditReservationData? = null,
    vehicle: com.example.limouserapp.data.model.booking.Vehicle? = null,
    meetAndGreetOptions: List<String>,
    showMeetAndGreetDropdown: Boolean,
    onMeetAndGreetDropdownChange: (Boolean) -> Unit
) {
    Text(
        "Transportation Details",
        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LimoOrange),
        modifier = Modifier.padding(bottom = 8.dp)
    )
    
    // Driver Card
    val driverName = editData?.driverName ?: vehicle?.driverInformation?.name ?: "1800limo Chauffeurs"
    val driverImg = editData?.driverImage ?: vehicle?.driverInformation?.imageUrl
    val driverRating =  "4.5"
    val driverGender = editData?.driverGender ?: vehicle?.driverInformation?.gender ?: "Male"
    val driverPhone = editData?.driverCell ?: vehicle?.driverInformation?.phone ?: "+1 8005466626"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = LimoWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("DRIVER DETAILS", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Profile Image
                Surface(
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                    color = Color.LightGray
                ) {
                    if (driverImg != null && driverImg.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(driverImg),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(driverName, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFEEEEEE), shape = RoundedCornerShape(4.dp)) {
                            Text(driverGender, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = TextStyle(fontSize = 11.sp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Surface(color = LimoOrange, shape = RoundedCornerShape(4.dp)) {
                            Text(driverPhone, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = TextStyle(fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
    
    Spacer(Modifier.height(16.dp))
    
    // Vehicle Card
    val vehicleName = vehicle?.name ?: "Vehicle"
    // Use vehicleImages.first() to match iOS, fallback to image
    val vehicleImg = vehicle?.vehicleImages?.firstOrNull() ?: vehicle?.image
    val vMake = vehicle?.vehicleDetails?.make ?: "BMW"
    val vModel = vehicle?.vehicleDetails?.model ?: "Series"
    val vYear = vehicle?.vehicleDetails?.year ?: "2023"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = LimoWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("VEHICLE DETAILS", style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Vehicle Image (matches iOS: 80x60, horizontal layout)
                if (vehicleImg != null && vehicleImg.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(vehicleImg),
                        contentDescription = null,
                        modifier = Modifier
                            .width(80.dp)
                            .height(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Placeholder with car icon (matches iOS)
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(60.dp)
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🚗", fontSize = 32.sp)
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(vehicleName, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag(vMake)
                        Tag(vModel)
                        Tag(vYear)
                    }
                }
            }
        }
    }
}

