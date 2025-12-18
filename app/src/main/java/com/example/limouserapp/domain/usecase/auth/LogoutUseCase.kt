package com.example.limouserapp.domain.usecase.auth

import com.example.limouserapp.data.local.SharedDataStore
import com.example.limouserapp.data.local.UserStateManager
import com.example.limouserapp.data.notification.interfaces.FcmTokenRepository
import com.example.limouserapp.data.repository.AuthRepository
import com.example.limouserapp.data.socket.SocketService
import com.example.limouserapp.data.tracking.RealTimeTrackingService
import com.example.limouserapp.ui.navigation.AuthStateManager
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for logging out user
 * 
 * Handles complete user logout by clearing all user data and services:
 * - Stops real-time tracking services
 * - Disconnects socket connections
 * - Clears FCM tokens
 * - Clears authentication tokens
 * - Clears user state data
 * - Clears shared data store
 * - Refreshes authentication state for navigation
 * 
 * All operations are executed with graceful error handling to ensure
 * maximum cleanup even if individual steps fail.
 */
class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userStateManager: UserStateManager,
    private val socketService: SocketService,
    private val fcmTokenRepository: FcmTokenRepository,
    private val sharedDataStore: SharedDataStore,
    private val realTimeTrackingService: RealTimeTrackingService,
    private val authStateManager: AuthStateManager
) {
    
    /**
     * Execute logout - clears all user data and services
     * 
     * @return Result<Unit> - Always returns success to allow navigation,
     *                        even if some cleanup steps fail
     */
    suspend operator fun invoke(): Result<Unit> {
        Timber.i("LogoutUseCase: Starting logout process")
        
        // Execute all cleanup operations with individual error handling
        // This ensures maximum cleanup even if some steps fail
        executeSafely("Stop real-time tracking") {
            realTimeTrackingService.stopTracking()
        }
        
        executeSafely("Disconnect socket") {
            socketService.disconnect()
        }
        
        executeSafely("Clear FCM token") {
            fcmTokenRepository.clearToken()
        }
        
        executeSafely("Clear authentication tokens") {
            authRepository.logout()
        }
        
        executeSafely("Clear user state") {
            userStateManager.clearUserState()
        }
        
        executeSafely("Clear shared data store") {
            sharedDataStore.clearData()
        }
        
        executeSafely("Refresh authentication state") {
            authStateManager.refreshAuthState()
        }
        
        Timber.i("LogoutUseCase: Logout process completed successfully")
        return Result.success(Unit)
    }
    
    /**
     * Execute an operation safely, logging errors but not failing the entire logout process
     * 
     * @param operationName Human-readable name of the operation for logging
     * @param operation The suspend function to execute
     */
    private suspend fun executeSafely(operationName: String, operation: suspend () -> Unit) {
        try {
            operation()
            Timber.d("LogoutUseCase: $operationName completed successfully")
        } catch (e: Exception) {
            Timber.w(e, "LogoutUseCase: Failed to $operationName (non-critical, continuing logout)")
        }
    }
}

