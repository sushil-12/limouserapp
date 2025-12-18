package com.example.limouserapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.R
import com.example.limouserapp.data.model.notification.AuditEvent
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoBlack

/**
 * Notification list item component
 * Displays audit event with appropriate icon (Email/SMS) and styling
 */
@Composable
fun NotificationListItem(
    event: AuditEvent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon - no background, just the icon
            EventIcon(event = event)
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = LimoBlack
                )
                
                // Description
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = Color(0xFF666666),
                    maxLines = 2
                )
                
                // Timestamp
                Text(
                    text = formatTimestamp(event.timestampFormatted),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp
                    ),
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * Format timestamp to show relative time when appropriate
 */
private fun formatTimestamp(timestampFormatted: String): String {
    // If the API already provides formatted timestamp, use it
    // Otherwise, try to parse and format
    return try {
        // Try to extract time from formatted string like "Nov 04, 2025 1:58 AM CST"
        val parts = timestampFormatted.split(" ")
        if (parts.size >= 4) {
            // Return just the date and time part, e.g., "Nov 04, 2025 1:58 AM"
            parts.take(4).joinToString(" ")
        } else {
            timestampFormatted
        }
    } catch (e: Exception) {
        timestampFormatted
    }
}

/**
 * Composable to display appropriate icon based on event type
 */
@Composable
private fun EventIcon(event: AuditEvent) {
    val iconColor = getEventIconColor(event)
    
    when {
        event.eventType.contains("status_changed", ignoreCase = true) -> {
            Icon(
                painter = painterResource(id = R.drawable.info_icon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        event.eventType.contains("email", ignoreCase = true) -> {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        event.eventType.contains("sms", ignoreCase = true) -> {
            Icon(
                imageVector = Icons.Default.Message,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        event.eventCategory.contains("communication", ignoreCase = true) -> {
            if (event.eventType.contains("email", ignoreCase = true)) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        event.eventType.contains("booking_updated", ignoreCase = true) -> {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        event.eventType.contains("payment", ignoreCase = true) -> {
            Icon(
                imageVector = Icons.Default.Payment,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        else -> {
            Icon(
                painter = painterResource(id = R.drawable.bell_icon),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Get icon color based on event type
 */
private fun getEventIconColor(event: AuditEvent): Color {
    return when {
        event.eventType.contains("email", ignoreCase = true) -> LimoOrange
        event.eventType.contains("sms", ignoreCase = true) -> Color(0xFF2196F3) // Blue for SMS
        event.eventCategory.contains("communication", ignoreCase = true) -> LimoOrange
        event.eventType.contains("status_changed", ignoreCase = true) -> Color(0xFF4CAF50) // Green for info icon
        event.eventType.contains("payment", ignoreCase = true) -> Color(0xFF9C27B0) // Purple
        else -> LimoOrange // Orange for bell icon
    }
}

