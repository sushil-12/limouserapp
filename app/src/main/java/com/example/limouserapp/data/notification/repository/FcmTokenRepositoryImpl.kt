package com.example.limouserapp.data.notification.repository

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.limouserapp.data.api.NotificationApi
import com.example.limouserapp.data.network.error.ErrorHandler
import com.example.limouserapp.data.notification.interfaces.FcmTokenRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of FcmTokenRepository
 * Handles FCM token storage and synchronization with backend
 */
@Singleton
class FcmTokenRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationApi: NotificationApi,
    private val errorHandler: ErrorHandler
) : FcmTokenRepository {
    
    companion object {
        private const val PREFS_NAME = "fcm_token_prefs"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_TOKEN_SYNCED = "token_synced"
    }
    
    private val _currentToken = MutableStateFlow<String?>(null)
    override val currentToken: StateFlow<String?> = _currentToken.asStateFlow()
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    init {
        // Load stored token on initialization
        _currentToken.value = sharedPreferences.getString(KEY_FCM_TOKEN, null)
    }
    
    override suspend fun getToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            Timber.d("FCM Token retrieved: ${token.take(20)}...")
            
            if (token != _currentToken.value) {
                saveToken(token)
            }
            
            token
        } catch (e: Exception) {
            Timber.e(e, "Error getting FCM token")
            _currentToken.value
        }
    }
    
    override suspend fun saveToken(token: String): Boolean {
        return try {
            sharedPreferences.edit()
                .putString(KEY_FCM_TOKEN, token)
                .putBoolean(KEY_TOKEN_SYNCED, false)
                .apply()
            
            _currentToken.value = token
            Timber.d("FCM Token saved locally")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error saving FCM token")
            false
        }
    }
    
    override suspend fun syncTokenWithServer(token: String): Result<Unit> {
        // Token registration APIs disabled per request. Skip network calls and mark as not-synced.
        Timber.i("FCM token sync disabled. Skipping backend registration.")
        sharedPreferences.edit()
            .putBoolean(KEY_TOKEN_SYNCED, false)
            .apply()
        return Result.success(Unit)
    }
    
    override suspend fun needsSync(token: String): Boolean {
        val storedToken = sharedPreferences.getString(KEY_FCM_TOKEN, null)
        val isSynced = sharedPreferences.getBoolean(KEY_TOKEN_SYNCED, false)
        
        return token != storedToken || !isSynced
    }
    
    override suspend fun clearToken() {
        try {
            val deviceId = getDeviceId()
            val currentTokenValue = _currentToken.value
            
            // Unregister API disabled; skipping backend call
            
            // Clear local storage
            sharedPreferences.edit()
                .remove(KEY_FCM_TOKEN)
                .remove(KEY_TOKEN_SYNCED)
                .apply()
            
            _currentToken.value = null
            Timber.d("FCM Token cleared")
        } catch (e: Exception) {
            Timber.e(e, "Error clearing FCM token")
        }
    }
    
    private fun getDeviceId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown_device"
    }
}

