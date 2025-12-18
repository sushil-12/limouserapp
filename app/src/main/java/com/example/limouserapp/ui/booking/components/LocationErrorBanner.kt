package com.example.limouserapp.ui.booking.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.ui.theme.GoogleSansFamily

/**
 * Location error banner component
 * Displays red alert errors for location validation issues
 */
@Composable
fun LocationErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, subtitle) = parseErrorMessage(message)
    
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFFFEBEE) // Light red background
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = GoogleSansFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F) // Red text
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            fontFamily = GoogleSansFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFFD32F2F).copy(alpha = 0.8f)
                        )
                    }
                }
                
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color(0xFFD32F2F),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Parse error message into title and subtitle
 */
private fun parseErrorMessage(message: String): Pair<String, String> {
    val lowercased = message.lowercase()
    
    when {
        lowercased.contains("country") -> {
            return Pair(
                "Pickup and drop countries must match",
                "Please select pickup and drop-off within the same country."
            )
        }
        lowercased.contains("same") || lowercased.contains("cannot be the same") -> {
            return Pair(
                "Pickup and drop locations are same",
                "Please change your pickup or drop location."
            )
        }
        else -> {
            // Try to split by period
            val periodIndex = message.indexOf('.')
            if (periodIndex != -1 && periodIndex < message.length - 1) {
                val title = message.substring(0, periodIndex).trim()
                val subtitle = message.substring(periodIndex + 1).trim()
                if (title.isNotEmpty() && subtitle.isNotEmpty()) {
                    return Pair(title, subtitle)
                }
            }
            return Pair("Invalid location selected", message)
        }
    }
}

