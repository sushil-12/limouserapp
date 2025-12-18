package com.example.limouserapp

import android.app.Application
import com.example.limouserapp.data.notification.channel.NotificationChannelManager
import com.example.limouserapp.data.notification.FcmTopicManager
import com.example.limouserapp.data.notification.interfaces.FcmTokenRepository
import com.example.limouserapp.data.socket.SocketService
import com.example.limouserapp.data.storage.TokenManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Application class for Limo app
 * Initializes Hilt, logging, and notification system
 */
@HiltAndroidApp
class LimoApplication : Application() {
    
    @Inject
    lateinit var socketService: SocketService
    
    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager
    
    @Inject
    lateinit var fcmTokenRepository: FcmTokenRepository
    
    @Inject
    lateinit var tokenManager: TokenManager
    
    @Inject
    lateinit var fcmTopicManager: FcmTopicManager
    
    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // Initialize notification channels
        notificationChannelManager.initializeChannels()
        
        // Sync FCM token and subscribe to topic if user is authenticated
        applicationScope.launch {
            if (tokenManager.isAuthenticated()) {
                syncFcmToken()
                // Subscribe to FCM topic based on userId (topic = userId)
                fcmTopicManager.subscribeToCurrentUserTopic()
                    .onSuccess {
                        Timber.i("✅ Subscribed to FCM topic on app start")
                    }
                    .onFailure {
                        Timber.w("⚠️ Could not subscribe to FCM topic (userId may not be available yet)")
                    }
            }
        }
    }
    
    /**
     * Sync FCM token with backend
     * Note: This is optional - notifications work without backend sync
     */
    private suspend fun syncFcmToken() {
        try {
            val token = fcmTokenRepository.getToken()
            token?.let {
                Timber.i("📱 FCM Token retrieved: ${it.take(30)}...")
                if (fcmTokenRepository.needsSync(it)) {
                    Timber.d("Attempting to sync FCM token with backend...")
                    val result = fcmTokenRepository.syncTokenWithServer(it)
                    result.onSuccess {
                        Timber.i("✅ FCM token synced successfully with backend")
                    }.onFailure { error ->
                        // Backend sync failed - this is OK, notifications will still work
                        Timber.w("⚠️ Backend token sync unavailable (notifications will still work)")
                        Timber.d("Full FCM Token: $it")
                        Timber.d("Error: ${error.message}")
                    }
                } else {
                    Timber.d("FCM token already synced, skipping")
                }
            } ?: run {
                Timber.w("⚠️ FCM token not available yet")
            }
        } catch (e: Exception) {
            Timber.w(e, "Error syncing FCM token (notifications will still work)")
        }
    }
}
