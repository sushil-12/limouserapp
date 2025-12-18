package com.example.limouserapp.data.notification.interfaces

import android.app.PendingIntent
import android.content.Context
import com.example.limouserapp.data.notification.model.NotificationPayload

/**
 * Factory interface for creating notification intents
 * Follows Factory pattern and SOLID principles
 */
interface NotificationIntentFactory {
    /**
     * Create a PendingIntent based on notification payload
     */
    fun createIntent(
        context: Context,
        payload: NotificationPayload,
        requestCode: Int
    ): PendingIntent
}

