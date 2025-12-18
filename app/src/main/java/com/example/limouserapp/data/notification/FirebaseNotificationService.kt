package com.example.limouserapp.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.limouserapp.MainActivity
import com.example.limouserapp.R
import com.example.limouserapp.data.notification.channel.NotificationChannelManager
import com.example.limouserapp.data.notification.converter.RemoteMessageConverter
import com.example.limouserapp.data.notification.handler.NotificationHandlerManager
import com.example.limouserapp.data.notification.interfaces.FcmTokenRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service
 * Refactored with SOLID principles and industry-grade architecture
 * 
 * Responsibilities:
 * - Receives FCM messages
 * - Converts RemoteMessage to domain models
 * - Delegates to appropriate handlers
 * - Manages FCM token lifecycle
 */
@AndroidEntryPoint
class FirebaseNotificationService : FirebaseMessagingService() {
    
    @Inject
    lateinit var handlerManager: NotificationHandlerManager
    
    @Inject
    lateinit var tokenRepository: FcmTokenRepository
    
    @Inject
    lateinit var channelManager: NotificationChannelManager
    
    // Service scope for coroutines
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "FirebaseNotificationService"
    }
    
    override fun onCreate() {
        super.onCreate()
        // Initialize notification channels
        channelManager.initializeChannels()
        Timber.d("FirebaseNotificationService initialized")
    }
    
    /**
     * Called when a message is received from FCM
     * Handles both data-only and notification messages
     * 
     * NOTE: When app is in FOREGROUND:
     * - Data-only messages: Always trigger this method
     * - Notification payload messages: Always trigger this method (FCM doesn't auto-display)
     * 
     * When app is in BACKGROUND:
     * - Data-only messages: Always trigger this method
     * - Notification payload messages: FCM auto-displays, but also triggers this method
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Timber.i("═══════════════════════════════════════")
        Timber.i("🔥 FCM Message Received")
        Timber.i("From: ${remoteMessage.from}")
        Timber.i("Message ID: ${remoteMessage.messageId}")
        Timber.i("Data payload: ${remoteMessage.data}")
        Timber.i("Notification payload: ${remoteMessage.notification?.title} - ${remoteMessage.notification?.body}")
        
        // Convert RemoteMessage to domain model
        val payload = RemoteMessageConverter.convert(remoteMessage)
        
        if (payload == null) {
            Timber.e("❌ Failed to convert RemoteMessage to NotificationPayload")
            Timber.e("Raw data: ${remoteMessage.data}")
            Timber.e("Raw notification: ${remoteMessage.notification}")
            return
        }
        
        Timber.i("✅ Converted to NotificationPayload:")
        Timber.i("   Type: ${payload.type}")
        Timber.i("   Title: ${payload.title}")
        Timber.i("   Body: ${payload.body}")
        Timber.i("   Booking ID: ${payload.bookingId}")
        
        // Process notification asynchronously
        serviceScope.launch {
            try {
                val handled = handlerManager.processNotification(payload)
                if (handled) {
                    Timber.i("✅ Notification displayed successfully: ${payload.type}")
                } else {
                    Timber.w("⚠️ Notification was not handled: ${payload.type}")
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Error processing notification")
            }
        }
        Timber.i("═══════════════════════════════════════")
    }
    
    /**
     * Called when a new FCM token is generated
     * Syncs token with backend server
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("New FCM token received: ${token.take(20)}...")
        
        // Handle token update asynchronously
        serviceScope.launch {
            try {
                // Save token locally
                tokenRepository.saveToken(token)
                
                // Check if token needs to be synced
                if (tokenRepository.needsSync(token)) {
                    // Sync with server
                    val result = tokenRepository.syncTokenWithServer(token)
                    result.onSuccess {
                        Timber.d("FCM token synced with server successfully")
                    }.onFailure { error ->
                        Timber.e(error, "Failed to sync FCM token with server")
                        // Token will be retried on next app launch
                    }
                } else {
                    Timber.d("FCM token already synced")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error handling new FCM token")
            }
        }
    }
}
