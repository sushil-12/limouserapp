package com.example.limouserapp.data.notification

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for notification permissions
 * Handles Android 13+ (API 33+) notification permission requests
 */
@Singleton
class NotificationPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Check if notification permission is granted
     * For Android 13+ (API 33+), POST_NOTIFICATIONS permission is required
     * For older versions, permission is granted by default
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, notifications are enabled by default
            true
        }
    }
    
    /**
     * Check if notification permission should be requested
     * Only for Android 13+ (API 33+)
     */
    fun shouldRequestPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()
    }
    
    companion object {
        const val PERMISSION_REQUEST_CODE = 1001
    }
}

