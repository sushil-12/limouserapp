package com.example.limouserapp.data.notification

import com.example.limouserapp.data.local.UserStateManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for FCM topic subscriptions
 * Handles subscribing and unsubscribing from FCM topics based on user ID
 */
@Singleton
class FcmTopicManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val userStateManager: UserStateManager
) {
    
    companion object {
        private const val TAG = "FcmTopicManager"
    }
    
    /**
     * Subscribe to user-specific topic (topic = userId)
     * This allows backend to send notifications to specific user
     */
    suspend fun subscribeToUserTopic(userId: Int): Result<Unit> {
        return try {
            val topic = userId.toString()
            FirebaseMessaging.getInstance().subscribeToTopic(topic).await()
            Timber.i("✅ Subscribed to FCM topic: $topic (userId: $userId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to subscribe to FCM topic: $userId")
            Result.failure(e)
        }
    }
    
    /**
     * Unsubscribe from user-specific topic
     */
    suspend fun unsubscribeFromUserTopic(userId: Int): Result<Unit> {
        return try {
            val topic = userId.toString()
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic).await()
            Timber.i("✅ Unsubscribed from FCM topic: $topic (userId: $userId)")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to unsubscribe from FCM topic: $userId")
            Result.failure(e)
        }
    }
    
    /**
     * Subscribe to current user's topic if available
     */
    suspend fun subscribeToCurrentUserTopic(): Result<Unit> {
        val userId = userStateManager.getUserId()
        return if (userId != null) {
            subscribeToUserTopic(userId)
        } else {
            Timber.w("⚠️ Cannot subscribe to topic: userId not available")
            Result.failure(Exception("UserId not available"))
        }
    }
    
    /**
     * Unsubscribe from current user's topic if available
     */
    suspend fun unsubscribeFromCurrentUserTopic(): Result<Unit> {
        val userId = userStateManager.getUserId()
        return if (userId != null) {
            unsubscribeFromUserTopic(userId)
        } else {
            Timber.w("⚠️ Cannot unsubscribe from topic: userId not available")
            Result.failure(Exception("UserId not available"))
        }
    }
    
    /**
     * Get current subscribed topics (for debugging)
     * Note: FCM doesn't provide API to list subscribed topics
     */
    fun getCurrentUserId(): Int? {
        return userStateManager.getUserId()
    }
}

