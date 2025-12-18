package com.example.limouserapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Email
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
import com.example.limouserapp.data.model.booking.DriverInformation
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite
import android.content.Intent
import android.net.Uri

/**
 * Driver Info Card - matches iOS driverDetailsCard
 * Displays driver information in Transportation Details section
 * Supports both create mode (using Vehicle) and edit mode (using EditReservationData)
 */
@Composable
fun DriverInfoCard(
    editData: EditReservationData? = null,
    vehicle: Vehicle? = null,
    modifier: Modifier = Modifier
) {
    // Extract driver info from editData (edit mode) or vehicle (create mode)
    val driverImageUrl = editData?.driverImage ?: vehicle?.driverInformation?.imageUrl ?: ""
    val driverName = editData?.driverName ?: vehicle?.driverInformation?.name ?: "1800limo Chauffeurs"
    val driverGender = editData?.driverGender ?: vehicle?.driverInformation?.gender ?: "Male"
    val driverPhone = editData?.driverCell ?: vehicle?.driverInformation?.cellNumber ?: ""
    val driverPhoneIsd = editData?.driverCellIsd ?: vehicle?.driverInformation?.cellIsd ?: "+1"
    val driverEmail = editData?.driverEmail ?: vehicle?.driverInformation?.email ?: ""
    val driverLanguages = editData?.driverLanguages ?: vehicle?.driverInformation?.languages ?: ""
    val driverDress = editData?.driverDresses ?: vehicle?.driverInformation?.dress ?: ""
    
    // Don't show card if no driver info available
    if (driverImageUrl.isEmpty() && driverName == "1800limo Chauffeurs" && driverPhone.isEmpty()) {
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
                text = "Driver Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LimoBlack
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Driver Image
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(driverImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Driver Image",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
                
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Driver Name
                    Text(
                        text = driverName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = LimoBlack
                    )
                    
                    // Driver Gender
                    if (driverGender.isNotEmpty()) {
                        val displayGender = if (driverGender.lowercase() == "other") "LGBTQ" else driverGender
                        Text(
                            text = displayGender.replaceFirstChar { it.uppercaseChar() },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
            
            Divider()
            
            // Driver Contact Information
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Phone Number (tappable)
                if (driverPhone.isNotEmpty()) {
                    val phoneNumber = "$driverPhoneIsd $driverPhone"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:$phoneNumber")
                                }
                                context.startActivity(intent)
                            },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = LimoOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = phoneNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LimoOrange
                        )
                    }
                }
                
                // Email (tappable)
                if (driverEmail.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$driverEmail")
                                }
                                context.startActivity(intent)
                            },
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = LimoOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = driverEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LimoOrange
                        )
                    }
                }
            }
            
            // Additional Driver Information (if available)
            if (driverLanguages.isNotEmpty() || driverDress.isNotEmpty()) {
                Divider()
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (driverLanguages.isNotEmpty()) {
                        Text(
                            text = "Languages: $driverLanguages",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                    if (driverDress.isNotEmpty()) {
                        Text(
                            text = "Dress: $driverDress",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

