package com.example.limouserapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.limouserapp.ui.liveride.LiveRideViewModel
import com.example.limouserapp.ui.theme.LimoOrange

/**
 * Bottom card component for live ride details
 * Displays status, OTP, estimated time, and action buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRideBottomCard(
    viewModel: LiveRideViewModel,
    statusMessage: String,
    estimatedTime: String,
    distance: String,
    rideOTP: String,
    isDriverOnLocation: Boolean,
    modifier: Modifier = Modifier
) {
    var showChatDialog by remember { mutableStateOf(false) }
    var showCancelAlert by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status message
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            
            // Estimated time and distance
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                InfoRow(
                    icon = Icons.Default.AccessTime,
                    label = "ETA",
                    value = estimatedTime
                )
                
                InfoRow(
                    icon = Icons.Default.Place,
                    label = "Distance",
                    value = distance
                )
            }
            
            // OTP section (shown when driver is on location)
            if (isDriverOnLocation && rideOTP.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)),
                    color = LimoOrange.copy(alpha = 0.1f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "OTP",
                            tint = LimoOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Your Ride OTP",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = rideOTP,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = LimoOrange
                                )
                            )
                        }
                    }
                }
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Chat button
                Button(
                    onClick = { showChatDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "Chat",
                        tint = LimoOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Chat",
                        color = LimoOrange
                    )
                }
                
                // Call button
                Button(
                    onClick = { /* Handle call */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Call",
                        tint = LimoOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Call",
                        color = LimoOrange
                    )
                }
                
                // Share button
                Button(
                    onClick = { /* Handle share */ },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = LimoOrange
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Share",
                        color = LimoOrange
                    )
                }
            }
            
            // Cancel ride button
            Button(
                onClick = { showCancelAlert = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Cancel Ride")
            }
        }
    }
    
    // Chat dialog
    if (showChatDialog) {
        AlertDialog(
            onDismissRequest = { showChatDialog = false },
            title = { Text("Chat with Driver") },
            text = { Text("Chat functionality coming soon") },
            confirmButton = {
                TextButton(onClick = { showChatDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Cancel alert
    if (showCancelAlert) {
        AlertDialog(
            onDismissRequest = { showCancelAlert = false },
            title = { Text("Cancel Ride") },
            text = { Text("Are you sure you want to cancel this ride?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // TODO: Implement cancel ride
                        showCancelAlert = false
                    }
                ) {
                    Text("Yes, Cancel")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelAlert = false }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = LimoOrange,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
