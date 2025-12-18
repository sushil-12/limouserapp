package com.example.limouserapp.data.notification.interfaces

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for FCM token management
 * Handles token synchronization with backend
 * Follows SOLID - Single Responsibility Principle
 */
interface FcmTokenRepository {
    /**
     * Current FCM token as StateFlow for reactive updates
     */
    val currentToken: StateFlow<String?>
    
    /**
     * Refresh and get the current FCM token
     */
    suspend fun getToken(): String?
    
    /**
     * Save token locally
     */
    suspend fun saveToken(token: String): Boolean
    
    /**
     * Sync token with backend server
     */
    suspend fun syncTokenWithServer(token: String): Result<Unit>
    
    /**
     * Check if token needs to be synced with server
     */
    suspend fun needsSync(token: String): Boolean
    
    /**
     * Clear stored token (on logout)
     */
    suspend fun clearToken()
}

