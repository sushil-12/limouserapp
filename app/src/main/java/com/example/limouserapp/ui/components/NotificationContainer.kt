package com.example.limouserapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.limouserapp.data.socket.SocketService
import com.example.limouserapp.data.socket.UserNotification
import javax.inject.Inject

/**
 * Container for displaying real-time in-app notifications
 * Observes SocketService notifications and displays them at the top of the screen
 * Similar to Uber's notification system
 */
@Composable
fun NotificationContainer(
    socketService: SocketService,
    modifier: Modifier = Modifier
) {
    val notifications by socketService.userNotifications.collectAsStateWithLifecycle()
    var visibleNotificationId by remember { mutableStateOf<String?>(null) }
    
    // Track the latest notification to display
    val latestNotification = notifications.lastOrNull()
    
    // Update visible notification when a new one arrives
    LaunchedEffect(latestNotification?.id) {
        latestNotification?.id?.let { id ->
            if (id != visibleNotificationId) {
                visibleNotificationId = id
            }
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 56.dp) // Account for status bar
    ) {
        // Display the current notification
        latestNotification?.let { notification ->
            if (notification.id == visibleNotificationId) {
                NotificationBanner(
                    title = notification.title.ifEmpty { "Notification" },
                    message = notification.message.ifEmpty { "You have a new notification" },
                    type = notification.type,
                    priority = notification.priority,
                    onDismiss = {
                        // Only remove this specific notification
                        visibleNotificationId = null
                    }
                )
            }
        }
    }
}

/**
 * Standalone notification system that can be used anywhere
 */
@Composable
fun NotificationDisplay(
    title: String,
    message: String,
    type: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    NotificationBanner(
        title = title,
        message = message,
        type = type,
        priority = "normal",
        onDismiss = onDismiss,
        modifier = modifier
    )
}

