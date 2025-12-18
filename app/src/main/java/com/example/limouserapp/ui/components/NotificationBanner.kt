package com.example.limouserapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoBlack
import kotlinx.coroutines.launch

/**
 * Clean, professional notification banner inspired by Uber
 * Features:
 * - Auto-dismiss after 4 seconds
 * - Swipe to dismiss
 * - Clean, modern design
 * - Color-coded by notification type
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationBanner(
    title: String,
    message: String,
    type: String,
    priority: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-dismiss after 4 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(4000)
        isVisible = false
        kotlinx.coroutines.delay(300) // Wait for animation
        onDismiss()
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(300))
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            onClick = {
                isVisible = false
                coroutineScope.launch {
                    kotlinx.coroutines.delay(300)
                    onDismiss()
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Icon(
                    imageVector = getNotificationIcon(type),
                    contentDescription = null,
                    tint = getNotificationColor(type),
                    modifier = Modifier.size(24.dp)
                )
                
                // Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = Color(0xFF1A1A1A)
                    )
                    
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp
                        ),
                        color = Color(0xFF666666)
                    )
                }
                
                // Close button
                IconButton(
                    onClick = {
                        isVisible = false
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(300)
                            onDismiss()
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF999999),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Get appropriate icon based on notification type
 */
private fun getNotificationIcon(type: String): ImageVector {
    return when {
        type.contains("ride", ignoreCase = true) -> Icons.Default.DirectionsCar
        type.contains("arrived", ignoreCase = true) -> Icons.Default.NotificationImportant
        type.contains("message", ignoreCase = true) -> Icons.Default.Message
        type.contains("payment", ignoreCase = true) -> Icons.Default.Payment
        type.contains("info", ignoreCase = true) -> Icons.Default.Info
        type.contains("success", ignoreCase = true) -> Icons.Default.CheckCircle
        type.contains("warning", ignoreCase = true) -> Icons.Default.Warning
        type.contains("error", ignoreCase = true) -> Icons.Default.Error
        else -> Icons.Default.Notifications
    }
}

/**
 * Get appropriate color based on notification type
 */
private fun getNotificationColor(type: String): Color {
    return when {
        type.contains("success", ignoreCase = true) -> Color(0xFF4CAF50)
        type.contains("warning", ignoreCase = true) -> Color(0xFFFF9800)
        type.contains("error", ignoreCase = true) -> Color(0xFFF44336)
        type.contains("arrived", ignoreCase = true) -> Color(0xFF00BCD4)
        type.contains("ride", ignoreCase = true) -> LimoOrange
        type.contains("payment", ignoreCase = true) -> Color(0xFF9C27B0)
        type.contains("info", ignoreCase = true) -> Color(0xFF2196F3)
        else -> LimoOrange
    }
}

