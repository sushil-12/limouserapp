package com.example.limouserapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.model.dashboard.ConnectionStatus

/**
 * Small status indicator showing Socket.IO connection status
 * Shows a colored dot with status text on tap
 * Inspired by iOS ConnectionStatusIndicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocketStatusIndicator(
    connectionStatus: ConnectionStatus,
    modifier: Modifier = Modifier
) {
    var showDetails by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        // Main status indicator
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { showDetails = !showDetails }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Status dot
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(getStatusColor(connectionStatus))
            )
            
            // Status text - visible when tapped
            AnimatedVisibility(
                visible = showDetails,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Text(
                    text = getStatusText(connectionStatus),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        
        // Detailed info card (shown on tap)
        AnimatedVisibility(
            visible = showDetails,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 32.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Connection Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    
                    StatusRow(
                        label = "Status",
                        value = getStatusText(connectionStatus),
                        valueColor = getStatusColor(connectionStatus)
                    )
                    
                    if (connectionStatus.isReconnecting) {
                        StatusRow(
                            label = "Attempts",
                            value = "${connectionStatus.connectionAttempts}",
                            valueColor = Color(0xFFFF9800)
                        )
                    }
                    
                    connectionStatus.error?.let { error ->
                        Text(
                            text = "Error: $error",
                            fontSize = 12.sp,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    valueColor: Color = Color(0xFF1A1A1A)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF666666)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * Get status color based on connection state
 */
private fun getStatusColor(status: ConnectionStatus): Color {
    return when {
        status.isConnected -> Color(0xFF4CAF50) // Green
        status.isReconnecting -> Color(0xFFFFC107) // Orange/Yellow
        status.connectionAttempts > 0 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
}

/**
 * Get status text based on connection state
 */
private fun getStatusText(status: ConnectionStatus): String {
    return when {
        status.isConnected -> "Connected"
        status.isReconnecting -> "Reconnecting..."
        status.connectionAttempts > 0 -> "Failed"
        else -> "Disconnected"
    }
}

