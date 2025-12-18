package com.example.limouserapp.data.notification.channel

import android.app.NotificationManager
import android.os.Build
import com.example.limouserapp.data.notification.model.NotificationChannels
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for notification channels
 * Handles creation and management of notification channels
 */
@Singleton
class NotificationChannelManager @Inject constructor(
    private val notificationManager: NotificationManager
) {
    
    /**
     * Initialize all notification channels
     * Should be called during app initialization
     */
    fun initializeChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannels.getAllChannels().forEach { config ->
                try {
                    val channel = config.toNotificationChannel()
                    notificationManager.createNotificationChannel(channel)
                    Timber.d("Notification channel created: ${config.id}")
                } catch (e: Exception) {
                    Timber.e(e, "Error creating notification channel: ${config.id}")
                }
            }
            Timber.d("All notification channels initialized")
        }
    }
    
    /**
     * Check if a channel exists
     */
    fun channelExists(channelId: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(channelId) != null
        } else {
            true // Channels not supported below Android O
        }
    }
}

